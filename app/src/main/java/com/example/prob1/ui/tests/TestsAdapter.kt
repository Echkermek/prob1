package com.example.prob1.ui.tests

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.prob1.databinding.ItemTestBinding
import com.example.prob1.data.Test

class TestsAdapter(
    private var deadlines: Map<String, String>,
    private val onTestClick: (testId: String, semester: Int, hasDeadline: Boolean) -> Unit
) : RecyclerView.Adapter<TestsAdapter.TestViewHolder>() {

    private var tests: List<Test> = emptyList()
    private var testCompletionStatus = mutableMapOf<String, Boolean>() // testId -> isCompleted

    class TestViewHolder(val binding: ItemTestBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestViewHolder {
        val binding = ItemTestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TestViewHolder, position: Int) {
        val test = tests[position]

        holder.binding.apply {
            testTitle.text = test.title
            testNumber.text = "Тест ${position + 1}"

            val deadline = deadlines[test.id]
            val hasDeadline = !deadline.isNullOrEmpty()

            if (hasDeadline) {
                testDeadline.text = "Срок сдачи: $deadline"
                testDeadline.visibility = View.VISIBLE
            } else {
                testDeadline.text = "Срок сдачи не установлен"
                testDeadline.visibility = View.VISIBLE
            }

            // Отображаем статус прохождения теста
            val isCompleted = testCompletionStatus[test.id] == true
            if (isCompleted) {
                testStatus.text = "Пройден"
                testStatus.visibility = View.VISIBLE
                testStatus.setTextColor(Color.parseColor("#4CAF50")) // Зеленый цвет
            } else {
                testStatus.visibility = View.GONE
            }

            root.setOnClickListener {
                onTestClick(test.id, test.semester, hasDeadline)
            }
        }
    }

    override fun getItemCount(): Int = tests.size

    fun submitList(newTests: List<Test>) {
        this.tests = newTests
        notifyDataSetChanged()
    }

    fun updateDeadlines(newDeadlines: Map<String, String>) {
        this.deadlines = newDeadlines
        notifyDataSetChanged()
    }

    fun updateTestCompletionStatus(status: Map<String, Boolean>) {
        this.testCompletionStatus.clear()
        this.testCompletionStatus.putAll(status)
        notifyDataSetChanged()
    }
}