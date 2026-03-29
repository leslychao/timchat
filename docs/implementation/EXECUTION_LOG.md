# TimChat — MVP Execution Log

## Usage

This file tracks the execution of each stage from [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md).

After completing each stage, fill in the corresponding section below.
Do not remove sections for stages that haven't been executed yet — keep them as templates.

---

## Stage 1: Project Scaffolding

| Field | Value |
|-------|-------|
| Date/Time | 2026-03-29T06:07+04:00 |
| Status | DONE |
| Executor | AI Agent |

### Changes Made

_List of files created/modified:_

- `pom.xml` — root Maven parent (Java 17, Spring Boot 3.3.5, modules: backend)
- `backend/pom.xml` — backend module with all required dependencies
- `backend/src/main/java/ru/timchat/TimChatApplication.java` — @SpringBootApplication
- `backend/src/main/java/ru/timchat/infra/config/SecurityConfig.java` — permit-all security config
- `backend/src/main/resources/application.yml` — datasource, redis, liquibase config
- `backend/src/main/resources/db/changelog/db.changelog-master.yaml` — empty changelog
- `backend/src/test/java/ru/timchat/TimChatApplicationTest.java` — smoke test
- `backend/src/test/resources/application-test.yml` — H2 + no Redis for tests
- `infra/docker-compose.yml` — PostgreSQL 16, Redis 7, MinIO with health checks
- `infra/.env.example` — default credentials
- `frontend/` — Angular 18 project (standalone components, SCSS, routing)
- `frontend/src/app/app.component.ts` — cleaned boilerplate
- `.gitignore` — updated for Java, Node, Angular, IDE
- `.mvn/wrapper/` — Maven Wrapper (3.9.9)
- `mvnw`, `mvnw.cmd` — Maven Wrapper scripts

### Risks Found

- Node.js v21.7.3 is odd-numbered (not LTS), Angular 18 shows engine warnings. Functional but not ideal for production.
- Java 21 not available on this machine — Java 17 used instead. Spring Boot 3.3.5 supports Java 17+, so no functional impact.

### Gaps Found

- Java version downgraded from planned 21 to 17 due to local environment. Plan originally specified Java 21.
- Angular CLI version: using 18 instead of latest due to Node 21 incompatibility with Angular 21.

### Fixes Applied

_Fixes beyond original stage scope:_

- Added `SecurityConfig` with permit-all to prevent Spring Security from blocking all requests by default
- Added `application-test.yml` with H2 and disabled Redis/Liquibase for test profile
- Added H2 test dependency in `backend/pom.xml`
- Added Maven Wrapper (`mvnw`/`mvnw.cmd`) since Maven was not installed on the system
- Removed deprecated `hibernate.dialect` explicit config (auto-detected by Hibernate 6.5+)

### Tests Run

| Test | Result |
|------|--------|
| mvn clean compile | PASS — BUILD SUCCESS |
| ng build | PASS — bundle generated |
| docker-compose up | PASS — all 3 services healthy |
| Spring Boot context loads | PASS — connected to PG, Redis, Liquibase ran |
| TimChatApplicationTest (smoke) | PASS — 1 test, 0 failures |

### Result

- [x] All acceptance criteria met
- [ ] Committed

### Commit

| Field | Value |
|-------|-------|
| Hash | pending |
| Message | feat: scaffold project structure with Maven backend, Angular frontend, and Docker Compose infra |
| Pushed | pending |

### Next Stage

Stage 2: Common Module & Error Handling

### Notes

_Additional observations, decisions, or deviations:_

- Java 17 used instead of 21 — environment constraint, no functional impact for Spring Boot 3.3.5
- Angular 18 used instead of latest — Node 21.7.3 incompatibility with Angular 21
- Maven Wrapper added since Maven was not pre-installed on the system
- SecurityConfig added early to prevent Spring Security default deny-all behavior
- H2 used for tests to avoid requiring live PostgreSQL for unit tests

---

## Stage 2: Common Module & Error Handling

| Field | Value |
|-------|-------|
| Date/Time | |
| Status | PLANNED |
| Executor | |

### Changes Made

- 

### Risks Found

- 

### Gaps Found

- 

### Fixes Applied

- 

### Tests Run

| Test | Result |
|------|--------|
| GlobalExceptionHandler tests | |
| i18n locale resolution | |

### Result

- [ ] All acceptance criteria met
- [ ] Committed

### Commit

| Field | Value |
|-------|-------|
| Hash | |
| Message | |
| Pushed | |

