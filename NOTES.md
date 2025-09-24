Key Endpoints

- Auth
    - POST /api/auth/login → returns token, refreshToken
    - POST /api/auth/refresh with body { "refreshToken": "..." } → returns new access token
    - Existing: /api/auth/signup, /api/auth/verify, /api/auth/resend
- Projects
    - POST /api/projects create
    - GET /api/projects list my projects
    - GET /api/projects/{projectId} get
    - PUT /api/projects/{projectId} update (ADMIN+)
    - DELETE /api/projects/{projectId} delete (OWNER)
    - GET /api/projects/{projectId}/members list
    - DELETE /api/projects/{projectId}/members/{userId} remove (ADMIN+; protects last owner)
    - POST /api/projects/{projectId}/invites invite by email and role
- Invites
    - POST /api/invites/{token}/accept accept as logged-in user
- Tasks
    - POST /api/projects/{projectId}/tasks create
    - GET /api/projects/{projectId}/tasks list
    - GET /api/tasks/{taskId} get
    - PUT /api/tasks/{taskId} update
    - DELETE /api/tasks/{taskId} delete (ADMIN/OWNER or reporter)
    - PATCH /api/tasks/{taskId}/move change status/order
    - PATCH /api/tasks/{taskId}/assign set/unset assignee

Models (high-level)

- Project: id, name, key (unique), description, owner, timestamps.
- Member: project, user, role (OWNER, ADMIN, MEMBER, VIEWER), joinedAt.
- Invite: project, email, token, role, expiresAt, acceptedAt, invitedBy.
- Task: project, title, description, status (TODO, DOING, DONE), priority, assignee, reporter, dueDate, orderIndex, timestamps.

Sample Calls

- Create project
    - POST /api/projects with Authorization Bearer <token>
    - Body: { "name": "Capstone Board", "key": "CAP", "description": "CS project" }
- Invite collaborator
    - POST /api/projects/{id}/invites body: { "email": "student@uni.edu", "role": "MEMBER" }
- Accept invite
    - POST /api/invites/{token}/accept (must be authenticated)
- Create task
    - POST /api/projects/{id}/tasks body: { "title":"Write report", "priority":"HIGH" }
- Move task
    - PATCH /api/tasks/{taskId}/move body: { "status":"DOING" }
- Refresh token
    - POST /api/auth/refresh body: { "refreshToken":"<refresh>" }

Config Notes

- Add security.jwt.refresh-expiration-time in application.properties if you want to override default 30 days.
- CORS allowed origins are in SecurityConfiguration.corsConfigurationSource().

Would you like me to:

- Run mvn -q -DskipTests package to verify everything compiles?
- Add lightweight response DTOs to avoid returning entities directly (e.g., to slim down Project/Task responses)?
- Seed a few sample projects/tasks for manual testing?

▌how would i test this in postman

> Here’s a lean Postman workflow to exercise everything end‑to‑end.

Prereqs

- Run the API locally (e.g., http://localhost:8080).
- Have at least one verified user account to log in. If you’re inviting a second user, they’ll also need an account.

Postman Environment
Create an environment with these variables:

- baseUrl = http://localhost:8080
- email / password = your login creds
- accessToken = empty
- refreshToken = empty
- projectId, taskId, inviteToken = empty
- collabEmail = email of user you’ll invite

1) Auth: Login and capture tokens

- Request: POST {{baseUrl}}/api/auth/login
- Body (raw JSON):
  { "email": "{{email}}", "password": "{{password}}" }
- Tests tab:
  pm.test("Save tokens", function () {
  const json = pm.response.json();
  pm.environment.set("accessToken", json.token);
  pm.environment.set("refreshToken", json.refreshToken);
  });

All subsequent protected requests: add header Authorization: Bearer {{accessToken}}.

Optional: Refresh token

- Request: POST {{baseUrl}}/api/auth/refresh
- Body: { "refreshToken": "{{refreshToken}}" }
- Tests tab:
  pm.environment.set("accessToken", pm.response.json().token);

2) Projects: create, list, get, update, delete

