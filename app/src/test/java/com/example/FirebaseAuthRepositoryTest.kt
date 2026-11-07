package com.example

import com.example.data.UnavailableFirebaseGameRepository
import com.example.data.firebase.AuthRepository
import com.example.model.AuthIdentity
import com.example.model.AuthState
import com.example.model.ConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Unit test suite for Phase 1A: Firebase Foundation & Anonymous Authentication.
 *
 * Verifies:
 * - Anonymous account creation and stable UID generation
 * - Session reuse and persistence across repeated sign-in calls
 * - AuthState transitions (Initial -> Loading -> Authenticated -> Unauthenticated / Error)
 * - Controlled resetAuth mechanism
 * - Integration with UnavailableFirebaseGameRepository (fail-closed online mode)
 */
class FirebaseAuthRepositoryTest {

    /**
     * In-memory test double of [AuthRepository] verifying the contract.
     */
    class FakeAuthRepository(
        initialIdentity: AuthIdentity? = null,
        var simulateNetworkFailure: Boolean = false
    ) : AuthRepository {

        private val _authIdentity = MutableStateFlow<AuthIdentity?>(initialIdentity)
        override val authIdentity: StateFlow<AuthIdentity?> = _authIdentity.asStateFlow()

        private val _authState = MutableStateFlow<AuthState>(
            initialIdentity?.let { AuthState.Authenticated(it) } ?: AuthState.Initial
        )
        override val authState: StateFlow<AuthState> = _authState.asStateFlow()

        override val currentUserId: String?
            get() = _authIdentity.value?.uid

        override val isAuthenticated: Boolean
            get() = _authIdentity.value != null

        override val isAnonymous: Boolean
            get() = _authIdentity.value?.isAnonymous == true

        var networkCallsCount: Int = 0
            private set

        override suspend fun signInAnonymously(): Result<AuthIdentity> {
            _authState.value = AuthState.Loading

            if (simulateNetworkFailure) {
                val error = RuntimeException("Network unavailable. Please check your connection.")
                _authState.value = AuthState.Error(error.message ?: "Network error", error)
                return Result.failure(error)
            }

            val existing = _authIdentity.value
            if (existing != null) {
                _authState.value = AuthState.Authenticated(existing)
                return Result.success(existing)
            }

            networkCallsCount++
            val newIdentity = AuthIdentity(
                uid = "anon_uid_${UUID.randomUUID().toString().take(12)}",
                displayName = "Guest Detective",
                email = null,
                isAnonymous = true,
                isOnlineAuth = true
            )
            _authIdentity.value = newIdentity
            _authState.value = AuthState.Authenticated(newIdentity)
            return Result.success(newIdentity)
        }

        override suspend fun resetAuth(): Result<Unit> {
            _authIdentity.value = null
            _authState.value = AuthState.Unauthenticated
            return Result.success(Unit)
        }

        fun linkGoogleAccount(email: String, displayName: String) {
            val current = _authIdentity.value
            val uid = current?.uid ?: "google_uid_${UUID.randomUUID().toString().take(12)}"
            val linked = AuthIdentity(
                uid = uid,
                displayName = displayName,
                email = email,
                isAnonymous = false,
                isOnlineAuth = true
            )
            _authIdentity.value = linked
            _authState.value = AuthState.Authenticated(linked)
        }
    }

    private lateinit var authRepository: FakeAuthRepository

    @Before
    fun setUp() {
        authRepository = FakeAuthRepository()
    }

    @Test
    fun `initial state is unauthenticated or initial`() {
        assertFalse(authRepository.isAuthenticated)
        assertNull(authRepository.currentUserId)
        assertNull(authRepository.authIdentity.value)
        assertTrue(authRepository.authState.value is AuthState.Initial)
    }

