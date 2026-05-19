package com.manekelsa.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manekelsa.app.data.model.Worker
import com.manekelsa.app.data.repository.AuthRepository
import com.manekelsa.app.data.repository.WorkerRepository
import com.manekelsa.app.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val workerRepository: WorkerRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _workerProfile = MutableStateFlow<Resource<Worker?>>(Resource.Loading)
    val workerProfile: StateFlow<Resource<Worker?>> = _workerProfile.asStateFlow()

    private val _saveState = MutableStateFlow<Resource<Unit>?>(null)
    val saveState: StateFlow<Resource<Unit>?> = _saveState.asStateFlow()

    private val _availState = MutableStateFlow<Resource<Unit>?>(null)
    val availState: StateFlow<Resource<Unit>?> = _availState.asStateFlow()

    init {
        loadMyProfile()
    }

    fun loadMyProfile() {
        val uid = authRepository.getCurrentUser()?.uid ?: return

        viewModelScope.launch {
            _workerProfile.value = Resource.Loading
            _workerProfile.value = workerRepository.getWorkerByOwnerId(uid)
        }
    }

    fun saveProfile(worker: Worker) {

        val uid = authRepository.getCurrentUser()?.uid
            ?: run {
                _saveState.value = Resource.Error("Not logged in")
                return
            }

        viewModelScope.launch {

            _saveState.value = Resource.Loading

            val workerWithOwner = worker.copy(ownerId = uid)

            val current = (_workerProfile.value as? Resource.Success)?.data

            if (current == null) {
                // ✅ CREATE
                val result = workerRepository.createWorkerProfile(workerWithOwner)

                _saveState.value = when (result) {
                    is Resource.Success -> Resource.Success(Unit)
                    is Resource.Error -> Resource.Error(result.message)
                    Resource.Loading -> Resource.Loading
                }

            } else {
                // ✅ UPDATE (🔥 FIXED: ownerId included)
                val fields = mapOf<String, Any>(
                    "nameEn" to workerWithOwner.nameEn,
                    "nameKn" to workerWithOwner.nameKn,

                    "skillEn" to workerWithOwner.skillEn,
                    "skillKn" to workerWithOwner.skillKn,

                    "areaEn" to workerWithOwner.areaEn,
                    "areaKn" to workerWithOwner.areaKn,

                    "rate" to workerWithOwner.rate,
                    "phone" to workerWithOwner.phone,

                    "isAvailable" to workerWithOwner.isAvailable,

                    // 🔥 IMPORTANT FIX
                    "ownerId" to workerWithOwner.ownerId
                )

                _saveState.value =
                    workerRepository.updateWorkerProfile(current.id, fields)
            }

            loadMyProfile()
        }
    }

    fun toggleAvailability() {

        viewModelScope.launch {

            val current = (_workerProfile.value as? Resource.Success)?.data
                ?: return@launch

            _availState.value = Resource.Loading

            _availState.value =
                workerRepository.updateAvailability(
                    current.id,
                    !current.isAvailable
                )

            loadMyProfile()
        }
    }

    fun signOut() = authRepository.signOut()
}