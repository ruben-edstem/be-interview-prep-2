| # | Question | PR link |
|---|----------|---------|
| 1 | Task Manager API | [PR #1](https://github.com/ruben-edstem/be-interview-prep/pull/1) |
| 2 | URL Shortener | [PR #2](https://github.com/ruben-edstem/be-interview-prep/pull/2) |
| 3 | Authentication & Roles | |
| 4 | Product Catalog | |
| 5 | Order Service | |

## Configuration

The app needs `JWT_SECRET` (at least 32 bytes) and refuses to start without it. `ADMIN_EMAIL` and `ADMIN_PASSWORD` are optional and create the first ADMIN on startup.

```bash
export JWT_SECRET="$(openssl rand -base64 48)"
./mvnw spring-boot:run
```
