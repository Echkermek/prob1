package com.example.prob1.ui.tests

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.prob1.data.Answer
import com.example.prob1.databinding.ItemAnswerBinding

class AnswersAdapter(
    private val answers: List<Answer>,
    private val onAnswerSelected: (Boolean, Boolean) -> Unit  // Изменен callback: передаем isCorrect и isUserSelected
) : RecyclerView.Adapter<AnswersAdapter.AnswerViewHolder>() {

    private var selectedPosition = -1
    private var isAnswerSelected = false
    private var isSelectionDisabled = false

    inner class AnswerViewHolder(val binding: ItemAnswerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(answer: Answer) {
            binding.answerText.text = answer.text

            if (isAnswerSelected && adapterPosition == selectedPosition) {
                val color = if (answer.isCorrect) Color.GREEN else Color.RED
                binding.cardView.setCardBackgroundColor(color)
                binding.answerText.setTextColor(Color.WHITE)
            } else {
                binding.cardView.setCardBackgroundColor(Color.WHITE)
                binding.answerText.setTextColor(Color.BLACK)
            }

            binding.root.setOnClickListener {
                if (!isAnswerSelected && !isSelectionDisabled) {
                    selectedPosition = adapterPosition
                    isAnswerSelected = true
                    // Передаем isCorrect И то, что это выбор пользователя
                    onAnswerSelected(answer.isCorrect, true)
                    notifyDataSetChanged()
                }
            }
        }
    }

    fun disableSelection() {
        isSelectionDisabled = true
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnswerViewHolder {
        val binding = ItemAnswerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AnswerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AnswerViewHolder, position: Int) {
        holder.bind(answers[position])
    }

    override fun getItemCount() = answers.size

    fun resetSelection() {
        selectedPosition = -1
        isAnswerSelected = false
        notifyDataSetChanged()
    }
}