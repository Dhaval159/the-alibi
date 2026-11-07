package com.example.data.firebase

import com.example.model.AuthIdentity
import com.example.model.AuthState
import kotlinx.coroutines.flow.StateFlow

/**
 * Domain-level authentication repository abstraction for THE ALIBI.
 *
 * Decouples the application from concrete Firebase Auth SDK classes,
 * enabling clean architecture, seamless testing with fakes/doubles, and
 * future multi-provider extensions.
 */
interface AuthRepository {

    /**
     * Observable domain identity of the currently signed-in user.
     * Null when unauthenticated.
     */
    val authIdentity: StateFlow<AuthIdentity?>

    /**
     * Observable authentication lifecycle state.
     */
    val authState: StateFlow<AuthState>

    /**
     * Current user ID (stable Firebase UID), or null if not signed in.
     */
    val currentUserId: String?

    /**
     * True if a user is currently authenticated (anonymous or linked).
     */
    val isAuthenticated: Boolean

    /**
     * True if the current authenticated user is an anonymous guest.
     */
    val isAnonymous: Boolean

    /**
     * Performs anonymous authentication.
     * If an authenticated session already exists, reuses it without network calls.
     */
    suspend fun signInAnonymously(): Result<AuthIdentity>

    /**
     * Resets authentication state by signing out and clearing credentials.
     * Used for development and testing.
     */
    suspend fun resetAuth(): Result<Unit>
}

