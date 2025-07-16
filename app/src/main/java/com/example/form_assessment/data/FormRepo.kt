package com.example.form_assessment.data

import android.content.Context
import com.example.form_assessment.db.AnswerEntity
import com.example.form_assessment.db.AppDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FormRepo(context: Context) {
//    suspend fun fetchSurvey(): List<QuestionItem> {
//        return RetrofitClient.api.getSurvey().record
//    }
    private val db = AppDatabase.getInstance(context)
    private val dao = db.answerDao()
    suspend fun fetchSurvey(): List<QuestionItem>? {
        val response = RetrofitClient.api.getSurvey()
        return if (response.isSuccessful) response.body()?.record else null
    }

    suspend fun saveToRoom(answers: List<AnswerSubmission>) {
        val roomAnswers = answers.map {
            AnswerEntity(questionId = it.questionId, answer = it.answer, timestamp = it.timestamp)
        }
        dao.insertAnswers(roomAnswers)
    }

    suspend fun getLocalAnswers(): List<AnswerEntity> = dao.getAllAnswers()

    suspend fun clearLocalAnswers() {
        dao.clearAll()
    }


}