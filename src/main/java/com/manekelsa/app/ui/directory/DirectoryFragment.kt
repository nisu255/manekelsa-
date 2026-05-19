package com.manekelsa.app.ui.directory

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.manekelsa.app.R
import com.manekelsa.app.data.firebase.FirebaseService
import com.manekelsa.app.data.repository.WorkerRepository
import com.manekelsa.app.databinding.FragmentDirectoryBinding
import com.manekelsa.app.util.Resource
import com.manekelsa.app.LanguageManager
import com.manekelsa.app.util.LocationUtil
import com.manekelsa.app.util.DistanceUtil
import kotlinx.coroutines.launch

class DirectoryFragment : Fragment() {

    private var _binding: FragmentDirectoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var workerAdapter: WorkerAdapter

    private val skills = listOf("All", "Cleaning", "Gardening", "Cooking", "Plumbing")
    private val skillsKn = listOf("ಎಲ್ಲಾ", "ಸ್ವಚ್ಛತೆ", "ತೋಟಗಾರಿಕೆ", "ಅಡುಗೆ", "ಪ್ಲಂಬಿಂಗ್")

    private val viewModel: DirectoryViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return DirectoryViewModel(WorkerRepository(FirebaseService())) as T
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDirectoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        requestLocationAndLoadWorkers()
    }

    private fun setupUI() {

        var isKannada = LanguageManager.isKannada(requireContext())
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        workerAdapter = WorkerAdapter(
            onThumbsUp = { worker -> viewModel.thumbsUpWorker(worker.id) },
            onAvailabilityToggle = { worker, available ->
                viewModel.updateAvailability(worker.id, available)
            },
            currentUserId = currentUid,
            isKannada = isKannada
        )

        binding.rvWorkers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = workerAdapter
        }

        setupSkillChips(isKannada)

        binding.chipAvailableOnly.setOnCheckedChangeListener { _, checked ->
            viewModel.setAvailableOnlyFilter(checked)
        }

        binding.btnLangToggle.setOnClickListener {
            isKannada = !isKannada
            LanguageManager.setLanguage(requireContext(), isKannada)

            updateLanguage(isKannada)
            workerAdapter.isKannada = isKannada
        }

        binding.btnSeedData.setOnClickListener {
            viewModel.seedData()
            binding.btnSeedData.visibility = View.GONE
        }

        updateLanguage(isKannada)
        observeViewModel()
    }

    // ✅ LOCATION PERMISSION
    private fun requestLocationAndLoadWorkers() {

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fetchLocationAndLoad()
        } else {
            requestPermissions(
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                101
            )
        }
    }

    // ✅ FINAL CORRECT LOCATION FLOW
    private fun fetchLocationAndLoad() {
        viewLifecycleOwner.lifecycleScope.launch {

            val loc = LocationUtil.getCurrentLocation(requireContext())

            val userLat = loc?.first ?: 12.9716
            val userLng = loc?.second ?: 77.5946

            DistanceUtil.CURRENT_USER_LAT = userLat
            DistanceUtil.CURRENT_USER_LNG = userLng

            // ✅ NOW load workers AFTER location
            viewModel.loadWorkers()
        }
    }

    // ✅ PERMISSION RESULT
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 101 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            fetchLocationAndLoad()
        } else {
            Toast.makeText(
                requireContext(),
                "Location needed to show nearest workers",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupSkillChips(isKannada: Boolean) {
        binding.chipGroupSkills.removeAllViews()

        skills.forEachIndexed { index, skill ->
            val chip = Chip(requireContext()).apply {
                text = if (isKannada) skillsKn[index] else skill
                isCheckable = true
                isChecked = skill == "All"
                tag = skill
                setChipBackgroundColorResource(R.color.chip_background_selector)
                setTextColor(ContextCompat.getColorStateList(context, R.color.chip_text_selector))
            }

            chip.setOnClickListener {
                viewModel.setSkillFilter(skill)
            }

            binding.chipGroupSkills.addView(chip)
        }
    }

    private fun updateLanguage(isKannada: Boolean) {
        binding.btnLangToggle.text = if (isKannada) "EN" else "ಕನ್ನಡ"
        binding.tvTitle.text = if (isKannada) "ಮನೆ-ಕೆಲಸ" else "Mane-Kelsa"
        binding.tvSubtitle.text =
            if (isKannada) "ಸ್ಥಳೀಯ ಕೆಲಸಗಾರರ ಡೈರೆಕ್ಟರಿ" else "Local Worker Directory"

        setupSkillChips(isKannada)
    }

    private fun observeViewModel() {

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                viewModel.workersState.collect { state ->

                    when (state) {

                        is Resource.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.rvWorkers.visibility = View.GONE
                        }

                        is Resource.Success -> {
                            binding.progressBar.visibility = View.GONE
                            binding.rvWorkers.visibility = View.VISIBLE

                            val isKannada = LanguageManager.isKannada(requireContext())
                            workerAdapter.isKannada = isKannada

                            workerAdapter.submitList(state.data)

                            binding.tvWorkerCount.text =
                                "${state.data.size} ${
                                    if (isKannada) "ಹತ್ತಿರದ ಕೆಲಸಗಾರರು" else "workers nearby"
                                }"
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}