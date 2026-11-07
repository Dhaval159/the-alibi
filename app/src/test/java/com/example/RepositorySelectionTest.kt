package com.example

import com.example.data.GameRepositoryFactory
import com.example.data.TheAlibiRepository
import com.example.data.UnavailableFirebaseGameRepository
import com.example.model.ConnectionState
import com.example.model.GameMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/**
 * Verifies explicit repository selection behavior and ensures online mode
 * does not silently fall back to local in-memory gameplay.
 */
class RepositorySelectionTest {

    @Test
    fun `offline demo mode explicitly selects local repository implementation`() {
        val repository = GameRepositoryFactory.createRepository(GameMode.OFFLINE_DEMO)

        assertTrue(
            "Offline demo mode must instantiate TheAlibiRepository",
            repository is TheAlibiRepository
        )
        assertEquals(GameMode.OFFLINE_DEMO, repository.gameMode)
    }

    @Test
    fun `online mode does not silently select local repository implementation`() {
        val repository = GameRepositoryFactory.createRepository(GameMode.FIREBASE_ONLINE)

        assertFalse(
            "Online mode must NOT silently fall back to TheAlibiRepository",
            repository is TheAlibiRepository
        )
        assertTrue(
            "Online mode in Phase 0 must instantiate UnavailableFirebaseGameRepository",
            repository is UnavailableFirebaseGameRepository
        )
        assertEquals(GameMode.FIREBASE_ONLINE, repository.gameMode)
    }

    @Test
    fun `unavailable online implementation produces explicit error and unavailable connection state`() = runBlocking {
        val repository = GameRepositoryFactory.createRepository(GameMode.FIREBASE_ONLINE)

        // 1. Connection state must explicitly report Unavailable
        val connection = repository.connectionState.first()
        assertTrue(
            "Connection state must be Unavailable",
            connection is ConnectionState.Unavailable
        )
        val reason = (connection as ConnectionState.Unavailable).reason
        assertTrue(reason.contains("Phase 0") || reason.contains("not available"))

        // 2. Room commands must fail with explicit error rather than silently succeeding
        val createResult = repository.createRoom("Online Player")
        assertTrue("createRoom must fail in unavailable mode", createResult.isFailure)
        assertTrue(createResult.exceptionOrNull() is IllegalStateException)
        assertTrue(
            createResult.exceptionOrNull()?.message?.contains("not available") == true
        )

        val joinResult = repository.joinRoom("123456", "Online Player")
        assertTrue("joinRoom must fail in unavailable mode", joinResult.isFailure)

        val readyResult = repository.setReady("any_room", true)
        assertTrue("setReady must fail in unavailable mode", readyResult.isFailure)
    }
}

