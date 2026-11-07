package com.thealibi.server

import com.thealibi.server.gemini.CaseGenerationEngine
import com.thealibi.server.gemini.GeminiClient
import com.thealibi.server.models.*
import com.thealibi.server.storage.CaseRepository
import com.thealibi.server.storage.RoomRepository
import com.thealibi.server.validation.CaseValidator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RoomGenerationTest {

    private lateinit var caseRepository: CaseRepository
    private lateinit var roomRepository: RoomRepository
    private lateinit var validator: CaseValidator
    private lateinit var engine: CaseGenerationEngine

    @BeforeEach
    fun setUp() {
        validator = CaseValidator()
        caseRepository = CaseRepository(validator)
        roomRepository = RoomRepository(caseRepository)
        engine = CaseGenerationEngine(GeminiClient("fake_test_key"), validator)
    }

    @Test
    fun `test 1 - four players join and generate exact 4-player case`() {
        val (room, host) = roomRepository.createRoom("Host Detective")
        roomRepository.joinRoom(room.roomCode, "Player 2")
        roomRepository.joinRoom(room.roomCode, "Player 3")
        roomRepository.joinRoom(room.roomCode, "Player 4")

        val freezeResult = roomRepository.freezeRosterForGeneration(room.roomCode, host.playerId, host.sessionToken)
        assertTrue(freezeResult.isSuccess)
        val (_, count) = freezeResult.getOrThrow()
        assertEquals(4, count)

        val casePkg = engine.generateProceduralMasterCase(CaseGenerationRequest(playerCount = count))
        val validation = validator.validateForPlayerCount(casePkg, count)
        assertTrue(validation.isValid, "Validation failed: ${validation.errors}")
        assertEquals(4, casePkg.playerCountConfigs[4]?.assignedHumanCharacterIds?.size)

        caseRepository.saveCase(casePkg)
        val assignments = roomRepository.assignCharacters(room.roomCode, casePkg)
        assertEquals(4, assignments.size)
        assertEquals(RoomState.CASE_BRIEFING, roomRepository.getRoomByCode(room.roomCode)?.state)
    }

    @Test
    fun `test 2 - five players join and generate exact 5-player case`() {
        val (room, host) = roomRepository.createRoom("Host")
        repeat(4) { i -> roomRepository.joinRoom(room.roomCode, "Player ${i + 2}") }

        val freezeResult = roomRepository.freezeRosterForGeneration(room.roomCode, host.playerId, host.sessionToken)
        assertTrue(freezeResult.isSuccess)
        val (_, count) = freezeResult.getOrThrow()
        assertEquals(5, count)

        val casePkg = engine.generateProceduralMasterCase(CaseGenerationRequest(playerCount = count))
        val validation = validator.validateForPlayerCount(casePkg, count)
        assertTrue(validation.isValid)
        assertEquals(5, casePkg.playerCountConfigs[5]?.assignedHumanCharacterIds?.size)
    }

    @Test
    fun `test 3 - eight players join and generate exact 8-player case`() {
        val (room, host) = roomRepository.createRoom("Host")
        repeat(7) { i -> roomRepository.joinRoom(room.roomCode, "Player ${i + 2}") }

        val freezeResult = roomRepository.freezeRosterForGeneration(room.roomCode, host.playerId, host.sessionToken)
        assertTrue(freezeResult.isSuccess)
        val (_, count) = freezeResult.getOrThrow()
        assertEquals(8, count)

        val casePkg = engine.generateProceduralMasterCase(CaseGenerationRequest(playerCount = count))
        val validation = validator.validateForPlayerCount(casePkg, count)
        assertTrue(validation.isValid)
        assertEquals(8, casePkg.playerCountConfigs[8]?.assignedHumanCharacterIds?.size)
    }

    @Test
    fun `test 4 - three players request generation is rejected (minimum 4)`() {
        val (room, host) = roomRepository.createRoom("Host")
        roomRepository.joinRoom(room.roomCode, "Player 2")
        roomRepository.joinRoom(room.roomCode, "Player 3")

        val result = roomRepository.freezeRosterForGeneration(room.roomCode, host.playerId, host.sessionToken)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Minimum 4 players") == true)
        assertEquals(RoomState.WAITING_FOR_PLAYERS, roomRepository.getRoomByCode(room.roomCode)?.state)
    }

    @Test
    fun `test 5 - eleven players is rejected because maximum supported is 10`() {
        val (room, host) = roomRepository.createRoom("Host")
        repeat(9) { i ->
            val res = roomRepository.joinRoom(room.roomCode, "Player ${i + 2}")
            assertTrue(res.isSuccess)
        }
        assertEquals(10, room.players.size)

        // Attempting 11th join
        val overflow = roomRepository.joinRoom(room.roomCode, "Player 11")
        assertTrue(overflow.isFailure)
        assertTrue(overflow.exceptionOrNull()?.message?.contains("full") == true)
    }

    @Test
    fun `test 6 - non-host request generation is rejected`() {
        val (room, _) = roomRepository.createRoom("Host")
        val (_, player2) = roomRepository.joinRoom(room.roomCode, "Player 2").getOrThrow()
        roomRepository.joinRoom(room.roomCode, "Player 3")
        roomRepository.joinRoom(room.roomCode, "Player 4")

        val result = roomRepository.freezeRosterForGeneration(room.roomCode, player2.playerId, player2.sessionToken)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalAccessException)
    }

    @Test
    fun `test 7 - second generation request returns existing job without duplicate generation`() {
        val (room, host) = roomRepository.createRoom("Host")
        repeat(3) { i -> roomRepository.joinRoom(room.roomCode, "Player ${i + 2}") }

        val first = roomRepository.freezeRosterForGeneration(room.roomCode, host.playerId, host.sessionToken)
        assertTrue(first.isSuccess)
        roomRepository.setGenerationJob(room.roomCode, "job_test_123")

        val second = roomRepository.freezeRosterForGeneration(room.roomCode, host.playerId, host.sessionToken)
        assertTrue(second.isSuccess)
        assertEquals("job_test_123", roomRepository.getRoomByCode(room.roomCode)?.generationJobId)
    }

    @Test
    fun `test 8 - player attempts to join after generation begins is rejected`() {
        val (room, host) = roomRepository.createRoom("Host")
        repeat(3) { i -> roomRepository.joinRoom(room.roomCode, "Player ${i + 2}") }

        roomRepository.freezeRosterForGeneration(room.roomCode, host.playerId, host.sessionToken)

        val lateJoin = roomRepository.joinRoom(room.roomCode, "Late Player")
        assertTrue(lateJoin.isFailure)
        assertTrue(lateJoin.exceptionOrNull()?.message?.contains("no longer accepting players") == true)
    }

    @Test
    fun `test 9 - roster remains frozen throughout generation`() {
        val (room, host) = roomRepository.createRoom("Host")
        repeat(4) { i -> roomRepository.joinRoom(room.roomCode, "Player ${i + 2}") }

        roomRepository.freezeRosterForGeneration(room.roomCode, host.playerId, host.sessionToken)
        val frozen = roomRepository.getRoomByCode(room.roomCode)?.frozenRoster
        assertNotNull(frozen)
        assertEquals(5, frozen?.size)
    }

    @Test
    fun `test 10 - disconnect during generation does not alter reserved player count`() {
        val (room, host) = roomRepository.createRoom("Host")
        val (_, p2) = roomRepository.joinRoom(room.roomCode, "Player 2").getOrThrow()
        roomRepository.joinRoom(room.roomCode, "Player 3")
        roomRepository.joinRoom(room.roomCode, "Player 4")

        roomRepository.freezeRosterForGeneration(room.roomCode, host.playerId, host.sessionToken)
        assertEquals(4, roomRepository.getRoomByCode(room.roomCode)?.frozenPlayerCount)

        roomRepository.disconnectPlayer(room.roomCode, p2.playerId)
        assertEquals(4, roomRepository.getRoomByCode(room.roomCode)?.frozenPlayerCount)
        assertFalse(room.players[p2.playerId]?.isConnected ?: true)
    }

    @Test
    fun `test 11 - exactly one culprit is assigned to a human player`() {
        val (room, host) = roomRepository.createRoom("Host")
        repeat(4) { i -> roomRepository.joinRoom(room.roomCode, "Player ${i + 2}") }

        val (_, count) = roomRepository.freezeRosterForGeneration(room.roomCode, host.playerId, host.sessionToken).getOrThrow()
        val casePkg = engine.generateProceduralMasterCase(CaseGenerationRequest(playerCount = count))
        caseRepository.saveCase(casePkg)

        val assignments = roomRepository.assignCharacters(room.roomCode, casePkg)
        val culpritCharId = casePkg.secretSolution!!.culpritId

        val culpritAssignedPlayers = assignments.filter { it.value == culpritCharId }
        assertEquals(1, culpritAssignedPlayers.size, "Exactly one human player must be assigned the culprit character")
    }

    @Test
    fun `test 12 - every player receives a unique character`() {
        val (room, host) = roomRepository.createRoom("Host")
        repeat(5) { i -> roomRepository.joinRoom(room.roomCode, "Player ${i + 2}") } // 6 players

        val (_, count) = roomRepository.freezeRosterForGeneration(room.roomCode, host.playerId, host.sessionToken).getOrThrow()
        val casePkg = engine.generateProceduralMasterCase(CaseGenerationRequest(playerCount = count))
        caseRepository.saveCase(casePkg)

        val assignments = roomRepository.assignCharacters(room.roomCode, casePkg)
        assertEquals(6, assignments.size)
        val assignedCharacterIds = assignments.values.toSet()
        assertEquals(6, assignedCharacterIds.size, "All character assignments must be unique")
    }

    @Test
    fun `test 13 - no player receives another character private data`() {
        val (room, host) = roomRepository.createRoom("Host")
        val (_, p2) = roomRepository.joinRoom(room.roomCode, "Player 2").getOrThrow()
        roomRepository.joinRoom(room.roomCode, "Player 3")
        roomRepository.joinRoom(room.roomCode, "Player 4")

        val (_, count) = roomRepository.freezeRosterForGeneration(room.roomCode, host.playerId, host.sessionToken).getOrThrow()
        val casePkg = engine.generateProceduralMasterCase(CaseGenerationRequest(playerCount = count))
        caseRepository.saveCase(casePkg)
        roomRepository.assignCharacters(room.roomCode, casePkg)

        val hostChar = roomRepository.getAssignedCharacter(room.roomCode, host.playerId, host.sessionToken).getOrThrow()
        val p2Char = roomRepository.getAssignedCharacter(room.roomCode, p2.playerId, p2.sessionToken).getOrThrow()

        assertNotEquals(hostChar.character.id, p2Char.character.id)
        assertFalse(hostChar.privateSecret.isBlank())
        assertFalse(p2Char.privateSecret.isBlank())
        assertNotEquals(hostChar.privateSecret, p2Char.privateSecret)
    }

    @Test
    fun `test 14 - public snapshots never expose the solution or culprit`() {
        val (room, host) = roomRepository.createRoom("Host")
        repeat(3) { i -> roomRepository.joinRoom(room.roomCode, "Player ${i + 2}") }

        val (_, count) = roomRepository.freezeRosterForGeneration(room.roomCode, host.playerId, host.sessionToken).getOrThrow()
        val casePkg = engine.generateProceduralMasterCase(CaseGenerationRequest(playerCount = count))
        caseRepository.saveCase(casePkg)
        roomRepository.assignCharacters(room.roomCode, casePkg)

        val snapshot = roomRepository.getPublicSnapshot(room.roomCode)
        assertNotNull(snapshot)
        assertNotNull(snapshot?.caseIdentity)
        val publicCase = caseRepository.getPublicCase(casePkg.identity.caseId)
        assertNull(publicCase?.secretSolution, "Public case snapshot must never expose secretSolution")
    }

    @Test
    fun `test 15 - generation failure is recoverable`() {
        val (room, host) = roomRepository.createRoom("Host")
        repeat(3) { i -> roomRepository.joinRoom(room.roomCode, "Player ${i + 2}") }

        roomRepository.freezeRosterForGeneration(room.roomCode, host.playerId, host.sessionToken)
        roomRepository.updateRoomState(room.roomCode, RoomState.WAITING_FOR_PLAYERS, "API timeout")

        val current = roomRepository.getRoomByCode(room.roomCode)
        assertEquals(RoomState.WAITING_FOR_PLAYERS, current?.state)
        assertEquals("API timeout", current?.errorMessage)

        // Host can retry
        val retry = roomRepository.freezeRosterForGeneration(room.roomCode, host.playerId, host.sessionToken)
        assertTrue(retry.isSuccess)
    }

    @Test
    fun `test 16 - successful generation transitions room to briefing state`() {
        val (room, host) = roomRepository.createRoom("Host")
        repeat(3) { i -> roomRepository.joinRoom(room.roomCode, "Player ${i + 2}") }

        val (_, count) = roomRepository.freezeRosterForGeneration(room.roomCode, host.playerId, host.sessionToken).getOrThrow()
        val casePkg = engine.generateProceduralMasterCase(CaseGenerationRequest(playerCount = count))
        caseRepository.saveCase(casePkg)
        roomRepository.assignCharacters(room.roomCode, casePkg)

        val updated = roomRepository.getRoomByCode(room.roomCode)
        assertEquals(RoomState.CASE_BRIEFING, updated?.state)
        assertEquals(casePkg.identity.caseId, updated?.caseId)
    }

    @Test
    fun `test 17 - private messaging enforces authorization and recipient privacy`() {
        val (room, host) = roomRepository.createRoom("Host")
        val (_, p2) = roomRepository.joinRoom(room.roomCode, "Player 2").getOrThrow()
        val (_, p3) = roomRepository.joinRoom(room.roomCode, "Player 3").getOrThrow()
        val (_, p4) = roomRepository.joinRoom(room.roomCode, "Player 4").getOrThrow()

        val (_, count) = roomRepository.freezeRosterForGeneration(room.roomCode, host.playerId, host.sessionToken).getOrThrow()
        val casePkg = engine.generateProceduralMasterCase(CaseGenerationRequest(playerCount = count))
        caseRepository.saveCase(casePkg)
        roomRepository.assignCharacters(room.roomCode, casePkg)
        roomRepository.transitionPhase(room.roomCode, RoomState.INVESTIGATION, 300)

        // Host sends private message to Player 2
        val sendResult = roomRepository.sendPrivateMessage(
            room.roomCode,
            SendMessageRequest(
                senderPlayerId = host.playerId,
                sessionToken = host.sessionToken,
                recipientPlayerId = p2.playerId,
                text = "Did you check the study terrace?"
            )
        )
        assertTrue(sendResult.isSuccess)

        // Host and Player 2 can read the message
        val hostMsgs = roomRepository.getPrivateMessages(room.roomCode, host.playerId, host.sessionToken).getOrThrow()
        assertEquals(1, hostMsgs.size)
        val p2Msgs = roomRepository.getPrivateMessages(room.roomCode, p2.playerId, p2.sessionToken).getOrThrow()
        assertEquals(1, p2Msgs.size)

        // Player 3 cannot see this conversation
        val p3Msgs = roomRepository.getPrivateMessages(room.roomCode, p3.playerId, p3.sessionToken).getOrThrow()
        assertEquals(0, p3Msgs.size)
    }

    @Test
    fun `test 18 - server timer manages phase countdown correctly`() {
        val (room, host) = roomRepository.createRoom("Host")
        repeat(3) { i -> roomRepository.joinRoom(room.roomCode, "Player ${i + 2}") }

        val timer = roomRepository.transitionPhase(room.roomCode, RoomState.INVESTIGATION, 300)
        assertNotNull(timer)
        assertEquals(RoomState.INVESTIGATION, timer?.phase)
        assertEquals(300, timer?.durationSeconds)

        val retrieved = roomRepository.getTimerState(room.roomCode)
        assertNotNull(retrieved)
        assertTrue(retrieved!!.remainingSeconds in 298..300)
    }

    @Test
    fun `test 19 - secret voting tally and resolution accurately calculates results`() {
        val (room, host) = roomRepository.createRoom("Host")
        val (_, p2) = roomRepository.joinRoom(room.roomCode, "Player 2").getOrThrow()
        val (_, p3) = roomRepository.joinRoom(room.roomCode, "Player 3").getOrThrow()
        val (_, p4) = roomRepository.joinRoom(room.roomCode, "Player 4").getOrThrow()

        val (_, count) = roomRepository.freezeRosterForGeneration(room.roomCode, host.playerId, host.sessionToken).getOrThrow()
        val casePkg = engine.generateProceduralMasterCase(CaseGenerationRequest(playerCount = count))
        caseRepository.saveCase(casePkg)
        val assignments = roomRepository.assignCharacters(room.roomCode, casePkg)
        val culpritCharId = casePkg.secretSolution!!.culpritId

        roomRepository.transitionPhase(room.roomCode, RoomState.VOTING, 120)

        // Submit votes
        listOf(host, p2, p3).forEach { p ->
            roomRepository.submitBallotVote(
                room.roomCode,
                SubmitVoteRequest(
                    playerId = p.playerId,
                    sessionToken = p.sessionToken,
                    accusedCulpritId = culpritCharId,
                    methodAccusation = "Poison in decanter",
                    motiveAccusation = "Inheritance greed",
                    smokingGunEvidenceId = "ev_silver_cigarette_case"
                )
            )
        }
        roomRepository.submitBallotVote(
            room.roomCode,
            SubmitVoteRequest(
                playerId = p4.playerId,
                sessionToken = p4.sessionToken,
                accusedCulpritId = "char_beatrice_vance",
                methodAccusation = "Poison",
                motiveAccusation = "Spite"
            )
        )

        val resolution = roomRepository.getResolutionSummary(room.roomCode)
        assertNotNull(resolution)
        assertTrue(resolution!!.isCulpritConvicted)
        assertEquals(culpritCharId, resolution.trueCulpritId)
        assertEquals(4, resolution.playerScores.size)
    }
}
