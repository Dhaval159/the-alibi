package com.example.data

import com.example.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import org.json.JSONObject
import java.util.UUID
import kotlin.random.Random

class TheAlibiRepository {

    // Room and multiplayer state
    private val _currentRoom = MutableStateFlow<RoomSnapshot?>(null)
    val currentRoom: StateFlow<RoomSnapshot?> = _currentRoom.asStateFlow()

    private val _myPlayerId = MutableStateFlow<String?>(null)
    val myPlayerId: StateFlow<String?> = _myPlayerId.asStateFlow()

    private val _mySessionToken = MutableStateFlow<String?>(null)
    val mySessionToken: StateFlow<String?> = _mySessionToken.asStateFlow()

    private val _isHost = MutableStateFlow(false)
    val isHost: StateFlow<Boolean> = _isHost.asStateFlow()

    private val _myAssignedCharacter = MutableStateFlow<MyAssignedCharacter?>(null)
    val myAssignedCharacter: StateFlow<MyAssignedCharacter?> = _myAssignedCharacter.asStateFlow()

    // Active cases & gameplay state
    private val _savedCases = MutableStateFlow<List<CasePackage>>(emptyList())
    val savedCases: StateFlow<List<CasePackage>> = _savedCases.asStateFlow()

    private val _activeCase = MutableStateFlow<CasePackage?>(null)
    val activeCase: StateFlow<CasePackage?> = _activeCase.asStateFlow()

    private val _assignedCharacter = MutableStateFlow<CharacterDossier?>(null)
    val assignedCharacter: StateFlow<CharacterDossier?> = _assignedCharacter.asStateFlow()

    private val _isDossierConcealed = MutableStateFlow(false)
    val isDossierConcealed: StateFlow<Boolean> = _isDossierConcealed.asStateFlow()

    private val _activeLocationId = MutableStateFlow<String?>("loc_study")
    val activeLocationId: StateFlow<String?> = _activeLocationId.asStateFlow()

    private val _primaryObjective = MutableStateFlow<PersonalObjective?>(null)
    val primaryObjective: StateFlow<PersonalObjective?> = _primaryObjective.asStateFlow()

    private val _secondaryObjective = MutableStateFlow<PersonalObjective?>(null)
    val secondaryObjective: StateFlow<PersonalObjective?> = _secondaryObjective.asStateFlow()

    private val _privateMessages = MutableStateFlow<List<PrivateMessage>>(emptyList())
    val privateMessages: StateFlow<List<PrivateMessage>> = _privateMessages.asStateFlow()

    private val _dealProposals = MutableStateFlow<List<DealProposal>>(emptyList())
    val dealProposals: StateFlow<List<DealProposal>> = _dealProposals.asStateFlow()

    private val _activeTwist = MutableStateFlow<MidGameTwist?>(null)
    val activeTwist: StateFlow<MidGameTwist?> = _activeTwist.asStateFlow()

    private val _isTwistDismissed = MutableStateFlow(false)
    val isTwistDismissed: StateFlow<Boolean> = _isTwistDismissed.asStateFlow()

    private val _gameTimer = MutableStateFlow<GameTimerState?>(null)
    val gameTimer: StateFlow<GameTimerState?> = _gameTimer.asStateFlow()

    private val _hasVoted = MutableStateFlow(false)
    val hasVoted: StateFlow<Boolean> = _hasVoted.asStateFlow()

    private val _gameResolution = MutableStateFlow<GameResolutionSummary?>(null)
    val gameResolution: StateFlow<GameResolutionSummary?> = _gameResolution.asStateFlow()

    private val _discoveredEvidenceIds = MutableStateFlow<Set<String>>(emptySet())
    val discoveredEvidenceIds: StateFlow<Set<String>> = _discoveredEvidenceIds.asStateFlow()

    private val _inspectedHotspotIds = MutableStateFlow<Set<String>>(emptySet())
    val inspectedHotspotIds: StateFlow<Set<String>> = _inspectedHotspotIds.asStateFlow()

    private val _suspiciousCharacterIds = MutableStateFlow<Set<String>>(emptySet())
    val suspiciousCharacterIds: StateFlow<Set<String>> = _suspiciousCharacterIds.asStateFlow()

    private val _boardConnections = MutableStateFlow<List<BoardConnection>>(emptyList())
    val boardConnections: StateFlow<List<BoardConnection>> = _boardConnections.asStateFlow()

    private val _deductionNotes = MutableStateFlow<List<DeductionNote>>(emptyList())
    val deductionNotes: StateFlow<List<DeductionNote>> = _deductionNotes.asStateFlow()

    private val _interrogationHistory = MutableStateFlow<Map<String, List<InterrogationQnA>>>(emptyMap())
    val interrogationHistory: StateFlow<Map<String, List<InterrogationQnA>>> = _interrogationHistory.asStateFlow()

    private val _verdict = MutableStateFlow<AccusationVerdict?>(null)
    val verdict: StateFlow<AccusationVerdict?> = _verdict.asStateFlow()

    // In-memory active rooms store for local multiplayer / test mode
    private val activeRooms = mutableMapOf<String, RoomSnapshot>()
    private var internalFrozenRoster: List<PlayerInfo>? = null
    private var internalCharacterAssignments: Map<String, String>? = null

    // OkHttp WebSocket client
    private val okHttpClient = OkHttpClient()
    private var webSocket: WebSocket? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    var serverBaseUrl: String = "http://10.0.2.2:8080"

