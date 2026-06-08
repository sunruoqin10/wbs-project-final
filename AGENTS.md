# AGENTS.md

Guidance for AI coding agents working in this repository (WBS Project Management System).

- Backend: Spring Boot 3.2.0 + MyBatis + MySQL (Java 17, Maven). Package root: `com.wbs.project`.
- Frontend: Vue 3 + TypeScript + Vite + Tailwind CSS + Pinia + DHTMLX Gantt + ECharts.
- There is **no ESLint, Prettier, EditorConfig, or Checkstyle** configured. Follow the conventions below; do not introduce new linters without asking.
- There is no `.cursor/`, `.cursorrules`, or `.github/copilot-instructions.md`. This file is the only agent rule source.
- User-facing strings and identifiers are mostly **Chinese (zh-CN)** by convention; keep new copy consistent with that.

## Build / Lint / Test Commands

### Backend (`backend/`)
```bash
mvn clean install                 # build + run all tests
mvn clean install -DskipTests     # build, skip tests
mvn spring-boot:run               # run on http://localhost:8084 (context /api)
mvn test                          # run all tests
mvn test -Dtest=UserServiceTest              # run one test class
mvn test -Dtest=UserServiceTest#createUser   # run a single test method
mvn -pl . test -DfailIfNoTests=false         # safe variant when no tests exist
```

### Frontend (`frontend/`)
```bash
npm install
npm run dev            # Vite dev server on http://localhost:5173
npm run build          # vue-tsc (type check) + vite build
npm run preview        # serve production build
npx vue-tsc            # type check only
```

There is no frontend test runner configured. Treat `vue-tsc` and `npm run build` as the verification gate before declaring a frontend change done.

### Database
- MySQL on `localhost:3306`, database `db_webwbs` (charset `utf8mb4`), creds default `root/root`.
- Init scripts live in `backend/*.bat` and `backend/*.sql` (e.g. `init_weekly_report_tables.bat`).

## Backend Architecture (quick map)

Package layout under `com.wbs.project`:
- `controller/` — REST endpoints, thin wrappers over services.
- `service/` — business logic, foreign-key / cascade handling, validation.
- `mapper/` — MyBatis interfaces; XML in `src/main/resources/mapper/`.
- `entity/` — domain models (`User`, `Project`, `Task`, `WeeklyReport`, `Document`, `OvertimeRecord`, …).
- `dto/` — request/response payloads, separate from entities.
- `enums/` — `UserRole`, `ProjectStatus`, `TaskStatus`, `Priority`, `ReportStatus`.
- `common/Result.java` — unified response wrapper.
- `config/` — CORS, email, FreeMarker config.
- `exception/` — `BusinessException` + `@RestControllerAdvice` `GlobalExceptionHandler`.
- `interceptor/AuthInterceptor` — server-side auth/role enforcement.
- `scheduler/` — `@Scheduled` jobs (e.g. delay-notification email).
- `annotation/` — custom permission/role annotations.
- `util/` — static helpers (JWT, date, etc.).

**ID scheme**: users are `C0000001`, `C0000002`, … (7-digit zero-padded). Next id is computed via `UserMapper.selectMaxIdByPrefix` — do not switch to UUIDs.

**Soft delete**: entities use a `status` field; do not issue `DELETE` from mappers unless explicitly required.

## Backend Code Style

### Imports & layout
- One blank line between package, imports, and class. Imports grouped: `java.*`, then third-party (`lombok`, `org.springframework.*`, etc.), then `com.wbs.project.*` — no blank lines between groups.
- Wildcard imports are not used; import specific classes.
- Classes annotated with Lombok: `@Data` for DTOs/entities, `@RequiredArgsConstructor` for DI in controllers/services, `@Slf4j` for logging, `@Getter`/`@Builder` as needed.

### Naming
- Classes: `PascalCase`. Controllers end with `Controller`, services with `Service`, mappers with `Mapper`, entities match the table singular noun (`User`, `Project`, `Task`, `WeeklyReport`).
- Methods: `camelCase` verbs (`getUserById`, `createUser`, `searchUsers`).
- Constants in enums: `UPPER_SNAKE` value mapped to a Chinese `description` (see `enums/TaskStatus`).
- All API responses are wrapped in `common.Result<T>` with `code` (200 success, 4xx/5xx error), `message` (Chinese), `data`.

### Types & validation
- Entities use wrapper types (no primitives) for DB-nullable fields; IDs are `String` and follow the `C0000001` 7-digit zero-padded scheme — do not switch to UUIDs. The next id comes from `UserMapper.selectMaxIdByPrefix`.
- DTOs are distinct from entities (`dto/` package). Do not return entities directly to the controller when a DTO exists.
- Use `jakarta.validation` annotations on request DTOs; the global handler converts `MethodArgumentNotValidException` to `Result.error(400, ...)`.

### Error handling
- Throw `BusinessException(code, message)` for expected business errors; the `GlobalExceptionHandler` (`@RestControllerAdvice`) maps it to a `Result`.
- Unexpected exceptions are logged and returned as `Result.error(500, "系统异常")`.
- MyBatis / SQL errors are not caught and rethrown as business errors — let `GlobalExceptionHandler` handle them and log.

