# THE ALIBI — Firebase Emulator Suite Foundation (Phase 0)

This document establishes the architecture, configuration, and verification foundation for running the Firebase Local Emulator Suite with **THE ALIBI**.

---

## 1. Named Firestore Database Requirement

> [!IMPORTANT]
> The Firebase project for THE ALIBI uses a **named Firestore database**:
> 
> **Database ID:** `ai-studio-android-remixthe-b1d35dd2-d6f5-4ff6-8c75-8d89468bf295`
> 
> **CRITICAL RULE:**
> All future Cloud Functions Admin SDK operations, Firestore triggers, security rules, and Android client SDK calls must explicitly target this named database.
> Do NOT assume `getFirestore()` or `FirebaseFirestore.getInstance()` defaults to the correct database.

### Android Client Specification
```kotlin
// Android client connection to the named database:
val databaseId = context.getString(R.string.firestore_database_id)
// databaseId is "ai-studio-android-remixthe-b1d35dd2-d6f5-4ff6-8c75-8d89468bf295"
val firestore = FirebaseFirestore.getInstance(databaseId)
```

### Backend / Admin SDK Specification (Future Phases)
```typescript
// Cloud Functions Admin SDK targeting named database:
import { initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";

const app = initializeApp();
const db = getFirestore(app, "ai-studio-android-remixthe-b1d35dd2-d6f5-4ff6-8c75-8d89468bf295");
```

---

## 2. Configuration Files

### `firebase.json`
Specifies the named database rules mapping and emulator ports:
```json
{
  "firestore": [
    {
      "database": "ai-studio-android-remixthe-b1d35dd2-d6f5-4ff6-8c75-8d89468bf295",
      "rules": "firestore.rules"
    }
  ],
  "emulators": {
    "firestore": {
      "host": "127.0.0.1",
      "port": 8085
    },
    "auth": {
      "host": "127.0.0.1",
      "port": 9099
    },
    "ui": {
      "enabled": false
    }
  }
}
```

### `.firebaserc`
Configures the default project ID:
```json
{
  "projects": {
    "default": "gen-lang-client-0571820623"
  }
}
```

### `package.json`
Provides convenience scripts for launching emulators and running security rules tests:
- `npm run emulators:start`: Starts Firestore (port 8085) and Auth (port 9099) emulators.
- `npm run test:rules`: Executes `firestore.test.js` against the running emulator.

---

## 3. How to Start the Emulator Suite

### Prerequisites
1. **Node.js** (v20+ LTS recommended; v24.18.0 detected in development environment).
2. **Java JRE/JDK** (Java 11+; OpenJDK 25 detected in Android Studio `jbr`).
3. **Firebase CLI** (`firebase-tools`):
   ```bash
   npm install -g firebase-tools
   ```

### Execution Commands
Start the emulators with persistence disabled:
```bash
firebase emulators:start --only firestore,auth
```
Or with export/import of test fixtures:
```bash
firebase emulators:start --only firestore,auth --import=./emulator-data --export-on-exit=./emulator-data
```

---

## 4. How Future Android Clients Connect to Emulators

In future phases (Phase 2 & 3), Android clients will connect to the local emulators in debug builds via an explicit debug configuration flag (e.g., `BuildConfig.DEBUG` or an environment toggle).

> [!WARNING]
> Production builds must NEVER connect to emulators by default.

```kotlin
fun configureFirebaseEmulators(context: Context, useEmulators: Boolean) {
    if (!useEmulators) return

    val emulatorHost = "10.0.2.2" // Android emulator loopback alias to host machine 127.0.0.1

    // 1. Auth Emulator (Port 9099)
    Firebase.auth.useEmulator(emulatorHost, 9099)

    // 2. Named Firestore Database Emulator (Port 8085)
    val dbId = context.getString(R.string.firestore_database_id)
    val firestore = FirebaseFirestore.getInstance(dbId)
    firestore.useEmulator(emulatorHost, 8085)
    firestore.firestoreSettings = firestoreSettings {
        isPersistenceEnabled = false
    }
}
```

---

## 5. Environment & Execution Status in Phase 0

| Component | Status | Details |
|---|---|---|
| `firebase.json` | CONFIGURED | Firestore (8085) & Auth (9099) emulators defined with named database |
| `.firebaserc` | CONFIGURED | Project `gen-lang-client-0571820623` configured |
| `package.json` | CONFIGURED | Scripts `emulators:start` and `test:rules` added |
| Node.js | INSTALLED | v24.18.0 |
| Java JRE | INSTALLED | OpenJDK 25 (Android Studio JBR) |
| Firebase CLI (`firebase-tools`) | NOT INSTALLED | `firebase` command is not available in system PATH |
| Emulator Execution | NOT RUN | Requires global or local `firebase-tools` installation |

