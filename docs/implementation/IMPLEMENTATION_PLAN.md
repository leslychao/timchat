# TimChat — MVP Implementation Plan

## Meta

| Field | Value |
|-------|-------|
| Status | ACTIVE |
| Created | 2026-03-29 |
| Scope | TimChat MVP — confirmed business scope only |
| Source of truth | `docs/business/TimChat_Business_Features.md`, `docs/adr/TimChat_ADR.md`, `docs/frontend/FRONTEND_UI_DIRECTION.md`, `docs/frontend/FRONTEND_LAYOUT_PRINCIPLES.md`, `.cursor/rules/*` |
| Tracking | `docs/implementation/EXECUTION_LOG.md` |

---

## A. Source-of-Truth Summary

### A.1. Confirmed business scope

TimChat — командный realtime-продукт для общения и совместной работы внутри рабочих пространств.

MVP включает ровно эти блоки:

1. **Workspaces / Channels** — структурированные рабочие пространства с каналами (text, voice, video).
2. **Chat** — send, receive realtime, history, edit, delete.
3. **Attachments** — metadata + controlled object storage (S3-compatible).
4. **Presence** — online / offline / idle / dnd.
5. **Calls / Room-control** — join, leave, self-state, room snapshots, one active call per channel.
6. **Moderation** — mute, kick, block capabilities, server-side enforcement.
7. **Permissions / Roles** — 12 permission types, role-based access, channel-level overrides.
8. **Reconnect** — snapshot-based only.
9. **Audit** — moderation actions, significant room events, access changes.

Auth: OAuth2/OIDC + JWT via Keycloak.
File storage: S3-compatible object storage (MinIO for local dev).
NAT traversal: STUN/TURN via coturn.

### A.2. Confirmed architecture invariants

1. Backend не передаёт media (RTP). Backend = control plane only.
2. SFU не является источником business truth.
3. PostgreSQL — durable truth.
4. Redis — transient truth only.
5. Клиент не пишет authoritative state. Клиент request, сервер decide.
6. Каждая команда (REST и WS) авторизуется на сервере. Handshake auth недостаточен.
7. Reconnect восстанавливает состояние через authoritative snapshot, не через replay.
8. Chat и call-control — отдельные домены.
9. Бинарные данные живут в object storage, не в PostgreSQL.
10. `CallSession` и media transport — разные сущности.
11. В MVP один канал имеет максимум одну активную call session.
12. Effective participant state вычисляет сервер из self-state + server constraints + moderation.

### A.3. Technology stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21, Spring Boot 3.3.5, Maven multi-module |
| Frontend | Angular (standalone components), `ng serve` for local dev |
| DB | PostgreSQL (Liquibase migrations) |
| Cache/Live state | Redis |
| Realtime | Raw WebSocket (typed protocol) |
| Auth | OAuth2/OIDC + JWT via Keycloak, oauth2-proxy at edge |
| File storage | S3-compatible (MinIO for local dev) |
| Media | SFU (adapter interface; concrete SFU not fixed) |
| NAT traversal | STUN/TURN via coturn |

### A.4. Frontend design constraints

- Cursor-inspired design language: light-theme-first, minimalistic, dense, professional, desktop-first, editor-like.
- Layout zones: left navigation rail, secondary sidebar (channels), main central area (chat/room), right contextual panel (members/presence/moderation/audit), compact top bar.
- No oversized cards, excessive whitespace, decorative gradients, mobile-first composition.
- Priorities: fast scanning, high information density, predictable interaction zones, keyboard-friendly, low-noise state presentation.
- Feature-oriented architecture: `core/`, `shared/`, `features/`.
- Key stores/services: `AuthStore`, `WorkspaceStore`, `ChannelStore`, `ChatStore`, `PresenceStore`, `CallStore`, `RealtimeGateway`, `RtcMediaService`, `DeviceService`, `AttachmentUploadService`.

### A.5. Hard "not now" list

Не реализуем, не проектируем, не предлагаем:

- E2EE
- Breakout rooms
- Несколько параллельных call sessions в одном channel
- Replay-based reconnect recovery
- Server-side recording / transcription
- Отдельный signaling service
- Microservices split
- STOMP
- Generic plugin system for room types
- Binary protocol вместо JSON
- Сложная CQRS / read-model архитектура
- Kafka / event-streaming для internal fanout
- Kubernetes / Helm / Terraform / Nomad / Swarm

### A.6. Confirmed decisions and resolved gaps

Below items were either confirmed by the project owner or resolved with minimal-sufficient defaults.

1. **OIDC provider: Keycloak (confirmed).** Keycloak is used for authentication in all environments. Local dev runs Keycloak in docker-compose (`infra/`). Backend validates JWT tokens issued by Keycloak via Spring Security OAuth2 Resource Server. A `@Profile("dev")` security config with a mock JWT decoder is acceptable **only** for running backend unit/integration tests without Keycloak; it must never be used as a production path.
2. **SFU: interface-only adapter (confirmed).** Backend defines an `SfuAdapter` interface. No concrete SFU (LiveKit/Janus/Mediasoup) is integrated in MVP. The adapter remains a stub. Concrete SFU choice is deferred to post-MVP media integration phase.
3. **Object storage: MinIO (confirmed for local dev).** `infra/docker-compose.yml` includes MinIO. Backend uses AWS SDK v2 S3 client, which is compatible with both MinIO and any S3-compatible production storage.
4. **Angular component library: none (confirmed).** No Angular Material, PrimeNG, or other component libraries. All components are standalone Angular components with custom SCSS, styled in Cursor-inspired aesthetic (light-theme-first, dense, professional). CSS custom properties are used for the design token system.
5. **OpenAPI contract pipeline (confirmed).** Backend: springdoc-openapi generates `/v3/api-docs`. Frontend: `@openapitools/openapi-generator-cli` (typescript-angular generator) produces a typed Angular HTTP client from the spec. The generated client is the single source of truth for all frontend API calls.
6. **TURN server for local dev: coturn (confirmed).** `infra/docker-compose.yml` includes a coturn container for local TURN/STUN. `TurnCredentialService` generates time-limited HMAC credentials compatible with coturn's REST API.
7. **`ROOM_MOVE` permission type: defined but not enforced in MVP (confirmed).** The `PermissionType` enum includes `ROOM_MOVE`, but since MVP has only one active call per channel and no breakout rooms, there is no move target. The permission is defined for forward-compatibility but has no enforcement logic in MVP.
8. **Numbered cursor rules (01_scope.mdc ... 10_no_overengineering.mdc):** These files do not exist in the repository. Equivalent content lives in always-applied workspace rules with descriptive names (`timchat-product-scope.mdc`, `timchat-architectural-invariants.mdc`, `timchat-domain-and-modules.mdc`, `timchat-realtime-and-data.mdc`, `timchat-test-strategy.mdc`, `timchat-frontend.mdc`, etc.).

---

## B. Delivery Strategy

### B.1. Why this stage order

Порядок стадий определяется тремя принципами:

1. **Bottom-up foundation:** нельзя строить бизнес-логику без скелета проекта, common-модулей и auth.
2. **Backend-first per feature:** REST API должен быть готов до того, как frontend начнёт его потреблять. Frontend зависит от сгенерированного OpenAPI-клиента.
3. **Business priority alignment:** порядок совпадает с приоритетами из бизнес-документа: (1) workspace/channels/permissions/chat/attachments → (2) presence/realtime/reconnect → (3) calls/moderation → (4) audit/hardening.

### B.2. Dependencies between stages

```
S1 (Scaffolding) ──────► S2 (Common) ──► S3 (Auth) ──► S4 (User/Workspace)
                                                            │
                                                            ▼
                                                       S5 (Permissions) ──► S6 (Channels)
                                                                               │
                                                           ┌───────────────────┤
                                                           ▼                   ▼
                                                      S7 (Chat)          S11 (WS Foundation)
                                                           │                   │
                                                           ▼                   ▼
                                                      S8 (Attachments)   S12 (Realtime Chat+Presence)
                                                           │                   │
                                                           ▼               ┌───┤
                                                      S9 (FE Foundation)   │   ▼
                                                           │               │  S15 (Call Session)
                                                           ▼               │   │
                                                      S10 (FE WS/CH/Chat)  │   ▼
                                                           │               │  S16 (Media+Moderation)
                                                           ▼               │   │
                                                      S13 (FE Realtime) ◄──┘   ▼
                                                           │              S17 (FE Call+Moderation)
                                                           ▼                   │
                                                      S14 (Reconnect)          ▼
                                                                          S18 (Audit)
                                                                               │
                                                                               ▼
                                                                          S19 (Hardening)
                                                                               │
                                                                               ▼
                                                                          S20 (Gap Audit)
```

### B.3. What must be built first to avoid rework

1. **Common module (error handling, i18n, base types)** — используется всеми модулями. Если сделать позже, придётся переписывать error responses во всех уже написанных контроллерах.
2. **Auth module** — Spring Security filter chain и `CurrentUserContext` нужны до первого контроллера. Без них нельзя тестировать ни один endpoint.
3. **Permission resolution service** — используется всеми доменными модулями (chat, channels, calls, moderation). Централизованный resolver нужен ДО модулей-потребителей, иначе permission checks расползутся по хэлперам.
4. **WebSocket protocol envelope и dispatcher** — это фундамент для всех realtime handlers. Без него каждый handler будет изобретать свой формат.
5. **Frontend core layout** — navigation rail, sidebar, main area, top bar, right panel. Все feature-компоненты рендерятся внутри этого скелета.

---

## C. Stage Overview Table

| # | Stage Name | Status | Objective | Dependencies | Output / Deliverables | Commit Template |
|---|-----------|--------|-----------|--------------|----------------------|-----------------|
| 1 | Project Scaffolding | DONE | Создать скелет проекта: Maven, Spring Boot, Angular, Docker Compose | — | pom.xml, Spring Boot app, Angular project, docker-compose.yml, Liquibase config | `feat: scaffold project structure` |
| 2 | Common Module & Error Handling | DONE | Базовые типы, global error handling, i18n | S1 | common package, exception handler, error DTO, message bundles | `feat: add common module with error handling and i18n` |
| 3 | Auth Module | DONE | Keycloak JWT auth, Spring Security, CurrentUserContext | S2 | auth module, security config, Keycloak in docker-compose, CurrentUserContext | `feat: add auth module with Keycloak JWT validation and Spring Security` |
| 4 | User & Workspace | DONE | User/profile, workspace CRUD, membership | S3 | entities, repos, services, REST controllers, migrations | `feat: add user and workspace modules` |
| 5 | Roles & Permissions | DONE | Role/permission model, resolution service, channel overrides | S4 | permission module, 12 permission types, resolver, migrations | `feat: add roles and permissions module` |
| 6 | Channels | DONE | Channel CRUD, type enforcement, visibility | S5 | channel module, REST, visibility enforcement, migrations | `feat: add channels module with CRUD, types, and visibility enforcement` |
| 7 | Chat Backend | DONE | Send/edit/delete messages, history pagination | S6 | message module, REST, keyset pagination, migrations | `feat: add chat message module` |
| 8 | Attachments Backend | DONE | Attachment metadata, S3 upload/download auth, signed URLs | S7 | attachment module, S3 adapter, REST, migrations | `feat: add attachments module` |
| 9 | Frontend Foundation | PLANNED | Angular shell, layout, auth, OpenAPI client, routing | S8 | Angular project, layout shell, auth flow, API client | `feat: scaffold frontend with layout and auth` |
| 10 | Frontend Workspace/Channels/Chat | PLANNED | Workspace selector, channel list, chat timeline, message compose | S9 | workspace/channel/chat feature modules, stores | `feat: add workspace, channels, and chat UI` |
| 11 | WebSocket Realtime Foundation | PLANNED | WS transport, protocol, dispatcher, typed handlers, registry | S6 | realtime module, protocol model, dispatcher, registry | `feat: add WebSocket realtime foundation` |
| 12 | Realtime Chat & Presence Backend | PLANNED | Chat fanout via WS, presence model, heartbeat | S11 | ChatCommandHandler, PresenceCommandHandler, Redis projections | `feat: add realtime chat delivery and presence` |
| 13 | Frontend Realtime & Presence | PLANNED | RealtimeGateway, WS client, chat updates, presence indicators | S10, S12 | RealtimeGateway, presence UI, live chat | `feat: integrate frontend realtime and presence` |
| 14 | Reconnect | PLANNED | Snapshot-based reconnect, resume_session | S13 | reconnect flow, ReconnectToken, snapshot assembly | `feat: add snapshot-based reconnect` |
| 15 | Call Session & Room Control | PLANNED | CallSession lifecycle, join/leave, self/effective state, snapshots | S12 | call module, CallCommandHandler, Redis projections, migrations | `feat: add call session and room control` |
| 16 | Media & Moderation Backend | PLANNED | TURN credentials (coturn), SFU adapter, moderation enforcement | S15 | media module, moderation module, coturn in docker-compose, effective state computation | `feat: add media integration and moderation` |
| 17 | Frontend Call & Moderation | PLANNED | Call panel, self-state controls, WebRTC, moderation UI | S16 | CallStore, RtcMediaService, call UI, moderation controls | `feat: add call and moderation UI` |
| 18 | Audit | PLANNED | AuditEvent model, audit service, REST, frontend panel | S17 | audit module, REST endpoint, frontend panel, migrations | `feat: add audit module` |
| 19 | Hardening & Completeness | PLANNED | Idempotency, rate limiting, i18n completeness, permission enforcement | S18 | hardened error paths, complete permission checks, rate limits | `fix: harden error handling, permissions, and idempotency` |
| 20 | Gap Audit & Cleanup | PLANNED | Cross-flow verification, dead code cleanup, final review | S19 | clean codebase, verified cross-flow consistency | `chore: final gap audit and cleanup` |

---

## D. Stage Details

---

### Stage 1: Project Scaffolding

#### 1. Objective

Создать полный скелет проекта: Maven multi-module backend, Angular frontend, Docker Compose для инфраструктуры, Liquibase конфигурацию.

#### 2. In-scope work

- Maven parent pom.xml (Java 21, Spring Boot 3.3.5, dependency management)
- Backend Maven module с Spring Boot application class `ru.timchat.TimChatApplication`
- Angular CLI project в `frontend/` (standalone components, SCSS)
- Docker Compose в `infra/` (PostgreSQL 16, Redis 7, MinIO)
- Liquibase config + пустой master changelog
- `application.yml` для local dev (datasource, redis, minio)
- `.gitignore` обновление (node_modules, target, dist)

#### 3. Explicit out-of-scope

- Бизнес-логика любого модуля
- Keycloak в docker-compose (будет добавлен в Stage 3)
- Тесты (нечего тестировать)
- Frontend компоненты

#### 4. Backend work

- `pom.xml` (parent): groupId `ru.timchat`, Java 21, Spring Boot 3.3.5 starter parent, modules declaration
- `backend/pom.xml`: spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-data-redis, spring-boot-starter-websocket, spring-boot-starter-security, spring-boot-starter-validation, liquibase-core, postgresql driver, lombok, springdoc-openapi
- `backend/src/main/java/ru/timchat/TimChatApplication.java`
- `backend/src/main/resources/application.yml` (datasource, redis, liquibase, server port)
- `backend/src/main/resources/db/changelog/db.changelog-master.yaml`

#### 5. Frontend work

- `ng new timchat-frontend --directory frontend --style scss --routing --standalone` (или equivalent manual setup)
- Verify `angular.json`, `tsconfig.json`, `package.json` exist
- Remove default boilerplate content

#### 6. Infra work

- `infra/docker-compose.yml`: PostgreSQL 16, Redis 7, MinIO (with health checks, volume mounts, port mappings)
- `.env.example` for infra credentials

#### 7. Test work

- Backend: verify application context loads (`@SpringBootTest` smoke test)
- Frontend: verify `ng build` succeeds
- Infra: verify `docker-compose up` starts all services

#### 8. Main risks

- Dependency version conflicts in Maven
- Angular CLI version mismatch
- Docker Compose port conflicts on developer machine

#### 9. Expected implementation gaps

- No business logic
- No auth — application is wide open
- No API endpoints

#### 10. What must be fixed before done

- `mvn clean compile` succeeds
- `ng build` succeeds
- `docker-compose up -d` starts PostgreSQL, Redis, MinIO without errors
- Spring Boot connects to PostgreSQL and Redis on startup
- Liquibase runs empty changelog without error

#### 11. Acceptance criteria

- [ ] Maven project compiles without errors
- [ ] Spring Boot application starts and connects to PostgreSQL and Redis
- [ ] Liquibase executes master changelog (empty, no error)
- [ ] Angular project builds without errors
- [ ] Docker Compose starts all 3 services with health checks passing

#### 12. Manual verification checklist

- [ ] `cd infra && docker-compose up -d` — all containers running
- [ ] `cd backend && mvn clean compile` — BUILD SUCCESS
- [ ] `cd backend && mvn spring-boot:run` — application starts, no stacktrace
- [ ] `cd frontend && npm install && ng build` — build succeeds
- [ ] Access MinIO console at localhost:9001

#### 13. Files/modules likely to change

```
pom.xml
backend/pom.xml
backend/src/main/java/ru/timchat/TimChatApplication.java
backend/src/main/resources/application.yml
backend/src/main/resources/application-dev.yml
backend/src/main/resources/db/changelog/db.changelog-master.yaml
backend/src/test/java/ru/timchat/TimChatApplicationTest.java
infra/docker-compose.yml
infra/.env.example
frontend/angular.json
frontend/package.json
frontend/tsconfig.json
frontend/src/main.ts
frontend/src/app/app.component.ts
frontend/src/app/app.routes.ts
.gitignore
```

#### 14. Suggested commit message

```
feat: scaffold project structure with Maven backend, Angular frontend, and Docker Compose infra
```

