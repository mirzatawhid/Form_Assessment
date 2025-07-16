package com.example.form_assessment.viewmodel
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.form_assessment.data.FormRepo
import androidx.lifecycle.viewModelScope
import com.example.form_assessment.data.AnswerSubmission
import com.example.form_assessment.data.QuestionItem
import com.example.form_assessment.firebase.FirebaseHelper
import kotlinx.coroutines.launch

class FormViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FormRepo(application.applicationContext)
    private val _questions = MutableLiveData<List<QuestionItem>>()
    val questions: LiveData<List<QuestionItem>> get() = _questions

    private val _currentQuestion = MutableLiveData<QuestionItem>()
    val currentQuestion: LiveData<QuestionItem> get() = _currentQuestion

    private val answers = mutableMapOf<String, String>()

    fun loadSurvey() {
        viewModelScope.launch {
            repository.fetchSurvey()?.let {
                _questions.value = it
                _currentQuestion.value = it.firstOrNull()
            }
        }
    }

    fun answerCurrent(id: String, value: String) {
        answers[id] = value
    }

    fun getNextQuestion(referId: String?) {
        _questions.value?.firstOrNull { it.id == referId }?.let {
            _currentQuestion.value = it
        }
    }

    fun getAnswers(): Map<String, String> = answers

    fun getAnswerSubmissions(): List<AnswerSubmission> {
        return answers.map { (questionId, answer) ->
            AnswerSubmission(questionId, answer)
        }
    }

    suspend fun getLocalAnswers(): List<AnswerSubmission> {
        return repository.getLocalAnswers().map {
            AnswerSubmission(
                questionId = it.questionId,
                answer = it.answer,
                timestamp = it.timestamp
            )
        }
    }


    suspend fun saveAnswersToRoom() {
        repository.saveToRoom(getAnswerSubmissions())
    }

    suspend fun syncLocalToFirebase(): Boolean {
        val localAnswers = repository.getLocalAnswers()

        if (localAnswers.isNotEmpty()) {
            val submissions = localAnswers.map {
                AnswerSubmission(
                    questionId = it.questionId,
                    answer = it.answer,
                    timestamp = it.timestamp
                )
            }

            return try {
                FirebaseHelper.submitAnswers(
                    answers = submissions,
                    onSuccess = {},
                    onError = {}
                )
                repository.clearLocalAnswers() // clear Room after successful sync
                true
            } catch (e: Exception) {
                false
            }
        }
        return false
    }


}
