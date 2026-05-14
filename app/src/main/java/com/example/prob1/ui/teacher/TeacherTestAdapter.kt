package com.example.prob1.ui.teacher

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.prob1.R

class TeacherTestAdapter(
    private var tests: List<DeadlinesActivity.Test>,
    private var deadlines: Map<String, String>,
    private val onClick: (DeadlinesActivity.Test) -> Unit
) : RecyclerView.Adapter<TeacherTestAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val testNumber: TextView = itemView.findViewById(R.id.testNumber)
        val testTitle: TextView = itemView.findViewById(R.id.testTitle)
        val testDeadline: TextView = itemView.findViewById(R.id.testDeadline)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_test, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val test = tests[position]

        holder.testNumber.text = "Тест ${position + 1}"
        holder.testTitle.text = test.title

        val deadline = deadlines[test.id]

        if (!deadline.isNullOrEmpty()) {
            // Форматируем дату из yyyy-MM-dd в dd.MM.yyyy для отображения
            val formattedDate = try {
                val parts = deadline.split("-")
                if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else deadline
            } catch (e: Exception) {
                deadline
            }
            holder.testDeadline.text = "Срок сдачи: $formattedDate"
        } else {
            holder.testDeadline.text = "Срок сдачи: не установлен"
        }

        holder.itemView.setOnClickListener {
            onClick(test)
        }
    }

    override fun getItemCount() = tests.size

    fun updateTests(newTests: List<DeadlinesActivity.Test>, newDeadlines: Map<String, String>) {
        this.tests = newTests
        this.deadlines = newDeadlines
        notifyDataSetChanged()
    }
}