### Persistence
- Foreign keys and cascading deletes are handled in the **Service** layer, not the database. Do not add `ON DELETE CASCADE` constraints.
- List-shaped fields (skills, tags) are stored as JSON strings in a single column.
- Soft delete via status fields, not `DELETE` rows, unless explicitly required.
- MyBatis XML mappings live in `src/main/resources/mapper/` and follow camelCase Java ↔ snake_case DB mapping (already configured in `application.yml`).

### Configuration
- `src/main/resources/application.yml` — port 8084, context `/api`.
- Do **not** commit production SMTP credentials; override via env vars (`spring.mail.password`, etc.). The current `631955572@qq.com` config is dev-only.

## Frontend Code Style

### Files & components
- Use Vue 3 `<script setup lang="ts">` SFCs. `views/` for pages, `components/` (often grouped: `common/`, `project/`, `task/`, `gantt/`, `document/`, etc.) for reusable UI.
- Naming: `PascalCase.vue` for components, `camelCase.ts` for stores/services/composables/types.
- i18n keys live in `src/i18n/locales/zh.ts`, `ko.ts`, `en.ts`. Add the same key to all three; English may be a stub.

### TypeScript
- `strict` is on (see `tsconfig.json`). Avoid `any`; if unavoidable, narrow with a type guard and a comment.
- Backend entity types live in `src/types/index.ts`; keep them in sync with backend DTOs.
- API calls go through `src/services/api.ts`; the `Result<T>` wrapper is unwrapped there — components receive `T` directly. Use the exported `ApiError` for failure cases.

### State & data
- Pinia stores in `src/stores/*` group by domain (`project`, `task`, `user`, `weeklyReport`, `overtime`, `permission`, `ui`). Keep stores thin: HTTP lives in `services/api.ts`.
- `useUserStore` owns the JWT and is the single source of truth for `Authorization` / `X-User-Id` / `X-User-Role` headers (see `services/api.ts`).
- Use composables (`src/composables/`) for shared reactive logic.

### Styling
- Tailwind utility classes only; do not add a new CSS framework. Theme tokens are `primary-*`, `secondary-*`, `accent-*` (see `Dashboard.vue`).
- Use `vue-i18n` `$t(...)` for all user-facing text. No hard-coded Chinese/English strings in templates.
- Icons are inline SVGs (Tailwind `h-6 w-6`) — there is no icon library dependency.

### Routing
- Lazy-loaded route components in `src/router/index.ts`; route meta includes `title` (used for page title) and i18n keys.
- No frontend router guards — auth is enforced server-side via `interceptor/AuthInterceptor`. Do not add client-side auth checks that the backend does not also enforce.

## API Contract

- All REST responses are `Result<T>`: `{ code: number, message: string, data: T }`. `code === 200` is success; anything else is a business error.
- Frontend unwraps the wrapper in `services/api.ts` — components only see `T`. On failure, an `ApiError` is thrown with the HTTP status and the backend `data` payload attached.
- User-facing `message` strings are in **Chinese**; the frontend displays them via `$t(...)` fallbacks or as-is depending on the call site.

## Git Workflow

- **Never `git commit` or `git push` without explicit user confirmation.** Show the proposed message and file list first.
- Same rule for destructive operations: `git commit --amend`, `git push --force`, `git reset --hard`, `git rebase`.
- `frontend/dist/` is in `.gitignore`. If a previously tracked `dist/index.html` shows as modified, do not include it in the commit unless the user asks.

## Service Startup

- **Do not auto-start backend or frontend services.** Never run `mvn spring-boot:run`, `npm run dev`, `npm run preview`, or any other long-running dev/preview server on your own.
- If a build, type-check, or test is needed to verify a change, run the non-interactive command only (e.g. `mvn test`, `mvn clean install -DskipTests`, `npx vue-tsc`, `npm run build`) and stop. Do not leave servers running in the background.
- Only start a service when the user explicitly asks for it, and stop it again as soon as the requested task is done.

## Platform

- Windows host, **bash** shell by default. Use Unix-style paths and forward slashes.
- PowerShell is **not** the default — `&&` / `||` chain operators may not work there. When writing portable commands prefer `;` for sequential steps in docs.

## Common Pitfalls

- **Port mismatch**: backend runs on **8084** (context `/api`), not 8080. Frontend `VITE_API_BASE_URL` should resolve to `/api` and Vite proxies or CORS (`config/CorsConfig.java`) covers `localhost:5173`.
- **Hardcoded dev SMTP password** lives in `application.yml`. Do not commit production secrets; override `spring.mail.password` (and friends) via env vars.
- **File uploads**: stored under `./uploads/documents/` (configurable in `application.yml`); max 10MB per file / 50MB per request. Keep the directory in mind when changing upload paths in tests.
- **No client-side auth checks** — the backend's `AuthInterceptor` is the source of truth; do not duplicate role checks in the router or stores.
- **`.gitignore` lists `dist/`**, but a tracked `frontend/dist/index.html` may still appear modified after a build. Do not stage it without explicit user approval.
- **Test suite is empty.** `backend/src/test/` has no Java tests today; the `mvn -pl . test -DfailIfNoTests=false` variant avoids hard failures. When adding tests, follow JUnit 5 (Spring Boot starter test) and the same package layout as `src/main/java/com/wbs/project/`.
