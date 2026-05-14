// com/example/prob1/viewmodel/MainViewModel.kt
package com.example.prob1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.prob1.data.Lection
import com.example.prob1.data.Test
import com.example.prob1.data.database.entities.UserDataEntity
import com.example.prob1.data.database.repository.LectionRepository
import com.example.prob1.data.database.repository.TestRepository
import com.example.prob1.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = UserRepository(application)
    private val testRepository = TestRepository(application)
    private val lectionRepository = LectionRepository(application)

    // ==================== Состояния ====================

    private val _userData = MutableStateFlow<UserDataEntity?>(null)
    val userData: StateFlow<UserDataEntity?> = _userData.asStateFlow()

    private val _tests = MutableStateFlow<List<Test>>(emptyList())
    val tests: StateFlow<List<Test>> = _tests.asStateFlow()

    private val _lections = MutableStateFlow<List<Lection>>(emptyList())
    val lections: StateFlow<List<Lection>> = _lections.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ==================== Флаги загрузки ====================

    private var userDataLoaded = false
    private var testsLoaded = false
    private var lectionsLoaded = false

    // ==================== Загрузка данных ====================

    fun loadUserData(userId: String, forceRefresh: Boolean = false) {
        if (userDataLoaded && !forceRefresh && _userData.value != null) {
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val data = userRepository.getUserData(userId, forceRefresh)
                _userData.value = data
                userDataLoaded = true

                // После загрузки пользователя загружаем тесты и лекции
                data?.courseId?.let { courseId ->
                    loadTestsForCourse(courseId, forceRefresh)
                    loadLectionsForCourse(courseId, forceRefresh)
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadTestsForCourse(courseId: String, forceRefresh: Boolean) {
        if (testsLoaded && !forceRefresh && _tests.value.isNotEmpty()) {
            return
        }

        try {
            val testEntities = testRepository.getTestsForCourse(courseId, forceRefresh)
            _tests.value = testEntities.map { entity ->
                Test(
                    id = entity.id,
                    title = entity.title,
                    num = entity.num,
                    semester = entity.semester,
                    isAvailable = entity.isAvailable,
                    hasParts = entity.hasParts
                )
            }.sortedBy { it.num }
            testsLoaded = true
        } catch (e: Exception) {
            _error.value = e.message
        }
    }

    private suspend fun loadLectionsForCourse(courseId: String, forceRefresh: Boolean) {
        if (lectionsLoaded && !forceRefresh && _lections.value.isNotEmpty()) {
            return
        }

        try {
            _lections.value = lectionRepository.getLectionsForCourse(courseId, forceRefresh)
                .sortedBy { it.num.toFloatOrNull() ?: 0f }
            lectionsLoaded = true
        } catch (e: Exception) {
            _error.value = e.message
        }
    }

    // ==================== Обновление данных ====================

    fun refreshAllData(userId: String) {
        userDataLoaded = false
        testsLoaded = false
        lectionsLoaded = false
        loadUserData(userId, forceRefresh = true)
    }

    fun clearError() {
        _error.value = null
    }

    // ==================== Обновление монет ====================

    fun updateCoins(newCoins: Int) {
        _userData.value = _userData.value?.copy(coins = newCoins)
    }

    // ==================== Очистка при выходе ====================

    fun clearData() {
        _userData.value = null
        _tests.value = emptyList()
        _lections.value = emptyList()
        userDataLoaded = false
        testsLoaded = false
        lectionsLoaded = false
    }
}