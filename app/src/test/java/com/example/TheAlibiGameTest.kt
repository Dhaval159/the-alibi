package com.example

import com.example.data.TheAlibiRepository
import com.example.model.CaseGenerationConfig
import com.example.model.InterrogationQnA
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TheAlibiGameTest {

    private lateinit var repository: TheAlibiRepository

    @Before
    fun setUp() {
        repository = TheAlibiRepository()
    }

    @Test
    fun `repository starts with empty case library on fresh instance`() {
        val freshRepo = TheAlibiRepository()
        // Verifying no case001 is auto-generated on construction
        assertTrue(freshRepo.savedCases.value.isEmpty())
        assertEquals(0, freshRepo.savedCases.value.size)
    }

    @Test
    fun `case library can be populated via case generation`() = runBlocking {
        val config = CaseGenerationConfig(
            theme = "Luxury train mystery",
            difficulty = "Hard",
            playerCount = 7,
            atmosphere = "Suspense thriller",
            customPrompt = "The Orient Express Midnight Affair"
        )

        var lastProgress = 0
        val generated = repository.generateCase(config) { progress, _ ->
            lastProgress = progress
        }

        assertEquals(100, lastProgress)
        assertNotNull(generated)
        assertTrue(generated.identity.title.contains("Orient Express") || generated.identity.title.contains("Mystery"))
        assertEquals("Hard", generated.identity.difficulty)
        assertTrue(repository.savedCases.value.isNotEmpty())
    }

    @Test
    fun `player assignment and shoulder-surfing conceal toggle work`() {
        val defaultCase = repository.createGeneratedCase(CaseGenerationConfig())
        repository.selectCase(defaultCase, defaultCase.characters.first().id)
        val assigned = repository.assignedCharacter.value
        assertNotNull(assigned)
        assertNotNull(assigned!!.id)
        assertTrue(assigned.isCulprit == true || assigned.isCulprit == false)

        assertFalse(repository.isDossierConcealed.value)
        repository.toggleConcealDossier()
        assertTrue(repository.isDossierConcealed.value)
        repository.toggleConcealDossier()
        assertFalse(repository.isDossierConcealed.value)
    }

    @Test
    fun `hotspot inspection collects associated forensic evidence`() {
        val defaultCase = repository.createGeneratedCase(CaseGenerationConfig())
        repository.selectCase(defaultCase, defaultCase.characters.first().id)
        val initialEvidenceCount = repository.discoveredEvidenceIds.value.size
        // Inspect desk hotspot which has associated evidence ev_poisoned_decanter
        repository.inspectHotspot("hs_desk")
        assertTrue("hs_desk" in repository.inspectedHotspotIds.value)
        assertTrue("ev_poisoned_decanter" in repository.discoveredEvidenceIds.value)
        assertTrue(repository.discoveredEvidenceIds.value.size >= initialEvidenceCount)
    }

    @Test
    fun `interrogation records testimony and detects contradictions`() {
        val suspectId = "char_edward_vance"
        val qna = InterrogationQnA(
            question = "We found red clay from the study terrace on your shoes, Mr. Vance.",
            answer = "Ah... well, I might have stepped near the terrace doors briefly when listening to the storm, but I never crossed the threshold!",
            requiredEvidenceToTrigger = "ev_muddy_footprints",
            revealsContradiction = true
        )

        repository.askInterrogationQuestion(suspectId, qna)
        val history = repository.interrogationHistory.value[suspectId]
        assertNotNull(history)
        assertEquals(1, history?.size)
        assertTrue(history?.first()?.revealsContradiction == true)

        // Verifies deduction note was automatically generated
        val notes = repository.deductionNotes.value
        assertTrue(notes.any { it.text.contains("CONTRADICTION DISCOVERED") })
    }

    @Test
    fun `investigation board pins connection threads between suspects and clues`() {
        repository.addBoardConnection(
            suspectId = "char_edward_vance",
            evidenceId = "ev_muddy_footprints",
            label = "Contradicts Conservatory Alibi"
        )
        val connections = repository.boardConnections.value
        assertEquals(1, connections.size)
        assertEquals("char_edward_vance", connections.first().suspectId)
        assertEquals("ev_muddy_footprints", connections.first().evidenceId)

        repository.removeBoardConnection(connections.first().id)
        assertEquals(0, repository.boardConnections.value.size)
    }

    @Test
    fun `case generation produces new playable mystery`() = runBlocking {
        val config = CaseGenerationConfig(
            theme = "Luxury train mystery",
            difficulty = "Hard",
            playerCount = 7,
            atmosphere = "Suspense thriller",
            customPrompt = "The Orient Express Midnight Affair"
        )

        var lastProgress = 0
        val generated = repository.generateCase(config) { progress, _ ->
            lastProgress = progress
        }

        assertEquals(100, lastProgress)
        assertNotNull(generated)
        assertTrue(generated.identity.title.contains("Orient Express") || generated.identity.title.contains("Mystery"))
        assertEquals("Hard", generated.identity.difficulty)
        assertTrue(repository.savedCases.value.isNotEmpty())
    }

    @Test
    fun `full investigation playability cycle from briefing to final conviction`() {
        // Step 1: Ensure we have an active case
        val casePkg = repository.activeCase.value
            ?: repository.createGeneratedCase(CaseGenerationConfig())

        val culpritChar = casePkg.characters.first { it.isCulprit }
        repository.selectCase(casePkg, culpritChar.id)

        // Step 2: Private Dossier Reveal
        val playerChar = repository.assignedCharacter.value!!
        assertNotNull(playerChar.id)

        // Step 3: Crime Scene Inspection & Evidence Collection
        repository.inspectHotspot("hs_desk")
        repository.inspectHotspot("hs_terrace")
        assertTrue("ev_muddy_footprints" in repository.discoveredEvidenceIds.value)

        // Step 4: Suspect Interrogation
        repository.askInterrogationQuestion(
            culpritChar.id,
            InterrogationQnA(
                question = "Where were you between 23:00 and 23:45?",
                answer = "I was strolling through the conservatory.",
                revealsContradiction = true
            )
        )

        // Step 5: Investigation Board deduction
        repository.addBoardConnection(
            culpritChar.id,
            "ev_muddy_footprints",
            "Breaks Alibi"
        )
        repository.addDeductionNote("Generated suspect's alibi of being in the conservatory is shattered by terrace clay footprints.")

        // Step 6: Accusation & Verdict
        val verdict = repository.submitAccusation(
            accusedId = culpritChar.id,
            motiveText = "Desperate debt & altered will disinheritance",
            smokingGunEvidenceId = "ev_muddy_footprints"
        )

        // Step 7: Final Resolution Validation
        assertTrue(verdict.isCorrect)
        assertTrue(verdict.trueCulpritName.isNotBlank())
        assertTrue(verdict.accusedCharacterName.isNotBlank())
        assertTrue(verdict.smokingGunConfirmed)
        assertTrue(verdict.motiveConfirmed)
        assertFalse(verdict.finalVerdictExplanation.isBlank())
    }

    @Test
    fun `room creation generates 6-digit code and marks host`() {
        val room = repository.createRoom(
            com.example.model.CreateRoomParams(
                hostPlayerName = "Host Vance"
            )
        )
        assertNotNull(room)
        assertEquals(6, room.roomCode.length)
        assertTrue(room.roomCode.all { it.isDigit() })
        assertEquals(1, room.playerCount)
        assertTrue(repository.isHost.value)
        assertEquals(com.example.model.RoomState.WAITING_FOR_PLAYERS, room.state)
    }

    @Test
    fun `join room adds player and updates lobby count`() {
        val room = repository.createRoom(
            com.example.model.CreateRoomParams(hostPlayerName = "Host 1")
        )
        val joinResult = repository.joinRoom(
            com.example.model.JoinRoomParams(
                roomCode = room.roomCode,
                playerName = "Guest 2"
            )
        )
        assertTrue(joinResult.isSuccess)
        val updated = joinResult.getOrThrow()
        assertEquals(2, updated.playerCount)
        assertEquals(2, updated.players.size)
        assertTrue(updated.players.any { it.playerName == "Guest 2" && !it.isHost })
    }

    @Test
    fun `generation rejected when fewer than 4 players`() = runBlocking {
        repository.createRoom(
            com.example.model.CreateRoomParams(hostPlayerName = "Solo Host")
        )
        val result = repository.generateRoomMystery(
            com.example.model.CaseGenerationConfig(playerCount = 1)
        ) { _, _ -> }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Minimum 4 players") == true)
    }

    @Test
    fun `exact player count mystery generation for 5 players freezes roster and assigns characters`() = runBlocking {
        val room = repository.createRoom(
            com.example.model.CreateRoomParams(hostPlayerName = "Detective 1")
        )
        repository.addMockInvestigator("Detective 2")
        repository.addMockInvestigator("Detective 3")
        repository.addMockInvestigator("Detective 4")
        repository.addMockInvestigator("Detective 5")

        assertEquals(5, repository.currentRoom.value?.players?.size)

        var lastProgress = 0
        var lastStage = ""
        val result = repository.generateRoomMystery(
            com.example.model.CaseGenerationConfig(playerCount = 5)
        ) { progress, stage ->
            lastProgress = progress
            lastStage = stage
        }

        assertTrue(result.isSuccess)
        val generatedCase = result.getOrThrow()
        assertEquals(5, generatedCase.characters.size)
        assertEquals(5, repository.currentRoom.value?.frozenPlayerCount)
        assertEquals(com.example.model.RoomState.CASE_BRIEFING, repository.currentRoom.value?.state)

        // Verify local player got a secret character assignment
        val assigned = repository.myAssignedCharacter.value
        assertNotNull(assigned)
        assertFalse(assigned?.character?.name.isNullOrBlank())

        // Verify room is now locked and refuses new joins
        val lateJoin = repository.joinRoom(
            com.example.model.JoinRoomParams(
                roomCode = room.roomCode,
                playerName = "Late Player"
            )
        )
        assertTrue(lateJoin.isFailure)
        assertTrue(lateJoin.exceptionOrNull()?.message?.contains("no longer accepting players") == true)
    }

    @Test
    fun `five manor locations are available and can be explored`() {
        val casePkg = repository.activeCase.value
            ?: repository.createGeneratedCase(CaseGenerationConfig())

        assertNotNull(casePkg)
        assertTrue(casePkg.locations.size >= 5)
        assertTrue(casePkg.locations.any { it.id == "loc_study" && it.isPrimaryCrimeScene })
        assertTrue(casePkg.locations.any { it.id == "loc_dining" })
        assertTrue(casePkg.locations.any { it.id == "loc_terrace" })
        assertTrue(casePkg.locations.any { it.id == "loc_billiards" })
        assertTrue(casePkg.locations.any { it.id == "loc_drawing" })

        repository.setActiveLocation("loc_terrace")
        assertEquals("loc_terrace", repository.activeLocationId.value)
    }

    @Test
    fun `private messaging and evidence attachment updates sharing visibility`() {
        val defaultCase = repository.createGeneratedCase(CaseGenerationConfig())
        repository.selectCase(defaultCase, defaultCase.characters.first().id)

        val msg = repository.sendPrivateMessage(
            recipientPlayerId = "player_ally_1",
            text = "I found red clay footprints near the terrace.",
            attachedEvidenceId = "ev_muddy_footprints"
        )
        assertNotNull(msg)
        assertEquals("EVIDENCE_SHARE", msg.messageType)
        assertEquals("ev_muddy_footprints", msg.attachedEvidenceId)
        assertTrue(repository.privateMessages.value.any { it.id == msg.id })

        val ev = repository.activeCase.value?.evidence?.find { it.id == "ev_muddy_footprints" }
        assertEquals(com.example.model.EvidenceVisibility.SHARED, ev?.visibility)
        assertTrue("player_ally_1" in (ev?.sharedWithPlayerIds ?: emptyList()))
    }

    @Test
    fun `deal proposal and acceptance flow`() {
        val deal = repository.proposeDeal(
            recipientPlayerId = "player_partner",
            title = "Alibi Accord",
            terms = "You vouch for my presence in the conservatory."
        )
        assertNotNull(deal)
        assertEquals("PENDING", deal.status)
        assertTrue(repository.dealProposals.value.any { it.id == deal.id })

        // Partner accepts deal
        repository.respondToDeal(deal.id, true)
        val accepted = repository.dealProposals.value.find { it.id == deal.id }
        assertEquals("ACCEPTED", accepted?.status)
    }

    @Test
    fun `mid-game twist reveals new clue and triggers event`() {
        repository.triggerMidGameTwist()
        val twist = repository.activeTwist.value
        assertNotNull(twist)
        assertEquals("twist_blackwood_cut", twist?.id)
        assertTrue("ev_midnight_cipher_note" in repository.discoveredEvidenceIds.value)
        assertFalse(repository.isTwistDismissed.value)

        repository.dismissTwist()
        assertTrue(repository.isTwistDismissed.value)
    }

    @Test
    fun `secret ballot voting and full case resolution scoring`() {
        repository.submitBallotVote(
            accusedCulpritId = "char_edward_vance",
            method = "Laced belladonna into vintage scotch decanter",
            motive = "Desperate debts of 40,000 pounds due at midnight",
            smokingGunEvidenceId = "ev_muddy_footprints"
        )
        assertTrue(repository.hasVoted.value)

        val resolution = repository.tallyAndResolveCase()
        assertNotNull(resolution)
        assertEquals("char_edward_vance", resolution.trueCulpritId)
        assertTrue(resolution.isCulpritConvicted)
        assertTrue(resolution.playerScores.isNotEmpty())
        assertTrue(resolution.playerScores.first().earnedAchievements.isNotEmpty())
    }

    @Test
    fun `sound manager independent toggles`() {
        val soundManager = com.example.util.SoundManager()
        assertTrue(soundManager.isMusicEnabled.value)
        assertTrue(soundManager.isSfxEnabled.value)
        assertTrue(soundManager.isNarrationEnabled.value)

        soundManager.toggleMusic()
        assertFalse(soundManager.isMusicEnabled.value)

        soundManager.toggleSfx()
        assertFalse(soundManager.isSfxEnabled.value)

        soundManager.toggleNarration()
        assertFalse(soundManager.isNarrationEnabled.value)

        soundManager.release()
    }
}
