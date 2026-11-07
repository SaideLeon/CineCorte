package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.nio.charset.StandardCharsets

/**
 * Gestor seguro de credenciais e definições locais.
 * Guarda a chave da API da Groq e o consentimento do utilizador.
 */
class SecurePreferencesHelper(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getGroqApiKey(): String {
        val encoded = prefs.getString(KEY_GROQ_API_KEY, "") ?: ""
        if (encoded.isBlank()) return ""
        return try {
            String(Base64.decode(encoded, Base64.DEFAULT), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            encoded
        }
    }

    fun saveGroqApiKey(apiKey: String) {
        val trimmed = apiKey.trim()
        val encoded = Base64.encodeToString(trimmed.toByteArray(StandardCharsets.UTF_8), Base64.DEFAULT)
        prefs.edit().putString(KEY_GROQ_API_KEY, encoded).apply()
    }

    fun hasAcceptedGroqConsent(): Boolean {
        return prefs.getBoolean(KEY_GROQ_CONSENT, false)
    }

    fun setGroqConsentAccepted(accepted: Boolean) {
        prefs.edit().putBoolean(KEY_GROQ_CONSENT, accepted).apply()
    }

    fun getLanguage(): String {
        return prefs.getString(KEY_LANGUAGE, "pt") ?: "pt"
    }

    fun setLanguage(language: String) {
        prefs.edit().putString(KEY_LANGUAGE, language).apply()
    }

    companion object {
        private const val PREFS_NAME = "cinecorte_secure_prefs"
        private const val KEY_GROQ_API_KEY = "groq_api_key_obf"
        private const val KEY_GROQ_CONSENT = "groq_consent_accepted"
        private const val KEY_LANGUAGE = "groq_transcribe_language"
    }
}
