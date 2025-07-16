package com.example.form_assessment.adapter

import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.marginTop
import androidx.recyclerview.widget.RecyclerView
import com.example.form_assessment.R
import com.example.form_assessment.data.DisplaySubmission

class SubmissionAdapter(private val list: List<DisplaySubmission>) :
    RecyclerView.Adapter<SubmissionAdapter.SubmissionViewHolder>() {

    class SubmissionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: LinearLayout = view.findViewById(R.id.answer_container)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubmissionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_submission, parent, false)
        return SubmissionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubmissionViewHolder, position: Int) {
        val submission = list[position]
        holder.container.removeAllViews()

        submission.answers.forEach { ans ->
            val context = holder.itemView.context

            val questionTV = TextView(context).apply {
                text = "Q: ${ans.questionText}"
                setTypeface(null, Typeface.BOLD)
                textSize = 18f
                setTextColor(Color.BLACK)
                setPadding(0, 16, 0, 8) // inner padding
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 16, 0, 4) // top margin between questions
                }
            }

            val answerView: View = if (ans.isImage && ans.imagePath != null) {
                ImageView(context).apply {
                    setImageBitmap(BitmapFactory.decodeFile(ans.imagePath))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        720
                    ).apply {
                        setMargins(0, 0, 0, 24)
                    }
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }
            } else {
                TextView(context).apply {
                    text = "A: ${ans.answer}"
                    textSize = 16f
                    setTextColor(Color.DKGRAY)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, 24)
                    }
                }
            }

            holder.container.addView(questionTV)
            holder.container.addView(answerView)
        }
    }


    override fun getItemCount() = list.size
}
