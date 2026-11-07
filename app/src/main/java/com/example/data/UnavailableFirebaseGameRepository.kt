package com.example.data

import com.example.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Explicit placeholder repository for Firebase Online mode in Phase 0.
 *
 * This implementation fails closed: it refuses commands and presents an explicit
 * [ConnectionState.Unavailable] rather than silently falling back to local/offline mode.
 */
class UnavailableFirebaseGameRepository(
    val unavailabilityReason: String = "Firebase online multiplayer is not available in Phase 0. Please select Offline/Demo mode."
) : GameRepository {

    override val gameMode: GameMode = GameMode.FIREBASE_ONLINE

    private val _connectionState = MutableStateFlow<ConnectionState>(
        ConnectionState.Unavailable(unavailabilityReason)
    )
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _authIdentity = MutableStateFlow<AuthIdentity?>(null)
    override val authIdentity: StateFlow<AuthIdentity?> = _authIdentity.asStateFlow()

    fun setAuthIdentity(identity: AuthIdentity?) {
        _authIdentity.value = identity
    }

    override val currentRoom: StateFlow<RoomSnapshot?> = MutableStateFlow<RoomSnapshot?>(null).asStateFlow()
    override val myPlayerId: StateFlow<String?> = MutableStateFlow<String?>(null).asStateFlow()
    override val isHost: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
    override val myAssignedCharacter: StateFlow<MyAssignedCharacter?> = MutableStateFlow<MyAssignedCharacter?>(null).asStateFlow()

    override val savedCases: StateFlow<List<CasePackage>> = MutableStateFlow<List<CasePackage>>(emptyList()).asStateFlow()
    override val activeCase: StateFlow<CasePackage?> = MutableStateFlow<CasePackage?>(null).asStateFlow()
    override val assignedCharacter: StateFlow<CharacterDossier?> = MutableStateFlow<CharacterDossier?>(null).asStateFlow()
    override val isDossierConcealed: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
    override val activeLocationId: StateFlow<String?> = MutableStateFlow<String?>(null).asStateFlow()
    override val primaryObjective: StateFlow<PersonalObjective?> = MutableStateFlow<PersonalObjective?>(null).asStateFlow()
    override val secondaryObjective: StateFlow<PersonalObjective?> = MutableStateFlow<PersonalObjective?>(null).asStateFlow()
    override val privateMessages: StateFlow<List<PrivateMessage>> = MutableStateFlow<List<PrivateMessage>>(emptyList()).asStateFlow()
    override val dealProposals: StateFlow<List<DealProposal>> = MutableStateFlow<List<DealProposal>>(emptyList()).asStateFlow()
    override val activeTwist: StateFlow<MidGameTwist?> = MutableStateFlow<MidGameTwist?>(null).asStateFlow()
    override val isTwistDismissed: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
    override val gameTimer: StateFlow<GameTimerState?> = MutableStateFlow<GameTimerState?>(null).asStateFlow()
    override val hasVoted: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
    override val gameResolution: StateFlow<GameResolutionSummary?> = MutableStateFlow<GameResolutionSummary?>(null).asStateFlow()
    override val discoveredEvidenceIds: StateFlow<Set<String>> = MutableStateFlow<Set<String>>(emptySet()).asStateFlow()
    override val inspectedHotspotIds: StateFlow<Set<String>> = MutableStateFlow<Set<String>>(emptySet()).asStateFlow()
    override val suspiciousCharacterIds: StateFlow<Set<String>> = MutableStateFlow<Set<String>>(emptySet()).asStateFlow()
    override val boardConnections: StateFlow<List<BoardConnection>> = MutableStateFlow<List<BoardConnection>>(emptyList()).asStateFlow()
    override val deductionNotes: StateFlow<List<DeductionNote>> = MutableStateFlow<List<DeductionNote>>(emptyList()).asStateFlow()
    override val interrogationHistory: StateFlow<Map<String, List<InterrogationQnA>>> = MutableStateFlow<Map<String, List<InterrogationQnA>>>(emptyMap()).asStateFlow()
    override val verdict: StateFlow<AccusationVerdict?> = MutableStateFlow<AccusationVerdict?>(null).asStateFlow()

    private fun unavailableException(): IllegalStateException =
        IllegalStateException(unavailabilityReason)

    override suspend fun createRoom(playerName: String): Result<RoomSession> =
        Result.failure(unavailableException())

    override suspend fun joinRoom(roomCode: String, playerName: String): Result<RoomSession> =
        Result.failure(unavailableException())

    override suspend fun setReady(roomId: String, ready: Boolean): Result<Unit> =
        Result.failure(unavailableException())

    override suspend fun leaveRoom(roomId: String): Result<Unit> =
        Result.failure(unavailableException())

    override fun createRoom(params: CreateRoomParams): RoomSnapshot =
        throw unavailableException()

    override fun joinRoom(params: JoinRoomParams): Result<RoomSnapshot> =
        Result.failure(unavailableException())

    override fun addMockInvestigator(name: String): Result<RoomSnapshot> =
        Result.failure(unavailableException())

    override fun removeMockInvestigator(playerId: String): Result<RoomSnapshot> =
        Result.failure(unavailableException())

    override fun togglePlayerReady(isReady: Boolean) {
        throw unavailableException()
    }

    override fun leaveRoom() {
        // No-op for safety
    }

    override fun observeRoom(roomId: String): Flow<RoomSnapshot?> = emptyFlow()

    override fun observePlayers(roomId: String): Flow<List<PlayerInfo>> = emptyFlow()

    override suspend fun generateCase(
        config: CaseGenerationConfig,
        onProgress: (Int, String) -> Unit
    ): CasePackage = throw unavailableException()

    override suspend fun generateRoomMystery(
        config: CaseGenerationConfig,
        onProgress: (Int, String) -> Unit
    ): Result<CasePackage> = Result.failure(unavailableException())

    override fun transitionRoomState(newState: RoomState) {
        throw unavailableException()
    }

    override fun selectCase(casePkg: CasePackage, assignedCharId: String?) {
        throw unavailableException()
    }

    override fun deleteCase(caseId: String) {
        throw unavailableException()
    }

    override fun setActiveLocation(locationId: String) {
        throw unavailableException()
    }

    override fun toggleConcealDossier() {
        throw unavailableException()
    }

    override fun inspectHotspot(hotspotId: String) {
        throw unavailableException()
    }

    override fun collectEvidence(evidenceId: String) {
        throw unavailableException()
    }

    override fun toggleSuspectSuspicion(characterId: String) {
        throw unavailableException()
    }

    override fun askInterrogationQuestion(characterId: String, qna: InterrogationQnA) {
        throw unavailableException()
    }

    override fun addBoardConnection(suspectId: String, evidenceId: String, label: String) {
        throw unavailableException()
    }

    override fun removeBoardConnection(connectionId: String) {
        throw unavailableException()
    }

    override fun addDeductionNote(text: String) {
        throw unavailableException()
    }

    override fun updateObjectiveProgress(objectiveType: String, delta: Int) {
        throw unavailableException()
    }

    override fun sendPrivateMessage(
        recipientPlayerId: String,
        text: String,
        attachedEvidenceId: String?,
        dealDetails: String?
    ): PrivateMessage = throw unavailableException()

    override fun proposeDeal(recipientPlayerId: String, title: String, terms: String): DealProposal =
        throw unavailableException()

    override fun respondToDeal(dealId: String, accept: Boolean) {
        throw unavailableException()
    }

    override fun triggerMidGameTwist() {
        throw unavailableException()
    }

    override fun dismissTwist() {
        // No-op
    }

    override fun submitBallotVote(
        accusedCulpritId: String,
        method: String,
        motive: String,
        smokingGunEvidenceId: String?
    ) {
        throw unavailableException()
    }

    override fun tallyAndResolveCase(): GameResolutionSummary =
        throw unavailableException()

    override fun submitAccusation(
        accusedId: String,
        motiveText: String,
        smokingGunEvidenceId: String?,
        accusedSuspectId: String,
        selectedMotive: String
    ): AccusationVerdict = throw unavailableException()
}

