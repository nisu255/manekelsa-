package com.manekelsa.app.util

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.*
import kotlinx.coroutines.tasks.await

object TranslatorUtil {

    private val options = TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.ENGLISH)
        .setTargetLanguage(TranslateLanguage.KANNADA)
        .build()

    private val translator = Translation.getClient(options)

    suspend fun translate(text: String): String {
        return try {
            val conditions = DownloadConditions.Builder()
                .requireWifi()
                .build()

            translator.downloadModelIfNeeded(conditions).await()
            translator.translate(text).await()

        } catch (e: Exception) {
            text // fallback
        }
    }
}