#### 15. Exact stage implementation prompt

```
Read and follow all cursor rules from .cursor/rules/.

Read the implementation plan: docs/implementation/IMPLEMENTATION_PLAN.md — execute Stage 1 only.

Create the project scaffolding for TimChat MVP:

BACKEND:
1. Create parent pom.xml at project root:
   - groupId: ru.timchat, artifactId: timchat, packaging: pom
   - Java 21, Spring Boot 3.3.5 (spring-boot-starter-parent)
   - modules: backend
2. Create backend/pom.xml:
   - Dependencies: spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-data-redis, spring-boot-starter-websocket, spring-boot-starter-security, spring-boot-starter-validation, liquibase-core, postgresql, lombok, springdoc-openapi-starter-webmvc-ui
   - Do NOT add test dependencies beyond spring-boot-starter-test
3. Create backend/src/main/java/ru/timchat/TimChatApplication.java — standard @SpringBootApplication
4. Create backend/src/main/resources/application.yml:
   - server.port: 8080
   - spring.datasource: PostgreSQL on localhost:5432/timchat
   - spring.data.redis: localhost:6379
   - spring.liquibase.change-log: classpath:db/changelog/db.changelog-master.yaml
   - spring.security: permit-all for now (will be configured in Stage 3)
5. Create backend/src/main/resources/db/changelog/db.changelog-master.yaml — empty databaseChangeLog
6. Create backend/src/test/java/ru/timchat/TimChatApplicationTest.java — @SpringBootTest smoke test

FRONTEND:
7. Initialize Angular project in frontend/ directory:
   - Use Angular CLI or create manually
   - Standalone components, SCSS, routing enabled
   - Remove default boilerplate content from app.component

INFRA:
8. Create infra/docker-compose.yml:
   - PostgreSQL 16: port 5432, db: timchat, user: timchat, password: timchat
   - Redis 7: port 6379
   - MinIO: port 9000 (API), 9001 (console), root user: minioadmin/minioadmin
   - Health checks for all services
   - Named volumes for persistence
9. Create infra/.env.example with default values

OTHER:
10. Update .gitignore to cover: node_modules, target, dist, .angular, *.class, *.jar, *.log

Do NOT add any business logic, auth configuration, or domain modules.
Verify that mvn clean compile succeeds and the project structure is correct.

After completing, update docs/implementation/EXECUTION_LOG.md with Stage 1 results.
```

---

### Stage 2: Common Module & Error Handling

#### 1. Objective

Создать common-пакет с базовыми типами, global error handling и i18n-инфраструктурой, которые будут использоваться всеми последующими модулями.

#### 2. In-scope work

- Common error types и error codes
- Error response DTO (code + localized message + details + traceId)
- `@RestControllerAdvice` global exception handler
- Spring `MessageSource` i18n setup
- Message bundles: `messages.properties`, `messages_ru.properties`, `messages_en.properties`
- Base exception classes
- Common ID types if needed

#### 3. Explicit out-of-scope

- Domain entities
- Business validation rules
- Auth-related errors (Stage 3)
- Domain-specific error codes (added in respective stages)

#### 4. Backend work

- `ru.timchat.common.error.ErrorCode` — enum or constants for stable error codes
- `ru.timchat.common.error.ErrorResponse` — record: `code`, `message`, `details`, `traceId`
- `ru.timchat.common.error.ApiException` — base runtime exception with error code + message key + args
- `ru.timchat.common.error.NotFoundException` extends `ApiException`
- `ru.timchat.common.error.ForbiddenException` extends `ApiException`
- `ru.timchat.common.error.ConflictException` extends `ApiException`
- `ru.timchat.common.error.ValidationException` extends `ApiException`
- `ru.timchat.common.handler.GlobalExceptionHandler` — `@RestControllerAdvice`, resolves localized messages via `MessageSource`
- `ru.timchat.common.config.I18nConfig` — `MessageSource` bean, `LocaleResolver` bean
- Message bundle files with initial keys (generic errors)

#### 5. Frontend work

None.

#### 6. Infra work

None.

#### 7. Test work

- Unit tests for `GlobalExceptionHandler` — verify error responses are correctly formed
- Verify i18n resolution for `ru` and `en` locales

#### 8. Main risks

- i18n locale resolution from `Accept-Language` header may need careful configuration
- Error response structure must be established now — changing it later affects all consumers

#### 9. Expected implementation gaps

- Only generic error codes; domain-specific codes added in later stages
- No auth-related error handling yet

#### 10. What must be fixed before done

- `GlobalExceptionHandler` correctly catches all `ApiException` subclasses
- Error responses contain `code`, localized `message`, `traceId`
- Both `ru` and `en` locales produce correct messages

#### 11. Acceptance criteria

- [ ] `ApiException` hierarchy compiles and is usable
- [ ] `GlobalExceptionHandler` returns correct JSON error response
- [ ] `Accept-Language: ru` returns Russian error message
- [ ] `Accept-Language: en` returns English error message
- [ ] Missing locale falls back to default
- [ ] `ErrorResponse` record contains: code, message, details, traceId

#### 12. Manual verification checklist

- [ ] Throw `NotFoundException` from a test controller → verify 404 JSON response with localized message
- [ ] Same request with `Accept-Language: ru` vs `en` → different messages
- [ ] Verify `traceId` is populated

#### 13. Files/modules likely to change

```
backend/src/main/java/ru/timchat/common/error/ErrorCode.java
backend/src/main/java/ru/timchat/common/error/ErrorResponse.java
backend/src/main/java/ru/timchat/common/error/ApiException.java
backend/src/main/java/ru/timchat/common/error/NotFoundException.java
backend/src/main/java/ru/timchat/common/error/ForbiddenException.java
backend/src/main/java/ru/timchat/common/error/ConflictException.java
backend/src/main/java/ru/timchat/common/error/ValidationException.java
backend/src/main/java/ru/timchat/common/handler/GlobalExceptionHandler.java
backend/src/main/java/ru/timchat/common/config/I18nConfig.java
backend/src/main/resources/messages.properties
backend/src/main/resources/messages_ru.properties
backend/src/main/resources/messages_en.properties
backend/src/test/java/ru/timchat/common/handler/GlobalExceptionHandlerTest.java
```

#### 14. Suggested commit message

```
feat: add common module with error handling, i18n, and base exception hierarchy
```

#### 15. Exact stage implementation prompt

```
Read and follow all cursor rules from .cursor/rules/.

Read the implementation plan: docs/implementation/IMPLEMENTATION_PLAN.md — execute Stage 2 only.

Create the common module for TimChat backend:

1. Create error hierarchy in ru.timchat.common.error:
   - ErrorResponse record: String code, String message, String details, String traceId
   - ApiException (RuntimeException): errorCode (String), messageKey (String), args (Object[])
   - NotFoundException extends ApiException
   - ForbiddenException extends ApiException
   - ConflictException extends ApiException
   - ValidationException extends ApiException

2. Create GlobalExceptionHandler in ru.timchat.common.handler:
   - @RestControllerAdvice
   - Handle ApiException subclasses: resolve localized message from MessageSource using messageKey + locale from request
   - Handle MethodArgumentNotValidException for validation errors
   - Handle generic Exception as 500 with generic message
   - Generate traceId (UUID) for each error response
   - Map NotFoundException → 404, ForbiddenException → 403, ConflictException → 409, ValidationException → 422

3. Create I18nConfig in ru.timchat.common.config:
   - MessageSource bean pointing to classpath:messages
   - LocaleResolver (AcceptHeaderLocaleResolver, default Locale.ENGLISH)

4. Create message bundles:
   - messages.properties (default = English)
   - messages_ru.properties
   - messages_en.properties
   Initial keys: error.not-found, error.forbidden, error.conflict, error.validation, error.internal

5. Write unit tests for GlobalExceptionHandler verifying correct HTTP status, error code, and locale resolution.

Follow rules:
- Use records for DTOs (ErrorResponse)
- Use @Slf4j logging
- Do not use ResponseEntity — use @ResponseStatus
- Both ru and en message bundles must have semantically equivalent translations

After completing, update docs/implementation/EXECUTION_LOG.md with Stage 2 results.
```

---

### Stage 3: Auth Module

#### 1. Objective

Настроить Spring Security с JWT-валидацией от Keycloak, создать auth-модуль с `CurrentUserContext`, добавить Keycloak в docker-compose с realm-экспортом для local dev.

#### 2. In-scope work

- Spring Security filter chain configuration (OAuth2 Resource Server + JWT)
- JWT token validation against Keycloak
- `CurrentUserContext` — доступ к текущему пользователю из `SecurityContext`
- `@Profile("dev")` security config с mock JWT decoder (только для тестов без Keycloak)
- Keycloak 24 в docker-compose с realm import (`timchat` realm, test users)
- Security-related error handling (401, 403)
- CORS configuration для localhost:4200

#### 3. Explicit out-of-scope

- User registration/management UI
- OAuth2 login flow on frontend (basic token management only — Stage 9)
- WebSocket handshake auth (Stage 11)
- oauth2-proxy edge gateway (not needed for local dev)

#### 4. Backend work

- `ru.timchat.auth.config.SecurityConfig` — `SecurityFilterChain` bean: stateless, CSRF disabled, OAuth2 Resource Server with JWT, permit actuator/health
- `ru.timchat.auth.config.CorsConfig` — CORS for localhost:4200
- `ru.timchat.auth.context.CurrentUserContext` — extract userId (from `sub` claim), username (from `preferred_username`), roles (from `realm_access.roles`) via `SecurityContextHolder`
- `ru.timchat.auth.config.DevSecurityConfig` — `@Profile("dev")` config with mock `JwtDecoder` for integration tests only
- `application.yml` updates: `spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8180/realms/timchat`, `jwk-set-uri`
- Auth-related error messages in message bundles (error.auth.unauthorized, error.auth.forbidden, error.auth.token-expired, error.auth.token-invalid)

#### 5. Frontend work

None (frontend auth flow in Stage 9).

#### 6. Infra work

- Add Keycloak 24 to `infra/docker-compose.yml`: port 8180, admin: admin/admin, health check
- Create `infra/keycloak/timchat-realm.json` — realm export with:
  - Realm: `timchat`
  - Client: `timchat-frontend` (public, authorization code flow, redirect to localhost:4200)
  - Client: `timchat-backend` (confidential, service account if needed)
  - Test users: `testuser` / `testpass` (MEMBER role), `testadmin` / `testpass` (ADMIN role)
  - Realm roles: OWNER, ADMIN, MEMBER, GUEST (mapped to workspace roles later)
- Keycloak volume mount for realm import on first start

#### 7. Test work

- Test that secured endpoint returns 401 without token
- Test that secured endpoint returns 200 with valid Keycloak JWT
- Test `CurrentUserContext` extracts correct userId, username, roles
- Test CORS headers for localhost:4200

#### 8. Main risks

- JWT issuer/audience mismatch between Keycloak config and Spring Security `issuer-uri`
- Keycloak realm import must include correct client settings (redirect URIs, web origins)
- Dev security profile must be `@Profile("dev")` only — never active in production

#### 9. Expected implementation gaps

- No user registration — users pre-created in Keycloak realm export
- No frontend auth flow yet (Stage 9)
- No automatic user provisioning from JWT to local DB (Stage 4)

#### 10. What must be fixed before done

- Keycloak starts in docker-compose and imports timchat realm
- Backend validates JWTs issued by Keycloak
- `CurrentUserContext` correctly extracts userId from Keycloak JWT `sub` claim
- Dev profile with mock decoder works for tests

#### 11. Acceptance criteria

- [ ] Keycloak starts in docker-compose with timchat realm
- [ ] Request without JWT → 401
- [ ] Request with valid Keycloak JWT → 200
- [ ] `CurrentUserContext` extracts userId, username from Keycloak token
- [ ] CORS allows requests from localhost:4200
- [ ] Dev profile works for integration tests without Keycloak

#### 12. Manual verification checklist

- [ ] `cd infra && docker-compose up -d` — Keycloak starts on port 8180
- [ ] Open Keycloak admin console at http://localhost:8180 (admin/admin)
- [ ] Verify `timchat` realm exists with test users
- [ ] Obtain JWT: `curl -X POST http://localhost:8180/realms/timchat/protocol/openid-connect/token -d "client_id=timchat-frontend&grant_type=password&username=testuser&password=testpass"`
- [ ] Call secured endpoint with token → 200
- [ ] Call secured endpoint without token → 401
- [ ] Verify `CurrentUserContext` values in debug

#### 13. Files/modules likely to change

```
backend/src/main/java/ru/timchat/auth/config/SecurityConfig.java
backend/src/main/java/ru/timchat/auth/config/CorsConfig.java
backend/src/main/java/ru/timchat/auth/config/DevSecurityConfig.java
backend/src/main/java/ru/timchat/auth/context/CurrentUserContext.java
backend/src/main/resources/application.yml
backend/src/main/resources/application-dev.yml
backend/src/main/resources/messages_en.properties
backend/src/main/resources/messages_ru.properties
infra/docker-compose.yml
infra/keycloak/timchat-realm.json
backend/src/test/java/ru/timchat/auth/config/SecurityConfigTest.java
```

#### 14. Suggested commit message

```
feat: add auth module with Keycloak JWT validation and Spring Security
```

#### 15. Exact stage implementation prompt

```
Read and follow all cursor rules from .cursor/rules/.

Read the implementation plan: docs/implementation/IMPLEMENTATION_PLAN.md — execute Stage 3 only.

Create the auth module for TimChat backend with Keycloak as the OIDC provider:

1. Create SecurityConfig in ru.timchat.auth.config:
   - @Configuration @EnableWebSecurity @EnableMethodSecurity
   - SecurityFilterChain: stateless session, CSRF disabled, OAuth2 resource server with JWT
   - Permit /actuator/health, /v3/api-docs/**, /swagger-ui/**
   - All other endpoints require authentication
   - Use @Slf4j

2. Create CorsConfig:
   - Allow origin http://localhost:4200 for local dev
   - Allow Authorization header, Content-Type, Accept, Accept-Language
   - Allow GET, POST, PUT, DELETE, PATCH, OPTIONS

3. Create CurrentUserContext in ru.timchat.auth.context:
   - Static methods to extract:
     - userId (UUID from 'sub' claim)
     - username (from 'preferred_username' claim)
     - roles (from 'realm_access.roles' claim — Keycloak-specific JWT structure)
   - Return from Jwt principal in SecurityContextHolder

4. Create DevSecurityConfig (@Profile("dev")):
   - Mock JwtDecoder bean that returns a fixed Jwt with test user claims
   - Must be clearly isolated from production config
   - Only for integration tests, never for production

5. Add Keycloak 24 to infra/docker-compose.yml:
   - Image: quay.io/keycloak/keycloak:24.0
   - Port 8180 (to avoid clash with backend 8080)
   - Admin: admin/admin
   - Command: start-dev --import-realm
   - Volume mount: ./keycloak/timchat-realm.json:/opt/keycloak/data/import/timchat-realm.json
   - Health check: curl http://localhost:8080/health/ready (internal Keycloak port)
   - Depends on: nothing (standalone)

6. Create infra/keycloak/timchat-realm.json:
   - Realm: timchat
   - Client "timchat-frontend": public client, standard flow enabled, redirect URIs [http://localhost:4200/*], web origins [http://localhost:4200]
   - Users: testuser/testpass (realm role: MEMBER), testadmin/testpass (realm role: ADMIN)

7. Update application.yml with Keycloak JWT config:
   - spring.security.oauth2.resourceserver.jwt.issuer-uri: http://localhost:8180/realms/timchat
   - spring.security.oauth2.resourceserver.jwt.jwk-set-uri: http://localhost:8180/realms/timchat/protocol/openid-connect/certs

8. Add auth error messages to message bundles (both ru and en):
   - error.auth.unauthorized
   - error.auth.forbidden
   - error.auth.token-expired
   - error.auth.token-invalid

8. Write tests: verify 401 without token, verify CurrentUserContext extraction.

After completing, update docs/implementation/EXECUTION_LOG.md with Stage 3 results.
```

---

### Stage 4: User & Workspace

#### 1. Objective

Создать модули `user` и `workspace` с полным CRUD: сущности, JPA, сервисы, REST контроллеры, Liquibase миграции.

#### 2. In-scope work

- User entity + UserProfile
- User REST (get profile, update profile)
- Workspace entity + CRUD REST
- WorkspaceMember entity + membership management
- Invite entity + invite flow (create, accept)
- Liquibase migrations for all tables

#### 3. Explicit out-of-scope

- Roles and permissions (Stage 5)
- Channel management (Stage 6)
- User registration (handled by OIDC provider)

#### 4. Backend work

- `ru.timchat.user.domain.User` — JPA entity (id, externalId, username, email, createdAt, updatedAt)
- `ru.timchat.user.domain.UserProfile` — JPA entity (userId, displayName, avatarUrl, status)
- `ru.timchat.user.domain.UserRepository` — JPA repository interface
- `ru.timchat.user.application.UserService` — profile management
- `ru.timchat.user.api.UserController` — REST endpoints
- `ru.timchat.user.api.UserProfileResponse` — response DTO (record)
- `ru.timchat.user.api.UpdateProfileRequest` — request DTO (record)
- `ru.timchat.workspace.domain.Workspace` — JPA entity (id, name, slug, ownerId, createdAt)
- `ru.timchat.workspace.domain.WorkspaceMember` — JPA entity (workspaceId, userId, joinedAt)
- `ru.timchat.workspace.domain.Invite` — JPA entity (id, workspaceId, code, createdBy, expiresAt, usedBy)
- `ru.timchat.workspace.domain.WorkspaceRepository`, `WorkspaceMemberRepository`, `InviteRepository`
- `ru.timchat.workspace.application.WorkspaceService` — create, update, delete workspace
- `ru.timchat.workspace.application.MembershipService` — join, leave, list members
- `ru.timchat.workspace.application.InviteService` — create invite, accept invite
- `ru.timchat.workspace.api.WorkspaceController` — REST
- `ru.timchat.workspace.api.*Request` / `*Response` — DTOs (records)
- `ru.timchat.workspace.mapper.WorkspaceMapper` — entity ↔ DTO mapping
- Liquibase changelogs: users, user_profiles, workspaces, workspace_members, invites

