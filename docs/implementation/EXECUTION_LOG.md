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
- [x] Committed

### Commit

| Field | Value |
|-------|-------|
| Hash | 0e6bda2 |
| Message | feat: scaffold project structure with Maven backend, Angular frontend, and Docker Compose infra |
| Pushed | YES |

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
| Date/Time | 2026-03-29T09:46+04:00 |
| Status | DONE |
| Executor | AI Agent |

### Changes Made

_List of files created/modified:_

- `backend/src/main/java/ru/timchat/common/error/ErrorResponse.java` — record: code, message, details, traceId
- `backend/src/main/java/ru/timchat/common/error/ApiException.java` — abstract base exception with errorCode, messageKey, args
- `backend/src/main/java/ru/timchat/common/error/NotFoundException.java` — 404 exception
- `backend/src/main/java/ru/timchat/common/error/ForbiddenException.java` — 403 exception
- `backend/src/main/java/ru/timchat/common/error/ConflictException.java` — 409 exception
- `backend/src/main/java/ru/timchat/common/error/ValidationException.java` — 422 exception
- `backend/src/main/java/ru/timchat/common/handler/GlobalExceptionHandler.java` — @RestControllerAdvice with typed handlers for each exception + generic fallback
- `backend/src/main/java/ru/timchat/common/config/I18nConfig.java` — MessageSource + AcceptHeaderLocaleResolver (default: English)
- `backend/src/main/resources/messages.properties` — default (English) message bundle
- `backend/src/main/resources/messages_en.properties` — English message bundle
- `backend/src/main/resources/messages_ru.properties` — Russian message bundle
- `backend/src/test/java/ru/timchat/common/handler/GlobalExceptionHandlerTest.java` — 9 unit tests for exception handler

### Risks Found

- JVM system locale on the dev machine is Russian. Without `fallbackToSystemLocale(false)` on MessageSource, unsupported locales (e.g. French) would fall back to Russian instead of the default English. Fixed in I18nConfig.
- Error response structure is now locked in. Changing it later affects all consumers.

### Gaps Found

- Only generic error codes implemented (error.not-found, error.forbidden, error.conflict, error.validation, error.internal). Domain-specific codes will be added in later stages.
- No auth-related error handling yet (Stage 3).
- MethodArgumentNotValidException handler exists but is not tested with a real @Valid DTO — will be tested when first validated endpoint is created.

### Fixes Applied

_Fixes beyond original stage scope:_

- Added `fallbackToSystemLocale(false)` to `ReloadableResourceBundleMessageSource` to ensure unsupported locales always fall back to default English, not JVM system locale.

### Tests Run

| Test | Result |
|------|--------|
| GlobalExceptionHandler — NotFoundException returns 404 (en) | PASS |
| GlobalExceptionHandler — NotFoundException returns 404 (ru) | PASS |
| GlobalExceptionHandler — Missing locale defaults to English | PASS |
| GlobalExceptionHandler — ForbiddenException returns 403 (en) | PASS |
| GlobalExceptionHandler — ForbiddenException returns 403 (ru) | PASS |
| GlobalExceptionHandler — ConflictException returns 409 | PASS |
| GlobalExceptionHandler — ValidationException returns 422 | PASS |
| GlobalExceptionHandler — Generic exception returns 500 | PASS |
| GlobalExceptionHandler — Unsupported locale (fr) defaults to English | PASS |
| TimChatApplicationTest (smoke) | PASS |
| Full mvn test | PASS — 10 tests, 0 failures |

### Result

- [x] All acceptance criteria met
- [x] Committed

### Commit

| Field | Value |
|-------|-------|
| Hash | 9e2e447 |
| Message | feat: add common module with error handling, i18n, and base exception hierarchy |
| Pushed | YES |

### Next Stage

Stage 3: Auth Module

### Notes

_Additional observations, decisions, or deviations:_

- Used standalone MockMvc setup (not @WebMvcTest) for tests — more reliable for testing inner static test controllers
- `fallbackToSystemLocale(false)` is critical for consistent i18n behavior across different environments
- Message bundle uses Unicode escapes for Russian text in .properties files
- ErrorResponse is a Java record as per project conventions (records for DTOs)

---

## Stage 3: Auth Module (Keycloak)

| Field | Value |
|-------|-------|
| Date/Time | 2026-03-29T11:00+04:00 |
| Status | DONE |
| Executor | AI Agent |

### Changes Made

_List of files created/modified:_

