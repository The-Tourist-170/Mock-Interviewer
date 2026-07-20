# AI Interviewer

A resume-aware mock interview app. Upload a resume, the AI analyzes it and asks
questions that probe your actual strengths and weaknesses. Each answer is scored
with feedback, and a final report cards the whole interview.

**Live demo:** https://ai-mern-interviewer.web.app/

---

## Features

- **Resume upload** — PDF is parsed and analyzed for strengths, weaknesses, skill
  ratings, and experience breakdown.
- **Resume-aware questions** — 6 questions (2 easy / 2 medium / 2 hard) generated
  to probe the candidate's weaknesses and test claimed strengths, not random topics.
- **Per-answer scoring** — AI scores each answer 0–10 with written feedback.
- **Final report** — Average score, performance summary, and the full Q&A transcript
  stored per candidate and viewable from the dashboard.
- **Per-question timer** — Difficulty-based: easy 20s, medium 60s, hard 120s.
- **Pluggable AI provider** — Use Gemini (default) or any OpenAI-compatible
  endpoint (OpenAI, opencode-go, Ollama, Groq, Together, vLLM) by changing env vars.
- **Cross-tab session sync** — Resume an in-progress interview from another browser tab.
- **Clean, plain UI** — Light, corporate aesthetic (no glassmorphism / dark mode / gimmicks).

---

## Tech stack

| Layer    | Stack                                                                              |
| -------- | --------------------------------------------------------------------------------- |
| Client   | React 19, Vite 7, Redux Toolkit, React Router 7, Tailwind v4, Framer Motion, Recharts, Headless UI, lucide-react, react-hot-toast |
| Server   | Spring Boot 3.5.6, Java 21, Maven, Spring Data JPA + Hibernate, PostgreSQL        |
| AI       | Gemini by default; any OpenAI-compatible chat-completions endpoint via env switch |
| Infra    | Docker Compose (prod + dev), Firebase Hosting for the client                       |
| Testing  | Vitest + @testing-library/react + MSW (client); JUnit 5 + Mockito + Testcontainers (server) |

---

## Quick start with Docker

The fastest way to run the whole stack. Requires Docker (Orbstack or Docker Desktop).

```sh
docker compose up --build
```

- Client: http://localhost:80
- Server: http://localhost:8080 (internal proxy from nginx at `/api/*`)
- Postgres: internal-only (not exposed)

Nginx serves the React build and reverse-proxies `/api/*` to the server, so the
client and server share an origin and there are no CORS issues in production.

Stop with `docker compose down`.

### Dev mode (hot reload for both apps)

```sh
docker compose -f docker-compose.dev.yml up
```

- Client (Vite HMR): http://localhost:5173
- Server (spring-boot devtools auto-restart): http://localhost:8080
- Postgres exposed on `5433` (avoids conflict with any local Postgres on 5432)

Stop with `docker compose -f docker-compose.dev.yml down`.

---

## Manual setup

If you'd rather run the apps outside Docker (e.g. for active development).

### Prerequisites

- Java 21 (JDK 25 also works — Lombok 1.18.40 handles it)
- Node.js 18+ (npm or bun)
- PostgreSQL 17+ running on `localhost:5432`

### 1. Server

The server reads env vars from `server/.env` on startup via a custom
`EnvEnvironmentPostProcessor` (no manual `source` needed). Create it:

```sh
cat > server/.env <<'EOF'
DB_URL=jdbc:postgresql://localhost:5432/ai_interviewer
DB_USERNAME=admin
DB_PASSWORD=admin

# Default provider (Gemini). To use an OpenAI-compatible endpoint, see below.
GEMINI_API_KEY=your-gemini-key
GEMINI_MODEL_NAME=gemini-2.5-flash-lite

# Or switch to OpenAI-compatible:
# AI_PROVIDER=openai
# AI_API_KEY=sk-...
# AI_BASE_URL=https://api.openai.com/v1
# AI_MODEL=gpt-4o

CORS_ALLOWED_ORIGINS=https://ai-mern-interviewer.web.app,http://localhost:5173,http://127.0.0.1:5173
EOF
```

Create the database and role to match:

```sql
CREATE ROLE admin WITH LOGIN PASSWORD 'admin' CREATEDB SUPERUSER;
CREATE DATABASE ai_interviewer OWNER admin;
```

Then from `server/`:

```sh
./mvnw spring-boot:run     # starts on :8080
```

