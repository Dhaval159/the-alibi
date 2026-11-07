# THE ALIBI — CASE GENERATION DATA SCHEMA

## 1. CasePackage Schema
```json
{
  "identity": {
    "caseId": "case_blackwood_001",
    "title": "The Cipher at Blackwood Hall",
    "subtitle": "A deadly poison, an altered will, and six suspects behind locked doors.",
    "classification": "CASE 001 - CLASSIFIED DOSSIER",
    "setting": "Blackwood Hall, Northumberland, England",
    "dateTime": "October 24, 1928, 23:45 GMT",
    "difficulty": "Medium",
    "estimatedDurationMinutes": "7-10 min",
    "cinematicIntro": "...",
    "incidentSummary": "...",
    "investigationObjective": "...",
    "posterAssetId": "poster.jpg"
  },
  "victim": {
    "name": "Lord Reginald Blackwood",
    "age": 62,
    "occupation": "Patriarch & Industrialist",
    "background": "...",
    "causeOfDeath": "Poisoning by rare belladonna alkaloid in vintage scotch",
    "locationFound": "Private library study, seated at mahogany desk"
  },
  "characters": [
    {
      "id": "char_edward_vance",
      "name": "Edward Vance",
      "age": 38,
      "occupation": "Estranged Nephew & Antiquities Dealer",
      "publicBio": "...",
      "personality": "Charming, nervous when pressed",
      "publicMotive": "Stands to inherit under original will, but was being disinherited",
      "relationshipWithVictim": "Nephew and debtor",
      "privateSecret": "...",
      "personalAlibi": "Claims he was in south conservatory",
      "claimedLocation": "South Conservatory",
      "actualActions": "...",
      "personalObjective": "...",
      "interrogationResponses": {
        "Motive": [...],
        "Opportunity": [...],
        "Evidence confrontation": [...]
      }
    }
  ],
  "evidence": [
    {
      "id": "ev_poisoned_decanter",
      "name": "Cut-Crystal Scotch Decanter",
      "category": "WeaponTrace",
      "discoveryLocation": "Study writing desk",
      "discoveryPhase": "Initial",
      "description": "...",
      "detailedInspection": "...",
      "linkedCharacterIds": ["char_edward_vance", "char_dr_julian_finch"],
      "whatItProves": "...",
      "isMisleading": false,
      "timelineRelationship": "...",
      "assetId": "ev_poisoned_decanter.jpg",
      "relevanceToSolution": "Direct murder weapon delivery method"
    }
  ],
  "crimeScene": {
    "roomName": "Lord Blackwood's Private Library Study",
    "description": "...",
    "sceneAssetId": "crime_scene.jpg",
    "hotspots": [
      {
        "id": "hs_desk",
        "title": "Mahogany Writing Desk",
        "normalizedX": 0.35,
        "normalizedY": 0.65,
        "radius": 0.10,
        "inspectableDescription": "...",
        "associatedEvidenceId": "ev_poisoned_decanter"
      }
    ]
  },
  "timeline": [
    {
      "id": "tl_1",
      "timestamp": "19:00",
      "title": "Will Rewritten",
      "description": "...",
      "characterIds": ["char_edward_vance"],
      "isHiddenSequence": false,
      "isDiscoveredByDefault": true
    }
  ],
  "deductionPaths": [
    {
      "id": "dp_opportunity",
      "title": "The Broken Alibi & Terrace Route",
      "requiredEvidenceIds": ["ev_muddy_footprints", "ev_secret_passage_latch"],
      "contradictionDescription": "...",
      "conclusion": "..."
    }
  ],
  "playerCountConfigs": {
    "4": {
      "humanPlayerCount": 4,
      "assignedHumanCharacterIds": ["char_edward_vance", "..."],
      "npcCharacterIds": ["..."],
      "culpritCharacterId": "char_edward_vance"
    }
  },
  "secretSolution": {
    "culpritId": "char_edward_vance",
    "motive": "...",
    "opportunity": "...",
    "method": "...",
    "hiddenTimeline": [...],
    "misleadingStrategy": "...",
    "smokingGunEvidenceIds": ["ev_silver_cigarette_case", "ev_muddy_footprints"],
    "contradictionExplanations": {...},
    "misleadingEvidenceExplanations": {...},
    "finalVerdictExplanation": "..."
  }
}
```