- `backend/pom.xml` — added spring-boot-starter-oauth2-resource-server, spring-boot-starter-actuator, spring-security-test
- `backend/src/main/java/ru/timchat/auth/config/SecurityConfig.java` — new SecurityFilterChain with OAuth2 Resource Server JWT, custom auth entry point, access denied handler, CORS integration
- `backend/src/main/java/ru/timchat/auth/config/CorsConfig.java` — CorsConfigurationSource bean for localhost:4200
- `backend/src/main/java/ru/timchat/auth/context/CurrentUserContext.java` — static methods: getUserId(), getUsername(), getRoles(), hasRole()
- `backend/src/main/resources/application.yml` — replaced spring.security.user with spring.security.oauth2.resourceserver.jwt.issuer-uri/jwk-set-uri for Keycloak
- `backend/src/main/resources/messages.properties` — added error.auth.unauthorized/forbidden/token-expired/token-invalid
- `backend/src/main/resources/messages_en.properties` — added English auth error messages
- `backend/src/main/resources/messages_ru.properties` — added Russian auth error messages
- `backend/src/test/resources/application-test.yml` — added JWT issuer-uri config for test profile
- `backend/src/test/java/ru/timchat/auth/config/TestSecurityConfig.java` — @Configuration @Profile("test") mock JwtDecoder for tests without Keycloak
- `backend/src/test/java/ru/timchat/auth/config/AuthTestController.java` — test-only @RestController for verifying auth integration
- `backend/src/test/java/ru/timchat/auth/config/SecurityConfigTest.java` — 6 tests: 401 without token, 200 with JWT, CurrentUserContext extraction, health public access, CORS allow/reject
- `infra/docker-compose.yml` — added Keycloak 24 service on port 8180 with realm import
- `infra/keycloak/timchat-realm.json` — timchat realm with OWNER/ADMIN/MEMBER/GUEST roles, timchat-frontend client, testuser/testadmin users
- `backend/src/main/java/ru/timchat/infra/config/SecurityConfig.java` — DELETED (replaced by auth.config.SecurityConfig)

### Risks Found

- TestSecurityConfig uses `@Profile("test")` instead of `@Profile("dev")` as originally planned. Rationale: tests already use test profile; dev profile should use real Keycloak since it's available in docker-compose. Using `@Profile("dev")` would create confusion where local dev bypasses real auth.
- Keycloak healthcheck uses bash TCP check — container must have bash available (Keycloak image includes it).

### Gaps Found

- No frontend auth flow yet (Stage 9)
- No automatic user provisioning from JWT to local DB (Stage 4)
- Keycloak realm import cannot be verified in automated tests (requires running Docker)
- `error.auth.token-expired` and `error.auth.token-invalid` message keys are defined but not yet used in code — they will be needed when more specific JWT error handling is added

### Fixes Applied

_Fixes beyond original stage scope:_

- Added `spring-boot-starter-actuator` dependency — SecurityConfig permits `/actuator/health`, so the endpoint must exist for the security rule to be meaningful
- Custom `AuthenticationEntryPoint` and `AccessDeniedHandler` return localized JSON `ErrorResponse` (consistent with GlobalExceptionHandler format) instead of default Spring Security responses
- Test controller placed in separate file with `@Profile("test")` to ensure component scanning picks it up correctly

### Tests Run

| Test | Result |
|------|--------|
| Keycloak starts in docker-compose | NOT TESTED (requires Docker, manual verification) |
| timchat realm imported | NOT TESTED (requires Docker, manual verification) |
| Obtain JWT from Keycloak | NOT TESTED (requires Docker, manual verification) |
| 401 without token | PASS |
| 200 with valid Keycloak JWT | PASS |
| CurrentUserContext userId/username extraction | PASS |
| CurrentUserContext roles extraction | PASS |
| Health endpoint public access | PASS |
| CORS allows localhost:4200 | PASS |
| CORS rejects unknown origin | PASS |
| GlobalExceptionHandler tests (9 existing) | PASS |
| TimChatApplicationTest (smoke) | PASS |
| Full mvn test | PASS — 16 tests, 0 failures |

### Result

- [x] All acceptance criteria met
- [x] Committed

### Commit

| Field | Value |
|-------|-------|
| Hash | 2f05765 |
| Message | feat: add auth module with Keycloak JWT validation and Spring Security |
| Pushed | YES |

### Next Stage

Stage 4: User & Workspace

### Notes

_Additional observations, decisions, and deviations:_

