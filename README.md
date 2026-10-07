# be-interview-prep

Five Spring Boot features, each shipped as its own branch, pull request and merge.

**Stack:** Java 17, Spring Boot 3.5, Maven, H2 (in-memory).

## Run the app

```bash
export JWT_SECRET="$(openssl rand -base64 48)"
export ADMIN_EMAIL=admin@example.com      # optional: creates the first ADMIN on startup
export ADMIN_PASSWORD="choose-a-password" # optional, at least 8 characters
./mvnw spring-boot:run
```

The app starts on http://localhost:8080.

`JWT_SECRET` is required (at least 32 bytes); the app refuses to start without it. No secrets live in the source.

### Try the auth API (Q3)

```bash
curl -X POST localhost:8080/api/v1/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"jane@example.com","password":"a-long-password"}'

TOKEN=$(curl -s -X POST localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"jane@example.com","password":"a-long-password"}' | sed 's/.*"accessToken":"\([^"]*\)".*/\1/')

curl localhost:8080/api/v1/users/me -H "Authorization: Bearer $TOKEN"   # 200, own profile
curl localhost:8080/api/v1/users    -H "Authorization: Bearer $TOKEN"   # 403 for a USER
curl localhost:8080/api/v1/users/me                                      # 401, no token
```

## Run the tests

```bash
./mvnw test
```

## Questions

| # | Question | PR link |
|---|----------|---------|
| 1 | Task Manager API | |
| 2 | URL Shortener | |
| 3 | Authentication & Roles | |
| 4 | Product Catalog | |
| 5 | Order Service | |

**Video:**