### Next Stage

Stage 3: Auth Module

### Notes

---

## Stage 3: Auth Module (Keycloak)

| Field | Value |
|-------|-------|
| Date/Time | |
| Status | PLANNED |
| Executor | |

### Changes Made

- 

### Risks Found

- 

### Gaps Found

- 

### Fixes Applied

- 

### Tests Run

| Test | Result |
|------|--------|
| Keycloak starts in docker-compose | |
| timchat realm imported | |
| Obtain JWT from Keycloak | |
| 401 without token | |
| 200 with valid Keycloak JWT | |
| CurrentUserContext extraction | |
| CORS validation | |

### Result

- [ ] All acceptance criteria met
- [ ] Committed

### Commit

| Field | Value |
|-------|-------|
| Hash | |
| Message | |
| Pushed | |

### Next Stage

Stage 4: User & Workspace

### Notes

---

## Stage 4: User & Workspace

| Field | Value |
|-------|-------|
| Date/Time | |
| Status | PLANNED |
| Executor | |

### Changes Made

- 

### Risks Found

- 

### Gaps Found

- 

### Fixes Applied

- 

### Tests Run

| Test | Result |
|------|--------|
| Workspace CRUD | |
| Membership management | |
| Invite flow | |
| User profile | |
| Liquibase migrations | |

### Result

- [ ] All acceptance criteria met
- [ ] Committed

### Commit

| Field | Value |
|-------|-------|
| Hash | |
| Message | |
| Pushed | |

### Next Stage

Stage 5: Roles & Permissions

### Notes

---

## Stage 5: Roles & Permissions

| Field | Value |
|-------|-------|
| Date/Time | |
| Status | PLANNED |
| Executor | |

### Changes Made

- 

### Risks Found

- 

### Gaps Found

- 

### Fixes Applied

- 

### Tests Run

| Test | Result |
|------|--------|
| PermissionResolutionService tests | |
| Channel override tests | |
| @PreAuthorize integration | |
| Default roles seeded | |

### Result

- [ ] All acceptance criteria met
- [ ] Committed

### Commit

| Field | Value |
|-------|-------|
| Hash | |
| Message | |
| Pushed | |

### Next Stage

Stage 6: Channels

### Notes

---

## Stage 6: Channels

| Field | Value |
|-------|-------|
| Date/Time | |
| Status | PLANNED |
| Executor | |

### Changes Made

- 

### Risks Found

- 

### Gaps Found

- 

### Fixes Applied

- 

### Tests Run

| Test | Result |
|------|--------|
| Channel CRUD | |
| Type validation | |
| Visibility enforcement | |
| Channel ordering | |

### Result

- [ ] All acceptance criteria met
- [ ] Committed

### Commit

| Field | Value |
|-------|-------|
| Hash | |
| Message | |
| Pushed | |

### Next Stage

Stage 7: Chat Backend

### Notes

---

## Stage 7: Chat Backend

| Field | Value |
|-------|-------|
| Date/Time | |
| Status | PLANNED |
| Executor | |

### Changes Made

- 

### Risks Found

- 

### Gaps Found

- 

### Fixes Applied

- 

### Tests Run

| Test | Result |
|------|--------|
| Send message | |
| Edit message (revision created) | |
| Delete message (soft delete) | |
| Keyset pagination | |
| Permission checks | |

### Result

- [ ] All acceptance criteria met
- [ ] Committed

### Commit

| Field | Value |
|-------|-------|
| Hash | |
| Message | |
| Pushed | |

### Next Stage

Stage 8: Attachments Backend

### Notes

---

## Stage 8: Attachments Backend

| Field | Value |
|-------|-------|
| Date/Time | |
| Status | PLANNED |
| Executor | |

### Changes Made

- 

### Risks Found

- 

### Gaps Found

- 

### Fixes Applied

- 

### Tests Run

| Test | Result |
|------|--------|
| Upload URL generation | |
| Upload confirmation | |
| Download URL generation | |
| Content-type validation | |
| Size limit validation | |
| MinIO integration | |

### Result

- [ ] All acceptance criteria met
- [ ] Committed

### Commit

| Field | Value |
|-------|-------|
| Hash | |
| Message | |
| Pushed | |

### Next Stage

Stage 9: Frontend Foundation

### Notes

---

## Stage 9: Frontend Foundation

| Field | Value |
|-------|-------|
| Date/Time | |
| Status | PLANNED |
| Executor | |

### Changes Made

- 

### Risks Found

- 

### Gaps Found

- 

