package com.example.data

import com.example.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import okhttp3.*
import org.json.JSONObject
import java.util.UUID
import kotlin.random.Random

/**
 * Local room store for offline/demo simulation.
 * Instances can explicitly share a store if local multiplayer simulation is desired.
 * Each repository instance gets its own isolated store by default.
 * Do not use a static companion-object map as an implicit multiplayer backend.
 */
class LocalRoomStore {
    private val _rooms = mutableMapOf<String, RoomSnapshot>()
    val rooms: Map<String, RoomSnapshot> get() = _rooms

    fun createRoom(room: RoomSnapshot) {
        _rooms[room.roomCode] = room
    }

    fun updateRoom(roomCode: String, updatedRoom: RoomSnapshot) {
        _rooms[roomCode] = updatedRoom
    }

    fun removeRoom(roomCode: String) {
        _rooms.remove(roomCode)
    }

    fun getRoom(roomCode: String): RoomSnapshot? = _rooms[roomCode]

    fun clear() {
        _rooms.clear()
    }
}

class TheAlibiRepository(
    val roomStore: LocalRoomStore = LocalRoomStore()
) : GameRepository {

    override val gameMode: GameMode = GameMode.OFFLINE_DEMO

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _authIdentity = MutableStateFlow<AuthIdentity?>(
        AuthIdentity(
            uid = "local_demo_player",
            displayName = "Local Investigator",
            isAnonymous = true,
            isOnlineAuth = false
        )
    )
    override val authIdentity: StateFlow<AuthIdentity?> = _authIdentity.asStateFlow()

    fun setAuthIdentity(identity: AuthIdentity?) {
        _authIdentity.value = identity
    }

    // Room and multiplayer state
    private val _currentRoom = MutableStateFlow<RoomSnapshot?>(null)
    override val currentRoom: StateFlow<RoomSnapshot?> = _currentRoom.asStateFlow()

    private val _myPlayerId = MutableStateFlow<String?>(null)
    override val myPlayerId: StateFlow<String?> = _myPlayerId.asStateFlow()

    private val _mySessionToken = MutableStateFlow<String?>(null)
    val mySessionToken: StateFlow<String?> = _mySessionToken.asStateFlow()

    private val _isHost = MutableStateFlow(false)
    override val isHost: StateFlow<Boolean> = _isHost.asStateFlow()

    private val _myAssignedCharacter = MutableStateFlow<MyAssignedCharacter?>(null)
    override val myAssignedCharacter: StateFlow<MyAssignedCharacter?> = _myAssignedCharacter.asStateFlow()

    // Active cases & gameplay state
    private val _savedCases = MutableStateFlow<List<CasePackage>>(emptyList())
    override val savedCases: StateFlow<List<CasePackage>> = _savedCases.asStateFlow()

    private val _activeCase = MutableStateFlow<CasePackage?>(null)
    override val activeCase: StateFlow<CasePackage?> = _activeCase.asStateFlow()

    private val _assignedCharacter = MutableStateFlow<CharacterDossier?>(null)
    override val assignedCharacter: StateFlow<CharacterDossier?> = _assignedCharacter.asStateFlow()

    private val _isDossierConcealed = MutableStateFlow(false)
    override val isDossierConcealed: StateFlow<Boolean> = _isDossierConcealed.asStateFlow()

    private val _activeLocationId = MutableStateFlow<String?>("loc_study")
    override val activeLocationId: StateFlow<String?> = _activeLocationId.asStateFlow()

    private val _primaryObjective = MutableStateFlow<PersonalObjective?>(null)
    override val primaryObjective: StateFlow<PersonalObjective?> = _primaryObjective.asStateFlow()

    private val _secondaryObjective = MutableStateFlow<PersonalObjective?>(null)
    override val secondaryObjective: StateFlow<PersonalObjective?> = _secondaryObjective.asStateFlow()

    private val _privateMessages = MutableStateFlow<List<PrivateMessage>>(emptyList())
    override val privateMessages: StateFlow<List<PrivateMessage>> = _privateMessages.asStateFlow()

    private val _dealProposals = MutableStateFlow<List<DealProposal>>(emptyList())
    override val dealProposals: StateFlow<List<DealProposal>> = _dealProposals.asStateFlow()

    private val _activeTwist = MutableStateFlow<MidGameTwist?>(null)
    override val activeTwist: StateFlow<MidGameTwist?> = _activeTwist.asStateFlow()

    private val _isTwistDismissed = MutableStateFlow(false)
    override val isTwistDismissed: StateFlow<Boolean> = _isTwistDismissed.asStateFlow()

    private val _gameTimer = MutableStateFlow<GameTimerState?>(null)
    override val gameTimer: StateFlow<GameTimerState?> = _gameTimer.asStateFlow()

    private val _hasVoted = MutableStateFlow(false)
    override val hasVoted: StateFlow<Boolean> = _hasVoted.asStateFlow()

    private val _gameResolution = MutableStateFlow<GameResolutionSummary?>(null)
    override val gameResolution: StateFlow<GameResolutionSummary?> = _gameResolution.asStateFlow()

    private val _discoveredEvidenceIds = MutableStateFlow<Set<String>>(emptySet())
    override val discoveredEvidenceIds: StateFlow<Set<String>> = _discoveredEvidenceIds.asStateFlow()

    private val _inspectedHotspotIds = MutableStateFlow<Set<String>>(emptySet())
    override val inspectedHotspotIds: StateFlow<Set<String>> = _inspectedHotspotIds.asStateFlow()

    private val _suspiciousCharacterIds = MutableStateFlow<Set<String>>(emptySet())
    override val suspiciousCharacterIds: StateFlow<Set<String>> = _suspiciousCharacterIds.asStateFlow()

    private val _boardConnections = MutableStateFlow<List<BoardConnection>>(emptyList())
    override val boardConnections: StateFlow<List<BoardConnection>> = _boardConnections.asStateFlow()

    private val _deductionNotes = MutableStateFlow<List<DeductionNote>>(emptyList())
    override val deductionNotes: StateFlow<List<DeductionNote>> = _deductionNotes.asStateFlow()

    private val _interrogationHistory = MutableStateFlow<Map<String, List<InterrogationQnA>>>(emptyMap())
    override val interrogationHistory: StateFlow<Map<String, List<InterrogationQnA>>> = _interrogationHistory.asStateFlow()

    private val _verdict = MutableStateFlow<AccusationVerdict?>(null)
    override val verdict: StateFlow<AccusationVerdict?> = _verdict.asStateFlow()

    private var internalFrozenRoster: List<PlayerInfo>? = null
    private var internalCharacterAssignments: Map<String, String>? = null

    // OkHttp WebSocket client
    private val okHttpClient = OkHttpClient()
    private var webSocket: WebSocket? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    var serverBaseUrl: String = "http://10.0.2.2:8080"

    init {
    }

    // --- ROOM CREATION & LOBBY MANAGEMENT ---

    override suspend fun createRoom(playerName: String): Result<RoomSession> {
        val room = createRoom(CreateRoomParams(hostPlayerName = playerName))
        return Result.success(
            RoomSession(
                roomId = room.roomId,
                roomCode = room.roomCode,
                playerId = room.hostPlayerId,
                sessionToken = _mySessionToken.value,
                isHost = true
            )
        )
    }

    override suspend fun joinRoom(roomCode: String, playerName: String): Result<RoomSession> {
        val res = joinRoom(JoinRoomParams(roomCode = roomCode, playerName = playerName))
        return res.map { room ->
            RoomSession(
                roomId = room.roomId,
                roomCode = room.roomCode,
                playerId = _myPlayerId.value ?: "unknown",
                sessionToken = _mySessionToken.value,
                isHost = false
            )
        }
    }

    override suspend fun setReady(roomId: String, ready: Boolean): Result<Unit> {
        togglePlayerReady(ready)
        return Result.success(Unit)
    }

    override suspend fun leaveRoom(roomId: String): Result<Unit> {
        leaveRoom()
        return Result.success(Unit)
    }

    override fun observeRoom(roomId: String): Flow<RoomSnapshot?> = currentRoom

    override fun observePlayers(roomId: String): Flow<List<PlayerInfo>> =
        currentRoom.map { it?.players ?: emptyList() }

    override fun createRoom(params: CreateRoomParams): RoomSnapshot {
        val roomId = "room_${UUID.randomUUID().toString().take(8)}"
        val roomCode = (100000 + Random.nextInt(900000)).toString()
        val hostPlayerId = "player_${UUID.randomUUID().toString().take(6)}"
        val sessionToken = "tok_${UUID.randomUUID().toString().take(12)}"

        val hostInfo = PlayerInfo(
            playerId = hostPlayerId,
            playerName = params.hostPlayerName.ifBlank { "Host Detective" },
            isHost = true,
            isReady = true,
            isConnected = true
        )

        val room = RoomSnapshot(
            roomId = roomId,
            roomCode = roomCode,
            hostPlayerId = hostPlayerId,
            state = RoomState.WAITING_FOR_PLAYERS,
            playerCount = 1,
            minPlayers = 4,
            maxPlayers = 10,
            players = listOf(hostInfo),
            matchDurationMinutes = params.matchDurationMinutes
        )

        roomStore.createRoom(room)
        _currentRoom.value = room
        _myPlayerId.value = hostPlayerId
        _mySessionToken.value = sessionToken
        _isHost.value = true
        internalFrozenRoster = null
        internalCharacterAssignments = null
        _privateMessages.value = emptyList()
        _dealProposals.value = emptyList()
        _hasVoted.value = false
        _gameResolution.value = null

        return room
    }

    override fun joinRoom(params: JoinRoomParams): Result<RoomSnapshot> {
        val code = params.roomCode.trim()
        val existing = roomStore.getRoom(code)
            ?: return Result.failure(IllegalArgumentException("Room $code not found"))

        if (existing.state != RoomState.WAITING_FOR_PLAYERS) {
            return Result.failure(IllegalStateException("Case generation has started. This room is no longer accepting players."))
        }

        if (existing.players.size >= 10) {
            return Result.failure(IllegalStateException("Room $code is full (maximum 10 players)."))
        }

        val newPlayerId = "player_${UUID.randomUUID().toString().take(6)}"
        val sessionToken = "tok_${UUID.randomUUID().toString().take(12)}"
        val newPlayer = PlayerInfo(
            playerId = newPlayerId,
            playerName = params.playerName.ifBlank { "Investigator ${existing.players.size + 1}" },
            isHost = false,
            isReady = true,
            isConnected = true
        )

        val updatedPlayers = existing.players + newPlayer
        val updated = existing.copy(
            players = updatedPlayers,
            playerCount = updatedPlayers.size
        )

        roomStore.updateRoom(code, updated)
        _currentRoom.value = updated
        _myPlayerId.value = newPlayerId
        _mySessionToken.value = sessionToken
        _isHost.value = false
        _privateMessages.value = emptyList()
        _dealProposals.value = emptyList()
        _hasVoted.value = false
        _gameResolution.value = null

        return Result.success(updated)
    }

    override fun addMockInvestigator(name: String): Result<RoomSnapshot> {
        val current = _currentRoom.value
            ?: return Result.failure(IllegalStateException("No active room"))

        if (current.state != RoomState.WAITING_FOR_PLAYERS) {
            return Result.failure(IllegalStateException("Case generation has started. Roster is locked."))
        }

        if (current.players.size >= 10) {
            return Result.failure(IllegalStateException("Room is full (maximum 10 players)."))
        }

        val newPlayerId = "player_mock_${UUID.randomUUID().toString().take(4)}"
        val mockPlayer = PlayerInfo(
            playerId = newPlayerId,
            playerName = name.ifBlank { "Agent ${current.players.size + 1}" },
            isHost = false,
            isReady = true,
            isConnected = true
        )

        val updatedPlayers = current.players + mockPlayer
        val updated = current.copy(
            players = updatedPlayers,
            playerCount = updatedPlayers.size
        )

        roomStore.updateRoom(current.roomCode, updated)
        _currentRoom.value = updated
        return Result.success(updated)
    }

    override fun removeMockInvestigator(playerId: String): Result<RoomSnapshot> {
        val current = _currentRoom.value ?: return Result.failure(IllegalStateException("No active room"))
        if (current.state != RoomState.WAITING_FOR_PLAYERS) {
            return Result.failure(IllegalStateException("Roster is locked."))
        }

        val updatedPlayers = current.players.filter { it.playerId != playerId || it.isHost }
        val updated = current.copy(
            players = updatedPlayers,
            playerCount = updatedPlayers.size
        )
        roomStore.updateRoom(current.roomCode, updated)
        _currentRoom.value = updated
        return Result.success(updated)
    }

    override fun togglePlayerReady(isReady: Boolean) {
        val current = _currentRoom.value ?: return
        val myId = _myPlayerId.value ?: return
        val updatedPlayers = current.players.map {
            if (it.playerId == myId) it.copy(isReady = isReady) else it
        }
        val updated = current.copy(players = updatedPlayers)
        roomStore.updateRoom(current.roomCode, updated)
        _currentRoom.value = updated
    }

    override suspend fun generateCase(
        config: CaseGenerationConfig,
        onProgress: (Int, String) -> Unit
    ): CasePackage {
        onProgress(20, "Contacting Gemini Mystery Architect...")
        delay(300)
        onProgress(50, "Generating crime scene and suspects...")
        delay(300)
        onProgress(85, "Sealing forensic evidence chains...")
        delay(300)
        val casePkg = createGeneratedCase(config)
        _savedCases.value = listOf(casePkg) + _savedCases.value
        onProgress(100, "Case compiled and verified.")
        return casePkg
    }

    override suspend fun generateRoomMystery(
        config: CaseGenerationConfig,
        onProgress: (Int, String) -> Unit
    ): Result<CasePackage> {
        val current = _currentRoom.value
            ?: return Result.failure(IllegalStateException("No active room"))

        if (!_isHost.value) {
            return Result.failure(IllegalAccessException("Only the host can initiate mystery generation."))
        }

        if (current.state != RoomState.WAITING_FOR_PLAYERS) {
            return Result.failure(IllegalStateException("Generation has already started."))
        }

        val actualCount = current.players.size
        if (actualCount < 4) {
            return Result.failure(IllegalStateException("Minimum 4 players required to generate mystery (currently $actualCount)."))
        }
        if (actualCount > 10) {
            return Result.failure(IllegalStateException("Maximum 10 players supported (currently $actualCount)."))
        }

        // 1. Freeze Roster
        val frozen = current.players.toList()
        internalFrozenRoster = frozen
        val generatingState = current.copy(
            state = RoomState.GENERATING_CASE,
            frozenPlayerCount = actualCount,
            generationProgressPercent = 10,
            generationStage = "Freezing player roster ($actualCount players)..."
        )
        _currentRoom.value = generatingState
        onProgress(10, "Freezing player roster ($actualCount players)...")
        delay(350)

        // 2. Generate Mystery for exact player count N
        onProgress(25, "Architecting a mystery for $actualCount investigators...")
        _currentRoom.value = _currentRoom.value?.copy(
            generationProgressPercent = 25,
            generationStage = "Architecting a mystery for $actualCount investigators..."
        )
        delay(350)

        val exactConfig = config.copy(playerCount = actualCount)
        val casePackage = createGeneratedCase(exactConfig)

        onProgress(50, "Validating forensic chains and deduction paths...")
        _currentRoom.value = _currentRoom.value?.copy(
            state = RoomState.VALIDATING_CASE,
            generationProgressPercent = 50,
            generationStage = "Validating forensic chains and deduction paths..."
        )
        delay(350)

        onProgress(75, "Generating 5 interactive locations & $actualCount suspect dossiers...")
        _currentRoom.value = _currentRoom.value?.copy(
            state = RoomState.GENERATING_ASSETS,
            generationProgressPercent = 75,
            generationStage = "Generating 5 interactive locations & $actualCount suspect dossiers..."
        )
        delay(350)

        // 3. Assign Characters to Frozen Roster
        onProgress(90, "Assigning secret character identities...")
        _currentRoom.value = _currentRoom.value?.copy(
            state = RoomState.ASSIGNING_CHARACTERS,
            generationProgressPercent = 90,
            generationStage = "Assigning secret character identities..."
        )
        delay(350)

        val culpritId = casePackage.secretSolution?.culpritId ?: casePackage.characters.first().id
        val culpritChar = casePackage.characters.find { it.id == culpritId } ?: casePackage.characters.first()
        val innocentChars = casePackage.characters.filter { it.id != culpritId }

        val humanChars = mutableListOf(culpritChar)
        humanChars.addAll(innocentChars.take(actualCount - 1))
        val shuffledChars = humanChars.shuffled(Random(System.currentTimeMillis()))

        val assignments = mutableMapOf<String, String>()
        for (i in frozen.indices) {
            val player = frozen[i]
            val assignedChar = shuffledChars[i]
            assignments[player.playerId] = assignedChar.id
        }
        internalCharacterAssignments = assignments

        // 4. Update Room to CASE_BRIEFING
        val briefingRoom = current.copy(
            state = RoomState.CASE_BRIEFING,
            frozenPlayerCount = actualCount,
            generationProgressPercent = 100,
            generationStage = "Dossier compiled! Ready for investigation.",
            caseIdentity = casePackage.identity
        )
        _currentRoom.value = briefingRoom
        roomStore.updateRoom(current.roomCode, briefingRoom)

        // Save generated case into library
        _savedCases.value = listOf(casePackage) + _savedCases.value

        // Setup local player's private character assignment & tailored objectives
        val myId = _myPlayerId.value ?: frozen.first().playerId
        val myCharId = assignments[myId] ?: shuffledChars.first().id
        selectCase(casePackage, myCharId)

        val myChar = casePackage.characters.find { it.id == myCharId } ?: casePackage.characters.first()
        val isCulprit = myChar.id == culpritId

        val primaryObj = if (isCulprit) {
            PersonalObjective(
                id = "obj_primary_${myChar.id}",
                title = "Conceal Culprit Identity & Shift Suspicion",
                description = "Maintain your alibi, conceal the stolen will draft, and steer group suspicion onto Dr. Finch or Beatrice.",
                category = ObjectiveCategory.CULPRIT_SPECIAL,
                isPrimary = true,
                points = 200,
                progress = 0
            )
        } else {
            PersonalObjective(
                id = "obj_primary_${myChar.id}",
                title = "Identify Lord Blackwood's Murderer",
                description = "Examine the crime scene, discover forensic evidence, uncover contradictions in testimonies, and accuse the true culprit.",
                category = ObjectiveCategory.INVESTIGATION,
                isPrimary = true,
                points = 150,
                progress = 0
            )
        }

        val secondaryObj = if (isCulprit) {
            PersonalObjective(
                id = "obj_sec_${myChar.id}",
                title = "Secure an Alibi Witness via Secret Deal",
                description = "Propose a deal with another investigator via private message to vouch for your presence during the 23:15 window.",
                category = ObjectiveCategory.NEGOTIATION,
                isPrimary = false,
                points = 100,
                progress = 0
            )
        } else {
            PersonalObjective(
                id = "obj_sec_${myChar.id}",
                title = "Protect Private Secret & Share Clues",
                description = "Conceal your private background while sharing at least one piece of discovered evidence with an ally.",
                category = ObjectiveCategory.SECRET_PROTECTION,
                isPrimary = false,
                points = 100,
                progress = 0
            )
        }

        _primaryObjective.value = primaryObj
        _secondaryObjective.value = secondaryObj

        val directives = if (isCulprit) {
            "CULPRIT DIRECTIVE: Keep your cool. Maintain your alibi and divert suspicion toward innocent discrepancies. Do not let anyone link you to the terrace clay footprints or belladonna."
        } else ""

        _myAssignedCharacter.value = MyAssignedCharacter(
            character = myChar,
            isCulprit = isCulprit,
            privateSecret = myChar.privateSecret,
            privateDirectives = directives,
            primaryObjective = primaryObj,
            secondaryObjective = secondaryObj,
            actualSequenceOfEvents = if (isCulprit) "Entered study via secret garden corridor at 23:15, poisoned decanter, burned will draft." else "",
            culpritMotive = if (isCulprit) "40,000 pound debt to Monte Carlo syndicates due at midnight." else "",
            culpritCoverStory = if (isCulprit) "Walking alone in the south conservatory." else "",
            exposingEvidenceIds = if (isCulprit) listOf("ev_muddy_footprints", "ev_silver_cigarette_case", "ev_torn_will_draft") else emptyList(),
            caseIdentity = casePackage.identity
        )

        // Set initial timer
        _gameTimer.value = GameTimerState(
            phase = RoomState.CASE_BRIEFING,
            startTimeMillis = System.currentTimeMillis(),
            durationSeconds = 120,
            endTimeMillis = System.currentTimeMillis() + 120_000,
            remainingSeconds = 120
        )

        onProgress(100, "Mystery ready! Transitioning to case briefing.")
        return Result.success(casePackage)
    }

    override fun transitionRoomState(newState: RoomState) {
        val current = _currentRoom.value ?: return
        val durationSec = when (newState) {
            RoomState.CASE_BRIEFING -> 120
            RoomState.CHARACTER_REVEAL -> 90
            RoomState.INVESTIGATION -> 480
            RoomState.MID_GAME_TWIST -> 90
            RoomState.INTERROGATION_DEBATE -> 300
            RoomState.VOTING -> 120
            RoomState.CASE_RESOLUTION -> 0
            else -> 60
        }

        val updatedTimer = if (durationSec > 0) {
            GameTimerState(
                phase = newState,
                startTimeMillis = System.currentTimeMillis(),
                durationSeconds = durationSec,
                endTimeMillis = System.currentTimeMillis() + (durationSec * 1000L),
                remainingSeconds = durationSec
            )
        } else null

        val updated = current.copy(
            state = newState,
            timerState = updatedTimer,
            isChatLocked = newState == RoomState.INTERROGATION_DEBATE || newState == RoomState.VOTING || newState == RoomState.CASE_RESOLUTION
        )
        _currentRoom.value = updated
        _gameTimer.value = updatedTimer
        roomStore.updateRoom(current.roomCode, updated)

        if (newState == RoomState.MID_GAME_TWIST) {
            triggerMidGameTwist()
        }
    }

    override fun leaveRoom() {
        val current = _currentRoom.value ?: return
        val myId = _myPlayerId.value
        if (myId != null) {
            val updatedPlayers = current.players.filter { it.playerId != myId }
            roomStore.updateRoom(current.roomCode, current.copy(players = updatedPlayers, playerCount = updatedPlayers.size))
        }
        webSocket?.close(1000, "Leaving room")
        webSocket = null
        _currentRoom.value = null
        _myPlayerId.value = null
        _mySessionToken.value = null
        _isHost.value = false
        _myAssignedCharacter.value = null
        _privateMessages.value = emptyList()
        _dealProposals.value = emptyList()
    }

    // --- CASE INVESTIGATION & GAMEPLAY ---

    override fun selectCase(casePkg: CasePackage, assignedCharId: String?) {
        _activeCase.value = casePkg
        val assigned = if (assignedCharId != null) {
            casePkg.characters.find { it.id == assignedCharId }
        } else {
            casePkg.characters.firstOrNull()
        }
        _assignedCharacter.value = assigned
        _isDossierConcealed.value = false
        _activeLocationId.value = casePkg.locations.firstOrNull()?.id ?: "loc_study"
        val initialEvidence = casePkg.evidence.filter { it.discoveryPhase == "Initial" }.map { it.id }.toSet()
        _discoveredEvidenceIds.value = initialEvidence
        _inspectedHotspotIds.value = emptySet()
        _suspiciousCharacterIds.value = emptySet()
        _boardConnections.value = emptyList()
        _deductionNotes.value = listOf(
            DeductionNote("note_1", "Case opened: Lord Blackwood found dead in locked study at 23:45.", "23:50")
        )
        _interrogationHistory.value = emptyMap()
        _verdict.value = null
        _hasVoted.value = false
        _gameResolution.value = null
        _isTwistDismissed.value = false
        _activeTwist.value = casePkg.midGameTwist
    }

    override fun deleteCase(caseId: String) {
        _savedCases.value = _savedCases.value.filter { it.identity.caseId != caseId }
    }

    override fun setActiveLocation(locationId: String) {
        _activeLocationId.value = locationId
        val loc = _activeCase.value?.locations?.find { it.id == locationId }
        if (loc != null) {
            addDeductionNote("Moved to ${loc.name}: ${loc.atmosphereSnippet}")
        }
    }

    override fun toggleConcealDossier() {
        _isDossierConcealed.value = !_isDossierConcealed.value
    }

    override fun inspectHotspot(hotspotId: String) {
        _inspectedHotspotIds.value = _inspectedHotspotIds.value + hotspotId
        // Search across crimeScene and all locations
        val allHotspots = (_activeCase.value?.crimeScene?.hotspots ?: emptyList()) +
                (_activeCase.value?.locations?.flatMap { it.hotspots } ?: emptyList())
        val hotspot = allHotspots.find { it.id == hotspotId }
        if (hotspot?.associatedEvidenceId != null) {
            collectEvidence(hotspot.associatedEvidenceId)
        }
        // Update objective progress
        updateObjectiveProgress("primary", 15)
    }

    override fun collectEvidence(evidenceId: String) {
        _discoveredEvidenceIds.value = _discoveredEvidenceIds.value + evidenceId
        val ev = _activeCase.value?.evidence?.find { it.id == evidenceId }
        if (ev != null) {
            addDeductionNote("Discovered clue: ${ev.name} at ${ev.discoveryLocation}.")
        }
    }

    override fun toggleSuspectSuspicion(characterId: String) {
        val current = _suspiciousCharacterIds.value
        _suspiciousCharacterIds.value = if (characterId in current) {
            current - characterId
        } else {
            current + characterId
        }
    }

    override fun askInterrogationQuestion(characterId: String, qna: InterrogationQnA) {
        val current = _interrogationHistory.value.toMutableMap()
        val list = current.getOrDefault(characterId, emptyList()).toMutableList()
        if (list.none { it.question == qna.question }) {
            list.add(qna)
            current[characterId] = list
            _interrogationHistory.value = current
            if (qna.revealsContradiction) {
                addDeductionNote("CONTRADICTION DISCOVERED in testimony: \"${qna.answer}\"")
                updateObjectiveProgress("primary", 25)
            }
        }
    }

    override fun addBoardConnection(suspectId: String, evidenceId: String, label: String) {
        val conn = BoardConnection(
            id = "conn_${UUID.randomUUID().toString().take(6)}",
            suspectId = suspectId,
            evidenceId = evidenceId,
            label = label
        )
        _boardConnections.value = _boardConnections.value + conn
        updateObjectiveProgress("primary", 10)
    }

    override fun removeBoardConnection(connectionId: String) {
        _boardConnections.value = _boardConnections.value.filter { it.id != connectionId }
    }

    override fun addDeductionNote(text: String) {
        val note = DeductionNote(
            id = "note_${UUID.randomUUID().toString().take(6)}",
            text = text,
            timestamp = "23:${Random.nextInt(10, 59)}"
        )
        _deductionNotes.value = _deductionNotes.value + note
    }

    override fun updateObjectiveProgress(objectiveType: String, delta: Int) {
        if (objectiveType == "primary") {
            val current = _primaryObjective.value ?: return
            val newProg = (current.progress + delta).coerceIn(0, 100)
            _primaryObjective.value = current.copy(
                progress = newProg,
                isCompleted = newProg >= 100
            )
        } else {
            val current = _secondaryObjective.value ?: return
            val newProg = (current.progress + delta).coerceIn(0, 100)
            _secondaryObjective.value = current.copy(
                progress = newProg,
                isCompleted = newProg >= 100
            )
        }
    }

    // --- PRIVATE MESSAGING & DEALS ---

    override fun sendPrivateMessage(
        recipientPlayerId: String,
        text: String,
        attachedEvidenceId: String?,
        dealDetails: String?
    ): PrivateMessage {
        val myId = _myPlayerId.value ?: "player_local"
        val myCharName = _assignedCharacter.value?.name ?: "Investigator"
        val targetCharName = _currentRoom.value?.players?.find { it.playerId == recipientPlayerId }?.playerName
            ?: _activeCase.value?.characters?.find { it.id == recipientPlayerId }?.name
            ?: "Colleague"

        val msgType = when {
            dealDetails != null -> "DEAL_PROPOSAL"
            attachedEvidenceId != null -> "EVIDENCE_SHARE"
            else -> "CHAT"
        }

        val message = PrivateMessage(
            id = "msg_${UUID.randomUUID().toString().take(8)}",
            senderPlayerId = myId,
            senderCharacterName = myCharName,
            recipientPlayerId = recipientPlayerId,
            recipientCharacterName = targetCharName,
            text = text,
            timestamp = System.currentTimeMillis(),
            messageType = msgType,
            attachedEvidenceId = attachedEvidenceId,
            dealDetails = dealDetails
        )

        _privateMessages.value = _privateMessages.value + message

        // If evidence is attached, share it with the recipient!
        if (attachedEvidenceId != null) {
            val casePkg = _activeCase.value
            if (casePkg != null) {
                val updatedEvList = casePkg.evidence.map { ev ->
                    if (ev.id == attachedEvidenceId) {
                        val shared = (ev.sharedWithPlayerIds + recipientPlayerId).distinct()
                        ev.copy(
                            visibility = EvidenceVisibility.SHARED,
                            sharedWithPlayerIds = shared
                        )
                    } else ev
                }
                _activeCase.value = casePkg.copy(evidence = updatedEvList)
                updateObjectiveProgress("secondary", 30)
                addDeductionNote("Shared evidence with $targetCharName: attached #${attachedEvidenceId.takeLast(6)}")
            }
        }

        return message
    }

    override fun proposeDeal(recipientPlayerId: String, title: String, terms: String): DealProposal {
        val myId = _myPlayerId.value ?: "player_local"
        val deal = DealProposal(
            id = "deal_${UUID.randomUUID().toString().take(8)}",
            proposerPlayerId = myId,
            recipientPlayerId = recipientPlayerId,
            title = title,
            terms = terms,
            status = "PENDING"
        )
        _dealProposals.value = _dealProposals.value + deal

        sendPrivateMessage(
            recipientPlayerId = recipientPlayerId,
            text = "ðŸ¤ PROPOSED DEAL: $title - $terms",
            dealDetails = terms
        )

        updateObjectiveProgress("secondary", 20)
        return deal
    }

    override fun respondToDeal(dealId: String, accept: Boolean) {
        val currentDeals = _dealProposals.value.map {
            if (it.id == dealId) {
                it.copy(status = if (accept) "ACCEPTED" else "REJECTED")
            } else it
        }
        _dealProposals.value = currentDeals

        val deal = currentDeals.find { it.id == dealId }
        if (deal != null) {
            val statusText = if (accept) "ACCEPTED" else "DECLINED"
            sendPrivateMessage(
                recipientPlayerId = deal.proposerPlayerId,
                text = "Deal [$statusText]: \"${deal.title}\"",
                dealDetails = "STATUS: $statusText"
            )
            if (accept) {
                updateObjectiveProgress("secondary", 50)
                addDeductionNote("Formed secret alliance regarding: ${deal.title}")
            }
        }
    }

    // --- MID-GAME TWIST ---

    override fun triggerMidGameTwist() {
        val twist = _activeCase.value?.midGameTwist ?: MidGameTwist(
            id = "twist_blackwood_cut",
            title = "The Midnight Power Cut & Second Cipher Note",
            eventNarration = "At 23:30, lightning struck the manor generator, plunging Blackwood Hall into pitch darkness. In the confusion, footsteps were heard rushing from the library. Butler Pendleton discovered a scorched cipher note pinned to the study door!",
            triggerHalfwayMinute = 3,
            revealedEvidence = EvidenceItem(
                id = "ev_midnight_cipher_note",
                name = "Scorched Blackwood Cipher Note",
                category = "Document",
                discoveryLocation = "Study doorframe pinned with pearl tie pin",
                discoveryPhase = "MidGameTwist",
                description = "Scorched slip bearing shorthand cipher in Lord Blackwood's hand: 'He has taken the codicil. The decanter is bitter.'",
                detailedInspection = "Matches ink from Blackwood's fountain pen. Proves Lord Blackwood realized he was poisoned before he collapsed.",
                linkedCharacterIds = listOf("char_edward_vance"),
                whatItProves = "Edward Vance took the codicil after poisoning the decanter.",
                timelineRelationship = "Written between 23:20 and 23:25.",
                isDiscovered = true
            ),
            impactDescription = "Proves Lord Blackwood was alive when the will was stolen, collapsing Edward's conservatory walking alibi."
        )

        _activeTwist.value = twist
        _isTwistDismissed.value = false
        collectEvidence(twist.revealedEvidence.id)
        addDeductionNote("âš ï¸ MID-GAME EVENT: ${twist.title}")
    }

    override fun dismissTwist() {
        _isTwistDismissed.value = true
    }

    // --- SECRET ACCUSATION VOTING & RESOLUTION ---

    override fun submitBallotVote(
        accusedCulpritId: String,
        method: String,
        motive: String,
        smokingGunEvidenceId: String?
    ) {
        _hasVoted.value = true
        addDeductionNote("Secret ballot cast against suspect #${accusedCulpritId.takeLast(6)}")
    }

    override fun tallyAndResolveCase(): GameResolutionSummary {
        val casePkg = _activeCase.value ?: createGeneratedCase(CaseGenerationConfig())
        val solution = casePkg.secretSolution ?: SecretSolution(
            culpritId = casePkg.characters.firstOrNull { it.isCulprit }?.id ?: "char_generated_culprit",
            motive = "Desperate gambling debts and impending disinheritance.",
            opportunity = "Used secret garden terrace passage to enter the study undetected.",
            method = "Laced vintage scotch decanter with concentrated poison powder.",
            hiddenTimeline = casePkg.timeline.filter { it.isHiddenSequence },
            misleadingStrategy = "Fabricated an alibi and placed false traces.",
            smokingGunEvidenceIds = listOf("ev_muddy_footprints", "ev_silver_cigarette_case", "ev_torn_will_draft"),
            contradictionExplanations = mapOf(
                "Conservatory Alibi" to "Footprints placed suspect directly at the study exterior door during the rain.",
                "Cigarette Case" to "Contained poison powder traces in hidden compartment."
            ),
            misleadingEvidenceExplanations = mapOf(
                "ev_burnt_charity_receipts" to "Burned receipts were hiding financial embezzlement, not murder."
            ),
            finalVerdictExplanation = "The culprit is convicted of the murder."
        )

        val culpritChar = casePkg.characters.find { it.id == solution.culpritId } ?: casePkg.characters.first()

        val players = _currentRoom.value?.players ?: listOf(
            PlayerInfo(playerId = "player_1", playerName = "Lead Detective", isHost = true)
        )

        val myChar = _assignedCharacter.value
        val isMeCulprit = myChar?.id == solution.culpritId

        val playerScores = players.mapIndexed { idx, p ->
            val isPlayerCulprit = if (isMeCulprit && p.playerId == _myPlayerId.value) true else idx == 0 && isMeCulprit
            val votedCorrectly = !isPlayerCulprit
            val primaryDone = true
            val secDone = true
            val score = (if (votedCorrectly) 150 else 50) + (if (primaryDone) 100 else 0) + (if (secDone) 50 else 0)
            val titles = mutableListOf<String>()
            if (votedCorrectly) titles.add("Master Sleuth")
            if (secDone) titles.add("Astute Negotiator")
            if (isPlayerCulprit) titles.add("Shadow Operator")

            PlayerScoreCard(
                playerId = p.playerId,
                playerName = p.playerName,
                characterName = if (isPlayerCulprit) culpritChar.name else "Investigator ${idx + 1}",
                isCulprit = isPlayerCulprit,
                primaryObjectiveCompleted = primaryDone,
                secondaryObjectiveCompleted = secDone,
                correctlyVotedCulprit = votedCorrectly,
                totalScore = score,
                earnedAchievements = titles
            )
        }

        val smokingGuns = casePkg.evidence.filter { it.id in solution.smokingGunEvidenceIds }

        val resolution = GameResolutionSummary(
            trueCulpritId = solution.culpritId,
            trueCulpritName = culpritChar.name,
            trueMotive = solution.motive,
            trueMethod = solution.method,
            trueTimeline = solution.hiddenTimeline,
            smokingGunEvidence = smokingGuns,
            misleadingCluesExplanation = "Lady Beatrice's burned receipts were an attempt to hide unrelated charity embezzlement, while Dr. Finch's heated argument was over prescription dosage, neither having any part in the belladonna poisoning.",
            culpritConfession = "Edward broke down when confronted with the silver cigarette case and the wet terrace clay: 'I had no choice! Monte Carlo syndicates gave me until midnight. Uncle Reginald wouldn't listen... he laughed as he signed the new will! I only meant to destroy the document, but the anger consumed me!'",
            playerScores = playerScores,
            isCulpritConvicted = true
        )

        _gameResolution.value = resolution
        transitionRoomState(RoomState.CASE_RESOLUTION)
        return resolution
    }

    // --- ACCUSATION LEGACY VERDICT EVALUATION ---

    override fun submitAccusation(
        accusedId: String,
        motiveText: String,
        smokingGunEvidenceId: String?,
        accusedSuspectId: String,
        selectedMotive: String
    ): AccusationVerdict {
        val targetId = if (accusedId.isNotBlank()) accusedId else accusedSuspectId
        val targetMotive = if (motiveText.isNotBlank()) motiveText else selectedMotive
        val casePkg = _activeCase.value ?: createGeneratedCase(CaseGenerationConfig())
        val solution = casePkg.secretSolution ?: SecretSolution(
            culpritId = casePkg.characters.firstOrNull { it.isCulprit }?.id ?: "char_generated_culprit",
            motive = targetMotive.ifBlank { "Unspecified motive" },
            opportunity = "Unspecified opportunity",
            method = "Unspecified method",
            hiddenTimeline = emptyList(),
            misleadingStrategy = "",
            smokingGunEvidenceIds = if (smokingGunEvidenceId != null) listOf(smokingGunEvidenceId) else emptyList(),
            contradictionExplanations = emptyMap(),
            misleadingEvidenceExplanations = emptyMap(),
            finalVerdictExplanation = "Investigation concluded."
        )
        val accusedSuspect = casePkg.characters.find { it.id == targetId }
        val trueCulprit = casePkg.characters.find { it.id == solution.culpritId }

        val isSuspectCorrect = targetId == solution.culpritId
        val motiveConfirmed = targetMotive.isNotBlank()
        val smokingGunConfirmed = smokingGunEvidenceId != null && smokingGunEvidenceId in solution.smokingGunEvidenceIds

        val overallCorrect = isSuspectCorrect && smokingGunConfirmed

        val explanation = if (overallCorrect) {
            "ACCUSATION CONFIRMED: ${trueCulprit?.name} is indeed the culprit! ${solution.finalVerdictExplanation}"
        } else if (isSuspectCorrect) {
            "PARTIAL DEDUCTION: You correctly identified ${trueCulprit?.name}, but lacked the decisive smoking gun evidence to secure a full conviction."
        } else {
            "WRONGFUL ACCUSATION: ${accusedSuspect?.name ?: "Unknown Suspect"} was innocent of this crime. The true killer escaped justice."
        }

        val verdictResult = AccusationVerdict(
            isCorrect = overallCorrect,
            accusedCharacterName = accusedSuspect?.name ?: "Unknown Suspect",
            trueCulpritName = trueCulprit?.name ?: "Unknown",
            motiveConfirmed = motiveConfirmed,
            smokingGunConfirmed = smokingGunConfirmed,
            finalVerdictExplanation = explanation,
            solution = solution
        )

        _verdict.value = verdictResult
        return verdictResult
    }

    // --- WEBSOCKET CLIENT ---

    fun connectWebSocket(serverBaseUrl: String) {
        val roomCode = _currentRoom.value?.roomCode ?: return
        val playerId = _myPlayerId.value ?: return
        val token = _mySessionToken.value ?: return

        val wsUrl = serverBaseUrl.replace("http://", "ws://").replace("https://", "wss://") +
                "/rooms/$roomCode/ws?playerId=$playerId&sessionToken=$token"

        val request = Request.Builder().url(wsUrl).build()
        webSocket?.close(1000, "Reconnecting")
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                handleWebSocketMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                // Graceful fallback to local in-memory synchronization
            }
        })
    }

    private fun handleWebSocketMessage(jsonText: String) {
        try {
            val json = JSONObject(jsonText)
            val event = json.optString("event").ifBlank { json.optString("type") }
            val rawPayload = json.opt("payload")

            when (event) {
                "ROOM_SNAPSHOT" -> {
                    val payloadObj = when (rawPayload) {
                        is JSONObject -> rawPayload
                        is String -> JSONObject(rawPayload)
                        else -> null
                    } ?: return

                    val roomId = payloadObj.optString("roomId")
                    val roomCode = payloadObj.optString("roomCode")
                    val hostPlayerId = payloadObj.optString("hostPlayerId")
                    val stateStr = payloadObj.optString("state")
                    val state = RoomState.values().find { it.name == stateStr } ?: RoomState.WAITING_FOR_PLAYERS
                    val playerCount = payloadObj.optInt("playerCount", 1)
                    val frozenCount = if (payloadObj.has("frozenPlayerCount") && !payloadObj.isNull("frozenPlayerCount")) payloadObj.optInt("frozenPlayerCount") else null
                    val matchDuration = payloadObj.optInt("matchDurationMinutes", 18)
                    val isChatLocked = payloadObj.optBoolean("isChatLocked", false)
                    val errorMsg = payloadObj.optString("errorMessage").takeIf { it.isNotBlank() }

                    // Parse players list
                    val playersArray = payloadObj.optJSONArray("players")
                    val playersList = mutableListOf<PlayerInfo>()
                    if (playersArray != null) {
                        for (i in 0 until playersArray.length()) {
                            val pObj = playersArray.getJSONObject(i)
                            playersList.add(
                                PlayerInfo(
                                    playerId = pObj.optString("playerId"),
                                    playerName = pObj.optString("playerName"),
                                    isHost = pObj.optBoolean("isHost"),
                                    isReady = pObj.optBoolean("isReady", true),
                                    isConnected = pObj.optBoolean("isConnected", true),
                                    assignedCharacterId = pObj.optString("assignedCharacterId").takeIf { it.isNotBlank() },
                                    assignedCharacterName = pObj.optString("assignedCharacterName").takeIf { it.isNotBlank() }
                                )
                            )
                        }
                    }

                    // Parse timerState
                    val timerObj = payloadObj.optJSONObject("timerState")
                    val timerState = if (timerObj != null) {
                        val timerPhase = RoomState.values().find { it.name == timerObj.optString("phase") } ?: state
                        GameTimerState(
                            phase = timerPhase,
                            startTimeMillis = timerObj.optLong("startTimeMillis"),
                            durationSeconds = timerObj.optInt("durationSeconds"),
                            endTimeMillis = timerObj.optLong("endTimeMillis"),
                            remainingSeconds = timerObj.optInt("remainingSeconds")
                        )
                    } else null

                    // Parse public evidence IDs
                    val evArray = payloadObj.optJSONArray("publicDiscoveredEvidenceIds")
                    val publicEvList = mutableListOf<String>()
                    if (evArray != null) {
                        for (i in 0 until evArray.length()) {
                            publicEvList.add(evArray.getString(i))
                        }
                    }

                    val updatedRoom = RoomSnapshot(
                        roomId = roomId,
                        roomCode = roomCode,
                        hostPlayerId = hostPlayerId,
                        state = state,
                        playerCount = playerCount,
                        minPlayers = 4,
                        maxPlayers = 10,
                        players = if (playersList.isNotEmpty()) playersList else (_currentRoom.value?.players ?: emptyList()),
                        frozenPlayerCount = frozenCount,
                        generationJobId = payloadObj.optString("generationJobId").takeIf { it.isNotBlank() },
                        matchDurationMinutes = matchDuration,
                        timerState = timerState,
                        publicDiscoveredEvidenceIds = publicEvList,
                        isChatLocked = isChatLocked,
                        errorMessage = errorMsg
                    )

                    _currentRoom.value = updatedRoom
                    if (timerState != null) {
                        _gameTimer.value = timerState
                    }
                    if (publicEvList.isNotEmpty()) {
                        _discoveredEvidenceIds.value = _discoveredEvidenceIds.value + publicEvList
                    }

                    // If room entered CASE_BRIEFING or later and we don't have assigned character yet, fetch it!
                    if (state in listOf(
                            RoomState.CASE_BRIEFING,
                            RoomState.CHARACTER_REVEAL,
                            RoomState.INVESTIGATION,
                            RoomState.MID_GAME_TWIST,
                            RoomState.INTERROGATION_DEBATE,
                            RoomState.VOTING,
                            RoomState.CASE_RESOLUTION
                        ) && _myAssignedCharacter.value == null
                    ) {
                        coroutineScope.launch(Dispatchers.IO) {
                            fetchAuthoritativeMyCharacter(roomCode)
                        }
                    }
                }
                "NEW_MESSAGE", "PRIVATE_MESSAGE_RECEIVED" -> {
                    val payloadObj = when (rawPayload) {
                        is JSONObject -> rawPayload
                        is String -> JSONObject(rawPayload)
                        else -> null
                    } ?: return
                    val msg = PrivateMessage(
                        id = payloadObj.optString("id", UUID.randomUUID().toString()),
                        senderPlayerId = payloadObj.optString("senderPlayerId"),
                        senderCharacterName = payloadObj.optString("senderCharacterName"),
                        recipientPlayerId = payloadObj.optString("recipientPlayerId"),
                        recipientCharacterName = payloadObj.optString("recipientCharacterName"),
                        text = payloadObj.optString("text"),
                        timestamp = payloadObj.optLong("timestamp", System.currentTimeMillis()),
                        messageType = payloadObj.optString("messageType", "CHAT"),
                        attachedEvidenceId = payloadObj.optString("attachedEvidenceId").takeIf { it.isNotBlank() },
                        dealDetails = payloadObj.optString("dealDetails").takeIf { it.isNotBlank() }
                    )
                    if (_privateMessages.value.none { it.id == msg.id }) {
                        _privateMessages.value = _privateMessages.value + msg
                    }
                }
                "DEAL_PROPOSED", "DEAL_RESPONDED" -> {
                    val payloadObj = when (rawPayload) {
                        is JSONObject -> rawPayload
                        is String -> JSONObject(rawPayload)
                        else -> null
                    } ?: return
                    val deal = DealProposal(
                        id = payloadObj.optString("id"),
                        proposerPlayerId = payloadObj.optString("proposerPlayerId"),
                        recipientPlayerId = payloadObj.optString("recipientPlayerId"),
                        title = payloadObj.optString("title"),
                        terms = payloadObj.optString("terms"),
                        status = payloadObj.optString("status", "PENDING"),
                        timestamp = payloadObj.optLong("timestamp", System.currentTimeMillis())
                    )
                    val existing = _dealProposals.value.filter { it.id != deal.id }
                    _dealProposals.value = existing + deal
                }
                "EVIDENCE_DISCOVERED" -> {
                    val evId = rawPayload?.toString() ?: ""
                    if (evId.isNotBlank()) {
                        collectEvidence(evId)
                    }
                }
                "TWIST_ALERT", "TWIST_TRIGGERED" -> {
                    triggerMidGameTwist()
                }
                "RESOLUTION" -> {
                    coroutineScope.launch(Dispatchers.IO) {
                        fetchAuthoritativeResolution(_currentRoom.value?.roomCode ?: return@launch)
                    }
                }
                "STAGE_CHANGED" -> {
                    val payloadObj = (rawPayload as? JSONObject) ?: JSONObject(rawPayload?.toString() ?: "{}")
                    val stageStr = payloadObj.optString("stage")
                    val state = RoomState.values().find { it.name == stageStr }
                    if (state != null) {
                        _currentRoom.value = _currentRoom.value?.copy(state = state)
                    }
                }
                "TIMER_SYNC" -> {
                    val payloadObj = (rawPayload as? JSONObject) ?: JSONObject(rawPayload?.toString() ?: "{}")
                    val remaining = payloadObj.optInt("remainingSeconds")
                    val duration = payloadObj.optInt("durationSeconds")
                    val phaseStr = payloadObj.optString("phase")
                    val phase = RoomState.values().find { it.name == phaseStr } ?: RoomState.INVESTIGATION
                    _gameTimer.value = GameTimerState(
                        phase = phase,
                        startTimeMillis = System.currentTimeMillis() - ((duration - remaining) * 1000L),
                        durationSeconds = duration,
                        endTimeMillis = System.currentTimeMillis() + (remaining * 1000L),
                        remainingSeconds = remaining
                    )
                }
            }
        } catch (_: Exception) {
            // Safe JSON parse ignore
        }
    }

    suspend fun fetchAuthoritativeMyCharacter(roomCode: String) {
        val playerId = _myPlayerId.value ?: return
        val token = _mySessionToken.value ?: return

        try {
            val url = "$serverBaseUrl/rooms/$roomCode/my-character"
            val request = Request.Builder()
                .url(url)
                .addHeader("X-Player-Id", playerId)
                .addHeader("X-Session-Token", token)
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return
                val json = JSONObject(body)
                val isCulprit = json.optBoolean("isCulprit", false)
                val secret = json.optString("privateSecret", "")
                val directives = json.optString("privateDirectives", "")
                val seq = json.optString("actualSequenceOfEvents", "")
                val motive = json.optString("culpritMotive", "")
                val cover = json.optString("culpritCoverStory", "")

                val charObj = json.optJSONObject("character")
                val charId = charObj?.optString("id") ?: "char_unknown"
                val charName = charObj?.optString("name") ?: "Investigator"
                val charAge = charObj?.optInt("age", 40) ?: 40
                val charOccupation = charObj?.optString("occupation") ?: "Special Agent"
                val charBio = charObj?.optString("publicBio") ?: ""
                val charPersonality = charObj?.optString("personality") ?: ""
                val charPublicMotive = charObj?.optString("publicMotive") ?: ""
                val charRel = charObj?.optString("relationshipWithVictim") ?: ""
                val charAlibi = charObj?.optString("personalAlibi") ?: ""
                val charClaimed = charObj?.optString("claimedLocation") ?: ""
                val charActual = charObj?.optString("actualActions") ?: ""

                val dossier = CharacterDossier(
                    id = charId,
                    name = charName,
                    age = charAge,
                    occupation = charOccupation,
                    publicBio = charBio,
                    personality = charPersonality,
                    publicMotive = charPublicMotive,
                    relationshipWithVictim = charRel,
                    isCulprit = isCulprit,
                    privateSecret = secret,
                    personalAlibi = charAlibi,
                    claimedLocation = charClaimed,
                    actualActions = charActual,
                    privateDirectives = directives
                )

                val primObj = json.optJSONObject("primaryObjective")?.let { p ->
                    PersonalObjective(
                        id = p.optString("id"),
                        title = p.optString("title"),
                        description = p.optString("description"),
                        category = ObjectiveCategory.values().find { it.name == p.optString("category") } ?: ObjectiveCategory.INVESTIGATION,
                        isPrimary = true,
                        isVerified = p.optBoolean("isVerified", true),
                        points = p.optInt("points", 200)
                    )
                }
                val secObj = json.optJSONObject("secondaryObjective")?.let { s ->
                    PersonalObjective(
                        id = s.optString("id"),
                        title = s.optString("title"),
                        description = s.optString("description"),
                        category = ObjectiveCategory.values().find { it.name == s.optString("category") } ?: ObjectiveCategory.SOCIAL,
                        isPrimary = false,
                        points = s.optInt("points", 100)
                    )
                }

                val caseIdObj = json.optJSONObject("caseIdentity")
                val identity = CaseIdentity(
                    caseId = caseIdObj?.optString("caseId") ?: "case_001",
                    title = caseIdObj?.optString("title") ?: "The Mystery at Blackwood Hall",
                    subtitle = caseIdObj?.optString("subtitle") ?: "",
                    setting = caseIdObj?.optString("setting") ?: "Blackwood Manor",
                    dateTime = caseIdObj?.optString("dateTime") ?: "November 6, 1928",
                    difficulty = caseIdObj?.optString("difficulty") ?: "Medium",
                    cinematicIntro = caseIdObj?.optString("cinematicIntro") ?: "",
                    incidentSummary = caseIdObj?.optString("incidentSummary") ?: "",
                    investigationObjective = caseIdObj?.optString("investigationObjective") ?: ""
                )

                val myAssigned = MyAssignedCharacter(
                    character = dossier,
                    isCulprit = isCulprit,
                    privateSecret = secret,
                    privateDirectives = directives,
                    primaryObjective = primObj,
                    secondaryObjective = secObj,
                    actualSequenceOfEvents = seq,
                    culpritMotive = motive,
                    culpritCoverStory = cover,
                    caseIdentity = identity
                )

                _myAssignedCharacter.value = myAssigned
                _assignedCharacter.value = dossier
                _primaryObjective.value = primObj
                _secondaryObjective.value = secObj
            }
        } catch (_: Exception) {}
    }

    suspend fun fetchAuthoritativeResolution(roomCode: String) {
        try {
            val url = "$serverBaseUrl/rooms/$roomCode/resolution"
            val request = Request.Builder().url(url).get().build()
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return
                val json = JSONObject(body)
                val culpritId = json.optString("trueCulpritId")
                val culpritName = json.optString("trueCulpritName")
                val motive = json.optString("trueMotive")
                val method = json.optString("trueMethod")
                val confession = json.optString("culpritConfession")
                val isConvicted = json.optBoolean("isCulpritConvicted", true)

                val scoreCards = mutableListOf<PlayerScoreCard>()
                val scoresArray = json.optJSONArray("playerScores")
                if (scoresArray != null) {
                    for (i in 0 until scoresArray.length()) {
                        val s = scoresArray.getJSONObject(i)
                        val achList = mutableListOf<String>()
                        val achArray = s.optJSONArray("earnedAchievements")
                        if (achArray != null) {
                            for (j in 0 until achArray.length()) achList.add(achArray.getString(j))
                        }
                        scoreCards.add(
                            PlayerScoreCard(
                                playerId = s.optString("playerId"),
                                playerName = s.optString("playerName"),
                                characterName = s.optString("characterName"),
                                isCulprit = s.optBoolean("isCulprit"),
                                primaryObjectiveCompleted = s.optBoolean("primaryObjectiveCompleted"),
                                secondaryObjectiveCompleted = s.optBoolean("secondaryObjectiveCompleted"),
                                correctlyVotedCulprit = s.optBoolean("correctlyVotedCulprit"),
                                totalScore = s.optInt("totalScore"),
                                earnedAchievements = achList
                            )
                        )
                    }
                }

                val resolution = GameResolutionSummary(
                    trueCulpritId = culpritId,
                    trueCulpritName = culpritName,
                    trueMotive = motive,
                    trueMethod = method,
                    trueTimeline = _activeCase.value?.timeline ?: emptyList(),
                    smokingGunEvidence = _activeCase.value?.evidence?.filter { it.id == "ev_muddy_footprints" } ?: emptyList(),
                    misleadingCluesExplanation = "The poisoned decanter was planted to disguise the true sequence.",
                    culpritConfession = confession,
                    playerScores = scoreCards,
                    isCulpritConvicted = isConvicted
                )
                _gameResolution.value = resolution
            }
        } catch (_: Exception) {}
    }


    // --- CASE 001 GENERATION WITH 5 LOCATIONS & TWIST ---

    fun createGeneratedCase(config: CaseGenerationConfig): CasePackage {
        val exactCount = config.playerCount.coerceIn(4, 10)
        val caseId = "case_${UUID.randomUUID().toString().take(8)}"

        // Build suspect pool from grounded character templates (no case001 dependency)
        val allPool = buildDefaultSuspectPool()
        val culprit = allPool.first { it.isCulprit }
        val innocents = allPool.filter { !it.isCulprit }.shuffled(Random(System.currentTimeMillis()))

        val humanCast = mutableListOf(culprit)
        humanCast.addAll(innocents.take(exactCount - 1))

        val themeWord = if (config.theme.isNotBlank()) {
            config.theme.substringBefore(" ")
        } else "Blackwood"
        val title = if (config.customPrompt.isNotBlank()) {
            "The Mystery of ${config.customPrompt.take(24)}"
        } else {
            "The ${themeWord} Enigma"
        }

        val locations = buildDefaultLocations()
        val evidence = buildDefaultEvidence()
        val crimeScene = CrimeSceneData(
            roomName = "The Study & Private Annexes",
            description = "A quiet room with desk and papers where the incident occurred.",
            sceneAssetId = null,
            hotspots = locations.firstOrNull()?.hotspots ?: emptyList()
        )
        val secretSolution = SecretSolution(
            culpritId = culprit.id,
            motive = "Desperate financial motives and impending disinheritance.",
            opportunity = "Used secret passage at 23:15 to enter undetected.",
            method = "Laced beverage with concentrated poison powder.",
            hiddenTimeline = emptyList(),
            misleadingStrategy = "Fabricated an alibi and placed false traces.",
            smokingGunEvidenceIds = listOf("ev_muddy_footprints", "ev_silver_cigarette_case", "ev_torn_will_draft"),
            contradictionExplanations = mapOf(
                "Conservatory Alibi" to "Footprints placed suspect directly at the scene."
            ),
            misleadingEvidenceExplanations = mapOf(
                "ev_burnt_charity_receipts" to "Burned receipts were hiding embezzlement, not murder."
            ),
            finalVerdictExplanation = "${culprit.name} is convicted of the murder."
        )

        return CasePackage(
            identity = CaseIdentity(
                caseId = caseId,
                title = title,
                subtitle = "${config.atmosphere} • Designed for $exactCount Human Investigators • ${config.difficulty}",
                classification = "CASE FILE - $exactCount PLAYERS",
                setting = "Blackwood Manor",
                dateTime = "1928-10-24 23:45",
                difficulty = config.difficulty,
                estimatedDurationMinutes = "15-20 min",
                cinematicIntro = "A mysterious incident has occurred requiring investigation.",
                incidentSummary = "Investigate the scene and uncover contradictions in suspect testimonies.",
                investigationObjective = "Identify the culprit, discover key evidence, and cast the decisive ballot.",
                posterAssetId = "img_case_poster"
            ),
            victim = VictimProfile(
                name = "Lord Reginald Blackwood",
                age = 62,
                occupation = "Patriarch & Industrialist",
                background = "Magnate of Blackwood Mining, rumored to be rewriting his will.",
                causeOfDeath = "Poisoning by rare alkaloid in vintage scotch",
                locationFound = "Private library study, seated at mahogany desk"
            ),
            characters = humanCast,
            evidence = evidence,
            timeline = emptyList(),
            crimeScene = crimeScene,
            locations = locations,
            midGameTwist = MidGameTwist(
                id = "twist_generated_${UUID.randomUUID().toString().take(6)}",
                title = "Generated Mystery Twist",
                eventNarration = "A new twist emerges mid-investigation, revealing hidden connections among the suspects.",
                triggerHalfwayMinute = 3,
                revealedEvidence = EvidenceItem(
                    id = "ev_generated_twist",
                    name = "Generated Twist Evidence",
                    category = "Document",
                    discoveryLocation = "Mid-game discovery",
                    discoveryPhase = "MidGameTwist",
                    description = "New evidence uncovered during investigation.",
                    detailedInspection = "Important forensic details linking suspects.",
                    linkedCharacterIds = humanCast.map { it.id },
                    whatItProves = "Proves crucial timeline contradictions.",
                    timelineRelationship = "Found at 23:30.",
                    isDiscovered = false
                ),
                impactDescription = "Reorients the timeline and refutes key alibis."
            ),
            secretSolution = secretSolution
        )
    }

    /** Builds a default suspect pool with 6 characters - one culprit and 5 innocents. */
    private fun buildDefaultSuspectPool(): List<CharacterDossier> {
        val culprit = CharacterDossier(
            id = "char_edward_vance",
            name = "Edward Vance",
            age = 38,
            occupation = "Suspect",
            publicBio = "A suspect in the current investigation.",
            personality = "Variable, depends on generated narrative",
            publicMotive = "Motive varies by generated scenario",
            relationshipWithVictim = "Involved in the case",
            isCulprit = true,
            privateSecret = "",
            personalAlibi = "",
            claimedLocation = "",
            actualActions = "",
            personalObjective = "",
            privateDirectives = "",
            interrogationResponses = emptyMap()
        )
        val innocents = listOf(
            CharacterDossier(
                id = "char_generated_1",
                name = "Generated Suspect 1",
                age = 30,
                occupation = "Suspect",
                publicBio = "A suspect in the current investigation.",
                personality = "Variable, depends on generated narrative",
                publicMotive = "Motive varies by generated scenario",
                relationshipWithVictim = "Involved in the case",
                isCulprit = false,
                privateSecret = "",
                personalAlibi = "",
                claimedLocation = "",
                actualActions = "",
                personalObjective = "",
                privateDirectives = "",
                interrogationResponses = emptyMap()
            ),
            CharacterDossier(
                id = "char_generated_2",
                name = "Generated Suspect 2",
                age = 42,
                occupation = "Suspect",
                publicBio = "A suspect in the current investigation.",
                personality = "Variable, depends on generated narrative",
                publicMotive = "Motive varies by generated scenario",
                relationshipWithVictim = "Involved in the case",
                isCulprit = false,
                privateSecret = "",
                personalAlibi = "",
                claimedLocation = "",
                actualActions = "",
                personalObjective = "",
                privateDirectives = "",
                interrogationResponses = emptyMap()
            ),
            CharacterDossier(
                id = "char_generated_3",
                name = "Generated Suspect 3",
                age = 50,
                occupation = "Suspect",
                publicBio = "A suspect in the current investigation.",
                personality = "Variable, depends on generated narrative",
                publicMotive = "Motive varies by generated scenario",
                relationshipWithVictim = "Involved in the case",
                isCulprit = false,
                privateSecret = "",
                personalAlibi = "",
                claimedLocation = "",
                actualActions = "",
                personalObjective = "",
                privateDirectives = "",
                interrogationResponses = emptyMap()
            ),
            CharacterDossier(
                id = "char_generated_4",
                name = "Generated Suspect 4",
                age = 28,
                occupation = "Suspect",
                publicBio = "A suspect in the current investigation.",
                personality = "Variable, depends on generated narrative",
                publicMotive = "Motive varies by generated scenario",
                relationshipWithVictim = "Involved in the case",
                isCulprit = false,
                privateSecret = "",
                personalAlibi = "",
                claimedLocation = "",
                actualActions = "",
                personalObjective = "",
                privateDirectives = "",
                interrogationResponses = emptyMap()
            ),
            CharacterDossier(
                id = "char_generated_5",
                name = "Generated Suspect 5",
                age = 45,
                occupation = "Suspect",
                publicBio = "A suspect in the current investigation.",
                personality = "Variable, depends on generated narrative",
                publicMotive = "Motive varies by generated scenario",
                relationshipWithVictim = "Involved in the case",
                isCulprit = false,
                privateSecret = "",
                personalAlibi = "",
                claimedLocation = "",
                actualActions = "",
                personalObjective = "",
                privateDirectives = "",
                interrogationResponses = emptyMap()
            )
        )
        return mutableListOf(culprit) + innocents
    }

    /** Builds a default set of 5 locations for a generated case. */
    private fun buildDefaultLocations(): List<InvestigationLocation> {
        return listOf(
            InvestigationLocation(
                id = "loc_study",
                name = "Study",
                description = "A quiet room with desk and papers.",
                atmosphereSnippet = "A quiet room with desk and papers.",
                isPrimaryCrimeScene = true,
                hotspots = listOf(
                    SceneHotspot("hs_desk", "Study Writing Desk", 0.35f, 0.65f, 0.10f, "Writing desk where residue was noted.", "ev_poisoned_decanter")
                )
            ),
            InvestigationLocation(
                id = "loc_dining",
                name = "Dining Room",
                description = "Formal dining room table.",
                atmosphereSnippet = "Formal dining table set for dinner.",
                isPrimaryCrimeScene = false,
                hotspots = emptyList()
            ),
            InvestigationLocation(
                id = "loc_terrace",
                name = "Terrace",
                description = "Outside terrace balcony.",
                atmosphereSnippet = "Outside balcony with garden view.",
                isPrimaryCrimeScene = false,
                hotspots = listOf(
                    SceneHotspot("hs_terrace", "Terrace Doors", 0.20f, 0.45f, 0.09f, "Wet red clay prints on the threshold.", "ev_muddy_footprints")
                )
            ),
            InvestigationLocation(
                id = "loc_billiards",
                name = "Billiards Room",
                description = "Recreation room.",
                atmosphereSnippet = "Pool table and seating area.",
                isPrimaryCrimeScene = false,
                hotspots = emptyList()
            ),
            InvestigationLocation(
                id = "loc_drawing",
                name = "Drawing Room",
                description = "Sitting room with fireplace.",
                atmosphereSnippet = "Decorative sitting room with fireplace.",
                isPrimaryCrimeScene = false,
                hotspots = emptyList()
            )
        )
    }

    /** Builds a default set of evidence items for a generated case. */
    private fun buildDefaultEvidence(): List<EvidenceItem> {
        return listOf(
            EvidenceItem(
                id = "ev_poisoned_decanter",
                name = "Cut-Crystal Scotch Decanter",
                category = "WeaponTrace",
                discoveryLocation = "Study writing desk",
                discoveryPhase = "Initial",
                description = "Antique crystal decanter containing vintage Glenmorangie, tainted with white powdery residue.",
                detailedInspection = "White residue coats the rim of the stopper.",
                whatItProves = "Poison was delivered via the decanter.",
                timelineRelationship = "Placed before 23:15."
            ),
            EvidenceItem(
                id = "ev_muddy_footprints",
                name = "Muddy Footprints",
                category = "PhysicalTrace",
                discoveryLocation = "Study terrace",
                discoveryPhase = "Initial",
                description = "Red clay mud near terrace doors.",
                detailedInspection = "Distinctive brogue print matching size 10 shoes.",
                whatItProves = "Someone crossed the wet terrace into the study.",
                timelineRelationship = "Left between 23:10 and 23:35."
            ),
            EvidenceItem(
                id = "ev_torn_will_draft",
                name = "Torn Will Draft",
                category = "Document",
                discoveryLocation = "Study desk drawer",
                discoveryPhase = "Initial",
                description = "A portion of a revised will document.",
                detailedInspection = "Shows revoking of earlier bequests.",
                whatItProves = "Provides strong motive for the crime.",
                timelineRelationship = "Drafted at 19:00."
            ),
            EvidenceItem(
                id = "ev_silver_cigarette_case",
                name = "Silver Cigarette Case",
                category = "PersonalItem",
                discoveryLocation = "Guest pocket",
                discoveryPhase = "Initial",
                description = "Engraved silver cigarette case with initials.",
                detailedInspection = "Contains a secret compartment with traces of white powder.",
                whatItProves = "Connects the bearer directly to the poison.",
                timelineRelationship = "Dropped near the terrace steps."
            ),
            EvidenceItem(
                id = "ev_burnt_charity_receipts",
                name = "Burnt Charity Receipts",
                category = "Document",
                discoveryLocation = "Drawing room hearth",
                discoveryPhase = "Initial",
                description = "Charred receipts from estate charity files.",
                detailedInspection = "Shows unauthorized fund transfers.",
                whatItProves = "Explains suspicious financial movements.",
                timelineRelationship = "Burned at 22:30."
            )
        )
    }

    /** Returns a default suspect pool replacing the case001 dependency. */
    private fun getAllSuspectPool(): List<CharacterDossier> {
        return buildDefaultSuspectPool()
    }
}