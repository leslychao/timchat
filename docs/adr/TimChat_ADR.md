# TimChat — Architecture ADR

## 1. Статус

**Статус:** Approved for implementation baseline  
**Тип документа:** Architecture Decision Record (ADR)  
**Проект:** `TimChat`  
**Java root package:** `ru.timchat`

---

## 2. Зачем нужен этот ADR

Этот документ фиксирует **целевую архитектуру TimChat** для текущего подтвержденного scope.

Это не попытка построить “идеальную архитектуру на будущее”.
Это фиксация **минимально достаточной, корректной и practically production-ready архитектуры** для того, что реально подтверждено сейчас.

---

## 3. Контекст и подтвержденные ограничения

Подтверждено следующее:

- **Frontend:** Angular
- **Backend:** Java 21 + Spring Boot
- **Realtime control plane:** raw WebSocket
- **Media plane:** отдельный SFU
- **Durable storage:** PostgreSQL
- **Ephemeral / live state:** Redis
- **Auth:** OAuth2/OIDC + JWT
- **File storage:** S3-compatible object storage
- **NAT traversal:** STUN/TURN
- **E2EE:** не входит в MVP

Подтвержденный продуктовый scope, который должна выдерживать архитектура:

- chat
- workspaces / channels
- presence
- calls / room-control
- moderation
- reconnect
- audit
- permissions

---

## 4. Архитектурный вердикт

**Вердикт: Sufficient with must-fix issues.**

Общая архитектурная форма выбрана правильно.
Большой редизайн не нужен.

Нужно заранее зафиксировать несколько обязательных контрактов, чтобы реализация не расползлась в разные стороны:

1. жесткая граница backend ↔ SFU
2. одна активная call session на канал в MVP
3. reconnect только через snapshot restore
4. effective participant state вычисляется сервером
5. typed realtime handlers вместо giant gateway logic
6. централизованная permission / moderation resolution

---

## 5. Главная архитектурная идея

TimChat реализуется как **modular monolith** с:

- **маленьким стабильным core**
- **четкими интеграционными edge-слоями**

### 5.1. Stable core

В стабильное ядро входят:

- identity
- workspaces
- membership
- roles and permissions
- channels
- messages
- call sessions
- moderation rules
- audit
- realtime protocol contracts

### 5.2. Flexible edges

На гибких краях остаются:

- WebSocket transport and protocol handlers
- Redis-backed live projections and session registry
- SFU integration adapter
- TURN credential provider
- S3-compatible storage adapter
- future E2EE layer
- future recording / transcription layer

---

## 6. Архитектурные решения

## 6.1. Backend — только control plane, не media server

### Решение

Spring Boot backend **не передает RTP/media** и не становится медиасервером.

**Backend отвечает за:**

- authentication and authorization
- workspace / channel / call permissions
- room lifecycle
- moderation
- TURN credential issuing
- join metadata / room policy / call snapshot
- authoritative business state
- audit
- orchestration

**Media path лежит в:**

- client WebRTC stack
- SFU
- STUN/TURN

### Почему это важно

Иначе медиадетали начнут протекать в:

- WebSocket handlers
- application services
- Redis projections
- frontend orchestration

Это создаст лишнюю связанность и увеличит blast radius любого изменения.

---

## 6.2. PostgreSQL — durable truth

### Решение

PostgreSQL хранит весь долговечный бизнес-state:

- users
- profiles
- workspaces
- memberships
- roles
- permissions
- channels
- channel overrides
- messages
- attachment metadata
- call session metadata
- participant join/leave history
- moderation actions
- audit events

### Почему это важно

Durable truth должен жить в одном понятном месте.
Нельзя делать Redis или SFU скрытым источником бизнес-истины.

---

## 6.3. Redis — только transient truth

### Решение

Redis хранит только live / transient state:

- online/offline presence
- websocket connection registry
- active room participant projections
- self-state / effective-state live projections
- dedupe keys
- reconnect/session lookup data
- cross-node fanout/pub-sub

### Правило

Если Redis потерян:

- durable business state остается корректным
- live state может временно деградировать
- reconnect + snapshot rebuild должны восстановить operational state

### Почему это важно

Redis нужен для скорости, а не для бизнес-истины.

---

## 6.4. Клиент не пишет authoritative room truth

### Решение

Клиент может только **запрашивать действие**:

