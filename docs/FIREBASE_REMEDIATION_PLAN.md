# THE ALIBI Firebase remediation plan

**Status:** proposed implementation plan only. No source, Gradle, Firebase config, or rule files were changed to produce this plan. The 2026-11-07 audits remain the source of truth for current state: Firebase Auth gates the app, while gameplay still uses local `TheAlibiRepository`; Firebase repositories are unused; rules permit privilege escalation; Functions, RTDB presence, Storage rules, and verified Firebase multiplayer are absent.

## Target outcome

An anonymous player can enter quickly, join a four-player room, and play a complete synchronized match. Firebase UID is the identity key. Android observes Firestore state and calls authenticated Functions for all authoritative transitions. Direct client writes cannot alter roles, solution, phase, ballots, scoring, or evidence unlock state. Compose screens are retained; repository/ViewModel boundaries change incrementally. Any offline/demo mode is visibly labeled and cannot masquerade as an online room.

## 1. Authentication

Enable Firebase Anonymous Auth and make it the default quick-entry path. On launch, observe `FirebaseAuth.currentUser`; if absent, call `signInAnonymously()`, then enter the app when the Firebase UID is available. Firebase persists that user on the same app installation across process/app restarts. Repositories obtain UID from Firebase Auth and never generate a gameplay identity independently.

Offer Google account linking from settings or a later prompt. Link the Google credential to the existing anonymous `FirebaseUser` with `linkWithCredential`, preserving its UID and all room membership. Do not sign out then sign in as a different user as the normal linking flow. If the Google credential is already attached to another Firebase UID, Firebase reports a credential collision: keep the anonymous session active, explain the choice, and only merge after an explicit, server-verified migration of room memberships/game records from old UID to new UID. Never silently switch identity or discard access.

If app data is cleared or the app is reinstalled, the anonymous auth state may be lost and a new UID issued. Existing rooms then remain associated with the old UID and cannot be recovered by that anonymous identity. Encourage account linking for durable identity, and implement a short room recovery code only as a separate, rate-limited server capability if product wants recovery. Do not promise anonymous UID recovery.

## 2. Trusted backend design

Create a Firebase Functions v2 TypeScript service. Callable functions require Firebase Auth, validate App Check in production, derive caller UID from verified context, validate membership/phase and use Admin SDK transactions. Never accept a client-supplied UID as the authenticated actor. Idempotency keys and transactionally claimed request records protect retries. The data store is Firestore; RTDB owns ephemeral connection presence; Cloud Storage holds immutable generated assets. Put Gemini credentials in Secret Manager bound only to generation functions.

