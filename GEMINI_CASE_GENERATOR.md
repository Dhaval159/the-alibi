# THE ALIBI — GEMINI LIVE CASE GENERATOR ARCHITECTURE

## 1. System Overview
The Live Case Generator in **THE ALIBI** uses Google Gemini to architect complete, playable, airtight detective mystery cases. Each generated case is an interactive investigation package featuring:
- Case Identity & Cinematic Briefing
- Detailed Victim Profile
- Dynamic Character Roster (8–10 suspects, with exactly 1 human culprit)
- Deep Forensic Evidence System (10–14 clues, with authentic crime scene hotspots)
- Interrogation Dialogue Bank with conditional testimony and contradiction triggers
- Investigation Board with connection threads and detective notes
- Hidden Master Timeline & Secret Solution (securely protected server-side until case resolution)
- Adaptive Player Count configurations (supporting 4 to 10 players)

---

## 2. Server-Client Boundary & Data Privacy
- **Client App (Android)**:
  - Jetpack Compose + Material 3 interface adhering to dark noir aesthetics (#101114 background, #C69B55 gold accents, #8D2929 warning red).
  - Displays public dossiers, interactive crime scene inspection, evidence gallery, suspect gallery, interrogation transcript, investigation board, and accusation courtroom.
  - Never receives secret solution data, culprit identity, or private character directives before the final verdict.
  - Features an anti-shoulder-surfing privacy toggle to conceal the player's secret role during local pass-and-play sessions.
- **Backend Server (Ktor)**:
  - Resides in `backend/server`.
  - Executes Gemini text and image generation calls using the `GEMINI_API_KEY` server environment variable.
  - Executes deterministic Kotlin validation checks on generated mysteries.
  - Stores secret solutions and serves sanitized public case packages.
  - Hosts REST endpoints for asynchronous generation jobs, case retrieval, asset streaming, and final verdict verification.

---

## 3. Asynchronous Generation Flow
1. User configures mystery settings (Theme, Difficulty, Player Count 4–10, Atmosphere, Custom Directives) in `CaseGeneratorScreen`.
2. Client sends `POST /cases/generate`, receiving an asynchronous `jobId`.
3. Client polls `GET /cases/generate/{jobId}`, displaying real-time generation stages in `CaseGenerationProgressScreen`:
   - Stage 1: Designing the mystery setting and victim profile
   - Stage 2: Consulting Gemini mystery architect for structured case data
   - Stage 3: Parsing and structuring mystery evidence, timeline, and suspects
   - Stage 4: Deterministic schema and deduction logic validation (with automatic repair passes)
   - Stage 5: Generating visual assets and suspect portraits
   - Stage 6: Finalizing case package for investigation
4. Upon completion, client previews the case in `GeneratedCasePreviewScreen` and launches gameplay.