Hibernate auto-creates/evolves the schema (`ddl-auto=update`); there are no
migration files.

### 2. Client

From `client/`:

```sh
npm install
npm run dev                # Vite dev server on http://localhost:5173
```

The client reads `VITE_API_BASE_URL` from `client/.env`. For local dev against a
server on `:8080`:

```sh
echo 'VITE_API_BASE_URL=http://localhost:8080/api' > client/.env
```

If unset, the client falls back to `/api` (relative — works with nginx same-origin
proxy in the Docker prod stack).

---

## AI provider configuration

The backend implements a `com.tourist.server.service.AiService` interface with two
implementations, selected via `AI_PROVIDER`:

| Provider               | When                          | Env vars                                             |
| ---------------------- | ----------------------------- | --------------------------------------------------- |
| `GeminiService` (default) | `AI_PROVIDER=gemini` or unset | `GEMINI_API_KEY`, `GEMINI_MODEL_NAME`               |
| `OpenAiCompatibleService` | `AI_PROVIDER=openai`          | `AI_API_KEY`, `AI_BASE_URL`, `AI_MODEL`             |

`OpenAiCompatibleService` POSTs `{AI_BASE_URL}/chat/completions` with
`Authorization: Bearer {AI_API_KEY}` and the standard OpenAI request shape. Works
with OpenAI, opencode-go, Ollama, Groq, Together, vLLM, and any other
OpenAI-compatible endpoint.

### Example: opencode-go

```sh
AI_PROVIDER=openai
AI_API_KEY=sk-...
AI_BASE_URL=https://opencode.ai/zen/go/v1
AI_MODEL=deepseek-v4-flash
```

Note: `deepseek-v4-flash` is a reasoning model and takes 10–15s per AI call. For
faster responses, use Gemini Flash-Lite (already the default).

---

## API reference

All endpoints are mounted under `/api`.

| Method | Path                                  | Body                                   | Returns              |
| ------ | ------------------------------------- | -------------------------------------- | -------------------- |
| POST   | `/api/interviews/start`               | multipart: `name`, `email`, `phone`, `resume` (PDF) | 201 `InterviewStateDTO` |
| POST   | `/api/interviews/{sessionId}/answer`  | JSON: `{ "answer": "..." }`            | 200 `InterviewStateDTO` |
| GET    | `/api/interviews/candidates`          | —                                      | 200 `CandidateSummaryDTO[]` |
| GET    | `/api/interviews/candidates/{id}`     | —                                      | 200 `CandidateDetailDTO` / 404 |
| POST   | `/api/resumes/extract-info`           | multipart: `resume` (PDF)              | 200 `ExtractedInfoDTO` |

`InterviewStateDTO` carries the next question, difficulty, timer, and session
status so the client can drive the interview flow from one response.

---

## Testing

### Client (run from `client/`)

```sh
npm run test          # vitest run — 59 tests, no external services
npm run test:watch    # vitest watch
npm run lint          # eslint — 0 errors expected
npm run build         # vite build
```

Tests use Vitest + jsdom + @testing-library/react + MSW (mock fetch). No Docker or
Postgres required.

### Server (run from `server/`)

```sh
./mvnw test                              # all 62 tests
./mvnw test -Dtest=InterviewServiceTest  # single class
```

- Unit tests (Mockito) and `@WebMvcTest` controller tests run with no external deps.
- `InterviewFlowIntegrationTest` and `ServerApplicationTests` use **Testcontainers**
  Postgres and require **Docker running** locally.

---

## Project structure

