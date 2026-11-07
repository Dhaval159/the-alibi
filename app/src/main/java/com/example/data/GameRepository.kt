package com.example.data

import com.example.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

typealias AlibiRepository = GameRepository

/**
 * Architectural seam abstraction separating the ViewModel from the data layer.
 * Allows the game to switch between local in-memory gameplay and future Firebase online multiplayer.
 */
interface GameRepository {

    // Identity and Connection
    val authIdentity: StateFlow<AuthIdentity?>
    val connectionState: StateFlow<ConnectionState>
    val gameMode: GameMode

    // Room and multiplayer state flows
    val currentRoom: StateFlow<RoomSnapshot?>
    val myPlayerId: StateFlow<String?>
    val isHost: StateFlow<Boolean>
    val myAssignedCharacter: StateFlow<MyAssignedCharacter?>

    // Active cases & gameplay state flows
    val savedCases: StateFlow<List<CasePackage>>
    val activeCase: StateFlow<CasePackage?>
    val assignedCharacter: StateFlow<CharacterDossier?>
    val isDossierConcealed: StateFlow<Boolean>
    val activeLocationId: StateFlow<String?>
    val primaryObjective: StateFlow<PersonalObjective?>
    val secondaryObjective: StateFlow<PersonalObjective?>
    val privateMessages: StateFlow<List<PrivateMessage>>
    val dealProposals: StateFlow<List<DealProposal>>
    val activeTwist: StateFlow<MidGameTwist?>
    val isTwistDismissed: StateFlow<Boolean>
    val gameTimer: StateFlow<GameTimerState?>
    val hasVoted: StateFlow<Boolean>
    val gameResolution: StateFlow<GameResolutionSummary?>
    val discoveredEvidenceIds: StateFlow<Set<String>>
    val inspectedHotspotIds: StateFlow<Set<String>>
    val suspiciousCharacterIds: StateFlow<Set<String>>
    val boardConnections: StateFlow<List<BoardConnection>>
    val deductionNotes: StateFlow<List<DeductionNote>>
    val interrogationHistory: StateFlow<Map<String, List<InterrogationQnA>>>
    val verdict: StateFlow<AccusationVerdict?>

    // Suspend commands
    suspend fun createRoom(playerName: String): Result<RoomSession>
    suspend fun joinRoom(roomCode: String, playerName: String): Result<RoomSession>
    suspend fun setReady(roomId: String, ready: Boolean): Result<Unit>
    suspend fun leaveRoom(roomId: String): Result<Unit>

    // Legacy / parameter-based room commands (preserved for compatibility)
    fun createRoom(params: CreateRoomParams): RoomSnapshot
    fun joinRoom(params: JoinRoomParams): Result<RoomSnapshot>
    fun addMockInvestigator(name: String): Result<RoomSnapshot>
    fun removeMockInvestigator(playerId: String): Result<RoomSnapshot>
    fun togglePlayerReady(isReady: Boolean)
    fun leaveRoom()

    // Room observation
    fun observeRoom(roomId: String): Flow<RoomSnapshot?>
    fun observePlayers(roomId: String): Flow<List<PlayerInfo>>

    // Case generation
    suspend fun generateCase(
        config: CaseGenerationConfig,
        onProgress: (Int, String) -> Unit
    ): CasePackage

    suspend fun generateRoomMystery(
        config: CaseGenerationConfig,
        onProgress: (Int, String) -> Unit
    ): Result<CasePackage>

    // Phase transition
    fun transitionRoomState(newState: RoomState)

    // Investigation & Case Selection
    fun selectCase(casePkg: CasePackage, assignedCharId: String? = null)
    fun deleteCase(caseId: String)
    fun setActiveLocation(locationId: String)
    fun toggleConcealDossier()
    fun inspectHotspot(hotspotId: String)
    fun collectEvidence(evidenceId: String)
    fun toggleSuspectSuspicion(characterId: String)
    fun askInterrogationQuestion(characterId: String, qna: InterrogationQnA)
    fun addBoardConnection(suspectId: String, evidenceId: String, label: String)
    fun removeBoardConnection(connectionId: String)
    fun addDeductionNote(text: String)
    fun updateObjectiveProgress(objectiveType: String, delta: Int)

    // Messaging & Deals
    fun sendPrivateMessage(
        recipientPlayerId: String,
        text: String,
        attachedEvidenceId: String? = null,
        dealDetails: String? = null
    ): PrivateMessage

    fun proposeDeal(recipientPlayerId: String, title: String, terms: String): DealProposal
    fun respondToDeal(dealId: String, accept: Boolean)

    // Mid-game twist
    fun triggerMidGameTwist()
    fun dismissTwist()

    // Accusation & Secret Ballot
    fun submitBallotVote(
        accusedCulpritId: String,
        method: String,
        motive: String,
        smokingGunEvidenceId: String? = null
    )

    fun tallyAndResolveCase(): GameResolutionSummary

    fun submitAccusation(
        accusedId: String = "",
        motiveText: String = "",
        smokingGunEvidenceId: String? = null,
        accusedSuspectId: String = accusedId,
        selectedMotive: String = motiveText
    ): AccusationVerdict
}

