# OpenUI KMP Mobile App

A **Kotlin Multiplatform (KMP)** mobile application for Android and iOS that connects to a self-hosted [OpenUI](https://github.com/wandb/openui) backend — an open-source tool that lets you describe UI using your imagination and see it rendered live through a conversational LLM interface.

---

## Overview

OpenUI runs as a local Python server (port `7878`) backed by any LLM (OpenAI, Anthropic, Ollama, Groq, etc.). This app provides a native mobile front-end for that server:

1. A **splash screen** is shown while the app establishes a connection to the backend.
2. Once connected, a **chat window** opens where the user types prompts and receives streamed UI generation results.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Shared code (logic + UI) | Kotlin Multiplatform + Compose Multiplatform |
| Android host app | Jetpack Compose (Material 3) |
| iOS host app | SwiftUI shell + Compose Multiplatform views |
| HTTP client | Ktor (multiplatform) |
| Streaming | Server-Sent Events (SSE) via Ktor |
| Async / state | Kotlin Coroutines + `StateFlow` |
| DI | Koin (multiplatform) |
| Serialization | `kotlinx.serialization` |
| Build | Gradle 8 + Kotlin 2.x |

> **UI sharing strategy:** Compose Multiplatform is used for all shared screens (Splash, Chat). Each platform host (Android Activity / iOS `UIHostingController`) embeds these composables. Platform-native chrome (status bar tinting, edge-to-edge, safe-area insets) is handled per-platform via `expect/actual`.

---

## Repository Structure

```
openui-explore/
├── shared/                        # KMP shared module (logic + UI)
│   ├── commonMain/
│   │   ├── data/
│   │   │   ├── model/             # ChatMessage, ChatSession, UIComponent
│   │   │   ├── network/           # Ktor client, OpenUIApiService
│   │   │   └── repository/        # ConnectionRepository, ChatRepository
│   │   ├── presentation/
│   │   │   ├── splash/            # SplashViewModel, SplashState
│   │   │   └── chat/              # ChatViewModel, ChatState, ChatUiEvent
│   │   └── ui/
│   │       ├── splash/            # SplashScreen composable
│   │       └── chat/              # ChatScreen, MessageBubble composables
│   ├── androidMain/               # Android actuals (HttpClient engine, etc.)
│   └── iosMain/                   # iOS actuals
├── androidApp/                    # Android application module
│   └── MainActivity.kt            # Sets content to shared SplashScreen
└── iosApp/                        # Xcode project
    └── ContentView.swift          # Hosts shared Compose UI via ComposeUIViewController
```

---

## Architecture

The app uses a strict **layered architecture** inside the shared KMP module:

```
UI (Compose) → ViewModel → Repository → Network (Ktor) → OpenUI Backend
```

All layers except the Ktor engine `actual` implementations are in `commonMain` and shared between platforms.

---

### High-Level Component Diagram

```mermaid
%%{init: {'theme': 'dark'}}%%
graph TD
    subgraph Android["Android Host"]
        AM[MainActivity]
    end

    subgraph iOS["iOS Host"]
        IM[ContentView.swift]
    end

    subgraph KMP_UI["Shared UI — Compose Multiplatform"]
        SP[SplashScreen]
        CH[ChatScreen]
    end

    subgraph KMP_VM["Shared Presentation"]
        SVM[SplashViewModel]
        CVM[ChatViewModel]
    end

    subgraph KMP_Data["Shared Data"]
        CR[ConnectionRepository]
        CHR[ChatRepository]
        API[OpenUIApiService — Ktor]
    end

    subgraph Backend["OpenUI Backend :7878"]
        PY[Python FastAPI]
        LLM[LLM Provider\nOpenAI · Ollama · Groq…]
    end

    AM -->|setContent| SP
    IM -->|ComposeUIViewController| SP
    SP --> SVM
    SP -- "on connected" --> CH
    CH --> CVM
    SVM --> CR
    CVM --> CHR
    CR --> API
    CHR --> API
    API -->|HTTP + SSE| PY
    PY --> LLM
```

---

### App Launch & Connection Flow

```mermaid
%%{init: {'theme': 'dark'}}%%
sequenceDiagram
    participant App as App (Android / iOS)
    participant Splash as SplashScreen
    participant SVM as SplashViewModel
    participant Repo as ConnectionRepository
    participant BE as OpenUI Backend

    App->>Splash: render
    Splash->>SVM: init / collect state
    SVM->>Repo: checkConnection()
    Repo->>BE: GET /v1/health
    
    alt 200 OK
        BE-->>Repo: 200 OK
        Repo-->>SVM: Result.Success
        SVM-->>Splash: SplashState.Connected
        Splash-->>App: Navigate → ChatScreen
    else Timeout or error
        BE-->>Repo: IOException / 5xx
        Repo-->>SVM: Result.Error(message)
        SVM-->>Splash: SplashState.Error(message)
        Splash-->>App: Show retry dialog
    end
```

---

### Chat & Streaming Flow

```mermaid
%%{init: {'theme': 'dark'}}%%
sequenceDiagram
    participant User
    participant Chat as ChatScreen
    participant CVM as ChatViewModel
    participant Repo as ChatRepository
    participant BE as OpenUI Backend

    User->>Chat: type prompt → Send
    Chat->>CVM: onSendMessage(prompt)
    CVM->>Repo: streamChatMessage(prompt)
    Repo->>BE: POST /v1/chat/completions\n{stream: true}

    loop SSE token chunks
        BE-->>Repo: data: {"delta": "..."}
        Repo-->>CVM: Flow<String> emit
        CVM-->>Chat: update ChatState.messages
        Chat-->>User: append token to bubble
    end

    BE-->>Repo: data: [DONE]
    Repo-->>CVM: flow complete
    CVM-->>Chat: ChatState.Idle (ready for next input)
```

---

### Screen & State Machine

```mermaid
%%{init: {'theme': 'dark'}}%%
stateDiagram-v2
    [*] --> Splash

    state Splash {
        [*] --> Connecting
        Connecting --> Connected : Health check OK
        Connecting --> ConnectionError : Timeout / unreachable
        ConnectionError --> Connecting : User taps Retry
    }

    Connected --> Chat

    state Chat {
        [*] --> Idle
        Idle --> Streaming : User sends prompt
        Streaming --> Idle : Response complete
        Streaming --> StreamError : Network drop
        StreamError --> Idle : Dismiss
    }
```

---

## Key Design Decisions

### 1. Splash Screen Purpose
The splash screen is not just decorative — it actively pings the OpenUI backend. No navigation to the chat screen occurs until the connection is confirmed. This prevents the user from reaching a broken chat experience.

### 2. SSE Streaming
OpenUI uses the OpenAI-compatible `POST /v1/chat/completions` endpoint with `"stream": true`, responding with `text/event-stream`. Ktor reads the byte channel incrementally and emits parsed chunks into a Kotlin `Flow<String>`, keeping the UI reactive without blocking.

### 3. Backend URL Configuration
The backend URL is configurable. Default values differ by platform:

| Context | Default URL |
|---|---|
| Android Emulator | `http://10.0.2.2:7878` (`10.0.2.2` = host `localhost`) |
| iOS Simulator | `http://localhost:7878` |
| Physical device (both) | LAN IP of the machine running OpenUI, e.g. `http://192.168.1.x:7878` |

A settings screen (post-MVP) will allow the user to enter a custom URL at runtime.

### 4. Compose Multiplatform for UI Sharing
Rather than writing separate Compose (Android) and SwiftUI (iOS) screens, Compose Multiplatform targets both platforms from `commonMain`. The iOS app wraps the shared composable in a `ComposeUIViewController`. This maximises code sharing while still letting each platform handle its own navigation chrome and OS-level integration via `expect/actual`.

---

## OpenUI API Endpoints Used

| Endpoint | Method | Purpose |
|---|---|---|
| `/v1/health` | `GET` | Splash: verify backend is reachable |
| `/v1/models` | `GET` | Chat: populate model selector |
| `/v1/chat/completions` | `POST` (SSE) | Chat: stream LLM response |

---

## Build Requirements

| Requirement | Version |
|---|---|
| Android Studio | Jellyfish+ |
| Xcode | 15+ |
| Kotlin | 2.0+ |
| Gradle | 8.x |
| Android minSdk | 26 |
| iOS deployment target | 16.0+ |
| OpenUI backend | Self-hosted Docker or `openui.fly.dev` |

---

## Running the Backend (Docker — quickest)

```bash
# Set at least one LLM API key
export OPENAI_API_KEY=sk-...

docker run --rm -p 7878:7878 \
  -e OPENAI_API_KEY \
  ghcr.io/wandb/openui
```

The app will connect to `http://<host>:7878`. The live public demo at `https://openui.fly.dev` can also be used as the backend URL during development (requires GitHub login for quota enforcement).
