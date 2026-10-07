# be-interview-prep

Five Spring Boot features, each shipped as its own branch, pull request and merge.

**Stack:** Java 17, Spring Boot 3.5, Maven, H2 (in-memory).

## Run the app

```bash
./mvnw spring-boot:run
```

The app starts on http://localhost:8080. Data lives in an in-memory H2 database, so it resets on every restart.

## Run the tests

```bash
./mvnw test
```

## Questions

| # | Question | PR link |
|---|----------|---------|
| 1 | Task Manager API | [PR #1](https://github.com/ruben-edstem/be-interview-prep/pull/1) |
| 2 | URL Shortener | |
| 3 | Authentication & Roles | |
| 4 | Product Catalog | |
| 5 | Order Service | |

**Video:**

## Project layout

Code is grouped by feature under `com.edstem.interviewprep`, each feature split into `controller`, `service`, `repository`, `entity`, `dto` and `exception`. Code shared between features lives in `common`.

```
src/main/java/com/edstem/interviewprep/
  common/exception/   shared error handling (ApiException, ErrorResponse, GlobalExceptionHandler)
  task/               Q1: Task Manager API
```

## Q1: Task Manager API

Base path: `/api/v1/tasks`

| Method | Path | Purpose | Success |
|--------|------|---------|---------|
| POST | `/api/v1/tasks` | Create a task | 201 + `Location` header |
| GET | `/api/v1/tasks` | List tasks, optional `?status=TODO\|IN_PROGRESS\|DONE` | 200 |
| GET | `/api/v1/tasks/{id}` | Get one task | 200 |
| PUT | `/api/v1/tasks/{id}` | Replace a task | 200 |
| DELETE | `/api/v1/tasks/{id}` | Delete a task | 204 |

Fields: `title` (required, max 100 characters), `description` (max 1000), `status` (`TODO`, `IN_PROGRESS`, `DONE`; defaults to `TODO` on create, required on update), `dueDate` (`yyyy-MM-dd`, not in the past). `id` and `createdAt` are set by the server.

```bash
curl -i -X POST localhost:8080/api/v1/tasks -H 'Content-Type: application/json' \
  -d '{"title":"Write report","description":"Q3","dueDate":"2030-01-01"}'

curl 'localhost:8080/api/v1/tasks?status=TODO'
```

### Error format

Every error, including framework ones such as an unknown route or a wrong HTTP method, returns the same JSON shape. `fieldErrors` appears only when specific fields are invalid.

```json
{
  "timestamp": "2026-10-07T05:54:24.662877Z",
  "status": 400,
  "error": "VALIDATION_FAILED",
  "message": "Validation failed",
  "path": "/api/v1/tasks",
  "fieldErrors": [
    { "field": "dueDate", "message": "Due date cannot be in the past" },
    { "field": "title", "message": "Title is required" }
  ]
}
```

An unknown task returns 404 with `"error": "TASK_NOT_FOUND"`.
