package com.example.form_assessment.firebase

import android.content.Context
import android.graphics.Bitmap
import com.example.form_assessment.data.AnswerSubmission
import com.example.form_assessment.data.FormRepo
import com.example.form_assessment.data.getCurrentDateTime
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object FirebaseHelper {
    private val db = Firebase.firestore
    private val storage = Firebase.storage.reference

    fun submitAnswers(
        answers: List<AnswerSubmission>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val docId = db.collection("submissions").document().id
        val data = hashMapOf(
            "id" to docId,
            "submittedAt" to getCurrentDateTime(),
            "answers" to answers.map {
                mapOf(
                    "questionId" to it.questionId,
                    "answer" to it.answer,
                    "timestamp" to it.timestamp
                )
            }
        )

        db.collection("submissions")
            .document(docId)
            .set(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun uploadCameraImage(
        bitmap: Bitmap,
        fileName: String,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val baos = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val imageRef = storage.child("images/$fileName")
        imageRef.putBytes(data)
            .addOnSuccessListener {
                imageRef.downloadUrl
                    .addOnSuccessListener { uri -> onSuccess(uri.toString()) }
                    .addOnFailureListener { onError(it) }
            }
            .addOnFailureListener { onError(it) }
    }

    fun submitAnswersWithRoomBackup(
        context: Context,
        answers: List<AnswerSubmission>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val docId = Firebase.firestore.collection("submissions").document().id
        val data = hashMapOf(
            "id" to docId,
            "submittedAt" to System.currentTimeMillis(),
            "answers" to answers.map {
                mapOf(
                    "questionId" to it.questionId,
                    "answer" to it.answer,
                    "timestamp" to it.timestamp
                )
            }
        )

        Firebase.firestore.collection("submissions")
            .document(docId)
            .set(data)
            .addOnSuccessListener {
                CoroutineScope(Dispatchers.IO).launch {
                    FormRepo(context).saveToRoom(answers)
                }
                onSuccess()
            }
            .addOnFailureListener {
                onError(it)
            }
    }




}

