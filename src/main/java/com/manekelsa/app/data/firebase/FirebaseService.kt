package com.manekelsa.app.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.manekelsa.app.data.model.Worker
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseService {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    companion object {
        private const val WORKERS_COLLECTION = "workers"
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            val result = auth.signInAnonymously().await()
            val user = result.user ?: throw Exception("User is null")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() = auth.signOut()

    // 🔥🔥 CENTRAL MAPPER (MOST IMPORTANT)
    private fun mapDocToWorker(docId: String, data: Map<String, Any?>): Worker {

        return Worker(
            id = docId,

            nameEn = data["nameEn"] as? String ?: "",
            nameKn = data["nameKn"] as? String ?: "",

            skillEn = data["skillEn"] as? String ?: "",
            skillKn = data["skillKn"] as? String ?: "",
            skillIcon = data["skillIcon"] as? String ?: "",

            areaEn = data["areaEn"] as? String ?: "",
            areaKn = data["areaKn"] as? String ?: "",

            latitude = (data["latitude"] as? Number)?.toDouble() ?: 0.0,
            longitude = (data["longitude"] as? Number)?.toDouble() ?: 0.0,

            rate = data["rate"] as? String ?: "",
            rating = (data["rating"] as? Number)?.toInt() ?: 0,
            phone = data["phone"] as? String ?: "",

            // 🔥 CRITICAL FIX (handles BOTH fields)
            isAvailable = when {
                data["isAvailable"] != null -> data["isAvailable"] as Boolean
                data["available"] != null -> data["available"] as Boolean
                else -> false
            },

            ownerId = data["ownerId"] as? String ?: "",

            // ❌ NEVER READ FROM FIREBASE
            distance = 0.0
        )
    }

    // ✅ ALL WORKERS
    fun observeWorkers(): Flow<List<Worker>> = callbackFlow {

        val listener: ListenerRegistration = firestore
            .collection(WORKERS_COLLECTION)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val workers = snapshot?.documents?.map { doc ->
                    mapDocToWorker(doc.id, doc.data ?: emptyMap())
                } ?: emptyList()

                trySend(workers)
            }

        awaitClose { listener.remove() }
    }

    // ✅ FILTER BY SKILL
    fun observeWorkersBySkill(skill: String): Flow<List<Worker>> = callbackFlow {

        val listener = firestore
            .collection(WORKERS_COLLECTION)
            .whereEqualTo("skillEn", skill)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val workers = snapshot?.documents?.map { doc ->
                    mapDocToWorker(doc.id, doc.data ?: emptyMap())
                } ?: emptyList()

                trySend(workers)
            }

        awaitClose { listener.remove() }
    }

    // ✅ AVAILABLE ONLY
    fun observeAvailableWorkers(): Flow<List<Worker>> = callbackFlow {

        val listener = firestore
            .collection(WORKERS_COLLECTION)
            .whereEqualTo("isAvailable", true)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val workers = snapshot?.documents?.map { doc ->
                    mapDocToWorker(doc.id, doc.data ?: emptyMap())
                } ?: emptyList()

                trySend(workers)
            }

        awaitClose { listener.remove() }
    }

    // ✅ UPDATE AVAILABILITY
    suspend fun updateAvailability(workerId: String, available: Boolean): Result<Unit> {
        return try {
            firestore.collection(WORKERS_COLLECTION)
                .document(workerId)
                .update("isAvailable", available)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ✅ THUMBS UP
    suspend fun thumbsUpWorker(workerId: String): Result<Unit> {
        return try {

            val ref = firestore.collection(WORKERS_COLLECTION).document(workerId)

            firestore.runTransaction { transaction ->
                val snap = transaction.get(ref)
                val current = snap.getLong("rating") ?: 0L
                transaction.update(ref, "rating", current + 1)
            }.await()

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ✅ CREATE PROFILE
    suspend fun createWorkerProfile(worker: Worker): Result<String> {
        return try {
            val ref = firestore.collection(WORKERS_COLLECTION)
                .add(worker)
                .await()

            Result.success(ref.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ✅ GET USER PROFILE
    suspend fun getWorkerByOwnerId(ownerId: String): Result<Worker?> {
        return try {

            val snapshot = firestore.collection(WORKERS_COLLECTION)
                .whereEqualTo("ownerId", ownerId)
                .limit(1)
                .get()
                .await()

            val worker = snapshot.documents.firstOrNull()?.let { doc ->
                mapDocToWorker(doc.id, doc.data ?: emptyMap())
            }

            Result.success(worker)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ✅ UPDATE PROFILE
    suspend fun updateWorkerProfile(
        workerId: String,
        fields: Map<String, Any>
    ): Result<Unit> {
        return try {

            firestore.collection(WORKERS_COLLECTION)
                .document(workerId)
                .update(fields)
                .await()

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ✅ SEED DATA
    suspend fun seedSampleWorkers() {

        val existing = firestore.collection(WORKERS_COLLECTION)
            .limit(1)
            .get()
            .await()

        if (!existing.isEmpty) return

        val samples = listOf(

            Worker(
                nameEn = "Savitha R",
                nameKn = "ಸವಿತಾ ಆರ್",
                skillEn = "Cleaning",
                skillKn = "ಸ್ವಚ್ಛತೆ",
                skillIcon = "🧹",
                areaEn = "Rajajinagar",
                areaKn = "ರಾಜಾಜಿನಗರ",
                latitude = 12.9916,
                longitude = 77.5546,
                rate = "400",
                rating = 24,
                phone = "9876543210",
                isAvailable = true
            ),

            Worker(
                nameEn = "Raju M",
                nameKn = "ರಾಜು ಎಂ",
                skillEn = "Gardening",
                skillKn = "ತೋಟಗಾರಿಕೆ",
                skillIcon = "🌿",
                areaEn = "Vijayanagar",
                areaKn = "ವಿಜಯನಗರ",
                latitude = 12.9716,
                longitude = 77.5246,
                rate = "350",
                rating = 18,
                phone = "9765432109",
                isAvailable = true
            ),

            Worker(
                nameEn = "Meena K",
                nameKn = "ಮೀನಾ ಕೆ",
                skillEn = "Cooking",
                skillKn = "ಅಡುಗೆ",
                skillIcon = "🍳",
                areaEn = "Malleshwaram",
                areaKn = "ಮಲ್ಲೇಶ್ವರಂ",
                latitude = 12.9816,
                longitude = 77.5746,
                rate = "500",
                rating = 31,
                phone = "9654321098",
                isAvailable = false
            )
        )

        samples.forEach {
            firestore.collection(WORKERS_COLLECTION)
                .add(it)
                .await()
        }
    }
}