# THE ALIBI — DEVELOPMENT & MILESTONE STATUS

## Completed Milestones
1. **Core Architecture & Identity**:
   - Platform metadata, app name ("The Alibi"), and unique package configuration synced.
   - Noir visual design system implemented (#101114 background, #C69B55 gold accents, #8D2929 warning red).
   - Custom adaptive launcher icon created with gold emblem on #101114 noir background.

2. **Room-Based Dynamic Mystery Generation (Post-Lobby Pipeline)**:
   - **Lobby Roster Quorum**: Rooms created with unique 6-digit numeric codes. Minimum 4 players required, maximum 10 supported.
   - **Exact Player Count Generation**: Mystery case generation is initiated *after* players join the lobby. The frozen human player count $N$ directly dictates the case schema: exactly $N$ human characters, 1 human culprit, $N-1$ innocent characters, and guaranteed deduction paths.
   - **Roster Freeze & Zero Late Joins**: When generation begins, the player roster is frozen; subsequent join attempts return clear rejection errors (`"Case generation has started. This room is no longer accepting players."`).
   - **Authoritative Backend State Machine**: States include `WAITING_FOR_PLAYERS`, `GENERATING_CASE`, `VALIDATING_CASE`, `GENERATING_ASSETS`, `ASSIGNING_CHARACTERS`, `CASE_BRIEFING`, `CHARACTER_REVEAL`, `INVESTIGATION`, `VOTING`, `CASE_RESOLUTION`.
   - **Private Character Assignment**: Each investigator securely receives only their assigned character dossier (with secret culprit directives if assigned the murderer role) while keeping other dossiers and the secret solution sealed.
   - **Single-Device Test Mode**: In-app "Add Investigator" and "Remove" controls allow rapid local verification of 4 to 10 players on an emulator or test device.

3. **Ktor Backend (`backend/server`)**:
   - Structured JSON generation engine with Gemini 2.5 Flash supporting 4 to 10 player mysteries with rich character pools.
   - Image generation pipeline with Imagen 3 / Gemini vision models.
   - Deterministic `CaseValidator` in Kotlin verifying unique IDs, single human culprit, exact player count matching, referential integrity, and 3+ deduction paths.
   - Room management routes (`POST /rooms`, `POST /rooms/{code}/join`, `GET /rooms/{code}`, `POST /rooms/{code}/generate`, `GET /rooms/{code}/my-character`, `POST /rooms/{code}/transition`, `POST /rooms/{code}/leave`).
   - Comprehensive unit testing in `backend/server/src/test/kotlin/com/thealibi/server/RoomGenerationTest.kt`.

4. **Android Client Gameplay & UI**:
   - **Lobby Screen**: 6-digit room code with copy action, live player roster, host badge, connection status, player count progress ($N/10$), confirmation dialog, and host-only generation triggers.
   - **Create & Join Screens**: Dedicated host configuration screen and guest 6-digit code entry screen.
   - **Case Hub**: Clear navigation for multiplayer room creation, joining, standalone case generation, and official case archives.
   - **Case Opening & Briefing**: Cinematic case briefing, victim details, and investigation objective.
   - **Private Character Dossier**: Role identity, public biography, private secret, alibi, and shoulder-surfing concealment shield.
   - **Interactive Crime Scene**: Clickable hotspots, forensic inspection sheet, and evidence collection.
   - **Suspect Gallery & Interrogation**: Suspect suspicion flags, categorized inquiries (Motive, Opportunity, Alibi, Evidence Confrontation), authored responses, and contradiction detection.
   - **Investigation Board**: Visual detective board, yarn connection threads between suspects and clues, and private observation notebook.
   - **Timeline Reconstruction**: Chronological event logs with timestamp tracking.
   - **Accusation Courtroom & Resolution**: Culprit accusation, motive selection, smoking gun presentation, and dramatic case resolution revealing the true culprit and hidden timeline.

5. **Testing & Verification**:
   - Backend `RoomGenerationTest` covering all 16 room generation requirements.
   - Android JVM Robolectric tests in `TheAlibiGameTest` covering room creation, joining, player count enforcement, host-only generation, roster freezing, character assignment, and the full investigation lifecycle.
   - 100% build compilation verified.