| Operation | Trusted entry point | Core authority |
|---|---|---|
| Create room / allocate code | Callable `createRoom` | Transactionally reserve random six-digit code in `roomCodes/{code}` and create room+host membership. Retry collisions; enforce code alphabet exactly six digits. |
| Join / leave room | Callable `joinRoom`, `leaveRoom` | Transaction checks lobby, expiry, capacity, existing membership and canonical UID. Join is idempotent; count is derived/maintained transactionally. |
| Host and permissions | All callables | Host is immutable `rooms/{id}.hostUid`; ignore client role claims. Host-only operations check caller UID against this field. Host receives no private dossier access by default. |
| Readiness / freeze | Callable `setReady`, `freezeRoster` | Player may change own ready flag only in lobby. Freeze transaction checks minimum four, max capacity, readiness, membership, and writes immutable UID roster/hash + state transition. No joins after freeze. |
| Case generation / validation / repair | Callable `generateCase` (long-running task pattern if platform duration requires); server task worker | Uses frozen roster. Reuse `GeminiClient`, `CaseGenerationEngine`, and `CaseValidator` after porting/shared module; add strict roster and narrative invariant validator. Secret kept server-side. Persist progress/status and idempotency. |
| Roles / culprit / dossiers | Internal generation finalizer; callable `getMyDossier` | Server selects exactly one human culprit, assigns unique characters, makes victim NPC, all other humans innocent; stores solution separately. Only owner may retrieve own dossier. Host cannot read it. |
| Evidence discovery/share | Callable `discoverEvidence`, `shareEvidence` | Validate evidence ID against generated case, unlock condition and phase. Server writes discovered/shared records; client cannot set smoking-gun truth or owner/share flags. |
| Interrogation / private interactions | Callable `submitInterrogation`, `createInteraction` | Check member, phase, target and allowed interaction; write validated event and return only authorized response. No arbitrary hidden facts from clients. |
| Deals | Callable `proposeDeal`, `respondToDeal` | Immutable sender/recipient/terms after create; only target may accept/reject pending deal; validate clue ownership and phase; transaction enforces one terminal transition. |
| Private messaging | Callable `sendMessage` | Actor derived from auth, recipient must be room member, length/rate limits. Public/private visibility field fixed on creation; server timestamp. Reads filtered by membership and recipient/public status. |
| Phase/timer | Callable `advancePhase` for host request; scheduled `advanceExpiredPhase` | Server validates transition graph, required completion, and writes `phaseStartedAt`, `phaseDeadline`, monotonically increasing revision. Scheduled task rechecks revision/deadline transactionally; client clocks are display-only. |
| Ballots / resolution | Callable `submitBallot`, internal finalizer or scheduled `resolveWhenComplete` | Create-only canonical ballot UID; voting phase only; validate character/evidence; no ballot reads by players until resolution. Transaction locks vote set and writes one result. Tally/scoring deterministic server-side. |
| Expiration / cleanup | Scheduled `expireRooms`, `purgeExpiredRoom` task | Mark expired based on server timestamps; remove code reservation; recursively delete room subcollections/storage after retention window. RTDB events can mark members offline but never delete rooms directly. |

For generation likely exceeding a callable request limit, callable `requestCaseGeneration` should enqueue a task and return a job ID. A task-triggered worker performs Gemini, validation/repair, asset creation and assignment. Callable `getGenerationStatus`/Firestore listener reports progress. This task queue is proposed; deployment/runtime limits must be confirmed during implementation.

## 3. Firestore target schema and access design

Use room IDs independent of the join code. Keep join codes in a separate reservation collection so code uniqueness is transactionally enforced and public queries do not enumerate rooms.

```text
roomCodes/{sixDigitCode}                 # server-only: roomId, expiresAt
rooms/{roomId}                           # public lobby/game summary, hostUid, status/revision/deadlines
rooms/{roomId}/members/{uid}              # public alias/ready/seat; character assignment hidden
rooms/{roomId}/case/public                # member-readable public case content, no solution
rooms/{roomId}/dossiers/{uid}             # owner-only read; server-only writes
rooms/{roomId}/solution/actual            # server-only until resolution; never mixed into public case
rooms/{roomId}/evidence/{evidenceId}       # member-readable only after server unlock/share policy
rooms/{roomId}/messages/{messageId}        # server-created immutable event, public or recipient scoped
rooms/{roomId}/deals/{dealId}              # parties read; server writes transitions
rooms/{roomId}/interactions/{eventId}      # visibility-scoped immutable records
rooms/{roomId}/ballots/{uid}               # server-only reads before resolution; create through callable
rooms/{roomId}/resolution/final            # members read after resolved; server write only
rooms/{roomId}/requests/{requestId}         # server-only idempotency/job state
```

Rules default-deny. For every room-scoped read, prove `request.auth != null` and `exists(/rooms/{roomId}/members/{request.auth.uid})`; existence alone is adequate for authorization, while membership status/phase checks are repeated by Functions for writes. No collection-wide room listing: clients use a room ID returned by authenticated create/join or a narrowly scoped `myRooms/{uid}` index maintained server-side. This avoids broad enumeration and makes listener queries compatible with the rules.