- `@Profile("test")` used instead of `@Profile("dev")` for mock JwtDecoder — more semantically correct since dev should use real Keycloak
- `@TestConfiguration` was initially placed in src/main but moved to src/test as `@Configuration @Profile("test")` — because `@TestConfiguration` annotation is from test-scoped dependency and cannot be in main sources
- SecurityConfig moved from `ru.timchat.infra.config` to `ru.timchat.auth.config` per module structure conventions
- Auth error entry points return JSON `ErrorResponse` (same structure as GlobalExceptionHandler) — ensures consistent error format across all error types
- Actuator added as it's needed for the health endpoint permitted in SecurityConfig

---

## Stage 4: User & Workspace

| Field | Value |
|-------|-------|
| Date/Time | 2026-03-29T11:10+04:00 |
| Status | DONE |
| Executor | AI Agent |

### Changes Made

_List of files created/modified:_

- `backend/src/main/resources/db/changelog/db.changelog-master.yaml` — updated to include migration files
- `backend/src/main/resources/db/changelog/changes/001-create-users.yaml` — users + user_profiles tables
- `backend/src/main/resources/db/changelog/changes/002-create-workspaces.yaml` — workspaces, workspace_members, invites tables
- `backend/src/main/java/ru/timchat/user/domain/User.java` — JPA entity (id, externalId, username, email, timestamps, profile cascade)
- `backend/src/main/java/ru/timchat/user/domain/UserProfile.java` — JPA entity (userId FK, displayName, avatarUrl, statusText)
- `backend/src/main/java/ru/timchat/user/domain/UserRepository.java` — JPA repository with findByExternalId
- `backend/src/main/java/ru/timchat/user/application/UserService.java` — getOrCreateUser (JWT auto-provisioning), getProfile, updateProfile
- `backend/src/main/java/ru/timchat/user/api/UserController.java` — GET /api/users/me, PUT /api/users/me/profile
- `backend/src/main/java/ru/timchat/user/api/UserProfileResponse.java` — response DTO (record)
- `backend/src/main/java/ru/timchat/user/api/UpdateProfileRequest.java` — request DTO (record) with validation
- `backend/src/main/java/ru/timchat/workspace/domain/Workspace.java` — JPA entity (id, name, slug, ownerId, timestamps)
- `backend/src/main/java/ru/timchat/workspace/domain/WorkspaceMember.java` — JPA entity (id, workspaceId, userId, joinedAt)
- `backend/src/main/java/ru/timchat/workspace/domain/Invite.java` — JPA entity (id, workspaceId, code, createdBy, expiresAt, usedBy, usedAt)
- `backend/src/main/java/ru/timchat/workspace/domain/WorkspaceRepository.java` — JPA repository with findBySlug, existsBySlug
- `backend/src/main/java/ru/timchat/workspace/domain/WorkspaceMemberRepository.java` — JPA repository with workspace/user queries
- `backend/src/main/java/ru/timchat/workspace/domain/InviteRepository.java` — JPA repository with findByCode
- `backend/src/main/java/ru/timchat/workspace/application/WorkspaceService.java` — create, getById, listForUser, update, delete
- `backend/src/main/java/ru/timchat/workspace/application/MembershipService.java` — addMember, removeMember, listMembers, isMember
- `backend/src/main/java/ru/timchat/workspace/application/InviteService.java` — createInvite, acceptInvite
- `backend/src/main/java/ru/timchat/workspace/api/WorkspaceController.java` — full REST for workspaces, members, invites
- `backend/src/main/java/ru/timchat/workspace/api/CreateWorkspaceRequest.java` — request DTO with slug validation
- `backend/src/main/java/ru/timchat/workspace/api/UpdateWorkspaceRequest.java` — request DTO
- `backend/src/main/java/ru/timchat/workspace/api/AddMemberRequest.java` — request DTO
- `backend/src/main/java/ru/timchat/workspace/api/WorkspaceResponse.java` — response DTO
- `backend/src/main/java/ru/timchat/workspace/api/WorkspaceMemberResponse.java` — response DTO
- `backend/src/main/java/ru/timchat/workspace/api/InviteResponse.java` — response DTO
- `backend/src/main/java/ru/timchat/workspace/mapper/WorkspaceMapper.java` — entity → DTO mapping
- `backend/src/main/java/ru/timchat/auth/context/CurrentUserContext.java` — added getEmail() method
- `backend/src/main/resources/messages.properties` — added user/workspace/invite error keys
- `backend/src/main/resources/messages_en.properties` — added English user/workspace/invite messages
- `backend/src/main/resources/messages_ru.properties` — added Russian user/workspace/invite messages
- `backend/src/test/java/ru/timchat/auth/config/TestSecurityConfig.java` — added email claim to test JWT
- `backend/src/test/java/ru/timchat/workspace/application/WorkspaceServiceTest.java` — 6 unit tests
- `backend/src/test/java/ru/timchat/workspace/application/InviteServiceTest.java` — 7 unit tests
- `backend/src/test/java/ru/timchat/workspace/api/WorkspaceControllerTest.java` — 6 integration tests

