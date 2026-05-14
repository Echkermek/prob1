package com.example.prob1.ui.tests

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
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
    private lateinit var spinnerContainer: LinearLayout

    private val db = Firebase.firestore

    private var questionId: String = ""
    private var questionTextStr: String = ""
    private var contentTextStr: String = ""
    private var partId: String = ""
    private var testId: String = ""
    private var position: Int = 0
    private var total: Int = 0

    private val spinners = mutableListOf<Spinner>()

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

    @SuppressLint("MissingInflatedId")
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
        spinnerContainer = view.findViewById(R.id.spinnerContainer)

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

        val activity = activity as? TestActivity
        val raw = activity?.getQuestionRawData(questionId)

        // Получаем тип теста из Activity
        val testType = activity?.getTestType()
        val partType = raw?.get("type") as? String
        val questionType = raw?.get("questionType") as? String
        val isManualInput = raw?.get("isManualInput") as? Boolean ?: false
        val enterAnswer = raw?.get("enterAnswer") as? Boolean ?: false

        // Определяем тип вопроса
        val isInputType = testType == "input" ||
                partType == "input" ||
                enterAnswer ||
                (isManualInput && questionType != "spinner_sequence" && questionType != "spinner_matching")

        val isSpinnerType = questionType == "spinner_sequence" || questionType == "spinner_matching"

        Log.d("ManualQuestionFragment", "Question $questionId: isInputType=$isInputType, isSpinnerType=$isSpinnerType, testType=$testType, partType=$partType")

        when {
            isSpinnerType -> {
                // Тип 2: Спиннеры
                answerInput.visibility = View.GONE
                spinnerContainer.visibility = View.VISIBLE
                submitButton.visibility = View.VISIBLE
                createSpinnerQuestion(raw)
            }
            isInputType -> {
                // Тип 3: Текстовый ввод
                answerInput.visibility = View.VISIBLE
                spinnerContainer.visibility = View.GONE
                submitButton.visibility = View.VISIBLE
                answerInput.setText("")
                answerInput.hint = "Введите ваш ответ здесь..."
            }
            else -> {
                // Fallback: показываем текстовое поле
                answerInput.visibility = View.VISIBLE
                spinnerContainer.visibility = View.GONE
                submitButton.visibility = View.VISIBLE
            }
        }
    }

    private fun setupClickListeners() {
        submitButton.setOnClickListener {
            val activity = activity as? TestActivity
            val raw = activity?.getQuestionRawData(questionId)
            val questionType = raw?.get("questionType") as? String ?: "manual_text"

            val isSpinnerType = questionType == "spinner_sequence" || questionType == "spinner_matching"

            if (isSpinnerType) {
                submitSpinnerAnswer(raw)
            } else {
                submitTextAnswer()
            }
        }
    }

    private fun createSpinnerQuestion(raw: Map<String, Any>?) {
        spinnerContainer.removeAllViews()
        spinners.clear()

        val labels = raw.getStringList("spinnerLabels")
        val leftItems = raw.getStringList("leftItems")
        val spinnerOptions = raw.getStringList("spinnerOptions")
        val optionTexts = raw.getStringList("optionTexts")

        if (labels.isEmpty() || spinnerOptions.isEmpty()) {
            Toast.makeText(requireContext(), "Ошибка настройки вопроса", Toast.LENGTH_SHORT).show()
            return
        }

        val visibleOptions = if (optionTexts.isNotEmpty()) optionTexts else spinnerOptions

        labels.forEachIndexed { index, label ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 10, 0, 10)
            }

            val labelText = if (leftItems.isNotEmpty() && index < leftItems.size) {
                "$label) ${leftItems[index]}"
            } else {
                "$label)"
            }

            val tv = TextView(requireContext()).apply {
                text = labelText
                textSize = 16f
                setTextColor(Color.WHITE)
            }

            val spinner = Spinner(requireContext())
            val adapter = ArrayAdapter(
                requireContext(),
                R.layout.spinner_item_white,
                listOf("Выберите") + visibleOptions
            )
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_white)
            spinner.adapter = adapter

            row.addView(tv)
            row.addView(spinner)

            spinnerContainer.addView(row)
            spinners.add(spinner)
        }
    }

    private fun submitSpinnerAnswer(raw: Map<String, Any>?) {
        val spinnerOptions = raw.getStringList("spinnerOptions")

        if (spinnerOptions.isEmpty()) {
            Toast.makeText(requireContext(), "Ошибка вариантов ответа", Toast.LENGTH_SHORT).show()
            return
        }

        val result = mutableListOf<String>()

        for (spinner in spinners) {
            val selectedPosition = spinner.selectedItemPosition

            if (selectedPosition == 0) {
                Toast.makeText(requireContext(), "Заполните все ответы", Toast.LENGTH_SHORT).show()
                return
            }

            result.add(spinnerOptions[selectedPosition - 1].trim().uppercase())
        }

        val answer = result.joinToString(",")

        saveManualAnswer(answer)
    }

    private fun submitTextAnswer() {
        val answerText = answerInput.text.toString().trim()
        Log.d("ManualQuestionFragment", "submitTextAnswer - answerText: $answerText")

        if (answerText.isEmpty()) {
            Toast.makeText(requireContext(), "Введите ответ", Toast.LENGTH_SHORT).show()
            return
        }

        // Сначала сохраняем в Activity
        (activity as? TestActivity)?.onManualAnswerSubmitted(questionId, answerText)

        // Затем сохраняем в Firestore
        saveManualAnswer(answerText)
    }

    private fun saveManualAnswer(answerText: String) {
        val activity = requireActivity() as? TestActivity
        val attemptId = activity?.attemptDocId

        Log.d("ManualQuestionFragment", "=== SAVING ANSWER ===")
        Log.d("ManualQuestionFragment", "attemptId: $attemptId")
        Log.d("ManualQuestionFragment", "questionId: $questionId")
        Log.d("ManualQuestionFragment", "answerText: $answerText")
        Log.d("ManualQuestionFragment", "questionTextStr: $questionTextStr")

        if (attemptId != null) {
            val updates = hashMapOf<String, Any>(
                "manualAnswer" to answerText,
                "manualQuestionId" to questionId,
                "questionText" to questionTextStr,  // Сохраняем текст вопроса
                "timestamp" to FieldValue.serverTimestamp()
            )

            db.collection("test_attempts").document(attemptId)
                .update(updates)
                .addOnSuccessListener {
                    Log.d("ManualQuestionFragment", "Answer saved successfully!")
                    Toast.makeText(requireContext(), "Ответ сохранён", Toast.LENGTH_SHORT).show()

                    if (position < total - 1) {
                        activity.moveToNextQuestion()
                    } else {
                        activity.finishTest()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ManualQuestionFragment", "Error saving: ${e.message}", e)
                    Toast.makeText(requireContext(), "Ошибка сохранения: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Log.e("ManualQuestionFragment", "attemptId is null!")
            Toast.makeText(requireContext(), "Ошибка: попытка не найдена", Toast.LENGTH_SHORT).show()
        }
    }

    private fun Map<String, Any>?.getStringList(key: String): List<String> {
        return (this?.get(key) as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
    }
}