#### 5. Frontend work

None.

#### 6. Infra work

None.

#### 7. Test work

- Unit tests for domain logic (workspace creation, membership join/leave)
- Integration test: create workspace → add member → list members

#### 8. Main risks

- User entity must align with OIDC user model (externalId from JWT sub claim)
- Workspace slug uniqueness

#### 9. Expected implementation gaps

- No permission checks on workspace operations (added in Stage 5)
- No workspace-level access restrictions yet

#### 10. What must be fixed before done

- All CRUD endpoints work end-to-end
- Liquibase migrations apply cleanly on fresh database
- User provisioning from JWT works (first login creates user)

#### 11. Acceptance criteria

- [ ] POST /api/workspaces creates workspace
- [ ] GET /api/workspaces lists user's workspaces
- [ ] POST /api/workspaces/{id}/members adds member
- [ ] GET /api/workspaces/{id}/members lists members
- [ ] POST /api/workspaces/{id}/invites creates invite
- [ ] POST /api/invites/{code}/accept joins workspace
- [ ] GET /api/users/me returns current user profile
- [ ] PUT /api/users/me/profile updates profile
- [ ] Liquibase migrations apply cleanly

#### 12. Manual verification checklist

- [ ] Create workspace via REST → record appears in PostgreSQL
- [ ] Add member → member appears in member list
- [ ] Create invite → use invite code to join → member added
- [ ] Get current user profile → returns correct data

#### 13. Files/modules likely to change

```
backend/src/main/java/ru/timchat/user/domain/User.java
backend/src/main/java/ru/timchat/user/domain/UserProfile.java
backend/src/main/java/ru/timchat/user/domain/UserRepository.java
backend/src/main/java/ru/timchat/user/application/UserService.java
backend/src/main/java/ru/timchat/user/api/UserController.java
backend/src/main/java/ru/timchat/user/api/UserProfileResponse.java
backend/src/main/java/ru/timchat/user/api/UpdateProfileRequest.java
backend/src/main/java/ru/timchat/workspace/domain/Workspace.java
backend/src/main/java/ru/timchat/workspace/domain/WorkspaceMember.java
backend/src/main/java/ru/timchat/workspace/domain/Invite.java
backend/src/main/java/ru/timchat/workspace/domain/WorkspaceRepository.java
backend/src/main/java/ru/timchat/workspace/domain/WorkspaceMemberRepository.java
backend/src/main/java/ru/timchat/workspace/domain/InviteRepository.java
backend/src/main/java/ru/timchat/workspace/application/WorkspaceService.java
backend/src/main/java/ru/timchat/workspace/application/MembershipService.java
backend/src/main/java/ru/timchat/workspace/application/InviteService.java
backend/src/main/java/ru/timchat/workspace/api/WorkspaceController.java
backend/src/main/java/ru/timchat/workspace/api/*.java (DTOs)
backend/src/main/java/ru/timchat/workspace/mapper/WorkspaceMapper.java
backend/src/main/resources/db/changelog/changes/001-create-users.yaml
backend/src/main/resources/db/changelog/changes/002-create-workspaces.yaml
backend/src/main/resources/db/changelog/db.changelog-master.yaml
backend/src/main/resources/messages_en.properties
backend/src/main/resources/messages_ru.properties
```

#### 14. Suggested commit message

```
feat: add user and workspace modules with CRUD, membership, and invites
```

#### 15. Exact stage implementation prompt

```
Read and follow all cursor rules from .cursor/rules/.

Read the implementation plan: docs/implementation/IMPLEMENTATION_PLAN.md — execute Stage 4 only.

Create user and workspace modules for TimChat backend:

USER MODULE:
1. Create User entity in ru.timchat.user.domain:
   - UUID id (generated), String externalId (from OIDC sub), String username, String email
   - Timestamps: createdAt, updatedAt
   - JPA entity, @Getter, explicit constructor
2. Create UserProfile entity: userId (FK), displayName, avatarUrl, statusText
3. Create UserRepository (Spring Data JPA)
4. Create UserService in ru.timchat.user.application:
   - getOrCreateUser(externalId, username, email) — for first-login provisioning
   - getProfile(userId), updateProfile(userId, request)
5. Create UserController: GET /api/users/me, PUT /api/users/me/profile
6. Create request/response DTOs as records

WORKSPACE MODULE:
7. Create Workspace entity in ru.timchat.workspace.domain:
   - UUID id, String name, String slug (unique), UUID ownerId, createdAt, updatedAt
8. Create WorkspaceMember entity: UUID id, workspaceId (FK), userId (FK), joinedAt
9. Create Invite entity: UUID id, workspaceId (FK), String code (unique), UUID createdBy, Instant expiresAt, UUID usedBy (nullable), Instant usedAt (nullable)
10. Create repositories for all entities
11. Create WorkspaceService: create, update, delete, getById, listForUser
12. Create MembershipService: addMember, removeMember, listMembers, isMember
13. Create InviteService: createInvite, acceptInvite
14. Create WorkspaceController:
    - POST /api/workspaces
    - GET /api/workspaces
    - GET /api/workspaces/{id}
    - PUT /api/workspaces/{id}
    - DELETE /api/workspaces/{id}
    - GET /api/workspaces/{id}/members
    - POST /api/workspaces/{id}/members
    - DELETE /api/workspaces/{id}/members/{memberId}
    - POST /api/workspaces/{id}/invites
    - POST /api/invites/{code}/accept
15. Create DTOs as records, mapper for entity↔DTO

MIGRATIONS:
16. Liquibase changelogs for users, user_profiles, workspaces, workspace_members, invites tables

Follow rules:
- Entities are Java classes, not records. Use @Getter, explicit constructors.
- DTOs are records.
- Controllers return DTOs directly, use @ResponseStatus.
- Add i18n message keys for all error messages (both ru and en).
- Use @Slf4j for logging.

After completing, update docs/implementation/EXECUTION_LOG.md with Stage 4 results.
```

---

### Stage 5: Roles & Permissions

#### 1. Objective

Создать permission-модуль: роли, гранты, 12 типов permissions, resolution service, channel-level overrides, интеграция с `@PreAuthorize`.

#### 2. In-scope work

- `PermissionType` enum (12 types)
- `Role` entity
- `PermissionGrant` entity (role → permission mapping)
- `MemberRole` entity (member → role assignment)
- `ChannelPermissionOverride` entity
- `PermissionResolutionService` — resolves effective permissions for user in workspace + channel context
- `@PreAuthorize` integration via custom security expressions
- Default roles seeding (OWNER, ADMIN, MEMBER, GUEST)

#### 3. Explicit out-of-scope

- UI for role management
- Dynamic role creation (only predefined roles in MVP)

#### 4. Backend work

- `ru.timchat.permission.domain.PermissionType` — enum with 12 values
- `ru.timchat.permission.domain.Role` — entity (id, workspaceId, name, isSystem)
- `ru.timchat.permission.domain.PermissionGrant` — entity (roleId, permissionType)
- `ru.timchat.permission.domain.MemberRole` — entity (workspaceMemberId, roleId)
- `ru.timchat.permission.domain.ChannelPermissionOverride` — entity (channelId, roleId, permissionType, allowed)
- Repositories for all entities
- `ru.timchat.permission.application.PermissionResolutionService`:
  - `hasPermission(userId, workspaceId, permissionType)` → boolean
  - `hasChannelPermission(userId, channelId, permissionType)` → boolean
  - Resolves: workspace role grants + channel overrides
- `ru.timchat.permission.application.RoleService` — assign/revoke roles
- `ru.timchat.permission.api.RoleController` — REST for role assignment
- `ru.timchat.permission.config.PermissionSecurityConfig` — custom `@PreAuthorize` expressions
- Liquibase: roles, permission_grants, member_roles, channel_permission_overrides tables
- Seed data: default roles with permission grants

#### 5. Frontend work

None.

#### 6. Infra work

None.

#### 7. Test work

- Unit tests: `PermissionResolutionService` — all 12 permission types, allow/deny, override logic
- Integration test: assign role → check permission → verify access

#### 8. Main risks

- Permission resolution performance (must be efficient, potentially cached)
- Channel overrides must correctly override workspace-level grants

#### 9. Expected implementation gaps

- No UI for permission management
- Only predefined roles

#### 10. What must be fixed before done

- All 12 permission types are defined and resolvable
- Channel overrides work correctly (allow/deny override for specific roles)
- `@PreAuthorize` works with custom permission checks

#### 11. Acceptance criteria

- [x] 12 `PermissionType` values exist
- [x] Default roles (OWNER, ADMIN, MEMBER, GUEST) are seeded with correct grants
- [x] `PermissionResolutionService.hasPermission()` returns correct results
- [x] Channel overrides correctly modify effective permissions
- [x] `@PreAuthorize` integrates with permission service
- [x] Role assignment/revocation via REST works

#### 12. Manual verification checklist

- [ ] Assign MEMBER role → verify MESSAGE_WRITE is allowed
- [ ] Assign GUEST role → verify MESSAGE_WRITE is denied
- [ ] Add channel override → verify it overrides workspace-level grant
- [ ] Access protected endpoint without required permission → 403

#### 13. Files/modules likely to change

```
backend/src/main/java/ru/timchat/permission/domain/PermissionType.java
backend/src/main/java/ru/timchat/permission/domain/Role.java
backend/src/main/java/ru/timchat/permission/domain/PermissionGrant.java
backend/src/main/java/ru/timchat/permission/domain/MemberRole.java
backend/src/main/java/ru/timchat/permission/domain/ChannelPermissionOverride.java
backend/src/main/java/ru/timchat/permission/domain/*Repository.java
backend/src/main/java/ru/timchat/permission/application/PermissionResolutionService.java
backend/src/main/java/ru/timchat/permission/application/RoleService.java
backend/src/main/java/ru/timchat/permission/api/RoleController.java
backend/src/main/java/ru/timchat/permission/api/*Request.java
backend/src/main/java/ru/timchat/permission/api/*Response.java
backend/src/main/java/ru/timchat/permission/config/PermissionSecurityConfig.java
backend/src/main/resources/db/changelog/changes/003-create-permissions.yaml
backend/src/main/resources/messages_en.properties
backend/src/main/resources/messages_ru.properties
backend/src/test/java/ru/timchat/permission/application/PermissionResolutionServiceTest.java
```

#### 14. Suggested commit message

```
feat: add roles and permissions module with 12 permission types and resolution service
```

#### 15. Exact stage implementation prompt

```
Read and follow all cursor rules from .cursor/rules/.

Read the implementation plan: docs/implementation/IMPLEMENTATION_PLAN.md — execute Stage 5 only.

Create the permission module for TimChat backend:

1. Create PermissionType enum in ru.timchat.permission.domain:
   CHANNEL_VIEW, MESSAGE_WRITE, MESSAGE_DELETE_OWN, MESSAGE_DELETE_ANY,
   ROOM_JOIN, ROOM_SPEAK, ROOM_VIDEO, ROOM_SCREEN_SHARE,
   ROOM_MODERATE, ROOM_FORCE_MUTE, ROOM_KICK, ROOM_MOVE

2. Create entities:
   - Role: UUID id, UUID workspaceId, String name, boolean isSystem, createdAt
   - PermissionGrant: UUID id, UUID roleId (FK), PermissionType permissionType
   - MemberRole: UUID id, UUID workspaceMemberId (FK), UUID roleId (FK)
   - ChannelPermissionOverride: UUID id, UUID channelId, UUID roleId (FK), PermissionType permissionType, boolean allowed

3. Create PermissionResolutionService:
   - hasPermission(UUID userId, UUID workspaceId, PermissionType) → boolean
   - hasChannelPermission(UUID userId, UUID channelId, PermissionType) → boolean
   - Logic: get user's roles in workspace → collect granted permissions → apply channel overrides if checking channel-level
   - Must handle: user has no roles → deny all

4. Create RoleService: assignRole, revokeRole, listRolesForMember

5. Create RoleController:
   - POST /api/workspaces/{workspaceId}/members/{memberId}/roles — assign role
   - DELETE /api/workspaces/{workspaceId}/members/{memberId}/roles/{roleId} — revoke role
   - GET /api/workspaces/{workspaceId}/members/{memberId}/roles — list roles

6. Create Liquibase migrations for roles, permission_grants, member_roles, channel_permission_overrides
   Include seed data for default roles: OWNER (all permissions), ADMIN (all except ROOM_MOVE), MEMBER (VIEW, WRITE, DELETE_OWN, JOIN, SPEAK, VIDEO, SCREEN_SHARE), GUEST (VIEW only)

7. Integrate with @PreAuthorize using a custom security bean that delegates to PermissionResolutionService

8. Write unit tests for PermissionResolutionService covering all permission types and override logic.

After completing, update docs/implementation/EXECUTION_LOG.md with Stage 5 results.
```

---

### Stage 6: Channels

#### 1. Objective

Создать модуль channels: CRUD, типы каналов (text/voice/video), visibility enforcement через permission system.

#### 2. In-scope work

- Channel entity with type (TEXT, VOICE, VIDEO)
- Channel CRUD REST
- Channel visibility enforcement via `CHANNEL_VIEW` permission
- Channel ordering within workspace

#### 3. Explicit out-of-scope

- Chat messages (Stage 7)
- Call sessions in channels (Stage 15)
- Realtime channel subscriptions (Stage 11)

#### 4. Backend work

- `ru.timchat.channel.domain.Channel` — entity (id, workspaceId, name, type, position, createdAt, updatedAt)
- `ru.timchat.channel.domain.ChannelType` — enum (TEXT, VOICE, VIDEO)
- `ru.timchat.channel.domain.ChannelRepository`
- `ru.timchat.channel.application.ChannelService` — create, update, delete, reorder, list visible channels
- `ru.timchat.channel.api.ChannelController` — REST
- `ru.timchat.channel.api.*Request` / `*Response` — DTOs (records)
- Liquibase migration for channels table
- Permission check: list only channels where user has `CHANNEL_VIEW`

#### 5. Frontend work

None.

#### 6. Infra work

None.

#### 7. Test work

- Unit: channel creation with valid/invalid types
- Integration: create channel → list → verify visibility with/without permission

#### 8. Main risks

- Channel type must be immutable after creation (voice channel should not become text)
- Ordering logic must handle concurrent reorders

#### 9. Expected implementation gaps

- No realtime subscription to channel events
- No messages in channels yet

#### 10. What must be fixed before done

- CRUD works end-to-end
- Visibility is enforced via permission
- Channel type is validated and immutable

#### 11. Acceptance criteria

- [ ] POST /api/workspaces/{id}/channels creates channel with type
- [ ] GET /api/workspaces/{id}/channels returns only visible channels
- [ ] Channel type is validated (TEXT, VOICE, VIDEO)
- [ ] Channel type cannot be changed after creation
- [ ] Channel ordering works

#### 12. Manual verification checklist

- [ ] Create TEXT channel → appears in list
- [ ] Create VOICE channel → appears in list
- [ ] User without CHANNEL_VIEW on specific channel → channel not in list
- [ ] Attempt to change channel type → error

#### 13. Files/modules likely to change

```
backend/src/main/java/ru/timchat/channel/domain/Channel.java
backend/src/main/java/ru/timchat/channel/domain/ChannelType.java
backend/src/main/java/ru/timchat/channel/domain/ChannelRepository.java
backend/src/main/java/ru/timchat/channel/application/ChannelService.java
backend/src/main/java/ru/timchat/channel/api/ChannelController.java
backend/src/main/java/ru/timchat/channel/api/*.java
backend/src/main/resources/db/changelog/changes/004-create-channels.yaml
backend/src/main/resources/messages_en.properties
backend/src/main/resources/messages_ru.properties
```

#### 14. Suggested commit message

```
feat: add channels module with CRUD, types, and visibility enforcement
```

#### 15. Exact stage implementation prompt

```
Read and follow all cursor rules from .cursor/rules/.

Read the implementation plan: docs/implementation/IMPLEMENTATION_PLAN.md — execute Stage 6 only.

Create the channels module for TimChat backend:

1. Create ChannelType enum: TEXT, VOICE, VIDEO

2. Create Channel entity in ru.timchat.channel.domain:
   - UUID id, UUID workspaceId (FK), String name, ChannelType type, int position, Instant createdAt, Instant updatedAt
   - Type is immutable after creation

3. Create ChannelRepository

4. Create ChannelService in ru.timchat.channel.application:
   - createChannel(workspaceId, name, type) — validate type, set position
   - updateChannel(channelId, name) — cannot change type
   - deleteChannel(channelId)
   - listVisibleChannels(workspaceId, userId) — filter by CHANNEL_VIEW permission using PermissionResolutionService
   - reorderChannels(workspaceId, channelIds) — update positions

5. Create ChannelController:
   - POST /api/workspaces/{workspaceId}/channels — create
   - GET /api/workspaces/{workspaceId}/channels — list visible
   - GET /api/channels/{id} — get by id (with permission check)
   - PUT /api/channels/{id} — update (name only)
   - DELETE /api/channels/{id}
   - PUT /api/workspaces/{workspaceId}/channels/order — reorder

6. Create DTOs as records, mapper

7. Liquibase migration for channels table

8. Add i18n messages: error.channel.not-found, error.channel.type-immutable, error.channel.name-required

9. Permission checks: @PreAuthorize or service-level check for CHANNEL_VIEW

After completing, update docs/implementation/EXECUTION_LOG.md with Stage 6 results.
```

