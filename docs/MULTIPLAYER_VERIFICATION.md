# Multiplayer verification report

**Audit date:** 2026-11-07  
**Status:** multiplayer Firebase verification is **not performed**. Findings below separate source inspection from tests actually run.

## System under test found in source

The UI's `TheAlibiViewModel` uses `TheAlibiRepository`. That repository keeps rooms, roster, readiness, assignments, evidence, chat, ballots, timer, and resolution in process-local state (`MutableStateFlow` and `activeRooms`). IDs/tokens are random locally generated values, not authenticated Firebase UID memberships. A WebSocket client and synchronous REST helpers target a configurable-looking property that defaults to `http://10.0.2.2:8080`, but normal ViewModel create/join code does not establish a Firebase listener or connect the WebSocket. WebSocket behavior cannot make separate app processes share the local map.

Firebase snapshot flows and writes exist in `FirebaseRoomRepository` / `FirebaseGameplayRepository`, but code search found no application call sites. Those classes are not the system under test in current UI multiplayer.

The Ktor backend implements WebSocket routes and in-memory server room state, but there is no confirmed deployment/configuration and the Android connection path is partial. The server test suite is for this backend's in-memory room implementation, not Firebase multiplayer.

## Requested scenarios

| Scenario | Result | Evidence / limitation |
|---|---|---|
| Four independent clients join one room | **Not run; Firebase path not wired** | No independent clients, emulator, or device available. Local map is process-local; Ktor join endpoint exists in source. |
| Simultaneous joining | **Not run; unsafe Firebase design** | `joinRoom` performs read, player set, and count increment separately. No transaction/capacity check. Concurrent overfill/count drift and duplicate increments on rejoin are possible. |
| Ready-state synchronization | **Not run; local only in active app path** | Local `togglePlayerReady` mutates local room; Firebase method exists but unused. |
| Host starts match | **Not run; not server-authoritative on Firebase** | Local app checks host and conditions in Kotlin. Firestore room rules let host set any valid status. |
| Frozen roster consistency | **Not run; local only** | `generateRoomMystery` copies roster to in-memory `internalFrozenRoster`. No persisted Firebase frozen roster. |
| Private dossier isolation | **Not run** | Firestore rule intends owner-only except host (host can read all); Firebase write methods permit caller-specified UID and rules permit owner self-write. No emulator execution. |
| Shared evidence synchronization | **Not run** | Firebase snapshots are unused; local state is local. Firestore rules let users write arbitrary evidence fields. |
| Private messaging | **Not run** | Firebase method/listener unused; current repository local messages. Ktor routes exist. Firestore individual rules filter sender/recipient but membership and query compatibility are not verified. |
| Simultaneous voting | **Not run** | Firebase ballot path is canonical UID but writes mutable and not phase-gated; current UI voting is local. |
| Resolution synchronization | **Not run** | Current local resolution; legacy REST getter exists. No trusted Firebase resolver. |
| Reconnection after temporary disconnect | **Not run; Firebase presence missing** | No RTDB or Firestore online-presence handler. Ktor WebSocket has server-side session tracking in source, but no verified client lifecycle/reconnect restoration; local fallback state is not durable. |
| Consistent phase changes/server timestamps | **Not run** | Firestore uses server timestamp sentinels in unused methods, but no canonical server transition/timer service or synced client call path. Local timer/phase state is not shared. |

## Test execution record

- **Firebase Emulator Suite:** not run. The environment did not expose `firebase`, `adb`, Java, or installed emulator test dependencies; repository has no Node package manifest declaring `@firebase/rules-unit-testing`. `firebase.json` configures Firestore emulator at `127.0.0.1:8085` and Auth at `127.0.0.1:9099`, but configuration is not evidence an emulator is installed or running.
- **Actual Android devices:** none tested.
- **Independent authenticated clients:** none tested.
- **Existing `firestore.test.js`:** inspected, not executed. It contains a small set of intended rules checks, not multiplayer workflows. It seeds fixtures with rules disabled and does not cover race conditions, host escalation, mutable ballots, membership, or complete sync.
- **Android unit/instrumentation tests:** not run. Existing `TheAlibiGameTest` exercises local in-memory gameplay; it does not validate Firebase repositories, auth, rules or sync.
- **Ktor backend tests:** not run as part of this audit. Even if passing, they would not establish deployed backend/client connectivity or Firebase correctness.

## Readiness decision

Do not describe the Firebase migration as complete or Firebase multiplayer as working. The current source supports local single-process gameplay and contains partial Firebase repository code plus legacy backend code. Real multiplayer, security, reconnection and cross-device synchronization remain **unverified**, and the Firebase architecture currently has material direct-write vulnerabilities (see `FIREBASE_SECURITY_AUDIT.md`).

## Verification sequence after wiring and hardening

1. Configure Emulator Suite dependencies and assert Firebase Auth UID persists/re-auths; exercise anonymous-first and Google linking while preserving UID.
2. Run multi-client emulator tests with four distinct auth contexts: create, join concurrently, ready, freeze, disconnect/reconnect and verify all clients observe the same roster and server state.
3. Attempt all forbidden direct writes/reads from distinct clients, including host and nonmember accounts. Verify rules deny them and callable functions enforce phase and membership.
4. Complete a full match through Functions: generation, role assignment, evidence, private messages, deals, phase/timers, sealed voting, resolution/scoring; verify clients converge after listener reconnect.
5. Repeat on multiple real Android devices with production-like Auth, App Check, network interruption and app process death. Record device/OS/build, test accounts, run IDs and outcomes.