```
.
├── client/                 # React 19 + Vite frontend
│   ├── src/
│   │   ├── api/            # apiService.js — all backend calls
│   │   ├── components/     # Logo, Timer, Dashboard, Spinner, PageWrapper, WelcomeBackModal
│   │   ├── pages/          # Home, NewInterview, IntervieweeChat, CandidateDetail
│   │   ├── redux/          # interviewSlice, candidatesSlice, store
│   │   ├── hooks/          # useTabSync (cross-tab session sync)
│   │   └── test/           # setup.js, handlers.js (MSW), test-utils.jsx
│   ├── Dockerfile          # prod: node:20 build → nginx:alpine serve + /api proxy
│   ├── Dockerfile.dev      # dev: vite HMR
│   ├── nginx.conf
│   ├── firebase.json       # SPA rewrite to /index.html
│   └── .firebaserc         # project: ai-mern-interviewer
├── server/                 # Spring Boot 3.5.6 backend (Java 21)
│   ├── src/main/java/com/tourist/server/
│   │   ├── controller/     # InterviewController, ResumeController (mounted under /api)
│   │   ├── service/        # InterviewService, AiService + GeminiService / OpenAiCompatibleService, ResumeService, FileStorageService
│   │   ├── repository/     # CandidateRepository, InterviewSessionRepository
│   │   ├── model/          # Candidate, InterviewSession, Question + enums (JPA entities)
│   │   ├── dto/            # InterviewStateDTO, CandidateDetailDTO, ResumeAnalysisDTO, Gemini/OpenAI request/response records
│   │   ├── config/         # AppConfig (RestTemplate), WebConfig (CORS), EnvEnvironmentPostProcessor (auto-load .env)
│   │   └── exception/      # GlobalExceptionHandler, ResourceNotFoundException
│   ├── src/test/java/      # unit + integration tests
│   ├── Dockerfile          # maven build → temurin-21 runtime
│   └── .env                # gitignored — all required env vars live here
├── docker-compose.yml      # prod stack: postgres + server + nginx(client) → http://localhost:80
├── docker-compose.dev.yml  # dev stack: HMR for both, postgres on 5433
└── AGENTS.md               # repo-specific guidance for OpenCode agents
```

---

## Deployment

### Docker (self-hosted)

```sh
docker compose up --build -d   # prod stack on http://localhost:80
```

To deploy publicly, put the compose stack behind a reverse proxy (Caddy, Traefik)
with TLS, or push the `client` image to a container host and the `server` image to
a container host with a managed Postgres.

### Client only (Firebase Hosting)

The client is already configured for Firebase Hosting (project
`ai-mern-interviewer`). From `client/`:

```sh
npm run build        # outputs dist/
firebase deploy      # requires Firebase CLI + auth
```

The hosted SPA calls the backend via `VITE_API_BASE_URL`. Set this in
`client/.env` to point at your deployed backend before building. The live demo at
https://ai-mern-interviewer.web.app/ uses this setup.

---

## Roadmap

Features that would meaningfully improve the app and are good candidates for
contributions. Roughly ordered by impact.

### High impact

- **Streaming AI responses** — Currently each AI call blocks until the full
  response is received (10–15s with reasoning models like `deepseek-v4-flash`).
  Switch to SSE / chunked streaming so the first question and answer feedback
  appear incrementally.
- **Authentication & candidate accounts** — No auth today; anyone can start an
  interview and view any candidate's report. Add OAuth (Google/GitHub) so
  candidates see only their own reports, and an admin role for reviewers.
- **Interview templates by role** — Today questions target the resume generically.
  Add role-specific templates (frontend, backend, system design, behavioral,
  data engineering) that bias the question topics and difficulty mix.
- **Multiple sessions per candidate** — `InterviewSession.candidate` is
  `@OneToOne`, so a candidate can only have one interview. Switch to `@OneToMany`
  + `findByCandidateId` (already in the repository) and a candidate history view
  so users can retake interviews and track progress over time.

### Medium impact

- **Report export** — PDF / JSON / CSV export of the final interview report from
  `CandidateDetail`. Useful for sharing with recruiters.
- **Question type taxonomy** — Beyond easy/medium/hard, tag questions as
  technical / behavioral / system-design / coding and weight the final score by
  category.
- **Anthropic provider** — The `AiService` interface makes this a drop-in: add a
  `ClaudeService` implementation + `@ConditionalOnProperty(ai.provider=anthropic)`
  + the Claude Messages API request/response DTOs.
- **Resume format support** — Only PDF is parsed today (PDFBox). Add DOCX
  (Apache POI) and OCR for scanned PDFs (Tesseract) so candidates aren't forced
  to convert.
- **Pagination + search on the dashboard** — `getAllCandidates` returns the full
  list. Add `Pageable` + search by name/email/score range on
  `CandidateRepository` and a paginated table on the client.
- **Migrations instead of `ddl-auto=update`** — Add Flyway or Liquibase so schema
  changes are reviewable and reversible. Today Hibernate auto-evolves the schema
  with no migration history.

### Quality of life

- **CI pipeline** — No CI today. A GitHub Actions workflow running
  `npm run lint && npm run test` on the client and `./mvnw test` on the server
  (with Docker for Testcontainers) would catch regressions on PRs.
