package com.manekelsa.app

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class ManeKelsaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        enableFirestoreOfflinePersistence()
    }

    private fun enableFirestoreOfflinePersistence() {
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = settings
    }
}
