# Tests + Pluggable AI Providers + Resume-Based Questions

**Date:** 2026-07-20
**Status:** Approved (pending spec review)
**Scope:** Server + Client

## Goals

1. **Test suite** — Add unit + integration tests across server and client. No test framework exists on the client; server has only a single `contextLoads` smoke test.
2. **Pluggable AI provider** — Let the backend use any OpenAI-compatible chat-completion endpoint (OpenAI, opencode-go, Ollama, Groq, Together, vLLM) **or** the existing Gemini client, selected via env var. This lets the user swap providers without code changes (e.g. use their opencode-go API key).
3. **Resume-based questions** — Feed the already-computed resume analysis (strengths, weaknesses, skill ratings, experience) into question generation so questions probe the candidate's actual profile instead of random topics.

## Non-goals

- Anthropic adapter (out of scope; can be added later behind the same interface).
- Multi-provider fan-out / routing / fallback chains.
- Changing the client/server HTTP contract. New env vars are server-side only.
- Migrations / Flyway / Liquibase (Hibernate `ddl-auto=update` stays).

---

## Design

### Feature 1: Pluggable AI provider

**New interface** `com.tourist.server.service.AiService`:

```java
public interface AiService {
    String generateQuestions(List<Question> previousQuestions, ResumeAnalysisDTO resumeAnalysis);
    String extractInfoFromResume(String resumeText);
    String evaluateAnswer(String question, String answer);
    String analyzeResume(String resumeText);
    String summarizePerformance(List<Question> questions);
}
```

All methods return raw response text (same contract as the current `GeminiService`). `generateQuestions` now takes `ResumeAnalysisDTO` (see Feature 2).

**Two implementations, Spring selects one via `@ConditionalOnProperty(prefix="ai", name="provider")`:**

| Implementation | Selected when | Credentials | Endpoint |
|---|---|---|---|
| `GeminiService` (refactored to `implements AiService`) | `ai.provider=gemini` (default) | `gemini.api-key`, `gemini.model-name` (existing) | `generativelanguage.googleapis.com/v1beta/models/{model}:generateContent` |
| `OpenAiCompatibleService` (new) | `ai.provider=openai` | `ai.api-key` | `{ai.base-url}/chat/completions` |

Both use the existing `RestTemplate` bean from `AppConfig`.

**`OpenAiCompatibleService`** POSTs standard OpenAI chat-completion request:

```json
{ "model": "<ai.model>", "messages": [{ "role": "user", "content": "<prompt>" }] }
```

and extracts `choices[0].message.content` from the response.

**New DTOs** (mirror the Gemini record style):
- `dto/OpenAiRequest.java` — `{ model, messages: [{ role, content }] }`
- `dto/OpenAiResponse.java` — `{ choices: [{ message: { content } }] }` with a `content()` helper

**New `application.properties` keys** (defaults preserve current behavior):

```properties
ai.provider=${AI_PROVIDER:gemini}
ai.api-key=${AI_API_KEY:}
ai.base-url=${AI_BASE_URL:https://api.openai.com/v1}
ai.model=${AI_MODEL:gpt-4o}
```

`gemini.api-key` / `gemini.model-name` stay (only read when `ai.provider=gemini`).

**Wiring changes:**
- `InterviewService` and `ResumeService` depend on `AiService` (interface), not `GeminiService`. No other service changes.
- `GeminiService` gains `implements AiService` + `@ConditionalOnProperty(prefix="ai", name="provider", havingValue="gemini", matchIfMissing=true)`.
- `OpenAiCompatibleService` gets `@ConditionalOnProperty(prefix="ai", name="provider", havingValue="openai")`.
- `AppConfig.restTemplate()` bean unchanged.

**Backward compatibility:** Default `ai.provider=gemini` means the deployed backend behaves exactly as today unless `AI_PROVIDER` is set. `GEMINI_API_KEY` / `GEMINI_MODEL_NAME` remain the only required AI env vars in the default config.

### Feature 2: Resume-based questions

**`InterviewService.analyzeAndSetCandidateDetails` change:** Currently `void`; the parsed `ResumeAnalysisDTO` is a local variable that's discarded after copying fields onto `Candidate`. Change the signature to return `ResumeAnalysisDTO` so `startInterview` can pass it to `generateQuestions`. The 7 fields are still set on `Candidate` exactly as before.

**`InterviewService.startInterview` change:** Capture the returned `ResumeAnalysisDTO` and pass it into `aiService.generateQuestions(previousQuestions, resumeAnalysis)`.

**`AiService.generateQuestions` prompt change:** After the existing random-topic + previous-questions context, append a structured block when `resumeAnalysis != null`:

```
Candidate resume analysis:
- Strengths: <comma-joined list>
- Weaknesses: <comma-joined list>
- Skill ratings: <key=value, ...>
- Experience: <key=value, ...>
Generate questions that probe the weaknesses and test the claimed strengths.
```

The 2/2/2 difficulty distribution and the `EASY:/MEDIUM:/HARD:` output format are unchanged, so `parseQuestions` regex needs no change. When `resumeAnalysis == null` (e.g. resume parse failed), the prompt falls back to the current behavior (random topics only) — no regression.

**No schema, DTO, or client contract changes.** This is a prompt-only server change.

---

## Test design

### Server unit tests (Mockito, no Spring context — fast, hermetic)

**`InterviewServiceTest`** — mocks all 5 collaborators (`CandidateRepository`, `InterviewSessionRepository`, `FileStorageService`, `AiService`, `ObjectMapper`). Covers:
- `parseQuestions` via `startInterview`: valid `EASY:/MEDIUM:/HARD:` parses 6 questions; **unparseable Gemini output → throws** (SR-H1 fix); difficulty enum mapping.
- `parseEvaluation` via `submitAnswer`: valid `SCORE:/FEEDBACK:`; missing format → score 0 + fallback; partial.
- `mapToInterviewStateDTO`: IN_PROGRESS returns correct timer per difficulty (EASY=20/MEDIUM=60/HARD=120); COMPLETED returns `null` difficulty + `"Interview Complete!"` + timer 0.
- `submitAnswer`: advances `currentQuestionIndex`; completes at last question (calls `calculateFinalScoreAndSummary`); throws `ResourceNotFoundException` for missing session.
- `getCandidateDetails`: uses `findByCandidateId` (SR-H2 fix); throws for missing candidate and missing session.
- `analyzeAndSetCandidateDetails`: success sets all 7 fields from `ResumeAnalysisDTO`; failure path sets fallback values **without `e.getMessage()`** (SR-M5 fix); returns the DTO.
- **Resume-driven questions:** `startInterview` passes the analysis DTO to `aiService.generateQuestions`.

**`FileStorageServiceTest`** — uses `@TempDir` real filesystem: normal store writes file + returns path; **path-traversal `../` → throws** (SR-M3 fix); null original filename defaults to `"file"`.

**`GeminiServiceTest`** — mocks `RestTemplate`: `generateQuestions` with/without previous questions + with/without `ResumeAnalysisDTO` (verifies prompt content); `evaluateAnswer`/`analyzeResume`/`summarizePerformance`/`extractInfoFromResume` delegate to `generateText`; null response → throws; valid response returns `text()`.

**`OpenAiCompatibleServiceTest`** (new) — mocks `RestTemplate`: all 5 `AiService` methods build an `OpenAiRequest` with the right prompt and extract `choices[0].message.content`; null/empty choices → throws; verifies endpoint URL is `{base-url}/chat/completions`.

### Server integration tests (Testcontainers Postgres — hermetic, supports jsonb)

**`TestcontainersConfiguration`** — shared `@TestConfiguration` with `PostgreSQLContainer("postgres:17-alpine")` + `@ServiceConnection`. Reused across integration test classes. Requires Docker running locally.

**`InterviewControllerIntegrationTest`** — `@WebMvcTest(InterviewController.class)` + `@MockBean InterviewService` + `@Import(GlobalExceptionHandler)`:
- `POST /api/interviews/start` missing name/email/phone → 400 (SR-M2 validation fix).
- `POST /{sessionId}/answer` blank answer → 400 (`@Valid @NotBlank`).
- Valid start → 201; valid answer → 200; `getCandidateDetails` not found → 404.

**`InterviewFlowIntegrationTest`** — `@SpringBootTest` + Testcontainers + `@MockBean AiService` (external API mocked) + real `FileStorageService` pointed at `@TempDir` via `@DynamicPropertySource`:
- Full flow: `startInterview` (mock AiService returns valid question text) → `submitAnswer` ×6 → session COMPLETED, candidate score set.
- Empty-questions guard: mock AiService returns garbage → `RuntimeException`.
- `findByCandidateId` returns the right session (SR-H2).
- `Candidate` jsonb columns (strengths, weaknesses, skillRatings, experienceBreakdown) persist + round-trip.
- **Provider wiring:** run the full flow with `ai.provider=gemini` (bean = `GeminiService`) and with `ai.provider=openai` (bean = `OpenAiCompatibleService`); both satisfy `AiService` injection.

### Client unit tests (Vitest, pure logic — no DOM)

