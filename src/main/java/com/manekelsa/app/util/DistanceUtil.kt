package com.manekelsa.app.util

import android.location.Location

object DistanceUtil {

    var CURRENT_USER_LAT = 0.0
    var CURRENT_USER_LNG = 0.0

    fun calculateDistance(
        workerLat: Double,
        workerLng: Double
    ): Double {

        // ❌ REMOVE strict 0.0 blocking
        if (workerLat == 0.0 || workerLng == 0.0) return Double.MAX_VALUE

        val results = FloatArray(1)

        Location.distanceBetween(
            CURRENT_USER_LAT,
            CURRENT_USER_LNG,
            workerLat,
            workerLng,
            results
        )

        return results[0] / 1000.0
    }
}