- Create project
    - POST {{baseUrl}}/api/projects
    - Body: { "name":"Capstone Board", "key":"CAP", "description":"CS project" }
    - Tests:
      pm.environment.set("projectId", pm.response.json().id);
- List my projects
    - GET {{baseUrl}}/api/projects
- Get project
    - GET {{baseUrl}}/api/projects/{{projectId}}
- Update project (ADMIN+)
    - PUT {{baseUrl}}/api/projects/{{projectId}}
    - Body: { "name":"Capstone Board v2", "description":"Updated" }
- Delete project (OWNER)
    - DELETE {{baseUrl}}/api/projects/{{projectId}}

3) Members: list, invite, accept invite

- List members
    - GET {{baseUrl}}/api/projects/{{projectId}}/members
- Invite collaborator (ADMIN+)
    - POST {{baseUrl}}/api/projects/{{projectId}}/invites
    - Body: { "email": "{{collabEmail}}", "role": "MEMBER" }
    - Tests:
      pm.environment.set("inviteToken", pm.response.json().token);
- Accept invite (as invited user)
    - Log in as {{collabEmail}} (repeat step 1 with that user; this sets a new accessToken).
    - POST {{baseUrl}}/api/invites/{{inviteToken}}/accept
    - You can now list members again to see the user joined.
- Remove member (ADMIN+; cannot remove last OWNER)
    - DELETE {{baseUrl}}/api/projects/{{projectId}}/members/{userId}

4) Tasks: create, list, get, update, move, assign, delete

- Create task
    - POST {{baseUrl}}/api/projects/{{projectId}}/tasks
    - Body: { "title": "Write report", "priority": "HIGH" }
    - Tests:
      pm.environment.set("taskId", pm.response.json().id);
- List tasks
    - GET {{baseUrl}}/api/projects/{{projectId}}/tasks
- Get task
    - GET {{baseUrl}}/api/tasks/{{taskId}}
- Update task (MEMBER+)
    - PUT {{baseUrl}}/api/tasks/{{taskId}}
    - Body: { "description": "Add results section", "priority": "MEDIUM" }
- Move task (MEMBER+)
    - PATCH {{baseUrl}}/api/tasks/{{taskId}}/move
    - Body: { "status": "DOING" } or { "status": "DONE" }
- Assign/unassign (MEMBER+)
    - PATCH {{baseUrl}}/api/tasks/{{taskId}}/assign
    - Body: { "assigneeId": 123 } or { "assigneeId": null }
- Delete task (ADMIN/OWNER or reporter)
    - DELETE {{baseUrl}}/api/tasks/{{taskId}}


Changes Codex made and why
Auth Tokens

- Added refresh tokens support:
    - Updated LoginResponse to include refreshToken and refreshExpiresIn.
    - Enhanced JwtService:
        - security.jwt.refresh-expiration-time property.
        - generateRefreshToken, getRefreshExpirationTime, isRefreshToken.
        - Adds typ="access"/"refresh" claim and ensures subject = email.
    - AuthenticationController:
        - /login now returns both access and refresh tokens.
        - Added /refresh endpoint to mint a new access token from a valid refresh token.
    - AuthenticationService: added loadUserByEmail.
    - Added DTO RefreshTokenRequest.

Domain Models

- Projects/members/invites/tasks:
    - Added Project (owner, key, description, timestamps).
    - Added ProjectMember with unique constraint (project_id,user_id) named uk_project_user.
    - Added ProjectInvite (email, token, role, invitedBy, expires/accepted timestamps).
    - Added Task (status, priority, assignee, reporter, dueDate, orderIndex).
    - Enums: ProjectRole, TaskStatus (TODO/DOING/DONE), TaskPriority.
- Serialization safety:
    - @JsonIgnoreProperties({"hibernateLazyInitializer","handler"}) added to Project, User, Task, ProjectMember, ProjectInvite.
    - User: @JsonIgnore on password, verificationCode, verificationCodeExpiresAt.
    - ProjectMember.project: @JsonIgnore.
    - Task.project: @JsonIgnore.
    - ProjectInvite.project: @JsonIgnore.
