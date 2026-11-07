package com.example

import com.example.data.GameRepository
import com.example.data.LocalRoomStore
import com.example.data.TheAlibiRepository
import com.example.model.ConnectionState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Verifies that the local repository implementation of [GameRepository] fulfills
 * all architectural seam contracts required for Phase 0.
 */
class LocalRepositoryTest {

    private lateinit var sharedStore: LocalRoomStore
    private lateinit var repository: GameRepository

    @Before
    fun setUp() {
        sharedStore = LocalRoomStore()
        repository = TheAlibiRepository(roomStore = sharedStore)
    }

    @Test
    fun `local repository exposes valid local auth identity and disconnected state`() = runBlocking {
        val identity = repository.authIdentity.first()
        assertNotNull(identity)
        assertEquals("local_demo_player", identity?.uid)
        assertFalse("Local identity must not claim to be a live Firebase Auth identity", identity?.isOnlineAuth ?: true)
        assertTrue(identity?.isAnonymous == true)

        val connection = repository.connectionState.first()
        // Local mode correctly reports Disconnected; online mode would be Connected
        assertTrue(connection is ConnectionState.Disconnected)
    }

    @Test
    fun `player can create a room through suspend abstraction`() = runBlocking {
        val result = repository.createRoom("Host Detective")
        assertTrue(result.isSuccess)

        val session = result.getOrThrow()
        assertNotNull(session.roomId)
        assertEquals(6, session.roomCode.length)
        assertTrue(session.isHost)

        val room = repository.currentRoom.value
        assertNotNull(room)
        assertEquals(session.roomId, room?.roomId)
        assertEquals(1, room?.playerCount)
        assertTrue(repository.isHost.value)
    }

    @Test
    fun `second player can join a room through suspend abstraction`() = runBlocking {
        val hostSession = repository.createRoom("Host Detective").getOrThrow()

        // Create a second repository instance sharing the store to simulate local multiplayer joining
        val secondPlayerRepo: GameRepository = TheAlibiRepository(roomStore = sharedStore)
        val joinResult = secondPlayerRepo.joinRoom(hostSession.roomCode, "Detective Partner")
        assertTrue(joinResult.isSuccess)

        val guestSession = joinResult.getOrThrow()
        assertFalse(guestSession.isHost)
        assertEquals(hostSession.roomCode, guestSession.roomCode)

        val guestRoom = secondPlayerRepo.currentRoom.value
        assertEquals(2, guestRoom?.playerCount)
    }

    @Test
    fun `room exposes correct player list and host designation`() = runBlocking {
        val session = repository.createRoom("Chief Inspector").getOrThrow()

        repository.addMockInvestigator("Investigator Adams")
        repository.addMockInvestigator("Investigator Baker")

        val players = repository.observePlayers(session.roomId).first()
        assertEquals(3, players.size)
        assertTrue(players[0].isHost)
        assertEquals("Chief Inspector", players[0].playerName)
        assertFalse(players[1].isHost)
        assertEquals("Investigator Adams", players[1].playerName)
        assertFalse(players[2].isHost)
        assertEquals("Investigator Baker", players[2].playerName)
    }

    @Test
    fun `player can change readiness state`() = runBlocking {
        val session = repository.createRoom("Host Ready Check").getOrThrow()

        val initialPlayers = repository.currentRoom.value?.players
        assertTrue(initialPlayers?.first()?.isReady == true)

        val readyResult = repository.setReady(session.roomId, false)
        assertTrue(readyResult.isSuccess)

        val updatedPlayers = repository.currentRoom.value?.players
        assertFalse(updatedPlayers?.first()?.isReady == true)
    }

    @Test
    fun `observers receive room and player updates`() = runBlocking {
        val session = repository.createRoom("Observer Test Host").getOrThrow()

        val observedRoom = repository.observeRoom(session.roomId).first()
        assertNotNull(observedRoom)
        assertEquals(session.roomCode, observedRoom?.roomCode)

        repository.addMockInvestigator("New Arrival")
        val updatedPlayers = repository.observePlayers(session.roomId).first()
        assertEquals(2, updatedPlayers.size)
        assertTrue(updatedPlayers.any { it.playerName == "New Arrival" })
    }

    @Test
    fun `leaving room resets room state and clears session`() = runBlocking {
        val session = repository.createRoom("Departing Host").getOrThrow()
        assertNotNull(repository.currentRoom.value)

        val leaveResult = repository.leaveRoom(session.roomId)
        assertTrue(leaveResult.isSuccess)

        assertNull(repository.currentRoom.value)
        assertNull(repository.myPlayerId.value)
        assertFalse(repository.isHost.value)
    }
}