- **Configurable question count & difficulty mix** — Hardcoded to 6 (2/2/2). Let
  the interviewer pick the count and difficulty split from the UI.
- **Rate limiting & abuse protection** — AI calls are expensive. Add per-IP /
  per-session rate limiting on `/api/interviews/start` and `/answer` so a single
  caller can't burn through the AI budget.
- **WebRTC voice mode** — Stream the candidate's spoken answers via WebRTC,
  transcribe with Whisper, and feed the transcript into `evaluateAnswer`. Turns
  the text chat into a real voice interview.
- **Dark mode toggle** — UI is light-only today. The token system in
  `client/src/index.css` is already centralized, so a `[data-theme="dark"]`
  override + a toggle would be straightforward.
- **Internationalization** — All copy is English. Add `react-i18next` and locale
  files; the question prompts would also need a language parameter.

---

## Contributing

Contributions are welcome. The codebase is small, well-tested (118 tests across
both apps), and the boundaries are clean.

### Getting started

1. Fork the repo and clone your fork.
2. Pick something from the **Roadmap** above, or open an issue describing what
   you want to work on so we can align before you write code.
3. Run the dev stack to confirm everything works locally:
   ```sh
   docker compose -f docker-compose.dev.yml up   # HMR for both apps
   ```
   Or run the apps manually (see [Manual setup](#manual-setup)).

### Before opening a PR

- **Server changes:** `./mvnw test` passes (62 tests). If you add a service or
  controller, add unit + integration tests in the same style
  (`@ExtendWith(MockitoExtension.class)` for unit, `@WebMvcTest` / `@SpringBootTest`
  + Testcontainers for integration). Docker must be running for the flow tests.
- **Client changes:** `npm run lint` (0 errors), `npm run test` (59 tests), and
  `npm run build` all pass. If you add a component, add a Vitest + @testing-library/react
  test in `src/` mirroring the existing `.test.jsx` files; mock API calls with MSW
  handlers in `src/test/handlers.js`.
- **Keep the UI plain** — Light theme, minimal motion, no glassmorphism / glow /
  gradient text. See the token definitions in `client/src/index.css` and follow
  the existing solid-border + `shadow-sm` card pattern.
- **Don't commit secrets** — `server/.env` and `client/.env` are gitignored.
  Never put API keys in `application.properties` or source files.
- **No new dependencies without reason** — The stacks are deliberately lean. If
  a new dep is justified, explain why in the PR description.

### Conventions worth knowing

- Server package is `com.tourist.server`; standard Spring Boot layering
  (`controller/`, `service/`, `repository/`, `model/`, `dto/`, `config/`,
  `exception/`). See `AGENTS.md` for the full layout and gotchas.
- AI features go through the `AiService` interface — never call `GeminiService`
  or `OpenAiCompatibleService` directly from `InterviewService` / `ResumeService`.
  If you need a new AI method, add it to the interface and implement it in both.
- JPA entities use Lombok `@Getter` / `@Setter` (no manual accessors). Hibernate
  runs `ddl-auto=update`, so new fields auto-create columns — but for production
  changes prefer adding a Flyway migration (see Roadmap).
- Client uses Redux Toolkit slices (`src/redux/`) for all server state;
  `apiService.js` is the single source of API calls. Don't fetch directly from
  components.
- Tests use text/role assertions, not class names — keep that pattern so UI
  refactors don't break tests.

### Areas that especially need contributions

- **CI/CD** — No pipeline today; a working GitHub Actions setup is the single
  most useful contribution.
- **Streaming responses** — The biggest UX win for any AI provider, especially
  reasoning models.
- **Anthropic provider** — Self-contained, well-scoped, and the `AiService`
  interface makes it a clean drop-in.
- **Auth** — The foundation for candidate history, retake tracking, and admin
  review. Probably the highest-leverage feature after streaming.

---

## Notes

- `server/.env` and `client/.env` are both gitignored — never commit secrets.
- `server/` is registered as a git submodule (gitlink mode) but has no
  `.gitmodules` file and is not initialized. Treat it as plain files on disk;
  its real history lives in a separate repository. See `AGENTS.md` for the full
  git gotcha.
- Hibernate runs with `ddl-auto=update` — schema evolves from entities, no
  migration files.
- See `AGENTS.md` for agent-oriented repo guidance (commands, conventions, gotchas).
