package com.manekelsa.app

import android.content.Context

object LanguageManager {

    private const val PREF_NAME = "app_prefs"
    private const val KEY_LANG = "language"

    fun setLanguage(context: Context, isKannada: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_LANG, isKannada).apply()
    }

    fun isKannada(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_LANG, true) // default Kannada
    }
}