- join call
- leave call
- unmute self
- enable camera
- start screen share

Сервер решает:

- разрешено ли это
- каким будет итоговое состояние
- какой snapshot увидят остальные участники

### Почему это важно

Иначе UI одного клиента может начать расходиться с реальным server-side state.

---

## 6.5. Reconnect — только snapshot-based

### Решение

Для MVP reconnect работает только через **fresh authoritative snapshot**.

Храним только:

- `clientSessionId`
- `connectionId`
- `resumeToken`
- `snapshotVersion`

**Не делаем в MVP:**

- replay-based catch-up
- `lastSeenEventId`
- event journal for reconnect

### Почему это важно

Одновременно поддерживать snapshot и replay — это две разные модели согласованности.
Для текущего scope snapshot проще, безопаснее и дешевле в реализации.

---

## 6.6. CallSession в MVP имеет простую и явную семантику

### Решение

Для MVP:

- у одного voice/video channel может быть **не более одной активной CallSession**
- session создается при первом join
- session живет, пока есть участники
- session закрывается, когда уходит последний участник
- reconnect **не создает новую** бизнес-call session

### Почему это важно

Это убирает двусмысленность в:

- join behavior
- leave behavior
- reconnect behavior
- moderation scope
- participant lifecycle

---

## 6.7. Effective participant state вычисляет сервер

### Решение

Нужно явно разделить:

- **self state** — чего хочет пользователь
- **server / moderation state** — что ограничено политикой
- **effective state** — что реально действует сейчас

Примеры self state:

- `selfMuted`
- `selfVideoEnabled`
- `selfScreenShareEnabled`

Примеры server constraints:

- `serverMuted`
- `serverVideoBlocked`
- `serverScreenShareBlocked`

Итоговый visible state публикуется как:

- `ParticipantEffectiveState`

### Почему это важно

Без этого легко получить рассинхрон между:

- тем, что показывает UI
- тем, что считает сервер
- тем, что разрешила модерация

---

## 7. Верхнеуровневая runtime topology

```text
[ Angular Client ]
  ├─ HTTPS/REST  -> TimChat API
  ├─ WSS         -> TimChat Realtime Gateway
  ├─ WebRTC      -> SFU
  └─ Upload      -> Object Storage (via signed upload flow)

[ TimChat Backend (Spring Boot, ru.timchat) ]
  ├─ Auth / OIDC / JWT
  ├─ Workspace / Membership / Roles / Permissions
  ├─ Channels / Chat / Attachments metadata
  ├─ Realtime Gateway (raw WebSocket)
  ├─ Presence / Session Registry
  ├─ Call Control / Moderation
  ├─ TURN credential issuing
  ├─ SFU orchestration adapter
  └─ Audit

[ PostgreSQL ]
  └─ Durable business state

[ Redis ]
  ├─ Live presence
  ├─ Room snapshots
  ├─ Connection registry
  ├─ Fanout/pub-sub
  └─ Dedupe/session keys

[ SFU ]
  ├─ Publish audio/video/screen
  ├─ Subscribe tracks
  └─ Adaptive media forwarding

[ Object Storage ]
  ├─ Avatars
  ├─ Message attachments
  ├─ Thumbnails
  └─ Future exports/recordings
```

---

## 8. Domain model

### 8.1. Identity and collaboration

- `User`
- `UserProfile`
- `Workspace`
- `WorkspaceMember`
- `Role`
- `PermissionGrant`
- `MemberRole`
- `Channel`
- `ChannelPermissionOverride`
- `Invite`
- `AttachmentMetadata`

### 8.2. Chat domain

- `Message`
- `MessageAttachment`
- `MessageRevision`
- `MessageDeletion`

### 8.3. Call-control domain

- `CallSession`
- `CallParticipant`
- `ParticipantDevice`
- `ParticipantSelfState`
- `ParticipantEffectiveState`
- `ModerationAction`
- `CallJoinTicket`
- `ReconnectToken`

### 8.4. Realtime operational domain

- `WebSocketConnection`
- `ClientSession`
- `PresenceSnapshot`
- `ChannelRealtimeSubscription`
- `LiveCallSnapshot`

### 8.5. Audit domain

- `AuditEvent`
- `AuditActor`
- `AuditTarget`
- `AuditPayload`

---

## 9. Backend module structure

Root package:

