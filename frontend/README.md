# AI Agent Chat System — Frontend

Angular frontend for the AI Agent Chat System: chat UI, conversation list, settings (model providers, model configs, MCP servers), and real-time updates via WebSocket.

## Tech Stack

- **Angular 21** with TypeScript
- **Angular Material** + **CDK**
- **Tailwind CSS**
- **SockJS + STOMP** (`@stomp/stompjs`, `sockjs-client`) — WebSocket
- **RxJS** — reactive state and API calls
- **marked** — markdown rendering in messages

## Prerequisites

- Node.js 18+ and npm
- Backend running at the URL configured in the app (default `http://localhost:8080`)

## Quick Start

1. **Install dependencies:**

   ```bash
   npm install
   ```

2. **Start the dev server:**

   ```bash
   npm start
   ```

   App is available at **http://localhost:4200**. It will proxy API/WebSocket requests to the backend (see `angular.json` or env if you change the backend URL).

3. **Optional:** Configure backend base URL (e.g. in environment or `src/app/services/api.service.ts`) if not using default `http://localhost:8080`.

## Scripts

| Command       | Description                    |
|---------------|--------------------------------|
| `npm start`   | Dev server (`ng serve`)       |
| `npm run build` | Production build             |
| `npm test`    | Unit tests (Vitest)           |

## Project Structure

```
frontend/
├── README.md           # This file
├── angular.json
├── package.json
├── src/
│   ├── index.html
│   ├── main.ts
│   ├── styles.css      # Global styles
│   └── app/
│       ├── app.ts
│       ├── app.config.ts
│       ├── app.css
│       ├── app.html
│       ├── app.routes.ts
│       ├── components/
│       │   ├── auth/           # Login / register
│       │   ├── chat-window/    # Chat thread + input
│       │   ├── conversation-list/  # Sidebar conversations
│       │   ├── settings/       # Tabs: providers, configs, MCP
│       │   ├── toast/          # Toast notifications
│       │   └── confirm-dialog/
│       ├── models/             # TypeScript interfaces (conversation, message, MCP, etc.)
│       ├── pipes/
│       │   └── markdown.pipe.ts
│       └── services/
│           ├── api.service.ts      # REST API client
│           ├── auth.service.ts     # JWT and auth state
│           ├── chat.service.ts      # Conversation/message state
│           ├── websocket.service.ts # STOMP subscription and send
│           ├── toast.service.ts
│           ├── theme.service.ts
│           └── confirm.service.ts
├── public/
│   └── favicon.ico
├── tailwind.config.js
└── tsconfig*.json
```

## Main Features

- **Auth:** Login and register; JWT stored and sent with API/WebSocket requests.
- **Conversations:** List, create, select; messages loaded per conversation.
- **Chat:** Send messages; stream or final reply shown; markdown rendered.
- **Settings:**
  - **Providers:** Register AI providers (OpenAI, Anthropic, Ollama, custom) with API keys and base URLs.
  - **Configs:** Create/edit model configs (provider, model, temperature, etc.) and set default.
  - **MCP:** Add/edit MCP servers (URL, headers, default context tools, tool input schemas).
- **Real-time:** New messages for the current conversation appear via WebSocket without refresh.

## Code Scaffolding

Generated with [Angular CLI](https://github.com/angular/angular-cli). To add components:

```bash
ng generate component component-name
ng generate --help
```

## Building for Production

```bash
npm run build
```

Output is in `dist/`. Serve that folder with any static file server or your backend.

## Running Tests

```bash
npm test
```

Uses [Vitest](https://vitest.dev/) as configured in the project.

## License

MIT (or as per project root).
