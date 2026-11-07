# THE ALIBI — PHASE 1A IMPLEMENTATION REPORT
## Firebase Foundation & Anonymous Authentication

**Project:** THE ALIBI — Android Online Multiplayer Social Deduction Game  
**Phase:** Phase 1A  
**Date:** November 7, 2026  
**Status:** COMPLETED & VERIFIED  

---

## 1. Executive Summary

Phase 1A of the Firebase migration for **THE ALIBI** has been successfully implemented and verified. This phase establishes the core Firebase foundation and anonymous authentication system, ensuring that any user can immediately enter and play the game without being blocked by a mandatory Google Sign-In gate. Every player is assigned a stable Firebase UID that persists across app restarts, with the option to link a Google account for cross-device backup.

The entire implementation complies strictly with the **Firebase Spark (no-cost) plan**.

---

## 2. Non-Negotiable Cost Requirements (Spark Plan Adherence)

> [!IMPORTANT]
> **Zero Paid Infrastructure Commitment:**
> - Plan: **Firebase Spark (Free Tier) ONLY**.
> - Prohibitions strictly upheld:
>   - No Firebase Blaze upgrade.
>   - No billing account added.
>   - No Cloud Functions.
>   - No Cloud Run.
>   - No paid hosting or paid third-party servers.
> - Authentication uses standard Firebase Anonymous Auth, which is permanently free under Spark quotas.
> - Emulator testing runs 100% locally on developer machines at zero cost.

---

## 3. Project Configuration & Target Verification

| Setting | Verified Target | Location |
|---|---|---|
| **Android Application ID** | `com.aistudio.applet.cihhgm` | `app/build.gradle.kts:17` |
| **Firebase Project ID** | `gen-lang-client-0571820623` | `app/google-services.json:4`, `.firebaserc:3` |
| **Google Services Plugin** | `com.google.gms.google-services` | `app/build.gradle.kts:9` |
| **Named Firestore Database** | `ai-studio-android-remixthe-b1d35dd2-d6f5-4ff6-8c75-8d89468bf295` | `firebase_applet_config.xml:3`, `firebase.json:4` |
| **Auth Emulator Port** | `9099` | `firebase.json:15`, `FirebaseEmulatorConfig.kt:25` |
| **Firestore Emulator Port** | `8085` | `firebase.json:11`, `FirebaseEmulatorConfig.kt:26` |

---

## 4. Key Architectural Implementations

### 4.1 Authentication Abstraction (`AuthRepository.kt`)
Created `app/src/main/java/com/example/data/firebase/AuthRepository.kt` defining:
- `val authIdentity: StateFlow<AuthIdentity?>`: Observable domain model (`uid`, `displayName`, `isAnonymous`, `isOnlineAuth`).
- `val authState: StateFlow<AuthState>`: Observable lifecycle states (`Initial`, `Loading`, `Authenticated`, `Unauthenticated`, `Error`).
- `suspend fun signInAnonymously(): Result<AuthIdentity>`: Contract for guest entry.
- `suspend fun resetAuth(): Result<Unit>`: Contract for development/testing reset.

### 4.2 Firebase Auth Implementation (`FirebaseAuthRepository.kt`)
Updated `app/src/main/java/com/example/data/firebase/FirebaseAuthRepository.kt` to:
- Implement `AuthRepository`.
- **Session Reuse:** Inspects `auth.currentUser`. If an active session is already present on device, reuses it immediately without issuing redundant network calls.
- **Anonymous Sign-In:** Calls `auth.signInAnonymously().await()` when no user exists.
- **Account Linking:** Implements `linkWithGoogle` using `currentUser.linkWithCredential(authCredential)` so anonymous users who later choose to sign in with Google retain their existing UID and game progress.
- **Controlled Reset:** Implements `resetAuth()` clearing `CredentialManager` and signing out of Firebase Auth.

