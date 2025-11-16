package com.example.prob1.ui.tests

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.prob1.data.Question

class TestPagerAdapter(
    private val fragmentActivity: FragmentActivity,
    private val questions: List<Question>,
    private val partId: String,
    private val testId: String,
    private val isManual: Boolean
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = questions.size

    override fun createFragment(position: Int): Fragment {
        val question = questions[position]

        return if (isManual) {
            // Для ручного теста используем ManualQuestionFragment
            ManualQuestionFragment.newInstance(
                questionId = question.id,
                questionText = question.text,
                contentText = question.content ?: "",
                partId = partId,
                testId = testId,
                position = position,
                total = questions.size
            )
        } else {
            // Для обычного теста используем QuestionFragment
            QuestionFragment.newInstance(
                question = question,
                position = position,
                total = questions.size
            )
        }
    }

    override fun getItemId(position: Int): Long {
        return questions[position].id.hashCode().toLong()
    }

    override fun containsItem(itemId: Long): Boolean {
        return questions.any { it.id.hashCode().toLong() == itemId }
    }
}