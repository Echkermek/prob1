package com.example.prob1.ui.tests

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.prob1.R

class ManualQuestionFragment : Fragment() {

    private lateinit var questionText: TextView
    private lateinit var contentText: TextView
    private lateinit var answerInput: EditText
    private lateinit var submitButton: Button
    private lateinit var positionText: TextView

    private var questionId: String = ""
    private var questionTextStr: String = ""
    private var contentTextStr: String = ""
    private var partId: String = ""
    private var testId: String = ""
    private var position: Int = 0
    private var total: Int = 0

    companion object {
        fun newInstance(
            questionId: String,
            questionText: String,
            contentText: String,
            partId: String,
            testId: String,
            position: Int,
            total: Int
        ): ManualQuestionFragment {
            val fragment = ManualQuestionFragment()
            val args = Bundle().apply {
                putString("questionId", questionId)
                putString("questionText", questionText)
                putString("contentText", contentText)
                putString("partId", partId)
                putString("testId", testId)
                putInt("position", position)
                putInt("total", total)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            questionId = it.getString("questionId") ?: ""
            questionTextStr = it.getString("questionText") ?: ""
            contentTextStr = it.getString("contentText") ?: ""
            partId = it.getString("partId") ?: ""
            testId = it.getString("testId") ?: ""
            position = it.getInt("position")
            total = it.getInt("total")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_manual_question, container, false)

        questionText = view.findViewById(R.id.questionText)
        contentText = view.findViewById(R.id.contentText)
        answerInput = view.findViewById(R.id.manualAnswerInput)
        submitButton = view.findViewById(R.id.submitManualAnswer)
        positionText = view.findViewById(R.id.positionText)

        setupUI()
        setupClickListeners()

        return view
    }

    private fun setupUI() {
        questionText.text = questionTextStr
        if (contentTextStr.isNotEmpty()) {
            contentText.text = contentTextStr
            contentText.visibility = View.VISIBLE
        } else {
            contentText.visibility = View.GONE
        }
        positionText.text = "Вопрос ${position + 1} из $total"
    }

    private fun setupClickListeners() {
        submitButton.setOnClickListener {
            val answerText = answerInput.text.toString().trim()

            if (answerText.isEmpty()) {
                Toast.makeText(requireContext(), "Введите ответ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Передаём ответ в TestActivity
            (activity as? TestActivity)?.onManualAnswerSubmitted(questionId, answerText)

            // Переходим к следующему вопросу или завершаем тест
            if (position < total - 1) {
                (activity as? TestActivity)?.moveToNextQuestion()
            } else {
                (activity as? TestActivity)?.finishTest()
            }
        }
    }
}