### 4.3 Firebase Emulator Configuration (`FirebaseEmulatorConfig.kt`)
Created `app/src/main/java/com/example/util/FirebaseEmulatorConfig.kt`:
- Resolves host networking difference between Android Emulator (`10.0.2.2`) and host/adb (`127.0.0.1`).
- Configures `Firebase.auth.useEmulator(host, 9099)`.
- Configures named database `FirebaseFirestore.getInstance(databaseId).useEmulator(host, 8085)`.

### 4.4 Lifecycle & UI Integration
- **`MainActivity.kt`:** Removed the blocking check `if (currentUser == null) GoogleSignInScreen(...)`. The app renders `TheAlibiApp(viewModel)` directly. In `onCreate`, `authRepo.signInAnonymously()` runs in background.
- **`GoogleSignInScreen.kt`:** Repurposed into an optional account management screen showing the user's current UID, offering Google account linking, guest continuation, and dev reset.

### 4.5 Repository Seam Integration
- **`TheAlibiRepository` (Offline Demo):** Remains completely functional without requiring online authentication. Supports local `setAuthIdentity`.
- **`UnavailableFirebaseGameRepository` (Online):** Obtains the live authenticated `AuthIdentity` with real Firebase UID, but continues to fail closed for multiplayer room operations with an explicit `ConnectionState.Unavailable` state.

---

## 5. Security & Firestore Rules Audit

Inspected `firestore.rules`:
- Default deny-all rule: `match /{document=**} { allow read, write: if false; }`.
- Authentication gate: `function isSignedIn() { return request.auth != null; }`.
- Zero open access rules (`allow read, write: if true` does not exist).
- No service-account keys, API secrets, or private credentials are embedded in client code.

---

## 6. Test Suite & Verification Results

All unit test suites were executed with Gradle (`:app:testDebugUnitTest`) and passed cleanly:

```
> Task :app:testDebugUnitTest

ExampleUnitTest > addition_isCorrect PASSED
FirebaseAuthRepositoryTest > linking Google account preserves original UID and updates identity PASSED
FirebaseAuthRepositoryTest > subsequent anonymous sign-in reuses existing session without generating new UID PASSED
FirebaseAuthRepositoryTest > resetAuth signs out and transitions to Unauthenticated state PASSED
FirebaseAuthRepositoryTest > initial state is unauthenticated or initial PASSED
FirebaseAuthRepositoryTest > online repository obtains live authenticated UID while failing closed on multiplayer operations PASSED
FirebaseAuthRepositoryTest > network failure during anonymous sign-in transitions to Error state gracefully PASSED
FirebaseAuthRepositoryTest > anonymous sign-in creates anonymous account with stable UID PASSED
LocalRepositoryTest > room exposes correct player list and host designation PASSED
LocalRepositoryTest > player can change readiness state PASSED
LocalRepositoryTest > observers receive room and player updates PASSED
LocalRepositoryTest > local repository exposes valid local auth identity and disconnected state PASSED
LocalRepositoryTest > second player can join a room through suspend abstraction PASSED
LocalRepositoryTest > leaving room resets room state and clears session PASSED
LocalRepositoryTest > player can create a room through suspend abstraction PASSED
RepositorySelectionTest > online mode does not silently select local repository implementation PASSED
RepositorySelectionTest > offline demo mode explicitly selects local repository implementation PASSED
RepositorySelectionTest > unavailable online implementation produces explicit error and unavailable connection state PASSED
TheAlibiGameTest > join room adds player and updates lobby count PASSED
TheAlibiGameTest > exact player count mystery generation for 5 players freezes roster and assigns characters PASSED
TheAlibiGameTest > mid-game twist reveals new clue and triggers event PASSED
TheAlibiGameTest > investigation board pins connection threads between suspects and clues PASSED
TheAlibiGameTest > case library can be populated via case generation PASSED
TheAlibiGameTest > repository starts with empty case library on fresh instance PASSED
TheAlibiGameTest > interrogation records testimony and detects contradictions PASSED
TheAlibiGameTest > player assignment and shoulder-surfing conceal toggle work PASSED
TheAlibiGameTest > generation rejected when fewer than 4 players PASSED
TheAlibiGameTest > case generation produces new playable mystery PASSED
TheAlibiGameTest > hotspot inspection collects associated forensic evidence PASSED
TheAlibiGameTest > full investigation playability cycle from briefing to final conviction PASSED
TheAlibiGameTest > deal proposal and acceptance flow PASSED
TheAlibiGameTest > five manor locations are available and can be explored PASSED
TheAlibiGameTest > private messaging and evidence attachment updates sharing visibility PASSED
TheAlibiGameTest > room creation generates 6-digit code and marks host PASSED
TheAlibiGameTest > sound manager independent toggles PASSED
TheAlibiGameTest > secret ballot voting and full case resolution scoring PASSED
TheAlibiViewModelTest > two repository instances have isolated stores by default PASSED
TheAlibiViewModelTest > existing gameplay behavior remains intact PASSED
TheAlibiViewModelTest > repository failures are handled and surface to toast message PASSED
TheAlibiViewModelTest > room joining works through ViewModel with shared local store PASSED
TheAlibiViewModelTest > viewModel can be constructed using repository abstraction and factory PASSED
TheAlibiViewModelTest > readiness changes work through ViewModel PASSED
TheAlibiViewModelTest > room creation works through ViewModel and updates UI state PASSED

BUILD SUCCESSFUL: 43 tests completed, 0 failed, 0 skipped.
```

