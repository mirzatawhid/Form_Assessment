package com.example.form_assessment.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ApiResponse(val record: List<QuestionItem>)

data class QuestionItem(
    val id: String,
    val type: String,
    val question: QuestionText,
    val options: List<Option>?,
    val referTo: ReferTo?,
    val skip: ReferTo?,
    val validations: Validations?
)

data class QuestionText(val slug: String)
data class Option(val value: String, val referTo: ReferTo? = null)
data class ReferTo(val id: String)
data class Validations(val regex: String?)

data class AnswerSubmission(
    val questionId: String,
    val answer: String,
    val timestamp: String = getCurrentDateTime()
)

data class DisplaySubmission(
    val answers: List<DisplayAnswer>
)

data class DisplayAnswer(
    val questionText: String,
    val answer: String,
    val isImage: Boolean = false,
    val imagePath: String? = null
)

fun getCurrentDateTime(): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    return sdf.format(Date())
}
