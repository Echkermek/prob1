package com.example.prob1.ui.tests

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.prob1.R
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ManualQuestionFragment : Fragment() {

    private lateinit var questionText: TextView
    private lateinit var contentText: TextView
    private lateinit var answerInput: EditText
    private lateinit var submitButton: Button
    private lateinit var positionText: TextView

    private val db = Firebase.firestore

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
            val args = Bundle()
            args.putString("questionId", questionId)
            args.putString("questionText", questionText)
            args.putString("contentText", contentText)
            args.putString("partId", partId)
            args.putString("testId", testId)
            args.putInt("position", position)
            args.putInt("total", total)
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

            // Сохраняем ручной ответ в attempt
            saveManualAnswer(answerText)
        }
    }

    private fun saveManualAnswer(answerText: String) {
        val activity = requireActivity() as? TestActivity
        val attemptId = activity?.attemptDocId

        if (attemptId != null) {
            // Сохраняем ответ в attempt
            db.collection("test_attempts").document(attemptId)
                .update(
                    "manualAnswer", answerText,
                    "manualQuestionId", questionId,
                    "timestamp", FieldValue.serverTimestamp()
                )
                .addOnSuccessListener {
                    Log.d("ManualQuestion", "Manual answer saved for question $questionId")

                    // Уведомляем активность о сохранении ответа
                    activity.onManualAnswerSaved()

                    // Переходим к следующему вопросу или завершаем тест
                    if (position < total - 1) {
                        activity.onAnswerSelected(false)
                    } else {
                        activity.finishTest()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ManualQuestion", "Error saving manual answer", e)
                    Toast.makeText(requireContext(), "Ошибка сохранения ответа", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(requireContext(), "Ошибка: attempt не найден", Toast.LENGTH_SHORT).show()
        }
    }
}