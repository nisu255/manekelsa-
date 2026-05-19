package com.manekelsa.app.data.repository

import com.manekelsa.app.data.firebase.FirebaseService
import com.manekelsa.app.data.model.Worker
import com.manekelsa.app.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

class WorkerRepository(private val firebaseService: FirebaseService) {

    // ✅ ALL WORKERS
    fun observeAllWorkers(): Flow<Resource<List<Worker>>> = flow {
        emit(Resource.Loading)

        try {
            firebaseService.observeWorkers().collect { workers ->
                emit(
                    Resource.Success(
                        workers.sortedBy { it.distance }
                    )
                )
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load workers"))
        }
    }

    // ✅ FILTER BY SKILL
    fun observeWorkersBySkill(skill: String): Flow<Resource<List<Worker>>> = flow {
        emit(Resource.Loading)

        try {
            firebaseService.observeWorkersBySkill(skill).collect { workers ->
                emit(
                    Resource.Success(
                        workers.sortedBy { it.distance }
                    )
                )
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load workers"))
        }
    }

    // ✅ AVAILABLE WORKERS
    fun observeAvailableWorkers(): Flow<Resource<List<Worker>>> = flow {
        emit(Resource.Loading)

        try {
            firebaseService.observeAvailableWorkers().collect { workers ->
                emit(
                    Resource.Success(
                        workers.sortedBy { it.distance }
                    )
                )
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load available workers"))
        }
    }

    // ✅ UPDATE AVAILABILITY
    suspend fun updateAvailability(workerId: String, available: Boolean): Resource<Unit> =
        withContext(Dispatchers.IO) {
            val result = firebaseService.updateAvailability(workerId, available)
            if (result.isSuccess) Resource.Success(Unit)
            else Resource.Error(result.exceptionOrNull()?.message ?: "Update failed")
        }

    // ✅ THUMBS UP
    suspend fun thumbsUpWorker(workerId: String): Resource<Unit> =
        withContext(Dispatchers.IO) {
            val result = firebaseService.thumbsUpWorker(workerId)
            if (result.isSuccess) Resource.Success(Unit)
            else Resource.Error(result.exceptionOrNull()?.message ?: "Rating failed")
        }

    // ✅ CREATE PROFILE
    suspend fun createWorkerProfile(worker: Worker): Resource<String> =
        withContext(Dispatchers.IO) {
            val result = firebaseService.createWorkerProfile(worker)
            if (result.isSuccess) Resource.Success(result.getOrThrow())
            else Resource.Error(result.exceptionOrNull()?.message ?: "Create failed")
        }

    // ✅ GET PROFILE
    suspend fun getWorkerByOwnerId(ownerId: String): Resource<Worker?> =
        withContext(Dispatchers.IO) {
            val result = firebaseService.getWorkerByOwnerId(ownerId)
            if (result.isSuccess) Resource.Success(result.getOrNull())
            else Resource.Error(result.exceptionOrNull()?.message ?: "Fetch failed")
        }

    // ✅ UPDATE PROFILE
    suspend fun updateWorkerProfile(workerId: String, fields: Map<String, Any>): Resource<Unit> =
        withContext(Dispatchers.IO) {
            val result = firebaseService.updateWorkerProfile(workerId, fields)
            if (result.isSuccess) Resource.Success(Unit)
            else Resource.Error(result.exceptionOrNull()?.message ?: "Update failed")
        }

    // ✅ SAMPLE DATA
    suspend fun seedSampleWorkers() = withContext(Dispatchers.IO) {
        firebaseService.seedSampleWorkers()
    }
}