---

## 7. File Modifications Summary

| File | Change Type | Description |
|---|---|---|
| `app/src/main/java/com/example/data/firebase/AuthRepository.kt` | **Created** | Domain interface for authentication lifecycle. |
| `app/src/main/java/com/example/data/firebase/FirebaseAuthRepository.kt` | **Updated** | Implemented anonymous authentication, session reuse, account linking, reset. |
| `app/src/main/java/com/example/util/FirebaseEmulatorConfig.kt` | **Created** | Helper for connecting to local Auth and named Firestore emulators. |
| `app/src/main/java/com/example/model/AuthIdentity.kt` | **Updated** | Added `AuthState` sealed class. |
| `app/src/main/java/com/example/MainActivity.kt` | **Updated** | Removed blocking Google sign-in gate; auto-sign in anonymously in background. |
| `app/src/main/java/com/example/ui/screens/GoogleSignInScreen.kt` | **Updated** | Repurposed into optional account linking and guest continuation screen. |
| `app/src/main/java/com/example/data/UnavailableFirebaseGameRepository.kt` | **Updated** | Added `setAuthIdentity` to receive live Firebase UID while failing closed. |
| `app/src/main/java/com/example/data/TheAlibiRepository.kt` | **Repaired** | Fixed corrupted methods, removed non-suspend store calls, cleaned model classes. |
| `app/src/test/java/com/example/FirebaseAuthRepositoryTest.kt` | **Created** | 7 unit tests verifying anonymous auth, session reuse, linking, and reset. |
| `app/src/test/java/com/example/LocalRepositoryTest.kt` | **Updated** | Injected shared store for multi-player join test. |
| `app/src/test/java/com/example/TheAlibiViewModelTest.kt` | **Updated** | Fixed shared store instance for view model join test. |
| `app/src/test/java/com/example/TheAlibiGameTest.kt` | **Updated** | Adapted tests to dynamic case generation without hardcoded case001. |
| `FIREBASE_EMULATOR_SETUP.md` | **Created** | Root documentation for Firebase Emulator suite setup and networking. |
| `FIREBASE_AUTHENTICATION_REPORT.md` | **Created** | Root report on anonymous authentication architecture and verification. |
| `PHASE_1A_IMPLEMENTATION_REPORT.md` | **Created** | Comprehensive implementation and verification report for Phase 1A. |

---

## 8. Conclusion & Readiness

Phase 1A is complete. The architectural seam is preserved, the application runs seamlessly with Firebase Anonymous Authentication without Google Sign-In requirements, the Spark free plan is strictly adhered to, and all 43 unit tests pass.

The repository is now ready for **Phase 1B: Firestore Room Creation & Code Generation**.

