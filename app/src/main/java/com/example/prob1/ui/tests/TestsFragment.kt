package com.example.prob1.ui.tests

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prob1.R
import com.example.prob1.data.Test
import com.example.prob1.databinding.FragmentTestsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class TestsFragment : Fragment() {
    private var _binding: FragmentTestsBinding? = null
    private val binding get() = _binding!!
    private val db = Firebase.firestore
    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private var currentSemester: Int = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadUserSemesterAndTests()
    }

    private fun setupRecyclerView() {
        binding.recyclerTests.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTests.adapter = TestsAdapter { testId, semester ->
            if (semester > currentSemester) {
                Toast.makeText(
                    requireContext(),
                    "Этот тест доступен только с $semester семестра",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                findNavController().navigate(
                    R.id.action_navigation_tests_to_testPartsFragment,
                    Bundle().apply { putString("testId", testId) }
                )
            }
        }
    }

    private fun loadUserSemesterAndTests() {
        if (userId == null) {
            loadAllTests(1)
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val groupSnapshot = withContext(Dispatchers.IO) {
                    db.collection("usersgroup")
                        .whereEqualTo("userId", userId)
                        .get()
                        .await()
                }
                Log.d("TestsFragment", "Group snapshot size: ${groupSnapshot.size()}")

                if (groupSnapshot.isEmpty) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Вы не состоите в группе", Toast.LENGTH_SHORT).show()
                    }
                    loadAllTests(1)
                    return@launch
                }

                val groupId = groupSnapshot.documents[0].getString("groupId") ?: ""
                Log.d("TestsFragment", "Retrieved groupId: $groupId")
                loadUserSemester(groupId)
            } catch (e: Exception) {
                Log.e("TestsFragment", "Error loading user group", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Ошибка загрузки группы", Toast.LENGTH_SHORT).show()
                }
                loadAllTests(1)
            }
        }
    }

    private fun loadUserSemester(groupId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (groupId.isEmpty()) {
                    throw IllegalStateException("GroupId is empty")
                }

                val coursesSnapshot = withContext(Dispatchers.IO) {
                    db.collection("courses")
                        .whereEqualTo("groupId", groupId)
                        .get()
                        .await()
                }
                Log.d("TestsFragment", "Courses snapshot size: ${coursesSnapshot.size()}")

                /*if (coursesSnapshot.isEmpty) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Курсы для вашей группы не найдены", Toast.LENGTH_SHORT).show()
                    }
                    loadAllTests(1)
                    return@launch
                }*/

                val semesterValue = coursesSnapshot.documents[0].get("semester")
                currentSemester = when {
                    semesterValue == null -> 1
                    semesterValue is Long -> semesterValue.toInt()
                    semesterValue is Double -> semesterValue.toInt()
                    semesterValue is String -> semesterValue.toIntOrNull() ?: 1
                    else -> {
                        try {
                            semesterValue.toString().toIntOrNull() ?: 1
                        } catch (e: Exception) {
                            Log.e("TestsFragment", "Error parsing semester: $semesterValue", e)
                            1
                        }
                    }
                }

                Log.d("TestsFragment", "Loaded semester: $currentSemester from value: $semesterValue")
                loadAllTests(currentSemester)
            } catch (e: Exception) {
                Log.e("TestsFragment", "Error loading semester for groupId: $groupId", e)
                withContext(Dispatchers.Main) {
                    //Toast.makeText(requireContext(), "Ошибка загрузки семестра", Toast.LENGTH_SHORT).show()
                }
                loadAllTests(1)
            }
        }
    }

    private fun loadAllTests(currentSemester: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val querySnapshot = withContext(Dispatchers.IO) {
                    db.collection("tests")
                        .get()
                        .await()
                }

                val tests = querySnapshot.documents.mapNotNull { doc ->
                    try {
                        val testSemester = doc.getLong("semester")?.toInt() ?: 1
                        Test(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            num = doc.getLong("num")?.toInt() ?: 0,
                            semester = testSemester,
                            isAvailable = testSemester <= currentSemester
                        )
                    } catch (e: Exception) {
                        null
                    }
                }.sortedBy { it.num }

                withContext(Dispatchers.Main) {
                    if (tests.isEmpty()) {
                        Toast.makeText(requireContext(), "Тесты не найдены", Toast.LENGTH_SHORT).show()
                    } else {
                        (binding.recyclerTests.adapter as TestsAdapter).submitList(tests)
                    }
                }
            } catch (e: Exception) {
                Log.e("TestsFragment", "Error loading tests", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Ошибка загрузки тестов", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}