- Fetch tweak:
    - Project.owner set to FetchType.EAGER to avoid lazy-proxy serialization issues.

Repositories

- Added JPA repos:
    - ProjectRepository (by key).
    - ProjectMemberRepository (membership checks, list by project/user, count by role).
    - ProjectInviteRepository (by token, exists by project+email).
    - TaskRepository (list ordered by orderIndex, with optional status).

Services

- ProjectService:
    - Create/list/get/update/delete projects with role checks (OWNER/ADMIN/MEMBER/VIEWER model).
    - Manage members: list/remove (protect last owner).
    - Invite flow: create invite (email sent via existing EmailService), accept invite (adds membership).
- TaskService:
    - Create/list/get/update/delete tasks with role checks.
    - Move task across columns with orderIndex handling.
    - Assign/unassign task assignee.

Controllers

- ProjectController:
    - POST /api/projects, GET /api/projects, GET/PUT/DELETE /api/projects/{projectId}
    - Members: GET /api/projects/{projectId}/members, DELETE /api/projects/{projectId}/members/{userId}
    - Invites: POST /api/projects/{projectId}/invites
- InviteController:
    - POST /api/invites/{token}/accept
- TaskController:
    - POST/GET /api/projects/{projectId}/tasks
    - GET/PUT/DELETE /api/tasks/{taskId}
    - PATCH /api/tasks/{taskId}/move, PATCH /api/tasks/{taskId}/assign

JWT Filter

- JwtAuthenticationFilter:
    - Adjusted try/catch so only JWT parsing/validation errors return 401.
    - Moved filterChain.doFilter outside the try to avoid masking controller/service errors as 401.

DTOs

- Projects: CreateProjectDto, UpdateProjectDto, InviteRequestDto.
- Tasks: CreateTaskDto, UpdateTaskDto, MoveTaskDto, AssignTaskDto.
- Auth: RefreshTokenRequest.

Notes/Recommendations (not code)

- For PgBouncer/transaction pooling: set JDBC driver props (e.g., preferQueryMode=simple or prepareThreshold=0 + autosave=conservative).
- Consider spring.jpa.open-in-view=false once DTOs are used consistently.
- In the future, we can return DTOs instead of entities for all endpoints to fully control payloads.

If you want, I can now:

- Convert responses to DTOs for Project, Task, and ProjectMember.
- Add a Flyway baseline for tables/constraints.
- Run a local build to confirm everything compiles.


Here’s a concise log of the issues we hit and how we fixed each.

Auth / 401s

- Symptom: 401 on /api/projects while /api/users/me worked with the same token.
- Root cause: JWT filter caught downstream exceptions (e.g., service/serialization), masking them as 401.
- Fix: Updated JwtAuthenticationFilter to only handle JWT parsing/validation errors and moved filterChain.doFilter outside the try/catch so
  real errors surface with correct status.
- Note: We also switched JWT subject to email and added refresh tokens — re-login ensured tokens matched the new behavior.

DB / PgBouncer + Prepared Statements

- Symptom: App failed to boot with “ERROR: prepared statement 'S_1' already exists”.
- Root cause: Server-side prepared statements + transaction pooling (PgBouncer) cause statement name clashes when Hibernate inspects schema
  on startup.
- Fix: Configure the PostgreSQL driver for pooler-friendly behavior:
    - Preferred: preferQueryMode=simple and autosave=conservative
    - Alternative: prepareThreshold=0 and autosave=conservative
- How to set: Append to JDBC URL (SPRING_DATASOURCE_URL) or via Hikari:
    - spring.datasource.hikari.data-source-properties.preferQueryMode=simple
    - spring.datasource.hikari.data-source-properties.autosave=conservative
    - Or: spring.datasource.hikari.data-source-properties.prepareThreshold=0

Hibernate DDL Warnings

- Symptom: “constraint … does not exist, skipping” during startup.
- Root cause: spring.jpa.hibernate.ddl-auto=update trying to reconcile constraints that may not exist (or were auto-named differently).
- Fix/Advice: Safe to ignore in dev. For prod, use Flyway/Liquibase and set ddl-auto=validate or none.

