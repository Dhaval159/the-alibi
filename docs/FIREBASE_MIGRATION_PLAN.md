# THE ALIBI — Migration Plan: From Ktor Microservice to Firebase Multiplayer

**Date:** November 2026  
**Document Status:** Complete Architecture & Implementation Plan  
**Target Platform:** Native Android (Kotlin + Jetpack Compose) + Firebase Cloud Services (Auth, Cloud Firestore, Realtime Database, Cloud Functions, Storage, App Check)

---

## 1. Existing Android Architecture

- **UI & Presentation:** 100% Jetpack Compose with Material 3, custom 1920s Noir palette (`#101114`, `#191B20`, Gold `#C69B55`, Red `#8D2929`, Paper `#F1EEE7`).
- **Screen Navigation:** Custom state-driven routing via `Screen` enum in `MainActivity.kt` with AndroidX `BackHandler` on all 21 secondary screens.
- **State Management:** MVVM architecture using `TheAlibiViewModel.kt` backed by `TheAlibiUiState` and `MutableStateFlow` observed via `collectAsStateWithLifecycle()`.
- **Data & Domain Layer:** `TheAlibiRepository.kt` manages in-memory game state, active mystery case, dossier concealment, evidence discovery, interrogation testimonies, board pin connections, private messages, deals, and secret voting ballots.
- **Audio & Immersion:** `SoundManager.kt` using Android `ToneGenerator` for low-latency acoustic cues (clue chime, accusation gong, secret fanfare) and independent toggles for music, SFX, and narration.

---

## 2. Existing Ktor Backend Architecture

- **Framework:** Ktor 2.3.12 embedded Netty server running on port 8080 (`backend/server/src/main/kotlin/com/thealibi/server/Application.kt`).
- **Plugins:** ContentNegotiation with `kotlinx.serialization.json`, CORS, CallLogging, StatusPages, WebSockets.
- **Repositories:**
  - `RoomRepository.kt`: In-memory `ConcurrentHashMap` of active rooms, player sessions, character assignments, phase timers, ballots, and resolution summaries.
  - `CaseRepository.kt`: In-memory registry of validated cases and async generation job statuses.
- **Procedural & AI Mystery Engine:**
  - `GeminiClient.kt`: Direct HTTP client to Google Generative Language API (`gemini-2.5-flash` for structured case JSON, `imagen-3.0-generate-002` for assets).
  - `CaseGenerationEngine.kt`: Generates setting, victim, 8-10 suspects, 10-14 clues, secret solution, deduction paths, and mid-game twist, with automatic fallback to pre-seeded master mystery ("The Cipher at Blackwood Hall").
  - `CaseValidator.kt`: Deterministic validation ensuring 1 human culprit, NPC victim, valid references, unique IDs, and 3+ deduction paths.
  - `ImageGenerationPipeline.kt`: Generates and caches posters, suspect portraits, and crime scenes to disk.

---

## 3. Existing Game Functionality

1. **Room Creation:** Host establishes a room with a 6-digit numeric code, custom mystery theme, difficulty, atmosphere, and duration (15–20 min, default 18 min).
2. **Lobby & Player Roster:** 4 to 10 players join via 6-digit room code, toggle readiness, and can be removed by the host. Developer mock investigator buttons exist for simulation.
3. **Roster Freeze & Generation:** Host initiates generation for the exact frozen human player count; room enters locked state and rejects late joiners.
4. **Cinematic Case Briefing:** Dramatic case poster, victim profile (Lord Alistair Blackwood), cause of death, incident summary, and investigation objective.
5. **Private Character Reveal:** Secret dossier with character portrait, public bio, private secret, alibi, and personal objective. Culprit receives secret directives, true sequence of events, and cover-up goals. Includes a shoulder-surfing privacy curtain.
6. **Act I Investigation:** Interactive crime scene across 5 manor locations (Study, Dining Hall, Terrace, Billiards Room, Drawing Room) with clickable hotspots, inspection popups, and clue collection into the evidence gallery.
7. **Mid-Game Twist:** Synchronized dramatic event (e.g. power cutoff, newly discovered telegram) revealing fresh evidence without altering the fundamental mystery truth.
8. **Act II Interrogation & Debate:** Suspect gallery, structured interrogation Q&A (Motive, Opportunity, Alibi), contradiction detection with acoustic gongs, private player-to-player messaging with evidence attachments, and pact/deal negotiation.
9. **Final Accusation Ballot:** Secret simultaneous voting for accused culprit, murder method, motive, and smoking-gun evidence.
10. **Final Case Resolution & Scoring:** Server-authoritative resolution revealing true culprit, motive, method, hidden timeline, confession, voting results, individual scores, and earned achievement badges.