---

### Stage 7: Chat Backend

#### 1. Objective

Создать модуль chat: отправка, редактирование, удаление сообщений; загрузка истории с keyset pagination.

#### 2. In-scope work

- Message entity with content, author, channel, timestamps
- MessageRevision for edit history
- MessageDeletion for soft deletes
- Send/edit/delete via REST
- History with keyset pagination (cursor-based)
- MessageAttachment relationship (metadata only)
- Permission checks: `MESSAGE_WRITE`, `MESSAGE_DELETE_OWN`, `MESSAGE_DELETE_ANY`

#### 3. Explicit out-of-scope

- Realtime message delivery via WebSocket (Stage 12)
- Attachment upload/storage (Stage 8)
- Message reactions, threads, formatting

#### 4. Backend work

- `ru.timchat.message.domain.Message` — entity (id, channelId, authorId, content, createdAt, updatedAt, deleted)
- `ru.timchat.message.domain.MessageRevision` — entity (id, messageId, previousContent, editedAt)
- `ru.timchat.message.domain.MessageDeletion` — entity (id, messageId, deletedBy, deletedAt, reason)
- `ru.timchat.message.domain.MessageAttachment` — entity (id, messageId, attachmentMetadataId)
- Repositories
- `ru.timchat.message.application.MessageService` — send, edit, delete, getHistory
- `ru.timchat.message.api.MessageController` — REST
- DTOs (records)
- Keyset pagination: use `createdAt` + `id` as cursor
- Liquibase migrations

#### 5. Frontend work

None.

#### 6. Infra work

None.

#### 7. Test work

- Unit: message creation, edit creates revision, delete creates deletion record
- Integration: send → edit → verify revision exists → delete → verify soft-deleted
- Pagination: verify keyset pagination returns correct page with cursor

#### 8. Main risks

- Keyset pagination cursor encoding/decoding
- Edit must preserve original content in revision
- Delete must be idempotent

#### 9. Expected implementation gaps

- Messages are only delivered via REST (polling), not realtime (Stage 12)
- No attachment file upload yet (only attachment metadata link)

#### 10. What must be fixed before done

- Send/edit/delete works end-to-end
- History pagination returns correct results
- Permission checks enforced

#### 11. Acceptance criteria

- [x] POST /api/channels/{id}/messages sends message
- [x] PUT /api/messages/{id} edits message (creates revision)
- [x] DELETE /api/messages/{id} soft-deletes message
- [x] GET /api/channels/{id}/messages returns paginated history
- [x] Pagination cursor works correctly
- [x] MESSAGE_WRITE permission required to send
- [x] MESSAGE_DELETE_OWN allows deleting own messages only
- [x] MESSAGE_DELETE_ANY allows deleting any message

#### 12. Manual verification checklist

- [ ] Send message → appears in history
- [ ] Edit message → old content preserved in revision
- [ ] Delete message → marked as deleted, not physically removed
- [ ] Pagination: first page → use cursor → second page → correct messages
- [ ] User without MESSAGE_WRITE → 403 on send

#### 13. Files/modules likely to change

```
backend/src/main/java/ru/timchat/message/domain/Message.java
backend/src/main/java/ru/timchat/message/domain/MessageRevision.java
backend/src/main/java/ru/timchat/message/domain/MessageDeletion.java
backend/src/main/java/ru/timchat/message/domain/MessageAttachment.java
backend/src/main/java/ru/timchat/message/domain/*Repository.java
backend/src/main/java/ru/timchat/message/application/MessageService.java
backend/src/main/java/ru/timchat/message/api/MessageController.java
backend/src/main/java/ru/timchat/message/api/*.java
backend/src/main/resources/db/changelog/changes/005-create-messages.yaml
backend/src/main/resources/messages_en.properties
backend/src/main/resources/messages_ru.properties
```

#### 14. Suggested commit message

```
feat: add chat message module with send, edit, delete, and keyset pagination
```

#### 15. Exact stage implementation prompt

```
Read and follow all cursor rules from .cursor/rules/.

Read the implementation plan: docs/implementation/IMPLEMENTATION_PLAN.md — execute Stage 7 only.

Create the message module for TimChat backend:

1. Create Message entity in ru.timchat.message.domain:
   - UUID id, UUID channelId (FK), UUID authorId (FK), String content (max 4000), Instant createdAt, Instant updatedAt, boolean deleted
   - @Getter, explicit constructor with invariants

2. Create MessageRevision entity: UUID id, UUID messageId (FK), String previousContent, Instant editedAt

3. Create MessageDeletion entity: UUID id, UUID messageId (FK), UUID deletedBy, Instant deletedAt, String reason (nullable)

4. Create MessageAttachment entity: UUID id, UUID messageId (FK), UUID attachmentMetadataId (FK, nullable for now)

5. Create repositories

6. Create MessageService in ru.timchat.message.application:
   - sendMessage(channelId, authorId, content, attachmentIds) → MessageResponse
     - Check MESSAGE_WRITE permission
   - editMessage(messageId, userId, newContent) → MessageResponse
     - Check ownership, create MessageRevision with old content
   - deleteMessage(messageId, userId) → void
     - Check MESSAGE_DELETE_OWN (own) or MESSAGE_DELETE_ANY (any), create MessageDeletion
   - getHistory(channelId, cursor, limit) → PageResponse<MessageResponse>
     - Keyset pagination using (createdAt, id) as cursor
     - Default limit 50, max 100
     - Do not return deleted messages (or return with deleted flag)

7. Create MessageController:
   - POST /api/channels/{channelId}/messages — send
   - PUT /api/messages/{id} — edit
   - DELETE /api/messages/{id} — delete
   - GET /api/channels/{channelId}/messages?cursor=...&limit=... — history

8. Create DTOs as records:
   - SendMessageRequest: content, attachmentIds
   - EditMessageRequest: content
   - MessageResponse: id, channelId, authorId, authorName, content, createdAt, updatedAt, deleted, attachments
   - PageResponse<T>: items, nextCursor, hasMore

9. Liquibase migrations for messages, message_revisions, message_deletions, message_attachments
   - Index on (channel_id, created_at, id) for keyset pagination

10. Add i18n messages for errors

After completing, update docs/implementation/EXECUTION_LOG.md with Stage 7 results.
```

---

### Stage 8: Attachments Backend

#### 1. Objective

Создать модуль attachments: metadata, S3 adapter, upload/download авторизация, signed URLs.

#### 2. In-scope work

- AttachmentMetadata entity
- S3 client adapter (MinIO compatible)
- Upload flow: request upload URL → upload to S3 → confirm metadata
- Download flow: request download URL → get signed URL
- Content-type validation, size limits

#### 3. Explicit out-of-scope

- Image thumbnails
- File preview
- Virus scanning

#### 4. Backend work

- `ru.timchat.attachment.domain.AttachmentMetadata` — entity (id, workspaceId, channelId, uploadedBy, fileName, contentType, size, storageKey, status, createdAt)
- `ru.timchat.attachment.domain.AttachmentStatus` — enum (PENDING, UPLOADED, FAILED)
- `ru.timchat.attachment.domain.AttachmentMetadataRepository`
- `ru.timchat.attachment.application.AttachmentService` — initUpload, confirmUpload, getDownloadUrl
- `ru.timchat.attachment.infra.S3StorageClient` — MinIO/S3 operations (presigned upload URL, presigned download URL)
- `ru.timchat.attachment.api.AttachmentController` — REST
- DTOs (records)
- Content-type whitelist, max file size config
- Liquibase migration

#### 5. Frontend work

None.

#### 6. Infra work

- Ensure MinIO bucket auto-creation in docker-compose or init script

#### 7. Test work

- Unit: content-type validation, size limit check
- Integration: initUpload → confirmUpload → getDownloadUrl flow

#### 8. Main risks

- Presigned URL expiry configuration
- MinIO client compatibility with S3 API

#### 9. Expected implementation gaps

- No frontend upload UI yet (Stage 10)
- No virus scanning or content inspection

#### 10. What must be fixed before done

- Full upload/download flow works end-to-end with MinIO
- Invalid content types are rejected
- Oversized files are rejected

#### 11. Acceptance criteria

- [ ] POST /api/attachments/upload-url returns presigned upload URL
- [ ] POST /api/attachments/{id}/confirm marks attachment as UPLOADED
- [ ] GET /api/attachments/{id}/download-url returns presigned download URL
- [ ] Invalid content type → 422
- [ ] File exceeding size limit → 422

#### 12. Manual verification checklist

- [ ] Request upload URL → upload file via curl to MinIO → confirm → download URL works
- [ ] Upload with disallowed content type → rejected
- [ ] Upload exceeding size limit → rejected

#### 13. Files/modules likely to change

```
backend/src/main/java/ru/timchat/attachment/domain/AttachmentMetadata.java
backend/src/main/java/ru/timchat/attachment/domain/AttachmentStatus.java
backend/src/main/java/ru/timchat/attachment/domain/AttachmentMetadataRepository.java
backend/src/main/java/ru/timchat/attachment/application/AttachmentService.java
backend/src/main/java/ru/timchat/attachment/infra/S3StorageClient.java
backend/src/main/java/ru/timchat/attachment/api/AttachmentController.java
backend/src/main/java/ru/timchat/attachment/api/*.java
backend/src/main/resources/db/changelog/changes/006-create-attachments.yaml
backend/src/main/resources/application.yml (S3/MinIO config)
backend/src/main/resources/messages_en.properties
backend/src/main/resources/messages_ru.properties
```

#### 14. Suggested commit message

```
feat: add attachments module with S3 storage, upload/download auth, and metadata
```

#### 15. Exact stage implementation prompt

```
Read and follow all cursor rules from .cursor/rules/.

Read the implementation plan: docs/implementation/IMPLEMENTATION_PLAN.md — execute Stage 8 only.

Create the attachments module for TimChat backend:

1. Create AttachmentStatus enum: PENDING, UPLOADED, FAILED

2. Create AttachmentMetadata entity in ru.timchat.attachment.domain:
   - UUID id, UUID workspaceId, UUID channelId (nullable), UUID uploadedBy
   - String fileName, String contentType, long sizeBytes, String storageKey
   - AttachmentStatus status, Instant createdAt

3. Create AttachmentMetadataRepository

4. Create S3StorageClient in ru.timchat.attachment.infra:
   - Uses AWS SDK v2 or MinIO Java client
   - generatePresignedUploadUrl(storageKey, contentType, expiry) → String
   - generatePresignedDownloadUrl(storageKey, expiry) → String
   - Configure from application.yml (endpoint, bucket, access-key, secret-key)

5. Create AttachmentService in ru.timchat.attachment.application:
   - initiateUpload(workspaceId, channelId, userId, fileName, contentType, sizeBytes):
     - Validate content type against whitelist (images, documents, archives)
     - Validate size <= configured max (default 25MB)
     - Create AttachmentMetadata with PENDING status
     - Generate presigned upload URL
     - Return upload URL + attachment ID
   - confirmUpload(attachmentId, userId):
     - Verify ownership, update status to UPLOADED
   - getDownloadUrl(attachmentId, userId):
     - Check user has access to the workspace/channel
     - Return presigned download URL

6. Create AttachmentController:
   - POST /api/attachments/upload — initiate upload
   - POST /api/attachments/{id}/confirm — confirm upload
   - GET /api/attachments/{id}/download-url — get download URL

7. Create DTOs as records

8. Liquibase migration for attachment_metadata table

9. Add S3/MinIO config to application.yml

10. Add i18n messages for errors

After completing, update docs/implementation/EXECUTION_LOG.md with Stage 8 results.
```

---

### Stage 9: Frontend Foundation

#### 1. Objective

Создать Angular shell: core layout (nav rail, sidebar, main area, top bar, right panel), auth flow, OpenAPI client generation, routing.

#### 2. In-scope work

- Angular project structure: `core/`, `shared/`, `features/`
- App layout component with all 5 zones
- Auth guard, HTTP interceptor (JWT), AuthStore
- OpenAPI client generation setup (from backend spec)
- Routing skeleton (login, workspace, channel)
- Design tokens (CSS custom properties for Cursor-inspired theme)
- Basic shared UI components (button, input, avatar placeholder)

#### 3. Explicit out-of-scope

- Feature implementations (workspace selector, chat timeline, etc. — Stage 10)
- WebSocket connection (Stage 13)
- Call/presence UI (Stages 17, 13)

#### 4. Backend work

- Ensure springdoc-openapi generates `/v3/api-docs` spec

#### 5. Frontend work

- `src/app/core/layout/` — AppLayoutComponent (shell with 5 zones)
- `src/app/core/auth/` — AuthGuard, AuthInterceptor, AuthStore (signal-based)
- `src/app/core/api/` — generated OpenAPI client
- `src/app/shared/ui/` — ButtonComponent, InputComponent, AvatarComponent
- `src/app/shared/models/` — shared TypeScript interfaces
- `src/app/features/auth/` — LoginComponent (Keycloak OIDC authorization code flow with PKCE)
- `angular-oauth2-oidc` npm package for Keycloak OIDC integration
- `src/app/app.routes.ts` — route definitions
- `src/styles.scss` — global styles, CSS custom properties
- `src/app/core/layout/` — NavigationRailComponent, SidebarComponent, TopBarComponent, RightPanelComponent
- Angular environment config

#### 6. Infra work

None.

#### 7. Test work

- Verify layout renders with all 5 zones
- Verify auth guard redirects unauthenticated users
- Verify OpenAPI client is generated and importable

#### 8. Main risks

- OpenAPI client generation pipeline reliability
- Auth token refresh handling
- Layout CSS must be responsive within desktop viewport only

#### 9. Expected implementation gaps

- Layout zones are empty shells (content added in Stage 10+)
- No actual API calls yet (no data to display)
- Auth flow may be simplified (dev mode)

#### 10. What must be fixed before done

- Layout renders correctly with 5 visible zones
- Auth guard works
- OpenAPI client generates from backend spec
- Routing works for basic navigation

#### 11. Acceptance criteria

- [ ] App renders with nav rail, sidebar, main area, top bar, right panel
- [ ] Unauthenticated user is redirected to login
- [ ] Auth token is attached to API requests via interceptor
- [ ] OpenAPI client is generated and builds without errors
- [ ] Routes: /login, /workspaces, /workspaces/:id/channels/:channelId

#### 12. Manual verification checklist

- [ ] Open app → see layout with all zones
- [ ] Navigate to protected route without token → redirect to login
- [ ] Login (dev mode) → navigate to workspace → layout shows
- [ ] Inspect network → API requests have Authorization header

#### 13. Files/modules likely to change

```
frontend/src/app/core/layout/app-layout.component.ts
frontend/src/app/core/layout/app-layout.component.scss
frontend/src/app/core/layout/app-layout.component.html
frontend/src/app/core/layout/navigation-rail.component.ts
frontend/src/app/core/layout/sidebar.component.ts
frontend/src/app/core/layout/top-bar.component.ts
frontend/src/app/core/layout/right-panel.component.ts
frontend/src/app/core/auth/auth.guard.ts
frontend/src/app/core/auth/auth.interceptor.ts
frontend/src/app/core/auth/auth.store.ts
frontend/src/app/features/auth/login.component.ts
frontend/src/app/shared/ui/button.component.ts
frontend/src/app/shared/ui/input.component.ts
frontend/src/app/app.routes.ts
frontend/src/app/app.component.ts
frontend/src/styles.scss
frontend/openapi-generator.config.json
frontend/package.json (openapi-generator, angular-oauth2-oidc dependencies)
frontend/src/environments/environment.ts
frontend/src/environments/environment.development.ts
```

#### 14. Suggested commit message

```
feat: scaffold frontend with Cursor-inspired layout, Keycloak auth, and OpenAPI client
```

#### 15. Exact stage implementation prompt

```
Read and follow all cursor rules from .cursor/rules/.
Read docs/frontend/FRONTEND_UI_DIRECTION.md and docs/frontend/FRONTEND_LAYOUT_PRINCIPLES.md for design guidance.

Read the implementation plan: docs/implementation/IMPLEMENTATION_PLAN.md — execute Stage 9 only.

Create the Angular frontend foundation for TimChat:

STRUCTURE:
1. Create directory structure: core/, shared/, features/ under src/app/
2. Create core/auth/, core/api/, core/layout/, core/routing/
3. Create shared/ui/, shared/util/, shared/models/
4. Create features/auth/, features/workspace/, features/channels/, features/chat/

LAYOUT:
5. Create AppLayoutComponent in core/layout/:
   - 5 zones: left navigation rail, secondary sidebar, main area, right contextual panel, top bar
   - Cursor-inspired: light theme, minimal, dense, professional
   - CSS Grid or Flexbox layout
   - Navigation rail: fixed left, narrow (48-56px wide)
   - Sidebar: ~240px, collapsible
   - Main area: flex-grow
   - Right panel: ~280px, collapsible
   - Top bar: compact, full width above main content

6. Create NavigationRailComponent, SidebarComponent, TopBarComponent, RightPanelComponent as standalone components

DESIGN TOKENS:
7. Create CSS custom properties in styles.scss:
   - Colors: neutral palette (grays), accent color, text colors, border colors
   - Typography: system font stack, sizes (12-16px range for density)
   - Spacing: 4px base unit
   - Borders: 1px solid, subtle colors
   - No gradients, no shadows heavier than subtle

AUTH:
8. Create AuthStore (signal-based state management):
   - token, user, isAuthenticated signals
   - login(), logout() methods
   - Store token in localStorage

9. Create AuthGuard: redirect to /login if not authenticated

10. Create AuthInterceptor: attach Bearer token to all API requests

11. Create LoginComponent in features/auth/:
    - Redirect to Keycloak authorization endpoint for login
    - Handle OAuth2 authorization code callback (exchange code for token)
    - Keycloak config: issuer http://localhost:8180/realms/timchat, clientId timchat-frontend
    - Use angular-oauth2-oidc or manual PKCE flow with fetch
    - On success: store tokens, redirect to workspace

OPENAPI:
12. Setup OpenAPI client generation:
    - Add @openapitools/openapi-generator-cli as dev dependency
    - Create generation config (typescript-angular generator)
    - npm script: "generate-api" to regenerate client from backend spec URL
    - Generated output to src/app/core/api/generated/

ROUTING:
13. Configure routes: /login, /w/:workspaceId, /w/:workspaceId/c/:channelId

14. Create basic shared UI components: ButtonComponent, InputComponent (standalone, styled)

After completing, update docs/implementation/EXECUTION_LOG.md with Stage 9 results.
```

