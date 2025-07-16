package com.example.form_assessment.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AnswerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswers(answers: List<AnswerEntity>)

    @Query("SELECT * FROM answers")
    suspend fun getAllAnswers(): List<AnswerEntity>

    @Query("DELETE FROM answers")
    suspend fun clearAll()
}
