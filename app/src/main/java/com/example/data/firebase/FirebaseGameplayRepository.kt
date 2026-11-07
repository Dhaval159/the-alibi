package com.example.data.firebase

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

class FirebaseGameplayRepository(private val db: FirebaseFirestore) {

  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth = Firebase.auth

  fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be authenticated before performing gameplay operations")
  }

  fun observeMyDossier(roomId: String): Flow<FirestorePrivateDossier?> {
    val uid = requireUserId()
    return db.collection("rooms").document(roomId).collection("privateDossiers").document(uid)
      .snapshots()
      .map { snapshot -> snapshot.toObject(FirestorePrivateDossier::class.java) }
  }

  suspend fun assignDossier(
    roomId: String,
    playerUid: String,
    characterId: String,
    isCulprit: Boolean,
    secretObjective: String,
    alibiSecret: String? = null,
    knownSecrets: String? = null
  ) {
    requireUserId()
    val docRef = db.collection("rooms").document(roomId).collection("privateDossiers").document(playerUid)
    val data = hashMapOf(
      "uid" to playerUid,
      "characterId" to characterId,
      "isCulprit" to isCulprit,
      "secretObjective" to secretObjective,
      "alibiSecret" to alibiSecret,
      "knownSecrets" to knownSecrets,
      "createdAt" to FieldValue.serverTimestamp()
    )
    try {
      docRef.set(data).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  fun observeEvidence(roomId: String): Flow<List<FirestoreEvidence>> {
    return db.collection("rooms").document(roomId).collection("evidence")
      .snapshots()
      .map { snapshot -> snapshot.toObjects(FirestoreEvidence::class.java) }
  }

  suspend fun recordEvidence(
    roomId: String,
    evidenceId: String,
    title: String,
    description: String,
    locationId: String,
    isSmokingGun: Boolean,
    isShared: Boolean
  ) {
    val uid = requireUserId()
    val docRef = db.collection("rooms").document(roomId).collection("evidence").document(evidenceId)
    val data = hashMapOf(
      "evidenceId" to evidenceId,
      "discoveredByUid" to uid,
      "title" to title,
      "description" to description,
      "locationId" to locationId,
      "isSmokingGun" to isSmokingGun,
      "isShared" to isShared,
      "discoveredAt" to FieldValue.serverTimestamp()
    )
    try {
      docRef.set(data).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  fun observeMessages(roomId: String): Flow<List<FirestoreMessage>> {
    val uid = requireUserId()
    return db.collection("rooms").document(roomId).collection("messages")
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(FirestoreMessage::class.java).filter { msg ->
          msg.recipientUid == "PUBLIC" || msg.recipientUid == uid || msg.senderUid == uid
        }
      }
  }

  suspend fun sendMessage(
    roomId: String,
    senderAlias: String,
    recipientUid: String,
    text: String
  ) {
    val uid = requireUserId()
    val docRef = db.collection("rooms").document(roomId).collection("messages").document()
    val data = hashMapOf(
      "senderUid" to uid,
      "senderAlias" to senderAlias,
      "recipientUid" to recipientUid,
      "text" to text,
      "timestamp" to FieldValue.serverTimestamp()
    )
    try {
      docRef.set(data).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun submitBallot(
    roomId: String,
    accusedCharacterId: String,
    smokingGunEvidenceId: String,
    motive: String
  ) {
    val uid = requireUserId()
    val docRef = db.collection("rooms").document(roomId).collection("votes").document(uid)
    val data = hashMapOf(
      "voterUid" to uid,
      "accusedCharacterId" to accusedCharacterId,
      "smokingGunEvidenceId" to smokingGunEvidenceId,
      "motive" to motive,
      "submittedAt" to FieldValue.serverTimestamp()
    )
    try {
      docRef.set(data).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  fun observeBallots(roomId: String): Flow<List<FirestoreVote>> {
    return db.collection("rooms").document(roomId).collection("votes")
      .snapshots()
      .map { snapshot -> snapshot.toObjects(FirestoreVote::class.java) }
  }
}