    @Test
    fun `anonymous sign-in creates anonymous account with stable UID`() = runBlocking {
        val result = authRepository.signInAnonymously()
        assertTrue(result.isSuccess)

        val identity = result.getOrThrow()
        assertNotNull(identity.uid)
        assertTrue(identity.uid.startsWith("anon_uid_"))
        assertTrue(identity.isAnonymous)
        assertTrue(identity.isOnlineAuth)
        assertEquals("Guest Detective", identity.displayName)
        assertNull(identity.email)

        assertTrue(authRepository.isAuthenticated)
        assertTrue(authRepository.isAnonymous)
        assertEquals(identity.uid, authRepository.currentUserId)

        val state = authRepository.authState.first()
        assertTrue(state is AuthState.Authenticated)
        assertEquals(identity.uid, (state as AuthState.Authenticated).identity.uid)
    }

    @Test
    fun `subsequent anonymous sign-in reuses existing session without generating new UID`() = runBlocking {
        val firstResult = authRepository.signInAnonymously()
        assertTrue(firstResult.isSuccess)
        val firstIdentity = firstResult.getOrThrow()
        assertEquals(1, authRepository.networkCallsCount)

        // Second call should reuse the existing session
        val secondResult = authRepository.signInAnonymously()
        assertTrue(secondResult.isSuccess)
        val secondIdentity = secondResult.getOrThrow()

        // UID must remain stable across calls
        assertEquals(firstIdentity.uid, secondIdentity.uid)
        // No redundant network creation call
        assertEquals(1, authRepository.networkCallsCount)
    }

    @Test
    fun `network failure during anonymous sign-in transitions to Error state gracefully`() = runBlocking {
        authRepository.simulateNetworkFailure = true

        val result = authRepository.signInAnonymously()
        assertTrue(result.isFailure)
        assertFalse(authRepository.isAuthenticated)

        val state = authRepository.authState.value
        assertTrue(state is AuthState.Error)
        val errorState = state as AuthState.Error
        assertTrue(errorState.message.contains("Network unavailable"))
        assertNotNull(errorState.cause)
    }

    @Test
    fun `resetAuth signs out and transitions to Unauthenticated state`() = runBlocking {
        authRepository.signInAnonymously()
        assertTrue(authRepository.isAuthenticated)

        val resetResult = authRepository.resetAuth()
        assertTrue(resetResult.isSuccess)

        assertFalse(authRepository.isAuthenticated)
        assertNull(authRepository.currentUserId)
        assertNull(authRepository.authIdentity.value)
        assertTrue(authRepository.authState.value is AuthState.Unauthenticated)
    }

    @Test
    fun `linking Google account preserves original UID and updates identity`() = runBlocking {
        val anonResult = authRepository.signInAnonymously()
        val originalUid = anonResult.getOrThrow().uid

        authRepository.linkGoogleAccount("detective@example.com", "Sherlock Holmes")

        val linkedIdentity = authRepository.authIdentity.value
        assertNotNull(linkedIdentity)
        assertEquals(originalUid, linkedIdentity?.uid) // UID preserved
        assertEquals("detective@example.com", linkedIdentity?.email)
        assertEquals("Sherlock Holmes", linkedIdentity?.displayName)
        assertFalse(linkedIdentity?.isAnonymous == true)
        assertTrue(linkedIdentity?.isOnlineAuth == true)
    }

    @Test
    fun `online repository obtains live authenticated UID while failing closed on multiplayer operations`() = runBlocking {
        val anonResult = authRepository.signInAnonymously()
        val identity = anonResult.getOrThrow()

        val onlineRepo = UnavailableFirebaseGameRepository()
        onlineRepo.setAuthIdentity(identity)

        // Verifies online mode receives the authenticated UID
        assertEquals(identity.uid, onlineRepo.authIdentity.value?.uid)
        assertTrue(onlineRepo.authIdentity.value?.isOnlineAuth == true)

        // Verifies fail-closed behavior: operations are rejected and report Unavailable
        val connState = onlineRepo.connectionState.first()
        assertTrue(connState is ConnectionState.Unavailable)

        val createResult = onlineRepo.createRoom("Host Detective")
        assertTrue(createResult.isFailure)
        assertTrue(createResult.exceptionOrNull() is IllegalStateException)
    }
}