Open-In-View Warning

- Symptom: “spring.jpa.open-in-view is enabled by default…”.
- Root cause: Default JPA config keeps session open across web layer; not ideal for REST.
- Fix/Advice: Set spring.jpa.open-in-view=false once responses use DTOs or service-layer fetches are complete.

Serialization / ByteBuddy Proxy Errors

- Symptom: 500s with “No serializer found for class org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor” on project/members/tasks
  responses.
- Root cause: Jackson tried to serialize Hibernate lazy proxies (hibernateLazyInitializer/handler) and deep object graphs (Task → Project →
  Owner → User Proxy).
- Fixes applied:
    - Added @JsonIgnoreProperties({"hibernateLazyInitializer","handler"}) on Project, User, Task, ProjectMember, ProjectInvite.
    - Ignored deep references that caused recursion/proxy exposure:
        - Task.project → @JsonIgnore
        - ProjectInvite.project → @JsonIgnore
    - Set Project.owner to EAGER to avoid proxy issues on owner.
- Longer-term: Move to response DTOs to fully control payloads and avoid lazy pitfalls.

API Enhancements (related context)

- Added refresh tokens:
    - /api/auth/login now returns { token, expiresIn, refreshToken, refreshExpiresIn }.
    - /api/auth/refresh validates a refresh token and returns a new access token.
- Implemented project/task/membership/invite features with RBAC and email invites.


ok, I will be having a technical interview with a company soon for a software engineer intern position. During the interview, I will likely be
▌asked about this project, such as questions like general behavioral type questions like what challenges u ran into but they follow up with some
▌technical stuff. Here is how I described this project in my resume: ProjectHub | Java, Spring Boot, Spring Security, JWT, PostgreSQL
▌• Developed a Trello-like task management backend where students can create projects, invite collaborators, and
▌manage tasks with statuses (To-Do, Doing, Done)
▌• Built a secure RESTful API with Spring Boot, JWT (access/refresh), and role-based access control, supporting
▌project sharing via ownership/collaboration.
▌• Designed PostgreSQL schema with JPA/Hibernate, added global exception handling, and wrote integration tests
▌using Testcontainers
▌could you generate some potential questions as well as how I should answer them?

> Below are realistic questions you might get, with crisp, high‑signal answers tied to your project.

Project Overview

- Q: Walk me through the architecture.
- A: Spring Boot REST API, JWT auth (access/refresh), RBAC. Layers: controllers → services → JPA repos on PostgreSQL via Hibernate. Entities:
  User, Project, ProjectMember, ProjectInvite, Task. Security filter validates JWT and sets SecurityContext. Email invites via SMTP. Deployed
  with Postgres behind a transaction pooler.

Key Challenges

- Q: Biggest technical challenges and how you solved them?
- A:
    - Postgres with PgBouncer: hit “prepared statement already exists.” Switched the PG driver to pooler‑friendly settings
      (preferQueryMode=simple or prepareThreshold=0, autosave=conservative).
    - Hibernate proxy serialization: Jackson tried to serialize lazy proxies (ByteBuddy). Fixed by returning DTOs/adding
      @JsonIgnoreProperties({"hibernateLazyInitializer","handler"}) and trimming deep references.
    - JWT filter masking errors as 401: Narrowed try/catch to only token parsing; let downstream exceptions surface correctly.
    - Invitation flow UX: Used single‑use token with expiry; accepting an invite automatically adds membership with role.

Security & Auth

- Q: Why access and refresh tokens?
- A: Short‑lived access token minimizes blast radius; refresh token allows seamless renewal without re‑auth. Access token carries minimal
  claims; refresh token is longer‑lived, used only at /auth/refresh.
- Q: How do you validate and rotate refresh tokens?
- A: Validate signature, expiry, and typ="refresh". For rotation in prod, persist refresh tokens (or their hashes), rotate on every refresh,
  and revoke on logout/compromise.
- Q: How is RBAC enforced?
- A: Membership table ProjectMember(project,user,role) and service guards like “require member” and “require role ≥ ADMIN” around project
  management endpoints. OWNER is a special case for destructive ops.

