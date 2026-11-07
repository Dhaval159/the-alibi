package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.TheAlibiRepository
import com.example.model.*
import com.example.util.SoundManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class Screen {
    HOME,
    CREATE_ROOM,
    JOIN_ROOM,
    LOBBY,
    CASE_GENERATOR,
    GENERATION_PROGRESS,
    GENERATED_PREVIEW,
    CASE_LIBRARY,
    CASE_OPENING,
    CHARACTER_REVEAL,
    INTERACTIVE_SCENE,
    EVIDENCE_GALLERY,
    SUSPECT_GALLERY,
    INTERROGATION,
    INVESTIGATION_BOARD,
    TIMELINE,
    OBJECTIVE_CENTER,
    PRIVATE_CHAT,
    DEBATE,
    ACCUSATION,
    RESOLUTION
}

data class TheAlibiUiState(
    val currentScreen: Screen = Screen.HOME,
    val activeCase: CasePackage? = null,
    val assignedCharacter: CharacterDossier? = null,
    val isDossierConcealed: Boolean = false,
    val savedCases: List<CasePackage> = emptyList(),
    // Room and multiplayer state
    val currentRoom: RoomSnapshot? = null,
    val isHost: Boolean = false,
    val myPlayerId: String? = null,
    val myAssignedCharacter: MyAssignedCharacter? = null,
    val roomMysteryTheme: String = "Mansion inheritance mystery",
    val roomMysteryDifficulty: String = "Medium",
    val roomAtmosphere: String = "Classic detective mystery",
    val roomCustomPrompt: String = "",
    val showGenerateConfirmationDialog: Boolean = false,
    // Generation states
    val generationConfig: CaseGenerationConfig = CaseGenerationConfig(),
    val generationProgress: Int = 0,
    val generationStage: String = "",
    val isGenerating: Boolean = false,
    val generatedCaseForPreview: CasePackage? = null,
    // Multi-location & Scene
    val activeLocationId: String? = "loc_study",
    val selectedHotspot: SceneHotspot? = null,
    val selectedEvidenceForDetail: EvidenceItem? = null,
    // Suspects & Interrogation
    val activeInterrogationSuspect: CharacterDossier? = null,
    val selectedInterrogationCategory: String = "Motive",
    val discoveredEvidenceIds: Set<String> = emptySet(),
    val inspectedHotspotIds: Set<String> = emptySet(),
    val suspiciousCharacterIds: Set<String> = emptySet(),
    val boardConnections: List<BoardConnection> = emptyList(),
    val deductionNotes: List<DeductionNote> = emptyList(),
    val interrogationHistory: Map<String, List<InterrogationQnA>> = emptyMap(),
    // Objectives
    val primaryObjective: PersonalObjective? = null,
    val secondaryObjective: PersonalObjective? = null,
    // Private chat & deals
    val privateMessages: List<PrivateMessage> = emptyList(),
    val dealProposals: List<DealProposal> = emptyList(),
    val activeChatRecipientPlayerId: String? = null,
    val activeChatRecipientName: String = "Investigator",
    // Mid-game twist
    val activeTwist: MidGameTwist? = null,
    val isTwistDismissed: Boolean = false,
    val showTwistDialog: Boolean = false,
    // Timer & Voting
    val gameTimer: GameTimerState? = null,
    val hasVoted: Boolean = false,
    val gameResolution: GameResolutionSummary? = null,
    // Accusation selection state
    val selectedAccusedCharacterId: String? = null,
    val selectedAccusationMotive: String = "Desperate 40,000 pound debt to Monte Carlo syndicates due at midnight.",
    val selectedAccusationMethod: String = "Laced pure belladonna into Lord Blackwood's vintage scotch decanter.",
    val selectedSmokingGunEvidenceId: String? = null,
    val verdict: AccusationVerdict? = null,
    // Audio toggles & modals
    val soundSettingsOpen: Boolean = false,
    val isMusicEnabled: Boolean = true,
    val isSfxEnabled: Boolean = true,
    val isNarrationEnabled: Boolean = true,
    // Notifications & user alerts
    val toastMessage: String? = null
)

