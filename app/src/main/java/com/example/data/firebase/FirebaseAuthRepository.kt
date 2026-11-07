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

class FirebaseAuthRepository(private val context: Context) {

  private val auth: FirebaseAuth = Firebase.auth
  private val credentialManager: CredentialManager = CredentialManager.create(context)

  private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
  val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

  init {
    auth.addAuthStateListener { firebaseAuth ->
      _currentUser.value = firebaseAuth.currentUser
    }
  }

  val currentUserId: String?
    get() = auth.currentUser?.uid

  val isAuthenticated: Boolean
    get() = auth.currentUser != null

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
        Log.d("TheAlibiAuth", "Auto sign-in silent pass failed, prompting interactive sign-in: ${e.message}")
        onUnauthenticated()
      }
    }
  }

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
          val authResult = auth.signInWithCredential(authCredential).await()
          val user = checkNotNull(authResult.user) { "FirebaseUser is null after Google credential sign-in" }
          _currentUser.value = user
          onSuccess(user)
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

  fun signOut(scope: CoroutineScope, onComplete: () -> Unit = {}) {
    scope.launch {
      withContext(NonCancellable + Dispatchers.IO) {
        try {
          credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
          Log.w("TheAlibiAuth", "Failed to clear credential state: ${e.message}")
        } finally {
          auth.signOut()
          _currentUser.value = null
          withContext(Dispatchers.Main) {
            onComplete()
          }
        }
      }
    }
  }
}
