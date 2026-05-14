package com.example.prob1.ui.tests

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.prob1.R
import com.example.prob1.data.Test

// Определяем класс данных здесь же
data class TestWithDeadline(
    val test: Test,
    val deadline: String?
)

class DebtTestsAdapter(
    private val tests: List<TestWithDeadline>,
    private val onTestClick: (String) -> Unit
) : RecyclerView.Adapter<DebtTestsAdapter.DebtTestViewHolder>() {

    inner class DebtTestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val testTitle: TextView = itemView.findViewById(R.id.testTitle)
        val testDeadline: TextView = itemView.findViewById(R.id.testDeadline)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onTestClick(tests[position].test.id)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DebtTestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_debt_test, parent, false)
        return DebtTestViewHolder(view)
    }

    override fun onBindViewHolder(holder: DebtTestViewHolder, position: Int) {
        val testWithDeadline = tests[position]
        holder.testTitle.text = testWithDeadline.test.title

        // Для тестов из долга НЕ показываем дедлайн
        holder.testDeadline.visibility = View.GONE
    }

    override fun getItemCount(): Int = tests.size
}