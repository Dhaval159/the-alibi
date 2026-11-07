# THE ALIBI — CASE VALIDATION & DEDUCTION COVERAGE

## 1. Validation Architecture
A compelling mystery requires airtight deductive logic. `CaseValidator.kt` performs strict, deterministic verification on all generated cases before they become playable:

### Rule 1: Identity & Integrity
- Case ID and title must not be blank.
- Character IDs must be strictly unique.
- Evidence IDs must be strictly unique.
- Minimum of 4 characters and 6 evidence items required.

### Rule 2: Single Culprit & No NPC Culprit
- Exactly one character is designated as the culprit in `secretSolution`.
- The culprit must be present in `characters`.
- In every adaptive player count configuration (4 through 10 human players), the culprit must ALWAYS be assigned to a human player and NEVER designated as an NPC.

### Rule 3: Referential Consistency
- Every character ID referenced in `evidence.linkedCharacterIds` must exist.
- Every character ID referenced in `timeline.characterIds` must exist.
- Every evidence ID referenced in `scene.hotspots.associatedEvidenceId` must exist.
- Every evidence ID referenced in `solution.smokingGunEvidenceIds` must exist.

### Rule 4: Deduction Coverage (Triple Path Rule)
- A mystery cannot depend on a single giveaway clue.
- At least **3 independent deduction paths** (`deductionPaths`) must lead to the culprit's conviction:
  1. **Motive Path**: Independent evidence proving extreme urgency or personal incentive.
  2. **Opportunity Path**: Evidence shattering the culprit's alibi and placing them at the crime scene.
  3. **Means / Execution Path**: Physical trace, poison origin, or mechanical key linking the culprit to the murder method.

### Rule 5: Zero-Leakage Sanitization
- `validator.createPublicCase(casePackage)` strips all `secretSolution` fields and all other characters' private secrets prior to transmission across public client endpoints.
