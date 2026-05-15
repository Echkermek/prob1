// com/example/prob1/base/BaseFragment.kt
package com.example.prob1.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.example.prob1.viewmodel.MainViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    private var _binding: VB? = null
    protected val binding get() = _binding!!

    private val jobs = mutableListOf<Job>()

    protected lateinit var mainViewModel: MainViewModel

    abstract fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): VB
    abstract fun onViewCreatedSafe(savedInstanceState: Bundle?)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = inflateBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализируем ViewModel через Activity
        try {
            val mainActivity = requireActivity() as com.example.prob1.MainActivity
            mainViewModel = mainActivity.getMainViewModel()
        } catch (e: Exception) {
            // Если не MainActivity, создаем через ViewModelProvider
            mainViewModel = androidx.lifecycle.ViewModelProvider(requireActivity())[MainViewModel::class.java]
        }

        onViewCreatedSafe(savedInstanceState)
    }

    protected fun launchSafe(block: suspend () -> Unit): Job {
        return viewLifecycleOwner.lifecycleScope.launch {
            if (isAdded && view != null && !isDetached && !isRemoving) {
                try {
                    block()
                } catch (e: Exception) {
                    handleError(e)
                }
            }
        }.also { jobs.add(it) }
    }

    protected val isUiSafe: Boolean
        get() = isAdded && view != null && !isDetached && !isRemoving

    protected fun showToast(message: String) {
        if (isAdded && context != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    protected open fun handleError(e: Exception) {
        e.printStackTrace()
        showToast(e.message ?: "Произошла ошибка")
    }

    override fun onDestroyView() {
        jobs.forEach {
            if (!it.isCancelled) it.cancel()
        }
        jobs.clear()
        _binding = null
        super.onDestroyView()
    }
}