# Postify Web — Social Media Frontend

[![React](https://img.shields.io/badge/React-19.2-blue.svg)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-6.0-blue.svg)](https://www.typescriptlang.org/)
[![Vite](https://img.shields.io/badge/Vite-8.1-646cff.svg)](https://vite.dev/)
[![Tests](https://img.shields.io/badge/Tests-166%20passing-success.svg)]()

React 19 single-page application for the Postify social media platform. Real-time messaging, ML-ranked feed, and admin dashboard.

## ✨ Features

| Category | Capabilities |
|---|---|
| **Authentication** | Login, registration, JWT session management, auto-logout on expiry |
| **Feed** | Infinite-scroll post feed with ML-ranked ordering, loading states, and error handling |
| **Posts** | Create posts with rich media (images, video), view details, comment threads |
| **Social** | Like posts, follow/unfollow users, save posts for later |
| **Profiles** | View and edit user profiles, upload avatar, manage saved posts |
| **Search** | Full-text search across posts and users with result highlighting |
| **Notifications** | Real-time notification feed with mark-as-read |
| **Direct Messages** | WebSocket-based real-time chat with STOMP, presence indicators, read receipts, conversation list |
| **Admin** | User management dashboard, user details view, platform moderation |
| **Responsive** | Mobile-first design with Tailwind CSS, adaptive sidebar navigation |

## 🏗 Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Postify Web (SPA)                    │
│                                                         │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐             │
│  │  Pages   │  │Components│  │  Context │             │
│  │  (12)    │  │  (shared) │  │ (Auth,   │             │
│  │          │  │           │  │  Msg)    │             │
│  └────┬─────┘  └─────┬─────┘  └────┬─────┘             │
│       │              │             │                    │
│  ┌────▼──────────────▼─────────────▼─────┐             │
│  │         Hooks & API Clients           │             │
│  │  React Query  ·  Axios  ·  STOMP.js   │             │
│  └────────────────────┬──────────────────┘             │
│                       │                                 │
│         ┌─────────────┼─────────────┐                  │
│         │ REST API    │  WebSocket  │                  │
│         │  :8080/api  │   /ws/chat  │                  │
│         └─────────────┴─────────────┘                  │
└─────────────────────────────────────────────────────────┘
```

### Technology Stack

| Layer | Technology |
|---|---|
| Framework | React 19.2 |
| Language | TypeScript 6.0 |
| Build | Vite 8.1 |
| Routing | React Router 7 |
| Server State | TanStack Query 5 |
| Styling | Tailwind CSS 4 |
| UI Components | Radix UI dialogs, Lucide icons, Recharts, Sonner toasts |
| Real-time | STOMP.js over WebSocket |
| HTTP Client | Axios with auth interceptors |
| Testing | Vitest 5, React Testing Library, jsdom |
| Linting | ESLint 10 with Prettier |

## 🚀 Quick Start

### Prerequisites

- **Node.js 22+**
- **npm 10+**
- **Postify Backend** running on `http://localhost:8080` (or configure a different URL)

### 1. Install Dependencies

```sh
npm ci
```

### 2. Configure the API URL

Create `.env.local` in the project root:

```dotenv
VITE_API_BASE_URL=http://localhost:8080/api
```

When unset, the API base URL defaults to `http://localhost:8080/api`.

> ⚠️ **Security note**: Values using the `VITE_` prefix are embedded in the client bundle. Never put secrets (API keys, tokens, passwords) in frontend environment variables.

### 3. Start the Development Server

```sh
npm run dev
```

The development server runs on `http://localhost:5173` by default. Open the printed URL in your browser.

## 📡 Pages

| Route | Page | Auth Required |
|---|---|---|
| `/` | Home — public feed | No |
| `/login` | Login | No |
| `/register` | Register | No |
| `/posts/:id` | Post details with comments | No |
| `/profile/:username` | User profile | No |
| `/profile/me` | Current user profile (edit) | Yes |
| `/search` | Search posts and users | No |
| `/notifications` | Notifications | Yes |
| `/messages` | Direct messages | Yes |
| `/admin` | Admin dashboard | Admin |
| `/admin/users` | User management | Admin |

## 🧪 Testing

```sh
npm test                 # Run the test suite once
npm run test:watch       # Watch mode during development
npm run test:coverage    # Run with coverage thresholds
npm run lint             # ESLint check
npm run build            # Type-check and production build
```

### Test Coverage

| Metric | Coverage | Floor |
|---|---|---|
| Lines | 75.22% | 70% |
| Statements | 74.64% | 70% |
| Functions | 73.79% | 70% |
| Branches | 65.09% | 60% |

**Total: 166 tests across 28 files, 0 failures.**

The test suite emphasizes:

- **Authentication & cache isolation** — token lifecycle, session expiry, React Query cache invalidation
- **Messaging** — WebSocket connection failures, reconnection, read receipts, pending message queue
- **Feed** — pagination, duplicate prevention, optimistic updates with rollback
- **Profile** — editing, avatar upload, saved posts, account deletion
- **Navigation** — protected routes, mobile sidebar behavior

Coverage is measured across executable TypeScript and TSX files in `src/`. Test files, shared fixtures, and type-only declarations are excluded. The HTML report is written to `coverage/` when `test:coverage` is run.

> These thresholds are regression floors, not a claim of full coverage. Areas such as feed media rendering, profile detail views, and admin navigation still need tests.

## 📁 Project Structure

```
src/
├── api/               # API clients, Axios configuration, request/response types
│   └── axios/         # Client creation, auth interceptors (request + response)
├── components/        # Shared UI components
│   └── layout/        # AppLayout, Sidebar, navigation
├── context/           # React contexts
│   ├── AuthContext    # Authentication state, token management, session expiry
│   └── MessagingContext  # WebSocket connection, messages, presence, read receipts
├── features/          # Feature-specific UI components
├── hooks/             # Reusable hooks (data fetching, behavior)
├── lib/               # Utility libraries
├── pages/             # Route-level pages
│   └── admin/         # Admin dashboard and user management
├── test/              # Shared test setup, fixtures, and utilities
└── utils/             # Token helpers, session utilities
```

## 🔄 Real-time Messaging

Direct messages use WebSocket (STOMP over `/ws/chat`) for bidirectional communication:

- **Authentication**: JWT token passed in STOMP connect headers
- **Reconnection**: Automatic reconnection with pending message queue
- **Presence**: Online/offline status tracking
- **Read receipts**: Sent and received read confirmations
- **Message validation**: Server-side validation with error feedback

The `MessagingContext` manages all WebSocket state and provides a clean API for sending, receiving, and managing conversations.

## 📦 Production Build

```sh
npm run build      # Type-check and create optimized production build
npm run preview    # Preview the production build locally
```

The build output is generated in `dist/`. Key characteristics:

- **CSS**: ~70 kB (Tailwind CSS, purged)
- **JS**: ~945 kB (pre-gzip, includes React, TanStack Query, STOMP.js)
- **SPA routing**: Configure your server to serve `index.html` for all routes

### Deployment Checklist

1. Set `VITE_API_BASE_URL` to your backend API endpoint
2. Configure your web server for SPA fallback (all routes → `index.html`)
3. Enable HTTPS in production
4. Configure CORS on the backend to allow your deployment domain
5. Set up a reverse proxy if WebSocket and REST share the same domain

## 🔒 Security Considerations

- **JWT stored in localStorage**: Accessible to XSS attacks. For production deployment, consider migrating to `httpOnly` cookies (requires backend changes).
- **No secrets in the bundle**: Only `VITE_*` variables are embedded. API authentication uses JWT tokens, not hardcoded credentials.
- **CORS**: Configured on the backend side. The frontend sends `withCredentials: true` for cookie-based sessions.
- **Input validation**: Form validation on the client side; backend validation is the authoritative check.

## 📚 Related Repositories

- **[Postify](https://github.com/your-username/Postify)** — Spring Boot backend (REST API, business logic)
- **[postify-ml](https://github.com/your-username/postify-ml)** — Python ML service (ranking, classification)

## 📄 License

MIT License — see [LICENSE](LICENSE) for details.
