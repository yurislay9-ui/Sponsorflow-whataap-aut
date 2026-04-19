package com.sponsorflow.core

import android.util.Log
import com.sponsorflow.jni.NativeBridges
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * MOTOR DE VOZ (Agente Auditivo - Whisper.cpp)
 * Transcribe los audios de los clientes usando la Regla "Load & Drop".
 */
object VoiceEngine {
    private const val TAG = "NEXUS_Voice"
    private const val whisperModelPath = "/data/user/0/com.sponsorflow/files/models/whisper_tiny.bin"

    suspend fun transcribeAudio(audioFilePath: String): String = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "==> 1. CARGANDO WHISPER EN RAM (Load)...")
            val isLoaded = NativeBridges.initWhisperModel(whisperModelPath)
            
            if (!isLoaded) {
                Log.w(TAG, "Modelo Whisper no encontrado físicamente.")
                return@withContext "[Audio no entendible por error técnico]"
            }

            Log.i(TAG, "==> 2. ESCUCHANDO Y TRADUCIENDO (Inferencia)...")
            val transcript = NativeBridges.transcribeAudio(audioFilePath)
            Log.i(TAG, "Transcripción: \$transcript")
            
            return@withContext transcript

        } catch (e: Exception) {
            Log.e(TAG, "Fallo al procesar audio: \${e.message}")
            return@withContext "[Audio incomprensible]"
        } finally {
            Log.i(TAG, "==> 3. LIBERANDO WHISPER DE MEMORIA (Drop)...")
            NativeBridges.releaseWhisperModel()
        }
    }
}
