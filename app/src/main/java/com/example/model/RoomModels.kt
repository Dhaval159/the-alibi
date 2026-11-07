package com.example.model

enum class RoomState {
    WAITING_FOR_PLAYERS,
    GENERATING_CASE,
    VALIDATING_CASE,
    GENERATING_ASSETS,
    ASSIGNING_CHARACTERS,
    CASE_BRIEFING,          // Phase B: Opening Briefing (2 mins)
    CHARACTER_REVEAL,       // Phase C: Private Character Dossier
    INVESTIGATION,          // Phase D: Act I Investigation (5-10 mins)
    MID_GAME_TWIST,         // Phase G: Mid-Game Dramatic Twist
    INTERROGATION_DEBATE,   // Phase H: Act II Group Interrogation & Debate
    VOTING,                 // Phase I: Secret Final Accusation Ballot
    CASE_RESOLUTION         // Phase J: Final Cinematic Reveal & Scoring
}

data class GameTimerState(
    val phase: RoomState,
    val startTimeMillis: Long,
    val durationSeconds: Int,
    val endTimeMillis: Long,
    val remainingSeconds: Int,
    val isPaused: Boolean = false
)

data class PlayerInfo(
    val playerId: String,
    val playerName: String,
    val isHost: Boolean,
    val isReady: Boolean = true,
    val isConnected: Boolean = true,
    val assignedCharacterId: String? = null,
    val assignedCharacterName: String? = null
)

data class RoomSnapshot(
    val roomId: String,
    val roomCode: String,
    val hostPlayerId: String,
    val state: RoomState,
    val playerCount: Int,
    val minPlayers: Int = 4,
    val maxPlayers: Int = 10,
    val players: List<PlayerInfo>,
    val frozenPlayerCount: Int? = null,
    val generationJobId: String? = null,
    val generationProgressPercent: Int = 0,
    val generationStage: String = "",
    val caseIdentity: CaseIdentity? = null,
    val timerState: GameTimerState? = null,
    val matchDurationMinutes: Int = 18,
    val publicDiscoveredEvidenceIds: List<String> = emptyList(),
    val isChatLocked: Boolean = false,
    val errorMessage: String? = null
)

data class CreateRoomParams(
    val hostPlayerName: String = "Host Detective",
    val theme: String = "Mansion inheritance mystery",
    val difficulty: String = "Medium",
    val atmosphere: String = "Classic detective mystery",
    val customPrompt: String = "",
    val matchDurationMinutes: Int = 18
)

data class JoinRoomParams(
    val roomCode: String,
    val playerName: String
)

data class MyAssignedCharacter(
    val character: CharacterDossier,
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