Clients get read-only access to room summary, member roster (excluding role/secret fields), public case and released evidence after membership check. Clients have no direct `create/update/delete` permission for authoritative paths. Functions Admin SDK bypasses rules, so every Function validates auth, membership, phase, schema, ownership, allowed transitions, and idempotency itself. Owner dossier read is a dedicated document path and owner UID must equal path UID; host gets no exception. Solution and ballots remain server-only until a server transition sets resolved state; final resolution is copied into a distinct member-readable result.

For private chat, Functions create immutable messages. Rules permit room members to read public messages and a recipient/sender to read private messages, using query shapes that include matching visibility/recipient predicates; alternatively serve private message pages via a callable to avoid rules/query mismatch. Do not load all messages and filter client-side. Deals expose only parties; an accepted outcome may be emitted as a deliberately public event if game design requires it. Do not put secrets or unfiltered case JSON in any public document.

## 4. Presence and synchronization

Use RTDB at `presence/{roomId}/{uid}/{connectionId}` with authenticated rules requiring the UID path to match `auth.uid` and membership claim/check strategy. Android creates a unique connection child, listens to `/.info/connected`, registers `onDisconnect().removeValue()` before setting its connection record, and refreshes connection time. Multiple devices are separate children; Cloud Function on disconnect/RTDB event updates Firestore member online status only when the last connection disappears. This online flag is advisory, never a basis for trusting game actions.

Firestore snapshot listeners drive room summary, members, public case/evidence, permitted messages/deals, own dossier and (after resolution) final result. `ServerTimestamp` fields are written by Functions; app displays local estimates but treats server values as canonical. Timers store `phaseStartedAt`, `phaseDeadline`, and `phaseRevision`; clients calculate remaining display time from server offset but only Functions advance phase. On reconnect/app restart, authenticate, resolve stored room ID, reattach allowed listeners, fetch own dossier and current revision, reconcile stale local UI from Firestore, and re-register RTDB presence. Never restore authoritative room state from process memory.

## 5. Android integration without replacing UI

Current composition root: `MainActivity` gates on `FirebaseAuthRepository`; `TheAlibiViewModel` constructor currently defaults to `TheAlibiRepository()`. Replace that dependency with an interface (e.g. `AlibiRepository`) and a Firebase-backed online implementation injected by a ViewModel factory. Keep the existing Compose screens and `TheAlibiUiState` where possible, mapping Firestore snapshots into its existing UI state. Avoid constructor-created in-memory multiplayer state.

Replace call paths at `TheAlibiViewModel` methods: `createRoom`, `joinRoom`, `togglePlayerReady`, `generateRoomMystery`, phase/character reveal transitions, evidence discovery/share, `sendPrivateMessage`, deal actions, `submitBallotVote`, and resolution/tally. These currently call synchronous/local `TheAlibiRepository` methods. Make asynchronous operations use `viewModelScope` and expose loading/errors. UI event signatures can stay stable initially while repository methods become suspend/Flow-based.

`TheAlibiRepository` currently owns case catalogue/sample content, local generation, in-memory gameplay, and OkHttp/WebSocket helpers. Extract reusable pure case/UI mapping logic as needed; do not silently use its local room/game state in online mode. Offer explicit “Demo game (offline)” mode only if it is visibly selected and isolated from online rooms. Once Firebase online path is complete, remove legacy network calls and Ktor dependencies in a separately validated retirement phase.

## 6. Case integrity, Gemini and Storage

Port/reuse Ktor `GeminiClient`, `CaseGenerationEngine`, `CaseValidator`, and `ImageGenerationPipeline` under trusted Functions/task worker, or keep them behind a secured server that verifies Firebase ID tokens and writes Firestore/Storage with Admin credentials. The current Ktor `RoomRepository` is in-memory and should not be reused as room authority. No live Gemini call/deployment is verified today.