---

### Stage 10: Frontend Workspace/Channels/Chat

#### 1. Objective

Реализовать frontend UI для workspace selector, channel list, chat timeline, message compose, edit/delete, attachment upload/preview.

#### 2. In-scope work

- WorkspaceStore + workspace selector in navigation rail
- ChannelStore + channel list in sidebar
- ChatStore + chat timeline in main area
- Message compose component
- Edit/delete message interactions
- Attachment upload trigger + preview
- History loading with pagination (scroll-based)

#### 3. Explicit out-of-scope

- Realtime updates (Stage 13)
- Presence indicators (Stage 13)
- Call UI (Stage 17)

#### 4. Backend work

None (API already built in Stages 4-8).

#### 5. Frontend work

- `features/workspace/workspace.store.ts` — WorkspaceStore (signal-based)
- `features/workspace/workspace-selector.component.ts` — in navigation rail
- `features/channels/channel.store.ts` — ChannelStore
- `features/channels/channel-list.component.ts` — in sidebar
- `features/channels/channel-list-item.component.ts`
- `features/chat/chat.store.ts` — ChatStore
- `features/chat/chat-timeline.component.ts` — in main area
- `features/chat/message-item.component.ts` — single message display
- `features/chat/message-compose.component.ts` — input area
- `features/chat/message-actions.component.ts` — edit/delete actions
- `features/chat/attachment-upload.component.ts` — file upload trigger
- `features/chat/attachment-preview.component.ts` — inline preview
- All using generated OpenAPI client for API calls

#### 6. Infra work

None.

#### 7. Test work

- Component tests: workspace selector renders workspaces
- Component tests: channel list renders channels grouped by type
- Component tests: chat timeline renders messages in correct order

#### 8. Main risks

- Scroll-based pagination performance
- Message editing inline UX

#### 9. Expected implementation gaps

- No live updates — must refresh to see new messages
- No presence indicators
- No channel type visual differentiation for voice/video

#### 10. What must be fixed before done

- User can select workspace → see channels → select channel → see messages → send message → see it in timeline
- Edit and delete work
- Attachment upload flow works end-to-end

#### 11. Acceptance criteria

- [ ] Workspace selector shows user's workspaces
- [ ] Selecting workspace loads channels in sidebar
- [ ] Channels grouped/sorted by type and position
- [ ] Selecting channel loads message history
- [ ] Scroll up loads older messages (pagination)
- [ ] Send message appears in timeline (after refresh)
- [ ] Edit message works
- [ ] Delete message works
- [ ] Attachment upload → preview in message

#### 12. Manual verification checklist

- [ ] Login → see workspaces → select → see channels
- [ ] Select text channel → see messages → send new message
- [ ] Scroll up → older messages load
- [ ] Edit own message → content updates
- [ ] Delete own message → message marked as deleted
- [ ] Upload file → see preview in compose → send → attachment visible

#### 13. Files/modules likely to change

```
frontend/src/app/features/workspace/workspace.store.ts
frontend/src/app/features/workspace/workspace-selector.component.ts
frontend/src/app/features/channels/channel.store.ts
frontend/src/app/features/channels/channel-list.component.ts
frontend/src/app/features/channels/channel-list-item.component.ts
frontend/src/app/features/chat/chat.store.ts
frontend/src/app/features/chat/chat-timeline.component.ts
frontend/src/app/features/chat/message-item.component.ts
frontend/src/app/features/chat/message-compose.component.ts
frontend/src/app/features/chat/message-actions.component.ts
frontend/src/app/features/chat/attachment-upload.component.ts
frontend/src/app/features/chat/attachment-preview.component.ts
frontend/src/app/core/layout/navigation-rail.component.ts
frontend/src/app/core/layout/sidebar.component.ts
```

#### 14. Suggested commit message

```
feat: add workspace, channels, and chat UI with message operations and attachments
```

#### 15. Exact stage implementation prompt

```
Read and follow all cursor rules from .cursor/rules/.
Read docs/frontend/FRONTEND_UI_DIRECTION.md and docs/frontend/FRONTEND_LAYOUT_PRINCIPLES.md.

Read the implementation plan: docs/implementation/IMPLEMENTATION_PLAN.md — execute Stage 10 only.

Create the workspace, channels, and chat UI for TimChat frontend:

WORKSPACE:
1. Create WorkspaceStore in features/workspace/:
   - Signal-based state: workspaces, activeWorkspace, loading
   - loadWorkspaces() — call generated API client
   - setActiveWorkspace(id) — switch context

2. Create WorkspaceSelector component for navigation rail:
   - Show workspace icons/avatars in vertical list
   - Highlight active workspace
   - Click to switch

CHANNELS:
3. Create ChannelStore in features/channels/:
   - Signal-based: channels, activeChannel, loading
   - loadChannels(workspaceId) — call API
   - setActiveChannel(id)

4. Create ChannelList component for sidebar:
   - Group channels by type (TEXT section, VOICE section, VIDEO section)
   - Show channel name, type icon
   - Highlight active channel

CHAT:
5. Create ChatStore in features/chat/:
   - Signal-based: messages, loading, hasMore, cursor
   - loadMessages(channelId, cursor?) — keyset pagination
   - sendMessage(channelId, content, attachmentIds)
   - editMessage(messageId, content)
   - deleteMessage(messageId)
   - prependOlderMessages() — for scroll pagination

6. Create ChatTimeline component for main area:
   - Render messages in chronological order
   - Scroll-based pagination: detect scroll to top → load older messages
   - Auto-scroll to bottom on new message send

7. Create MessageItem component:
   - Show: avatar, author name, timestamp, content
   - Show edit/delete actions on hover (for own messages)
   - Show attachment previews if present
   - Show "edited" indicator, "deleted" state

8. Create MessageCompose component:
   - Text input (textarea, auto-resize)
   - Send button (Enter to send, Shift+Enter for newline)
   - Attachment upload button
   - Show pending attachments before send

9. Create AttachmentUpload component:
   - File picker
   - Upload progress indicator
   - Preview before send

10. Wire everything together:
    - Route /w/:workspaceId/c/:channelId → load workspace, channels, messages
    - Workspace change → reload channels
    - Channel change → reload messages

After completing, update docs/implementation/EXECUTION_LOG.md with Stage 10 results.
```

---

### Stage 11: WebSocket Realtime Foundation

#### 1. Objective

Создать realtime-модуль: WebSocket transport, protocol envelope, parser, dispatcher, typed handler interfaces, connection registry в Redis.

#### 2. In-scope work

- WebSocket configuration (Spring)
- Handshake auth (JWT validation)
- Protocol envelope model (Java)
- Envelope parser + validator
- Dispatcher: route by scope+type to typed handlers
- Typed handler interfaces: System, Chat, Presence, Call, Moderation
- Connection registry in Redis (connectionId → userId, sessionId)
- ClientSession management

#### 3. Explicit out-of-scope

- Actual handler implementations (Stages 12, 15, 16)
- Frontend WebSocket client (Stage 13)
- Reconnect logic (Stage 14)

#### 4. Backend work

- `ru.timchat.realtime.ws.WebSocketConfig` — WebSocket endpoint registration
- `ru.timchat.realtime.ws.WebSocketHandshakeInterceptor` — JWT validation from query param
- `ru.timchat.realtime.ws.TimChatWebSocketHandler` — thin entrypoint, delegates to dispatcher
- `ru.timchat.realtime.protocol.ProtocolEnvelope` — model (protocolVersion, messageId, correlationId, type, scope, sentAt, clientSessionId, payload)
- `ru.timchat.realtime.protocol.ProtocolScope` — enum (SYSTEM, WORKSPACE, CHANNEL, CHAT, PRESENCE, CALL, MODERATION, MEDIA)
- `ru.timchat.realtime.protocol.ProtocolParser` — parse JSON → ProtocolEnvelope, validate required fields
- `ru.timchat.realtime.dispatch.MessageDispatcher` — route to handler by scope
- `ru.timchat.realtime.handler.CommandHandler` — interface (handle(ProtocolEnvelope, WebSocketSession))
- `ru.timchat.realtime.handler.SystemCommandHandler`, `ChatCommandHandler`, `PresenceCommandHandler`, `CallCommandHandler`, `ModerationCommandHandler` — interfaces/stubs
- `ru.timchat.realtime.registry.ConnectionRegistry` — Redis-backed: register, unregister, getUserConnections, getConnectionUser
- `ru.timchat.realtime.registry.ClientSessionManager` — manage clientSessionId lifecycle
- `ru.timchat.realtime.security.WsAuthService` — per-command authorization

#### 5. Frontend work

None.

#### 6. Infra work

None.

#### 7. Test work

- Unit: ProtocolParser parses valid envelope, rejects invalid
- Unit: MessageDispatcher routes to correct handler by scope
- Integration: WebSocket connection with valid JWT succeeds

#### 8. Main risks

- WebSocket handshake JWT extraction reliability
- Redis connection registry cleanup on disconnect
- Thread safety in WebSocket handler

#### 9. Expected implementation gaps

- Handlers are stubs — only parse and acknowledge
- No actual business logic behind WS commands yet

#### 10. What must be fixed before done

- WebSocket connects with valid JWT
- Protocol envelope is parsed and validated
- Dispatcher routes to correct handler stub
- Connection is registered/unregistered in Redis

#### 11. Acceptance criteria

- [ ] WebSocket connection with valid JWT → success
- [ ] WebSocket connection without JWT → rejected
- [ ] Send valid protocol envelope → dispatched to correct handler
- [ ] Send invalid envelope → error response
- [ ] Connection registered in Redis on connect
- [ ] Connection removed from Redis on disconnect

#### 12. Manual verification checklist

- [ ] Connect via wscat or browser console with JWT in query param
- [ ] Send JSON envelope with scope "system" → handler receives it
- [ ] Send malformed JSON → error response
- [ ] Disconnect → Redis entry removed

#### 13. Files/modules likely to change

```
backend/src/main/java/ru/timchat/realtime/ws/WebSocketConfig.java
backend/src/main/java/ru/timchat/realtime/ws/WebSocketHandshakeInterceptor.java
backend/src/main/java/ru/timchat/realtime/ws/TimChatWebSocketHandler.java
backend/src/main/java/ru/timchat/realtime/protocol/ProtocolEnvelope.java
backend/src/main/java/ru/timchat/realtime/protocol/ProtocolScope.java
backend/src/main/java/ru/timchat/realtime/protocol/ProtocolParser.java
backend/src/main/java/ru/timchat/realtime/dispatch/MessageDispatcher.java
backend/src/main/java/ru/timchat/realtime/handler/CommandHandler.java
backend/src/main/java/ru/timchat/realtime/handler/SystemCommandHandler.java
backend/src/main/java/ru/timchat/realtime/handler/ChatCommandHandler.java
backend/src/main/java/ru/timchat/realtime/handler/PresenceCommandHandler.java
backend/src/main/java/ru/timchat/realtime/handler/CallCommandHandler.java
backend/src/main/java/ru/timchat/realtime/handler/ModerationCommandHandler.java
backend/src/main/java/ru/timchat/realtime/registry/ConnectionRegistry.java
backend/src/main/java/ru/timchat/realtime/registry/ClientSessionManager.java
backend/src/main/java/ru/timchat/realtime/security/WsAuthService.java
```

#### 14. Suggested commit message

```
feat: add WebSocket realtime foundation with protocol, dispatcher, and connection registry
```

#### 15. Exact stage implementation prompt

```
Read and follow all cursor rules from .cursor/rules/.
Read the WS protocol spec from the always-applied rule timchat-realtime-and-data.mdc.

Read the implementation plan: docs/implementation/IMPLEMENTATION_PLAN.md — execute Stage 11 only.

Create the WebSocket realtime foundation for TimChat backend:

TRANSPORT:
1. Create WebSocketConfig: register WebSocket endpoint at /ws
2. Create WebSocketHandshakeInterceptor: extract JWT from query param "token", validate, store userId in session attributes
3. Create TimChatWebSocketHandler extends TextWebSocketHandler:
   - afterConnectionEstablished: register in ConnectionRegistry
   - handleTextMessage: parse → dispatch
   - afterConnectionClosed: unregister from ConnectionRegistry

PROTOCOL:
4. Create ProtocolEnvelope record:
   int protocolVersion, String messageId, String correlationId, String type, ProtocolScope scope, Instant sentAt, String clientSessionId, JsonNode payload

5. Create ProtocolScope enum: SYSTEM, WORKSPACE, CHANNEL, CHAT, PRESENCE, CALL, MODERATION, MEDIA

6. Create ProtocolParser:
   - parse(String json) → ProtocolEnvelope (validate all required fields, reject invalid)
   - Use Jackson ObjectMapper

DISPATCH:
7. Create CommandHandler interface:
   - boolean supports(ProtocolScope scope)
   - void handle(ProtocolEnvelope envelope, WebSocketSession session)

8. Create MessageDispatcher:
   - Inject List<CommandHandler> (Spring auto-collects)
   - dispatch(ProtocolEnvelope, WebSocketSession) — find handler by scope, delegate

9. Create stub handler implementations:
   - SystemCommandHandler (scope: SYSTEM) — just log and acknowledge
   - ChatCommandHandler (scope: CHAT) — stub
   - PresenceCommandHandler (scope: PRESENCE) — stub
   - CallCommandHandler (scope: CALL) — stub
   - ModerationCommandHandler (scope: MODERATION) — stub

REGISTRY:
10. Create ConnectionRegistry (Redis-backed):
    - register(connectionId, userId, clientSessionId)
    - unregister(connectionId)
    - getConnectionsByUserId(userId) → Set<connectionId>
    - getUserIdByConnection(connectionId) → userId
    - Use Spring Data Redis or RedisTemplate

11. Create ClientSessionManager:
    - createSession(userId) → clientSessionId
    - getSession(clientSessionId) → session info
    - removeSession(clientSessionId)

SECURITY:
12. Create WsAuthService: validate per-command authorization (for now, just verify user is authenticated)

13. Write unit tests for ProtocolParser and MessageDispatcher.

After completing, update docs/implementation/EXECUTION_LOG.md with Stage 11 results.
```

---

### Stage 12: Realtime Chat & Presence Backend

#### 1. Objective

Реализовать ChatCommandHandler (send_message через WS + fanout), PresenceCommandHandler (heartbeat, status change), Redis presence projections.

#### 2. In-scope work

- ChatCommandHandler: handle send_message via WS, fanout to channel subscribers
- Channel subscription management in Redis
- PresenceCommandHandler: heartbeat, status update
- Presence model: online/offline/idle/dnd
- Redis presence projection (userId → status + lastSeen)
- Fanout on meaningful state change only

#### 3. Explicit out-of-scope

- Frontend integration (Stage 13)
- Call handlers (Stage 15)
- Moderation handlers (Stage 16)
- Reconnect (Stage 14)

#### 4. Backend work

- `ru.timchat.realtime.handler.ChatCommandHandler` — full implementation:
  - handle type `send_message`: validate, persist via `MessageService`, fanout to channel subscribers
  - handle type `subscribe_channel`: add user to channel subscription in Redis
  - handle type `unsubscribe_channel`: remove from subscription
- `ru.timchat.realtime.handler.PresenceCommandHandler` — full implementation:
  - handle type `heartbeat`: update presence in Redis
  - handle type `set_status`: update user status (online/idle/dnd)
- `ru.timchat.presence.domain.PresenceStatus` — enum (ONLINE, OFFLINE, IDLE, DND)
- `ru.timchat.presence.application.PresenceService`:
  - updatePresence(userId, status)
  - getPresence(userId) → PresenceStatus
  - getPresenceForUsers(userIds) → Map
  - markOffline(userId)
- `ru.timchat.presence.infra.RedisPresenceProjection` — Redis hash for presence data
- `ru.timchat.realtime.fanout.ChannelFanoutService` — send message to all subscribers of a channel
- `ru.timchat.realtime.subscription.ChannelSubscriptionRegistry` — Redis set per channel: who is subscribed

#### 5. Frontend work

None.

#### 6. Infra work

None.

#### 7. Test work

- Integration: send_message via WS → verify message persisted and fanout delivered
- Unit: PresenceService correctly tracks status
- Integration: heartbeat keeps user ONLINE, no heartbeat → OFFLINE after timeout

#### 8. Main risks

- Fanout performance with many channel subscribers
- Presence heartbeat timeout tuning
- Redis key expiry for presence TTL

#### 9. Expected implementation gaps

- No frontend consuming these events yet
- Presence timeout/cleanup may need background scheduler

#### 10. What must be fixed before done

- send_message via WS persists message and fans out to subscribers
- Presence updates and queries work
- Channel subscription add/remove works

#### 11. Acceptance criteria

- [ ] WS send_message → message persisted in PostgreSQL
- [ ] WS send_message → all channel subscribers receive the message via WS
- [ ] subscribe_channel adds user to Redis set
- [ ] unsubscribe_channel removes user
- [ ] heartbeat updates presence to ONLINE
- [ ] getPresence returns correct status
- [ ] User marked OFFLINE after heartbeat timeout

