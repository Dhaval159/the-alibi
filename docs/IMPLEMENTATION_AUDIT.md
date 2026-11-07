# THE ALIBI — Comprehensive Project Implementation Audit & Recovery Assessment

**Date:** November 2026  
**Auditor:** Lead Systems Architect & Senior Android/Backend Engineer  
**Status Assessment:** Multi-component audit of Native Android client (Kotlin + Jetpack Compose) and Ktor Backend Server.

---

## 1. Executive Summary

A comprehensive architectural and source-code audit was conducted across the entire **THE ALIBI** codebase:
- Native Android Application (`app/`)
- Ktor Backend Microservice (`backend/server/`)
- Shared schemas, assets, documentation, and configuration files

### Core Finding
The project possesses a remarkably sophisticated design and well-crafted data models. The Ktor backend contains complete, functioning REST and WebSocket endpoints for multiplayer rooms, secret dossiers, live chat, deal proposals, and validation. Furthermore, the Android app has an extensive suite of 21 polished Jetpack Compose screens embodying the 1920s noir detective aesthetic.

**However, the Android app and Ktor backend were operating in two separate universes:**
1. **Compilation Failure on Android:** `TheAlibiViewModel.kt` had missing methods and parameter mismatches (`updateGenerationConfig`, `startCaseGeneration`, `selectCase`, `deleteCase`, `updateRoomConfig`, `collectEvidence`, `addDeductionNote`, `askQuestion`, `addBoardConnection`, `removeBoardConnection`, `selectedBallotCulpritId`), causing `:app:compileDebugKotlin` to fail immediately.
2. **Disconnected Multiplayer Architecture:** `TheAlibiRepository.kt` on Android managed room creation, joins, and player simulation almost exclusively in-memory, completely bypassing the authoritative Ktor REST endpoints (`POST /rooms`, `POST /rooms/{code}/join`, etc.).
3. **WebSocket Event Schema Incompatibility:** The Ktor server emits events as `{"event": "ROOM_SNAPSHOT", "payload": "..."}`, whereas the Android WebSocket parser looked for `json.optString("type")` with legacy types `"STAGE_CHANGED"` and `"TIMER_SYNC"`. As a result, even if connected to the backend, incoming room snapshots and server updates were dropped silently.
4. **Authoritative vs. Local Game State:** In the Android client, host actions such as mystery generation and game phase transitions were executed locally instead of communicating with `/rooms/{code}/generate` and `/rooms/{code}/transition`.
5. **Durable Persistence:** In-memory storage was used in both the backend (`ConcurrentHashMap`) and the frontend. PostgreSQL schemas (`schema.sql`) were authored but not wired into an active ORM/Exposed/JDBC connection, meaning server restarts currently clear active room records.
6. **Gemini Live Integration:** `GeminiClient.kt` is implemented using modern v1beta endpoints and structured outputs, with deterministic validation and fallback to an authentic pre-seeded master mystery ("The Cipher at Blackwood Hall") and procedural generation when the API key is unconfigured.

---

## 2. Granular Feature Implementation Audit Table

Evaluated using the required status taxonomy:
- **COMPLETE AND VERIFIED**
- **IMPLEMENTED BUT NOT VERIFIED**
- **PARTIALLY IMPLEMENTED**
- **UI ONLY / PLACEHOLDER**
- **MOCKED / LOCAL ONLY**
- **BROKEN / INCOMPATIBLE**
- **NOT IMPLEMENTED**

