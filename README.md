# TrueHunt Backend (Spring Boot)

A production-ready REST API backend for **TrueHunt**, built using **Java 17/21**, **Spring Boot 3.3.4**, **Spring Security**, **JWT Authentication**, and **Spring Data JPA** with dual-profile support for **MySQL** and **H2** (zero-setup in-memory dev).

---

## 🌟 Key Features Implemented

1. **Authentication & Authorization (`/api/auth`)**:
   - Register (`POST /api/auth/register`): Unique email validation, BCrypt password hashing, creates initial empty profile.
   - Login (`POST /api/auth/login`): Validates credentials, issues JWT token and sets an `HttpOnly` cookie.
   - Logout (`POST /api/auth/logout`): Clears authentication session and cookie.
   - Current User (`GET /api/auth/me`): Returns the authenticated account and profile data.

2. **Artist Profile & Contact Privacy (`/api/profiles`)**:
   - `GET /api/profiles/{userId}`: Enforces privacy rules. Contact fields (`phone`, `email`, `address`) are kept private by default and only visible to the owner or accepted connections. Private profiles are inaccessible to strangers.
   - `PATCH /api/profiles/me`: Authoritative server-side updates of profile fields, tags, and media keys.

3. **Media Upload & Storage (`/api/media`)**:
   - `POST /api/media/upload`: Validates file size (strictly enforces **5 MB max limit** for demo videos), checks allowed MIME types and file extensions, and saves files to the server.
   - `GET /api/media/{fileKey}`: Serves images, audio, demo videos, and documents directly to clients.

4. **Home Feed & Opportunities (`/api/posts`, `/api/opportunities`)**:
   - Unified feed domain model with validated type discriminator (`post`, `article`, `opportunity`).
   - Articles strictly require a `title` on the server.
   - Opportunities feed (`GET /api/opportunities`) returns **only real opportunity records** and returns an empty list `[]` when none exist (never seeded or mock jobs).
   - Cursor-based pagination (`?cursor=<ISO_TIMESTAMP>&limit=20`) newest-first.

5. **Network & Connections (`/api/connections`)**:
   - `GET /api/connections/suggestions`: Returns eligible suggested profiles (excludes self and pending/accepted connections).
   - `POST /api/connections`: Rejects self-connections and duplicate requests; creates `pending` status and recipient notification.
   - `PATCH /api/connections/{connectionId}`: Only recipient can accept/reject; generates notification upon acceptance.
   - `GET /api/connections`: Lists accepted connections.

6. **Conversations & Messaging (`/api/conversations`)**:
   - Direct conversation creation and lookup.
   - Enforces participant membership checks before reading or sending messages.
   - `POST /api/conversations/{id}/messages`: Persists message, updates `lastMessageAt`, and creates notifications for recipients.
   - Automatically marks incoming messages as read upon retrieval.

7. **In-App Notifications (`/api/notifications`)**:
   - Triggers notifications for connection requests, connection acceptances, and new messages.
   - `GET /api/notifications`: Cursor-paginated notifications for the authenticated user with actor profile enrichment.
   - `PATCH /api/notifications/{id}/read`: Mark individual notification read.
   - `PATCH /api/notifications/read`: Bulk mark notifications as read.

---

## 🏗️ Technology Stack

- **Language:** Java 17+ (LTS)
- **Framework:** Spring Boot 3.3.4
- **Security:** Spring Security 6 with JJWT (HMAC SHA-512)
- **Persistence:** Spring Data JPA with Hibernate
- **Database:** 
  - `dev` profile: H2 in-memory (zero installation required)
  - `mysql` profile: MySQL 8+
- **Build Tool:** Apache Maven

---

## 🚀 Getting Started

### 1. Running Locally (Zero-Setup with H2)

By default, the active profile is `dev` using H2 in-memory database:
```bash
mvn spring-boot:run
```
The server will start on `http://localhost:8080`.
H2 Console is available at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:truehunt`, User: `sa`, Password: `<empty>`).

### 2. Running with MySQL

To connect to your local MySQL 8 instance:
1. Ensure the MySQL service (e.g. `MySQL80`) is running on port 3306.
2. In `src/main/resources/application.yml` under profile `mysql`, update password if needed.
3. Start the application with the `mysql` profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

### 3. Running Automated Tests

Run the full integration test suite:
```bash
mvn test
```

---

## 📡 API Reference Summary

### Response Envelopes

#### Success Response Shape
```json
{
  "data": {},
  "meta": {
    "nextCursor": "2026-10-23T11:00:00Z"
  }
}
```

#### Error Response Shape
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Check the submitted fields.",
    "fields": {
      "content": "Content is required."
    }
  }
}
```

### Endpoints Table

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `POST` | `/api/auth/register` | Register new user + initial profile | No |
| `POST` | `/api/auth/login` | Authenticate and issue JWT token + cookie | No |
| `POST` | `/api/auth/logout` | Clear session cookie | Yes |
| `GET` | `/api/auth/me` | Fetch authenticated user & profile details | Yes |
| `GET` | `/api/profiles/{userId}` | Read profile subject to privacy rules | Optional |
| `PATCH` | `/api/profiles/me` | Update authenticated user's profile | Yes |
| `POST` | `/api/media/upload` | Upload media (enforces 5MB demo video limit) | Yes |
| `GET` | `/api/media/{fileKey}` | Serve media files | No |
| `GET` | `/api/posts?cursor=...` | Read posts and articles feed | No |
| `POST` | `/api/posts` | Create post or article | Yes |
| `POST` | `/api/posts/{postId}/media` | Attach media to post | Yes |
| `GET` | `/api/opportunities?cursor=...` | Read real opportunities feed (empty if none) | No |
| `POST` | `/api/opportunities` | Create opportunity | Yes |
| `GET` | `/api/connections/suggestions` | Suggested artist connections | Yes |
| `POST` | `/api/connections` | Send connection request | Yes |
| `PATCH` | `/api/connections/{id}` | Accept or reject connection request | Yes |
| `GET` | `/api/connections` | List accepted connections | Yes |
| `GET` | `/api/conversations` | List user's conversations | Yes |
| `POST` | `/api/conversations/direct/{userId}` | Get or create direct chat | Yes |
| `GET` | `/api/conversations/{id}/messages`| Cursor-paginated message history | Yes |
| `POST` | `/api/conversations/{id}/messages`| Send a message in conversation | Yes |
| `GET` | `/api/notifications` | List user notifications | Yes |
| `PATCH` | `/api/notifications/{id}/read` | Mark single notification as read | Yes |
| `PATCH` | `/api/notifications/read` | Mark all / multiple notifications as read | Yes |