### Fixes Applied

- 

### Tests Run

| Test | Result |
|------|--------|
| ng build | |
| Layout renders 5 zones | |
| Auth guard works | |
| OpenAPI client generated | |
| Routing works | |

### Result

- [ ] All acceptance criteria met
- [ ] Committed

### Commit

| Field | Value |
|-------|-------|
| Hash | |
| Message | |
| Pushed | |

### Next Stage

Stage 10: Frontend Workspace/Channels/Chat

### Notes

---

## Stage 10: Frontend Workspace/Channels/Chat

| Field | Value |
|-------|-------|
| Date/Time | |
| Status | PLANNED |
| Executor | |

### Changes Made

- 

### Risks Found

- 

### Gaps Found

- 

### Fixes Applied

- 

### Tests Run

| Test | Result |
|------|--------|
| Workspace selector | |
| Channel list | |
| Chat timeline | |
| Message send/edit/delete | |
| Attachment upload | |
| History pagination | |

### Result

- [ ] All acceptance criteria met
- [ ] Committed

### Commit

| Field | Value |
|-------|-------|
| Hash | |
| Message | |
| Pushed | |

### Next Stage

Stage 11: WebSocket Realtime Foundation

### Notes

---

## Stage 11: WebSocket Realtime Foundation

| Field | Value |
|-------|-------|
| Date/Time | |
| Status | PLANNED |
| Executor | |

### Changes Made

- 

### Risks Found

- 

### Gaps Found

- 

### Fixes Applied

- 

### Tests Run

| Test | Result |
|------|--------|
| WS connection with JWT | |
| WS connection without JWT rejected | |
| Protocol envelope parsing | |
| Dispatcher routing | |
| Redis connection registry | |

### Result

- [ ] All acceptance criteria met
- [ ] Committed

### Commit

| Field | Value |
|-------|-------|
| Hash | |
| Message | |
| Pushed | |

### Next Stage

Stage 12: Realtime Chat & Presence Backend

### Notes

---

## Stage 12: Realtime Chat & Presence Backend

| Field | Value |
|-------|-------|
| Date/Time | |
| Status | PLANNED |
| Executor | |

### Changes Made

- 

### Risks Found

- 

### Gaps Found

- 

### Fixes Applied

- 

### Tests Run

| Test | Result |
|------|--------|
| WS send_message → persisted | |
| WS send_message → fanout | |
| Channel subscription | |
| Heartbeat → ONLINE | |
| No heartbeat → OFFLINE | |
| Set status | |

### Result

- [ ] All acceptance criteria met
- [ ] Committed

### Commit

| Field | Value |
|-------|-------|
| Hash | |
| Message | |
| Pushed | |

### Next Stage

Stage 13: Frontend Realtime & Presence

### Notes

---

## Stage 13: Frontend Realtime & Presence

| Field | Value |
|-------|-------|
| Date/Time | |
| Status | PLANNED |
| Executor | |

### Changes Made

- 

### Risks Found

- 

### Gaps Found

- 

### Fixes Applied

- 

### Tests Run

| Test | Result |
|------|--------|
| WS connects on login | |
| Realtime message delivery | |
| Presence indicators | |
| Connection status indicator | |
| Auto-reconnect (basic) | |

### Result

- [ ] All acceptance criteria met
- [ ] Committed

### Commit

| Field | Value |
|-------|-------|
| Hash | |
| Message | |
| Pushed | |

### Next Stage

Stage 14: Reconnect

### Notes

---

## Stage 14: Reconnect

| Field | Value |
|-------|-------|
| Date/Time | |
| Status | PLANNED |
| Executor | |

### Changes Made

- 

### Risks Found

- 

### Gaps Found

- 

### Fixes Applied

- 

### Tests Run

| Test | Result |
|------|--------|
| Session init on connect | |
| Reconnect with valid token | |
| Snapshot received | |
| Invalid token rejected | |
| Expired token rejected | |

### Result

- [ ] All acceptance criteria met
- [ ] Committed

### Commit

| Field | Value |
|-------|-------|
| Hash | |
| Message | |
| Pushed | |

### Next Stage

Stage 15: Call Session & Room Control

### Notes

---

## Stage 15: Call Session & Room Control

| Field | Value |
|-------|-------|
| Date/Time | |
| Status | PLANNED |
| Executor | |

### Changes Made

- 

### Risks Found

- 

### Gaps Found

- 

### Fixes Applied

- 

### Tests Run