#### 12. Manual verification checklist

- [ ] Connect 2 WS clients → both subscribe to same channel → one sends → other receives
- [ ] Send heartbeat → presence shows ONLINE
- [ ] Stop heartbeat → eventually shows OFFLINE
- [ ] Set status to DND → presence shows DND

#### 13. Files/modules likely to change

```
backend/src/main/java/ru/timchat/realtime/handler/ChatCommandHandler.java
backend/src/main/java/ru/timchat/realtime/handler/PresenceCommandHandler.java
backend/src/main/java/ru/timchat/presence/domain/PresenceStatus.java
backend/src/main/java/ru/timchat/presence/application/PresenceService.java
backend/src/main/java/ru/timchat/presence/infra/RedisPresenceProjection.java
backend/src/main/java/ru/timchat/realtime/fanout/ChannelFanoutService.java
backend/src/main/java/ru/timchat/realtime/subscription/ChannelSubscriptionRegistry.java
```

#### 14. Suggested commit message

```
feat: implement realtime chat delivery via WebSocket and presence tracking
```

#### 15. Exact stage implementation prompt

```
Read and follow all cursor rules from .cursor/rules/.

Read the implementation plan: docs/implementation/IMPLEMENTATION_PLAN.md — execute Stage 12 only.

Implement realtime chat delivery and presence for TimChat backend:

CHAT HANDLER:
1. Implement ChatCommandHandler (upgrade from stub):
   - Type "send_message": extract channelId, content from payload
     - Check MESSAGE_WRITE permission
     - Persist message via MessageService
     - Build outbound event with message data
     - Fanout to all channel subscribers via ChannelFanoutService
   - Type "subscribe_channel": add connection to channel's subscriber set in Redis
     - Check CHANNEL_VIEW permission
   - Type "unsubscribe_channel": remove connection from set

2. Create ChannelFanoutService:
   - sendToChannel(channelId, outboundEvent): get all subscriber connections from ChannelSubscriptionRegistry, send WS message to each
   - Use ConnectionRegistry to resolve connectionId → WebSocketSession

3. Create ChannelSubscriptionRegistry (Redis-backed):
   - subscribe(channelId, connectionId)
   - unsubscribe(channelId, connectionId)
   - getSubscribers(channelId) → Set<connectionId>
   - Cleanup on disconnect

PRESENCE:
4. Create PresenceStatus enum in ru.timchat.presence.domain: ONLINE, OFFLINE, IDLE, DND

5. Create PresenceService in ru.timchat.presence.application:
   - updatePresence(userId, PresenceStatus) — write to Redis with TTL
   - getPresence(userId) → PresenceStatus (return OFFLINE if key missing)
   - getPresenceForUsers(Set<UUID>) → Map<UUID, PresenceStatus>
   - markOffline(userId) — delete Redis key

6. Create RedisPresenceProjection:
   - Store as Redis key: "presence:{userId}" → status, with TTL (e.g., 90 seconds)
   - Heartbeat refreshes TTL

7. Implement PresenceCommandHandler:
   - Type "heartbeat": call presenceService.updatePresence(userId, ONLINE)
   - Type "set_status": call presenceService.updatePresence(userId, requestedStatus)

8. On WebSocket disconnect: mark user OFFLINE, cleanup subscriptions

9. Create outbound event envelope for server→client messages (same structure as inbound but with server-generated messageId)

10. Write tests for chat fanout and presence tracking.

After completing, update docs/implementation/EXECUTION_LOG.md with Stage 12 results.
```

---

### Stage 13: Frontend Realtime & Presence

#### 1. Objective

Создать RealtimeGateway (WebSocket client), интегрировать live chat updates, presence indicators, connection state management.

#### 2. In-scope work

- RealtimeGateway service (WebSocket client, protocol handling)
- Protocol envelope model (TypeScript)
- Chat realtime updates (new messages appear without refresh)
- PresenceStore + presence indicators on member list
- Connection state management (connected/disconnected/reconnecting)

#### 3. Explicit out-of-scope

- Reconnect with snapshot restore (Stage 14)
- Call realtime events (Stage 17)
- Moderation realtime events (Stage 17)

#### 4. Backend work

None.

#### 5. Frontend work

- `src/app/core/realtime/realtime-gateway.service.ts` — WebSocket connection, send/receive, auto-reconnect (basic)
- `src/app/core/realtime/protocol.model.ts` — ProtocolEnvelope, ProtocolScope types
- `src/app/core/realtime/protocol.builder.ts` — envelope builder
- `src/app/features/presence/presence.store.ts` — PresenceStore
- `src/app/features/presence/presence-indicator.component.ts` — online/offline/idle/dnd dot
- Update ChatStore to handle incoming WS messages
- Update ChatTimeline to show new messages in realtime
- Update sidebar/member list with presence indicators
- Connection status indicator in top bar

#### 6. Infra work

None.

#### 7. Test work

- Unit: RealtimeGateway connects, sends, receives
- Unit: PresenceStore updates on presence events
- Integration: send message → appears in other client's timeline without refresh

#### 8. Main risks

- WebSocket reconnection reliability
- Message ordering on concurrent sends
- Presence status update latency

#### 9. Expected implementation gaps

- Basic auto-reconnect only (full reconnect with snapshot in Stage 14)
- No call or moderation events handled yet

#### 10. What must be fixed before done

- WebSocket connects on app load
- New messages appear in realtime in chat timeline
- Presence indicators show correct status
- Connection loss shows visual indicator

#### 11. Acceptance criteria

- [ ] WebSocket connects on login
- [ ] New messages from other users appear in realtime
- [ ] Presence indicators show ONLINE/OFFLINE/IDLE/DND
- [ ] Connection status indicator shows connected/disconnected
- [ ] Heartbeat keeps user ONLINE
- [ ] Auto-reconnect on connection drop (basic)

#### 12. Manual verification checklist

- [ ] Open two browser tabs → send message in one → appears in other immediately
- [ ] Presence dot shows green for online users
- [ ] Kill backend → frontend shows disconnected indicator
- [ ] Restart backend → frontend auto-reconnects

#### 13. Files/modules likely to change

```
frontend/src/app/core/realtime/realtime-gateway.service.ts
frontend/src/app/core/realtime/protocol.model.ts
frontend/src/app/core/realtime/protocol.builder.ts
frontend/src/app/features/presence/presence.store.ts
frontend/src/app/features/presence/presence-indicator.component.ts
frontend/src/app/features/chat/chat.store.ts
frontend/src/app/features/chat/chat-timeline.component.ts
frontend/src/app/core/layout/top-bar.component.ts
frontend/src/app/core/layout/sidebar.component.ts
```

#### 14. Suggested commit message

```
feat: integrate WebSocket realtime gateway, live chat updates, and presence indicators
```

#### 15. Exact stage implementation prompt

```
Read and follow all cursor rules from .cursor/rules/.
Read docs/frontend/FRONTEND_UI_DIRECTION.md.

Read the implementation plan: docs/implementation/IMPLEMENTATION_PLAN.md — execute Stage 13 only.

Integrate realtime WebSocket and presence into TimChat frontend:

REALTIME GATEWAY:
1. Create RealtimeGateway service in core/realtime/:
   - Connect to ws://localhost:8080/ws?token=JWT
   - Send/receive ProtocolEnvelope JSON
   - Observable streams for incoming messages by scope
   - Connection state: CONNECTING, CONNECTED, DISCONNECTED, RECONNECTING
   - Basic auto-reconnect with exponential backoff (1s, 2s, 4s, max 30s)
   - Heartbeat: send presence heartbeat every 30 seconds

2. Create ProtocolEnvelope TypeScript interface:
   protocolVersion, messageId, correlationId, type, scope, sentAt, clientSessionId, payload

3. Create envelope builder: helper to construct outbound envelopes with auto-generated messageId (UUID)

CHAT INTEGRATION:
4. Update ChatStore:
   - On entering channel: send subscribe_channel via WS
   - On leaving channel: send unsubscribe_channel
   - Listen for incoming chat events → prepend/append new messages to timeline
   - Send messages via WS (send_message) instead of REST POST (or keep both — WS for realtime, REST as fallback)

5. Update ChatTimeline: messages appear in realtime without refresh

PRESENCE:
6. Create PresenceStore in features/presence/:
   - Signal-based: presenceMap (userId → status)
   - Listen for presence events from RealtimeGateway
   - Provide getPresence(userId) → signal

7. Create PresenceIndicator component:
   - Small colored dot: green=ONLINE, gray=OFFLINE, yellow=IDLE, red=DND
   - Input: userId
   - Reads from PresenceStore

8. Add presence indicators to member list in sidebar/right panel

CONNECTION STATUS:
9. Add connection status indicator to top bar:
   - Green dot when connected
   - Yellow/pulsing when reconnecting
   - Red when disconnected

After completing, update docs/implementation/EXECUTION_LOG.md with Stage 13 results.
```

---

### Stage 14: Reconnect

#### 1. Objective

Реализовать snapshot-based reconnect: при переподключении клиент получает authoritative snapshot текущего состояния.

#### 2. In-scope work

- ReconnectToken generation and validation
- Snapshot assembly: current channel subscriptions, presence, active call state (if any)
- SystemCommandHandler: `resume_session` command
- Frontend reconnect flow: detect disconnect → reconnect → send resume_session → apply snapshot

#### 3. Explicit out-of-scope

- Replay-based catch-up
- Event journal
- `lastSeenEventId` tracking

#### 4. Backend work

- `ru.timchat.realtime.handler.SystemCommandHandler` — handle `resume_session`:
  - Validate clientSessionId and resumeToken
  - Build snapshot: user's subscribed channels, presence of relevant users, active call in current channel (if any)
  - Send snapshot response
- `ru.timchat.realtime.registry.ReconnectTokenService`:
  - generateToken(clientSessionId) → token (stored in Redis with TTL)
  - validateToken(clientSessionId, token) → boolean
- Snapshot assembly from Redis (primary) + PostgreSQL (fallback)

#### 5. Frontend work

- Update RealtimeGateway: on reconnect, send `resume_session` with clientSessionId + resumeToken
- Apply snapshot: update ChatStore, PresenceStore, (later) CallStore
- Store clientSessionId and resumeToken in memory

#### 6. Infra work

None.

#### 7. Test work

- Integration: connect → get session → disconnect → reconnect with token → receive snapshot
- Test: invalid token → rejection
- Test: expired token → rejection

#### 8. Main risks

- Snapshot assembly must be consistent (no partial state)
- ReconnectToken TTL must balance usability vs security
- Snapshot size with many channel subscriptions

#### 9. Expected implementation gaps

- Call state in snapshot is empty until Stage 15 is implemented
- Snapshot is basic (channels + presence only for now)

#### 10. What must be fixed before done

- Reconnect flow works end-to-end
- Client receives consistent snapshot
- Invalid/expired tokens are rejected

#### 11. Acceptance criteria

- [ ] On initial connect, server issues clientSessionId + resumeToken
- [ ] On disconnect + reconnect, resume_session restores state
- [ ] Snapshot contains channel subscriptions and presence
- [ ] Invalid resumeToken → rejected
- [ ] Expired resumeToken → rejected, full re-initialization required

#### 12. Manual verification checklist

- [ ] Connect → note session info → kill WS → reconnect with token → state restored
- [ ] Use expired token → rejected
- [ ] Use invalid token → rejected
- [ ] After reconnect, messages still flow in realtime

#### 13. Files/modules likely to change

```
backend/src/main/java/ru/timchat/realtime/handler/SystemCommandHandler.java
backend/src/main/java/ru/timchat/realtime/registry/ReconnectTokenService.java
backend/src/main/java/ru/timchat/realtime/snapshot/SnapshotAssembler.java
frontend/src/app/core/realtime/realtime-gateway.service.ts
frontend/src/app/features/chat/chat.store.ts
frontend/src/app/features/presence/presence.store.ts
```

#### 14. Suggested commit message

```
feat: add snapshot-based reconnect with resume_session and token validation
```

#### 15. Exact stage implementation prompt

```
Read and follow all cursor rules from .cursor/rules/.

Read the implementation plan: docs/implementation/IMPLEMENTATION_PLAN.md — execute Stage 14 only.

Implement snapshot-based reconnect for TimChat:

BACKEND:
1. Create ReconnectTokenService in ru.timchat.realtime.registry:
   - generateToken(clientSessionId) → String token (store in Redis with TTL, e.g., 5 minutes)
   - validateAndConsume(clientSessionId, token) → boolean (one-time use)

2. Create SnapshotAssembler in ru.timchat.realtime.snapshot:
   - assembleSnapshot(userId, clientSessionId):
     - Get user's active channel subscriptions from Redis
     - Get presence of relevant users (workspace members)
     - Get active call state in subscribed voice/video channels (placeholder for now)
     - Return SnapshotPayload with all this data

3. Implement SystemCommandHandler — handle type "resume_session":
   - Extract clientSessionId, resumeToken from payload
   - Validate token via ReconnectTokenService
   - If valid: re-register connection, assemble snapshot, send snapshot response
   - If invalid: send error, require full re-initialization

4. On initial connection established:
   - Generate clientSessionId
   - Generate resumeToken
   - Send "session_init" event to client with clientSessionId + resumeToken

5. On connection close: keep session data in Redis for TTL period (allow reconnect)

FRONTEND:
6. Update RealtimeGateway:
   - On "session_init" event: store clientSessionId, resumeToken
   - On disconnect: attempt reconnect
   - On reconnect: send resume_session with stored credentials
   - On snapshot received: dispatch to stores (ChatStore, PresenceStore)
   - On resume rejected: clear stored credentials, do full re-initialization (resubscribe channels, etc.)

7. Write integration tests: connect → disconnect → reconnect → snapshot received.

After completing, update docs/implementation/EXECUTION_LOG.md with Stage 14 results.
```

---

### Stage 15: Call Session & Room Control

#### 1. Objective

Создать call-модуль: CallSession lifecycle, join/leave, self/effective state, call snapshots, one active call per channel.

#### 2. In-scope work

- CallSession entity (lifecycle: ACTIVE, ENDED)
- CallParticipant entity
- ParticipantSelfState / ParticipantEffectiveState
- Join/leave call lifecycle
- One active call per channel enforcement
- CallCommandHandler (WS)
- LiveCallSnapshot in Redis
- CallJoinTicket
- Liquibase migrations

#### 3. Explicit out-of-scope

- WebRTC media connection (Stage 17)
- TURN credentials (Stage 16)
- Moderation enforcement on calls (Stage 16)
- Frontend call UI (Stage 17)

#### 4. Backend work

- `ru.timchat.call.domain.CallSession` — entity (id, channelId, status, startedAt, endedAt, startedBy)
- `ru.timchat.call.domain.CallSessionStatus` — enum (ACTIVE, ENDED)
- `ru.timchat.call.domain.CallParticipant` — entity (id, callSessionId, userId, joinedAt, leftAt)
- `ru.timchat.call.domain.ParticipantSelfState` — value object (selfMuted, selfVideoEnabled, selfScreenShareEnabled)
- `ru.timchat.call.domain.ParticipantEffectiveState` — value object (effectiveMuted, effectiveVideoEnabled, effectiveScreenShareEnabled)
- `ru.timchat.call.domain.CallJoinTicket` — entity or transient (userId, callSessionId, permissions snapshot)
- Repositories
- `ru.timchat.call.application.CallService`:
  - joinCall(channelId, userId) → join existing or create new session
  - leaveCall(callSessionId, userId) → if last participant, end session
  - updateSelfState(callSessionId, userId, selfState) → recompute effective state
  - getCallSnapshot(callSessionId) → snapshot of all participants + effective states
- `ru.timchat.call.infra.RedisCallProjection` — live call snapshot in Redis
- `ru.timchat.realtime.handler.CallCommandHandler` — full implementation:
  - join_call, leave_call, update_self_state
  - Fanout: participant_joined, participant_left, state_updated events to all call participants
- Liquibase: call_sessions, call_participants tables

#### 5. Frontend work

None.

#### 6. Infra work

None.

#### 7. Test work

- Unit: call session lifecycle (create, join, leave, end)
- Unit: one active call per channel enforcement
- Unit: effective state computation (without moderation — basic version)
- Integration: join → get snapshot → leave → session ended

#### 8. Main risks

- Race condition on concurrent joins creating multiple sessions
- Last participant detection for session close
- Effective state computation must be correct even with concurrent updates

#### 9. Expected implementation gaps

- Effective state does not include moderation constraints yet (added in Stage 16)
- No TURN credentials (Stage 16)
- No frontend UI (Stage 17)

#### 10. What must be fixed before done

- Join/leave works correctly
- One active call per channel enforced
- Self state updates produce correct effective state (basic)
- Live snapshot in Redis is accurate

#### 11. Acceptance criteria

- [ ] join_call on channel without active session → creates session + joins
- [ ] join_call on channel with active session → joins existing
- [ ] Second join_call attempt by same user → idempotent
- [ ] leave_call removes participant
- [ ] Last participant leaves → session status ENDED
- [ ] Attempt to create second session on same channel → denied
- [ ] update_self_state changes effective state
- [ ] Call snapshot contains all participants + effective states

#### 12. Manual verification checklist

- [ ] Send join_call → session created → participant added
- [ ] Second user join_call → same session, two participants
- [ ] update_self_state selfMuted=true → effective muted=true
- [ ] First user leave → one participant remains, session still ACTIVE
- [ ] Second user leave → session ENDED

#### 13. Files/modules likely to change

