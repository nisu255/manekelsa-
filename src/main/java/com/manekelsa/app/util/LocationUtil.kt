package com.manekelsa.app.util

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await

object LocationUtil {

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Pair<Double, Double>? {
        return try {
            val client = LocationServices.getFusedLocationProviderClient(context)

            val location = client.lastLocation.await()

            if (location != null) {
                Log.d("GPS_DEBUG", "LAT=${location.latitude}, LNG=${location.longitude}")
                Pair(location.latitude, location.longitude)
            } else {
                Log.d("GPS_DEBUG", "LOCATION NULL ❌")
                null
            }

        } catch (e: Exception) {
            Log.e("GPS_DEBUG", "ERROR: ${e.message}")
            null
        }
    }
}