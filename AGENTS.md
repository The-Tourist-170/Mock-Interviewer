# AGENTS.md

Repo-specific guidance for OpenCode agents. Verify against the codebase before relying on anything in `README.md` — parts of it are stale.

## Repository shape

Two independent apps with no shared build tooling:

- `client/` — React 19 + Vite 7 + Redux Toolkit + Tailwind v4 + Firebase Hosting. JavaScript/JSX (no TypeScript). Tracked directly in the root repo.
- `server/` — Spring Boot 3.5.6 + Java 21 + Maven + PostgreSQL + Hibernate + pluggable AI (Gemini or any OpenAI-compatible endpoint). Java package `com.tourist.server`.
- Root `package-lock.json` is an empty placeholder — there is no root npm project. Root remote: `The-Tourist-170/AI-Interviewer`.

### Git gotcha (important)

`server/` is registered as a git submodule (gitlink mode `160000`) but there is **no `.gitmodules`** file and the submodule is **not initialized** (no `server/.git`).

- `git submodule update --init` fails with "no submodule mapping found in .gitmodules for path 'server'".
- Git commands run from the root on `server/` paths fail with "in submodule 'server'".
- Treat `server/` as plain files on disk. The real server history lives in a separate repository; this repo only holds a pointer to commit `861cba0`.
- `client/` is a normal subdirectory — git works there from the root.

There is no CI, no pre-commit config, and no root `.gitignore`.

## Commands

### Client (run inside `client/`)

```
npm install        # or: bun install  (both bun.lock and package-lock.json exist)
npm run dev        # Vite dev server on http://localhost:5173
npm run build      # outputs dist/  (Firebase Hosting public dir)
npm run lint       # eslint .  — flat config; `no-unused-vars` ignores `^[A-Z_]|motion|AnimatePresence`
npm run test       # vitest run — unit + component + API tests (MSW mocks fetch)
npm run test:watch # vitest watch mode
npm run preview    # serve the production build locally
firebase deploy    # deploy Hosting (project: ai-mern-interviewer); requires Firebase CLI + auth
```

### Server (run inside `server/`)

```
./mvnw spring-boot:run   # starts on :8080  (or: mvn spring-boot:run)
./mvnw test              # all tests: unit + @WebMvcTest + @SpringBootTest flow (Testcontainers)
./mvnw test -Dtest=InterviewServiceTest  # run a single test class
./mvnw package           # build jar; Dockerfile uses: mvn package -DskipTests
```

Requires Java 21 (README says "JDK 17+" — wrong; `pom.xml` pins `java.version=21` and the Dockerfile uses `temurin-21`). Lombok 1.18.40 + the `annotationProcessorPaths` config in `pom.xml` compile fine under JDK 25 too.

### Server env loading (auto-loaded)

`server/.env` is **auto-loaded** by a custom `EnvEnvironmentPostProcessor` (`config/EnvEnvironmentPostProcessor.java`, registered via `META-INF/spring.factories`). It reads `.env` from the working directory at Spring Boot startup — no manual `source` needed. Works for `./mvnw test`, `./mvnw spring-boot:run`, and `java -jar`. Real environment variables override `.env` values (`.env` is lowest-priority property source).

Override the `.env` path with `-Denv.file=/path/to/.env` if needed.

`application.properties` reads these env vars with no defaults — they MUST exist in `.env` (or the real environment), or Spring receives the literal `${DB_URL}` and fails with `Driver claims to not accept jdbcUrl, ${DB_URL}`.

## Verification

No enforced pipeline. Effective checks:

- Client: `npm run lint` + `npm run test` (Vitest, no external services).
- Server: `./mvnw test` — unit tests run with no external deps; the `@SpringBootTest` flow test (`InterviewFlowIntegrationTest`) and `ServerApplicationTests` use **Testcontainers** and require **Docker running** locally. `@WebMvcTest` controller tests and pure Mockito unit tests run without Docker.

## Server configuration

