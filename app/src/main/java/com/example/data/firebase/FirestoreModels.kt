package com.example.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class FirestoreRoom(
  @DocumentId val id: String = "",
  val roomCode: String = "",
  val hostUid: String = "",
  val caseId: String = "",
  val status: String = "LOBBY",
  val playerCount: Long = 1L,
  val maxPlayers: Long = 6L,
  val turnNumber: Long = 0L,
  val activeTwistId: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestorePlayer(
  @DocumentId val id: String = "",
  val uid: String = "",
  val alias: String = "",
  val isHost: Boolean = false,
  val isReady: Boolean = false,
  val assignedCharacterId: String? = null,
  val assignedCharacterName: String? = null,
  val isOnline: Boolean = true,
  @ServerTimestamp val joinedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestorePrivateDossier(
  @DocumentId val id: String = "",
  val uid: String = "",
  val characterId: String = "",
  val isCulprit: Boolean = false,
  val secretObjective: String = "",
  val alibiSecret: String? = null,
  val knownSecrets: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreEvidence(
  @DocumentId val id: String = "",
  val evidenceId: String = "",
  val discoveredByUid: String = "",
  val title: String = "",
  val description: String = "",
  val locationId: String = "",
  val isSmokingGun: Boolean = false,
  val isShared: Boolean = false,
  @ServerTimestamp val discoveredAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreMessage(
  @DocumentId val id: String = "",
  val senderUid: String = "",
  val senderAlias: String = "",
  val recipientUid: String = "PUBLIC",
  val text: String = "",
  @ServerTimestamp val timestamp: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreDeal(
  @DocumentId val id: String = "",
  val senderUid: String = "",
  val targetUid: String = "",
  val offeredClueId: String? = null,
  val requestedClueId: String? = null,
  val terms: String = "",
  val status: String = "PENDING",
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreVote(
  @DocumentId val id: String = "",
  val voterUid: String = "",
  val accusedCharacterId: String = "",
  val smokingGunEvidenceId: String = "",
  val motive: String = "",
  @ServerTimestamp val submittedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreCase(
  @DocumentId val id: String = "",
  val caseId: String = "",
  val title: String = "",
  val synopsis: String = "",
  val difficulty: String = "INTERMEDIATE",
  val estimatedDurationMinutes: Long = 45L,
  val creatorUid: String = "SYSTEM",
  val isPublic: Boolean = true,
  val caseJson: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null
)