Data Model & JPA

- Q: Why these entities and relationships?
- A:
    - Project 1..* Task; Project 1..* ProjectMember.
    - ProjectInvite decouples invitee email from a User account.
    - Unique (project,user) constraint prevents duplicate memberships.
- Q: LAZY vs EAGER?
- A: Default LAZY for @ManyToOne can reduce payloads; eagerly loading owner for project read avoids proxy serialization. For lists, prefer DTO
  projections or JOIN FETCH to control queries and avoid N+1.

API Design

- Q: How did you design task movement?
- A: Task.status in {TODO, DOING, DONE} and orderIndex for column ordering; PATCH /tasks/{id}/move updates status and index. Keeps Kanban
  behavior simple and scalable.
- Q: Idempotency and duplicates?
- A: Resource creation is POST (non‑idempotent). Sensitive idempotency areas like invites: prevent duplicate pending invites per email+project
  and enforce unique (project,user) on acceptance.

Testing

- Q: How did you test it?
- A:
    - Unit tests for services and security utils.
    - Integration tests with Testcontainers: spin a real Postgres per test suite, apply schema, run repository and REST tests against real DB
      and security filters.
    - Covered happy paths and authz failures (401/403) plus validation errors.
- Q: Why Testcontainers?
- A: Closer to prod than H2; catches dialect and migration issues, and ensures repeatable, isolated runs.

Global Error Handling

- Q: How is error handling standardized?
- A: @ControllerAdvice maps domain exceptions to consistent JSON (timestamp, code, message, path). Examples: 400 for validation, 401 for auth
  failure, 403 for RBAC, 404 for not found, 409 for conflicts like duplicate keys.

Performance & Scale

- Q: How would you scale this?
- A:
    - DB: indexes on foreign keys (project_id, user_id), token and project key. Paginate list endpoints.
    - App: stateless nodes behind a load balancer; external cache for hot reads if needed.
    - Security: reduce JWT size, verify with cached signing key; consider token introspection if moving away from pure JWT.
    - Ordering: use gap‑based ordering or rebalancing to avoid large shifts; optimistic locking via @Version on tasks if concurrent reorders
      are common.

Follow‑Ups You Might Get

- Q: Why JWT over sessions?
- A: Statelessness, easy horizontal scaling, and client‑side token handling. Trade‑off: revocation is harder; mitigate with short access token
  TTLs and refresh rotation.
- Q: How do you prevent N+1 queries?
- A: DTO projections, JOIN FETCH for specific read models, or batch fetching settings; verify with logs/inspection.
- Q: What would you harden for production?
- A:
    - Persist/rotate refresh tokens and add logout revocation.
    - Migrations with Flyway, ddl-auto=validate.
    - DTO responses everywhere; spring.jpa.open-in-view=false.
    - Centralized logging/metrics, rate limiting on auth endpoints, stricter CORS.

Behavioral (STAR Example)

- Q: Describe a time you debugged a tricky prod‑like issue.
- A:
    - Situation: App failed to boot behind a transaction pooler with Postgres errors.
    - Task: Make startup reliable.
    - Action: Researched driver/pooler interactions, verified via Testcontainers, and set driver to preferQueryMode=simple/
      autosave=conservative.
    - Result: Stable startup; learned to align driver settings with pooler mode and to catch such issues in integration tests.

Quick Demos You Can Do

- Create project → invite collaborator → accept invite → create + move tasks.
- Show 403 by calling a project you don’t belong to.
- Expire access token and refresh at /auth/refresh.

If you want, I can tailor these to your exact implementation choices (e.g., show the specific exception JSON shape you return, or outline a
couple of concrete Testcontainers test cases).




Below are deeper, interview‑ready answers organized by topic. Each builds on your resume bullets and the issues we actually solved.

Project Architecture

- Overview: Spring Boot REST API with layered design — controllers → services → JPA repositories (PostgreSQL via Hibernate). Security via
  Spring Security + JWT (access + refresh). Email invites via JavaMail. Entities: User, Project, ProjectMember, ProjectInvite, Task; enums for
  roles and task status/priority.