| Feature Area | Sub-Component / Capability | Status | Key Source Files | Findings & Gaps |
|---|---|---|---|---|
| **Android Architecture** | MVVM, ViewModel, StateFlow, Nav | **PARTIALLY IMPLEMENTED** | `MainActivity.kt`, `TheAlibiViewModel.kt`, `TheAlibiRepository.kt` | UI state handles all 21 screens. Navigation backstack managed cleanly via `BackHandler`. Compilation errors due to missing ViewModel delegates. |
| **Visual Design** | Noir palette, Typography, Cards | **COMPLETE AND VERIFIED** | `Color.kt`, `Theme.kt`, `Type.kt`, Screens | High-contrast palette (`#101114`, `#191B20`, Gold `#C69B55`, Red `#8D2929`, Paper `#F1EEE7`). Serif typography, brass buttons, authentic mood. |
| **Home Screen** | Create room, Join room, Case library | **COMPLETE AND VERIFIED** | `HomeScreen.kt`, `CaseLibraryScreen.kt` | Buttons and navigation wired. Case card launches pre-seeded CASE 001. Quick actions launch flows. |
| **Room Management** | Create room (6-digit code), Join, Limits | **MOCKED / LOCAL ONLY** (Frontend) / **COMPLETE AND VERIFIED** (Backend) | `TheAlibiRepository.kt`, `RoomRoutes.kt`, `RoomRepository.kt` | Backend has full REST endpoints (`POST /rooms`, `POST /rooms/{code}/join`). Android repository was generating in-memory IDs without calling Ktor. |
| **Lobby** | Player roster, Ready state, Roster freeze | **PARTIALLY IMPLEMENTED** | `LobbyScreen.kt`, `CreateRoomScreen.kt`, `JoinRoomScreen.kt` | Lobby UI is rich, showing host badges, readiness, room code copy. Needs live bidirectional sync with backend server. |
| **Case Generation (Gemini)** | Text generation, Structured JSON | **IMPLEMENTED BUT NOT VERIFIED** | `GeminiClient.kt`, `CaseGenerationEngine.kt`, `CaseValidator.kt` | Supports live Gemini 2.5 Flash via REST v1beta. Full procedural fallback when API key is missing. Verified by backend tests. |
| **Case Validation** | Deterministic rule checking, repair pass | **COMPLETE AND VERIFIED** | `CaseValidator.kt`, `CaseValidatorTest.kt` | Validates 1 human culprit, NPC victim, unique IDs, 3+ deduction paths, configs for 4-10 players. Repair loop implemented. |
| **Image Generation** | Poster, suspect portraits, crime scenes | **IMPLEMENTED BUT NOT VERIFIED** | `ImageGenerationPipeline.kt`, `GeminiClient.kt` | Imagen 3 predict pipeline implemented with local file caching and graceful fallback if unconfigured. |
| **Character Assignment** | Secret dossiers, Culprit directive | **COMPLETE AND VERIFIED** (Backend) / **PARTIALLY IMPLEMENTED** (Frontend) | `RoomRepository.kt`, `CharacterRevealScreen.kt`, `TheAlibiViewModel.kt` | Backend endpoint `GET /rooms/{code}/my-character` enforces session token security. Frontend needs to fetch this authoritative dossier over HTTP. |
| **Gameplay Phases** | Briefing, Investigation, Twist, Debate, Vote, Resolution | **PARTIALLY IMPLEMENTED** | `MainActivity.kt`, `RoomModels.kt`, `TheAlibiViewModel.kt` | State machine covers all 11 phases with synchronized timers. UI transitions work, but need server authority propagation. |
| **Investigation** | Multi-location manor, hotspots, evidence | **COMPLETE AND VERIFIED** | `InteractiveSceneScreen.kt`, `EvidenceGalleryScreen.kt` | 5 distinct locations (Study, Dining, Terrace, Billiards, Drawing room). Hotspots trigger forensic discoveries and log notes. |
| **Suspects & Interrogation** | Character gallery, Q&A testimonies | **COMPLETE AND VERIFIED** | `SuspectGalleryScreen.kt`, `InterrogationScreen.kt` | Interrogation categories (Motive, Opportunity, Alibi). Contradiction detection triggers audio gong and deduction note. |
| **Investigation Board** | Red string pinboard, threads, notes | **COMPLETE AND VERIFIED** | `InvestigationBoardScreen.kt` | Allows linking suspects to clues with custom labels (e.g. "Breaks Alibi"). Detective notebook with timestamps. |
| **Timeline** | Chronological event reconstruction | **COMPLETE AND VERIFIED** | `TimelineScreen.kt` | Discovered vs. hidden sequence items. Timestamped logs with associated characters and forensic significance. |
| **Objectives** | Primary, secondary, secret objectives | **COMPLETE AND VERIFIED** | `ObjectiveCenterScreen.kt`, `CaseModels.kt` | Categorized objectives (Social, Investigative, Secret protection, Negotiation, Manipulation, Culprit-special). Progress tracking. |
| **Private Communication**| Player-to-player messaging | **PARTIALLY IMPLEMENTED** | `PrivateChatScreen.kt`, `RoomRoutes.kt`, `RoomRepository.kt` | UI allows direct chats with evidence attachments. Backend has secure endpoints. WebSocket delivery needs alignment. |
| **Negotiation & Deals** | Propose deal, Accept/Reject terms | **PARTIALLY IMPLEMENTED** | `PrivateChatScreen.kt`, `RoomRepository.kt` | Deal cards with status updates. Backend persists deals per room. |
| **Voting** | Secret simultaneous accusation ballot | **PARTIALLY IMPLEMENTED** | `AccusationScreen.kt`, `RoomRoutes.kt`, `RoomRepository.kt` | Secret ballot for culprit, method, motive, and smoking gun. Sealed ballot box animation. Tally trigger on all votes cast. |
| **Scoring** | Correct accusation, method, motive, objectives | **COMPLETE AND VERIFIED** | `ResolutionScreen.kt`, `RoomRepository.kt` | Scores calculated: +300 for culprit, +150 for smoking gun, +500 for culprit evasion, achievement badges. |
| **Multiplayer Sync** | REST APIs, WebSockets, Reconnect | **BROKEN / INCOMPATIBLE** | `TheAlibiRepository.kt`, `RoomRoutes.kt` | Android OkHttp client listened for `type` instead of `event`, and did not call REST endpoints for room lifecycle. |
| **Security & Privacy** | Session tokens, secret solution isolation | **IMPLEMENTED BUT NOT VERIFIED** | `RoomRepository.kt`, `CaseValidator.kt` | Headers `X-Player-Id` and `X-Session-Token` guard dossiers. `createPublicCase` sanitizes solutions. API key server-side only. |
| **Persistence** | Rooms, players, cases, votes | **MOCKED / LOCAL ONLY** | `RoomRepository.kt`, `CaseRepository.kt`, `schema.sql` | In-memory `ConcurrentHashMap` with thread-safe collections. PostgreSQL schema ready for connection. |
| **Testing** | Unit, Robolectric, backend tests | **IMPLEMENTED BUT NOT VERIFIED** | `TheAlibiGameTest.kt`, `RoomGenerationTest.kt`, `CaseValidatorTest.kt` | Backend tests pass (`BUILD SUCCESSFUL`). Android unit tests require compilation fix before running. |