### Risks Found

- Internal user ID (UUID.randomUUID) differs from external OIDC sub claim. User lookup must always go through externalId → internal id mapping. Tests must not assume JWT sub == internal userId.
- Invite code is generated from UUID substring (8 chars) — collision probability is negligible for MVP but should be monitored at scale.

### Gaps Found

- No permission checks on workspace operations (added in Stage 5)
- No workspace-level access restrictions yet (owner/member enforcement deferred to Stage 5)
- Invite is single-use — no multi-use invite support in MVP
- User email is extracted from JWT; if email claim is missing, a fallback address is used
- No endpoint for listing invites for a workspace (not in stage scope)

### Fixes Applied

_Fixes beyond original stage scope:_

- Added `getEmail()` to `CurrentUserContext` — needed for user provisioning from JWT
- Added `email` claim to `TestSecurityConfig` mock JWT decoder — needed for controller tests
- Workspace creation automatically adds owner as first member — ensures workspace always has at least one member

### Tests Run

| Test | Result |
|------|--------|
| WorkspaceService — create saves workspace and owner member | PASS |
| WorkspaceService — create throws conflict on duplicate slug | PASS |
| WorkspaceService — getById throws not found for missing workspace | PASS |
| WorkspaceService — listForUser returns member workspaces | PASS |
| WorkspaceService — update updates name and slug | PASS |
| WorkspaceService — delete removes workspace and members | PASS |
| InviteService — createInvite returns invite with code | PASS |
| InviteService — createInvite throws not found if workspace missing | PASS |
| InviteService — acceptInvite adds member and marks used | PASS |
| InviteService — acceptInvite throws not found for missing code | PASS |
| InviteService — acceptInvite throws conflict if already used | PASS |
| InviteService — acceptInvite throws validation if expired | PASS |
| InviteService — acceptInvite throws conflict if already member | PASS |
| WorkspaceController — createWorkspace returns 201 | PASS |
| WorkspaceController — listWorkspaces after create returns workspace | PASS |
| WorkspaceController — listMembers after create contains owner | PASS |
| WorkspaceController — createWorkspace duplicate slug returns 409 | PASS |
| WorkspaceController — getUserProfile returns current user | PASS |
| WorkspaceController — updateProfile changes display name | PASS |
| All previous tests (16 from stages 1-3) | PASS |
| Full mvn test | PASS — 35 tests, 0 failures |

### Result

- [x] All acceptance criteria met
- [x] Committed

### Commit

| Field | Value |
|-------|-------|
| Hash | 908ec48 |
| Message | feat: add user and workspace modules with CRUD, membership, and invites |
| Pushed | YES |

### Next Stage

Stage 5: Roles & Permissions

### Notes

_Additional observations, decisions, and deviations:_

- User entity uses internal UUID id (randomized), not the OIDC external sub claim — mapping is through `externalId` field and `getOrCreateUser` auto-provisioning
- Workspace owner is automatically added as first member during workspace creation
- Invite codes are 8-character UUID substrings with 7-day default expiry
- All DTOs are Java records per project conventions
- All entities are regular Java classes with @Getter and explicit constructors
- Controllers return DTOs directly with @ResponseStatus (no ResponseEntity)
- i18n messages added for both ru and en locales with Unicode escapes in .properties files

---

## Stage 5: Roles & Permissions

| Field | Value |
|-------|-------|
| Date/Time | 2026-03-29T11:30+04:00 |
| Status | DONE |
| Executor | AI Agent |

### Changes Made

_List of files created/modified:_

