package com.example.network

import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object OnDeviceTranslator {
    private const val TAG = "OnDeviceTranslator"

    private val options = TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.ARABIC)
        .setTargetLanguage(TranslateLanguage.URDU)
        .build()

    private val translator = Translation.getClient(options)

    /**
     * Translates Arabic text to Urdu on-device.
     * Returns null if the model is not yet downloaded, enabling instant fallback
     * to Gemini, while initiating background model download so subsequent attempts are local and instant.
     */
    suspend fun translateArabicToUrdu(text: String): String? = suspendCancellableCoroutine { continuation ->
        translator.translate(text)
            .addOnSuccessListener { result ->
                if (continuation.isActive) {
                    continuation.resume(result)
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "ML Kit Translate failed, likely due to offline model not downloaded: ${exception.message}")
                
                // Initiate download of models in the background so future translations are offline & near-instant
                val conditions = DownloadConditions.Builder().build()
                translator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener {
                        Log.d(TAG, "ML Kit Translate model downloaded successfully.")
                    }
                    .addOnFailureListener { downloadError ->
                        Log.e(TAG, "Failed to download ML Kit Translate model: ${downloadError.message}")
                    }
                
                if (continuation.isActive) {
                    continuation.resume(null) // return null for fallback
                }
            }
    }
}