| Test | Result |
|------|--------|
| Join call → session created | |
| One active call per channel | |
| Leave call → participant removed | |
| Last leave → session ended | |
| Self state update | |
| Call snapshot | |

### Result

- [ ] All acceptance criteria met
- [ ] Committed

### Commit

| Field | Value |
|-------|-------|
| Hash | |
| Message | |
| Pushed | |

### Next Stage

Stage 16: Media & Moderation Backend

### Notes

---

## Stage 16: Media & Moderation Backend

| Field | Value |
|-------|-------|
| Date/Time | |
| Status | PLANNED |
| Executor | |

### Changes Made

- 

### Risks Found

- 

### Gaps Found

- 

### Fixes Applied

- 

### Tests Run

| Test | Result |
|------|--------|
| coturn starts in docker-compose | |
| TURN credentials endpoint | |
| TURN HMAC credential validation | |
| Force mute → effective state | |
| Kick → participant removed | |
| Block capability | |
| Permission checks for moderation | |
| Effective state computation | |

### Result

- [ ] All acceptance criteria met
- [ ] Committed

### Commit

| Field | Value |
|-------|-------|
| Hash | |
| Message | |
| Pushed | |

### Next Stage

Stage 17: Frontend Call & Moderation

### Notes

---

## Stage 17: Frontend Call & Moderation

| Field | Value |
|-------|-------|
| Date/Time | |
| Status | PLANNED |
| Executor | |

### Changes Made

- 

### Risks Found

- 

### Gaps Found

- 

### Fixes Applied

- 

### Tests Run

| Test | Result |
|------|--------|
| Join/leave call UI | |
| Mute toggle | |
| Video toggle | |
| Participant list | |
| Moderation controls visibility | |
| Force mute/kick UI | |
| Device selection | |

### Result

- [ ] All acceptance criteria met
- [ ] Committed

### Commit

| Field | Value |
|-------|-------|
| Hash | |
| Message | |
| Pushed | |

### Next Stage

Stage 18: Audit

### Notes

---

## Stage 18: Audit

| Field | Value |
|-------|-------|
| Date/Time | |
| Status | PLANNED |
| Executor | |

### Changes Made

- 

### Risks Found

- 

### Gaps Found

- 

### Fixes Applied

- 

### Tests Run

| Test | Result |
|------|--------|
| Moderation → audit event | |
| Call lifecycle → audit events | |
| Role changes → audit events | |
| Audit query endpoint | |
| Frontend audit panel | |

### Result

- [ ] All acceptance criteria met
- [ ] Committed

### Commit

| Field | Value |
|-------|-------|
| Hash | |
| Message | |
| Pushed | |

### Next Stage

Stage 19: Hardening & Completeness

### Notes

---

## Stage 19: Hardening & Completeness

| Field | Value |
|-------|-------|
| Date/Time | |
| Status | PLANNED |
| Executor | |

### Changes Made

- 

### Risks Found

- 

### Gaps Found

- 

### Fixes Applied

- 

### Tests Run

| Test | Result |
|------|--------|
| Idempotency tests | |
| Rate limiting | |
| All 12 permissions tested | |
| Channel overrides tested | |
| i18n completeness | |

### Result

- [ ] All acceptance criteria met
- [ ] Committed

### Commit

| Field | Value |
|-------|-------|
| Hash | |
| Message | |
| Pushed | |

### Next Stage

Stage 20: Gap Audit & Cleanup

### Notes

---

## Stage 20: Gap Audit & Cleanup

| Field | Value |
|-------|-------|
| Date/Time | |
| Status | PLANNED |
| Executor | |

### Changes Made

- 

### Risks Found

- 

### Gaps Found

- 

### Fixes Applied

- 

### Tests Run

| Test | Result |
|------|--------|
| Full backend test suite (mvn clean verify) | |
| Full frontend build (ng build --production) | |
| Cross-flow consistency | |
| Full user journey | |

### Result

- [ ] All acceptance criteria met
- [ ] Committed

### Commit

| Field | Value |
|-------|-------|
| Hash | |
| Message | |
| Pushed | |

### Final Status

| Field | Value |
|-------|-------|
| MVP Complete | YES / NO |
| Total Stages Completed | /20 |
| Remaining Gaps | |
| Known Limitations | |

### Notes

---

## Appendix: Issue Tracker

Use this section to track issues found during implementation that need attention but don't block the current stage.

| # | Stage Found | Description | Severity | Affects Stage | Status | Resolution |
|---|------------|-------------|----------|--------------|--------|------------|
| 1 | | | | | OPEN | |