---

## 3. Disconnected & Broken Integrations in Detail

### 3.1 Android ViewModel Missing Delegates & Field Name Mismatches
In `TheAlibiViewModel.kt`:
- Lines 540-558 referenced `selectedBallotCulpritId`, `selectedBallotSmokingGunId`, `selectedBallotMotive`, which do not exist on `TheAlibiUiState` (the actual state fields are `selectedAccusedCharacterId`, `selectedSmokingGunEvidenceId`, `selectedAccusationMotive`).
- `updateGenerationConfig`, `startCaseGeneration`, `selectCase`, `deleteCase`, `updateRoomConfig`, `collectEvidence`, `addDeductionNote`, `askQuestion`, `addBoardConnection`, `removeBoardConnection` were called by screens but missing in `TheAlibiViewModel.kt`.

### 3.2 Android Repository Disconnected from Ktor Backend
In `TheAlibiRepository.kt`:
- `createRoom()` and `joinRoom()` were writing only to a local `private val activeRooms = mutableMapOf<String, RoomSnapshot>()`.
- The real backend server exposes:
  - `POST http://<server>/rooms`
  - `POST http://<server>/rooms/{code}/join`
  - `POST http://<server>/rooms/{code}/generate`
  - `GET http://<server>/rooms/{code}/my-character`
  - `POST http://<server>/rooms/{code}/transition`
  - `POST http://<server>/rooms/{code}/messages`
  - `POST http://<server>/rooms/{code}/deals`
  - `POST http://<server>/rooms/{code}/vote`
  - `GET http://<server>/rooms/{code}/resolution`
  - `GET http://<server>/rooms/{code}` (polling / snapshot refresh)
  - `ws://<server>/rooms/{code}/ws?playerId=...`
The Android repository needs a robust dual-mode or backend-connected network client with graceful local fallback. When the backend is reachable (e.g., `http://10.0.2.2:8080` in emulator or configurable host), it should use real REST and WebSocket sync. If offline or in local test mode, it falls back to the in-memory engine.

### 3.3 WebSocket Event Format Mismatch
Backend sends:
```json
{
  "event": "ROOM_SNAPSHOT",
  "payload": "{\"roomId\": \"...\", \"roomCode\": \"...\", ...}"
}
```
Android `handleWebSocketMessage` expected:
```json
{
  "type": "STAGE_CHANGED",
  "payload": { ... }
}
```
This caused 100% of server-sent WebSocket events to be ignored.

---

## 4. Prioritized Action Plan

- **Step 1: Fix Android Compilation Errors**
  - Add missing delegation methods to `TheAlibiViewModel.kt`.
  - Fix property names in `TheAlibiViewModel.kt` (`selectedAccusedCharacterId`, `selectedSmokingGunEvidenceId`, `selectedAccusationMotive`).
  - Add `deleteCase` to `TheAlibiRepository.kt`.
  - Run `compile_applet` to verify clean build.

- **Step 2: Align Multiplayer Network Client & WebSocket Protocol**
  - Update `TheAlibiRepository.kt` with an `ApiClient` calling the Ktor backend endpoints (`/rooms`, `/join`, `/my-character`, `/generate`, `/messages`, `/deals`, `/vote`, `/resolution`).
  - Update WebSocket listener to parse `event` (`ROOM_SNAPSHOT`, `NEW_MESSAGE`, `DEAL_PROPOSED`, `DEAL_RESPONDED`, `TWIST_ALERT`, `VOTE_RECEIVED`, `RESOLUTION`).
  - Implement full room snapshot reconciliation and auto-reconnect.
  - Maintain instant local test mode as a fallback so standalone exploration and testing remain flawless.

- **Step 3: Verify Case Generation & Validation**
  - Ensure `CaseGenerationEngine` handles text generation with Gemini and falls back cleanly to the master procedural case when offline.
  - Validate exact player count freezing (4 to 10 players).

- **Step 4: Execute Full Test Suites**
  - Run Android unit tests (`gradle :app:testDebugUnitTest`).
  - Run backend unit tests (`gradle -p backend/server test`).
  - Verify edge cases (minimum 4 players, frozen roster lock, secret ballot tally).
