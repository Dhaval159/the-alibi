package com.thealibi.server.models

import kotlinx.serialization.Serializable

@Serializable
data class CaseGenerationRequest(
    val theme: String = "Mansion inheritance mystery",
    val difficulty: String = "Medium",
    val playerCount: Int = 6,
    val atmosphere: String = "Classic detective mystery",
    val customPrompt: String = ""
)

@Serializable
data class GenerationJobStatus(
    val jobId: String,
    val status: String, // PENDING, GENERATING_CASE, VALIDATING, GENERATING_ASSETS, COMPLETED, FAILED
    val progressPercent: Int,
    val currentStage: String,
    val caseId: String? = null,
    val errorMessage: String? = null
)

@Serializable
data class CasePackage(
    val identity: CaseIdentity,
    val victim: VictimProfile,
    val characters: List<CharacterProfile>,
    val evidence: List<EvidenceItem>,
    val timeline: List<TimelineEvent>,
    val crimeScene: CrimeSceneData,
    val locations: List<InvestigationLocation> = emptyList(),
    val midGameTwist: MidGameTwist? = null,
    val deductionPaths: List<DeductionPath>,
    val playerCountConfigs: Map<Int, PlayerCountAssignment>,
    // Server-only secret solution - kept on backend until CASE_RESOLUTION
    val secretSolution: SecretSolution? = null
)

@Serializable
data class CaseIdentity(
    val caseId: String,
    val title: String,
    val subtitle: String,
    val classification: String,
    val setting: String,
    val dateTime: String,
    val difficulty: String,
    val estimatedDurationMinutes: String = "7-10 minutes",
    val cinematicIntro: String,
    val incidentSummary: String,
    val investigationObjective: String,
    val posterAssetId: String? = null
)

@Serializable
data class VictimProfile(
    val name: String,
    val age: Int,
    val occupation: String,
    val background: String,
    val causeOfDeath: String,
    val locationFound: String
)

@Serializable
data class CharacterProfile(
    val id: String,
    val name: String,
    val age: Int,
    val occupation: String,
    val publicBio: String,
    val personality: String,
    val publicMotive: String,
    val relationshipWithVictim: String,
    val portraitAssetId: String? = null,
    val portraitPrompt: String = "",
    // Private character fields (only delivered to the assigned player)
    val privateSecret: String = "",
    val personalAlibi: String = "",
    val claimedLocation: String = "",
    val actualActions: String = "",
    val knownInformation: String = "",
    val hiddenInformation: String = "",
    val possibleContradictions: List<String> = emptyList(),
    val personalObjective: String = "",
    val interrogationResponses: Map<String, List<InterrogationResponse>> = emptyMap()
)

@Serializable
data class InterrogationResponse(
    val question: String,
    val answer: String,
    val requiredEvidenceToTrigger: String? = null,
    val revealsContradiction: Boolean = false,
    val followUpQuestions: List<String> = emptyList()
)

@Serializable
data class EvidenceItem(
    val id: String,
    val name: String,
    val category: String, // Document, Forensic, PersonalItem, WeaponTrace, Environmental, Communication
    val discoveryLocation: String,
    val discoveryPhase: String, // Initial, SceneInspection, InterrogationReward, BoardDeduction
    val description: String,
    val detailedInspection: String,
    val linkedCharacterIds: List<String> = emptyList(),
    val whatItProves: String,
    val isMisleading: Boolean = false,
    val timelineRelationship: String,
    val assetId: String? = null,
    val imagePrompt: String = "",
    val relevanceToSolution: String
)

@Serializable
data class CrimeSceneData(
    val sceneAssetId: String? = null,
    val scenePrompt: String = "",
    val roomName: String,
    val description: String,
    val hotspots: List<SceneHotspot> = emptyList()
)

@Serializable
data class InvestigationLocation(
    val id: String,
    val name: String,
    val description: String,
    val atmosphereSnippet: String,
    val environmentAssetId: String? = null,
    val isPrimaryCrimeScene: Boolean = false,
    val hotspots: List<SceneHotspot> = emptyList()
)

@Serializable
data class MidGameTwist(
    val id: String,
    val title: String,
    val eventNarration: String,
    val triggerHalfwayMinute: Int = 3,
    val revealedEvidence: EvidenceItem,
    val visualAssetId: String? = null,
    val impactDescription: String
)

@Serializable
data class SceneHotspot(
    val id: String,
    val title: String,
    val normalizedX: Float, // 0.0 to 1.0
    val normalizedY: Float, // 0.0 to 1.0
    val radius: Float = 0.08f,
    val inspectableDescription: String,
    val associatedEvidenceId: String? = null
)

@Serializable
data class TimelineEvent(
    val id: String,
    val timestamp: String,
    val title: String,
    val description: String,
    val characterIds: List<String> = emptyList(),
    val isHiddenSequence: Boolean = false,
    val isDiscoveredByDefault: Boolean = true
)

@Serializable
data class DeductionPath(
    val id: String,
    val title: String,
    val requiredEvidenceIds: List<String>,
    val contradictionDescription: String,
    val conclusion: String
)

@Serializable
data class PlayerCountAssignment(
    val humanPlayerCount: Int,
    val assignedHumanCharacterIds: List<String>,
    val npcCharacterIds: List<String>,
    val culpritCharacterId: String
)

@Serializable
data class SecretSolution(
    val culpritId: String,
    val motive: String,
    val opportunity: String,
    val method: String,
    val hiddenTimeline: List<TimelineEvent>,
    val misleadingStrategy: String,
    val smokingGunEvidenceIds: List<String>,
    val contradictionExplanations: Map<String, String>,
    val misleadingEvidenceExplanations: Map<String, String>,
    val finalVerdictExplanation: String
)

@Serializable
data class CasePreview(
    val caseId: String,
    val title: String,
    val subtitle: String,
    val setting: String,
    val difficulty: String,
    val estimatedDuration: String,
    val victimName: String,
    val characterCount: Int,
    val evidenceCount: Int,
    val posterAssetId: String? = null
)
