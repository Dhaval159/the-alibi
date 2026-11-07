# THE ALIBI — VISUAL ASSET GENERATION PIPELINE

## 1. Asset Pipeline Structure
Visual assets are generated in a structured sequence to ensure artistic cohesion and aesthetic quality:
1. **Case Narrative First**: Case setting, suspects, and forensic clues are fully architected.
2. **Cohesive Visual Style Guide**:
   ```
   "Cinematic noir detective thriller, dramatic low-key lighting, 1920s gothic atmosphere, oil painting richness, deep shadows, gold and charcoal palette"
   ```
3. **Case Poster**: 3:4 or 16:9 cinematic poster saved to `generated_cases/{caseId}/poster/poster.jpg`.
4. **Suspect Portraits**: Headshots generated using the shared case style guide to ensure matching lighting, angle, and framing across all suspects in `generated_cases/{caseId}/characters/{characterId}.jpg`.
5. **Crime Scene Illustration**: 16:9 wide interior view of the incident room in `generated_cases/{caseId}/locations/crime_scene.jpg`.
6. **Key Evidence Photography**: Macro forensic still photographs of physical clues in `generated_cases/{caseId}/evidence/{evidenceId}.jpg`.

## 2. Directory Structure
```
generated_cases/
└── case_blackwood_001/
    ├── poster/
    │   └── poster.jpg
    ├── characters/
    │   ├── char_edward_vance.jpg
    │   ├── char_beatrice_thorne.jpg
    │   └── ...
    ├── locations/
    │   └── crime_scene.jpg
    └── evidence/
        ├── ev_poisoned_decanter.jpg
        ├── ev_torn_will_draft.jpg
        └── ...
```

## 3. Fallback & Resilience
- If Gemini image generation encounters a network timeout or quota limit, the pipeline falls back gracefully to pre-bundled high-definition noir assets without blocking mystery gameplay.
- Android client caches images in memory and local storage via Coil.
