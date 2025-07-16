package com.example.form_assessment

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.form_assessment.adapter.SubmissionAdapter
import com.example.form_assessment.data.AnswerSubmission
import com.example.form_assessment.data.DisplayAnswer
import com.example.form_assessment.data.DisplaySubmission
import com.example.form_assessment.data.RetrofitClient
import com.example.form_assessment.databinding.ActivityResultBinding
import com.example.form_assessment.viewmodel.FormViewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding
    private val viewModel: FormViewModel by lazy { FormViewModel(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadGroupedSubmissions()

        binding.btnBack.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.btnRetrySync.setOnClickListener {
            lifecycleScope.launch {
                val synced = viewModel.syncLocalToFirebase()
                if (synced) {
                    Toast.makeText(this@ResultActivity, "Synced to Firebase", Toast.LENGTH_SHORT).show()
                    loadGroupedSubmissions()
                } else {
                    Toast.makeText(this@ResultActivity, "Nothing to sync", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadGroupedSubmissions() {
        lifecycleScope.launch(Dispatchers.IO) {
            val questionMap = fetchQuestionMap()
            val rawSubmissions: List<List<AnswerSubmission>> =
                if (isInternetAvailable(this@ResultActivity)) {
                    fetchGroupedFirebaseSubmissions()
                } else {
                    listOf(viewModel.getLocalAnswers()) // Only one local set stored
                }

            val displaySubmissions = rawSubmissions.map { submission ->
                val answers = submission.map {
                    val isImage = it.answer.endsWith(".jpg") || it.answer.endsWith(".png")
                    DisplayAnswer(
                        questionText = questionMap[it.questionId] ?: it.questionId,
                        answer = it.answer,
                        isImage = isImage,
                        imagePath = if (isImage) it.answer else null
                    )
                }
                DisplaySubmission(answers)
            }

            withContext(Dispatchers.Main) {
                binding.answerRecyclerView.layoutManager = LinearLayoutManager(this@ResultActivity)
                binding.answerRecyclerView.adapter = SubmissionAdapter(displaySubmissions)
            }
        }
    }

    private suspend fun fetchQuestionMap(): Map<String, String> {
        return try {
            val response = RetrofitClient.api.getSurvey()
            if (response.isSuccessful) {
                response.body()?.record?.associate { it.id to it.question.slug } ?: emptyMap()
            } else emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private suspend fun fetchGroupedFirebaseSubmissions(): List<List<AnswerSubmission>> {
        return try {
            val snapshot = Firebase.firestore.collection("submissions").get().await()
            snapshot.documents.mapNotNull { doc ->
                val list = doc["answers"] as? List<HashMap<String, Any>> ?: return@mapNotNull null
                list.map {
                    AnswerSubmission(
                        questionId = it["questionId"] as? String ?: "",
                        answer = it["answer"] as? String ?: "",
                        timestamp = it["timestamp"] as? String ?: ""
                    )
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun isInternetAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}