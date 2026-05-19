package com.manekelsa.app.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import com.google.firebase.auth.FirebaseAuth
import com.google.mlkit.nl.translate.*
import com.manekelsa.app.LanguageManager
import com.manekelsa.app.data.firebase.FirebaseService
import com.manekelsa.app.data.model.Worker
import com.manekelsa.app.data.repository.AuthRepository
import com.manekelsa.app.data.repository.WorkerRepository
import com.manekelsa.app.databinding.FragmentProfileBinding
import com.manekelsa.app.util.LocationUtil
import com.manekelsa.app.util.Resource
import com.manekelsa.app.util.TranslatorUtil
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // ✅ FIX: ADD THIS (missing earlier)
    private val firebaseService = FirebaseService()

    private lateinit var translator: Translator

    private val viewModel: ProfileViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val service = FirebaseService()
                @Suppress("UNCHECKED_CAST")
                return ProfileViewModel(
                    WorkerRepository(service),
                    AuthRepository(service)
                ) as T
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // LOCATION PERMISSION
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
        }

        // ML KIT TRANSLATOR
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.KANNADA)
            .build()

        translator = Translation.getClient(options)
        translator.downloadModelIfNeeded()

        // AUTO TRANSLATION
        binding.etName.addTextChangedListener {
            val input = it.toString()
            if (input.isNotEmpty()) {
                viewLifecycleOwner.lifecycleScope.launch {
                    binding.etNameKn.setText(TranslatorUtil.translate(input))
                }
            }
        }

        binding.etSkill.addTextChangedListener {
            val input = it.toString()
            if (input.isNotEmpty()) {
                viewLifecycleOwner.lifecycleScope.launch {
                    binding.etSkillKn.setText(TranslatorUtil.translate(input))
                }
            }
        }

        binding.etArea.addTextChangedListener {
            val input = it.toString()
            if (input.isNotEmpty()) {
                viewLifecycleOwner.lifecycleScope.launch {
                    binding.etAreaKn.setText(TranslatorUtil.translate(input))
                }
            }
        }

        // LANGUAGE UI
        val isKannada = LanguageManager.isKannada(requireContext())

        binding.etName.visibility = if (isKannada) View.GONE else View.VISIBLE
        binding.etSkill.visibility = if (isKannada) View.GONE else View.VISIBLE
        binding.etArea.visibility = if (isKannada) View.GONE else View.VISIBLE

        binding.etNameKn.visibility = if (isKannada) View.VISIBLE else View.GONE
        binding.etSkillKn.visibility = if (isKannada) View.VISIBLE else View.GONE
        binding.etAreaKn.visibility = if (isKannada) View.VISIBLE else View.GONE

        binding.btnSaveProfile.setOnClickListener {
            saveProfile()
        }

        observeViewModel()
    }

    // ✅ FIXED SAVE PROFILE
    private fun saveProfile() {

        viewLifecycleOwner.lifecycleScope.launch {

            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val loc = LocationUtil.getCurrentLocation(requireContext())

            // ✅ fallback (Bangalore)
            val lat = loc?.first ?: 12.9716
            val lng = loc?.second ?: 77.5946

            Log.d("GPS_DEBUG", "FINAL LAT = $lat, LNG = $lng")

            val worker = Worker(
                id = "",

                nameEn = binding.etName.text.toString().trim(),
                nameKn = binding.etNameKn.text.toString().trim(),

                skillEn = binding.etSkill.text.toString().trim(),
                skillKn = binding.etSkillKn.text.toString().trim(),

                areaEn = binding.etArea.text.toString().trim(),
                areaKn = binding.etAreaKn.text.toString().trim(),

                latitude = lat,
                longitude = lng,

                distance = 0.0, // runtime only

                isAvailable = true,

                rate = binding.etRate.text.toString().trim(),
                phone = binding.etPhone.text.toString().trim(),

                ownerId = firebaseService.getCurrentUser()?.uid ?: ""
            )

            Log.d("WORKER_DEBUG", worker.toString())

            viewModel.saveProfile(worker)
        }
    }

    private fun observeViewModel() {

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.saveState.collect { state ->
                    when (state) {
                        is Resource.Loading -> binding.btnSaveProfile.isEnabled = false

                        is Resource.Success -> {
                            binding.btnSaveProfile.isEnabled = true
                            Toast.makeText(requireContext(), "Profile saved!", Toast.LENGTH_SHORT).show()
                        }

                        is Resource.Error -> {
                            binding.btnSaveProfile.isEnabled = true
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        }

                        null -> binding.btnSaveProfile.isEnabled = true
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        translator.close()
        _binding = null
    }
}