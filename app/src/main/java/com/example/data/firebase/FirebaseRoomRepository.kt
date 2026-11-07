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

class FirebaseRoomRepository(private val db: FirebaseFirestore) {

  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth = Firebase.auth

  fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be authenticated before performing room operations")
  }

  suspend fun createRoom(
    roomCode: String,
    caseId: String,
    alias: String,
    maxPlayers: Int = 6
  ): String {
    val uid = requireUserId()
    val roomId = "room_$roomCode"
    val roomDoc = db.collection("rooms").document(roomId)

    val roomData = hashMapOf(
      "roomCode" to roomCode,
      "hostUid" to uid,
      "caseId" to caseId,
      "status" to "LOBBY",
      "playerCount" to 1,
      "maxPlayers" to maxPlayers,
      "turnNumber" to 0,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )

    val hostPlayer = hashMapOf(
      "uid" to uid,
      "alias" to alias,
      "isHost" to true,
      "isReady" to true,
      "isOnline" to true,
      "joinedAt" to FieldValue.serverTimestamp()
    )

    try {
      roomDoc.set(roomData).await()
      roomDoc.collection("players").document(uid).set(hostPlayer).await()
      return roomId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, roomDoc.path)
      throw e
    }
  }

  suspend fun joinRoom(roomCode: String, alias: String): String {
    val uid = requireUserId()
    val roomId = "room_$roomCode"
    val roomDoc = db.collection("rooms").document(roomId)

    val playerDoc = roomDoc.collection("players").document(uid)
    val playerData = hashMapOf(
      "uid" to uid,
      "alias" to alias,
      "isHost" to false,
      "isReady" to false,
      "isOnline" to true,
      "joinedAt" to FieldValue.serverTimestamp()
    )

    try {
      val snapshot = roomDoc.get().await()
      if (!snapshot.exists()) {
        throw IllegalArgumentException("Room $roomCode not found")
      }
      playerDoc.set(playerData).await()
      roomDoc.update(
        "playerCount", FieldValue.increment(1),
        "updatedAt", FieldValue.serverTimestamp()
      ).await()
      return roomId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, playerDoc.path)
      throw e
    }
  }

  fun observeRoom(roomId: String): Flow<FirestoreRoom?> {
    return db.collection("rooms").document(roomId)
      .snapshots()
      .map { snapshot -> snapshot.toObject(FirestoreRoom::class.java) }
  }

  fun observePlayers(roomId: String): Flow<List<FirestorePlayer>> {
    return db.collection("rooms").document(roomId).collection("players")
      .snapshots()
      .map { snapshot -> snapshot.toObjects(FirestorePlayer::class.java) }
  }

  suspend fun setReady(roomId: String, isReady: Boolean) {
    val uid = requireUserId()
    val playerDoc = db.collection("rooms").document(roomId).collection("players").document(uid)
    try {
      playerDoc.update("isReady", isReady).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, playerDoc.path)
      throw e
    }
  }

  suspend fun updateGameStatus(roomId: String, newStatus: String) {
    requireUserId()
    val roomDoc = db.collection("rooms").document(roomId)
    try {
      roomDoc.update(
        "status", newStatus,
        "updatedAt", FieldValue.serverTimestamp()
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, roomDoc.path)
      throw e
    }
  }
}
