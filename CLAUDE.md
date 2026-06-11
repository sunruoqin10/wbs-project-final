# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> **Authoritative agent rules live in [`AGENTS.md`](./AGENTS.md).** It contains the canonical code style, naming, persistence, API-contract, and workflow rules. Read it before making non-trivial changes — it overrides any conflicting guidance below. This file is the short orientation; `AGENTS.md` is the rule book.

## Communication

**Default to Chinese (中文) for user-facing dialogue.** The assistant's replies, status reports, explanations, plan summaries, and tool output descriptions should all be in Chinese. Code, identifiers, code comments, and this `CLAUDE.md` itself remain in English. Backend `Result.message` strings stay in Chinese per the [API Contract](#api-contract). The user may switch to English mid-session by simply writing in English — match the user's current language for that turn, then return to Chinese on the next Chinese prompt.

## Project Overview

Full-stack **WBS (Work Breakdown Structure) Project Management System**.

- **Backend** — Spring Boot 3.2.0 + MyBatis + MySQL, Java 17, Maven. Package root `com.wbs.project`.
- **Frontend** — Vue 3 + TypeScript + Vite + Tailwind CSS + Pinia + DHTMLX Gantt + ECharts.

**Features** — project/task management with hierarchical WBS, Gantt visualization, weekly reports with approval workflow, versioned document management, overtime tracking, delay statistics + scheduled email notifications, role-based permissions (admin / project-manager / member / viewer), i18n (zh / ko / en), QQ-Mail SMTP notifications, DiceBear-generated avatars (no upload pipeline).

## Tooling Notes

- **No linters are configured** — no ESLint, Prettier, EditorConfig, or Checkstyle. Follow the existing code style in `AGENTS.md`; do not introduce new linters without asking.
- **No frontend test runner** is configured. Treat `npx vue-tsc` and `npm run build` as the verification gate for frontend changes.
- **The backend test suite is empty** — `backend/src/test/` has no Java tests. Use `mvn -pl . test -DfailIfNoTests=false` to avoid hard failures, and follow JUnit 5 / Spring Boot starter test conventions when adding new ones.

## Development Commands

### Backend (`backend/`)
```bash
mvn clean install                 # build + run all tests
mvn clean install -DskipTests     # build, skip tests
mvn spring-boot:run               # runs on http://localhost:8084 (context /api)
mvn test                          # run all tests
mvn test -Dtest=UserServiceTest              # run one test class
mvn test -Dtest=UserServiceTest#createUser   # run a single test method
```

### Frontend (`frontend/`)
```bash
npm install
npm run dev            # Vite dev server on http://localhost:5173
npm run build          # vue-tsc (type check) + vite build
npm run preview        # serve production build
npx vue-tsc            # type check only
```

### Database
MySQL on `localhost:3306`, database `db_webwbs` (charset `utf8mb4`), default creds `root/root`. Init scripts: `backend/init_weekly_report_tables.bat`, `backend/add_*.sql`.

## High-Level Architecture

**Layered Spring Boot backend** under `com.wbs.project`:
- `controller/` — thin REST endpoints; all responses are wrapped in `common.Result<T>` (`{ code, message, data }`, `code === 200` is success).
- `service/` — business logic, validation, foreign-key / cascade handling. **FKs and cascades are enforced in Java, not in the database** (no `ON DELETE CASCADE`).
- `mapper/` — MyBatis interfaces with XML in `src/main/resources/mapper/`.
- `entity/` — domain models; `dto/` — separate request/response payloads (do not return entities to the controller when a DTO exists).
- `enums/`, `common/Result.java`, `config/` (CORS, FreeMarker), `exception/` (`BusinessException` + `@RestControllerAdvice` `GlobalExceptionHandler`).
- `interceptor/AuthInterceptor` — server-side auth/role enforcement; **this is the only source of truth for auth — no frontend guards**.
- `scheduler/` — `@Scheduled` jobs (e.g. delay-notification email).
- `annotation/` — custom permission/role annotations.
- `util/` — JWT, date helpers, etc.

**Vue 3 + Pinia frontend** (`frontend/src/`):
- `views/` — page-level components (Dashboard, ProjectList, TaskBoard, GanttView, Reports, Team, Settings, Login, WeeklyReports, Documents, OvertimeManagement, DelayStats).
- `components/` — reusable UI, often grouped by domain (`common/`, `project/`, `task/`, `gantt/`, `document/`).
- `stores/` — Pinia stores by domain (`project`, `task`, `user`, `weeklyReport`, `overtime`, `permission`, `ui`). Stores are thin: HTTP lives in `services/api.ts`.
- `services/api.ts` — central API client using `VITE_API_BASE_URL` (default `/api`). Unwraps `Result<T>` here; components receive `T` directly and an `ApiError` on failure. `useUserStore` is the single source of truth for `Authorization` / `X-User-Id` / `X-User-Role` headers.
- `types/index.ts` — TypeScript types mirroring backend entities/DTOs.
- `i18n/locales/{zh,ko,en}.ts` — translations; user-facing strings should go through `$t(...)` (no hard-coded Chinese/English in templates).
- `composables/`, `router/`, `directives/`, `utils/`, `data/`.

