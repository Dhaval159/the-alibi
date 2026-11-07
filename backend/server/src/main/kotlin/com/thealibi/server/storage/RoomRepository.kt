package com.thealibi.server.storage

import com.thealibi.server.models.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Collections
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

data class InternalRoom(
    val roomId: String,
    val roomCode: String,
    val hostPlayerId: String,
    var state: RoomState = RoomState.WAITING_FOR_PLAYERS,
    val players: ConcurrentHashMap<String, PlayerSession> = ConcurrentHashMap(),
    var frozenRoster: List<PlayerSession>? = null,
    var frozenPlayerCount: Int? = null,
    var generationConfig: CaseGenerationRequest = CaseGenerationRequest(),
    var generationJobId: String? = null,
    var caseId: String? = null,
    var characterAssignments: Map<String, String>? = null, // playerId -> characterId
    var timerState: GameTimerState? = null,
    var matchDurationMinutes: Int = 18,
    val publicDiscoveredEvidenceIds: MutableSet<String> = ConcurrentHashMap.newKeySet(),
    val playerObjectives: ConcurrentHashMap<String, Pair<PersonalObjective, PersonalObjective>> = ConcurrentHashMap(),
    val privateMessages: MutableList<PrivateMessage> = Collections.synchronizedList(mutableListOf()),
    val dealProposals: ConcurrentHashMap<String, DealProposal> = ConcurrentHashMap(),
    val ballotVotes: ConcurrentHashMap<String, BallotVote> = ConcurrentHashMap(),
    var resolutionSummary: GameResolutionSummary? = null,
    val webSockets: ConcurrentHashMap<String, WebSocketSession> = ConcurrentHashMap(),
    var errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

class RoomRepository(
    private val caseRepository: CaseRepository
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val roomsByCode = ConcurrentHashMap<String, InternalRoom>()
    private val roomsById = ConcurrentHashMap<String, InternalRoom>()

    fun createRoom(
        hostPlayerName: String,
        config: CaseGenerationRequest = CaseGenerationRequest(),
        matchDurationMinutes: Int = 18
    ): Pair<InternalRoom, PlayerSession> {
        val roomId = "room_${UUID.randomUUID().toString().take(8)}"
        val roomCode = generateUniqueRoomCode()
        val hostPlayerId = "player_${UUID.randomUUID().toString().take(6)}"
        val sessionToken = "tok_${UUID.randomUUID().toString().take(12)}"

        val hostSession = PlayerSession(
            playerId = hostPlayerId,
            playerName = hostPlayerName.ifBlank { "Host Detective" },
            isHost = true,
            isReady = true,
            isConnected = true,
            sessionToken = sessionToken
        )

        val room = InternalRoom(
            roomId = roomId,
            roomCode = roomCode,
            hostPlayerId = hostPlayerId,
            generationConfig = config,
            matchDurationMinutes = matchDurationMinutes.coerceIn(15, 20)
        )
        room.players[hostPlayerId] = hostSession

        roomsByCode[roomCode] = room
        roomsById[roomId] = room

        return Pair(room, hostSession)
    }

    fun getRoomByCode(roomCode: String): InternalRoom? = roomsByCode[roomCode.trim()]

    fun joinRoom(roomCode: String, playerName: String): Result<Pair<InternalRoom, PlayerSession>> {
        val room = getRoomByCode(roomCode)
            ?: return Result.failure(IllegalArgumentException("Room $roomCode does not exist"))

        if (room.state != RoomState.WAITING_FOR_PLAYERS) {
            return Result.failure(IllegalStateException("Case generation has started. This room is no longer accepting players."))
        }

        if (room.players.size >= 10) {
            return Result.failure(IllegalStateException("Room $roomCode is full (maximum 10 players)."))
        }

        val playerId = "player_${UUID.randomUUID().toString().take(6)}"
        val sessionToken = "tok_${UUID.randomUUID().toString().take(12)}"
        val session = PlayerSession(
            playerId = playerId,
            playerName = playerName.ifBlank { "Investigator ${room.players.size + 1}" },
            isHost = false,
            isReady = true,
            isConnected = true,
            sessionToken = sessionToken
        )

        room.players[playerId] = session
        broadcastRoomSnapshot(room)
        return Result.success(Pair(room, session))
    }

    fun freezeRosterForGeneration(
        roomCode: String,
        requestingPlayerId: String,
        sessionToken: String,
        updatedConfig: CaseGenerationRequest? = null
    ): Result<Pair<InternalRoom, Int>> {
        val room = getRoomByCode(roomCode)
            ?: return Result.failure(IllegalArgumentException("Room $roomCode not found"))

        val caller = room.players[requestingPlayerId]
        if (caller == null || caller.sessionToken != sessionToken || !caller.isHost) {
            return Result.failure(IllegalAccessException("Only the authenticated room host can initiate mystery generation."))
        }

        if (room.state != RoomState.WAITING_FOR_PLAYERS) {
            if (room.generationJobId != null) {
                return Result.success(Pair(room, room.frozenPlayerCount ?: room.players.size))
            }
            return Result.failure(IllegalStateException("Room is not in WAITING_FOR_PLAYERS state (currently ${room.state})."))
        }

        val currentCount = room.players.size
        if (currentCount < 4) {
            return Result.failure(IllegalStateException("Minimum 4 players required to generate mystery (currently $currentCount)."))
        }
        if (currentCount > 10) {
            return Result.failure(IllegalStateException("Maximum 10 players supported (currently $currentCount)."))
        }

        val frozenList = room.players.values.toList().sortedBy { it.joinedAt }
        room.frozenRoster = frozenList
        room.frozenPlayerCount = frozenList.size
        room.state = RoomState.GENERATING_CASE
        if (updatedConfig != null) {
            room.generationConfig = updatedConfig.copy(playerCount = currentCount)
        } else {
            room.generationConfig = room.generationConfig.copy(playerCount = currentCount)
        }

        broadcastRoomSnapshot(room)
        return Result.success(Pair(room, currentCount))
    }

    fun setGenerationJob(roomCode: String, jobId: String) {
        val room = getRoomByCode(roomCode) ?: return
        room.generationJobId = jobId
        broadcastRoomSnapshot(room)
    }

    fun updateRoomState(roomCode: String, state: RoomState, errorMsg: String? = null) {
        val room = getRoomByCode(roomCode) ?: return
        room.state = state
        room.errorMessage = errorMsg
        broadcastRoomSnapshot(room)
    }

    fun transitionPhase(roomCode: String, nextPhase: RoomState, durationSeconds: Int? = null): GameTimerState? {
        val room = getRoomByCode(roomCode) ?: return null
        room.state = nextPhase

        val duration = durationSeconds ?: when (nextPhase) {
            RoomState.CASE_BRIEFING -> 120 // 2 minutes opening briefing
            RoomState.CHARACTER_REVEAL -> 90 // 1.5 minutes private dossier reading
            RoomState.INVESTIGATION -> 300 // 5 minutes Act I investigation
            RoomState.MID_GAME_TWIST -> 60 // 1 minute dramatic announcement
            RoomState.INTERROGATION_DEBATE -> 240 // 4 minutes Act II group debate
            RoomState.VOTING -> 120 // 2 minutes secret ballot
            RoomState.CASE_RESOLUTION -> 300 // 5 minutes final reveal & scores
            else -> 0
        }

        val start = System.currentTimeMillis()
        val end = start + (duration * 1000L)
        val timer = GameTimerState(
            phase = nextPhase,
            startTimeMillis = start,
            durationSeconds = duration,
            endTimeMillis = end,
            remainingSeconds = duration
        )
        room.timerState = timer
        broadcastRoomSnapshot(room)
        return timer
    }

    fun getTimerState(roomCode: String): GameTimerState? {
        val room = getRoomByCode(roomCode) ?: return null
        val timer = room.timerState ?: return null
        val remaining = ((timer.endTimeMillis - System.currentTimeMillis()) / 1000L).toInt().coerceAtLeast(0)
        return timer.copy(remainingSeconds = remaining)
    }

    fun assignCharacters(roomCode: String, casePackage: CasePackage): Map<String, String> {
        val room = getRoomByCode(roomCode) ?: throw IllegalArgumentException("Room $roomCode not found")
        val frozen = room.frozenRoster ?: room.players.values.toList()
        val humanCount = frozen.size

        val solution = casePackage.secretSolution
            ?: throw IllegalStateException("Case missing secret solution for character assignment")
        val culpritCharId = solution.culpritId

        val allChars = casePackage.characters
        val culpritChar = allChars.find { it.id == culpritCharId }
            ?: throw IllegalStateException("Culprit character not found in case package")
        val innocentChars = allChars.filter { it.id != culpritCharId }

        val selectedInnocents = innocentChars.take(humanCount - 1)
        val selectedChars = mutableListOf(culpritChar)
        selectedChars.addAll(selectedInnocents)

        val shuffledChars = selectedChars.shuffled(Random(System.currentTimeMillis()))

        val assignments = mutableMapOf<String, String>()
        for (i in frozen.indices) {
            val player = frozen[i]
            val char = shuffledChars[i]
            assignments[player.playerId] = char.id

            // Generate tailored personal objectives
            val isCulprit = char.id == culpritCharId
            val primaryObj = if (isCulprit) {
                PersonalObjective(
                    id = "obj_culprit_primary",
                    title = "Protect the Secret Alibi",
                    description = "Divert suspicion onto innocent inconsistencies and ensure fewer than 50% of detectives vote for you.",
                    category = ObjectiveCategory.CULPRIT_SPECIAL,
                    isPrimary = true,
                    isVerified = true,
                    points = 250
                )
            } else {
                PersonalObjective(
                    id = "obj_innocent_primary_${char.id}",
                    title = "Expose the True Contradiction",
                    description = "Cross-reference suspect testimonies with physical scene evidence to pinpoint who was at the crime scene.",
                    category = ObjectiveCategory.INVESTIGATION,
                    isPrimary = true,
                    isVerified = true,
                    points = 200
                )
            }

            val secondaryObj = when (i % 5) {
                0 -> PersonalObjective(
                    id = "obj_sec_social_${char.id}",
                    title = "Form a Coalition",
                    description = "Convince at least one other detective to exchange private information regarding their whereabouts.",
                    category = ObjectiveCategory.SOCIAL,
                    isPrimary = false,
                    points = 100
                )
                1 -> PersonalObjective(
                    id = "obj_sec_negotiation_${char.id}",
                    title = "Seal a Mutual Deal",
                    description = "Propose and successfully accept a strategic deal with a fellow suspect.",
                    category = ObjectiveCategory.NEGOTIATION,
                    isPrimary = false,
                    points = 100
                )
                2 -> PersonalObjective(
                    id = "obj_sec_secret_${char.id}",
                    title = "Conceal Private Motive",
                    description = "Do not allow other detectives to prove your personal secret during interrogation.",
                    category = ObjectiveCategory.SECRET_PROTECTION,
                    isPrimary = false,
                    points = 100
                )
                3 -> PersonalObjective(
                    id = "obj_sec_evidence_${char.id}",
                    title = "Documentary Discovery",
                    description = "Inspect written documents or records to establish exact timeline discrepancies.",
                    category = ObjectiveCategory.EVIDENCE_RELATED,
                    isPrimary = false,
                    points = 100
                )
                else -> PersonalObjective(
                    id = "obj_sec_manipulation_${char.id}",
                    title = "Testimony Scrutiny",
                    description = "Interrogate at least two different suspects regarding their timeline statements.",
                    category = ObjectiveCategory.INFORMATION_GATHERING,
                    isPrimary = false,
                    points = 100
                )
            }

            room.playerObjectives[player.playerId] = Pair(primaryObj, secondaryObj)
        }

        room.characterAssignments = assignments
        room.caseId = casePackage.identity.caseId

        transitionPhase(roomCode, RoomState.CASE_BRIEFING, 120)
        return assignments
    }

    fun getAssignedCharacter(
        roomCode: String,
        playerId: String,
        sessionToken: String
    ): Result<MyCharacterResponse> {
        val room = getRoomByCode(roomCode)
            ?: return Result.failure(IllegalArgumentException("Room $roomCode not found"))

        val player = room.players[playerId]
        if (player == null || player.sessionToken != sessionToken) {
            return Result.failure(IllegalAccessException("Invalid player credentials or session token"))
        }

        val caseId = room.caseId
            ?: return Result.failure(IllegalStateException("No case currently associated with room $roomCode"))

        val casePackage = caseRepository.getCase(caseId)
            ?: return Result.failure(IllegalStateException("Case $caseId not found in repository"))

        val charId = room.characterAssignments?.get(playerId)
            ?: return Result.failure(IllegalStateException("No character assigned to player $playerId"))

        val character = casePackage.characters.find { it.id == charId }
            ?: return Result.failure(IllegalStateException("Character $charId not found in case"))

        val isCulprit = casePackage.secretSolution?.culpritId == charId
        val directives = if (isCulprit) {
            "CULPRIT DIRECTIVE: Maintain your innocent persona. Divert suspicion toward innocent discrepancies and protect your secret."
        } else ""

        val objectives = room.playerObjectives[playerId]
        val solution = casePackage.secretSolution

        return Result.success(
            MyCharacterResponse(
                character = character,
                isCulprit = isCulprit,
                privateSecret = character.privateSecret,
                privateDirectives = directives,
                primaryObjective = objectives?.first,
                secondaryObjective = objectives?.second,
                actualSequenceOfEvents = if (isCulprit) solution?.method ?: "" else "",
                culpritMotive = if (isCulprit) solution?.motive ?: "" else "",
                culpritCoverStory = if (isCulprit) character.personalAlibi else "",
                exposingEvidenceIds = if (isCulprit) solution?.smokingGunEvidenceIds ?: emptyList() else emptyList(),
                caseIdentity = casePackage.identity
            )
        )
    }

    // --- REAL-TIME PRIVATE MESSAGING ---

    fun sendPrivateMessage(roomCode: String, request: SendMessageRequest): Result<PrivateMessage> {
        val room = getRoomByCode(roomCode) ?: return Result.failure(IllegalArgumentException("Room not found"))
        val sender = room.players[request.senderPlayerId]
        if (sender == null || sender.sessionToken != request.sessionToken) {
            return Result.failure(IllegalAccessException("Invalid credentials"))
        }

        if (room.state == RoomState.INTERROGATION_DEBATE || room.state == RoomState.VOTING || room.state == RoomState.CASE_RESOLUTION) {
            return Result.failure(IllegalStateException("Private messaging is locked during the debate and voting phases."))
        }

        val recipient = room.players[request.recipientPlayerId]
            ?: return Result.failure(IllegalArgumentException("Recipient not found in room"))

        val senderCharId = room.characterAssignments?.get(sender.playerId)
        val recipientCharId = room.characterAssignments?.get(recipient.playerId)
        val casePkg = room.caseId?.let { caseRepository.getCase(it) }

        val senderCharName = casePkg?.characters?.find { it.id == senderCharId }?.name ?: sender.playerName
        val recipientCharName = casePkg?.characters?.find { it.id == recipientCharId }?.name ?: recipient.playerName

        val message = PrivateMessage(
            id = "msg_${UUID.randomUUID().toString().take(8)}",
            roomCode = roomCode,
            senderPlayerId = sender.playerId,
            senderCharacterName = senderCharName,
            recipientPlayerId = recipient.playerId,
            recipientCharacterName = recipientCharName,
            text = request.text,
            messageType = request.messageType,
            attachedEvidenceId = request.attachedEvidenceId,
            dealDetails = request.dealDetails
        )

        room.privateMessages.add(message)

        // Send WebSocket update specifically to sender and recipient
        sendWsEvent(room, sender.playerId, WsEvent("NEW_MESSAGE", json.encodeToString(message)))
        sendWsEvent(room, recipient.playerId, WsEvent("NEW_MESSAGE", json.encodeToString(message)))

        return Result.success(message)
    }

    fun getPrivateMessages(roomCode: String, playerId: String, sessionToken: String): Result<List<PrivateMessage>> {
        val room = getRoomByCode(roomCode) ?: return Result.failure(IllegalArgumentException("Room not found"))
        val player = room.players[playerId]
        if (player == null || player.sessionToken != sessionToken) {
            return Result.failure(IllegalAccessException("Invalid credentials"))
        }

        val filtered = room.privateMessages.filter {
            it.senderPlayerId == playerId || it.recipientPlayerId == playerId
        }
        return Result.success(filtered)
    }

    // --- DEALS & NEGOTIATION ---

    fun proposeDeal(roomCode: String, request: ProposeDealRequest): Result<DealProposal> {
        val room = getRoomByCode(roomCode) ?: return Result.failure(IllegalArgumentException("Room not found"))
        val proposer = room.players[request.proposerPlayerId]
        if (proposer == null || proposer.sessionToken != request.sessionToken) {
            return Result.failure(IllegalAccessException("Invalid credentials"))
        }

        val deal = DealProposal(
            id = "deal_${UUID.randomUUID().toString().take(8)}",
            roomCode = roomCode,
            proposerPlayerId = request.proposerPlayerId,
            recipientPlayerId = request.recipientPlayerId,
            title = request.title,
            terms = request.terms,
            status = "PENDING"
        )

        room.dealProposals[deal.id] = deal
        sendWsEvent(room, request.recipientPlayerId, WsEvent("DEAL_PROPOSED", json.encodeToString(deal)))
        return Result.success(deal)
    }

    fun respondToDeal(roomCode: String, request: RespondDealRequest): Result<DealProposal> {
        val room = getRoomByCode(roomCode) ?: return Result.failure(IllegalArgumentException("Room not found"))
        val responder = room.players[request.responderPlayerId]
        if (responder == null || responder.sessionToken != request.sessionToken) {
            return Result.failure(IllegalAccessException("Invalid credentials"))
        }

        val deal = room.dealProposals[request.dealId]
            ?: return Result.failure(IllegalArgumentException("Deal not found"))

        if (deal.recipientPlayerId != request.responderPlayerId) {
            return Result.failure(IllegalAccessException("Not authorized to respond to this deal"))
        }

        val updated = deal.copy(status = if (request.accept) "ACCEPTED" else "REJECTED")
        room.dealProposals[deal.id] = updated
        sendWsEvent(room, deal.proposerPlayerId, WsEvent("DEAL_RESPONDED", json.encodeToString(updated)))
        return Result.success(updated)
    }

    // --- EVIDENCE DISCOVERY & SHARING ---

    fun recordEvidenceDiscovery(roomCode: String, playerId: String, evidenceId: String, makePublic: Boolean): Set<String> {
        val room = getRoomByCode(roomCode) ?: return emptySet()
        if (makePublic) {
            room.publicDiscoveredEvidenceIds.add(evidenceId)
            broadcastWs(room, WsEvent("EVIDENCE_DISCOVERED", evidenceId))
            broadcastRoomSnapshot(room)
        }
        return room.publicDiscoveredEvidenceIds
    }

    fun triggerMidGameTwist(roomCode: String): Result<MidGameTwist> {
        val room = getRoomByCode(roomCode) ?: return Result.failure(IllegalArgumentException("Room not found"))
        val casePkg = room.caseId?.let { caseRepository.getCase(it) }
            ?: return Result.failure(IllegalStateException("No case associated"))

        val twist = casePkg.midGameTwist ?: return Result.failure(IllegalStateException("No twist authored"))
        room.publicDiscoveredEvidenceIds.add(twist.revealedEvidence.id)
        room.state = RoomState.MID_GAME_TWIST

        broadcastWs(room, WsEvent("TWIST_ALERT", json.encodeToString(twist)))
        broadcastRoomSnapshot(room)
        return Result.success(twist)
    }

    // --- SECRET VOTING & CASE RESOLUTION ---

    fun submitBallotVote(roomCode: String, request: SubmitVoteRequest): Result<Boolean> {
        val room = getRoomByCode(roomCode) ?: return Result.failure(IllegalArgumentException("Room not found"))
        val voter = room.players[request.playerId]
        if (voter == null || voter.sessionToken != request.sessionToken) {
            return Result.failure(IllegalAccessException("Invalid credentials"))
        }

        val vote = BallotVote(
            voterPlayerId = request.playerId,
            accusedCulpritId = request.accusedCulpritId,
            methodAccusation = request.methodAccusation,
            motiveAccusation = request.motiveAccusation,
            smokingGunEvidenceId = request.smokingGunEvidenceId
        )

        room.ballotVotes[request.playerId] = vote
        broadcastWs(room, WsEvent("VOTE_RECEIVED", "${room.ballotVotes.size}/${room.frozenPlayerCount ?: room.players.size}"))

        // If all frozen players have voted, advance to resolution
        val totalExpected = room.frozenPlayerCount ?: room.players.size
        if (room.ballotVotes.size >= totalExpected) {
            tallyAndResolveCase(roomCode)
        }

        return Result.success(true)
    }

    fun tallyAndResolveCase(roomCode: String): Result<GameResolutionSummary> {
        val room = getRoomByCode(roomCode) ?: return Result.failure(IllegalArgumentException("Room not found"))
        val casePkg = room.caseId?.let { caseRepository.getCase(it) }
            ?: return Result.failure(IllegalStateException("No case"))

        val solution = casePkg.secretSolution ?: return Result.failure(IllegalStateException("No secret solution"))
        val culpritId = solution.culpritId
        val culpritProfile = casePkg.characters.find { it.id == culpritId }

        // Tally votes
        val voteCounts = mutableMapOf<String, Int>()
        room.ballotVotes.values.forEach { vote ->
            voteCounts[vote.accusedCulpritId] = (voteCounts[vote.accusedCulpritId] ?: 0) + 1
        }
        val topAccusedId = voteCounts.maxByOrNull { it.value }?.key ?: ""
        val isCulpritConvicted = topAccusedId == culpritId

        // Score each player
        val scoreCards = room.players.values.map { player ->
            val charId = room.characterAssignments?.get(player.playerId)
            val char = casePkg.characters.find { it.id == charId }
            val isCulprit = charId == culpritId
            val playerVote = room.ballotVotes[player.playerId]
            val votedCorrectly = playerVote?.accusedCulpritId == culpritId

            var score = 0
            val achievements = mutableListOf<String>()

            if (isCulprit) {
                if (!isCulpritConvicted) {
                    score += 500
                    achievements.add("The Perfect Alibi")
                }
                if (room.dealProposals.values.any { it.proposerPlayerId == player.playerId && it.status == "ACCEPTED" }) {
                    achievements.add("Master Manipulator")
                }
            } else {
                if (votedCorrectly) {
                    score += 300
                    achievements.add("The Uncovered Truth")
                }
                if (playerVote?.smokingGunEvidenceId in solution.smokingGunEvidenceIds) {
                    score += 150
                    achievements.add("Master Detective")
                }
            }

            // Secret Keeper achievement if never accused
            if ((voteCounts[charId] ?: 0) == 0) {
                score += 100
                achievements.add("Secret Keeper")
            } else if ((voteCounts[charId] ?: 0) >= 2) {
                achievements.add("Most Suspicious")
            }

            PlayerScoreCard(
                playerId = player.playerId,
                playerName = player.playerName,
                characterName = char?.name ?: player.playerName,
                isCulprit = isCulprit,
                primaryObjectiveCompleted = if (isCulprit) !isCulpritConvicted else votedCorrectly,
                secondaryObjectiveCompleted = achievements.isNotEmpty(),
                correctlyVotedCulprit = votedCorrectly,
                totalScore = score,
                earnedAchievements = achievements
            )
        }

        val summary = GameResolutionSummary(
            trueCulpritId = culpritId,
            trueCulpritName = culpritProfile?.name ?: "Unknown Culprit",
            trueMotive = solution.motive,
            trueMethod = solution.method,
            trueTimeline = solution.hiddenTimeline,
            smokingGunEvidence = casePkg.evidence.filter { it.id in solution.smokingGunEvidenceIds },
            misleadingCluesExplanation = solution.misleadingStrategy,
            culpritConfession = solution.finalVerdictExplanation,
            playerScores = scoreCards,
            isCulpritConvicted = isCulpritConvicted
        )

        room.resolutionSummary = summary
        transitionPhase(roomCode, RoomState.CASE_RESOLUTION, 300)
        broadcastWs(room, WsEvent("RESOLUTION", json.encodeToString(summary)))
        return Result.success(summary)
    }

    fun getResolutionSummary(roomCode: String): GameResolutionSummary? = getRoomByCode(roomCode)?.resolutionSummary

    fun setPlayerReady(roomCode: String, playerId: String, isReady: Boolean) {
        val room = getRoomByCode(roomCode) ?: return
        val player = room.players[playerId] ?: return
        room.players[playerId] = player.copy(isReady = isReady)
        broadcastRoomSnapshot(room)
    }

    fun registerWebSocket(roomCode: String, playerId: String, session: WebSocketSession) {
        val room = getRoomByCode(roomCode) ?: return
        room.webSockets[playerId] = session
        val player = room.players[playerId]
        if (player != null) {
            room.players[playerId] = player.copy(isConnected = true)
        }
        broadcastRoomSnapshot(room)
    }

    fun unregisterWebSocket(roomCode: String, playerId: String) {
        val room = getRoomByCode(roomCode) ?: return
        room.webSockets.remove(playerId)
        disconnectPlayer(roomCode, playerId)
    }

    private fun sendWsEvent(room: InternalRoom, playerId: String, event: WsEvent) {
        val session = room.webSockets[playerId] ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                session.send(Frame.Text(json.encodeToString(event)))
            } catch (_: Exception) {}
        }
    }

    private fun broadcastWs(room: InternalRoom, event: WsEvent) {
        val frameText = Frame.Text(json.encodeToString(event))
        CoroutineScope(Dispatchers.IO).launch {
            room.webSockets.values.forEach { session ->
                try {
                    session.send(frameText)
                } catch (_: Exception) {}
            }
        }
    }

    private fun broadcastRoomSnapshot(room: InternalRoom) {
        val snapshot = getPublicSnapshot(room.roomCode) ?: return
        broadcastWs(room, WsEvent("ROOM_SNAPSHOT", json.encodeToString(snapshot)))
    }

    fun disconnectPlayer(roomCode: String, playerId: String) {
        val room = getRoomByCode(roomCode) ?: return
        if (room.state == RoomState.WAITING_FOR_PLAYERS) {
            val player = room.players[playerId]
            if (player != null && !player.isHost) {
                room.players.remove(playerId)
            } else if (player != null) {
                room.players[playerId] = player.copy(isConnected = false)
            }
        } else {
            val player = room.players[playerId]
            if (player != null) {
                room.players[playerId] = player.copy(isConnected = false)
            }
        }
        broadcastRoomSnapshot(room)
    }

    fun getPublicSnapshot(roomCode: String): RoomPublicSnapshot? {
        val room = getRoomByCode(roomCode) ?: return null

        val playersList = room.players.values.map { session ->
            val charId = room.characterAssignments?.get(session.playerId)
            val charName = if (charId != null && room.caseId != null) {
                caseRepository.getCase(room.caseId!!)?.characters?.find { it.id == charId }?.name
            } else null

            PlayerPublicInfo(
                playerId = session.playerId,
                playerName = session.playerName,
                isHost = session.isHost,
                isReady = session.isReady,
                isConnected = session.isConnected,
                assignedCharacterId = charId,
                assignedCharacterName = charName
            )
        }.sortedBy { !it.isHost }

        val caseIdentity = if (room.caseId != null && room.state in listOf(
                RoomState.CASE_BRIEFING,
                RoomState.CHARACTER_REVEAL,
                RoomState.INVESTIGATION,
                RoomState.MID_GAME_TWIST,
                RoomState.INTERROGATION_DEBATE,
                RoomState.VOTING,
                RoomState.CASE_RESOLUTION
            )
        ) {
            caseRepository.getCase(room.caseId!!)?.identity
        } else null

        val generationStatus = if (room.generationJobId != null) {
            caseRepository.getJob(room.generationJobId!!)
        } else null

        return RoomPublicSnapshot(
            roomId = room.roomId,
            roomCode = room.roomCode,
            hostPlayerId = room.hostPlayerId,
            state = room.state,
            playerCount = room.players.size,
            frozenPlayerCount = room.frozenPlayerCount,
            players = playersList,
            generationJobId = room.generationJobId,
            generationStatus = generationStatus,
            caseIdentity = caseIdentity,
            timerState = getTimerState(roomCode),
            matchDurationMinutes = room.matchDurationMinutes,
            publicDiscoveredEvidenceIds = room.publicDiscoveredEvidenceIds.toList(),
            isChatLocked = room.state in listOf(RoomState.INTERROGATION_DEBATE, RoomState.VOTING, RoomState.CASE_RESOLUTION),
            errorMessage = room.errorMessage
        )
    }

    private fun generateUniqueRoomCode(): String {
        var code: String
        do {
            code = (100000 + Random.nextInt(900000)).toString()
        } while (roomsByCode.containsKey(code))
        return code
    }
}