    init {
        val case001 = createCase001()
        _savedCases.value = listOf(case001)
        selectCase(case001, "char_edward_vance")
    }

    // --- ROOM CREATION & LOBBY MANAGEMENT ---

    fun createRoom(params: CreateRoomParams): RoomSnapshot {
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

        activeRooms[roomCode] = room
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

    fun joinRoom(params: JoinRoomParams): Result<RoomSnapshot> {
        val code = params.roomCode.trim()
        val existing = activeRooms[code]
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

        activeRooms[code] = updated
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

    fun addMockInvestigator(name: String): Result<RoomSnapshot> {
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

        activeRooms[current.roomCode] = updated
        _currentRoom.value = updated
        return Result.success(updated)
    }

    fun removeMockInvestigator(playerId: String): Result<RoomSnapshot> {
        val current = _currentRoom.value ?: return Result.failure(IllegalStateException("No active room"))
        if (current.state != RoomState.WAITING_FOR_PLAYERS) {
            return Result.failure(IllegalStateException("Roster is locked."))
        }

        val updatedPlayers = current.players.filter { it.playerId != playerId || it.isHost }
        val updated = current.copy(
            players = updatedPlayers,
            playerCount = updatedPlayers.size
        )
        activeRooms[current.roomCode] = updated
        _currentRoom.value = updated
        return Result.success(updated)
    }

    fun togglePlayerReady(isReady: Boolean) {
        val current = _currentRoom.value ?: return
        val myId = _myPlayerId.value ?: return
        val updatedPlayers = current.players.map {
            if (it.playerId == myId) it.copy(isReady = isReady) else it
        }
        val updated = current.copy(players = updatedPlayers)
        activeRooms[current.roomCode] = updated
        _currentRoom.value = updated
    }

    suspend fun generateCase(
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

    suspend fun generateRoomMystery(
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
        val briefingRoom = _currentRoom.value?.copy(
            state = RoomState.CASE_BRIEFING,
            generationProgressPercent = 100,
            generationStage = "Dossier compiled! Ready for investigation.",
            caseIdentity = casePackage.identity
        )
        _currentRoom.value = briefingRoom
        activeRooms[current.roomCode] = briefingRoom!!

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

    fun transitionRoomState(newState: RoomState) {
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
        activeRooms[current.roomCode] = updated

        if (newState == RoomState.MID_GAME_TWIST) {
            triggerMidGameTwist()
        }
    }

    fun leaveRoom() {
        val current = _currentRoom.value ?: return
        val myId = _myPlayerId.value
        if (myId != null) {
            val updatedPlayers = current.players.filter { it.playerId != myId }
            activeRooms[current.roomCode] = current.copy(players = updatedPlayers, playerCount = updatedPlayers.size)
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

    fun selectCase(casePkg: CasePackage, assignedCharId: String? = null) {
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

    fun deleteCase(caseId: String) {
        _savedCases.value = _savedCases.value.filter { it.identity.caseId != caseId }
    }

    fun setActiveLocation(locationId: String) {
        _activeLocationId.value = locationId
        val loc = _activeCase.value?.locations?.find { it.id == locationId }
        if (loc != null) {
            addDeductionNote("Moved to ${loc.name}: ${loc.atmosphereSnippet}")
        }
    }

    fun toggleConcealDossier() {
        _isDossierConcealed.value = !_isDossierConcealed.value
    }

    fun inspectHotspot(hotspotId: String) {
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

    fun collectEvidence(evidenceId: String) {
        _discoveredEvidenceIds.value = _discoveredEvidenceIds.value + evidenceId
        val ev = _activeCase.value?.evidence?.find { it.id == evidenceId }
        if (ev != null) {
            addDeductionNote("Discovered clue: ${ev.name} at ${ev.discoveryLocation}.")
        }
    }

    fun toggleSuspectSuspicion(characterId: String) {
        val current = _suspiciousCharacterIds.value
        _suspiciousCharacterIds.value = if (characterId in current) {
            current - characterId
        } else {
            current + characterId
        }
    }

    fun askInterrogationQuestion(characterId: String, qna: InterrogationQnA) {
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

    fun addBoardConnection(suspectId: String, evidenceId: String, label: String) {
        val conn = BoardConnection(
            id = "conn_${UUID.randomUUID().toString().take(6)}",
            suspectId = suspectId,
            evidenceId = evidenceId,
            label = label
        )
        _boardConnections.value = _boardConnections.value + conn
        updateObjectiveProgress("primary", 10)
    }

    fun removeBoardConnection(connectionId: String) {
        _boardConnections.value = _boardConnections.value.filter { it.id != connectionId }
    }

    fun addDeductionNote(text: String) {
        val note = DeductionNote(
            id = "note_${UUID.randomUUID().toString().take(6)}",
            text = text,
            timestamp = "23:${Random.nextInt(10, 59)}"
        )
        _deductionNotes.value = _deductionNotes.value + note
    }

    fun updateObjectiveProgress(objectiveType: String, delta: Int) {
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

    fun sendPrivateMessage(
        recipientPlayerId: String,
        text: String,
        attachedEvidenceId: String? = null,
        dealDetails: String? = null
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

    fun proposeDeal(recipientPlayerId: String, title: String, terms: String): DealProposal {
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
            text = "🤝 PROPOSED DEAL: $title - $terms",
            dealDetails = terms
        )

        updateObjectiveProgress("secondary", 20)
        return deal
    }

    fun respondToDeal(dealId: String, accept: Boolean) {
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

    fun triggerMidGameTwist() {
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
        addDeductionNote("⚠️ MID-GAME EVENT: ${twist.title}")
    }

    fun dismissTwist() {
        _isTwistDismissed.value = true
    }

    // --- SECRET ACCUSATION VOTING & RESOLUTION ---

    fun submitBallotVote(
        accusedCulpritId: String,
        method: String,
        motive: String,
        smokingGunEvidenceId: String? = null
    ) {
        _hasVoted.value = true
        addDeductionNote("Secret ballot cast against suspect #${accusedCulpritId.takeLast(6)}")
    }

    fun tallyAndResolveCase(): GameResolutionSummary {
        val casePkg = _activeCase.value ?: createCase001()
        val solution = casePkg.secretSolution ?: SecretSolution(
            culpritId = "char_edward_vance",
            motive = "Desperate gambling debts of 40,000 pounds due at midnight, impending disinheritance in favor of charity.",
            opportunity = "Used secret garden terrace passage at 23:15 to enter the study undetected.",
            method = "Laced vintage scotch decanter with concentrated belladonna powder.",
            hiddenTimeline = casePkg.timeline.filter { it.isHiddenSequence },
            misleadingStrategy = "Framed Dr. Finch by using poison and claimed to be strolling in the conservatory.",
            smokingGunEvidenceIds = listOf("ev_muddy_footprints", "ev_silver_cigarette_case", "ev_torn_will_draft"),
            contradictionExplanations = mapOf(
                "Conservatory Alibi" to "Terrace clay brogue prints placed him directly at the study exterior door during the rain.",
                "Cigarette Case" to "Contained belladonna powder traces in hidden compartment."
            ),
            misleadingEvidenceExplanations = mapOf(
                "ev_burnt_charity_receipts" to "Lady Beatrice burned receipts to hide charity embezzlement, not murder."
            ),
            finalVerdictExplanation = "Edward Vance is convicted of the murder of Lord Reginald Blackwood."
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

    fun submitAccusation(
        accusedId: String = "",
        motiveText: String = "",
        smokingGunEvidenceId: String? = null,
        accusedSuspectId: String = accusedId,
        selectedMotive: String = motiveText
    ): AccusationVerdict {
        val targetId = if (accusedId.isNotBlank()) accusedId else accusedSuspectId
        val targetMotive = if (motiveText.isNotBlank()) motiveText else selectedMotive
        val casePkg = _activeCase.value ?: createCase001()
        val solution = casePkg.secretSolution ?: createCase001().secretSolution!!
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
            "WRONGFUL ACCUSATION: ${accusedSuspect?.name} was innocent of this crime. The true killer escaped justice."
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
        val baseCase = createCase001()
        val exactCount = config.playerCount.coerceIn(4, 10)
        val caseId = "case_${UUID.randomUUID().toString().take(8)}"

        val allPool = getAllSuspectPool()
        val culprit = allPool.first { it.isCulprit }
        val innocents = allPool.filter { !it.isCulprit }.shuffled(Random(System.currentTimeMillis()))

        val humanCast = mutableListOf(culprit)
        humanCast.addAll(innocents.take(exactCount - 1))

        val title = if (config.customPrompt.isNotBlank()) {
            "The Mystery of ${config.customPrompt.take(24)}"
        } else {
            "The ${config.theme.substringBefore(" ")} Enigma"
        }

        return baseCase.copy(
            identity = baseCase.identity.copy(
                caseId = caseId,
                title = title,
                subtitle = "${config.atmosphere} • Designed for $exactCount Human Investigators • ${config.difficulty}",
                classification = "CASE FILE - $exactCount PLAYERS",
                difficulty = config.difficulty
            ),
            characters = humanCast
        )
    }

    private fun getAllSuspectPool(): List<CharacterDossier> {
        val base = createCase001().characters.toMutableList()
        if (base.none { it.id == "char_inspector_kane" }) {
            base.add(
                CharacterDossier(
                    id = "char_inspector_kane",
                    name = "Inspector Thomas Kane",
                    age = 51,
                    occupation = "Scotland Yard Investigator",
                    publicBio = "Off-duty London detective visiting Lord Blackwood regarding security at the estate.",
                    personality = "Methodical, cynical, authoritative",
                    publicMotive = "Threatened with suspension over London case records.",
                    relationshipWithVictim = "Official acquaintance",
                    isCulprit = false,
                    privateSecret = "Suspended from active duty in London pending internal inquiry.",
                    personalAlibi = "Examining estate perimeter gates.",
                    claimedLocation = "Main Foyer",
                    actualActions = "Inspected grounds with flashlight at 23:20."
                )
            )
        }
        if (base.none { it.id == "char_marcus_greene" }) {
            base.add(
                CharacterDossier(
                    id = "char_marcus_greene",
                    name = "Marcus Greene",
                    age = 28,
                    occupation = "Estate Chauffeur & Mechanic",
                    publicBio = "Maintains the motor fleet and generator for Blackwood Hall.",
                    personality = "Blunt, watchful, discreet",
                    publicMotive = "Owed substantial gambling debts by Edward Vance.",
                    relationshipWithVictim = "Employee",
                    isCulprit = false,
                    privateSecret = "Prepared Edward's motorcar for an unannounced midnight escape.",
                    personalAlibi = "Tinkering in the carriage house workshop.",
                    claimedLocation = "Carriage House",
                    actualActions = "Heard Edward's engine turn over briefly at 23:40."
                )
            )
        }
        if (base.none { it.id == "char_evelyn_reed" }) {
            base.add(
                CharacterDossier(
                    id = "char_evelyn_reed",
                    name = "Evelyn Reed",
                    age = 27,
                    occupation = "Estranged Goddaughter & Painter",
                    publicBio = "Talented bohemian painter commissioned for Lord Blackwood's official portrait.",
                    personality = "Quiet, observing, perceptive",
                    publicMotive = "Disinherited after refusing Lord Blackwood's arranged suitor.",
                    relationshipWithVictim = "Goddaughter and artist",
                    isCulprit = false,
                    privateSecret = "Secretly sketched Edward Vance pacing on the terrace at 23:18 holding a vial.",
                    personalAlibi = "In the second-floor studio cleaning paintbrushes.",
                    claimedLocation = "Art Studio",
                    actualActions = "Watched garden terrace from studio window during the storm."
                )
            )
        }
        if (base.none { it.id == "char_prof_sterling" }) {
            base.add(
                CharacterDossier(
                    id = "char_prof_sterling",
                    name = "Professor Alistair Sterling",
                    age = 58,
                    occupation = "Cambridge Antiquities Historian",
                    publicBio = "Fellow scholar cataloging Lord Blackwood's Renaissance manuscripts.",
                    personality = "Pedantic, eccentric, obsessive collector",
                    publicMotive = "Accused by Blackwood of smuggling estate artifacts.",
                    relationshipWithVictim = "Colleague and guest",
                    isCulprit = false,
                    privateSecret = "Discovered an ancient botanical codex detailing belladonna in the library.",
                    personalAlibi = "Deciphering Latin manuscripts in the west reading nook.",
                    claimedLocation = "West Reading Nook",
                    actualActions = "Saw Edward near the decanter table earlier in the evening."
                )
            )
        }
        return base
    }

    private fun createCase001(): CasePackage {
        val characters = listOf(
            CharacterDossier(
                id = "char_edward_vance",
                name = "Edward Vance",
                age = 38,
                occupation = "Estranged Nephew & Antiquities Dealer",
                publicBio = "Nephew of Lord Blackwood who recently returned from Europe under heavy gambling debts.",
                personality = "Charming, nervous when pressed, meticulous dresser",
                publicMotive = "Stands to inherit Blackwood Manor under the original will, but was being disinherited.",
                relationshipWithVictim = "Nephew and debtor",
                isCulprit = true,
                privateSecret = "Secretly snuck into the study at 23:15 to destroy the revised will and substituted the poisoned decanter.",
                personalAlibi = "Claims he was walking alone in the south conservatory smoking a cigar.",
                claimedLocation = "South Conservatory",
                actualActions = "Entered study via secret garden corridor, laced Lord Blackwood's vintage scotch with belladonna, took the new will draft.",
                personalObjective = "Conceal the stolen will draft and shift suspicion onto Dr. Finch or Beatrice.",
                privateDirectives = "CULPRIT DIRECTIVE: Keep your cool. Maintain that you were smoking in the conservatory. If confronted with terrace mud, claim you briefly stepped out to listen to the storm.",
                interrogationResponses = mapOf(
                    "Motive" to listOf(
                        InterrogationQnA(
                            question = "Why did you return to Blackwood Manor so suddenly?",
                            answer = "My uncle summoned me to discuss our antiquities investments. We had our disagreements, but I held him in the highest regard."
                        )
                    ),
                    "Opportunity" to listOf(
                        InterrogationQnA(
                            question = "Where were you between 23:00 and 23:45?",
                            answer = "I was strolling through the conservatory. The night air calms my nerves. I met nobody."
                        )
                    ),
                    "Evidence confrontation" to listOf(
                        InterrogationQnA(
                            question = "We found red clay from the study terrace on your shoes, Mr. Vance.",
                            answer = "Ah... well, I might have stepped near the terrace doors briefly when listening to the storm, but I never crossed the threshold!",
                            requiredEvidenceToTrigger = "ev_muddy_footprints",
                            revealsContradiction = true
                        )
                    )
                )
            ),
            CharacterDossier(
                id = "char_beatrice_thorne",
                name = "Lady Beatrice Thorne",
                age = 54,
                occupation = "Sister-in-Law & Manor Administrator",
                publicBio = "Oversees the estate affairs and charitable foundations funded by Lord Blackwood.",
                personality = "Stern, regal, uncompromising",
                publicMotive = "Resented Reginald's financial audits into the charity accounts.",
                relationshipWithVictim = "Sister-in-law",
                isCulprit = false,
                privateSecret = "Had embezzled 10,000 pounds from the estate trust to pay her brother's debts.",
                personalAlibi = "Reviewing estate ledgers in the drawing room.",
                claimedLocation = "Drawing Room",
                actualActions = "Burned accounting receipts in the drawing room hearth at 22:30.",
                personalObjective = "Protect the charity ledgers and clear her name of murder.",
                interrogationResponses = mapOf(
                    "Motive" to listOf(
                        InterrogationQnA(
                            question = "What were you burning in the drawing room fireplace?",
                            answer = "Private personal letters from family. Nothing pertaining to Reginald's affairs."
                        )
                    ),
                    "Timeline" to listOf(
                        InterrogationQnA(
                            question = "Did you see anyone in the corridors around 23:00?",
                            answer = "I saw Edward pacing anxiously near the south terrace doors shortly after eleven."
                        )
                    )
                )
            ),
            CharacterDossier(
                id = "char_dr_julian_finch",
                name = "Dr. Julian Finch",
                age = 49,
                occupation = "Personal Physician",
                publicBio = "Longtime family doctor who managed Lord Blackwood's failing cardiovascular health.",
                personality = "Analytical, guarded, defensive about medical records",
                publicMotive = "Lord Blackwood threatened to report medical malpractice regarding experimental treatments.",
                relationshipWithVictim = "Physician and confidant",
                isCulprit = false,
                privateSecret = "Prescribed heavy digitalis to Blackwood, fearing an inquest into previous dosages.",
                personalAlibi = "In the guest quarters reading medical journals.",
                claimedLocation = "Guest Wing Room 4",
                actualActions = "Visited study at 21:30 to plead with Blackwood, left peacefully.",
                personalObjective = "Defend medical reputation and discover the poison used.",
                interrogationResponses = mapOf(
                    "Motive" to listOf(
                        InterrogationQnA(
                            question = "Were you arguing with Lord Blackwood earlier this evening?",
                            answer = "We had a sharp disagreement regarding his medication regiment, nothing more. A physician must advise his patient."
                        )
                    ),
                    "Evidence confrontation" to listOf(
                        InterrogationQnA(
                            question = "What do you make of the white powder in the scotch decanter?",
                            answer = "It is concentrated belladonna alkaloid. Fast-acting and fatal. I do not carry pure belladonna in my field bag."
                        )
                    )
                )
            ),
            CharacterDossier(
                id = "char_clara_montgomery",
                name = "Clara Montgomery",
                age = 31,
                occupation = "Private Secretary & Archivist",
                publicBio = "Brilliant archivist responsible for typing Lord Blackwood's letters and legal codicils.",
                personality = "Quiet, perceptive, observant",
                publicMotive = "Blackwood knew of her alias and past criminal record for document forgery.",
                relationshipWithVictim = "Secretary",
                isCulprit = false,
                privateSecret = "Forged a letter of recommendation using Blackwood's wax seal earlier that afternoon.",
                personalAlibi = "Typing transcripts in the library annex.",
                claimedLocation = "Library Annex",
                actualActions = "Overheard raised voices between Blackwood and Edward at 23:05.",
                personalObjective = "Ensure her forgery is never discovered while aiding the detective.",
                interrogationResponses = mapOf(
                    "Timeline" to listOf(
                        InterrogationQnA(
                            question = "What was the contents of the document Lord Blackwood had you draft?",
                            answer = "He ordered a full codicil revoking Edward Vance's inheritance in favor of the Royal Academy."
                        )
                    )
                )
            ),
            CharacterDossier(
                id = "char_arthur_pendleton",
                name = "Arthur Pendleton",
                age = 65,
                occupation = "Head Butler",
                publicBio = "Served Blackwood Hall for over thirty-five years. Knows every secret passage.",
                personality = "Impeccably formal, watchful, fiercely loyal to estate tradition",
                publicMotive = "Distressed that Blackwood was planning to sell the ancestral estate to overseas buyers.",
                relationshipWithVictim = "Head of household staff",
                isCulprit = false,
                privateSecret = "Eavesdropped on all family meetings from the pantry service speaking tube.",
                personalAlibi = "Securing the front gates and bolting the cellar doors.",
                claimedLocation = "Ground Floor Service Corridor",
                actualActions = "Saw someone in a dark trench coat slipping through the terrace doors at 23:30.",
                personalObjective = "Uphold the dignity of the manor and assist the investigation.",
                interrogationResponses = mapOf(
                    "Timeline" to listOf(
                        InterrogationQnA(
                            question = "When did you discover Lord Blackwood?",
                            answer = "At exactly 23:45 when delivering his chamomile tea. The study door was unlocked from within."
                        )
                    )
                )
            ),
            CharacterDossier(
                id = "char_madame_vivienne",
                name = "Vivienne Leclair",
                age = 42,
                occupation = "French Opera Singer & Socialite Guest",
                publicBio = "Distinguished guest visiting the estate for the autumn gala.",
                personality = "Dramatic, observant, fond of gossip",
                publicMotive = "Blackwood held scandalous love letters from her youth.",
                relationshipWithVictim = "Former romantic acquaintance",
                isCulprit = false,
                privateSecret = "Was searching the library desk for her old correspondence at 20:30.",
                personalAlibi = "Playing piano in the music salon.",
                claimedLocation = "Music Salon",
                actualActions = "Played piano to provide an alibi while watching guests move through the foyer.",
                personalObjective = "Retrieve her letters without getting implicated in murder.",
                interrogationResponses = mapOf(
                    "Suspicious behaviour" to listOf(
                        InterrogationQnA(
                            question = "Did anyone pass the music salon before midnight?",
                            answer = "Edward Vance ran past towards the north corridor around 23:35. He looked as pale as a ghost!"
                        )
                    )
                )
            )
        )

        val evidence = listOf(
            EvidenceItem(
                id = "ev_poisoned_decanter",
                name = "Cut-Crystal Scotch Decanter",
                category = "WeaponTrace",
                discoveryLocation = "Study writing desk",
                discoveryPhase = "Initial",
                description = "Antique crystal decanter containing vintage Glenmorangie, tainted with white powdery residue.",
                detailedInspection = "Chemical smell of bitter belladonna. Only two glasses poured. A single fingerprint smudge on the stopper.",
                linkedCharacterIds = listOf("char_edward_vance", "char_dr_julian_finch"),
                whatItProves = "The poison was delivered orally through the scotch between 23:00 and 23:30.",
                isMisleading = false,
                timelineRelationship = "Placed on desk before 23:15.",
                isDiscovered = true
            ),
            EvidenceItem(
                id = "ev_torn_will_draft",
                name = "Torn Parchment with Blackwood Seal",
                category = "Document",
                discoveryLocation = "Grate of study fireplace",
                discoveryPhase = "SceneInspection",
                description = "Scorched corner of legal parchment naming a sole heir, signed just hours before the murder.",
                detailedInspection = "Words legible: '...hereby revoke all previous bequests to Edward Vance and convey all estate properties to...'",
                linkedCharacterIds = listOf("char_edward_vance"),
                whatItProves = "Edward Vance had immediate motive to kill before the new will was registered with the solicitor tomorrow morning.",
                isMisleading = false,
                timelineRelationship = "Drafted at 19:00, burned partially around 23:20.",
                isDiscovered = false
            ),
            EvidenceItem(
                id = "ev_muddy_footprints",
                name = "Terrace Clay Footprints",
                category = "Forensic",
                discoveryLocation = "French doors leading to garden terrace",
                discoveryPhase = "SceneInspection",
                description = "Faint prints of expensive Italian leather brogues stained with red clay.",
                detailedInspection = "Shoe size 10, distinctive diamond tread pattern matching bespoke London shoemaker.",
                linkedCharacterIds = listOf("char_edward_vance"),
                whatItProves = "Edward Vance entered the study from the outside terrace despite claiming he stayed inside the conservatory.",
                isMisleading = false,
                timelineRelationship = "Made between 23:10 and 23:35 during rainfall.",
                isDiscovered = false
            ),
            EvidenceItem(
                id = "ev_silver_cigarette_case",
                name = "Engraved Silver Cigarette Case",
                category = "PersonalItem",
                discoveryLocation = "Found on terrace garden steps",
                discoveryPhase = "SceneInspection",
                description = "Silver case engraved with monogram 'E.V.', contains three Egyptian cigarettes and a small key.",
                detailedInspection = "Contains a secret compartment with traces of white belladonna powder and a small brass key.",
                linkedCharacterIds = listOf("char_edward_vance"),
                whatItProves = "Edward carried the poison and the key to the study garden door.",
                isMisleading = false,
                timelineRelationship = "Dropped during hasty escape around 23:32.",
                isDiscovered = false
            ),
            EvidenceItem(
                id = "ev_broken_pocket_watch",
                name = "Lord Blackwood's Gold Pocket Watch",
                category = "PersonalItem",
                discoveryLocation = "Study rug beside desk",
                discoveryPhase = "Initial",
                description = "Gold pocket watch stopped at 23:22 with cracked crystal.",
                detailedInspection = "Watch crystal was cracked violently, establishing the precise moment of Lord Blackwood's struggle.",
                linkedCharacterIds = emptyList(),
                whatItProves = "Incident occurred at approximately 23:20 to 23:25.",
                isMisleading = false,
                timelineRelationship = "23:22 GMT.",
                isDiscovered = true
            ),
            EvidenceItem(
                id = "ev_opened_wall_safe",
                name = "Open Wall Safe Behind Oil Painting",
                category = "Environmental",
                discoveryLocation = "Study wall behind tilted family portrait",
                discoveryPhase = "SceneInspection",
                description = "Heavy brass dial safe left ajar. Velvet jewel box intact, but legal folder empty.",
                detailedInspection = "Safe was opened with combination dial, not forced. Only the original will folder is missing.",
                linkedCharacterIds = listOf("char_edward_vance", "char_clara_montgomery"),
                whatItProves = "The thief had the combination or was given it by someone who knew it.",
                isMisleading = false,
                timelineRelationship = "Accessed at 23:25.",
                isDiscovered = false
            ),
            EvidenceItem(
                id = "ev_burnt_charity_receipts",
                name = "Half-Burned Account Receipts",
                category = "Document",
                discoveryLocation = "Drawing room fireplace",
                discoveryPhase = "SceneInspection",
                description = "Ledger carbon slips showing transfers to Thorne Overseas Trust.",
                detailedInspection = "Reveals Lady Beatrice's embezzlement, but completely unrelated to the poison.",
                linkedCharacterIds = listOf("char_beatrice_thorne"),
                whatItProves = "Lady Beatrice was hiding financial misconduct, creating a false suspicion.",
                isMisleading = true,
                timelineRelationship = "Burned at 22:30.",
                isDiscovered = false
            ),
            EvidenceItem(
                id = "ev_telegram_from_monte_carlo",
                name = "Urgent Telegram from Monte Carlo",
                category = "Communication",
                discoveryLocation = "Edward Vance's coat pocket",
                discoveryPhase = "SceneInspection",
                description = "Dated three days ago: 'DEBTS OF 40,000 POUNDS DUE MIDNIGHT OCT 25 OR ASSETS SEIZED.'",
                detailedInspection = "Proves extreme immediate financial pressure on Edward Vance.",
                linkedCharacterIds = listOf("char_edward_vance"),
                whatItProves = "Edward faced ruin within 24 hours unless he secured the estate inheritance.",
                isMisleading = false,
                timelineRelationship = "Received October 21.",
                isDiscovered = false
            )
        )

        val timeline = listOf(
            TimelineEvent("tl_1", "19:00", "Will Rewritten", "Lord Blackwood meets with legal counsel to draft new will disinheriting Edward Vance.", listOf("char_edward_vance"), false, true),
            TimelineEvent("tl_2", "21:00", "Dr. Finch Heated Consultation", "Dr. Finch argues with Blackwood over medical treatment in the study.", listOf("char_dr_julian_finch"), false, true),
            TimelineEvent("tl_3", "22:00", "Lady Beatrice Cellar Visit", "Lady Beatrice seen near the wine cellar, later burning papers in drawing room.", listOf("char_beatrice_thorne"), false, true),
            TimelineEvent("tl_4", "23:00", "Edward Vance Alibi Window", "Edward claims he walked alone in the south conservatory during the rainstorm.", listOf("char_edward_vance"), false, true),
            TimelineEvent("tl_5", "23:15", "Study Garden Entry", "Edward slips through the secret passage into the study, poisons the scotch decanter.", listOf("char_edward_vance"), true, false),
            TimelineEvent("tl_6", "23:22", "Fatal Toast & Struggle", "Lord Blackwood drinks poisoned scotch, watch shatters in collapse, safe emptied.", listOf("char_edward_vance"), true, false),
            TimelineEvent("tl_7", "23:45", "Discovery of the Body", "Butler Arthur Pendleton enters with midnight tea, discovers Lord Blackwood and sounds alarm.", listOf("char_arthur_pendleton"), false, true)
        )

        // 5 Illustrated Interactive Locations
        val locations = listOf(
            InvestigationLocation(
                id = "loc_study",
                name = "Private Library Study",
                description = "The primary crime scene. High vaulted oak bookshelves, crackling fireplace hearth, and velvet drapes overlooking the stormy garden terrace.",
                atmosphereSnippet = "Rain lashes the heavy leaded panes. The scent of aged paper and bitter almonds hangs thick in the air.",
                isPrimaryCrimeScene = true,
                hotspots = listOf(
                    SceneHotspot("hs_desk", "Mahogany Writing Desk", 0.35f, 0.65f, 0.10f, "Scattered legal papers, overturned inkstand, and the cut-crystal scotch decanter.", "ev_poisoned_decanter"),
                    SceneHotspot("hs_hearth", "Stone Fireplace Hearth", 0.72f, 0.60f, 0.09f, "Glowing peat embers beneath cast iron grate with charred fragments of parchment.", "ev_torn_will_draft"),
                    SceneHotspot("hs_safe", "Tilted Portrait Wall Safe", 0.74f, 0.32f, 0.08f, "Gilt oil painting pushed aside to reveal an open brass dial safe with empty legal pouch.", "ev_opened_wall_safe"),
                    SceneHotspot("hs_terrace", "Terrace French Doors", 0.20f, 0.45f, 0.09f, "Tall French doors with brass latch unlocked. Smears of wet red clay on the polished threshold.", "ev_muddy_footprints"),
                    SceneHotspot("hs_floor", "Antique Persian Floor Rug", 0.50f, 0.80f, 0.08f, "Beside the overturned leather armchair lies a shattered gold pocket watch stopped at 23:22.", "ev_broken_pocket_watch")
                )
            ),
            InvestigationLocation(
                id = "loc_dining",
                name = "Grand Dining Hall",
                description = "A formal twenty-seat mahogany banquet table under twin crystal chandeliers. Silver candelabras flicker against walnut wood paneling.",
                atmosphereSnippet = "The remains of the evening's formal dinner sit half-cleared, preserving the exact seating arrangement.",
                isPrimaryCrimeScene = false,
                hotspots = listOf(
                    SceneHotspot("hs_table_head", "Lord Blackwood's Seat", 0.50f, 0.55f, 0.10f, "Lord Blackwood's crystal wine goblet and handwritten place card. Smells faintly of port wine."),
                    SceneHotspot("hs_credenza", "Sideboard Credenza", 0.25f, 0.45f, 0.09f, "Silver service bells and butler's attendance ledger noting who stepped away from table during courses.")
                )
            ),
            InvestigationLocation(
                id = "loc_terrace",
                name = "Garden Terrace & Conservatory",
                description = "Glass-domed conservatory packed with tropical ferns, opening out onto wet limestone terrace steps soaked in autumn rain.",
                atmosphereSnippet = "Cold drafts blow through the iron lattice. The smell of wet earth and cigar smoke lingers under the glass roof.",
                isPrimaryCrimeScene = false,
                hotspots = listOf(
                    SceneHotspot("hs_conservatory_bench", "Wicker Smoking Chair", 0.40f, 0.60f, 0.09f, "An iron ashtray with cold cigarette butts and a silver matchbook from the Savoy Club."),
                    SceneHotspot("hs_terrace_steps", "Wet Limestone Terrace Steps", 0.70f, 0.75f, 0.10f, "An engraved silver cigarette case dropped between wet drainage ferns.", "ev_silver_cigarette_case")
                )
            ),
            InvestigationLocation(
                id = "loc_billiards",
                name = "Billiard Room & Cellar Passage",
                description = "Dark green felt slate table under green glass dome lamps. A corner oak door conceals the spiral stone stairs leading down to the wine cellar.",
                atmosphereSnippet = "Chalk dust on mahogany rails. The concealed cellar door stands two inches ajar.",
                isPrimaryCrimeScene = false,
                hotspots = listOf(
                    SceneHotspot("hs_cue_rack", "Oak Cue Rack & Secret Latch", 0.30f, 0.50f, 0.09f, "A hidden brass catch behind cue #4 releases the latch to the old service corridor."),
                    SceneHotspot("hs_scoreboard", "Gambling Tally Chalkboard", 0.75f, 0.40f, 0.09f, "Recent chalk scores showing Vance deeply in arrears to private London creditors.")
                )
            ),
            InvestigationLocation(
                id = "loc_drawing",
                name = "Drawing Room & Hearth",
                description = "Plush velvet sofas, a baby grand piano, and an ornate marble fireplace where guests gathered before the storm broke.",
                atmosphereSnippet = "The quiet hum of the storm outside. A lingering aroma of expensive perfume and burned stationery.",
                isPrimaryCrimeScene = false,
                hotspots = listOf(
                    SceneHotspot("hs_drawing_hearth", "Marble Drawing Room Hearth", 0.50f, 0.65f, 0.10f, "Half-burned accounting receipts and bank withdrawal slips signed by Lady Beatrice.", "ev_burnt_charity_receipts"),
                    SceneHotspot("hs_piano", "Baby Grand Piano", 0.80f, 0.50f, 0.09f, "Open French opera score with handwritten notes on the margins regarding Blackwood's letters.")
                )
            )
        )

        val crimeScene = CrimeSceneData(
            roomName = "Lord Blackwood's Private Library Study",
            description = "A gothic oak-paneled study with high bookshelves, crackling hearth, and heavy velvet curtains overlooking the terrace.",
            sceneAssetId = "img_crime_scene",
            hotspots = locations.first().hotspots
        )

        val midGameTwist = MidGameTwist(
            id = "twist_blackwood_cut",
            title = "The Midnight Power Cut & Second Cipher Note",
            eventNarration = "At 23:30, a sharp thunderclap severed the estate generator, plunging Blackwood Hall into pitch darkness. In the confusion, footsteps were heard rushing from the library. Butler Pendleton discovered a scorched cipher note pinned to the study door!",
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

        val solution = SecretSolution(
            culpritId = "char_edward_vance",
            motive = "Desperate 40,000 pound gambling debts due within 24 hours, combined with Lord Blackwood's sudden decision to disinherit him in favor of charity.",
            opportunity = "Utilized the secret garden terrace passage at 23:15, bypassing the main hallway and servants.",
            method = "Administered pure belladonna alkaloid powder into Lord Blackwood's private vintage scotch decanter.",
            hiddenTimeline = timeline.filter { it.isHiddenSequence },
            misleadingStrategy = "Attempted to frame Dr. Finch by leaving medical doubts, burned the new will draft, and fabricated a false conservatory walking alibi.",
            smokingGunEvidenceIds = listOf("ev_silver_cigarette_case", "ev_muddy_footprints", "ev_torn_will_draft"),
            contradictionExplanations = mapOf(
                "Conservatory Walk" to "Mud on his shoes proved he crossed the rainy terrace, not the dry indoor conservatory.",
                "Friendly Relationship" to "Torn will draft and Monte Carlo telegram proved bitter enmity and urgent debt."
            ),
            misleadingEvidenceExplanations = mapOf(
                "ev_burnt_charity_receipts" to "Lady Beatrice was hiding financial embezzlement from the estate charity, completely unconnected to the homicide."
            ),
            finalVerdictExplanation = "Edward Vance is guilty of the murder of Lord Reginald Blackwood. Driven by imminent financial ruin from Monte Carlo gambling syndicates, Edward entered the study via the terrace, laced the scotch decanter with belladonna, tore the new will draft, and fled. His alibi collapsed when forensic soil matches, the recovered cigarette case with poison residue, and his urgent telegram were confronted against his testimony."
        )

        return CasePackage(
            identity = CaseIdentity(
                caseId = "case_blackwood_001",
                title = "The Cipher at Blackwood Hall",
                subtitle = "A deadly poison, an altered will, and six suspects behind locked doors.",
                classification = "CASE 001 - CLASSIFIED DOSSIER",
                setting = "Blackwood Hall, Northumberland, England",
                dateTime = "October 24, 1928, 23:45 GMT",
                difficulty = "Medium",
                estimatedDurationMinutes = "15-20 min",
                cinematicIntro = "Rain lashes the leaded glass windows of Blackwood Hall. The storm has severed the telegraph wires. When the grandfather clock strikes midnight, Lord Blackwood sits motionless at his mahogany desk. A crystal glass lies shattered on the Persian rug. The newly revised will is gone.",
                incidentSummary = "Lord Reginald Blackwood was poisoned in his private library study between 23:15 and 23:30. Six individuals were inside the manor. One is the killer.",
                investigationObjective = "Uncover who poisoned Lord Blackwood, reconstruct the timeline, expose the broken alibi, and accuse the true culprit.",
                posterAssetId = "img_case_poster"
            ),
            victim = VictimProfile(
                name = "Lord Reginald Blackwood",
                age = 62,
                occupation = "Patriarch & Industrialist",
                background = "Magnate of Blackwood Mining & Antiquities, rumored to be rewriting his will.",
                causeOfDeath = "Poisoning by rare belladonna alkaloid in vintage scotch",
                locationFound = "Private library study, seated at mahogany desk"
            ),
            characters = characters,
            evidence = evidence + midGameTwist.revealedEvidence,
            timeline = timeline,
            crimeScene = crimeScene,
            locations = locations,
            midGameTwist = midGameTwist,
            secretSolution = solution
        )
    }
}