```
backend/src/main/java/ru/timchat/call/domain/CallSession.java
backend/src/main/java/ru/timchat/call/domain/CallSessionStatus.java
backend/src/main/java/ru/timchat/call/domain/CallParticipant.java
backend/src/main/java/ru/timchat/call/domain/ParticipantSelfState.java
backend/src/main/java/ru/timchat/call/domain/ParticipantEffectiveState.java
backend/src/main/java/ru/timchat/call/domain/CallJoinTicket.java
backend/src/main/java/ru/timchat/call/domain/*Repository.java
backend/src/main/java/ru/timchat/call/application/CallService.java
backend/src/main/java/ru/timchat/call/infra/RedisCallProjection.java
backend/src/main/java/ru/timchat/realtime/handler/CallCommandHandler.java
backend/src/main/resources/db/changelog/changes/007-create-calls.yaml
```

#### 14. Suggested commit message

```
feat: add call session module with lifecycle, self/effective state, and room snapshots
```

#### 15. Exact stage implementation prompt

```
Read and follow all cursor rules from .cursor/rules/.

Read the implementation plan: docs/implementation/IMPLEMENTATION_PLAN.md — execute Stage 15 only.

Create the call session and room control module for TimChat backend:

DOMAIN:
1. Create CallSessionStatus enum: ACTIVE, ENDED

2. Create CallSession entity:
   - UUID id, UUID channelId, CallSessionStatus status, Instant startedAt, Instant endedAt, UUID startedBy
   - Domain method: end() — set status ENDED, set endedAt

3. Create CallParticipant entity:
   - UUID id, UUID callSessionId (FK), UUID userId, Instant joinedAt, Instant leftAt (nullable)
   - Domain method: leave() — set leftAt

4. Create ParticipantSelfState (value object or embeddable):
   - boolean selfMuted, boolean selfVideoEnabled, boolean selfScreenShareEnabled

5. Create ParticipantEffectiveState (value object):
   - boolean effectiveMuted, boolean effectiveVideoEnabled, boolean effectiveScreenShareEnabled
   - Static factory: computeFrom(selfState, serverConstraints) — for now serverConstraints is empty, effective = self

6. Create CallJoinTicket: userId, callSessionId, allowed permissions snapshot

APPLICATION:
7. Create CallService:
   - joinCall(channelId, userId):
     - Check ROOM_JOIN permission
     - Find active session for channel (or create new)
     - Enforce ONE active session per channel
     - Create CallParticipant
     - Update Redis projection
     - Return join result with snapshot
   - leaveCall(callSessionId, userId):
     - Mark participant as left
     - If no active participants remain → end session
     - Update Redis projection
   - updateSelfState(callSessionId, userId, ParticipantSelfState):
     - Update self state
     - Recompute effective state
     - Update Redis projection
     - Return new effective state
   - getCallSnapshot(callSessionId):
     - All active participants with effective states

8. Create RedisCallProjection:
   - Store live call snapshot as Redis hash
   - Key: "call:{channelId}:active" → session info + participants

WS HANDLER:
9. Implement CallCommandHandler:
   - Type "join_call": delegate to CallService.joinCall, fanout participant_joined
   - Type "leave_call": delegate to CallService.leaveCall, fanout participant_left
   - Type "update_self_state": delegate to CallService.updateSelfState, fanout state_updated

10. Liquibase migrations for call_sessions, call_participants tables

11. Write unit tests: lifecycle, one-session-per-channel, effective state computation.

After completing, update docs/implementation/EXECUTION_LOG.md with Stage 15 results.
```

---

### Stage 16: Media & Moderation Backend

#### 1. Objective

Добавить TURN credential service, SFU adapter interface, moderation enforcement (mute/kick/block), effective state с учётом moderation.

#### 2. In-scope work

- TURN credential service
- SFU adapter interface (no concrete implementation)
- ModerationAction entity
- Mute/force-mute/kick/block enforcement
- Effective state recomputation with moderation constraints
- ModerationCommandHandler (WS)
- Permission checks: ROOM_MODERATE, ROOM_FORCE_MUTE, ROOM_KICK

#### 3. Explicit out-of-scope

- Concrete SFU integration (LiveKit/Janus/etc.)
- Frontend moderation UI (Stage 17)
- Audit logging of moderation actions (Stage 18)

#### 4. Backend work

- `ru.timchat.media.application.TurnCredentialService` — generate time-limited TURN credentials
- `ru.timchat.media.application.SfuAdapter` — interface (joinRoom, leaveRoom, muteTrack — stubs)
- `ru.timchat.media.api.MediaController` — REST endpoint for TURN credentials
- `ru.timchat.moderation.domain.ModerationAction` — entity (id, callSessionId, targetUserId, actionType, performedBy, createdAt, expiresAt)
- `ru.timchat.moderation.domain.ModerationActionType` — enum (FORCE_MUTE, KICK, BLOCK_VIDEO, BLOCK_SCREEN_SHARE, BLOCK_SPEAK)
- `ru.timchat.moderation.domain.ModerationActionRepository`
- `ru.timchat.moderation.application.ModerationService`:
  - forceMute(callSessionId, targetUserId, performedBy)
  - kick(callSessionId, targetUserId, performedBy)
  - blockCapability(callSessionId, targetUserId, capability, performedBy)
  - getActiveConstraints(callSessionId, userId) → server constraints
- Update `ParticipantEffectiveState.computeFrom()` to include server constraints from moderation
- `ru.timchat.realtime.handler.ModerationCommandHandler` — full implementation
- Liquibase: moderation_actions table

#### 5. Frontend work

None.

#### 6. Infra work

- Add coturn to `infra/docker-compose.yml`: STUN/TURN server on port 3478 (UDP/TCP), realm `timchat`, static auth secret for HMAC credentials
- Add TURN config to `application.yml`: turn.server.url, turn.server.secret, turn.credential.ttl

#### 7. Test work

- Unit: effective state with moderation (selfMuted=false + serverMuted=true → effectiveMuted=true)
- Unit: kick removes participant from call
- Integration: force_mute → effective state updated → fanout to participants
- Unit: TurnCredentialService generates valid HMAC credentials

#### 8. Main risks

- Moderation actions must immediately affect effective state
- Kicked user must be disconnected from call
- Permission checks for moderation actions must be strict
- coturn shared secret must match TurnCredentialService HMAC config

#### 9. Expected implementation gaps

- SFU adapter is interface-only (no real SFU)
- No audit trail yet (Stage 18)

#### 10. What must be fixed before done

- Moderation actions change effective state correctly
- Kick removes participant
- Permission checks enforced (only ROOM_MODERATE/ROOM_FORCE_MUTE/ROOM_KICK holders can moderate)
- TURN credential endpoint works

#### 11. Acceptance criteria

- [ ] force_mute → target's effectiveMuted becomes true regardless of selfMuted
- [ ] kick → target removed from call, receives kick notification
- [ ] block_video → target's effectiveVideoEnabled becomes false
- [ ] Moderation requires ROOM_MODERATE or specific permission
- [ ] TURN credential endpoint returns valid credentials
- [ ] Effective state correctly combines self + moderation constraints

#### 12. Manual verification checklist

- [ ] Moderator sends force_mute → target sees effectiveMuted=true
- [ ] Moderator kicks user → user removed from call
- [ ] Non-moderator attempts force_mute → 403 / denied
- [ ] GET /api/media/turn-credentials → returns credentials

#### 13. Files/modules likely to change

```
backend/src/main/java/ru/timchat/media/application/TurnCredentialService.java
backend/src/main/java/ru/timchat/media/application/SfuAdapter.java
backend/src/main/java/ru/timchat/media/api/MediaController.java
backend/src/main/java/ru/timchat/moderation/domain/ModerationAction.java
backend/src/main/java/ru/timchat/moderation/domain/ModerationActionType.java
backend/src/main/java/ru/timchat/moderation/domain/ModerationActionRepository.java
backend/src/main/java/ru/timchat/moderation/application/ModerationService.java
backend/src/main/java/ru/timchat/realtime/handler/ModerationCommandHandler.java
backend/src/main/java/ru/timchat/call/domain/ParticipantEffectiveState.java
backend/src/main/resources/db/changelog/changes/008-create-moderation.yaml
backend/src/main/resources/application.yml (TURN config)
infra/docker-compose.yml (coturn)
```

#### 14. Suggested commit message

```
feat: add media TURN credentials, SFU adapter interface, and moderation enforcement
```

#### 15. Exact stage implementation prompt

```
Read and follow all cursor rules from .cursor/rules/.

Read the implementation plan: docs/implementation/IMPLEMENTATION_PLAN.md — execute Stage 16 only.

Add media integration and moderation enforcement to TimChat backend:

INFRA:
0. Add coturn to infra/docker-compose.yml:
   - Image: coturn/coturn:latest
   - Ports: 3478/tcp, 3478/udp
   - Realm: timchat
   - Static auth secret: timchat-turn-secret (for HMAC-based temporary credentials)
   - No TLS for local dev

MEDIA:
1. Create TurnCredentialService in ru.timchat.media.application:
   - generateCredentials(userId) → TurnCredentials (urls, username, credential, ttl)
   - Use HMAC-based temporary credentials (standard TURN REST API / RFC 5766)
   - username = "{expiryTimestamp}:{userId}", credential = HMAC-SHA1(username, sharedSecret)
   - Configure from application.yml: timchat.turn.urls, timchat.turn.secret, timchat.turn.ttl-seconds
   - TTL default: 86400 seconds (24 hours)

2. Create SfuAdapter interface in ru.timchat.media.application:
   - joinRoom(roomId, userId, offer) → answer (stub)
   - leaveRoom(roomId, userId) (stub)
   - No concrete implementation — just the interface

3. Create MediaController:
   - GET /api/media/turn-credentials — requires authentication, returns TURN credentials (urls, username, credential, ttl)

MODERATION:
4. Create ModerationActionType enum: FORCE_MUTE, KICK, BLOCK_VIDEO, BLOCK_SCREEN_SHARE, BLOCK_SPEAK

5. Create ModerationAction entity:
   - UUID id, UUID callSessionId, UUID targetUserId, ModerationActionType actionType
   - UUID performedBy, Instant createdAt, Instant expiresAt (nullable), boolean active

6. Create ModerationService:
   - forceMute(callSessionId, targetUserId, performedBy):
     - Check ROOM_FORCE_MUTE permission
     - Create ModerationAction
     - Recompute effective state for target
   - kick(callSessionId, targetUserId, performedBy):
     - Check ROOM_KICK permission
     - Remove participant via CallService
     - Send kick notification
   - blockCapability(callSessionId, targetUserId, type, performedBy):
     - Check ROOM_MODERATE permission
     - Create ModerationAction
     - Recompute effective state
   - getActiveConstraints(callSessionId, userId) → ServerConstraints:
     - Query active ModerationActions for this user in this call
     - Build: serverMuted, serverVideoBlocked, serverScreenShareBlocked

7. Update ParticipantEffectiveState.computeFrom(selfState, serverConstraints):
   - effectiveMuted = selfState.selfMuted || serverConstraints.serverMuted
   - effectiveVideoEnabled = selfState.selfVideoEnabled && !serverConstraints.serverVideoBlocked
   - effectiveScreenShareEnabled = selfState.selfScreenShareEnabled && !serverConstraints.serverScreenShareBlocked

8. Implement ModerationCommandHandler:
   - Type "force_mute": delegate to ModerationService
   - Type "kick": delegate to ModerationService
   - Type "block_capability": delegate to ModerationService
   - Fanout: moderation_applied, participant_kicked events

9. Liquibase migration for moderation_actions table

10. Write tests: effective state with moderation, kick flow, permission checks.

After completing, update docs/implementation/EXECUTION_LOG.md with Stage 16 results.
```

---

### Stage 17: Frontend Call & Moderation

#### 1. Objective

Создать frontend call panel, self-state controls, WebRTC media integration, moderation controls.

#### 2. In-scope work

- CallStore (signal-based state)
- Call panel UI in main area
- Self-state controls (mute/video/screen share toggles)
- Participant list with effective states
- RtcMediaService (WebRTC peer connection)
- DeviceService (camera/mic selection)
- Moderation controls (force mute, kick — for moderators)
- Permission-aware UI (show/hide actions based on permissions)

#### 3. Explicit out-of-scope

- Actual SFU connection (depends on SFU choice)
- Recording
- Screen sharing implementation details

#### 4. Backend work

None.

#### 5. Frontend work

- `features/call/call.store.ts` — CallStore: activeCall, participants, selfState, effectiveState
- `features/call/call-panel.component.ts` — main call UI
- `features/call/call-controls.component.ts` — mute/video/screen share toggles
- `features/call/participant-list.component.ts` — list with effective states
- `features/call/participant-tile.component.ts` — single participant tile
- `features/call/join-call-button.component.ts` — join/leave call button
- `core/realtime/rtc-media.service.ts` — RtcMediaService: getUserMedia, createPeerConnection
- `core/realtime/device.service.ts` — DeviceService: enumerate devices, select camera/mic
- `features/call/moderation-controls.component.ts` — force mute, kick buttons
- Update RealtimeGateway to handle call scope events
- Update right panel to show call participants and moderation

#### 6. Infra work

None.

#### 7. Test work

- Component tests: call panel renders, controls work
- Integration: join call → see participants → toggle mute → see state change

#### 8. Main risks

- WebRTC getUserMedia browser compatibility
- Call state sync between WS events and local state
- UI responsiveness during call

#### 9. Expected implementation gaps

- No actual SFU — media tracks won't connect peer-to-peer in MVP without SFU
- Screen share may be UI-only without real sharing

#### 10. What must be fixed before done

- Call panel renders correctly
- Self-state toggles send WS commands and reflect effective state
- Participant list shows all participants with correct states
- Moderation controls visible only to moderators

#### 11. Acceptance criteria

- [ ] Join call button works → enters call → call panel shows
- [ ] Leave call button works → exits call
- [ ] Mute toggle sends update_self_state → reflects effective state
- [ ] Video toggle works similarly
- [ ] Participant list shows all participants with status indicators
- [ ] Moderator sees force mute / kick controls
- [ ] Non-moderator does not see moderation controls
- [ ] Camera/mic selection works

#### 12. Manual verification checklist

- [ ] Click join on voice channel → call panel appears
- [ ] Toggle mute → icon changes, effective state updates
- [ ] Second user joins → appears in participant list
- [ ] Moderator force mutes user → user's effective state shows muted
- [ ] Click leave → call panel hides

#### 13. Files/modules likely to change

```
frontend/src/app/features/call/call.store.ts
frontend/src/app/features/call/call-panel.component.ts
frontend/src/app/features/call/call-controls.component.ts
frontend/src/app/features/call/participant-list.component.ts
frontend/src/app/features/call/participant-tile.component.ts
frontend/src/app/features/call/join-call-button.component.ts
frontend/src/app/features/call/moderation-controls.component.ts
frontend/src/app/core/realtime/rtc-media.service.ts
frontend/src/app/core/realtime/device.service.ts
frontend/src/app/core/realtime/realtime-gateway.service.ts
frontend/src/app/core/layout/right-panel.component.ts
```

#### 14. Suggested commit message

```
feat: add call panel, self-state controls, WebRTC media, and moderation UI
```

#### 15. Exact stage implementation prompt

```
Read and follow all cursor rules from .cursor/rules/.
Read docs/frontend/FRONTEND_UI_DIRECTION.md and docs/frontend/FRONTEND_LAYOUT_PRINCIPLES.md.

Read the implementation plan: docs/implementation/IMPLEMENTATION_PLAN.md — execute Stage 17 only.

Create the call and moderation UI for TimChat frontend:

CALL STORE:
1. Create CallStore in features/call/:
   - Signals: activeCall, participants, selfState, effectiveState, isInCall
   - joinCall(channelId) → send join_call via WS
   - leaveCall() → send leave_call via WS
   - toggleMute() → update selfState, send update_self_state
   - toggleVideo() → same
   - toggleScreenShare() → same
   - Listen for WS events: participant_joined, participant_left, state_updated, call_ended, moderation_applied, participant_kicked

CALL UI:
2. Create CallPanel component (main area, replaces chat when in call):
   - Participant tiles grid
   - Self-state controls bar at bottom
   - Call info (duration, participant count)

3. Create CallControls component:
   - Mute/unmute button (shows effective state, not self state)
   - Video on/off button
   - Screen share button
   - Leave call button (red)
   - Visual feedback: if force-muted, show lock icon on mute button

4. Create ParticipantList component (right panel):
   - List all participants with: name, presence dot, effective state icons (muted, video, screen share)

5. Create ParticipantTile component:
   - Avatar, name, state indicators (muted icon, video icon)
   - Placeholder for video element (future SFU integration)

6. Create JoinCallButton component:
   - Shows in channel header for VOICE/VIDEO channels
   - "Join" if no active call or call exists but user not in it
   - "Leave" if user is in call
   - Check ROOM_JOIN permission

MEDIA:
7. Create RtcMediaService in core/realtime/:
   - requestMicrophoneAccess() → MediaStream
   - requestCameraAccess() → MediaStream
   - requestScreenShare() → MediaStream
   - stopTrack(track)
   - Manage local MediaStream lifecycle

8. Create DeviceService:
   - enumerateDevices() → { cameras, microphones, speakers }
   - selectCamera(deviceId), selectMicrophone(deviceId)

MODERATION:
9. Create ModerationControls component:
   - Only visible to users with ROOM_MODERATE/ROOM_FORCE_MUTE/ROOM_KICK permissions
   - Per-participant: force mute button, kick button
   - Sends force_mute, kick commands via WS

10. Update right panel to show participants and moderation controls when in call

After completing, update docs/implementation/EXECUTION_LOG.md with Stage 17 results.
```

---

### Stage 18: Audit

#### 1. Objective

Создать audit-модуль: AuditEvent model, audit service, REST endpoint для просмотра, frontend panel.

#### 2. In-scope work

- AuditEvent, AuditActor, AuditTarget, AuditPayload entities
- Audit service — recording audit events
- Audit hooks in moderation, call, permission modules
- REST endpoint for audit query
- Frontend audit panel in right panel

