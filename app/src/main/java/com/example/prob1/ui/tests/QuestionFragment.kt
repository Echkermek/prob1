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
    private var answerAdapter: AnswersAdapter? = null
    private var isAnswerSelected = false

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
        setupNextButton()
    }

    override fun onResume() {
        super.onResume()
        // Перезапускаем таймер только для обычных вопросов и если ответ еще не выбран
        if (!isAnswerSelected) {
            (activity as? TestActivity)?.startQuestionTimerIfNeeded()
        }
    }

    private fun setupViews() {
        binding.questionText.text = question?.text ?: "Вопрос не загружен"
        binding.questionNumber.text = "Вопрос ${currentPosition + 1}/$totalQuestions"

        // Изначально скрываем кнопку "Далее"
        binding.nextButton.visibility = View.GONE
        binding.finishButton.visibility = View.GONE
    }

    private fun setupNextButton() {
        binding.nextButton.setOnClickListener {
            (activity as? TestActivity)?.moveToNextQuestion()
        }
    }

    private fun loadAnswers() {
        val answers = question?.answers ?: emptyList()
        setupAnswersRecycler(answers)
    }

    private fun setupAnswersRecycler(answers: List<Answer>) {
        answerAdapter = AnswersAdapter(answers) { isCorrect, isUserSelected ->
            if (isUserSelected && !isAnswerSelected) {
                isAnswerSelected = true

                // Отключаем дальнейший выбор
                answerAdapter?.disableSelection()

                // Сообщаем Activity о выборе ответа
                (activity as? TestActivity)?.onAnswerSelected(isCorrect)

                // Показываем кнопку "Далее" вместо автоматического перехода
                showNextButton()

                // Останавливаем таймер, так как ответ уже выбран
                (activity as? TestActivity)?.stopTimer()
            }
        }

        binding.answersRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = answerAdapter
        }
    }

    private fun showNextButton() {
        // Показываем кнопку "Далее"
        binding.nextButton.visibility = View.VISIBLE

        // Скрываем кнопку завершения если она есть
        binding.finishButton.visibility = View.GONE

        // Обновляем текст кнопки в зависимости от того, последний ли вопрос
        if (currentPosition == totalQuestions - 1) {
            binding.nextButton.text = "Завершить"
        } else {
            binding.nextButton.text = "Следующий вопрос →"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        answerAdapter = null
    }
}