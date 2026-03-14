# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a full-stack WBS (Work Breakdown Structure) Project Management System with:
- **Backend**: Spring Boot 3.2.0 + MyBatis + MySQL (Java 17, Maven)
- **Frontend**: Vue 3 + TypeScript + Vite + Tailwind CSS + Pinia + DHTMLX Gantt + ECharts

**Key Features**:
- Project and task management with hierarchical task structures
- Gantt chart visualization with DHTMLX Gantt
- Weekly reports with approval workflow
- Document management with version control
- Overtime tracking and approval
- Delay statistics and notifications
- Role-based permissions (admin, project-manager, member, viewer)
- Multi-language support (Chinese, Korean, English)
- Email notifications (configured for QQ Mail SMTP)

## Development Commands

### Backend
```bash
cd backend
# Build
mvn clean install
# Run (requires MySQL on localhost:3306/db_webwbs)
mvn spring-boot:run
# Run tests
mvn test
# Skip tests during build
mvn clean install -DskipTests
```

### Frontend
```bash
cd frontend
# Install dependencies
npm install
# Dev server (runs on http://localhost:5173)
npm run dev
# Build for production (includes type checking)
npm run build
# Preview production build
npm run preview
# Type checking only
npx vue-tsc
```

### Database Initialization
```bash
# From backend directory
# Initialize weekly report tables
./init_weekly_report_tables.bat
# Check weekly report data
./check_weekly_report_data.sql
```

## Backend Architecture

**Package Structure** (`com.wbs.project`):
- `controller/` - REST API endpoints (handles HTTP requests/responses)
- `service/` - Business logic layer (validation, foreign key handling, cascading operations)
- `mapper/` - MyBatis data access layer with XML mappings in `resources/mapper/`
- `entity/` - Domain models (User, Project, Task, Comment, Attachment, WeeklyReport, Document, OvertimeRecord, etc.)
- `enums/` - UserRole, ProjectStatus, TaskStatus, Priority, ReportStatus
- `common/Result.java` - Unified API response wrapper with `code`, `message`, `data`
- `config/` - CORS configuration, email templates

**Key Controllers**:
- `UserController` - User management
- `ProjectController` - Project CRUD and statistics
- `TaskController` - Task CRUD with hierarchical support
- `WeeklyReportController` - Weekly reports with approval workflow
- `DocumentController` - Document upload/download with versioning
- `OvertimeController` - Overtime tracking and approval
- `DelayNotificationController` - Delay statistics and email notifications
- `PermissionController` - Role-based permission checks

**Key Design Decisions**:
- **Foreign keys and cascading deletes are handled in Java code** (Service layer), not database constraints
- Uses JSON fields for list data (e.g., skills, tags)
- Supports soft deletes via status fields
- File uploads stored locally in `./uploads/documents/` (configurable in application.yml)
- Email notifications via FreeMarker templates in `src/main/resources/templates/`

**Configuration**: `src/main/resources/application.yml`
- Server runs on port 8080 with context path `/api`
- Database: MySQL on `localhost:3306/db_webwbs` (username: `root`, password: `root`)
- File upload: Max 10MB per file, 50MB total request
- Email: QQ Mail SMTP configured (use environment variables for credentials in production)
- MyBatis: camelCase mapping, lazy loading enabled

## Frontend Architecture

**Directory Structure**:
- `views/` - Page components (Dashboard, ProjectList, TaskBoard, GanttView, Reports, Team, Settings, Login, WeeklyReports, Documents, OvertimeManagement, DelayStats)
- `components/` - Reusable Vue components
- `stores/` - Pinia state management (project.ts, task.ts, ui.ts, user.ts, weeklyReport.ts, overtime.ts, permission.ts)
- `services/api.ts` - Centralized API service layer (uses `VITE_API_BASE_URL` environment variable)
- `router/index.ts` - Vue Router configuration with route metadata and i18n integration
- `types/index.ts` - TypeScript type definitions (must match backend entities)
- `composables/` - Vue composition functions
- `i18n/` - Internationalization (locales/zh.ts, locales/ko.ts, locales/en.ts)

**State Management**:
- Uses Pinia for centralized state
- Stores handle project, task, user, overtime, weekly report, permission, and UI state
- Services layer (`services/api.ts`) abstracts data fetching with proper error handling

**Routing**:
- Lazy-loaded route components
- Route meta titles used for page titles
- No authentication guards implemented yet (planned feature)

**Key Libraries**:
- `dhtmlx-gantt` - Gantt chart visualization with export capabilities
- `echarts` - Charts and statistics
- `vuedraggable` - Drag-and-drop task boards
- `dayjs` - Date manipulation
- `exceljs` - Excel export with styling
- `jspdf` + `jspdf-autotable` - PDF export
- `vue-i18n` - Internationalization
- `@vueuse/core` - Vue composition utilities

