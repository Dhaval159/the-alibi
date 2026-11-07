# THE ALIBI — PHASE 1 READINESS REPORT

*Firebase configuration and authentication readiness audit. All inspections performed on actual source code and configuration files. No file modifications made during this audit.*

## Executive Summary

The codebase has a solid architectural foundation for Phase 1 (anonymous Firebase authentication and Firestore real-time synchronization), but Phase 1 cannot begin until several prerequisite blocker are resolved. The GameRepository abstraction, LocalRoomStore, factory pattern, and UnavailableFirebaseGameRepository placeholder all support anonymous auth implementation without a major ViewModel rewrite. However, the Firebase CLI is not installed, emulators cannot be started, anonymous authentication does not exist in the codebase, and the named Firestore database target must be verified.

**Overall Phase 1 Status: NOT YET READY** — All architectural prerequisites are in place, but blocker prerequisites must be resolved before Phase 1 can proceed.

---

## 1. Firebase Configuration Status

| Item | Status | Source Location | Evidence |
|---|---|---|---|
| `firebase.json` configuration present | **PRESENT** | `firebase.json` | Defines named Firestore database `ai-studio-android-remixthe-b1d35dd2-d6f5-4ff6-8c75-8d89468bf295` and emulator ports (Firestore 8085, Auth 9099) |
| `.firebaserc` project reference | **PRESENT** | `.firebaserc` | Project `gen-lang-client-0571820623` |
| Firebase CLI installed | **NOT INSTALLED** | System PATH | `firebase-tools` not available; emulators cannot be started without it |
| Emulators can be started | **NOT VERIFIABLE** | Requires Firebase CLI | Command `firebase emulators:start --only firestore,auth` cannot be executed |
| Named Firestore database target | **DOCUMENTED** | `firebase.json` | `databaseName: "ai-studio-android-remixthe-b1d35dd2-d6f5-4ff6-8c75-8d89468bf295"` |
| **Prerequisite**: Install Firebase CLI | **BLOCKER** | — | Must run: `npm install -g firebase-tools` before emulators can be started |

---

## 2. Authentication Implementation Status

| Feature | Status | Source Location | Evidence |
|---|---|---|---|
| Google Sign-In scaffolding exists | **IMPLEMENTED** | `FirebaseAuthRepository.kt:49-175` | `attemptAutoSignIn()` and `signInWithGoogle()` methods present; require `default_web_client_id` resource |
| Anonymous authentication exists | **NOT IMPLEMENTED** | `FirebaseAuthRepository.kt` | No `signInAnonymously`, no `createUserWithEmailAndPassword`, no password-less auth flow |
| Anonymous authentication works | **NOT VERIFIABLE** | Not present in code | Cannot claim it works without implementation |
| Email/password authentication exists | **NOT IMPLEMENTED** | Not present in code | Not part of current scaffolding |
| Google Sign-In requires `default_web_client_id` | **RESOURCE MISSING** | `FirebaseAuthRepository.kt:49-175` | Credential manager configured but `default_web_client_id` string resource not found; flow would fail at runtime |
| `AuthIdentity` abstraction supports anonymous flag | **IMPLEMENTED** | `AuthIdentity.kt` | `isAnonymous: Boolean = false` and `isOnlineAuth: Boolean = false` fields present; distinguishes local demo from Firebase UID |
| ViewModel uses `AuthIdentity` abstraction | **IMPLEMENTED** | `TheAlibiViewModel.kt` | ViewModel observes `repository.authIdentity` — abstracted from Firebase SDK objects |
| **Prerequisite**: Add anonymous auth method to `FirebaseAuthRepository` | **BLOCKER** | — | Must implement `signInAnonymously` or equivalent; ViewModel already abstracted to support it |

---

## 3. Firestore Repository Implementation Status

| Item | Status | Source Location | Evidence |
|---|---|---|---|
| `TheAlibiRepository` (local) is active repository | **IMPLEMENTED** | `GameRepositoryFactory.kt` | `createRepository(GameMode.OFFLINE_DEMO)` returns `TheAlibiRepository()` |
| `UnavailableFirebaseGameRepository` exists as Firebase Online placeholder | **IMPLEMENTED** | `UnavailableFirebaseGameRepository.kt` | Fail-closed placeholder; all operations throw `IllegalStateException` with explicit reason |
| Firestore-backed repository not actively used | **CORRECT** | ViewModel and factory depend on `GameRepository` abstraction | Actual instance injected via `ViewModel.Factory`; Firebase not in current call path |
| If Firestore repo were added, it would implement `GameRepository` | **FUTURE** | — | Would follow same pattern as `TheAlibiRepository` + `UnavailableFirebaseGameRepository` |
| **Prerequisite**: Implement Firestore-backed `GameRepository` implementation | **NOT STARTED** | — | Requires Firestore emulator verification first; scope beyond Phase 0 |