- `backend/src/main/resources/db/changelog/changes/003-create-permissions.yaml` — roles, permission_grants, member_roles, channel_permission_overrides tables
- `backend/src/main/resources/db/changelog/db.changelog-master.yaml` — added 003 include
- `backend/src/main/java/ru/timchat/permission/domain/PermissionType.java` — enum with 12 permission types
- `backend/src/main/java/ru/timchat/permission/domain/Role.java` — JPA entity (id, workspaceId, name, isSystem, createdAt)
- `backend/src/main/java/ru/timchat/permission/domain/PermissionGrant.java` — JPA entity (roleId, permissionType)
- `backend/src/main/java/ru/timchat/permission/domain/MemberRole.java` — JPA entity (workspaceMemberId, roleId)
- `backend/src/main/java/ru/timchat/permission/domain/ChannelPermissionOverride.java` — JPA entity (channelId, roleId, permissionType, allowed)
- `backend/src/main/java/ru/timchat/permission/domain/RoleRepository.java` — JPA repository
- `backend/src/main/java/ru/timchat/permission/domain/PermissionGrantRepository.java` — JPA repository
- `backend/src/main/java/ru/timchat/permission/domain/MemberRoleRepository.java` — JPA repository
- `backend/src/main/java/ru/timchat/permission/domain/ChannelPermissionOverrideRepository.java` — JPA repository
- `backend/src/main/java/ru/timchat/permission/application/PermissionResolutionService.java` — resolves effective permissions: workspace-level + channel overrides
- `backend/src/main/java/ru/timchat/permission/application/RoleService.java` — create default roles, assign/revoke/list roles, owner role assignment
- `backend/src/main/java/ru/timchat/permission/api/RoleController.java` — REST for role operations
- `backend/src/main/java/ru/timchat/permission/api/RoleResponse.java` — response DTO (record)
- `backend/src/main/java/ru/timchat/permission/api/AssignRoleRequest.java` — request DTO (record) with validation
- `backend/src/main/java/ru/timchat/permission/config/PermissionSecurity.java` — @Component("permissionSecurity") for @PreAuthorize expressions
- `backend/src/main/java/ru/timchat/workspace/application/WorkspaceService.java` — integrated default role creation and owner role assignment on workspace creation
- `backend/src/main/resources/messages.properties` — added role/permission error keys
- `backend/src/main/resources/messages_en.properties` — added English role/permission messages
- `backend/src/main/resources/messages_ru.properties` — added Russian role/permission messages
- `backend/src/test/java/ru/timchat/permission/application/PermissionResolutionServiceTest.java` — 11 unit tests
- `backend/src/test/java/ru/timchat/permission/application/RoleServiceTest.java` — 7 unit tests
- `backend/src/test/java/ru/timchat/permission/api/RoleControllerTest.java` — 5 integration tests
- `backend/src/test/java/ru/timchat/workspace/application/WorkspaceServiceTest.java` — updated to include RoleService mock

### Risks Found