All required env vars (from `server/src/main/resources/application.properties`) have **no defaults** and must be set (e.g. in `server/.env`):

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` — PostgreSQL connection
- `GEMINI_API_KEY`, `GEMINI_MODEL_NAME` — Gemini API credentials (used only when `AI_PROVIDER=gemini`)
- `AI_PROVIDER`, `AI_API_KEY`, `AI_BASE_URL`, `AI_MODEL` — pluggable AI provider (see below)

Notes:
- `server/.env` is **now in `server/.gitignore`** — do not `git add` it.
- `file.upload-dir=./uploads` — uploaded files are written to `server/uploads/`.
- `spring.jpa.hibernate.ddl-auto=update` — Hibernate auto-evolves the schema from entities. There are **no migration files** (no Flyway/Liquibase).

## AI provider is pluggable (Gemini or OpenAI-compatible)

`README.md` says OpenAI — that is stale. The backend implements a `com.tourist.server.service.AiService` interface with two implementations selected via `ai.provider` (`@ConditionalOnProperty`):

- **`GeminiService`** (default, `ai.provider=gemini`) — calls `https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent` with the API key via `RestTemplate`. Reads `gemini.api-key` / `gemini.model-name`.
- **`OpenAiCompatibleService`** (`ai.provider=openai`) — POSTs `{ai.base-url}/chat/completions` with `ai.api-key` + `ai.model`. Works with any OpenAI-compatible endpoint (OpenAI, opencode-go, Ollama, Groq, Together, vLLM).

To switch providers, set `AI_PROVIDER=openai` + the `AI_*` vars in `server/.env`. Default is `gemini` for backward compatibility. The `google-cloud-vertexai` dependency in `pom.xml` is unused. `ALLOWED_ORIGINS` in the Spring config metadata JSON is a dead stub — nothing reads it.

Question generation is **resume-aware**: `analyzeAndSetCandidateDetails` returns a `ResumeAnalysisDTO` (strengths, weaknesses, skillRatings, experienceBreakdown) which is passed into `generateQuestions` so questions probe the candidate's actual profile.

## Hardcoded values that affect local dev

- **CORS**: `server/.../config/WebConfig.java` allows only `https://ai-mern-interviewer.web.app`. A local frontend hitting the deployed backend (or any non-allowlisted origin) will be blocked; change the origin in `WebConfig.java`.
- **API base URL**: `client/src/api/apiService.js` reads `import.meta.env.VITE_API_BASE_URL` (from `client/.env`), falling back to `http://localhost:8080/api`. To point the frontend at another backend, edit `client/.env`.

## Client conventions

- ESLint flat config (`eslint.config.js`); `no-unused-vars` ignores identifiers starting with an uppercase letter or underscore, plus `motion` and `AnimatePresence` (framer-motion JSX tags).
- Tests: Vitest + jsdom + @testing-library/react + MSW. Config in `vitest.config.js`; setup in `src/test/setup.js`; MSW handlers in `src/test/handlers.js`; render helper `renderWithProviders` in `src/test/test-utils.jsx`.
- SPA routing: Firebase Hosting rewrites all routes to `/index.html` (`firebase.json`); `react-router-dom` v7 is used.
- Key libs: `@reduxjs/toolkit`, `react-redux`, `framer-motion`, `@headlessui/react`, `recharts`, `lucide-react`, `react-hot-toast`.

## Server layout

Standard Spring Boot layers under `com.tourist.server`: `controller/`, `service/` (`InterviewService`, `AiService` interface + `GeminiService` / `OpenAiCompatibleService` impls, `ResumeService`, `FileStorageService`), `repository/`, `model/` (JPA entities: `Candidate`, `InterviewSession`, `Question` + enums), `dto/`, `config/` (`AppConfig` provides `RestTemplate`, `WebConfig` CORS), `exception/` (`GlobalExceptionHandler`). REST endpoints are mounted under `/api` (see `InterviewController`, `ResumeController`).
