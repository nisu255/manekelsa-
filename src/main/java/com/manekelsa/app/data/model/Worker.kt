package com.manekelsa.app.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Worker(

    val id: String = "",

    // ✅ MULTI-LANGUAGE NAME
    val nameEn: String = "",
    val nameKn: String = "",

    // ✅ MULTI-LANGUAGE SKILL
    val skillEn: String = "",
    val skillKn: String = "",
    val skillIcon: String = "",

    // ✅ MULTI-LANGUAGE AREA
    val areaEn: String = "",
    val areaKn: String = "",

    // ✅ GPS LOCATION
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,

    // ❌ DO NOT STORE DISTANCE IN FIREBASE
    // ✅ Only used in app
    val distance: Double = 0.0,

    val rate: String = "",
    val rating: Int = 0,
    val phone: String = "",

    // ✅ ONLY FIELD USED EVERYWHERE
    val isAvailable: Boolean = false,

    val ownerId: String = ""

) : Parcelable {

    // ✅ Required empty constructor for Firebase
    constructor() : this(
        id = "",
        nameEn = "",
        nameKn = "",
        skillEn = "",
        skillKn = "",
        skillIcon = "",
        areaEn = "",
        areaKn = "",
        latitude = 0.0,
        longitude = 0.0,
        distance = 0.0,
        rate = "",
        rating = 0,
        phone = "",
        isAvailable = false,
        ownerId = ""
    )
}