#### 3. Explicit out-of-scope

- Audit data export
- Audit alerts/notifications
- Long-term audit archival

#### 4. Backend work

- `ru.timchat.audit.domain.AuditEvent` — entity (id, workspaceId, eventType, actorId, actorType, targetId, targetType, payload, createdAt)
- `ru.timchat.audit.domain.AuditEventType` — enum (MODERATION_FORCE_MUTE, MODERATION_KICK, CALL_STARTED, CALL_ENDED, PARTICIPANT_JOINED, PARTICIPANT_LEFT, ROLE_ASSIGNED, ROLE_REVOKED, MEMBER_ADDED, MEMBER_REMOVED, etc.)
- `ru.timchat.audit.domain.AuditEventRepository`
- `ru.timchat.audit.application.AuditService`:
  - record(workspaceId, eventType, actorId, targetId, payload)
  - query(workspaceId, filters, pagination) → Page<AuditEvent>
- `ru.timchat.audit.api.AuditController`:
  - GET /api/workspaces/{id}/audit — paginated audit log
- Add audit calls to:
  - ModerationService (force_mute, kick)
  - CallService (call started, call ended, participant join/leave)
  - RoleService (role assigned, role revoked)
  - MembershipService (member added, member removed)
- Liquibase migration for audit_events table

#### 5. Frontend work

- `features/audit/audit-panel.component.ts` — audit log in right contextual panel
- Paginated list of audit events with actor, action, target, timestamp

#### 6. Infra work

None.

#### 7. Test work

- Integration: moderation action → audit event recorded
- Integration: call started → audit event recorded
- Query: filter by event type, date range

#### 8. Main risks

- Audit write performance on high-frequency events
- Audit payload size

#### 9. Expected implementation gaps

- No audit export
- No real-time audit event streaming
- Limited filtering options

#### 10. What must be fixed before done

- All moderation actions produce audit events
- Call lifecycle events produce audit events
- Role/member changes produce audit events
- Audit query endpoint works with pagination

#### 11. Acceptance criteria

- [ ] Force mute → audit event recorded
- [ ] Kick → audit event recorded
- [ ] Call started/ended → audit events recorded
- [ ] Role assigned/revoked → audit events recorded
- [ ] GET /api/workspaces/{id}/audit returns paginated events
- [ ] Frontend audit panel shows events

#### 12. Manual verification checklist

- [ ] Perform moderation action → check audit log → event present
- [ ] Start/end call → audit events present
- [ ] Open audit panel in frontend → see recent events

#### 13. Files/modules likely to change

```
backend/src/main/java/ru/timchat/audit/domain/AuditEvent.java
backend/src/main/java/ru/timchat/audit/domain/AuditEventType.java
backend/src/main/java/ru/timchat/audit/domain/AuditEventRepository.java
backend/src/main/java/ru/timchat/audit/application/AuditService.java
backend/src/main/java/ru/timchat/audit/api/AuditController.java
backend/src/main/java/ru/timchat/audit/api/AuditEventResponse.java
backend/src/main/java/ru/timchat/moderation/application/ModerationService.java
backend/src/main/java/ru/timchat/call/application/CallService.java
backend/src/main/java/ru/timchat/permission/application/RoleService.java
backend/src/main/java/ru/timchat/workspace/application/MembershipService.java
backend/src/main/resources/db/changelog/changes/009-create-audit.yaml
frontend/src/app/features/audit/audit-panel.component.ts
frontend/src/app/core/layout/right-panel.component.ts
```

#### 14. Suggested commit message

```
feat: add audit module with event recording, REST query, and frontend panel
```

#### 15. Exact stage implementation prompt

```
Read and follow all cursor rules from .cursor/rules/.

Read the implementation plan: docs/implementation/IMPLEMENTATION_PLAN.md — execute Stage 18 only.

Create the audit module for TimChat:

BACKEND:
1. Create AuditEventType enum: MODERATION_FORCE_MUTE, MODERATION_KICK, MODERATION_BLOCK, CALL_STARTED, CALL_ENDED, PARTICIPANT_JOINED, PARTICIPANT_LEFT, ROLE_ASSIGNED, ROLE_REVOKED, MEMBER_ADDED, MEMBER_REMOVED, CHANNEL_CREATED, CHANNEL_DELETED

2. Create AuditEvent entity:
   - UUID id, UUID workspaceId, AuditEventType eventType
   - UUID actorId, String actorType (USER, SYSTEM)
   - UUID targetId, String targetType (USER, CALL_SESSION, CHANNEL, WORKSPACE)
   - String payload (JSON string with event-specific details)
   - Instant createdAt

3. Create AuditEventRepository with query methods:
   - findByWorkspaceId with pagination
   - findByWorkspaceIdAndEventType
   - findByWorkspaceIdAndActorId
   - findByWorkspaceIdAndCreatedAtBetween

4. Create AuditService:
   - record(workspaceId, eventType, actorId, targetId, targetType, payload)
   - query(workspaceId, eventType?, actorId?, from?, to?, pageable) → Page<AuditEventResponse>

5. Create AuditController:
   - GET /api/workspaces/{workspaceId}/audit?type=&actor=&from=&to=&page=&size=

6. Add audit hooks:
   - ModerationService: after force_mute → auditService.record(MODERATION_FORCE_MUTE)
   - ModerationService: after kick → auditService.record(MODERATION_KICK)
   - CallService: after session started → auditService.record(CALL_STARTED)
   - CallService: after session ended → auditService.record(CALL_ENDED)
   - RoleService: after role assigned/revoked
   - MembershipService: after member added/removed

7. Liquibase migration for audit_events table (indexed on workspaceId, createdAt)

FRONTEND:
8. Create AuditPanel component for right contextual panel:
   - Paginated list of audit events
   - Show: event type icon, actor name, action description, target name, timestamp
   - Basic filtering by event type

9. Show audit panel when user clicks "Audit" tab in right panel (for workspace admins)

After completing, update docs/implementation/EXECUTION_LOG.md with Stage 18 results.
```

---

### Stage 19: Hardening & Completeness

#### 1. Objective

Закрыть gaps: idempotency, rate limiting, i18n completeness, полная проверка всех 12 permission types end-to-end, channel permission overrides.

#### 2. In-scope work

- Idempotency: duplicate join_call, leave_call, send_message handling
- Rate limiting basics (simple in-memory or Redis-based)
- i18n: verify all error messages have both ru and en translations
- Permission enforcement verification for all 12 types
- Channel permission overrides working end-to-end
- Edge case handling: stale connections, invalid state transitions

#### 3. Explicit out-of-scope

- Performance optimization beyond basics
- Load testing
- Production deployment config

#### 4. Backend work

- Add idempotency key support to send_message (dedupe by key in Redis)
- Handle duplicate join_call (return existing participation, don't create new)
- Handle duplicate leave_call (no-op if already left)
- Simple rate limiter (Redis-based, per-user, for WS commands and REST endpoints)
- Verify all REST error messages have i18n keys
- Integration test: every permission type with allow/deny
- Fix any discovered gaps

#### 5. Frontend work

- Error display improvements (show localized error messages from backend)
- Handle edge cases: join call when already in call, send message to deleted channel
- Connection error retry improvements

#### 6. Infra work

None.

#### 7. Test work

- Behavioural reliability tests: duplicate operations, stale state
- Cross-flow tests: REST + WS state consistency
- All 12 permission types tested end-to-end

#### 8. Main risks

- Discovering fundamental issues late in hardening
- Rate limiter configuration tuning

#### 9. Expected implementation gaps

- Rate limiting is basic (not production-grade)
- Some edge cases may be deferred to post-MVP

#### 10. What must be fixed before done

- No duplicate message creation from same idempotency key
- All 12 permissions enforced correctly
- All error messages localized in ru and en
- Basic rate limiting active

#### 11. Acceptance criteria

- [ ] Duplicate send_message with same key → only one message created
- [ ] Duplicate join_call → idempotent, no error
- [ ] Duplicate leave_call → no-op
- [ ] All 12 permission types tested and enforced
- [ ] Channel permission overrides work end-to-end
- [ ] All error messages have ru and en translations
- [ ] Rate limiter rejects excessive requests

#### 12. Manual verification checklist

- [ ] Send same message twice with same idempotency key → only one in history
- [ ] Join call twice → no duplicate participant
- [ ] Test each permission type with allowed and denied user
- [ ] Trigger every error code → verify localized message

#### 13. Files/modules likely to change

```
backend/src/main/java/ru/timchat/message/application/MessageService.java
backend/src/main/java/ru/timchat/call/application/CallService.java
backend/src/main/java/ru/timchat/common/ratelimit/RateLimiter.java
backend/src/main/java/ru/timchat/common/ratelimit/RateLimitInterceptor.java
backend/src/main/resources/messages_en.properties
backend/src/main/resources/messages_ru.properties
backend/src/test/java/ru/timchat/permission/**
frontend/src/app/shared/ui/error-display.component.ts
```

#### 14. Suggested commit message

```
fix: harden idempotency, rate limiting, i18n completeness, and permission enforcement
```

#### 15. Exact stage implementation prompt

```
Read and follow all cursor rules from .cursor/rules/.

Read the implementation plan: docs/implementation/IMPLEMENTATION_PLAN.md — execute Stage 19 only.

Harden TimChat MVP for robustness and completeness:

IDEMPOTENCY:
1. Add idempotency key support to MessageService:
   - Accept optional idempotencyKey in send_message
   - Store key in Redis with TTL (e.g., 5 minutes)
   - If key exists, return existing message instead of creating duplicate

2. Make joinCall idempotent:
   - If user is already in the call, return existing participation
   - Do not create duplicate CallParticipant

3. Make leaveCall idempotent:
   - If user already left, return success (no-op)

RATE LIMITING:
4. Create simple Redis-based rate limiter:
   - Per-user limit for WS commands (e.g., 60 commands per minute)
   - Per-user limit for REST endpoints (e.g., 100 requests per minute)
   - Return 429 Too Many Requests when exceeded
   - Configurable via application.yml

I18N COMPLETENESS:
5. Review all error codes and message keys
   - Verify every error path has a message key
   - Verify every message key has both ru and en translations
   - Fix any missing translations

PERMISSION COMPLETENESS:
6. Write integration tests for all 12 permission types:
   - CHANNEL_VIEW, MESSAGE_WRITE, MESSAGE_DELETE_OWN, MESSAGE_DELETE_ANY
   - ROOM_JOIN, ROOM_SPEAK, ROOM_VIDEO, ROOM_SCREEN_SHARE
   - ROOM_MODERATE, ROOM_FORCE_MUTE, ROOM_KICK, ROOM_MOVE
   - Each test: user with permission → allowed; user without → denied

7. Test channel permission overrides:
   - Workspace-level grant + channel-level deny → denied
   - Workspace-level deny + channel-level allow → allowed

FRONTEND:
8. Add error display improvements:
   - Show localized error messages from backend in toast/notification
   - Handle 429 rate limit errors gracefully

After completing, update docs/implementation/EXECUTION_LOG.md with Stage 19 results.
```

---

### Stage 20: Gap Audit & Cleanup

#### 1. Objective

Финальная проверка: cross-flow consistency, dead code cleanup, test coverage review, документация.

#### 2. In-scope work

- Cross-flow consistency check (REST ↔ WS state, call ↔ moderation, permission ↔ enforcement)
- Dead code removal
- Unused import cleanup
- Test coverage review
- Final manual walkthrough of all user scenarios
- Update EXECUTION_LOG.md with final status

#### 3. Explicit out-of-scope

- New features
- Performance optimization
- Production deployment

#### 4. Backend work

- Review all modules for dead code, unused dependencies
- Verify all REST endpoints have proper auth
- Verify all WS handlers have proper auth
- Verify Liquibase migrations are clean and sequential
- Run full test suite

#### 5. Frontend work

- Review all components for dead code
- Verify all routes are protected by auth guard
- Verify all API calls use generated client
- Run `ng build --configuration=production` — no warnings

#### 6. Infra work

- Verify docker-compose starts all services cleanly
- Verify backend connects to all infra services

#### 7. Test work

- Run full backend test suite
- Run full frontend test suite
- Cross-flow manual test: full user journey

#### 8. Main risks

- Discovering significant issues requiring rework
- Time pressure to "just ship"

#### 9. Expected implementation gaps

- Some edge cases may remain documented but not fixed
- SFU integration is interface-only

#### 10. What must be fixed before done

- No dead code
- All tests pass
- All user scenarios work end-to-end
- EXECUTION_LOG.md is complete

#### 11. Acceptance criteria

- [ ] Full test suite passes (backend + frontend)
- [ ] No dead code or unused imports
- [ ] All user scenarios from business doc work end-to-end
- [ ] Cross-flow consistency verified
- [ ] `ng build --configuration=production` succeeds without warnings
- [ ] `mvn clean verify` succeeds
- [ ] EXECUTION_LOG.md is complete

#### 12. Manual verification checklist

- [ ] Full user journey: login → workspace → channel → send message → join call → toggle mute → moderator kicks → reconnect → audit log
- [ ] Permission denied scenarios work correctly
- [ ] Error messages are localized
- [ ] Presence indicators are accurate
- [ ] Attachments work end-to-end

#### 13. Files/modules likely to change

```
(any file with dead code or cleanup needed)
docs/implementation/EXECUTION_LOG.md
```

#### 14. Suggested commit message

```
chore: final gap audit, dead code cleanup, and cross-flow verification
```

#### 15. Exact stage implementation prompt

```
Read and follow all cursor rules from .cursor/rules/.

Read the implementation plan: docs/implementation/IMPLEMENTATION_PLAN.md — execute Stage 20 only.

Perform final gap audit and cleanup for TimChat MVP:

1. Run full backend test suite: mvn clean verify
   - Fix any failing tests
   - Remove any @Disabled tests that are no longer relevant

2. Run full frontend build: ng build --configuration=production
   - Fix any warnings or errors

3. Review for dead code:
   - Backend: unused classes, methods, imports
   - Frontend: unused components, services, imports
   - Remove all dead code

4. Cross-flow consistency check:
   - Send message via REST → verify it appears in WS channel subscribers
   - Send message via WS → verify it appears in REST history
   - Join call → verify presence shows "in call" context
   - Moderate user → verify effective state changes in call snapshot
   - Change permission → verify immediate effect on next action

5. Verify all user scenarios from docs/business/TimChat_Business_Features.md:
   - Scenario 8.1: Work in workspace
   - Scenario 8.2: Text communication
   - Scenario 8.3: Live room participation
   - Scenario 8.4: Self-state changes
   - Scenario 8.5: Moderation
   - Scenario 8.6: Reconnect

6. Verify Liquibase migrations apply cleanly on fresh database

7. Update docs/implementation/EXECUTION_LOG.md with final results and status

After completing, the MVP implementation is considered done per the confirmed scope.
```

---

## E. Project-Wide Tracking Model

### E.1. Stage statuses

| Status | Meaning |
|--------|---------|
| PLANNED | Stage defined, not started |
| IN_PROGRESS | Stage is being implemented |
| DONE | Stage completed, acceptance criteria met, committed |
| BLOCKED | Stage cannot proceed due to dependency or issue |

### E.2. How stage completion is determined

A stage is DONE when:

1. All acceptance criteria are checked off.
2. All must-fix items from "What must be fixed before done" are resolved.
3. Code compiles without errors.
4. Tests pass (existing + new).
5. Linter has no new errors.
6. Changes are committed.

### E.3. When to commit and push

- **Commit:** at the end of each completed stage.
- **Push:** after each commit (to maintain backup and enable CI).
- **Commit message:** use the suggested template from the stage.
- **Do not commit** if tests fail or acceptance criteria are not met.

### E.4. Recording gaps, risks, fixes, outcomes

All findings during implementation are recorded in `docs/implementation/EXECUTION_LOG.md`:

- **Gaps:** features or behaviors that should work but don't yet.
- **Risks:** issues that may cause problems later.
- **Fixes:** changes applied beyond the original stage scope.
- **Outcomes:** final result of the stage execution.

If a gap is found that blocks the current stage, the stage status becomes BLOCKED and the blocking issue is recorded.

If a gap is found that does not block the current stage but affects a future stage, it is recorded as a risk/gap for the affected stage.

---

## F. Execution Log

See [docs/implementation/EXECUTION_LOG.md](EXECUTION_LOG.md) for the execution tracking template.

---

## G. Final Completion Criteria

### What "implementation complete for current MVP scope" means:

1. **All 20 stages are DONE.**

2. **All business scenarios from `TimChat_Business_Features.md` section 8 work end-to-end:**
   - Work in workspace (8.1)
   - Text communication (8.2)
   - Live room participation (8.3)
   - Self-state changes (8.4)
   - Moderation (8.5)
   - Reconnect (8.6)

3. **All 12 permission types are enforced on every relevant action.**

4. **Audit covers:** moderation actions, room events, access changes.

5. **Reconnect:** snapshot-based restore works.

6. **Presence:** online/offline/idle/dnd visible in UI.

7. **Calls:** one active call per channel, join/leave, self-state, effective state with moderation.

8. **Chat:** send/edit/delete, history pagination, realtime delivery, attachments.

9. **Frontend:** Cursor-inspired layout with all 5 zones, feature-complete for MVP.

10. **Tests:** domain tests, integration tests, behavioural reliability tests pass.

11. **i18n:** all user-facing messages localized in ru and en.

12. **Code quality:** no dead code, clean imports, consistent style.

13. **Infra:** docker-compose starts all services, backend connects, frontend builds.

### What is NOT required for "complete":

- Production deployment config
- CI/CD pipeline
- Load testing results
- Concrete SFU integration (interface only is sufficient)
- E2EE, breakout rooms, replay reconnect, recording, microservices
- Mobile responsive layout
- Comprehensive unit test coverage for every method (focus on business-critical paths)
