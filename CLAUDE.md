# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a full-stack WBS (Work Breakdown Structure) Project Management System with:
- **Backend**: Spring Boot 3.2.0 + MyBatis + MySQL (Java 17, Maven)
- **Frontend**: Vue 3 + TypeScript + Vite + Tailwind CSS + Pinia + DHTMLX Gantt + ECharts

## Development Commands

### Backend
```bash
cd backend
# Build
mvn clean install
# Run
mvn spring-boot:run
# Run tests
mvn test
```

### Frontend
```bash
cd frontend
# Install dependencies
npm install
# Dev server
npm run dev
# Build for production
npm run build
# Preview production build
npm run preview
# Type checking
npx vue-tsc
```

## Backend Architecture

**Package Structure** (`com.wbs.project`):
- `controller/` - REST API endpoints
- `service/` - Business logic layer
- `mapper/` - MyBatis data access layer with XML mappings in `resources/mapper/`
- `entity/` - Domain models (User, Project, Task, Comment, Attachment)
- `enums/` - UserRole, ProjectStatus, TaskStatus, Priority
- `common/Result.java` - Unified API response wrapper with `code`, `message`, `data`
- `config/CorsConfig.java` - CORS configuration

**Key Design Decisions**:
- Foreign keys and cascading deletes are handled in Java code, not database constraints
- Uses JSON fields for list data (e.g., skills)
- Supports soft deletes via status fields
- No Spring Security or authentication implemented yet

**Configuration**: `src/main/resources/application.yml`
- Server runs on port 8080 with context path `/api`
- Database: MySQL on `localhost:3306/db_webwbs`
- MyBatis: camelCase mapping, lazy loading enabled

## Frontend Architecture

**Directory Structure**:
- `views/` - Page components (Dashboard, ProjectList, TaskBoard, GanttView, Reports, Team, Settings, Login)
- `components/` - Reusable Vue components
- `stores/` - Pinia state management (project.ts, task.ts, ui.ts, user.ts)
- `services/api.ts` - Centralized API service layer (currently returns mock data, ready for HTTP integration)
- `router/index.ts` - Vue Router configuration with route metadata
- `types/` - TypeScript type definitions
- `composables/` - Vue composition functions
- `data/` - Mock data for development

**State Management**:
- Uses Pinia for centralized state
- Stores handle project, task, user, and UI state
- Services layer (`services/api.ts`) abstracts data fetching

**Routing**:
- Lazy-loaded route components
- Route meta titles used for page titles
- No authentication guards implemented

**Key Libraries**:
- `dhtmlx-gantt` - Gantt chart visualization
- `echarts` - Charts and statistics
- `vuedraggable` - Drag-and-drop task boards
- `dayjs` - Date manipulation
- `xlsx` - Excel export
- `jspdf` - PDF export

## API Integration

**Backend API**: All endpoints prefixed with `/api`, e.g. `http://localhost:8080/api/users`

**Frontend API**: `services/api.ts` uses `VITE_API_BASE_URL` environment variable (defaults to `/api`)

To connect frontend to backend:
1. Update `services/api.ts` methods to use actual `fetch()` calls instead of mock data
2. Handle `Result<T>` response wrapper from backend
3. Configure CORS in `config/CorsConfig.java` if needed

## Database Schema

Tables: `sys_user`, `sys_project`, `sys_task`, `sys_comment`, `sys_attachment`, `sys_project_member`, `sys_project_tag`, `sys_task_tag`, `sys_task_dependency`

Database name: `db_webwbs` (create with `utf8mb4` charset)
