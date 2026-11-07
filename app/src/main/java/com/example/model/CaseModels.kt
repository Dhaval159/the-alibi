package com.example.model

data class CaseGenerationConfig(
    val theme: String = "Mansion inheritance mystery",
    val difficulty: String = "Medium",
    val playerCount: Int = 6,
    val atmosphere: String = "Classic detective mystery",
    val customPrompt: String = "",
    val matchDurationMinutes: Int = 18
)

data class CaseIdentity(
    val caseId: String,
    val title: String,
    val subtitle: String,
    val classification: String = "CLASSIFIED DOSSIER - LEVEL 4",
    val setting: String,
    val dateTime: String,
    val difficulty: String,
    val estimatedDurationMinutes: String = "15-20 min",
    val cinematicIntro: String,
    val incidentSummary: String,
    val investigationObjective: String,
    val posterAssetId: String? = null
)

data class VictimProfile(
    val name: String,
    val age: Int,
    val occupation: String,
    val background: String,
    val causeOfDeath: String,
    val locationFound: String
)

data class CharacterDossier(
    val id: String,
    val name: String,
    val age: Int,
    val occupation: String,
    val publicBio: String,
    val personality: String,
    val publicMotive: String,
    val relationshipWithVictim: String,
    val portraitAssetId: String? = null,
    val isCulprit: Boolean = false,
    val privateSecret: String = "",
    val personalAlibi: String = "",
    val claimedLocation: String = "",
    val actualActions: String = "",
    val personalObjective: String = "",
    val privateDirectives: String = "",
    val isSuspicious: Boolean = false,
    val isOnline: Boolean = true,
    val interrogationResponses: Map<String, List<InterrogationQnA>> = emptyMap()
)

data class InterrogationQnA(
    val question: String,
    val answer: String,
    val requiredEvidenceToTrigger: String? = null,
    val revealsContradiction: Boolean = false,
    val followUpQuestions: List<String> = emptyList()
)

enum class EvidenceVisibility {
    PUBLIC,
    PRIVATE,
    SHARED
}

data class EvidenceItem(
    val id: String,
    val name: String,
    val category: String, // Document, Forensic, PersonalItem, WeaponTrace, Environmental, Communication
    val discoveryLocation: String,
    val discoveryPhase: String = "Initial",
    val description: String,
    val detailedInspection: String,
    val linkedCharacterIds: List<String> = emptyList(),
    val whatItProves: String,
    val isMisleading: Boolean = false,
    val timelineRelationship: String,
    val assetId: String? = null,
    val isDiscovered: Boolean = false,
    val visibility: EvidenceVisibility = EvidenceVisibility.PUBLIC,
    val discoveredByPlayerId: String? = null,
    val sharedWithPlayerIds: List<String> = emptyList()
)

data class SceneHotspot(
    val id: String,
    val title: String,
    val normalizedX: Float,
    val normalizedY: Float,
    val radius: Float = 0.08f,
    val inspectableDescription: String,
    val associatedEvidenceId: String? = null,
    val isInspected: Boolean = false
)

data class CrimeSceneData(
    val roomName: String,
    val description: String,
    val sceneAssetId: String? = null,
    val hotspots: List<SceneHotspot> = emptyList()
)

data class InvestigationLocation(
    val id: String,
    val name: String,
    val description: String,
    val atmosphereSnippet: String,
    val environmentAssetId: String? = null,
    val isPrimaryCrimeScene: Boolean = false,
    val hotspots: List<SceneHotspot> = emptyList()
)

data class MidGameTwist(
    val id: String,
    val title: String,
    val eventNarration: String,
    val triggerHalfwayMinute: Int = 3,
    val revealedEvidence: EvidenceItem,
    val visualAssetId: String? = null,
    val impactDescription: String
)

enum class ObjectiveCategory {
    SOCIAL,
    INVESTIGATION,
    SECRET_PROTECTION,
    NEGOTIATION,
    INFORMATION_GATHERING,
    MANIPULATION,
    EVIDENCE_RELATED,
    CULPRIT_SPECIAL
}

data class PersonalObjective(
    val id: String,
    val title: String,
    val description: String,
    val category: ObjectiveCategory,
    val isPrimary: Boolean,
    val isVerified: Boolean = false,
    val progress: Int = 0, // 0 to 100
    val targetDescription: String = "",
    val isCompleted: Boolean = false,
    val points: Int = 100
)

data class TimelineEvent(
    val id: String,
    val timestamp: String,
    val title: String,
    val description: String,
    val characterIds: List<String> = emptyList(),
    val isHiddenSequence: Boolean = false,
    val isDiscovered: Boolean = true
)

data class BoardConnection(
    val id: String,
    val suspectId: String,
    val evidenceId: String,
    val label: String
)

data class DeductionNote(
    val id: String,
    val text: String,
    val timestamp: String
)

data class PrivateMessage(
    val id: String,
    val senderPlayerId: String,
    val senderCharacterName: String,
    val recipientPlayerId: String,
    val recipientCharacterName: String,
    val text: String,
    val timestamp: Long,
    val messageType: String = "CHAT", // CHAT, EVIDENCE_SHARE, DEAL_PROPOSAL, DEAL_ACCEPTED, DEAL_REJECTED
    val attachedEvidenceId: String? = null,
    val dealDetails: String? = null
)

data class DealProposal(
    val id: String,
    val proposerPlayerId: String,
    val recipientPlayerId: String,
    val title: String,
    val terms: String,
    val status: String = "PENDING", // PENDING, ACCEPTED, REJECTED
    val timestamp: Long = System.currentTimeMillis()
)

data class BallotVote(
    val voterPlayerId: String,
    val accusedCulpritId: String,
    val methodAccusation: String,
    val motiveAccusation: String,
    val smokingGunEvidenceId: String? = null,
    val submittedAt: Long = System.currentTimeMillis()
)

data class PlayerScoreCard(
    val playerId: String,
    val playerName: String,
    val characterName: String,
    val isCulprit: Boolean,
    val primaryObjectiveCompleted: Boolean,
    val secondaryObjectiveCompleted: Boolean,
    val correctlyVotedCulprit: Boolean,
    val totalScore: Int,
    val earnedAchievements: List<String>
)

data class GameResolutionSummary(
    val trueCulpritId: String,
    val trueCulpritName: String,
    val trueMotive: String,
    val trueMethod: String,
    val trueTimeline: List<TimelineEvent>,
    val smokingGunEvidence: List<EvidenceItem>,
    val misleadingCluesExplanation: String,
    val culpritConfession: String,
    val playerScores: List<PlayerScoreCard>,
    val isCulpritConvicted: Boolean
)

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

data class CasePackage(
    val identity: CaseIdentity,
    val victim: VictimProfile,
    val characters: List<CharacterDossier>,
    val evidence: List<EvidenceItem>,
    val timeline: List<TimelineEvent>,
    val crimeScene: CrimeSceneData,
    val locations: List<InvestigationLocation> = emptyList(),
    val midGameTwist: MidGameTwist? = null,
    val secretSolution: SecretSolution? = null
)

data class AccusationVerdict(
    val isCorrect: Boolean,
    val accusedCharacterName: String,
    val trueCulpritName: String,
    val motiveConfirmed: Boolean,
    val smokingGunConfirmed: Boolean,
    val finalVerdictExplanation: String,
    val solution: SecretSolution
)
