package com.manekelsa.app.ui.myjobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manekelsa.app.data.model.Worker
import com.manekelsa.app.data.repository.AuthRepository
import com.manekelsa.app.data.repository.WorkerRepository
import com.manekelsa.app.util.DistanceUtil
import com.manekelsa.app.util.Resource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MyJobsViewModel(
    private val workerRepository: WorkerRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // ✅ CORRECT STATE (MY JOBS)
    private val _myJobs =
        MutableStateFlow<Resource<List<Worker>>>(Resource.Loading)

    val myJobs: StateFlow<Resource<List<Worker>>> =
        _myJobs.asStateFlow()

    init {
        observeMyJobs()
    }

    // ✅ CORE FIX: FILTER BY OWNER ID (NOT AVAILABLE)
    private fun observeMyJobs() {
        viewModelScope.launch {

            val currentUserId =
                authRepository.getCurrentUser()?.uid ?: return@launch

            workerRepository.observeAllWorkers()
                .collectLatest { result ->

                    when (result) {

                        is Resource.Loading -> {
                            _myJobs.value = Resource.Loading
                        }

                        is Resource.Success -> {

                            val workers = result.data ?: emptyList()

                            val myWorkers = workers
                                .filter { it.ownerId == currentUserId } // 🔥 MAIN FIX
                                .map { worker ->

                                    val distance = DistanceUtil.calculateDistance(
                                        worker.latitude,
                                        worker.longitude
                                    )

                                    worker.copy(distance = distance)
                                }

                            _myJobs.value = Resource.Success(myWorkers)
                        }

                        is Resource.Error -> {
                            _myJobs.value =
                                Resource.Error(result.message ?: "Error")
                        }
                    }
                }
        }
    }

    // ✅ 👍 BUTTON FIX
    fun thumbsUpWorker(workerId: String) {
        viewModelScope.launch {
            workerRepository.thumbsUpWorker(workerId)
        }
    }
}