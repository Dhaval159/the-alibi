# Firebase migration verification audit

**Audit date:** 2026-11-07  
**Scope:** source/configuration inspection of the checked-out repository. No runtime code was modified. Emulator and device execution were not available in this environment.

## Executive finding

The Firebase migration is **partial and not wired into the playable application**. `MainActivity` uses Firebase Auth to gate the app, but `TheAlibiViewModel` constructs and uses the old in-memory `TheAlibiRepository`. The new Firestore repositories are not referenced by application call paths. No Cloud Functions, Realtime Database, Storage rules/configuration, or deployed-function evidence exists in this checkout. The current multiplayer path therefore remains local-memory/WebSocket/Ktor code, not Firebase multiplayer.

## Authentication

- **Implemented:** `FirebaseAuthRepository` integrates Android Credential Manager Google ID tokens with Firebase Auth; exposes the current user through `StateFlow`; uses `FirebaseAuth.currentUser`, which Firebase persists across process/app restarts. `MainActivity` constructs it and displays `GoogleSignInScreen` whenever `currentUser == null`.
- **Mandatory:** Google sign-in is mandatory in the current app flow. There is no anonymous sign-in call or anonymous-auth dependency/config evidence. `attemptAutoSignIn` exists but `MainActivity` does not call it; it checks an existing Firebase user and tries Credential Manager only after `currentUser` is absent. Thus persistence, rather than the method, is the automatic returning-user path.
- **Device verification:** **Unverified.** No Android device/emulator test was run; signing/OAuth client configuration and a real Google/Firebase round trip were not exercised.
- **Room identity:** Firebase repositories use authenticated `Firebase.auth.currentUser.uid`, but are not called by the ViewModel. Active room/player IDs instead come from random local `player_...` IDs and `tok_...` session tokens in `TheAlibiRepository`. Firebase UID is therefore not associated with current room membership.
- **No-registration recommendation:** enable Firebase Anonymous Authentication and sign users in anonymously before entering the app, with an optional later Google link. This retains a stable Firebase UID on the same installation while avoiding a registration screen. Clarify that anonymous identity may not survive app data clearing/reinstall. Link credentials to the existing anonymous `FirebaseUser` with `linkWithCredential`; do not sign out and sign in as a new user. This preserves UID and room ownership. Implement collision recovery for a Google credential already linked to another UID (explicit account merge/membership migration is needed in that case).

## Firestore architecture and call paths

`FirebaseRoomRepository` uses `rooms/{room_$code}` and `players/{uid}`; it offers room/player snapshot flows and ready updates. `FirebaseGameplayRepository` offers dossier, evidence, message, and ballot operations/flows. Their existence does **not** mean the app uses them: repository search finds no instantiation/use outside their own declarations. `FirestoreModels.kt` supplies DTOs.

Current `TheAlibiRepository` is the ViewModel's actual dependency. It stores rooms in `activeRooms: MutableMap`, creates random room codes/identities, holds game state in `StateFlow`, and has WebSocket plus REST helper calls. Its usual create/join/ready/game operations mutate local memory; WebSocket connection is not called by the ViewModel's create/join path. Some helpers use the Ktor endpoints (including character and resolution retrieval), but server sync is not an integrated Firebase replacement.

| Capability | Firebase implementation status | Actual current behavior / gap |
|---|---|---|
| Create room | Partial, unused | Non-transactional two writes (`room.set`, then host player set). Code uniqueness is only document-ID collision; code is not checked transactionally. A failed second write leaves orphan room. |
| Join room | Partial, unused | Reads then writes player and increments count separately. No transaction/capacity/status/duplicate guard; concurrent joins can overfill and count can drift. Rejoining overwrites player but increments again. |
| Ready state | Partial, unused | Writes own `isReady`; rules allow broader player mutation. |
| Host permissions | Unsafe | Room rules allow host to replace any valid room fields, including status/count/case. Host status is client-supplied in player documents. |
| Roster freeze | Missing in Firebase | Local `internalFrozenRoster`; no Firestore frozen roster or transaction/authority. |
| Disconnect/reconnect | Missing in Firebase | No RTDB SDK/dependency or presence repository. Old Ktor/local path has connection state, but not integrated with Firestore. |
| Cross-client sync | Partial/unverified | Firestore snapshot `Flow`s exist but are unused. Current `StateFlow` is local; old server has WebSocket implementation, but client connection path is not integrated into normal room flow. |
| Cleanup/expiry | Missing | No TTL policy, scheduled function, cleanup worker, RTDB disconnect handler, or room expiry field enforcement identified. |

The Firestore database ID is loaded from generated resources and configured in `firebase.json`. `app/google-services.json` targets a configured Firebase project. This proves configuration artifacts exist, not that deployed rules/database or runtime connectivity are working.

## Server authority

No `functions/` source, `package.json` for Functions, callable/HTTPS function references, Firebase Functions Android dependency, or deployment config was found. No deployed functions can be established from repository evidence. All critical authoritative operations remain **missing** on Firebase. The old Ktor backend has corresponding room/game routes and a `RoomRepository`, but it is not a Firebase Cloud Functions substitute and is in-memory.

