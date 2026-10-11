# Jules Android — Complete Functional Recreation with ChatGPT-Inspired UI

A production-grade, native Android application recreating the Google Jules coding agent client with an authentic ChatGPT mobile UI/UX and color palette, a standalone Kotlin `JulesSdk` client library, multi-turn Gemini 3.1 Pro Copilot with High Thinking and Jules tool execution, and Firebase Authentication with Firestore cloud sync.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following revisions have been incorporated based on your feedback:

- **Confirmed Revision 1 (Design & Accent Color)**: Exact ChatGPT mobile visual styling and color system. Neutral monochrome tones (warm graphite `#212121` / `#171717` in dark mode, pure `#FFFFFF` / `#F9F9F9` in light mode), ChatGPT signature dark action pills, and ChatGPT blue/cyan accents for active states and voice/chat buttons (as seen in screenshots 1-5).
- **Confirmed Revision 2 (Jules Kotlin SDK Architecture)**: Built as a clean, decoupled `JulesSdk` wrapper layer (`com.example.jules.sdk`) rather than raw ad-hoc HTTP calls. Exposes idiomatic Kotlin coroutines, typed results, and clean domain models (`julesSdk.sessions`, `julesSdk.sources`).
- **Confirmed Revision 3 (AI Assistant & Copilot)**: Dual Mode — Jules coding sessions + Gemini 3.1 Pro Copilot with High Thinking and integrated `JulesSdk` tool triggers (Create Session, Inspect Sources, Plan Project).
- **Confirmed Revision 4 (Input Bar)**: Vibrant subtle floating pill composer with ambient glow, spring animation, and full keyboard inset padding.

---

## 1. Overview & Core Concept

- **What It Does**: Jules Android delivers an authentic ChatGPT-style mobile client for Google's autonomous coding agent (Jules). It wraps the official `https://jules.googleapis.com/v1alpha` API in an idiomatic Kotlin SDK, offering real repository management, session creation, live activity monitoring, plan approval, and GitHub artifact inspection alongside a Gemini Copilot.
- **Target Audience**: Developers who want to manage, inspect, and guide coding agent tasks from an Android device with the speed and elegance of the ChatGPT app.

---

## 2. User Experience & Visual Design (Authentic ChatGPT Replication)

### Visual Tokens (Matching Screenshots 1–5 Exactly)

- **Light Mode**:
  - Background: Clean white `#FFFFFF` / canvas `#F9F9FB`
  - Text: Primary `#0D0D0D`, secondary muted `#6E6E80`
  - Cards & Groupings: Rounded `#F7F7F8` or outlined `#ECECF1` with 16dp–24dp corners
  - Buttons: Solid black pill button `#000000` with white text `#FFFFFF` (Screenshot 5 "Continue with Google", "Log in or sign up"), and blue icon badge `#1D70B8` / `#2563EB` (Screenshot 3 "Chat" pill)
- **Dark Mode**:
  - Background: Deep `#171717` / `#212121`
  - Text: Primary `#ECECEC`, secondary muted `#9A9A9A`
  - Cards: `#2F2F2F` / `#262626`
- **Floating Pill Composer (Screenshots 2 & 4)**:
  - Floating pill elevated 16dp above navigation bar
  - Subtle gradient border glow (`#6366F1` to `#3B82F6`)
  - Left `+` action button, center expandable prompt field ("Ask ChatGPT" / "Ask Jules"), right microphone / voice waveform icon and send button

### Core Screens & Flows

1. **Welcome / Auth Screen (Screenshot 5)**:
   - Centered minimalist brand icon, large bold heading: "Welcome to Jules"
   - Subtitle: "Bring your repositories, your ideas, and your next big coding task."
   - "Continue with Google" primary black pill button + "Log in or sign up" / "Skip" guest button
2. **Navigation Drawer (Screenshot 3)**:
   - ChatGPT slide drawer with search icon at top right
   - Chronological session groupings (Today, Yesterday, Previous 7 Days)
   - Active session blue dot indicator (Screenshot 3)
   - Bottom user profile pill with user avatar (Screenshot 1 "Ashik"), email, and blue "Chat" action pill
