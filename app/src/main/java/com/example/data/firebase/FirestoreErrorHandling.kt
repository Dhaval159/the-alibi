package com.example.data.firebase

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreException

enum class OperationType {
  CREATE,
  GET,
  LIST,
  UPDATE,
  DELETE
}

fun handleFirestoreError(
  exception: Exception,
  operationType: OperationType,
  path: String
) {
  val firestoreError = generateSequence<Throwable>(exception) { it.cause }
    .filterIsInstance<FirebaseFirestoreException>()
    .firstOrNull()

  val code = firestoreError?.code?.name ?: "UNKNOWN"
  val message = firestoreError?.localizedMessage ?: exception.localizedMessage ?: "Unknown error"

  val jsonReport = """
    {
      "error": "FirestoreOperationError",
      "operation": "${operationType.name}",
      "path": "$path",
      "code": "$code",
      "message": "$message"
    }
  """.trimIndent()

  Log.e("TheAlibiFirebase", jsonReport, exception)
}