```text
ru.timchat
```

Top-level modules:

```text
ru.timchat
  ├─ TimChatApplication
  ├─ auth
  ├─ user
  ├─ workspace
  ├─ permission
  ├─ channel
  ├─ message
  ├─ attachment
  ├─ presence
  ├─ realtime
  ├─ call
  ├─ media
  ├─ moderation
  ├─ audit
  ├─ common
  └─ infra
```

### Responsibilities

#### `auth`
- JWT / OIDC
- current user context
- handshake auth support

#### `workspace`
- workspace lifecycle
- members
- invites

#### `permission`
- roles
- grants
- permission resolution
- channel override resolution
- call policy checks

#### `channel`
- channel model
- text/voice/video channel rules
- ordering / visibility

#### `message`
- send/edit/delete
- history
- realtime message publication

#### `attachment`
- object metadata
- upload/download authorization
- signed URL orchestration if used

#### `presence`
- online/offline/idle/dnd
- live presence projection

#### `realtime`
- WebSocket transport
- protocol parsing
- dispatching
- reconnect logic
- session registry
- cross-node fanout

#### `call`
- call lifecycle
- participant lifecycle
- self/effective state
- call snapshots

#### `media`
- TURN credential issuing
- SFU integration adapter
- media policy

#### `moderation`
- mute/kick/block actions
- enforcement state

#### `audit`
- durable audit records

#### `common`
- errors
- ids
- shared contracts

#### `infra`
- PostgreSQL config
- Redis config
- storage clients
- observability
- serializers

---

## 10. Внутреннее layering rule по модулю

Рекомендуемая форма:

```text
<module>
  ├─ api
  ├─ application
  ├─ domain
  ├─ infra
  └─ mapper
```

### Смысл

- `api` — REST DTOs / controllers
- `application` — use cases / orchestration / command services
- `domain` — core model, invariants, repository interfaces
- `infra` — JPA / Redis / storage / SFU implementations
- `mapper` — model translations where needed

### Realtime internal structure

```text
realtime
  ├─ ws
  ├─ protocol
  ├─ dispatch
  ├─ handler
  ├─ registry
  └─ security
```

---

## 11. WebSocket protocol direction

### 11.1. Принцип

Так как выбран raw WebSocket, приложение обязано иметь **собственный строгий application protocol**.

### 11.2. Required envelope

```json
{
  "protocolVersion": 1,
  "messageId": "uuid",
  "correlationId": "uuid-or-client-id",
  "type": "join_call",
  "scope": "call",
  "sentAt": "2026-03-28T12:00:00Z",
  "clientSessionId": "uuid",
  "payload": {}
}
```

Обязательные поля:

- `protocolVersion`
- `messageId`
- `correlationId`
- `type`
- `scope`
- `clientSessionId`
- `payload`

### 11.3. Scopes

- `system`
- `workspace`
- `channel`
- `chat`
- `presence`
- `call`
- `moderation`
- `media`

### 11.4. Базовое правило

Команда клиента — это **request**, а не “истина”.

Клиент говорит:

- “я хочу подключиться”
- “я хочу включить микрофон”
- “я хочу включить камеру”

Сервер отвечает:

- “разрешено / запрещено”
- “effective state теперь такой”
- “snapshot теперь такой”

---

## 12. Realtime code organization guardrails

### 12.1. Не делать giant WebSocket handler

Нельзя складывать десятки команд в один giant `switch`.

Нужно:

- thin WebSocket entrypoint
- protocol parser
- dispatcher
- typed handlers по доменам

Примеры:

- `SystemCommandHandler`
- `ChatCommandHandler`
- `PresenceCommandHandler`
- `CallCommandHandler`
- `ModerationCommandHandler`

### 12.2. Handlers не содержат бизнес-логику

Handlers должны:

- парсить
- валидировать shape протокола
- маршрутизировать команду

Бизнес-логика живет в application services доменных модулей.

### 12.3. Permission / moderation resolution централизованы

Нельзя размазывать permission decisions по:

- controllers
- websocket handlers
- helper classes

Нужны централизованные resolver / service слои.

### 12.4. Не делать один “универсальный” model на все случаи

Не надо одним объектом закрывать одновременно:

- DB entity
- REST DTO
- WS payload
- Redis projection
- frontend state

### 12.5. Use case должен читатьcя как сценарий

