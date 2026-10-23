# TrueHunt (Full-Stack: React + Spring Boot)

Full-stack application for **TrueHunt**, containing:
- **Frontend**: React 19 Single Page Application built with Vite
- **Backend**: Spring Boot 3.3.4 REST API with Spring Security, JWT, and JPA (MySQL & H2)

---

## 🚀 Running the Project

### 1. Frontend (React + Vite)
```bash
npm install
npm run dev
```
The React frontend runs on `http://localhost:5173`.

### 2. Backend (Spring Boot)
- **Run with H2 In-Memory (Zero Setup):**
  ```bash
  mvn spring-boot:run
  ```
- **Run with MySQL:**
  ```bash
  mvn spring-boot:run -Dspring-boot.run.profiles=mysql
  ```
The REST API runs on `http://localhost:8080`.

### 3. Run Backend Test Suite (44 tests)
```bash
mvn test
```

---

## 🌟 Key Backend Features Implemented

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

## 📡 Endpoints Table

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
