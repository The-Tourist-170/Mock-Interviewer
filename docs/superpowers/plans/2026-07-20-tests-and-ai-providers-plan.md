# Implementation Plan: Tests + Pluggable AI Providers + Resume-Based Questions

**Spec:** `docs/superpowers/specs/2026-07-20-tests-and-ai-providers-design.md`
**Date:** 2026-07-20

## Task ordering

Tasks 1-2 are tightly coupled feature work → manual execution (controller has full context loaded).
Tasks 3, 6 are trivial config changes → manual.
Tasks 4-5, 7-8 are independent test files → subagent-driven (preserves controller context).

### Task 1: Pluggable AI Provider (Feature 1)

**Scope:** Server-only. Create `AiService` interface, `OpenAiCompatibleService` impl + DTOs, refactor `GeminiService` to implement interface, update `InterviewService`/`ResumeService` to depend on `AiService`, add `@ConditionalOnProperty` to both impls, add env vars to `application.properties`.

**Files:**
- NEW `service/AiService.java` — interface with 5 methods (generateQuestions takes `List<Question>` + `ResumeAnalysisDTO`)
- NEW `service/OpenAiCompatibleService.java` — `@Service` + `@ConditionalOnProperty(prefix="ai", name="provider", havingValue="openai")`
- NEW `dto/OpenAiRequest.java` — record `{model, messages:[{role, content}]}`
- NEW `dto/OpenAiResponse.java` — record `{choices:[{message:{content}}]}` + `content()` helper
- MODIFY `service/GeminiService.java` — add `implements AiService`, add `@ConditionalOnProperty(prefix="ai", name="provider", havingValue="gemini", matchIfMissing=true)`, change `generateQuestions` signature to accept `ResumeAnalysisDTO`
- MODIFY `service/InterviewService.java` — change `GeminiService` field to `AiService`
- MODIFY `service/ResumeService.java` — change `GeminiService` field to `AiService`
- MODIFY `src/main/resources/application.properties` — add `ai.provider`, `ai.api-key`, `ai.base-url`, `ai.model`

**Verify:** `./mvnw compile` succeeds.

### Task 2: Resume-Based Questions (Feature 2)

**Scope:** Server-only. `analyzeAndSetCandidateDetails` returns `ResumeAnalysisDTO`; `startInterview` passes it to `generateQuestions`; both impls append analysis to prompt.

**Files:**
- MODIFY `service/InterviewService.java` — `analyzeAndSetCandidateDetails` returns `ResumeAnalysisDTO` (still sets 7 fields on Candidate); `startInterview` captures return + passes to `aiService.generateQuestions(previousQuestions, resumeAnalysis)`
- MODIFY `service/GeminiService.java` — `generateQuestions` appends analysis block when `resumeAnalysis != null`
- MODIFY `service/OpenAiCompatibleService.java` — same prompt change (or delegate to shared prompt builder)

**Verify:** `./mvnw compile` succeeds.

### Task 3: Server Test Tooling

**Scope:** Add Testcontainers deps to `pom.xml` (test scope).

**Files:**
- MODIFY `server/pom.xml` — add `spring-boot-testcontainers`, `org.testcontainers:postgresql`, `org.testcontainers:junit-jupiter`

**Verify:** `./mvnw test-compile` succeeds.

### Task 4: Server Unit Tests

**Scope:** 4 unit test files, Mockito, no Spring context.

**Files:**
- NEW `server/src/test/java/com/tourist/server/service/InterviewServiceTest.java`
- NEW `server/src/test/java/com/tourist/server/service/FileStorageServiceTest.java`
- NEW `server/src/test/java/com/tourist/server/service/GeminiServiceTest.java`
- NEW `server/src/test/java/com/tourist/server/service/OpenAiCompatibleServiceTest.java`

**Verify:** `./mvnw test -Dtest="*ServiceTest"` passes.

### Task 5: Server Integration Tests

**Scope:** Testcontainers Postgres, `@WebMvcTest` + `@SpringBootTest`.

**Files:**
- NEW `server/src/test/java/com/tourist/server/config/TestcontainersConfiguration.java`
- NEW `server/src/test/java/com/tourist/server/controller/InterviewControllerIntegrationTest.java`
- NEW `server/src/test/java/com/tourist/server/InterviewFlowIntegrationTest.java`

**Verify:** `./mvnw test` passes (requires Docker for Testcontainers).

### Task 6: Client Test Tooling

**Scope:** Add Vitest + @testing-library/react + MSW, config files, setup.

**Files:**
- MODIFY `client/package.json` — devDeps + test scripts
- NEW `client/vitest.config.js`
- NEW `client/src/test/setup.js`
- NEW `client/src/test/handlers.js`

**Verify:** `npx vitest run --reporter=verbose` runs (0 tests found is OK at this point).

### Task 7: Client Unit Tests

**Scope:** 3 pure-logic test files (no DOM).

**Files:**
- NEW `client/src/redux/interviewSlice.test.js`
- NEW `client/src/redux/candidatesSlice.test.js`
- NEW `client/src/redux/store.test.js`

**Verify:** `npx vitest run src/redux` passes.

### Task 8: Client Component + API Tests

**Scope:** 7 component/API test files using @testing-library/react + MSW.

**Files:**
- NEW `client/src/api/apiService.test.js`
- NEW `client/src/components/Timer.test.jsx`
- NEW `client/src/pages/Home.test.jsx`
- NEW `client/src/pages/NewInterview.test.jsx`
- NEW `client/src/pages/IntervieweeChat.test.jsx`
- NEW `client/src/components/Dashboard.test.jsx`
- NEW `client/src/components/WelcomeBackModal.test.jsx`

**Verify:** `npx vitest run` passes (all suites). `npm run lint` clean. `npm run build` succeeds.
