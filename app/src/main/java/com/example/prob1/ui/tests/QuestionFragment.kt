package com.example.prob1.ui.tests

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prob1.data.Answer
import com.example.prob1.data.Question
import com.example.prob1.databinding.FragmentQuestionBinding

class QuestionFragment : Fragment() {

    private var _binding: FragmentQuestionBinding? = null
    private val binding get() = _binding!!

    private var question: Question? = null
    private var currentPosition: Int = 0
    private var totalQuestions: Int = 0

    companion object {
        private const val ARG_QUESTION = "question"
        private const val ARG_POSITION = "position"
        private const val ARG_TOTAL = "total"

        fun newInstance(question: Question, position: Int, total: Int): QuestionFragment {
            return QuestionFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_QUESTION, question)
                    putInt(ARG_POSITION, position)
                    putInt(ARG_TOTAL, total)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            question = it.getSerializable(ARG_QUESTION) as? Question
            currentPosition = it.getInt(ARG_POSITION, 0)
            totalQuestions = it.getInt(ARG_TOTAL, 0)

            Log.d("QuestionFragment", "position=$currentPosition, total=$totalQuestions")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuestionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        loadAnswers()
    }

    override fun onResume() {
        super.onResume()
        // Перезапускаем таймер только для обычных вопросов (Task 1)
        (activity as? TestActivity)?.startQuestionTimerIfNeeded()
    }

    private fun setupViews() {
        binding.questionText.text = question?.text ?: "Вопрос не загружен"
        binding.questionNumber.text = "Вопрос ${currentPosition + 1}/$totalQuestions"

        // Скрываем кнопку завершения (она только в ManualQuestionFragment)
        binding.finishButton.visibility = View.GONE
    }

    private fun loadAnswers() {
        val answers = question?.answers ?: emptyList()
        setupAnswersRecycler(answers)
    }

    private fun setupAnswersRecycler(answers: List<Answer>) {
        binding.answersRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = AnswersAdapter(answers) { isCorrect ->
                (activity as? TestActivity)?.onAnswerSelected(isCorrect)
                (adapter as? AnswersAdapter)?.disableSelection()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}