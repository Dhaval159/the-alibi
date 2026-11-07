# THE ALIBI — Firebase Local Emulator Suite Guide (Phase 1A)

This document provides instructions for configuring and connecting to the Firebase Local Emulator Suite for development and testing of **THE ALIBI**.

---

## 1. Firebase Spark (Zero Cost) Adherence

> [!IMPORTANT]
> **THE ALIBI strictly adheres to the Firebase Spark (free tier) plan.**
> - Zero paid infrastructure.
> - No Firebase Blaze upgrade.
> - No billing account required.
> - Cloud Functions, Cloud Run, and paid hosting/databases are strictly prohibited.
> - The Firebase Local Emulator Suite runs 100% locally on the developer machine at zero cost.

---

## 2. Configuration & Ports

### Service & Port Specifications

| Service | Host Port | Android Emulator Host | Physical Device Host |
|---|---|---|---|
| **Firebase Authentication** | `9099` | `10.0.2.2:9099` | `127.0.0.1:9099` (via `adb reverse`) |
| **Cloud Firestore** | `8085` | `10.0.2.2:8085` | `127.0.0.1:8085` (via `adb reverse`) |
| **Emulator UI** | Disabled | N/A | N/A |

### Named Firestore Database ID
The project targets the specific named database:
- **Database ID:** `ai-studio-android-remixthe-b1d35dd2-d6f5-4ff6-8c75-8d89468bf295`
- **Project ID:** `gen-lang-client-0571820623`

---

## 3. Configuration Files

### `firebase.json`
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
```json
{
  "projects": {
    "default": "gen-lang-client-0571820623"
  }
}
```

---

## 4. Host Machine vs. Android Emulator Networking

> [!WARNING]
> Networking differs fundamentally between the host machine and the Android Emulator:
> - **Host Machine Loopback:** `127.0.0.1` refers to the machine itself.
> - **Android Emulator Loopback:** Inside the Android Emulator, `127.0.0.1` refers to the Android virtual device itself, NOT your host machine running the emulators.
> - **Host Loopback Alias:** Inside the Android Emulator, use `10.0.2.2` to communicate with services listening on the host machine.
> - **Physical Android Devices:** Connect device via USB and run port forwarding:
>   ```bash
>   adb reverse tcp:9099 tcp:9099
>   adb reverse tcp:8085 tcp:8085
>   ```
>   Once forwarded, `127.0.0.1` on the physical device reaches the host emulators.

---

## 5. Android Client Emulator Helper

In the Android application, emulator connection is encapsulated in:
`app/src/main/java/com/example/util/FirebaseEmulatorConfig.kt`

```kotlin
// Automatically detects environment (10.0.2.2 for emulator, 127.0.0.1 otherwise)
FirebaseEmulatorConfig.configureEmulators(context)
```

Implementation details:
```kotlin
val host = if (isRunningOnAndroidEmulator()) "10.0.2.2" else "127.0.0.1"
Firebase.auth.useEmulator(host, 9099)
val databaseId = context.getString(R.string.firestore_database_id)
FirebaseFirestore.getInstance(databaseId).useEmulator(host, 8085)
```

---

## 6. How to Start the Emulator Suite

### Prerequisites
1. **Node.js** (v20+ LTS; v24.18.0 detected).
2. **Java JDK** (Java 11+; OpenJDK 25 detected in Android Studio `jbr`).
3. **Firebase CLI**:
   ```bash
   npm install -g firebase-tools
   ```
   Or run via npx:
   ```bash
   npx firebase-tools emulators:start --only auth,firestore
   ```

### Startup Commands
Start Auth and Firestore emulators without persistence:
```bash
firebase emulators:start --only auth,firestore
```
Start with test fixture import/export:
```bash
firebase emulators:start --only auth,firestore --import=./emulator-data --export-on-exit=./emulator-data
```
Verify port listeners:
```powershell
Get-NetTCPConnection -LocalPort 9099, 8085 -ErrorAction SilentlyContinue
```

