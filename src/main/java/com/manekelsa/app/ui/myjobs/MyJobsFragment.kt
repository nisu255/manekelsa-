package com.manekelsa.app.ui.myjobs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.manekelsa.app.LanguageManager
import com.manekelsa.app.data.firebase.FirebaseService
import com.manekelsa.app.data.repository.AuthRepository
import com.manekelsa.app.data.repository.WorkerRepository
import com.manekelsa.app.databinding.FragmentMyJobsBinding
import com.manekelsa.app.ui.directory.WorkerAdapter
import com.manekelsa.app.util.Resource
import kotlinx.coroutines.launch

class MyJobsFragment : Fragment() {

    private var _binding: FragmentMyJobsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: WorkerAdapter

    // ✅ FIX 1: PASS AuthRepository ALSO
    private val viewModel: MyJobsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return MyJobsViewModel(
                    WorkerRepository(FirebaseService()),
                    AuthRepository(FirebaseService())
                ) as T
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyJobsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        adapter = WorkerAdapter(
            onThumbsUp = { worker ->
                viewModel.thumbsUpWorker(worker.id)
            },
            onAvailabilityToggle = { _, _ -> }, // not needed here
            currentUserId = currentUid,
            isKannada = LanguageManager.isKannada(requireContext())
        )

        binding.rvAvailableWorkers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MyJobsFragment.adapter
        }

        applyLanguage()
        observeData()
    }

    // ✅ LANGUAGE
    private fun applyLanguage() {
        val isKannada = LanguageManager.isKannada(requireContext())

        binding.tvHeader.text = if (isKannada)
            "ನನ್ನ ಕೆಲಸಗಳು"
        else
            "My Jobs"

        adapter.isKannada = isKannada
    }

    // ✅ FIX 2: USE myJobs INSTEAD OF availableWorkers
    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                viewModel.myJobs.collect { state ->

                    when (state) {

                        is Resource.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                        }

                        is Resource.Success -> {
                            binding.progressBar.visibility = View.GONE

                            val list = state.data ?: emptyList()

                            if (list.isEmpty()) {
                                binding.tvEmpty.visibility = View.VISIBLE
                                binding.rvAvailableWorkers.visibility = View.GONE
                            } else {
                                binding.tvEmpty.visibility = View.GONE
                                binding.rvAvailableWorkers.visibility = View.VISIBLE

                                adapter.submitList(list)
                            }
                        }

                        is Resource.Error -> {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applyLanguage()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}