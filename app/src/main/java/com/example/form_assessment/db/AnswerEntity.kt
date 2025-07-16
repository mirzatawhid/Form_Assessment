package com.example.form_assessment.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "answers")
data class AnswerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val questionId: String,
    val answer: String,
    val timestamp: String
)