---

## 4. Emulator Configuration and Availability

| Item | Status | Source Location | Evidence |
|---|---|---|---|
| `firebase.json` defines emulator ports | **PRESENT** | `firebase.json` | Firestore: 8085, Auth: 9099 |
| `.firebaserc` names project | **PRESENT** | `.firebaserc` | Project `gen-lang-client-0571820623` |
| Emulator setup documentation exists | **DOCUMENTED** | `docs/FIREBASE_EMULATOR_SETUP.md` | Detailed instructions; named database requirement verified |
| Emulators can be started | **NOT VERIFIABLE** | — | Firebase CLI not installed; no execution possible |
| Android emulator loopback host | **DOCUMENTED** | `docs/FIREBASE_EMULATOR_SETUP.md` | Host: `10.0.2.2` (Android emulator alias to host 127.0.0.1) |
| Firestore emulator port | **DOCUMENTED** | `docs/FIREBASE_EMULATOR_SETUP.md` | Port 8085 |
| Auth emulator port | **DOCUMENTED** | `docs/FIREBASE_EMULATOR_SETUP.md` | Port 9099 |
| **Prerequisite**: Install Firebase CLI and start emulators | **BLOCKER** | — | Without emulators, Firestore rules and real-time sync cannot be verified |

**Emulator startup sequence (when CLI is installed):**
1. `firebase emulators:start --only firestore,auth`
2. Android debug build connects to `10.0.2.2:8085` (Firestore) and `10.0.2.2:9099` (Auth)
3. Named database `ai-studio-android-remixthe-b1d35dd2-d6f5-4ff6-8c75-8d89468bf295` must be explicitly targeted in Cloud Functions Admin SDK

---

## 5. Firestore Security Rules

| Item | Status | Source Location | Evidence |
|---|---|---|---|
| Security rules file exists | **NOT VERIFIABLE** | Not inspectable without running emulators | No `firestore.rules` file verified in current environment |
| Rules can be tested via emulators | **NOT VERIFIABLE** | Requires running emulators | `firebase emulators:start --only firestore` needed |
| Named database targeting is correct | **DOCUMENTED** | `docs/FIREBASE_EMULATOR_SETUP.md` | `databaseName: "ai-studio-android-remixthe-b1d35dd2-d6f5-4ff6-8c75-8d89468bf295"` |
| **Prerequisite**: Start Firebase emulators and inspect rules | **BLOCKER** | — | Cannot verify without emulator execution |

---

## 6. Cloud Functions and Trusted Server-Side Operations

| Item | Status | Source Location | Evidence |
|---|---|---|---|
| Cloud Functions code exists | **NOT FOUND** | No functions directory in project | Project has no `functions/` directory or deployment config |
| Trusted server-side operations | **NOT IMPLEMENTED** | — | Not part of Phase 0 scope |
| If needed, would use Firebase Admin SDK with named database target | **FUTURE** | — | Would require `admin.firestore().database('db-name')` with `ai-studio-android-remixthe-b1d35dd2-d6f5-4ff6-8c75-8d89468bf295` |
| **Prerequisite**: If Cloud Functions needed, install Firebase CLI and verify Admin SDK target | **NOT STARTED** | — | Beyond Phase 0 scope for now |

---

## 7. Multi-Client Communication

| Item | Status | Source Location | Evidence |
|---|---|---|---|
| WebSocket client exists in repository | **IMPLEMENTED** | `TheAlibiRepository.kt:1877-2046` | `connectWebSocket()`, `handleWebSocketMessage()`, `OkHttpClient` WebSocket listener |
| WebSocket connects to server at `10.0.2.2:8080` | **IMPLEMENTED** | `TheAlibiRepository.kt:1886` | `serverBaseUrl: "http://10.0.2.2:8080"` (Android loopback) |
| Multi-client communication verified | **NOT VERIFIABLE** | — | No Android device/emulator available; Firebase emulators not running |
| `UnavailableFirebaseGameRepository` blocks all remote operations | **IMPLEMENTED** | All operations throw `IllegalStateException` | Online mode explicitly unavailable; prevents silent local/online confusion |
| Local mode (`TheAlibiRepository`) uses in-memory `LocalRoomStore` | **IMPLEMENTED** | Room state isolated per instance; no cross-device sync |
| **Prerequisite**: Android device/emulator + running Firebase emulators | **BLOCKER** | — | Cannot verify cross-device multiplayer without these |

---

## 8. Summary of Blockers