---

## 4. Existing Backend Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/rooms` | Creates new multiplayer room with host session |
| `POST` | `/rooms/{code}/join` | Joins existing room with player alias |
| `GET` | `/rooms/{code}` | Returns public sanitized room snapshot |
| `POST` | `/rooms/{code}/ready` | Toggles player readiness |
| `POST` | `/rooms/{code}/generate` | Freezes roster and starts async case generation |
| `GET` | `/rooms/{code}/generation-status` | Polls progress of mystery generation |
| `GET` | `/rooms/{code}/my-character` | Returns caller's private dossier (guarded by `X-Player-Id` & `X-Session-Token`) |
| `POST` | `/rooms/{code}/transition` | Host updates authoritative room phase |
| `POST` | `/rooms/{code}/leave` | Player disconnects or leaves room |
| `GET` | `/rooms/{code}/timer` | Fetches synchronized server countdown timer |
| `POST` | `/rooms/{code}/messages` | Sends authenticated private dispatch |
| `GET` | `/rooms/{code}/messages` | Fetches caller's private chat history |
| `POST` | `/rooms/{code}/deals` | Proposes deal to another player |
| `POST` | `/rooms/{code}/deals/respond` | Accepts or declines proposed deal |
| `POST` | `/rooms/{code}/evidence/{id}/discover` | Marks evidence as publicly discovered |
| `POST` | `/rooms/{code}/twist/trigger` | Triggers synchronized mid-game twist |
| `POST` | `/rooms/{code}/vote` | Submits secret ballot |
| `GET` | `/rooms/{code}/resolution` | Tallies votes and returns final resolution summary |
| `POST` | `/cases/generate` | Standalone case generation endpoint |
| `GET` | `/cases/{id}` | Fetches public sanitized case package |
| `GET` | `/cases/{id}/assets/{assetId}` | Streams generated image files |
| `GET` | `/rooms/{code}/ws` | WebSocket connection for real-time room events |

---

## 5. Current WebSocket Events

The Ktor server sends WebSocket frames matching `WsEvent(val event: String, val payload: String)`:
- `ROOM_SNAPSHOT`: JSON-serialized `RoomPublicSnapshot` with player roster, phase, timer, and public evidence IDs.
- `NEW_MESSAGE`: JSON-serialized `PrivateMessage` delivered to sender and recipient.
- `DEAL_PROPOSED` & `DEAL_RESPONDED`: JSON-serialized `DealProposal` sent to involved players.
- `EVIDENCE_DISCOVERED`: Evidence ID string broadcast when a clue is made public.
- `TWIST_ALERT`: JSON-serialized `MidGameTwist` broadcast to trigger mid-game dialog.
- `VOTE_RECEIVED`: Vote progress count (e.g. `"3/6"`) broadcast as ballots arrive.
- `RESOLUTION`: JSON-serialized `GameResolutionSummary` broadcast once all votes are tallied.

---

## 6. Current Data Models