- Request flow: JWT filter extracts/validates access token, loads UserDetails by email (subject), injects Authentication into SecurityContext,
  controller methods use it to enforce membership/roles in services.

Authentication & Authorization

- Access vs refresh: Access token is short‑lived and sent on every request (Authorization: Bearer). Refresh token is long‑lived, used only at
  POST /api/auth/refresh to mint a new access token. Tokens include a “typ” claim (“access” or “refresh”) and subject set to the user’s email.
- Why both: Limits risk window for access tokens while keeping UX smooth via refresh. Stateless design scales horizontally.
- Security filter: Custom OncePerRequestFilter verifies signature, expiry, and binds the authenticated user. We restricted its try/catch to
  only handle JWT errors so that downstream exceptions don’t appear as 401s.

Role‑Based Access Control (RBAC)

- Model: ProjectMember(project, user, role), with roles OWNER, ADMIN, MEMBER, VIEWER.
- Enforcement: Service‑level guards:
    - requireMember(projectId) for any project scope access
    - requireRole(projectId, atLeast ADMIN) for managing projects/members/invites
    - OWNER gate for destructive actions (delete project, cannot remove last owner)
- Sharing: Ownership and collaboration are explicit via membership records; invitation flow turns email invites into memberships.

Data Model & Schema

- Entities:
    - Project: name, key (unique), description, owner, timestamps.
    - ProjectMember: unique (project,user) to prevent duplicates; role enum.
    - ProjectInvite: email, token (unique), invitedBy, role, expiresAt, acceptedAt.
    - Task: title, description, status (TODO/DOING/DONE), priority, assignee, reporter, dueDate, orderIndex for column ordering.
- Rationale:
    - Separate invites from users to handle not‑yet‑registered collaborators.
    - Unique constraints uphold invariants at the DB layer (e.g., one membership per user per project).

Notable Challenges & Fixes

- PgBouncer + prepared statements:
    - Symptom: App failed to boot with “prepared statement 'S_1' already exists.”
    - Root cause: Server‑side prepares clash in transaction pooling.
    - Fix: Use pooler‑friendly PG driver settings (preferQueryMode=simple or prepareThreshold=0) plus autosave=conservative. After update, the
      EntityManagerFactory initializes reliably.
- Jackson vs Hibernate proxies:
    - Symptom: 500s “No serializer for ByteBuddyInterceptor” on list endpoints.
    - Root cause: Jackson attempted to serialize lazy proxies (hibernateLazyInitializer/handler) and deep graphs.
    - Fix: Add @JsonIgnoreProperties({"hibernateLazyInitializer","handler"}) to entities, avoid deep references (@JsonIgnore on Task.project,
      Invite.project), and eagerly fetch Project.owner when needed. Longer-term: return DTOs.
- JWT filter masking errors as 401:
    - Symptom: /api/projects returned 401 while token worked on /api/users/me.
    - Root cause: Filter wrapped non‑auth errors in a catch‑all and returned 401.
    - Fix: Restrict catch to token parsing/validation and move filterChain.doFilter outside the try/catch.
- Invitation flow edge cases:
    - Duplicate pending invites: prevent duplicates on (project,email).
    - Expired/already accepted tokens: clear messaging and idempotent behavior.

API Design Choices

- Task movement:
    - Status transitions via PATCH /api/tasks/{id}/move with { "status": "DOING", "orderIndex": 12 }.
    - OrderIndex supports Kanban ordering without heavy joins or extra link tables; placing at end of column by default keeps logic simple.
- Idempotency & conflicts:
    - Unique constraints and existence checks for project keys, invite tokens, and memberships provide practical conflict detection (409).
    - For invites, accepting with an already‑member user marks the invite accepted but doesn’t duplicate membership.

Testing Strategy (with Testcontainers)

- Why Testcontainers: Real Postgres catches dialect issues and migration/DDL‑related behavior that H2 misses. Gives deterministic, isolated
  integration tests.