Пример правильного порядка:

1. validate auth / permission
2. update durable state
3. update transient projection
4. emit outbound realtime event
5. write audit if needed

---

## 13. Permission and moderation model

Минимально нужны отдельные permission types:

- `CHANNEL_VIEW`
- `MESSAGE_WRITE`
- `MESSAGE_DELETE_OWN`
- `MESSAGE_DELETE_ANY`
- `ROOM_JOIN`
- `ROOM_SPEAK`
- `ROOM_VIDEO`
- `ROOM_SCREEN_SHARE`
- `ROOM_MODERATE`
- `ROOM_FORCE_MUTE`
- `ROOM_KICK`
- `ROOM_MOVE`

Нужно явно различать:

- self intent
- server constraints
- effective visible state

Это обязательно для:

- mute / unmute
- camera enable / disable
- screen sharing
- speaker role
- raised hand

---

## 14. REST vs WebSocket responsibilities

### REST отвечает за

- auth / profile / workspace / channel CRUD
- history loading
- attachment metadata flows
- stable resource-oriented operations
- administrative endpoints where needed

### WebSocket отвечает за

- realtime message delivery
- live presence
- call control
- moderation notifications
- reconnect / snapshot synchronization

---

## 15. Frontend architecture direction

Frontend строится feature-oriented.

```text
src/app
  ├─ core
  │   ├─ auth
  │   ├─ api
  │   ├─ realtime
  │   ├─ routing
  │   └─ layout
  ├─ shared
  │   ├─ ui
  │   ├─ util
  │   └─ models
  ├─ features
  │   ├─ auth
  │   ├─ workspace
  │   ├─ channels
  │   ├─ chat
  │   ├─ call
  │   ├─ presence
  │   ├─ profile
  │   └─ settings
  └─ app.routes.ts
```

Ключевые frontend services / facades:

- `AuthStore`
- `WorkspaceStore`
- `ChannelStore`
- `ChatStore`
- `PresenceStore`
- `CallStore`
- `RealtimeGateway`
- `RtcMediaService`
- `DeviceService`
- `AttachmentUploadService`

Правило: frontend orchestrates UX and media lifecycle, но **не владеет authoritative business state**.

---

## 16. Persistence and live state rules

### PostgreSQL хранит

- business truth
- moderation history
- message history
- audit
- durable call metadata

### Redis хранит

- connection registry
- presence
- room participant live projection
- dedupe keys
- session restore helpers
- cross-node fanout support

### Recovery rule

Если Redis потерян:

- durable business data остаются корректными
- live state можно восстановить через reconnect + snapshot rebuild

---

## 17. Test strategy

### 17.1. Domain tests

Должны покрывать:

- call session lifecycle
- permission decisions
- moderation constraints
- effective state computation
- reconnect eligibility
- valid / invalid state transitions

### 17.2. Application / integration tests

Обязательно покрыть:

- `join_call`
- `leave_call`
- `resume_session`
- `send_message`
- `delete_message`
- `mute_participant`
- `update_self_state`

Проверять:

- durable state
- live projection
- outbound event behavior
- audit where required

### 17.3. Behavioural reliability tests

Обязательно покрыть:

- duplicate `join_call`
- duplicate `leave_call`
- duplicate `send_message` with same idempotency key
- repeated `resume_session`
- reconnect after stale connection
- reconnect after moderation change
- snapshot consistency

### 17.4. Cross-flow consistency tests

Нужно проверить согласованность между:

- REST
- WebSocket
- call flow
- moderation flow

---

## 18. Performance guardrails

### 18.1. Не гонять тяжелые байты через backend

- media: client ↔ SFU
- file upload/download: по возможности direct storage flows
- backend выдает metadata / authorization / signed URLs

### 18.2. Presence не должен создавать write amplification

- heartbeat не пишет в PostgreSQL
- presence живет в Redis
- fanout только по meaningful state change

### 18.3. Fanout не должен постоянно делать лишнюю serialization work

Где можно:

- готовим один room-wide payload
- переиспользуем один serialized event
- персонализируем только если реально нужно

### 18.4. Snapshot restore должен оставаться дешевым

- Redis хранит live projections
- PostgreSQL — durable facts и fallback
- не допускаем N+1 при сборке snapshot

### 18.5. History queries должны быть shaped correctly

