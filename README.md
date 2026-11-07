# THE ALIBI 🔍
*A Multiplayer Social Deduction & Detective Investigation Game with Live Gemini Mystery Generation*

---

## Overview
**THE ALIBI** puts players into the heart of a 1920s gothic noir murder mystery. One human player is always secretly the culprit; other players must investigate the crime scene, interrogate suspects, inspect physical clues, reconstruct the timeline, connect deductive threads, and deliver a formal accusation before the killer escapes.

---

## Features
- **Live Case Generator**: Generate endless unique mysteries powered by Google Gemini with custom themes, difficulties, atmospheres, and player counts (4–10 players).
- **Interactive Crime Scene**: Explore the incident room with interactive hotspots to inspect objects and collect forensic clues into your case file.
- **Interrogation Room**: Confront suspects across 7 inquiry categories with authored conditional dialogue and real-time contradiction detection.
- **Detective Pinboard**: Pin red yarn deduction threads between suspects and evidence on an authentic visual corkboard.
- **Anti-Shoulder-Surfing Shield**: Conceal your secret character directives during local pass-and-play sessions.
- **Airtight Deductive Logic**: Every case is verified by a deterministic Kotlin validator guaranteeing at least 3 distinct deduction paths and zero secret solution leakage.

---

## Project Structure
```
.
├── app/                  # Android application (Kotlin + Jetpack Compose + Material 3)
│   ├── src/main/java/com/example/
│   │   ├── model/        # Case, suspect, evidence, and board data models
│   │   ├── data/         # Repository, state store, and CASE 001 engine
│   │   ├── ui/           # ViewModel, themes, and screen composables
│   │   └── MainActivity.kt
│   └── src/test/         # Robolectric JVM test suite
├── backend/server/       # Ktor backend server
│   ├── src/main/kotlin/com/thealibi/server/
│   │   ├── gemini/       # Gemini structured text and image generation
│   │   ├── validation/   # Deterministic case validation engine
│   │   ├── storage/      # Case storage and asset management
│   │   └── routes/       # Ktor REST API endpoints
│   └── src/test/         # Backend schema & validator tests
└── documentation/        # Full technical specifications
```

---

## Running the Project
- **Android App**: Build and run via Android Studio or AI Studio streaming emulator (`gradle assembleDebug`).
- **Ktor Server**: Set `GEMINI_API_KEY` and run `gradle run` in `backend/server`.
