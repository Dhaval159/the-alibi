package com.thealibi.server.models

import kotlinx.serialization.Serializable

@Serializable
enum class RoomState {
    WAITING_FOR_PLAYERS,
    GENERATING_CASE,
    VALIDATING_CASE,
    GENERATING_ASSETS,
    ASSIGNING_CHARACTERS,
    CASE_BRIEFING,          // Phase B: Opening Briefing (2 minutes)
    CHARACTER_REVEAL,       // Phase C: Private Character Dossier
    INVESTIGATION,          // Phase D: Act I Investigation (5-10 minutes)
    MID_GAME_TWIST,         // Phase G: Mid-Game Dramatic Twist
    INTERROGATION_DEBATE,   // Phase H: Act II Group Interrogation & Debate
    VOTING,                 // Phase I: Secret Final Accusation Ballot
    CASE_RESOLUTION         // Phase J: Final Cinematic Reveal & Scoring
}

@Serializable
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

@Serializable
data class PersonalObjective(
    val id: String,
    val title: String,
    val description: String,
    val category: ObjectiveCategory,
    val isPrimary: Boolean,
    val isVerified: Boolean = false,
    val progress: Int = 0,
    val isCompleted: Boolean = false,
    val points: Int = 100
)

@Serializable
data class GameTimerState(
    val phase: RoomState,
    val startTimeMillis: Long,
    val durationSeconds: Int,
    val endTimeMillis: Long,
    val remainingSeconds: Int,
    val isPaused: Boolean = false
)

@Serializable
data class PlayerSession(
    val playerId: String,
    val playerName: String,
    val isHost: Boolean = false,
    val isReady: Boolean = true,
    val isConnected: Boolean = true,
    val sessionToken: String = "",
    val joinedAt: Long = System.currentTimeMillis()
)

@Serializable
data class PlayerPublicInfo(
    val playerId: String,
    val playerName: String,
    val isHost: Boolean,
    val isReady: Boolean,
    val isConnected: Boolean,
    val assignedCharacterId: String? = null,
    val assignedCharacterName: String? = null
)

@Serializable
data class CreateRoomRequest(
    val hostPlayerName: String = "Host Detective",
    val theme: String = "Mansion inheritance mystery",
    val difficulty: String = "Medium",
    val atmosphere: String = "Classic detective mystery",
    val customPrompt: String = "",
    val matchDurationMinutes: Int = 18
)

@Serializable
data class CreateRoomResponse(
    val roomId: String,
    val roomCode: String,
    val playerId: String,
    val sessionToken: String,
    val snapshot: RoomPublicSnapshot
)

@Serializable
data class JoinRoomRequest(
    val roomCode: String,
    val playerName: String
)

@Serializable
data class JoinRoomResponse(
    val roomId: String,
    val roomCode: String,
    val playerId: String,
    val sessionToken: String,
    val snapshot: RoomPublicSnapshot
)

@Serializable
data class GenerateMysteryRoomRequest(
    val playerId: String,
    val sessionToken: String,
    val theme: String? = null,
    val difficulty: String? = null,
    val atmosphere: String? = null,
    val customPrompt: String? = null,
    val matchDurationMinutes: Int? = null
)

@Serializable
data class RoomPublicSnapshot(
    val roomId: String,
    val roomCode: String,
    val hostPlayerId: String,
    val state: RoomState,
    val playerCount: Int,
    val minPlayers: Int = 4,
    val maxPlayers: Int = 10,
    val players: List<PlayerPublicInfo>,
    val frozenPlayerCount: Int? = null,
    val generationJobId: String? = null,
    val generationStatus: GenerationJobStatus? = null,
    val caseIdentity: CaseIdentity? = null,
    val timerState: GameTimerState? = null,
    val matchDurationMinutes: Int = 18,
    val publicDiscoveredEvidenceIds: List<String> = emptyList(),
    val isChatLocked: Boolean = false,
    val errorMessage: String? = null
)

@Serializable
data class MyCharacterResponse(
    val character: CharacterProfile,
    val isCulprit: Boolean,
    val privateSecret: String,
    val privateDirectives: String = "",
    val primaryObjective: PersonalObjective? = null,
    val secondaryObjective: PersonalObjective? = null,
    val actualSequenceOfEvents: String = "",
    val culpritMotive: String = "",
    val culpritCoverStory: String = "",
    val exposingEvidenceIds: List<String> = emptyList(),
    val caseIdentity: CaseIdentity
)

@Serializable
data class PrivateMessage(
    val id: String,
    val roomCode: String,
    val senderPlayerId: String,
    val senderCharacterName: String,
    val recipientPlayerId: String,
    val recipientCharacterName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val messageType: String = "CHAT", // CHAT, EVIDENCE_SHARE, DEAL_PROPOSAL, DEAL_ACCEPTED, DEAL_REJECTED
    val attachedEvidenceId: String? = null,
    val dealDetails: String? = null
)

@Serializable
data class SendMessageRequest(
    val senderPlayerId: String,
    val sessionToken: String,
    val recipientPlayerId: String,
    val text: String,
    val messageType: String = "CHAT",
    val attachedEvidenceId: String? = null,
    val dealDetails: String? = null
)

@Serializable
data class DealProposal(
    val id: String,
    val roomCode: String,
    val proposerPlayerId: String,
    val recipientPlayerId: String,
    val title: String,
    val terms: String,
    val status: String = "PENDING", // PENDING, ACCEPTED, REJECTED
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class ProposeDealRequest(
    val proposerPlayerId: String,
    val sessionToken: String,
    val recipientPlayerId: String,
    val title: String,
    val terms: String
)

@Serializable
data class RespondDealRequest(
    val responderPlayerId: String,
    val sessionToken: String,
    val dealId: String,
    val accept: Boolean
)

@Serializable
data class BallotVote(
    val voterPlayerId: String,
    val accusedCulpritId: String,
    val methodAccusation: String,
    val motiveAccusation: String,
    val smokingGunEvidenceId: String? = null,
    val submittedAt: Long = System.currentTimeMillis()
)

@Serializable
data class SubmitVoteRequest(
    val playerId: String,
    val sessionToken: String,
    val accusedCulpritId: String,
    val methodAccusation: String,
    val motiveAccusation: String,
    val smokingGunEvidenceId: String? = null
)

@Serializable
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

@Serializable
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

@Serializable
data class WsEvent(
    val type: String, // ROOM_SNAPSHOT, TIMER_TICK, PHASE_CHANGED, NEW_MESSAGE, EVIDENCE_SHARED, TWIST_ALERT, VOTE_RECEIVED, RESOLUTION
    val payload: String,
    val timestamp: Long = System.currentTimeMillis()
)