- keyset pagination
- корректные индексы
- batch loading
- без deep offset pagination на hot screens

---

## 19. Security rules

### 19.1. Transport security

- HTTPS
- WSS
- JWT validation
- DTLS/SRTP in WebRTC path

### 19.2. Application security

Каждая команда проверяет:

- authenticated user
- workspace membership
- effective permission
- channel visibility
- moderation constraints
- payload validation
- anti-flood / rate limits where needed

### 19.3. Raw WebSocket security rule

Handshake auth недостаточен.
Каждая application command проходит explicit authorization в backend-layer.

### 19.4. Attachment security

- metadata в PostgreSQL
- content-type validation
- size limits
- controlled access через signed URLs или authorized download flow
- без open object dump

---

## 20. Неприкосновенные инварианты

1. Backend не переносит media.
2. SFU не является источником business truth.
3. PostgreSQL — durable truth.
4. Redis — transient truth.
5. Клиент не пишет authoritative state.
6. Каждая команда авторизуется на сервере.
7. Reconnect восстанавливает состояние через snapshot.
8. Chat и call-control остаются отдельными доменами.
9. Бинарные данные живут в object storage, не в PostgreSQL.
10. `CallSession` и media transport — разные сущности.
11. В MVP один канал имеет максимум одну активную call session.
12. Effective participant state вычисляет сервер.

---

## 21. Что сознательно не делаем сейчас

- STOMP
- replay-based reconnect recovery
- multiple parallel call sessions per channel
- breakout rooms
- E2EE в MVP
- server-side recording
- server-side transcription
- complex CQRS / read-model architecture
- microservices split
- separate signaling service
- generic plugin system for room types
- binary protocol instead of JSON
- Kafka/event-streaming architecture for internal fanout

---

## 22. Принятые компромиссы

1. **raw WebSocket оставляем**
   - стек проще
   - больше дисциплины нужно в protocol design

2. **snapshot restore оставляем**
   - reconnect payload тяжелее
   - зато сложность сильно ниже, чем у replay model

3. **one active call per channel in MVP**
   - меньше гибкости
   - зато lifecycle и moderation намного яснее

4. **Redis only transient**
   - проще live performance
   - требует discipline на reconnect rebuild

5. **direct object storage flows**
   - меньше нагрузки на backend
   - нужны signed URL и metadata control

---

## 23. Триггеры для будущего архитектурного пересмотра

Пересмотр нужен только при **реальных сигналах**, а не “на всякий случай”.

Примеры таких сигналов:

- подтвержден кейс с несколькими одновременными calls в одном channel
- подтвержденная потребность в recording или transcription
- подтвержденная потребность в E2EE rooms
- reconnect storms вызывают реальную operational pain
- Redis pub/sub или session registry стал измеримым bottleneck
- history queries стали измеримой database problem
- появились новые SLA/SLO, требующие другого уровня resilience / latency
- монолит объективно уперся в границы масштабирования

---

## 24. Финальная рекомендация

### Что фиксируем сейчас

Перед активной реализацией нужно окончательно зафиксировать:

1. hard backend ↔ SFU boundary
2. one active CallSession per channel in MVP
3. reconnect only through authoritative snapshot restore
4. server-authoritative effective participant state
5. typed realtime handlers instead of giant gateway logic
6. centralized permission / moderation resolution
7. must-have domain / integration / reconnect tests
8. hot-path performance guardrails

### Что не нужно делать сейчас

- не дробить систему в microservices
- не добавлять STOMP без реальной боли
- не делать replay-based reconnect
- не тащить E2EE в MVP
- не строить generalized event platform “на будущее”
- не оптимизировать спекулятивные bottlenecks без метрик

### Что было бы overengineering

- отдельные сервисы для chat / call / presence / moderation
- universal DTO/entity/event models на все слои
- complex CQRS для MVP
- binary protocol replacement прямо сейчас
- generic plugin framework для future room types
- replay event journal для reconnect
- попытка использовать backend как partial media relay

---

## 25. Финальное утверждение

**Архитектура TimChat выбрана правильно по направлению и достаточна для текущего scope, если обязательные решения выше зафиксированы до начала активной реализации.**

Правильный следующий шаг:

- не переделывать архитектуру заново,
- а закрыть обязательные архитектурные контракты,
- после чего реализовывать систему с явными границами, простыми потоками и сильными тестами.
