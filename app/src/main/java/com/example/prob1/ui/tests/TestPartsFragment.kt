package com.example.prob1.ui.tests

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prob1.LectionWebViewActivity
import com.example.prob1.R
import com.example.prob1.data.Part
import com.example.prob1.databinding.FragmentTestPartsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TestPartsFragment : Fragment() {
    private var _binding: FragmentTestPartsBinding? = null
    private val binding get() = _binding!!
    private val db = Firebase.firestore
    private var testId: String? = null
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        testId = arguments?.getString("testId")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTestPartsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadParts()
    }

    private fun setupRecyclerView() {
        binding.partsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.partsRecycler.adapter = PartsAdapter { partId, _, attempts, isManual ->
            lifecycleScope.launch {
                // РУЧНЫЕ ТЕСТЫ — БЕЗ ПРОВЕРКИ ЛЕКЦИИ
                if (isManual) {
                    if (hasManualAnswer(partId)) {
                        AlertDialog.Builder(requireContext())
                            .setMessage("Вы уже отправили ответ. Ожидайте оценку.")
                            .setPositiveButton("OK", null).show()
                    } else {
                        startTest(partId, isManual)
                    }
                    return@launch
                }

                // ОБЫЧНЫЕ ТЕСТЫ — ПРОВЕРЯЕМ ЛЕКЦИЮ
                val lectionId = getLectionId(partId) ?: run {
                    Toast.makeText(requireContext(), "Лекция не привязана", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                if (lectionId.isEmpty()) {
                    Toast.makeText(requireContext(), "Лекция не указана", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val readCount = getReadCount(lectionId)

                if (readCount > attempts) {
                    startTest(partId, isManual)
                } else {
                    val lectionTitle = getLectionTitle(lectionId) ?: "лекцию"
                    val lectionNum = getLectionNum(lectionId) ?: ""
                    AlertDialog.Builder(requireContext())
                        .setMessage("Прочитайте $lectionTitle $lectionNum")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }

    private suspend fun getLectionId(partId: String): String? {
        return db.document("tests/$testId/parts/$partId").get().await().getString("lecId")
    }

    private suspend fun getLectionTitle(lectionId: String): String? {
        return db.document("lections/$lectionId").get().await().getString("name")
    }

    private suspend fun getLectionNum(lectionId: String): String? {
        return db.document("lections/$lectionId").get().await().getString("num")
    }

    private suspend fun isLectureRead(lectionId: String): Boolean {
        val snap = db.collection("user_lections")
            .whereEqualTo("userId", userId)
            .whereEqualTo("lectionId", lectionId)
            .get().await()
        return !snap.isEmpty
    }

    private suspend fun getReadCount(lectionId: String): Int {
        val snap = db.collection("user_lections")
            .whereEqualTo("userId", userId)
            .whereEqualTo("lectionId", lectionId)
            .get().await()
        return if (snap.isEmpty) 0 else snap.documents[0].getLong("readCount")?.toInt() ?: 0
    }

    private suspend fun hasManualAnswer(partId: String): Boolean {
        val snap = db.collection("manual_answers")
            .whereEqualTo("userId", userId)
            .whereEqualTo("partId", partId)
            .get().await()
        return !snap.isEmpty
    }

    private fun startTest(partId: String, isManual: Boolean) {
        val intent = Intent(requireActivity(), TestActivity::class.java).apply {
            putExtra("partId", partId)
            putExtra("testId", testId)
            putExtra("isManual", isManual)
        }
        startActivity(intent)
    }

    private fun loadParts() {
        testId?.let { id ->
            lifecycleScope.launch {
                val snap = db.collection("tests/$id/parts").get().await()
                val parts = snap.documents.mapNotNull {
                    Part(
                        id = it.id,
                        title = it.getString("title") ?: "",
                        num = it.getLong("num")?.toInt() ?: 0,
                        enterAnswer = it.getBoolean("enterAnswer") ?: false,
                        idLectures = it.getString("lecId") ?: ""
                    )
                }.sortedBy { it.num }
                (binding.partsRecycler.adapter as PartsAdapter).submitList(parts)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}