## API Integration

**Backend API**: All endpoints prefixed with `/api`, e.g. `http://localhost:8080/api/users`

**Frontend API**: `services/api.ts` uses `VITE_API_BASE_URL` environment variable (defaults to `/api`)

**Response Format**: All endpoints return `Result<T>` wrapper:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... }
}
```

**To connect frontend to backend**:
1. Backend is already connected - `services/api.ts` uses `fetch()` with proper error handling
2. Handle `Result<T>` response wrapper from backend
3. CORS is configured in `config/CorsConfig.java` for localhost:5173

## Database Schema

**Core Tables**: `sys_user`, `sys_project`, `sys_task`, `sys_comment`, `sys_attachment`

**Association Tables**: `sys_project_member`, `sys_project_tag`, `sys_task_tag`, `sys_task_dependency`

**Feature Tables**:
- `sys_weekly_report` - Weekly reports with status workflow
- `sys_weekly_report_comment` - Comments on weekly reports
- `sys_document` - Document management with versioning
- `sys_document_access_log` - Document access tracking
- `sys_overtime_record` - Overtime tracking and approval
- `sys_delay_notification` - Delay statistics and notifications

**Database name**: `db_webwbs` (create with `utf8mb4` charset)

**Important**: Foreign key relationships are enforced at application level, not database level.

## Feature-Specific Notes

### Weekly Reports
- Status workflow: draft → submitted → approved/rejected
- Approval by project managers only
- Comment system for feedback
- Weekly date ranges auto-calculated (Monday-Sunday)

### Document Management
- Version control via `parent_id` field
- Categories: requirements, design, development, testing, deployment, documentation, other
- Access logging for downloads/views
- File types validated against whitelist in application.yml

### Overtime Management
- Types: weekday, weekend, holiday
- Approval workflow: pending → approved/rejected
- Compensation: pay or timeoff
- Statistics by project and user

### Delay Statistics
- Task delay tracking with `delayedDays`, `delayCount`, `delayReason`
- Recursive delay calculation for child tasks
- Email notifications for delayed tasks
- Statistics at project and task level

### Permissions
- Roles: admin, project-manager, member, viewer
- Project-level permissions: owner, member
- System-level permissions: user management, system settings
- Checked via `PermissionController` and frontend stores

## Internationalization

**Supported Languages**:
- Chinese (zh) - Default, complete translations
- Korean (ko) - Complete translations
- English (en) - Basic translations

**Adding Translations**:
1. Add keys to `frontend/src/i18n/locales/[zh|ko|en].ts`
2. Use `$t('key')` in Vue components
3. Use `useI18n()` hook in composables

## Export Features

**Excel Export** (using `exceljs`):
- Gantt chart export with task hierarchy
- Styled columns: task name, dates, progress, assignee
- Custom headers and cell formatting

**PDF Export** (using `jspdf` + `jspdf-autotable`):
- Weekly reports
- Task lists
- Statistical reports

## Development Workflow

**Backend Changes**:
1. Modify entity in `entity/`
2. Update mapper interface and XML in `mapper/` and `resources/mapper/`
3. Add business logic in `service/`
4. Add/modify endpoint in `controller/`
5. Test with Postman/curl or frontend

**Frontend Changes**:
1. Update types in `types/index.ts` to match backend
2. Add API methods in `services/api.ts`
3. Create/update Pinia store in `stores/`
4. Build UI components
5. Add routes in `router/index.ts`
6. Add i18n translations

**Common Patterns**:
- Use `Result<T>` wrapper for all API responses
- Handle loading states and error messages in UI
- Use composables for reusable logic
- Maintain type safety between frontend and backend

## Troubleshooting

**Backend fails to start**:
- Check MySQL is running on `localhost:3306`
- Verify database `db_webwbs` exists with `utf8mb4` charset
- Check credentials in `application.yml` (default: `root/root`)

**Frontend API calls fail**:
- Verify backend is running on port 8080
- Check `VITE_API_BASE_URL` in `.env` or environment
- Check CORS configuration in `CorsConfig.java`

**File uploads fail**:
- Verify `./uploads/documents/` directory exists
- Check file size limits in `application.yml`
- Verify file type is in allowed types whitelist

**Email notifications not sent**:
- Check SMTP configuration in `application.yml`
- Verify QQ Mail authorization code (not password)
- Check network connectivity to smtp.qq.com:587

## Testing

**Backend Tests** (JUnit):
```bash
cd backend
mvn test
```

**Frontend Type Checking**:
```bash
cd frontend
npx vue-tsc
```

**Manual Testing**:
- Use frontend UI at http://localhost:5173
- Test API endpoints directly: http://localhost:8080/api
- Check browser console for errors
- Check backend logs for SQL errors (MyBatis logs to stdout)