## Critical Project Conventions

These are easy to violate and hard to walk back — confirm before changing:

- **User IDs are `C0000001`, `C0000002`, …** (7-digit zero-padded). The next id is computed by `UserMapper.selectMaxIdByPrefix`. **Do not switch to UUIDs.**
- **Soft delete** via `status` fields, not `DELETE` from mappers, unless explicitly required.
- **JWT auth** is enforced by `interceptor/AuthInterceptor`; the frontend has no router guards. Do not duplicate role checks client-side.
- **DTOs vs entities** — return DTOs, not entities, from controllers when a DTO exists.
- **Errors** — throw `BusinessException(code, message)` for expected business errors; the global handler maps to `Result.error(...)`. Let unexpected exceptions flow to `GlobalExceptionHandler`.
- **List-shaped fields** (skills, tags) are JSON strings in a single column, not join tables.
- **SMTP credentials** are hardcoded in `application.yml` for the dev account `631955572@qq.com` (`bkauvfogeavybdcc`). Do not commit production secrets — override `spring.mail.password` via env vars.
- **Backend port is 8084** (context `/api`), not 8080. Frontend should resolve `VITE_API_BASE_URL` to `/api`; CORS for `localhost:5173` is set in `config/CorsConfig.java`.
- **File uploads** land in `./uploads/documents/`, max 10MB/file, 50MB/request. Keep that path in mind for tests and migrations.

## API Contract

All REST responses are `Result<T>`: `{ code: number, message: string, data: T }`. Frontend unwraps in `services/api.ts`; components only see `T`. `message` strings are in **Chinese**.

## Service Startup — Do Not Auto-Start

**Never run `mvn spring-boot:run`, `npm run dev`, `npm run preview`, or any other long-running dev/preview server on your own — even for "verification" or "smoke test" steps.** If you need to verify a change, run the non-interactive command only (e.g. `mvn test`, `mvn clean install -DskipTests`, `npx vue-tsc`, `npm run build`) and stop.

- Do not start the backend (`mvn spring-boot:run`, `java -jar`, `gradle bootRun`, etc.) on your own, even if a curl-based smoke test seems to require it.
- Do not start the frontend dev/preview server (`npm run dev`, `npm run preview`, `vite`, `npx vite`) on your own.
- If a planned task or smoke test step requires a running server (e.g. `curl /api/orgs/tree`), **stop and tell the user** — do not start the server yourself. The user will start it and run the verification, or hand you a JWT/cookie if needed.
- Only start a service when the user **explicitly and unambiguously** says "启动后端" / "启动前端" / "跑一下后端" etc., and stop it again as soon as the requested task is done.
- Treat "I want to verify the API" / "let's do a smoke test" as **NOT** permission to start a server — verification in this repo means `mvn build` / `npm run build` / `vue-tsc` / DB queries via MCP, not running services.

## Git Workflow

- **Never `git commit` or `git push` without explicit user confirmation.** Show the proposed message and file list first, then wait for approval.
- **Even after the user reviews the proposed message and file list, do NOT run `git commit` proactively.** Only commit when the user explicitly says "提交" / "commit" / "commit 吧" / "确认提交" etc. Showing the message + getting an "approved" answer on a checklist is NOT the same as an explicit "commit now" instruction.
- Same rule for destructive operations: `git commit --amend`, `git push --force`, `git reset --hard`, `git rebase`.
- `frontend/dist/` is in `.gitignore`, but a tracked `frontend/dist/index.html` may still show as modified after a build. Do not stage it without explicit user approval.

## Platform

Windows host, **bash** shell by default — use Unix-style paths and forward slashes. PowerShell is **not** the default; `&&` / `||` chain operators may not work there. Prefer `;` for sequential steps in portable commands.

## Detailed Rules

For backend/frontend code style, naming, Lombok usage, import ordering, store structure, i18n key conventions, persistence patterns, and a list of common pitfalls, see **[`AGENTS.md`](./AGENTS.md)**.

## Common Pitfalls (Quick Reference)

- **Port mismatch** — backend is 8084 (`/api`), not 8080. Verify `VITE_API_BASE_URL` and CORS.
- **Hardcoded SMTP password** in `application.yml` — override via env vars in any non-dev environment.
- **Tracked `dist/index.html`** — do not stage without explicit user approval.
- **No client-side auth** — the backend's `AuthInterceptor` is the only source of truth.
- **Test suite is empty** — `mvn test` will not fail only if you use the `mvn -pl . test -DfailIfNoTests=false` variant.