At freeze, save a canonical roster ordered by server seat. Generation receives that roster count and stable UIDs; output validation must check: exactly one culprit is assigned to a human; victim character is NPC; all other human character assignments are innocent; assignments are unique; victim is not assigned; timeline events are chronologically coherent; all evidence references resolve; at least the planned deduction path connects available evidence to culprit/motive/opportunity; no public case field contains culprit, solution, hidden timeline or private secrets. Run one bounded repair prompt with explicit validator errors, then deterministic validated fallback case. On API/repair failure, mark generation failed with retry option or use the bundled Blackwood case only after the same invariants pass for the frozen count. Never expose partially generated roles or solution. Record model/version and validator result server-side.

Storage paths: `rooms/{roomId}/cases/{caseVersion}/poster.jpg`, `characters/{characterId}.jpg`, `scenes/{sceneId}.jpg`, `evidence/{evidenceId}.jpg`, `twists/{twistId}.jpg`. Functions create and upload immutable objects; app receives Storage SDK references/download URLs only after room membership and release rules. Storage rules deny client writes; reads require authenticated room membership and public/released asset status. If rules cannot safely consult Firestore for a given path, issue short-lived server-authorized download URLs after checking membership. Treat URLs as bearer secrets, avoid logging them. Missing/failed art uses existing bundled/default placeholder and never blocks a playable match; record asset status and retry generation separately. No Storage rules or complete Firebase image pipeline exist currently.

## 7. Testing and release gates

Use Firebase Emulator Suite for Auth, Firestore, RTDB, Functions and Storage. Add local Functions/rules package scripts and deterministic fake Gemini/image providers so all gameplay tests run offline without secrets. Assert rules explicitly and run clients under multiple authenticated contexts. Required test groups: anonymous persistence/link UID; collision handling; nonmember denial; room code collision and simultaneous capacity boundary; duplicate/idempotent joins; ready/freeze race; roles invariant; dossier isolation including host denial; evidence phase/ownership; private message visibility/query; deal transitions; phase/timer races; ballot secrecy/create-only/duplicate rejection; resolution/scoring idempotency; scheduled expiry; Storage path access; Functions validation; app listener restoration.

Four-client emulator end-to-end: four distinct Firebase Auth UIDs join; verify same roster and ready state; freeze; generate/validate a case; verify exactly one human culprit and NPC victim; read only each owner's dossier; discover/share evidence; message/deal; advance timed phases; submit simultaneous ballots; resolve once; reconnect one client after network interruption and verify revision/result convergence. Add contention tests that run many join attempts against limited slots and assert capacity never exceeded. These validate emulator behavior, not production Android, Google Play services, real network scheduling, or deployment IAM.

Manual device gate: at least four physical Android devices or a defined device/emulator mix, release-like Firebase project, anonymous entry, optional account link, create/join, background/kill/relaunch, airplane-mode interruption, deadline display, full match, Storage assets, Auth collision path, and App Check. Record build SHA, Android/API versions, project/environment, sanitized UIDs, and observed outcomes. Production rule/function deployments and actual Gemini generation require a separate controlled integration run. Do not claim these are complete before performed.

## Recommended implementation sequence

1. Phase 0: preserve UI behavior with a repository interface and emulator test harness; no gameplay feature migrated yet.
2. Phase 1: anonymous auth + optional linking and UID plumbing.
3. Phase 2: least-privilege rules/schema plus callable room creation/join/readiness/freeze and RTDB presence.
4. Phase 3: wire lobby UI/ViewModel to Firebase and pass four-client join/sync gate.
5. Phase 4: trusted case generation, validation, assignments and private dossier isolation.
6. Phase 5: evidence, chat, deals/interactions, phase/timer and sealed ballot/resolution Functions.
7. Phase 6: Storage generation/rules, full match tests, Android device validation, then Ktor retirement decision.

**Smallest independently testable first phase:** Phase 0 repository seam + Firebase Emulator Suite bootstrap and anonymous Auth integration test harness, without switching production gameplay yet. It is low risk, yields a repeatable test foundation, and verifies Auth UID acquisition. The first user-visible gameplay slice after that should be atomic room create/join/readiness through Functions, with emulator contention and rules tests.
