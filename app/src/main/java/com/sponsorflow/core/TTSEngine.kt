package com.sponsorflow.core

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * MOTOR DE HABLA (Agente Fonético - TTS)
 * Convierte las respuestas de la IA en notas de voz.
 * Atiende a tu petición: "alguien que lea audios y HAGA AUDIOS".
 */
class TTSEngine(private val context: Context) : TextToSpeech.OnInitListener {
    private const val TAG = "NEXUS_TTS"
    private var tts: TextToSpeech? = null
    private var isInitialized = CompletableDeferred<Boolean>()

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("es", "ES")) // O "es", "MX"
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Idioma Español no soportado o falta descargar.")
                isInitialized.complete(false)
            } else {
                Log.i(TAG, "Motor TTS inicializado y listo para crear Audios.")
                isInitialized.complete(true)
            }
        } else {
            Log.e(TAG, "Fallo al inicializar TextToSpeech de Android.")
            isInitialized.complete(false)
        }
    }

    /**
     * Sintetiza el texto y lo exporta a un archivo de Audio físico (.wav o .mp3)
     * para que pueda ser enviado por WhatsApp.
     */
    suspend fun createVoiceNote(text: String, outputPath: String): Boolean = withContext(Dispatchers.IO) {
        val ready = isInitialized.await()
        if (!ready) return@withContext false

        try {
            val file = java.io.File(outputPath)
            // Sintetizar a archivo en lugar de hablarlo por la bocina
            val result = tts?.synthesizeToFile(text, null, file, "sponsorflow_tts_id")
            
            if (result == TextToSpeech.SUCCESS) {
                Log.i(TAG, "Audio creado correctamente en: \$outputPath")
                // Truco de delay para esperar a que Android termine de escribir el archivo
                kotlinx.coroutines.delay(1000) 
                return@withContext true
            } else {
                Log.e(TAG, "Fallo al sintetizar el archivo de audio.")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en creación de audio: \${e.message}")
            return@withContext false
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
