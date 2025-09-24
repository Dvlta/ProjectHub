# ProjectHub – Interview Prep

## Elevator Pitch
Spring Boot REST backend for Trello-like task management. Students create projects, invite collaborators, and manage tasks across To‑Do/Doing/Done. Security uses Spring Security + JWT (access/refresh) with role‑based access (OWNER/ADMIN/MEMBER/VIEWER). Data modeled via JPA/Hibernate on PostgreSQL.

## Architecture (High‑Level)
- Request → JWT filter validates access token (subject = email) → SecurityContext → Controller → Service (RBAC checks) → JPA Repository → PostgreSQL
- Email invites via JavaMail (SMTP)

## Core Entities
- User(id, username, email, password[hash], enabled, verificationCode, verificationCodeExpiresAt)
- Project(id, name, key[unique], description, owner)
- ProjectMember(id, project, user, role[OWNER/ADMIN/MEMBER/VIEWER]) with unique (project,user)
- ProjectInvite(id, project, email, token[unique], role, invitedBy, expiresAt, acceptedAt)
- Task(id, project, title, description, status[TODO/DOING/DONE], priority, assignee, reporter, dueDate, orderIndex)

## Auth & RBAC
- Access token: short‑lived JWT (typ=access) sent as `Authorization: Bearer <token>`
- Refresh token: long‑lived JWT (typ=refresh) used only at `/api/auth/refresh`
- RBAC enforced in services: requireMember(projectId), requireRole(projectId, atLeast X), protect last OWNER

## Invitation Flow
1) ADMIN/OWNER invites by email → create token → send email
2) Invitee signs in → POST `/api/invites/{token}/accept` → add membership with role (idempotent if already a member)

## Notable Challenges & Solutions
1) Postgres + transaction pooler (PgBouncer)
   - Symptom: Startup failed with `ERROR: prepared statement "S_1" already exists`
   - Cause: Server‑side prepares clash in transaction pooling
   - Fix: PG driver config for poolers: `preferQueryMode=simple` or `prepareThreshold=0`, plus `autosave=conservative`

2) Jackson vs Hibernate proxies
   - Symptom: 500s with `ByteBuddyInterceptor` serialization errors on list endpoints
   - Cause: Jackson attempting to serialize lazy proxies / deep graphs
   - Fix: `@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})` on entities; `@JsonIgnore` on deep refs (Task.project, Invite.project); eager fetch for Project.owner where appropriate; plan DTOs

3) JWT filter masking errors as 401
   - Symptom: `/api/projects` returned 401 while `/api/users/me` worked
   - Cause: Filter caught downstream exceptions and returned 401
   - Fix: Catch only JWT parse/validation errors; run `filterChain.doFilter` outside try/catch so real errors surface

4) Hibernate DDL warnings
   - Symptom: "constraint ... does not exist, skipping" in logs
   - Cause: `ddl-auto=update` reconciling constraints
   - Fix: Safe to ignore in dev; for prod use Flyway/Liquibase and `ddl-auto=validate/none`

## Testing Strategy (incl. Testcontainers)
- Unit tests for services and utilities
- Integration tests with Testcontainers Postgres: exercise repositories and REST endpoints (auth flows, RBAC, invites, tasks)
- Validate both happy paths and failure cases (401/403/404/409)

## Performance & Scale
- DB: indexes on FKs (`project_id`, `user_id`), `project.key`, invite `token`; paginate lists
- App: stateless JWT enables horizontal scaling; use DTOs and explicit fetches to avoid N+1
- Ordering: `orderIndex` per column; can move to gap‑based ordering if heavy reorders

## Production Hardening
- Refresh token persistence + rotation + revocation on logout/compromise
- HTTPS, strict CORS, rate limiting on auth endpoints
- Secrets via env/secret manager; consider key IDs for JWT rotation
- Migrations with Flyway; turn off JPA schema updates
- `spring.jpa.open-in-view=false` with DTO responses

---

## Common Interview Q → A

1) Architecture overview?
- Layered API: controllers → services → repos. JWT (access/refresh) with a custom filter setting `SecurityContext`. RBAC via `ProjectMember` roles. PostgreSQL via JPA/Hibernate. Email invites via SMTP.

2) Why access + refresh tokens?
- Access tokens stay short‑lived (limit exposure). Refresh tokens enable seamless renewal without re‑auth. Access token carries minimal claims; refresh used only at `/auth/refresh`.

3) How do you enforce project permissions?
- Service guards: `requireMember(projectId)` for any project resource; `requireRole(projectId, ADMIN+)` for management; OWNER required for destructive actions. DB constraint prevents duplicate memberships.

4) How did you model tasks and movement?
- `Task.status` in {TODO, DOING, DONE} and `orderIndex` for Kanban ordering. `PATCH /tasks/{id}/move` updates status and index; default places at end of target column.

5) Why separate `ProjectInvite` from `User`?
- Supports inviting emails not yet registered. On acceptance, we connect the token/email to the authenticated user and create a membership.

6) How did you handle Hibernate lazy loading in JSON?
- Ignore proxy internals with `@JsonIgnoreProperties`, avoid deep references (`@JsonIgnore` on `Task.project`/`Invite.project`), and eagerly fetch `Project.owner` where needed. Longer‑term: DTO responses.

7) N+1 and query optimization?
- Use DTO projections or `JOIN FETCH` for read models, add indexes on common filters, paginate lists, and keep open‑in‑view disabled once DTOs are in place to catch lazy access early.

8) Why JWT over sessions?
- Stateless and horizontally scalable; less server memory. Trade‑off is revocation complexity, mitigated by short access TTLs and refresh rotation/persistence.

9) Global error handling?
- `@ControllerAdvice` mapping to consistent JSON: `{timestamp, status, error, message, path}`. Use 400 (validation), 401 (auth), 403 (RBAC), 404 (not found), 409 (conflict).

10) Biggest issue you debugged and what you learned?
- PgBouncer prepared‑statement clash at startup. Learned to align PG driver settings with pooler mode (`preferQueryMode=simple`/`autosave=conservative`) and to validate environment behavior with Testcontainers.

11) How do you test security and RBAC?
- Integration tests hit endpoints with/without tokens; assert 401/403; create projects, add members with roles, assert permitted/denied actions accordingly.

12) How would you scale this system?
- Stateless app instances behind a load balancer, Postgres with tuned indexes and connection pooling, DTO responses to minimize payloads, caching hot reads if needed, and background jobs for heavy work.

13) Migration strategy for production?
- Use Flyway/Liquibase for deterministic schema. Turn off JPA DDL changes and use `ddl-auto=validate`. Name constraints explicitly and version changes.

14) How do you secure invites?
- Random UUID tokens with expiry; dedupe invites by (project,email); idempotent acceptance; require authentication to accept; audit invitedBy and timestamps.

15) What would you improve next?
- Full DTO layer; refresh token persistence/rotation; comments/labels/subtasks; search/filter; WebSocket updates; more integration coverage (edge cases and load).