class TheAlibiViewModel(
    private val repository: TheAlibiRepository = TheAlibiRepository(),
    val soundManager: SoundManager = SoundManager()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TheAlibiUiState())
    val uiState: StateFlow<TheAlibiUiState> = _uiState.asStateFlow()

    private var generationJob: Job? = null

    init {
        viewModelScope.launch {
            repository.activeCase.collect { casePkg ->
                _uiState.value = _uiState.value.copy(activeCase = casePkg)
            }
        }
        viewModelScope.launch {
            repository.assignedCharacter.collect { char ->
                _uiState.value = _uiState.value.copy(assignedCharacter = char)
            }
        }
        viewModelScope.launch {
            repository.isDossierConcealed.collect { concealed ->
                _uiState.value = _uiState.value.copy(isDossierConcealed = concealed)
            }
        }
        viewModelScope.launch {
            repository.savedCases.collect { list ->
                _uiState.value = _uiState.value.copy(savedCases = list)
            }
        }
        viewModelScope.launch {
            repository.currentRoom.collect { room ->
                _uiState.value = _uiState.value.copy(currentRoom = room)
            }
        }
        viewModelScope.launch {
            repository.isHost.collect { isHost ->
                _uiState.value = _uiState.value.copy(isHost = isHost)
            }
        }
        viewModelScope.launch {
            repository.myPlayerId.collect { myId ->
                _uiState.value = _uiState.value.copy(myPlayerId = myId)
            }
        }
        viewModelScope.launch {
            repository.myAssignedCharacter.collect { myAssigned ->
                _uiState.value = _uiState.value.copy(myAssignedCharacter = myAssigned)
            }
        }
        viewModelScope.launch {
            repository.activeLocationId.collect { locId ->
                _uiState.value = _uiState.value.copy(activeLocationId = locId)
            }
        }
        viewModelScope.launch {
            repository.primaryObjective.collect { obj ->
                _uiState.value = _uiState.value.copy(primaryObjective = obj)
            }
        }
        viewModelScope.launch {
            repository.secondaryObjective.collect { obj ->
                _uiState.value = _uiState.value.copy(secondaryObjective = obj)
            }
        }
        viewModelScope.launch {
            repository.privateMessages.collect { msgs ->
                _uiState.value = _uiState.value.copy(privateMessages = msgs)
            }
        }
        viewModelScope.launch {
            repository.dealProposals.collect { deals ->
                _uiState.value = _uiState.value.copy(dealProposals = deals)
            }
        }
        viewModelScope.launch {
            repository.activeTwist.collect { twist ->
                _uiState.value = _uiState.value.copy(activeTwist = twist)
            }
        }
        viewModelScope.launch {
            repository.isTwistDismissed.collect { dismissed ->
                _uiState.value = _uiState.value.copy(
                    isTwistDismissed = dismissed,
                    showTwistDialog = !dismissed && _uiState.value.activeTwist != null &&
                            _uiState.value.currentRoom?.state == RoomState.MID_GAME_TWIST
                )
            }
        }
        viewModelScope.launch {
            repository.gameTimer.collect { timer ->
                _uiState.value = _uiState.value.copy(gameTimer = timer)
            }
        }
        viewModelScope.launch {
            repository.hasVoted.collect { voted ->
                _uiState.value = _uiState.value.copy(hasVoted = voted)
            }
        }
        viewModelScope.launch {
            repository.gameResolution.collect { res ->
                _uiState.value = _uiState.value.copy(gameResolution = res)
            }
        }
        viewModelScope.launch {
            repository.discoveredEvidenceIds.collect { ids ->
                _uiState.value = _uiState.value.copy(discoveredEvidenceIds = ids)
            }
        }
        viewModelScope.launch {
            repository.inspectedHotspotIds.collect { ids ->
                _uiState.value = _uiState.value.copy(inspectedHotspotIds = ids)
            }
        }
        viewModelScope.launch {
            repository.suspiciousCharacterIds.collect { ids ->
                _uiState.value = _uiState.value.copy(suspiciousCharacterIds = ids)
            }
        }
        viewModelScope.launch {
            repository.boardConnections.collect { conns ->
                _uiState.value = _uiState.value.copy(boardConnections = conns)
            }
        }
        viewModelScope.launch {
            repository.deductionNotes.collect { notes ->
                _uiState.value = _uiState.value.copy(deductionNotes = notes)
            }
        }
        viewModelScope.launch {
            repository.interrogationHistory.collect { history ->
                _uiState.value = _uiState.value.copy(interrogationHistory = history)
            }
        }
        viewModelScope.launch {
            repository.verdict.collect { verdict ->
                _uiState.value = _uiState.value.copy(verdict = verdict)
            }
        }
        viewModelScope.launch {
            soundManager.isMusicEnabled.collect {
                _uiState.value = _uiState.value.copy(isMusicEnabled = it)
            }
        }
        viewModelScope.launch {
            soundManager.isSfxEnabled.collect {
                _uiState.value = _uiState.value.copy(isSfxEnabled = it)
            }
        }
        viewModelScope.launch {
            soundManager.isNarrationEnabled.collect {
                _uiState.value = _uiState.value.copy(isNarrationEnabled = it)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }

    // --- NAVIGATION ---

    fun navigateTo(screen: Screen) {
        soundManager.playPhaseChangeChime()
        _uiState.value = _uiState.value.copy(currentScreen = screen)
    }

    // --- SOUND & SETTINGS ---

    fun toggleSoundSettingsDialog(open: Boolean? = null) {
        val newVal = open ?: !_uiState.value.soundSettingsOpen
        _uiState.value = _uiState.value.copy(soundSettingsOpen = newVal)
    }

    fun toggleMusic() = soundManager.toggleMusic()
    fun toggleSfx() = soundManager.toggleSfx()
    fun toggleNarration() = soundManager.toggleNarration()

    // --- ROOM CREATION & JOINING ---

    fun updateRoomMysteryTheme(theme: String) {
        _uiState.value = _uiState.value.copy(roomMysteryTheme = theme)
    }

    fun updateRoomMysteryDifficulty(difficulty: String) {
        _uiState.value = _uiState.value.copy(roomMysteryDifficulty = difficulty)
    }

    fun updateRoomAtmosphere(atmosphere: String) {
        _uiState.value = _uiState.value.copy(roomAtmosphere = atmosphere)
    }

    fun updateRoomCustomPrompt(prompt: String) {
        _uiState.value = _uiState.value.copy(roomCustomPrompt = prompt)
    }

    fun createRoom(hostPlayerName: String) {
        val params = CreateRoomParams(
            hostPlayerName = hostPlayerName,
            theme = _uiState.value.roomMysteryTheme,
            difficulty = _uiState.value.roomMysteryDifficulty,
            atmosphere = _uiState.value.roomAtmosphere,
            customPrompt = _uiState.value.roomCustomPrompt
        )
        repository.createRoom(params)
        soundManager.playNotificationAlert()
        navigateTo(Screen.LOBBY)
    }

    fun joinRoom(roomCode: String, playerName: String) {
        val res = repository.joinRoom(JoinRoomParams(roomCode = roomCode, playerName = playerName))
        if (res.isSuccess) {
            soundManager.playNotificationAlert()
            navigateTo(Screen.LOBBY)
        } else {
            showToast(res.exceptionOrNull()?.message ?: "Failed to join room")
        }
    }

    fun addMockInvestigator(name: String = "") {
        val count = (_uiState.value.currentRoom?.players?.size ?: 1) + 1
        val res = repository.addMockInvestigator(name.ifBlank { "Investigator $count" })
        if (res.isFailure) {
            showToast(res.exceptionOrNull()?.message ?: "Failed to add player")
        } else {
            soundManager.playNotificationAlert()
        }
    }

    fun removeMockInvestigator(playerId: String) {
        val res = repository.removeMockInvestigator(playerId)
        if (res.isFailure) {
            showToast(res.exceptionOrNull()?.message ?: "Failed to remove player")
        }
    }

    fun togglePlayerReady(isReady: Boolean) {
        repository.togglePlayerReady(isReady)
    }

    fun showGenerateConfirmationDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showGenerateConfirmationDialog = show)
    }

    fun setGenerateConfirmationDialog(show: Boolean) = showGenerateConfirmationDialog(show)
    fun startRoomMysteryGeneration() = startMysteryGeneration()

    fun startMysteryGeneration() {
        _uiState.value = _uiState.value.copy(
            showGenerateConfirmationDialog = false,
            isGenerating = true,
            generationProgress = 5,
            generationStage = "Initiating room mystery generation..."
        )
        navigateTo(Screen.GENERATION_PROGRESS)

        val config = CaseGenerationConfig(
            theme = _uiState.value.roomMysteryTheme,
            difficulty = _uiState.value.roomMysteryDifficulty,
            atmosphere = _uiState.value.roomAtmosphere,
            customPrompt = _uiState.value.roomCustomPrompt,
            playerCount = _uiState.value.currentRoom?.players?.size ?: 6
        )

        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            val result = repository.generateRoomMystery(config) { progress, stage ->
                _uiState.value = _uiState.value.copy(
                    generationProgress = progress,
                    generationStage = stage
                )
            }

            _uiState.value = _uiState.value.copy(isGenerating = false)

            if (result.isSuccess) {
                soundManager.playSecretReveal()
                // Transition all players to Case Briefing
                navigateTo(Screen.CASE_OPENING)
            } else {
                showToast(result.exceptionOrNull()?.message ?: "Generation failed.")
                navigateTo(Screen.LOBBY)
            }
        }
    }

    fun cancelGeneration() {
        generationJob?.cancel()
        _uiState.value = _uiState.value.copy(isGenerating = false)
        navigateTo(Screen.LOBBY)
    }

    fun leaveRoom() {
        repository.leaveRoom()
        navigateTo(Screen.HOME)
    }

    // --- GAME PHASES TRANSITION ---

    fun advanceToCharacterReveal() {
        repository.transitionRoomState(RoomState.CHARACTER_REVEAL)
        soundManager.playSecretReveal()
        navigateTo(Screen.CHARACTER_REVEAL)
    }

    fun advanceToInvestigation() {
        repository.transitionRoomState(RoomState.INVESTIGATION)
        soundManager.playPhaseChangeChime()
        navigateTo(Screen.INTERACTIVE_SCENE)
    }

    fun advanceToDebate() {
        repository.transitionRoomState(RoomState.INTERROGATION_DEBATE)
        soundManager.playPhaseChangeChime()
        navigateTo(Screen.DEBATE)
    }

    fun advanceToVoting() {
        repository.transitionRoomState(RoomState.VOTING)
        soundManager.playAccusationGong()
        navigateTo(Screen.ACCUSATION)
    }

    // --- MULTI-LOCATION & INVESTIGATION ---

    fun setActiveLocation(locationId: String) {
        repository.setActiveLocation(locationId)
        soundManager.playPhaseChangeChime()
    }

    fun selectHotspot(hotspot: SceneHotspot?) {
        _uiState.value = _uiState.value.copy(selectedHotspot = hotspot)
        if (hotspot != null) {
            repository.inspectHotspot(hotspot.id)
            soundManager.playEvidenceDiscovery()
        }
    }

    fun inspectSelectedHotspot() {
        val hotspot = _uiState.value.selectedHotspot ?: return
        repository.inspectHotspot(hotspot.id)
        soundManager.playEvidenceDiscovery()
        if (hotspot.associatedEvidenceId != null) {
            val ev = _uiState.value.activeCase?.evidence?.find { it.id == hotspot.associatedEvidenceId }
            _uiState.value = _uiState.value.copy(selectedEvidenceForDetail = ev)
        }
    }

    fun selectEvidenceForDetail(evidence: EvidenceItem?) {
        _uiState.value = _uiState.value.copy(selectedEvidenceForDetail = evidence)
    }

    fun toggleSuspectSuspicion(characterId: String) {
        repository.toggleSuspectSuspicion(characterId)
    }

    fun toggleConcealDossier() {
        repository.toggleConcealDossier()
    }

    // --- PRIVATE MESSAGING & DEALS ---

    fun openPrivateChat(recipientPlayerId: String, recipientName: String) {
        _uiState.value = _uiState.value.copy(
            activeChatRecipientPlayerId = recipientPlayerId,
            activeChatRecipientName = recipientName
        )
        soundManager.playNotificationAlert()
        navigateTo(Screen.PRIVATE_CHAT)
    }

    fun sendPrivateMessage(recipientPlayerId: String, text: String, attachedEvidenceId: String? = null) {
        if (text.isBlank()) return
        repository.sendPrivateMessage(
            recipientPlayerId = recipientPlayerId,
            text = text,
            attachedEvidenceId = attachedEvidenceId
        )
        soundManager.playNotificationAlert()
    }

    fun proposeDeal(recipientPlayerId: String, title: String, terms: String) {
        repository.proposeDeal(recipientPlayerId, title, terms)
        soundManager.playNotificationAlert()
        showToast("Deal proposed to ${_uiState.value.activeChatRecipientName}")
    }

    fun respondToDeal(dealId: String, accept: Boolean) {
        repository.respondToDeal(dealId, accept)
        soundManager.playNotificationAlert()
        showToast(if (accept) "Deal accepted!" else "Deal declined.")
    }

    // --- MID-GAME TWIST ---

    fun triggerMidGameTwist() {
        repository.triggerMidGameTwist()
        soundManager.playAccusationGong()
        _uiState.value = _uiState.value.copy(showTwistDialog = true)
    }

    fun dismissTwistDialog() {
        repository.dismissTwist()
        _uiState.value = _uiState.value.copy(showTwistDialog = false)
    }

    // --- INTERROGATION ---

    fun startInterrogationWithSuspect(suspect: CharacterDossier) {
        _uiState.value = _uiState.value.copy(
            activeInterrogationSuspect = suspect,
            selectedInterrogationCategory = "Motive"
        )
        navigateTo(Screen.INTERROGATION)
    }

    fun openInterrogation(suspect: CharacterDossier) = startInterrogationWithSuspect(suspect)

    fun setInterrogationCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedInterrogationCategory = category)
    }

    fun askInterrogationQuestion(suspect: CharacterDossier, qna: InterrogationQnA) {
        repository.askInterrogationQuestion(suspect.id, qna)
        if (qna.revealsContradiction) {
            soundManager.playAccusationGong()
        } else {
            soundManager.playNotificationAlert()
        }
    }

    // --- ACCUSATION & SECRET BALLOT ---

    fun selectBallotAccused(characterId: String) {
        _uiState.value = _uiState.value.copy(
            selectedAccusedCharacterId = characterId
        )
    }

    fun selectBallotSmokingGun(evidenceId: String) {
        _uiState.value = _uiState.value.copy(
            selectedSmokingGunEvidenceId = evidenceId
        )
    }

    fun setAccusationMotive(motive: String) {
        _uiState.value = _uiState.value.copy(
            selectedAccusationMotive = motive
        )
    }

    fun submitBallotVote() {
        val accusedId = _uiState.value.selectedAccusedCharacterId
            ?: _uiState.value.activeCase?.characters?.firstOrNull()?.id ?: return
        val method = _uiState.value.selectedAccusationMethod
        val motive = _uiState.value.selectedAccusationMotive
        val gun = _uiState.value.selectedSmokingGunEvidenceId

        repository.submitBallotVote(accusedId, method, motive, gun)
        soundManager.playAccusationGong()
        showToast("Ballot submitted into sealed ballot box.")

        // Automatically resolve after ballot
        tallyAndResolveCase()
    }

    fun tallyAndResolveCase() {
        soundManager.playAccusationGong()
        val resolution = repository.tallyAndResolveCase()
        // Also run legacy verdict evaluation for backward compatibility
        val accused = _uiState.value.selectedAccusedCharacterId ?: resolution.trueCulpritId
        val gun = _uiState.value.selectedSmokingGunEvidenceId
        repository.submitAccusation(accused, _uiState.value.selectedAccusationMotive, gun)
        navigateTo(Screen.RESOLUTION)
    }

    // Legacy method aliases
    fun selectAccusedSuspect(characterId: String) = selectBallotAccused(characterId)
    fun selectSmokingGunEvidence(evidenceId: String) = selectBallotSmokingGun(evidenceId)
    fun selectAccusationMotive(motive: String) = setAccusationMotive(motive)
    fun submitAccusation() = submitBallotVote()

    // --- SCREEN DELEGATES & HELPER ACTIONS ---

    fun updateGenerationConfig(config: CaseGenerationConfig) {
        _uiState.value = _uiState.value.copy(generationConfig = config)
    }

    fun startCaseGeneration() = startMysteryGeneration()

    fun selectCase(casePkg: CasePackage, assignedCharId: String? = null) {
        repository.selectCase(casePkg, assignedCharId)
        soundManager.playSecretReveal()
        navigateTo(Screen.CASE_OPENING)
    }

    fun deleteCase(caseId: String) {
        repository.deleteCase(caseId)
        showToast("Case deleted.")
    }

    fun updateRoomConfig(theme: String, diff: String, atmo: String, prompt: String) {
        _uiState.value = _uiState.value.copy(
            roomMysteryTheme = theme,
            roomMysteryDifficulty = diff,
            roomAtmosphere = atmo,
            roomCustomPrompt = prompt
        )
    }

    fun collectEvidence(evidenceId: String) {
        repository.collectEvidence(evidenceId)
        soundManager.playEvidenceDiscovery()
        showToast("Clue collected.")
    }

    fun addDeductionNote(text: String) {
        repository.addDeductionNote(text)
        soundManager.playNotificationAlert()
        showToast("Note added to notebook.")
    }

    fun askQuestion(qna: InterrogationQnA) {
        val suspect = _uiState.value.activeInterrogationSuspect ?: return
        askInterrogationQuestion(suspect, qna)
    }

    fun addBoardConnection(suspectId: String, evidenceId: String, label: String) {
        repository.addBoardConnection(suspectId, evidenceId, label)
        soundManager.playEvidenceDiscovery()
    }

    fun removeBoardConnection(connectionId: String) {
        repository.removeBoardConnection(connectionId)
    }

    // --- ALERTS ---

    fun showToast(msg: String) {
        _uiState.value = _uiState.value.copy(toastMessage = msg)
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }
}

