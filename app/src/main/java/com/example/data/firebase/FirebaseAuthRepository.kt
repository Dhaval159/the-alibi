package com.example.data.firebase

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.model.AuthIdentity
import com.example.model.AuthState
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Concrete Firebase implementation of [AuthRepository] for THE ALIBI.
 *
 * Implements anonymous authentication as the primary friction-free entry point,
 * allowing users to play immediately without mandatory Google Sign-In.
 * Also supports optional Google account linking for account backup and cross-device sync.
 */
class FirebaseAuthRepository(
    private val context: Context,
    val auth: FirebaseAuth = Firebase.auth,
    val credentialManager: CredentialManager = CredentialManager.create(context)
) : AuthRepository {

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _authIdentity = MutableStateFlow<AuthIdentity?>(toAuthIdentity(auth.currentUser))
    override val authIdentity: StateFlow<AuthIdentity?> = _authIdentity.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(
        toAuthIdentity(auth.currentUser)?.let { AuthState.Authenticated(it) } ?: AuthState.Initial
    )
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _currentUser.value = user
            val identity = toAuthIdentity(user)
            _authIdentity.value = identity
            _authState.value = if (identity != null) {
                AuthState.Authenticated(identity)
            } else {
                AuthState.Unauthenticated
            }
        }
    }

    override val currentUserId: String?
        get() = auth.currentUser?.uid

    override val isAuthenticated: Boolean
        get() = auth.currentUser != null

    override val isAnonymous: Boolean
        get() = auth.currentUser?.isAnonymous == true

    /**
     * Maps a FirebaseUser SDK entity into a domain-level AuthIdentity.
     */
    fun toAuthIdentity(user: FirebaseUser?): AuthIdentity? {
        return user?.let {
            AuthIdentity(
                uid = it.uid,
                displayName = it.displayName ?: if (it.isAnonymous) "Guest Detective" else null,
                email = it.email,
                isAnonymous = it.isAnonymous,
                isOnlineAuth = true
            )
        }
    }

    /**
     * Signs in anonymously if no authenticated user currently exists.
     * If an authenticated user already exists (persisted from previous session),
     * it reuses the existing session to avoid redundant network calls and maintain stable UID.
     */
    override suspend fun signInAnonymously(): Result<AuthIdentity> {
        _authState.value = AuthState.Loading

        val existingUser = auth.currentUser
        if (existingUser != null) {
            _currentUser.value = existingUser
            val identity = toAuthIdentity(existingUser)!!
            _authIdentity.value = identity
            _authState.value = AuthState.Authenticated(identity)
            return Result.success(identity)
        }

        return try {
            val authResult = auth.signInAnonymously().await()
            val user = checkNotNull(authResult.user) { "FirebaseUser is null after anonymous sign-in" }
            _currentUser.value = user
            val identity = toAuthIdentity(user)!!
            _authIdentity.value = identity
            _authState.value = AuthState.Authenticated(identity)
            Result.success(identity)
        } catch (e: Exception) {
            Log.e("TheAlibiAuth", "Anonymous authentication failed", e)
            _authState.value = AuthState.Error(e.localizedMessage ?: "Anonymous authentication failed", e)
            Result.failure(e)
        }
    }

    /**
     * Optional silent auto sign-in attempt with Google credentials.
     */
    fun attemptAutoSignIn(
        scope: CoroutineScope,
        onSuccess: (FirebaseUser) -> Unit,
        onUnauthenticated: () -> Unit
    ) {
        val existingUser = auth.currentUser
        if (existingUser != null) {
            _currentUser.value = existingUser
            onSuccess(existingUser)
            return
        }

        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            Log.w("TheAlibiAuth", "default_web_client_id resource not found", e)
            onUnauthenticated()
            return
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = auth.signInWithCredential(authCredential).await()
                    val user = checkNotNull(authResult.user) { "FirebaseUser is null after Google credential sign-in" }
                    _currentUser.value = user
                    onSuccess(user)
                } else {
                    onUnauthenticated()
                }
            } catch (e: Exception) {
                Log.d("TheAlibiAuth", "Auto sign-in silent pass failed, fallback to unauthenticated: ${e.message}")
                onUnauthenticated()
            }
        }
    }

    /**
     * Interactive Google Sign-In or Google Account Linking.
     * If the user is currently anonymous, links the Google credential to retain their UID.
     * If unauthenticated, performs a standard sign-in.
     */
    fun signInWithGoogle(
        activity: Activity,
        scope: CoroutineScope,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit,
        onCancelled: () -> Unit = {}
    ) {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            onError("Google Sign-In configuration missing: default_web_client_id not found")
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)

                    val currentUser = auth.currentUser
                    val finalUser = if (currentUser != null && currentUser.isAnonymous) {
                        // Link anonymous account with Google credential to preserve existing UID
                        val linkResult = currentUser.linkWithCredential(authCredential).await()
                        checkNotNull(linkResult.user) { "FirebaseUser is null after account linking" }
                    } else {
                        val authResult = auth.signInWithCredential(authCredential).await()
                        checkNotNull(authResult.user) { "FirebaseUser is null after Google credential sign-in" }
                    }

                    _currentUser.value = finalUser
                    val identity = toAuthIdentity(finalUser)
                    _authIdentity.value = identity
                    if (identity != null) {
                        _authState.value = AuthState.Authenticated(identity)
                    }
                    onSuccess(finalUser)
                } else {
                    onError("Unexpected credential type returned from CredentialManager")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w("TheAlibiAuth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                onCancelled()
            } catch (e: Exception) {
                Log.e("TheAlibiAuth", "Google Sign-In failed", e)
                onError(e.localizedMessage ?: "Google Sign-In failed")
            }
        }
    }

    /**
     * Resets authentication state by signing out and clearing credential manager state.
     * Controlled mechanism for development and testing.
     */
    override suspend fun resetAuth(): Result<Unit> = withContext(NonCancellable + Dispatchers.IO) {
        try {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("TheAlibiAuth", "Failed to clear credential state: ${e.message}")
            }
            auth.signOut()
            _currentUser.value = null
            _authIdentity.value = null
            _authState.value = AuthState.Unauthenticated
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("TheAlibiAuth", "Failed to reset auth state", e)
            Result.failure(e)
        }
    }

    /**
     * Traditional sign out with callback for UI layer.
     */
    fun signOut(scope: CoroutineScope, onComplete: () -> Unit = {}) {
        scope.launch {
            resetAuth()
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }
}
