package com.example.form_assessment.ui

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.form_assessment.R
import com.example.form_assessment.data.AnswerSubmission
import java.io.File

class AnswerAdapter(private val answers: List<AnswerSubmission>) :
    RecyclerView.Adapter<AnswerAdapter.AnswerViewHolder>() {

    class AnswerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val questionId: TextView = view.findViewById(R.id.tvQuestionId)
        val answer: TextView = view.findViewById(R.id.tvAnswer)
        val imageView: ImageView = view.findViewById(R.id.imgAnswer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnswerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.answer_item, parent, false)
        return AnswerViewHolder(view)
    }

    override fun onBindViewHolder(holder: AnswerViewHolder, position: Int) {
        val item = answers[position]
        holder.questionId.text = item.questionId

        // Check if it's an image file path
        if (item.answer.endsWith(".jpg") || item.answer.endsWith(".png")) {
            holder.answer.visibility = View.GONE
            holder.imageView.visibility = View.VISIBLE

            val imgFile = File(item.answer)
            if (imgFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                holder.imageView.setImageBitmap(bitmap)
            }
        } else {
            holder.answer.text = item.answer
            holder.imageView.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = answers.size
}
