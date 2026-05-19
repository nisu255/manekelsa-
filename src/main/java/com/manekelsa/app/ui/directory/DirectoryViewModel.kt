package com.manekelsa.app.ui.directory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manekelsa.app.data.model.Worker
import com.manekelsa.app.data.repository.WorkerRepository
import com.manekelsa.app.util.DistanceUtil
import com.manekelsa.app.util.Resource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DirectoryViewModel(
    private val workerRepository: WorkerRepository
) : ViewModel() {

    private val _workersState = MutableStateFlow<Resource<List<Worker>>>(Resource.Loading)
    val workersState: StateFlow<Resource<List<Worker>>> = _workersState.asStateFlow()

    private val _selectedSkillFilter = MutableStateFlow("All")
    private val _showAvailableOnly = MutableStateFlow(false)

    private val _thumbsUpState = MutableStateFlow<Resource<Unit>?>(null)
    val thumbsUpState: StateFlow<Resource<Unit>?> = _thumbsUpState.asStateFlow()

    private val _availUpdateState = MutableStateFlow<Resource<Unit>?>(null)
    val availUpdateState: StateFlow<Resource<Unit>?> = _availUpdateState.asStateFlow()

    private val _allWorkers = MutableStateFlow<List<Worker>>(emptyList())

    fun loadWorkers() {
        observeAllWorkers()
    }

    private fun observeAllWorkers() {
        viewModelScope.launch {

            workerRepository.observeAllWorkers()
                .collectLatest { result ->

                    when (result) {

                        is Resource.Loading -> {
                            _workersState.value = Resource.Loading
                        }

                        is Resource.Success -> {

                            val workers = result.data ?: emptyList()

                            val updated = workers.map { worker ->

                                // ✅ ALWAYS calculate distance (NO 0.0 condition)
                                val distance = DistanceUtil.calculateDistance(
                                    worker.latitude,
                                    worker.longitude
                                )

                                worker.copy(distance = distance)
                            }
                                .sortedBy { it.distance }

                            _allWorkers.value = updated

                            applyFilters()
                        }

                        is Resource.Error -> {
                            _workersState.value =
                                Resource.Error(result.message ?: "Failed to load workers")
                        }
                    }
                }
        }
    }

    // 🔥 Recalculate when GPS updates
    fun refreshWithNewLocation() {

        val recalculated = _allWorkers.value.map { worker ->

            val distance = DistanceUtil.calculateDistance(
                worker.latitude,
                worker.longitude
            )

            worker.copy(distance = distance)

        }.sortedBy { it.distance }

        _allWorkers.value = recalculated

        applyFilters()
    }

    // ✅ FILTER LOGIC
    private fun applyFilters() {

        val all = _allWorkers.value
        val skill = _selectedSkillFilter.value
        val onlyAvail = _showAvailableOnly.value

        val filtered = all
            .filter { w ->

                val skillMatch =
                    skill == "All" || w.skillEn.equals(skill, ignoreCase = true)

                val availMatch =
                    !onlyAvail || w.isAvailable

                skillMatch && availMatch
            }
            .sortedBy { it.distance }

        _workersState.value = Resource.Success(filtered)
    }

    fun setSkillFilter(skill: String) {
        _selectedSkillFilter.value = skill
        applyFilters()
    }

    fun setAvailableOnlyFilter(value: Boolean) {
        _showAvailableOnly.value = value
        applyFilters()
    }

    fun thumbsUpWorker(workerId: String) {
        viewModelScope.launch {
            _thumbsUpState.value = Resource.Loading
            _thumbsUpState.value = workerRepository.thumbsUpWorker(workerId)
        }
    }

    // 🔥🔥 IMPORTANT FIX (REAL-TIME UI UPDATE)
    fun updateAvailability(workerId: String, available: Boolean) {
        viewModelScope.launch {

            _availUpdateState.value = Resource.Loading

            val result = workerRepository.updateAvailability(workerId, available)

            _availUpdateState.value = result

            // ✅ INSTANT UI UPDATE (NO RELOAD NEEDED)
            if (result is Resource.Success) {

                _allWorkers.value = _allWorkers.value.map {
                    if (it.id == workerId) it.copy(isAvailable = available)
                    else it
                }

                applyFilters()
            }
        }
    }

    fun seedData() {
        viewModelScope.launch {
            workerRepository.seedSampleWorkers()
        }
    }
}