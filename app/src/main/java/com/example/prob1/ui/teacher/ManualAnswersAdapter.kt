package com.example.prob1.ui.teacher

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.prob1.databinding.ItemManualAnswerBinding

class ManualAnswersAdapter(
    private val onItemClick: (ManualAnswersActivity.ManualAnswer) -> Unit
) : RecyclerView.Adapter<ManualAnswersAdapter.ViewHolder>() {

    private var answers = emptyList<ManualAnswersActivity.ManualAnswer>()

    inner class ViewHolder(val binding: ItemManualAnswerBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(answer: ManualAnswersActivity.ManualAnswer) {
            binding.userName.text = answer.userName
            binding.testInfo.text = "Тест: ${answer.testTitle}, Часть: ${answer.partTitle}"
            binding.questionText.text = "Вопрос: ${answer.questionText}"
            binding.answerText.text = "Ответ: ${answer.answer}"
            binding.statusText.text = if (answer.isEvaluated) {
                "Оценка: ${answer.score}"
            } else {
                "Не оценено"
            }

            // Устанавливаем цвета текста
            binding.userName.setTextColor(Color.BLACK)
            binding.testInfo.setTextColor(Color.BLACK)
            binding.questionText.setTextColor(Color.BLACK)
            binding.answerText.setTextColor(Color.BLACK)
            binding.statusText.setTextColor(Color.BLACK)

            binding.root.setOnClickListener { onItemClick(answer) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemManualAnswerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(answers[position])
    }

    override fun getItemCount() = answers.size

    fun submitList(newList: List<ManualAnswersActivity.ManualAnswer>) {
        answers = newList
        notifyDataSetChanged()
    }
}