Required trusted operations: atomic room create/join and code allocation; membership validation and host claims; roster freeze; case generation and validation; character/culprit assignment; private dossier write; evidence unlock/share; interrogation and private interactions; deal transitions; phase transitions/timers; ballot acceptance/counting; resolution/scoring; and room cleanup. Until migrated, clients can forge game state wherever rules permit direct writes, while no Firebase server resolves/scorers matches.

## Gemini and cases

- No Gemini call exists in Android application sources despite `firebase-ai` dependency and UI copy. `TheAlibiRepository.generateCase` reports progress with delays and calls local `createGeneratedCase`; room generation also calls the local deterministic generator. This is **not live Gemini**.
- A separate Ktor `GeminiClient` calls the Generative Language REST API using server environment variable `GEMINI_API_KEY`; `CaseGenerationEngine` calls it and runs `CaseValidator`, repair attempts, then procedural fallback. `RoomRoutes`/`Application` wire this in the old backend. No deployment or Android call path to that service was verified. Thus a trusted backend implementation exists in source, but live production integration is **unverified**.
- No Gemini key was found in Android Kotlin/resources. `.env` is ignored; the backend reads a process environment variable. The Ktor code itself is not proven deployed or protected by Firebase Auth.
- Generator source attempts validation/repair, but the validator checks structural consistency and evidence references; it does not prove one human culprit, NPC victim, all other humans innocent, temporal logic consistency, or uniquely solvable evidence in all generated cases. Server assignment/game rules need stronger invariant checks. Frozen roster is not supplied through current Android generation path.
- Generation errors fall back to a procedural case in `CaseGenerationEngine`; tests use fake key and do not establish live API success. Android local generator is the effective current behavior.

## Images and storage

`ImageGenerationPipeline` in the Ktor source requests a poster, character portraits, crime scene, and first four evidence images, writes them to local `generated_cases/{caseId}` folders, and returns asset IDs. `GeminiClient.generateImage` returns null without a key, allowing absent assets; no Storage upload/download code or Storage rules are present. Twist artwork generation is not implemented in the inspected pipeline. No reliable Android fallback image/storage lifecycle was verified. The Google services file has a storage bucket name, which alone does not configure an app pipeline. End-to-end generation/upload/download is **missing/unverified**.

## Presence, synchronization, Ktor retirement

Realtime Database is absent from Gradle dependencies and app source. There is no `.info/connected`/`onDisconnect()` implementation. Firestore `FieldValue.serverTimestamp()` is used by unused repository writes/listeners, but canonical server timestamps and synchronized phase/timer state do not exist in actual app call paths.

Ktor has not been retired. `backend/server` contains Ktor routes, WebSockets, Gemini/image components and in-memory repositories. Android `TheAlibiRepository` keeps OkHttp, a default `http://10.0.2.2:8080`, WebSocket connection logic, and synchronous REST helpers (`my-character`, `resolution`). Gradle includes Retrofit/OkHttp and logging dependencies; Retrofit declarations appear unused in the inspected sources, while OkHttp is used by legacy repository. Manifest permits cleartext HTTP. The app gameplay repository remains local state, and Ktor endpoints are optional/partial helpers rather than a clearly wired deployable production service. Do not remove Ktor until call paths and product decision are resolved.

## Verification performed and limits

- Inspected repository implementations, ViewModel wiring, models, Firestore rules/tests, Gradle catalogs, Firebase config, backend source, and image/generation code.
- `firebase`, `adb`, Java, and Firebase Emulator Suite executables were not available in PATH; no Node Firebase test dependencies/package manifest were present. Firestore rules tests were therefore **not executed**. No Android device/emulator run, live Firebase/Gemini request, or independent-client multiplayer test was performed.
- Existing Android unit tests target the local repository, not Firebase integration. Backend tests target in-memory Ktor structures and fake Gemini keys, not deployed Firebase or live Gemini.

## Prioritized next steps

1. Keep this as a verification gate: choose anonymous-first Firebase Auth with optional account linking, then wire the ViewModel to Firebase repositories.
2. Design and implement trusted Cloud Functions for room/game invariants; move all authoritative writes behind them and redesign Firestore documents/rules around least privilege.
3. Add Emulator Suite setup/dependencies and executable rules tests; first cover unauthorized writes, host impersonation, private data, vote secrecy/immutability, and room join races.
4. Integrate presence with RTDB and server timestamps/phase deadlines; add scheduled room expiry.
5. Port the existing Gemini generation and validation into trusted functions or a secured backend; feed a frozen roster and assert culprit/victim/innocence invariants. Decide whether Ktor is retained as that backend or retired after parity.
6. Implement Storage rules and upload/download flow with failure fallbacks; verify with emulators and real clients.
7. Run four-client, race, disconnect/reconnect and full-match tests, then real Android/OAuth validation before declaring migration complete.
