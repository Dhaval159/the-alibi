package com.example.model

/**
 * Domain-level authentication identity abstraction.
 * Decouples the UI and ViewModel layers from Firebase Auth SDK objects.
 */
data class AuthIdentity(
    val uid: String,
    val displayName: String? = null,
    val email: String? = null,
    val isAnonymous: Boolean = false,
    val isOnlineAuth: Boolean = false
)

/**
 * Explicit mode selection for repository operations.
 * Prevents silent fallback between online and offline modes.
 */
enum class GameMode {
    OFFLINE_DEMO,
    FIREBASE_ONLINE
}

/**
 * Domain-level connection state representation.
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Unavailable(val reason: String) : ConnectionState()
    data class Error(val message: String, val cause: Throwable? = null) : ConnectionState()
}

/**
 * Domain representation of an active room session.
 */
data class RoomSession(
    val roomId: String,
    val roomCode: String,
    val playerId: String,
    val sessionToken: String? = null,
    val isHost: Boolean = false
)

/**
 * Domain representation of authentication lifecycle states.
 */
sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    data class Authenticated(val identity: AuthIdentity) : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String, val cause: Throwable? = null) : AuthState()
}