- **Room Models:** `RoomState`, `GameTimerState`, `PlayerInfo`, `RoomSnapshot`, `CreateRoomParams`, `JoinRoomParams`, `MyAssignedCharacter`.
- **Case Models:** `CaseIdentity`, `VictimProfile`, `CharacterDossier`, `InterrogationQnA`, `EvidenceItem`, `EvidenceVisibility`, `SceneHotspot`, `CrimeSceneData`, `InvestigationLocation`, `MidGameTwist`, `PersonalObjective`, `ObjectiveCategory`, `TimelineEvent`, `BoardConnection`, `DeductionNote`, `PrivateMessage`, `DealProposal`, `BallotVote`, `PlayerScoreCard`, `GameResolutionSummary`, `SecretSolution`, `CasePackage`, `AccusationVerdict`.

---

## 7. Features That Depend on Ktor

- Room creation, joining, and 6-digit room code lookup.
- Roster freezing and enforcing 4–10 player limits.
- Server-side mystery generation and deterministic validation.
- Secure private dossier delivery (`GET /rooms/{code}/my-character`).
- Real-time room event broadcasting and timers via WebSockets.
- Secret ballot collection, vote locking, and resolution calculation.
- Serving generated image assets via HTTP file streaming.

---

## 8. Features That Currently Use Local / Mock Data

- Standalone single-player mode on the Android client (`createCase001()` initialized by default).
- In-memory `activeRooms` map inside `TheAlibiRepository.kt` used as fallback when the Ktor server is unreachable.
- Mock investigator creation in the Lobby screen (`addMockInvestigator`).
- Local asset drawables (`R.drawable.img_crime_scene`, `img_case_poster`, `img_investigation_board`, `ic_the_alibi_logo`).

---

## 9. Firebase Services Required

1. **Firebase Authentication:** Player identity with Anonymous Auth (for instant frictionless onboarding) and Google Sign-In support.
2. **Cloud Firestore:** Primary authoritative game database:
   - `rooms/{roomId}`: Public room state, timers, player count, current phase, public case data.
   - `rooms/{roomId}/players/{uid}`: Player roster and ready states.
   - `rooms/{roomId}/privateDossiers/{uid}`: Isolated secret character dossier, private secret, alibi, objectives.
   - `rooms/{roomId}/evidence/{evidenceId}`: Evidence items with public/private/shared visibility.
   - `rooms/{roomId}/messages/{messageId}`: Private 1-to-1 messages between players.
   - `rooms/{roomId}/deals/{dealId}`: Pacts and trade proposals.
   - `rooms/{roomId}/votes/{uid}`: Secret accusation ballots.
   - `rooms/{roomId}/solution/actual`: True mystery solution (restricted to Cloud Functions until resolution).
   - `cases/{caseId}`: Shared public case packages.
3. **Firebase Realtime Database:** Low-latency player presence monitoring via `.info/connected` and `onDisconnect()`.
4. **Cloud Functions (TypeScript / Node.js):** Trusted server-side operations:
   - `createRoom`, `joinRoom`, `leaveRoom`, `setPlayerReady`.
   - `generateMysteryCase` (calling Gemini API server-side, validating logic, writing dossiers & solution).
   - `discoverEvidence`, `shareEvidence`.
   - `sendPrivateMessage`, `proposeDeal`, `respondToDeal`.
   - `transitionGamePhase`.
   - `submitBallotVote`, `resolveCase`.
5. **Firebase Storage:** Storing and serving generated case posters, suspect portraits, and crime scene art.
6. **Firebase App Check:** Request verification safeguarding Cloud Functions and Firestore from abuse.

---

## 10. Migration Sequence