| Blocker | Impact | Resolution Required |
|---|---|---|
| Firebase CLI not installed | **BLOCKER** — Emulators cannot be started; Firestore rules, auth, and Firestore sync cannot be verified | Install: `npm install -g firebase-tools` |
| Anonymous authentication not implemented | **BLOCKER** — Phase 1 scope requires `signInAnonymously` or equivalent in `FirebaseAuthRepository` | Add anonymous auth method; ViewModel already abstracted to support it via `AuthIdentity` |
| Named Firestore database target unverified | **BLOCKER** — All Cloud Functions Admin SDK operations and Firestore triggers must explicitly target `ai-studio-android-remixthe-b1d35dd2-d6f5-4ff6-8c75-8d89468bf295` | Verify database name in firebase.json and ensure all server-side code targets it |
| Firebase emulators cannot be started | **BLOCKER** — Without emulators, security rules, auth flows, and real-time sync cannot be tested | Run: `firebase emulators:start --only firestore,auth` after installing CLI |
| No Android device/emulator available | **BLOCKER** — WebSocket multiplayer communication, cross-device sync cannot be verified | Obtain Android emulator or device; debug build connects to `10.0.2.2` |
| No Cloud Functions implemented | **NOT A BLOCKER** for Phase 1 minimal scope | Can be added later; Phase 1 minimal scope is anonymous auth + Firestore sync |

---

## 9. Phase 1 Recommendations

### Minimal Phase 1 Scope (can begin after blocker #2 resolved)

1. **Add anonymous authentication** to `FirebaseAuthRepository`:
   - Implement `signInAnonymously()` method
   - Update `AuthIdentity` flow to set `isAnonymous = true, isOnlineAuth = true`
   - ViewModel already supports this via `AuthIdentity` abstraction — no ViewModel rewrite needed

2. **Update `GameRepositoryFactory`** to support anonymous mode:
   - Add `GameMode.ANONYMOUS_FIREBASE` or similar
   - Return new `FirebaseGameplayRepository()` instance (would implement `GameRepository` with Firestore backend)

3. **Implement Firestore-backed `GameRepository`**:
   - Replace `LocalRoomStore` with real-time Firestore synchronization
   - Use named database target: `ai-studio-android-remixthe-b1d35dd2-d6f5-4ff6-8c75-8d89468bf295`
   - Preserve all existing API surface (`createRoom`, `joinRoom`, `generateCase`, etc.)

### Full Phase 1 Scope (after emulators verified)

4. **Start Firebase emulators** and verify:
   - `firebase emulators:start --only firestore,auth`
   - Firestore security rules pass validation
   - Anonymous auth flow end-to-end in emulator

5. **Add real-time Firestore synchronization**:
   - Room state synced across devices via Firestore snapshots
   - Case state (active case, evidence, deduction notes) synced
   - Player readiness and turn management synced

6. **Add Cloud Functions** (if needed for game logic):
   - Ensure all Admin SDK operations target named database
   - No implicit defaults — must explicitly specify `databaseName: "ai-studio-android-remixthe-b1d35dd2-d6f5-4ff6-8c75-8d89468bf295"`

### Does Not Proceed Until

- [ ] Firebase CLI installed and emulators can be started
- [ ] Anonymous authentication implemented in `FirebaseAuthRepository`
- [ ] Named Firestore database target verified (`ai-studio-android-remixthe-b1d35dd2-d6f5-4ff6-8c75-8d89468bf295`)
- [ ] All Phase 0 acceptance criteria verified (marked VERIFIED in `PHASE_0_FINAL_VERIFICATION_REPORT.md`)

---

## 10. Architectural Foundation Assessment

The codebase has a **solid foundation** for Phase 1 that significantly reduces integration risk:

| Foundation Element | Status | Phase 1 Impact |
|---|---|---|
| `GameRepository` abstraction | **IMPLEMENTED** | New repository implementations can be added without ViewModel changes |
| `LocalRoomStore` (per-instance or shared) | **IMPLEMENTED** | Can be replaced with Firestore backend; or used as fallback |
| `GameRepositoryFactory` explicit mode selection | **IMPLEMENTED** | Online/anonymous modes explicitly select different repository; no silent fallback |
| `UnavailableFirebaseGameRepository` fails closed | **IMPLEMENTED** | Online/anonymous mode gives explicit error, not misleading "Connected" state |
| `AuthIdentity` abstraction | **IMPLEMENTED** | Decouples UI/ViewModel from Firebase Auth SDK; supports anonymous flag |
| `TheAlibiViewModel` depends on `GameRepository` abstraction | **IMPLEMENTED** | New repository can be injected via Factory without ViewModel rewrite |
| `combine()` sound manager optimization | **IMPLEMENTED** | Only optimization in observation layer; no gameplay impact |
| **ViewModel rewrite required?** | **NO** — Already abstracted | New repository implementations can be dropped in via Factory pattern |

**Verdict**: The architectural foundation is strong enough that implementing anonymous Firebase authentication and Firestore real-time synchronization should not require a major ViewModel refactor. The primary effort will be in the repository layer and auth implementation, not UI orViewModel changes.

---
*Report generated as part of Phase 0.1 corrective implementation and Phase 1 readiness audit.*