**`interviewSlice.test.js`** — all reducers: `interviewStarted` sets sessionId/status/questions[0]/difficulty/**timer**; `submitAnswer` pushes answer; `nextQuestionReceived` IN_PROGRESS pushes question + sets timer; **`nextQuestionReceived` COMPLETED does NOT push `"Interview Complete!"`** (SR-M7 fix) + sets status; `setResumeFile`; `resetInterview` → initialState.

**`candidatesSlice.test.js`** — `fetchCandidatesStart/Success/DetailSuccess/Failure/clearSelectedCandidate`.

**`store.test.js`** — localStorage persistence: `saveState` **excludes `resumeFile`** (CR-H3 fix); `loadState` returns `undefined` when empty; round-trips state; `subscribe` triggers save.

### Client component + API tests (@testing-library/react + MSW)

**`apiService.test.js`** — MSW handlers for all 5 endpoints: success returns parsed JSON; non-2xx → throws with correct message; verifies FormData construction for `extractResumeInfo`/`startInterviewAPI`.

**`Timer.test.jsx`** — renders `MM:SS` from `duration`; counts down; **calls `onTimeUp` at 0** (fake timers); resets on `duration` change.

**`Home.test.jsx`** — renders both tabs; first labeled "Interviewer", second labeled **"Dashboard"** with LayoutDashboard icon (CR-H2 fix); clicking switches rendered view (NewInterview ↔ Dashboard).

**`NewInterview.test.jsx`** — step 1 file upload (PDF accepted, non-PDF rejected with toast); MSW `extractResumeInfo` → populates form → step 2; submit validation (missing name/email → toast error); valid submit → MSW `startInterviewAPI` → dispatches `interviewStarted` + navigates.

**`IntervieweeChat.test.jsx`** — renders question messages; typing + submit → MSW `submitAnswerAPI` → `nextQuestionReceived` dispatched; **timer `onTimeUp` auto-submits**; COMPLETED status renders Home button → click dispatches `resetInterview` + navigates (CR-M5 fix).

**`Dashboard.test.jsx`** — MSW `fetchCandidatesAPI` → renders table rows; loading state shows Spinner; error state renders message; empty list shows "No candidates found".

**`WelcomeBackModal.test.jsx`** — closed → not visible; open → renders title/buttons; `onResume`/`onStartNew` callbacks fire.

### Tooling setup

**Server `pom.xml`** (test scope): add
- `org.testcontainers:postgresql`
- `org.testcontainers:junit-jupiter`
- `org.springframework.boot:spring-boot-testcontainers`

**Client `package.json`** (devDependencies): add
- `vitest`
- `@testing-library/react`
- `@testing-library/jest-dom`
- `@testing-library/user-event`
- `jsdom`
- `msw`

**Client scripts:** add `test` (`vitest run`), `test:watch` (`vitest`), `test:coverage` (`vitest run --coverage`).

**Client new files:**
- `vitest.config.js` — extends vite config, `environment: 'jsdom'`, setup file.
- `src/test/setup.js` — jest-dom matchers, MSW server lifecycle (beforeEach reset handlers, afterEach cleanup, afterAll close), `localStorage` cleanup.
- `src/test/handlers.js` — default MSW handlers for all 5 API endpoints.

---

## Verification plan

After implementation:

1. **Server:** `./mvnw test` — all unit + integration tests green. Requires Docker running (Testcontainers) and the env vars from `server/.env` (DB + Gemini keys are only read by `GeminiServiceTest`/`OpenAiCompatibleServiceTest` via mocked `RestTemplate`, so live keys are NOT required; Testcontainers provides the DB).
2. **Client:** `npm run test` — all Vitest suites green. `npm run lint` — 0 errors. `npm run build` — succeeds.
3. **Manual smoke (optional):** Set `AI_PROVIDER=openai`, `AI_API_KEY=<opencode-go key>`, `AI_BASE_URL=<opencode-go endpoint>`, `AI_MODEL=<model>` in `server/.env`; start backend; run a full interview from the client; confirm questions reference the resume content.

## Risks / trade-offs

- **`@ConditionalOnProperty` requires exactly one provider bean.** If a future change adds a third bean without the annotation, Spring fails fast with a multiple-candidates error — acceptable (loud failure).
- **Testcontainers requires Docker.** CI/local without Docker can't run `InterviewFlowIntegrationTest`. Mitigation: unit tests + `@WebMvcTest` integration tests run without Docker; only the flow test needs it. Document in AGENTS.md.
- **`OpenAiCompatibleService` request format is the lowest common denominator.** No streaming, no tool calls, no system message (single user message). Sufficient for this app's text-in/text-out prompts; can be extended later.
- **Resume prompt grows token usage.** The analysis JSON is small (~200–500 tokens) vs. raw resume text (potentially thousands). Accepted trade-off (user chose analysis-only).
- **No client test framework existed.** Adding Vitest + jsdom + RTL + MSW is a meaningful new dependency surface, but all are the de-facto Vite-standard stack and dev-only.