```
Phase 1: Firebase Project Scaffolding & Dependencies
  ├── Add Firebase BoM, Firestore, Auth, Database, Storage, Functions to Gradle
  ├── Generate / provision google-services.json
  └── Scaffold Cloud Functions project (TypeScript)

Phase 2: Firestore Schema & Security Rules
  ├── Design zero-trust firestore.rules
  ├── Create firebase-blueprint.json
  └── Unit test rules using Firestore Emulator Suite (@firebase/rules-unit-testing)

Phase 3: Player Authentication & Presence
  ├── Implement FirebaseAuthRepository (Anonymous Auth + Google Sign-In)
  └── Implement FirebasePresenceRepository using Realtime Database onDisconnect()

Phase 4: Cloud Functions Implementation
  ├── Author room management functions (createRoom, joinRoom, setReady)
  ├── Author Gemini case generation & validation function
  ├── Author gameplay functions (evidence, messages, deals, phase transitions)
  └── Author vote tallying & scoring function (resolveCase)

Phase 5: Android Repository Migration
  ├── Implement FirebaseRoomRepository, FirebaseCaseRepository, FirebaseGameplayRepository
  ├── Replace OkHttp / WebSocket polling with Firestore Snapshot Listeners (Kotlin Flow)
  └── Update TheAlibiViewModel to consume Firebase repositories

Phase 6: Visual Assets & Storage Migration
  ├── Upload generated assets to Firebase Storage
  └── Configure Coil to load images directly from Firebase Storage URLs

Phase 7: End-to-End Verification & Ktor Retirement
  ├── Execute multi-client emulator tests
  ├── Run Android unit tests & verify all 21 screens
  └── Safely remove obsolete Ktor server code and dependencies
```

---

## 11. Security & Secrecy Risks

1. **Dossier Leakage:** Innocent players or the culprit reading other players' secret directives before resolution.  
   *Mitigation:* Store dossiers under `rooms/{roomId}/privateDossiers/{uid}` with Firestore rule `allow read: if request.auth.uid == uid`.
2. **Early Solution Discovery:** Querying the mystery solution from client devtools or inspection tools.  
   *Mitigation:* Store true culprit, motive, and method in `rooms/{roomId}/solution/actual` with `allow read: if false` until `currentPhase == 'CASE_RESOLUTION'`. Only Cloud Functions using Firebase Admin SDK access the solution before resolution.
3. **Vote Tampering / Spying:** Viewing other players' ballots before voting ends.  
   *Mitigation:* Individual ballots in `rooms/{roomId}/votes/{uid}` readable only by `request.auth.uid == uid`. Tallying executed server-side.
4. **Private Chat Eavesdropping:** Reading private messages exchanged between suspects.  
   *Mitigation:* Messages enforce `allow read: if request.auth.uid == resource.data.senderUid || request.auth.uid == resource.data.recipientUid`.
5. **Gemini API Key Exposure:** Embedding keys in client source code.  
   *Mitigation:* Gemini API calls executed exclusively inside Cloud Functions using Google Cloud Secret Manager / environment variables.

---

## 12. Expected Files to Create, Modify, and Remove

### Files to Create:
- `docs/FIREBASE_MIGRATION_PLAN.md`
- `firebase.json`
- `firestore.rules`
- `database.rules.json`
- `storage.rules`
- `firebase-blueprint.json`
- `functions/package.json`
- `functions/tsconfig.json`
- `functions/src/index.ts`
- `functions/src/gemini/caseGenerator.ts`
- `functions/src/gemini/caseValidator.ts`
- `functions/src/rooms.ts`
- `functions/src/gameplay.ts`
- `app/src/main/java/com/example/data/firebase/FirebaseAuthRepository.kt`
- `app/src/main/java/com/example/data/firebase/FirebaseRoomRepository.kt`
- `app/src/main/java/com/example/data/firebase/FirebaseGameRepository.kt`
- `app/src/main/java/com/example/data/firebase/FirebasePresenceRepository.kt`

### Files to Modify:
- `gradle/libs.versions.toml`: Add Firebase Database, Storage, Functions SDKs.
- `app/build.gradle.kts`: Activate Firebase dependencies and remove Ktor client dependencies.
- `app/src/main/java/com/example/data/TheAlibiRepository.kt`: Adapt to delegate to Firebase repositories while preserving test compatibility.
- `app/src/main/java/com/example/ui/TheAlibiViewModel.kt`: Bind to Firebase reactive flows.
- `app/src/main/java/com/example/MainActivity.kt`: Initialize Firebase Auth and presence monitoring.

### Files to Retire (Stage G):
- `backend/server/` (after complete Firebase verification).