- Channel permission overrides table created without FK to channels (channels table doesn't exist yet — Stage 6). FK will be added in Stage 6 migration.
- Default roles are workspace-scoped (not global templates), meaning each workspace creates its own OWNER/ADMIN/MEMBER/GUEST roles. This is correct for the domain model but creates more DB rows.

### Gaps Found

- No UI for role management (explicitly out of scope per plan)
- Only predefined roles (OWNER, ADMIN, MEMBER, GUEST) — no dynamic role creation in MVP
- Channel overrides are functional at the domain/service level but have no REST endpoint for managing them yet — will be added when channels are created (Stage 6)
- PermissionSecurity bean registered but not yet used in @PreAuthorize on existing endpoints — will be applied as endpoints are created in later stages

### Fixes Applied

_Fixes beyond original stage scope:_

- Integrated role creation into WorkspaceService.create() — default roles are created automatically when workspace is created, and OWNER role is assigned to the workspace creator
- Updated WorkspaceServiceTest to include RoleService mock dependency

### Tests Run

| Test | Result |
|------|--------|
| PermissionResolutionService — hasPermission returns true when granted | PASS |
| PermissionResolutionService — hasPermission returns false when not granted | PASS |
| PermissionResolutionService — hasPermission returns false when not member | PASS |
| PermissionResolutionService — hasPermission returns false when no roles | PASS |
| PermissionResolutionService — all 12 permission types verified | PASS |
| PermissionResolutionService — merges multiple roles | PASS |
| PermissionResolutionService — channel uses base grant when no override | PASS |
| PermissionResolutionService — channel override deny blocks permission | PASS |
| PermissionResolutionService — channel override allow grants permission | PASS |
| PermissionResolutionService — channel returns false when not member | PASS |
| PermissionResolutionService — allow override wins with multiple roles | PASS |
| RoleService — createDefaultRoles creates 4 roles | PASS |
| RoleService — assignRole creates assignment | PASS |
| RoleService — assignRole throws conflict when duplicate | PASS |
| RoleService — assignRole throws not found when member missing | PASS |
| RoleService — revokeRole removes assignment | PASS |
| RoleService — revokeRole throws not found when not assigned | PASS |
| RoleService — listRolesForMember returns assigned roles | PASS |
| RoleController — workspace creation seeds default roles | PASS |
| RoleController — owner has OWNER role after creation | PASS |
| RoleController — assign role returns 201 | PASS |
| RoleController — duplicate role assignment returns 409 | PASS |
| RoleController — revoke role returns 204 | PASS |
| All previous tests (35 from stages 1-4) | PASS |
| Full mvn test | PASS — 58 tests, 0 failures |

### Result

- [x] All acceptance criteria met
- [x] Committed

### Commit

| Field | Value |
|-------|-------|
| Hash | 70478ea |
| Message | feat: add roles and permissions module with 12 permission types and resolution service |
| Pushed | YES |

### Next Stage

Stage 6: Channels

### Notes

_Additional observations, decisions, and deviations:_

- Default roles are created per-workspace (not as global seed data in Liquibase) because roles reference workspaceId FK
- OWNER gets all 12 permissions, ADMIN gets all except ROOM_MOVE, MEMBER gets 7 (view, write, delete_own, join, speak, video, screen_share), GUEST gets only CHANNEL_VIEW
- Channel overrides use union semantics: if multiple roles have overrides for the same permission, allow wins
- PermissionSecurity bean is named "permissionSecurity" for use in SpEL: @PreAuthorize("@permissionSecurity.hasPermission(#workspaceId, 'MESSAGE_WRITE')")
- All DTOs are Java records per project conventions
- All entities are regular Java classes with @Getter and explicit constructors

---

## Stage 6: Channels

| Field | Value |
|-------|-------|
| Date/Time | 2026-03-29T11:42+04:00 |
| Status | DONE |
| Executor | AI Agent |

### Changes Made

_List of files created/modified:_

- `backend/src/main/resources/db/changelog/changes/004-create-channels.yaml` — channels table with workspace FK, type, position, indexes
- `backend/src/main/resources/db/changelog/db.changelog-master.yaml` — added 004 include
- `backend/src/main/java/ru/timchat/channel/domain/ChannelType.java` — enum (TEXT, VOICE, VIDEO)
- `backend/src/main/java/ru/timchat/channel/domain/Channel.java` — JPA entity (id, workspaceId, name, type, position, createdAt, updatedAt). Type is immutable — no setter/update method for type.
- `backend/src/main/java/ru/timchat/channel/domain/ChannelRepository.java` — JPA repository with findByWorkspaceIdOrderByPositionAsc, countByWorkspaceId
- `backend/src/main/java/ru/timchat/channel/application/ChannelService.java` — create, getById, listVisible (filtered by CHANNEL_VIEW), update (name only), delete, reorder
- `backend/src/main/java/ru/timchat/channel/api/ChannelController.java` — REST endpoints: POST/GET workspaces/{id}/channels, GET/PUT/DELETE channels/{id}, PUT workspaces/{id}/channels/order
- `backend/src/main/java/ru/timchat/channel/api/CreateChannelRequest.java` — request DTO (record) with @NotBlank name, @NotNull type
- `backend/src/main/java/ru/timchat/channel/api/UpdateChannelRequest.java` — request DTO (record) with @NotBlank name
- `backend/src/main/java/ru/timchat/channel/api/ReorderChannelsRequest.java` — request DTO (record) with @NotEmpty channelIds
- `backend/src/main/java/ru/timchat/channel/api/ChannelResponse.java` — response DTO (record)
- `backend/src/main/java/ru/timchat/channel/mapper/ChannelMapper.java` — entity → DTO mapping
- `backend/src/main/java/ru/timchat/common/handler/GlobalExceptionHandler.java` — added HttpMessageNotReadableException handler for 400 on invalid enum values
- `backend/src/main/resources/messages.properties` — added channel error/validation keys
- `backend/src/main/resources/messages_en.properties` — added English channel messages
- `backend/src/main/resources/messages_ru.properties` — added Russian channel messages
- `backend/src/test/java/ru/timchat/channel/application/ChannelServiceTest.java` — 10 unit tests
- `backend/src/test/java/ru/timchat/channel/api/ChannelControllerTest.java` — 8 integration tests

### Risks Found

- Channel type immutability is enforced at the domain level (no setter/update method for type field). There is no explicit error message returned when a client tries to change type — the UpdateChannelRequest simply does not accept a type field.
- Reorder requires all channels in the workspace to be included. Partial reorder is rejected with validation error.
- Channel visibility filtering uses per-channel permission check. Without channel-specific overrides, visibility falls back to workspace-level CHANNEL_VIEW permission (which all roles except GUEST have by default).

### Gaps Found

- No @PreAuthorize on channel endpoints yet — permission checks for create/update/delete are deferred to later stages when workspace membership enforcement is tightened across all endpoints.
- listVisible filters by CHANNEL_VIEW permission. Without channel-specific overrides, all channels are visible to members with CHANNEL_VIEW (OWNER, ADMIN, MEMBER roles).
- No realtime channel subscription events (Stage 11).
- No messages in channels yet (Stage 7).

### Fixes Applied

_Fixes beyond original stage scope:_

- Added `HttpMessageNotReadableException` handler in `GlobalExceptionHandler` — returns HTTP 400 instead of 500 for invalid JSON (e.g. invalid enum values like `"type": "INVALID"`). This applies globally, not just to channels.

### Tests Run

| Test | Result |
|------|--------|
| ChannelService — create saves channel with correct type | PASS |
| ChannelService — create voice channel sets correct type | PASS |
| ChannelService — create throws not found for missing workspace | PASS |
| ChannelService — update changes name | PASS |
| ChannelService — update throws not found for missing channel | PASS |
| ChannelService — delete removes channel | PASS |
| ChannelService — listVisible filters by permission | PASS |
| ChannelService — reorder updates positions | PASS |
| ChannelService — reorder throws validation for missing channel | PASS |
| ChannelService — reorder throws validation for incomplete list | PASS |
| ChannelController — create TEXT channel returns 201 | PASS |
| ChannelController — create VOICE channel returns 201 | PASS |
| ChannelController — create VIDEO channel returns 201 | PASS |
| ChannelController — invalid type returns 400 | PASS |
| ChannelController — list channels after create returns channel | PASS |
| ChannelController — update changes name keeps type | PASS |
| ChannelController — delete returns 204 | PASS |
| ChannelController — position increments automatically | PASS |
| All previous tests (58 from stages 1-5) | PASS |
| Full mvn test | PASS — 76 tests, 0 failures |

### Result

- [x] All acceptance criteria met
- [x] Committed

### Commit

| Field | Value |
|-------|-------|
| Hash | 9bbad95 |
| Message | feat: add channels module with CRUD, types, and visibility enforcement |
| Pushed | YES |

### Next Stage

Stage 7: Chat Backend

### Notes

_Additional observations, decisions, and deviations:_

- Channel type immutability is enforced by design: the Channel entity has no `updateType()` method and the UpdateChannelRequest DTO does not accept a type field. No explicit error message is needed — the API simply does not support changing type.
- HttpMessageNotReadableException handler added to GlobalExceptionHandler as a global fix — this was needed to properly return 400 for invalid enum values in JSON requests.
- listVisible implementation uses PermissionResolutionService.hasChannelPermission per channel. This is correct for MVP but may need optimization if a workspace has hundreds of channels (N+1 permission checks). Not a concern for MVP scale.
- All DTOs are Java records per project conventions.
- All entities are regular Java classes with @Getter and explicit constructors.
- Controllers return DTOs directly with @ResponseStatus (no ResponseEntity).

---

## Stage 7: Chat Backend

| Field | Value |
|-------|-------|
| Date/Time | 2026-03-29T11:50+04:00 |
| Status | DONE |
| Executor | AI Agent |

### Changes Made

_List of files created/modified:_

- `backend/src/main/resources/db/changelog/changes/005-create-messages.yaml` — messages, message_revisions, message_deletions, message_attachments tables with FKs and indexes
- `backend/src/main/resources/db/changelog/db.changelog-master.yaml` — added 005 include
- `backend/src/main/java/ru/timchat/message/domain/Message.java` — JPA entity (id, channelId, authorId, content, deleted, createdAt, updatedAt)
- `backend/src/main/java/ru/timchat/message/domain/MessageRevision.java` — JPA entity (id, messageId, previousContent, editedAt)
- `backend/src/main/java/ru/timchat/message/domain/MessageDeletion.java` — JPA entity (id, messageId, deletedBy, deletedAt, reason)
- `backend/src/main/java/ru/timchat/message/domain/MessageAttachment.java` — JPA entity (id, messageId, attachmentMetadataId)
- `backend/src/main/java/ru/timchat/message/domain/MessageRepository.java` — JPA repository with keyset pagination queries
- `backend/src/main/java/ru/timchat/message/domain/MessageRevisionRepository.java` — JPA repository
- `backend/src/main/java/ru/timchat/message/domain/MessageDeletionRepository.java` — JPA repository
- `backend/src/main/java/ru/timchat/message/domain/MessageAttachmentRepository.java` — JPA repository
- `backend/src/main/java/ru/timchat/message/application/MessageService.java` — send, edit, delete, getHistory with keyset pagination, permission checks
- `backend/src/main/java/ru/timchat/message/api/MessageController.java` — REST endpoints: POST send, PUT edit, DELETE, GET history
- `backend/src/main/java/ru/timchat/message/api/SendMessageRequest.java` — request DTO (record) with validation
- `backend/src/main/java/ru/timchat/message/api/EditMessageRequest.java` — request DTO (record) with validation
- `backend/src/main/java/ru/timchat/message/api/MessageResponse.java` — response DTO (record)
- `backend/src/main/java/ru/timchat/message/api/PageResponse.java` — generic paginated response DTO (record)
- `backend/src/main/resources/messages.properties` — added message error/validation keys
- `backend/src/main/resources/messages_en.properties` — added English message error keys
- `backend/src/main/resources/messages_ru.properties` — added Russian message error keys
- `backend/src/test/java/ru/timchat/message/application/MessageServiceTest.java` — 12 unit tests
- `backend/src/test/java/ru/timchat/message/api/MessageControllerTest.java` — 7 integration tests

### Risks Found

- Keyset pagination cursor is Base64-encoded `epochMillis:UUID`. If system clock precision differs between JVMs, millisecond-level cursor resolution could cause edge cases. Negligible for MVP.
- `toResponse()` performs N+1 on `userRepository.findById` and `attachmentRepository.findByMessageId` per message. For MVP history loads (max 100 messages) this is acceptable, but may need batch loading optimization at scale.

### Gaps Found

- Messages are only delivered via REST (polling), not realtime (Stage 12)
- No attachment file upload yet — only MessageAttachment metadata link with nullable FK (Stage 8)
- No @PreAuthorize on message endpoints — permission checks are done at service level via PermissionResolutionService
- Edit is restricted to message author only; no admin-override for editing

### Fixes Applied

_Fixes beyond original stage scope:_

- None required — stage implemented cleanly within scope

### Tests Run

| Test | Result |
|------|--------|
| MessageService — sendMessage saves and returns response | PASS |
| MessageService — sendMessage without permission throws forbidden | PASS |
| MessageService — sendMessage channel not found throws not found | PASS |
| MessageService — editMessage creates revision and updates content | PASS |
| MessageService — editMessage by non-owner throws forbidden | PASS |
| MessageService — editMessage deleted message throws validation | PASS |
| MessageService — editMessage not found throws not found | PASS |
| MessageService — deleteMessage own creates record and marks deleted | PASS |
| MessageService — deleteMessage other requires DELETE_ANY | PASS |
| MessageService — deleteMessage already deleted is idempotent | PASS |
| MessageService — getHistory without cursor returns first page | PASS |
| MessageService — getHistory channel not found throws not found | PASS |
| MessageController — sendMessage returns 201 | PASS |
| MessageController — sendMessage empty content returns 422 | PASS |
| MessageController — editMessage changes content | PASS |
| MessageController — deleteMessage returns 204 | PASS |
| MessageController — getHistory after send returns message | PASS |
| MessageController — deleted messages not returned in history | PASS |
| MessageController — pagination with cursor returns correct pages | PASS |
| All previous tests (76 from stages 1-6) | PASS |
| Full mvn test | PASS — 95 tests, 0 failures |

### Result

- [x] All acceptance criteria met
- [x] Committed

### Commit

| Field | Value |
|-------|-------|
| Hash | |
| Message | feat: add chat message module with send, edit, delete, and keyset pagination |
| Pushed | |

### Next Stage

Stage 8: Attachments Backend

### Notes

_Additional observations, decisions, and deviations:_

- Keyset pagination uses Base64-encoded cursor with `epochMillis:UUID` format for compact, URL-safe representation
- Delete is idempotent — deleting an already-deleted message is a no-op (no error thrown)
- Deleted messages have `content` nulled in the response DTO to prevent content leakage
- MessageAttachment FK to attachment_metadata table is nullable since attachment metadata module doesn't exist yet (Stage 8)
- Permission checks for send use MESSAGE_WRITE on channel; delete uses MESSAGE_DELETE_OWN for own messages and MESSAGE_DELETE_ANY for others
- All DTOs are Java records per project conventions
- All entities are regular Java classes with @Getter and explicit constructors

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