- What we test:
    - Auth: signup/verify/login/refresh; invalid credentials; expired/invalid tokens behavior.
    - RBAC: 403s for non‑members; ADMIN vs OWNER paths; cannot remove last owner.
    - Invitations: creating invite, duplicate detection, accepting token, expired token.
    - Tasks: CRUD, moving across statuses, ordering, assignment, permissions (reporter vs admin).
- Pattern:
    - Boot full Spring context with a Testcontainers Postgres.
    - Seed minimal data per test (or use @Sql) and assert both HTTP responses and DB state.

Global Exception Handling

- Goal: Consistent, minimal error JSON, clear semantics (400/401/403/404/409).
- Approach: @ControllerAdvice translates domain exceptions (e.g., ResponseStatusException) to a standard payload: { timestamp, status, error,
  message, path }.
- Benefits: Uniform client handling and easier troubleshooting.

Performance & Scalability

- DB:
    - Indexes: project_id, user_id, token, project.key, and common filter columns (e.g., task.status).
    - Pagination on list endpoints; avoid returning entire graphs.
- App:
    - Stateless JWT auth allows horizontal scaling behind a load balancer.
    - DTO responses and explicit fetch strategies to prevent N+1 queries and overserialization.
- Ordering:
    - Start with simple integer orderIndex; if heavy reordering becomes frequent, adopt gap‑based ordering or periodic rebalancing.

Security Hardening (Production)

- Token management: Store/rotate refresh tokens (hash), revoke on logout/compromise, detect reuse.
- Transport: Enforce HTTPS, secure CORS, rate limiting on auth endpoints.
- Secrets: Proper key management (env/secret manager), rotate keys with key IDs if needed.
- DB schema: Use Flyway/Liquibase, switch JPA to ddl-auto=validate or none.
- Observability: Centralized logs/metrics/traces (e.g., request IDs, audit logs on critical actions).

JWT vs Sessions (Tradeoffs)

- JWT pros: Stateless, easy to scale, lower server memory overhead. Cons: harder revocation; mitigate with short TTLs, refresh rotation, and
  blacklists.
- Sessions pros: Easy revocation and server‑side control; cons: stateful (stickiness or distributed cache), scaling complexity.
- Chosen: JWT for simplicity and scalability; added refresh rotation readiness for production.

Preventing N+1 and Lazy Pitfalls

- Techniques:
    - Response DTOs that fetch only what you need.
    - JOIN FETCH or entity graphs for specific read models.
    - Batch size hints if needed; verify with SQL logs.
- Open‑in‑view:
    - Prefer spring.jpa.open-in-view=false with DTOs to catch lazy access at the service layer and avoid runtime surprises in controllers.

Future Enhancements

- DTOs for all responses (ProjectSummaryDto, TaskDto, MemberDto) to finalize payloads and remove entity leakage.
- Comments/subtasks/labels, search/filtering, and activity streams.
- WebSocket/polling for board updates; optimistic locking/versioning on tasks.

STAR Stories (Expanded)

- Debugging DB startup failure
    - Situation: App failed to boot using PgBouncer with Postgres; error: “prepared statement ‘S_1’ already exists.”
    - Task: Make startup reliable and environment‑agnostic.
    - Action: Researched PG driver behavior with transaction pooling, reproduced locally, and changed driver settings to preferQueryMode=simple
      (or prepareThreshold=0) plus autosave=conservative. Documented config for dev/prod parity.
    - Result: Stable startup across environments; learned to align database driver settings with pooler modes and to catch such issues with
      Testcontainers.
- Fixing proxy serialization in REST
    - Situation: List endpoints intermittently 500’d with Jackson errors about ByteBuddy/Hibernate proxies.
    - Task: Ensure deterministic, safe JSON serialization.
    - Action: Added proxy ignore annotations to entities, pruned deep references with @JsonIgnore, and adjusted fetch for specific fields
      (Project.owner). Planned move to DTOs to fully control payloads.
    - Result: Endpoints stabilized, clients received consistent JSON; improved my understanding of JPA fetch semantics and Jackson integration.

If you want, I can tailor 1–2 deep dives (e.g., walk through the invite acceptance flow with code references, or sketch a sample Testcontainers
test) so you can demo them fluently in the interview.