3. **Empty Chat / Welcome Zero-State (Screenshot 2)**:
   - Clean top bar with hamburger menu and new session icon
   - Quick prompt suggestion chips ("Create a session", "Review PR / Code", "Search sources")
   - Centered floating pill composer
4. **Session Conversation & Detail (Screenshot 4)**:
   - Top bar: back arrow, session title, three-dot menu
   - User bubble: right-aligned light gray pill
   - Collapsible thinking indicator: "Worked for 24s >" (revealing reasoning trace)
   - Agent cards: Structured GitHub issue / PR cards with status badge (`Open`, `Approved`), bold title, summary, and "View GitHub Issue" / "View PR" action button
   - Plan approval card: Multi-step plan checklist with prominent "Approve Plan" button
5. **Account & Settings (Screenshot 1)**:
   - Profile avatar with edit badge, username "Ashik"
   - Grouped card "My Jules": Personalization, Memory, Plugins / Tools, Prompt Gallery
   - Grouped card "Account": Workspace, Jules API Key configuration, Usage & Limits, Email, Sign Out

---

## 3. Dedicated `JulesSdk` Architecture

Rather than making loose HTTP calls inside UI components, all Jules API communications are encapsulated in a robust, testable Kotlin SDK:

```
┌────────────────────────────────────────────────────────┐
│                        JulesSdk                        │
│ ┌────────────────────────────────────────────────────┐ │
│ │                  JulesApiClient                    │ │
│ │  - OkHttp Engine with X-Goog-Api-Key Interceptor   │ │
│ │  - Moshi / Kotlinx Serialization JSON Converters   │ │
│ │  - Error Handler (JulesApiException, AuthError)    │ │
│ └─────────────────────────┬──────────────────────────┘ │
│                           │                            │
│ ┌─────────────────────────▼──────────────────────────┐ │
│ │                  Service Modules                   │ │
│ │  ├─ SourcesService                                 │ │
│ │  │   └─ listSources() -> List<Source>              │ │
│ │  └─ SessionsService                                │ │
│ │      ├─ listSessions() -> List<Session>            │ │
│ │      ├─ createSession(prompt, source, branch)      │ │
│ │      ├─ getSession(id) -> Session                  │ │
│ │      ├─ deleteSession(id)                          │ │
│ │      ├─ listActivities(id) -> List<Activity>       │ │
│ │      ├─ sendMessage(id, text)                      │ │
│ │      └─ approvePlan(id, planId)                    │ │
│ └────────────────────────────────────────────────────┘ │
└───────────────────────────┬────────────────────────────┘
                            │
              ┌─────────────▼─────────────┐
              │     JulesAppViewModel     │
              │  (Jetpack Compose M3 UI)  │
              └───────────────────────────┘
```

- **Clean SDK Entrypoint**:
  ```kotlin
  val jules = JulesSdk.Builder()
      .apiKey(userApiKey)
      .baseUrl("https://jules.googleapis.com/v1alpha/")
      .build()
  ```
- **Typed Responses & Error Modeling**:
  - `JulesResult.Success<T>` and `JulesResult.Failure(error)`
  - Concrete domain classes: `Source`, `GithubRepo`, `Session`, `Activity`, `Plan`, `Artifact`

---

## 4. Gemini Copilot & Tool Calling Integration

- **Model**: `gemini-3.1-pro-preview` with High Thinking (`ThinkingConfig(thinkingLevel = "high")`) for complex project planning, architectural design, code review, and Jules tool generation. Fallback/fast mode: `gemini-3.5-flash`.
- **Jules Tools for Copilot**:
  - `create_jules_session(prompt, source, branch, requireApproval)`
  - `list_jules_sources()`
  - `approve_jules_plan(sessionId, planId)`
- **Reasoning Display**: Collapsible "Worked for Xs >" thinking header as seen in ChatGPT mobile (Screenshot 4).

---

## 5. Firebase & Cloud Persistence Strategy

- **Google Sign-In**: Integrated using Jetpack Credential Manager (`GetSignInWithGoogleOption`).
- **Firestore Collections**:
  - `/users/{userId}/saved_prompts`: User's custom prompts and saved templates.
  - `/users/{userId}/settings`: Theme preference, default source/branch, model preferences.
  - Jules API key: Securely stored in Android Keystore / encrypted preferences, never logged or exposed.
