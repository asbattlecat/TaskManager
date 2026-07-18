# Менеджер задач — Техническое задание (ТЗ)

## 1. Цель проекта

Создать backend‑систему уровня «Jira Light», в которой пользователи работают внутри **workspace**, организуют работу по **project**, визуализируют задачи на **board** и управляют **task** с учётом ролей, прав доступа, статусов, приоритетов и истории изменений.

Проект ориентирован на прокачку:

- архитектурного мышления
- проектирования доменной модели
- ролевой модели доступа
- работы с транзакциями и бизнес‑правилами
- тестируемости и чистой архитектуры

---

## 2. Общая концепция домена

### 2.1. Основные сущности и их поля

---

## Workspace
**Описание:** пространство/организация, в рамках которой работают пользователи.

**Поля:**
- `id: UUID`
- `name: String`
- `description: String?`
- `ownerId: UUID`
- `createdAt: Instant`
- `updatedAt: Instant`

**Связи:**
- `projects: List<Project>`
- `members: List<WorkspaceMember>`

---

## WorkspaceMember
**Описание:** участие пользователя в workspace.

**Поля:**
- `id: UUID`
- `workspaceId: UUID`
- `userId: UUID`
- `role: WorkspaceRole` *(OWNER, ADMIN, MEMBER)*
- `joinedAt: Instant`

---

## Project
**Описание:** логическая единица работы внутри workspace.

**Поля:**
- `id: UUID`
- `workspaceId: UUID`
- `name: String`
- `description: String?`
- `createdAt: Instant`
- `updatedAt: Instant`
- `archived: Boolean`

**Связи:**
- `boards: List<Board>`

---

## Board
**Описание:** доска задач внутри проекта.

**Поля:**
- `id: UUID`
- `projectId: UUID`
- `name: String`
- `type: BoardType` *(KANBAN, SPRINT, BACKLOG)*
- `createdAt: Instant`
- `updatedAt: Instant`

**Связи:**
- `tasks: List<Task>`
- `columns: List<BoardColumn>` *(для kanban)*

---

## BoardColumn
**Описание:** колонка доски (для kanban).

**Поля:**
- `id: UUID`
- `boardId: UUID`
- `name: String`
- `position: Int`

---

## Task
**Описание:** единица работы на доске.

**Поля:**
- `id: UUID`
- `boardId: UUID`
- `columnId: UUID?` *(для kanban)*
- `title: String`
- `description: String?`
- `status: TaskStatus` *(TODO, IN_PROGRESS, DONE, BLOCKED)*
- `priority: TaskPriority` *(LOW, MEDIUM, HIGH, CRITICAL)*
- `assigneeId: UUID?`
- `creatorId: UUID`
- `deadline: Instant?`
- `createdAt: Instant`
- `updatedAt: Instant`

**Связи:**
- `subtasks: List<Task>`
- `tags: List<Tag>`
- `comments: List<Comment>`
- `history: List<AuditEntry>`

---

## Comment
**Описание:** комментарий к задаче.

**Поля:**
- `id: UUID`
- `taskId: UUID`
- `authorId: UUID`
- `content: String`
- `createdAt: Instant`
- `updatedAt: Instant`

---

## Tag
**Описание:** метка для классификации задач.

**Поля:**
- `id: UUID`
- `workspaceId: UUID`
- `name: String`
- `color: String?`

---

## AuditEntry
**Описание:** запись об изменении задачи.

**Поля:**
- `id: UUID`
- `taskId: UUID`
- `userId: UUID`
- `timestamp: Instant`
- `fieldName: String`
- `oldValue: String?`
- `newValue: String?`
- `workspaceId: UUID`
- `projectId: UUID`
- `boardId: UUID`

---

## 3. Ролевая модель и доступы

### 3.1. Уровни доступа

**Workspace:**
- `WORKSPACE_OWNER`
- `WORKSPACE_ADMIN`
- `WORKSPACE_MEMBER`

**Project:**
- `PROJECT_ADMIN`
- `PROJECT_MEMBER`

**Board:**
- `BOARD_ADMIN`
- `BOARD_MEMBER`
- `BOARD_VIEWER`

### 3.2. Принципы доступа

- Доступ к `Task` определяется доступом к её `Board` → `Project` → `Workspace`.
- Пользователь не может видеть/изменять сущности вне своих workspace/project/board.
- Операции над задачами разрешены только при наличии соответствующей роли.

---

## 4. Пользовательский опыт (UX)

### 4.1. Основные сценарии

1. Вход в систему (JWT).
2. Выбор workspace.
3. Просмотр проектов.
4. Выбор проекта.
5. Просмотр досок.
6. Работа с задачами.

### 4.2. Дополнительные сценарии

- Переключение workspace.
- Просмотр «моих задач».
- Фильтрация по статусу, приоритету, тегам, дедлайну.
- Просмотр истории изменений.

---

## 5. Функциональные требования

### 5.1. Workspace
- Создание workspace.
- Приглашение пользователей.
- Назначение ролей.
- Просмотр проектов.

### 5.2. Project
- Создание проекта.
- Просмотр проектов.
- Архивирование.

### 5.3. Board
- Создание доски.
- Настройка типа.
- Настройка колонок.

### 5.4. Task
- CRUD задач.
- Смена статуса.
- Назначение исполнителя.
- Подзадачи.
- Фильтрация и сортировка.
- История изменений.

### 5.5. Комментарии
- Добавление, редактирование, удаление.

### 5.6. Теги
- Создание тегов.
- Привязка к задачам.

### 5.7. Аудит
- Запись изменений.
- Просмотр истории.

---

## 6. Архитектурные решения

### 6.1. Слоистая архитектура
- Domain
- Repository
- Service
- Controller
- Mapper

### 6.2. Доменные правила
- Workspace требует владельца.
- Project требует workspace.
- Board требует project.
- Task требует board.

### 6.3. Domain Events
- `TaskCreatedEvent`
- `TaskStatusChangedEvent`
- `TaskAssignedEvent`

### 6.4. Безопасность
- JWT
- OAuth2 Resource Server
- Ролевые модели

### 6.5. Работа с данными
- PostgreSQL
- Liquibase/Flyway
- Индексы

### 6.6. Тестирование
- Unit‑тесты
- Интеграционные тесты
- Тестирование безопасности

---

## 7. Нефункциональные требования
- Чистая архитектура
- Логирование
- Глобальный обработчик ошибок
- Swagger/OpenAPI
- Docker

---

## 8. Цели проекта
- Проектирование доменной модели
- Ролевая модель доступа
- Иерархия сущностей
- Архитектурное мышление