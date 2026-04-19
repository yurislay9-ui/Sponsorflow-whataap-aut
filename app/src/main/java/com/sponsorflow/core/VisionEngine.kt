package com.sponsorflow.core

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/**
 * MOTOR DE VISIÓN (Agente Ocular)
 * Lee pantallazos o fotos que el cliente envía para ver qué producto quiere.
 * Extrae el texto offline en milisegundos sin consumir RAM excesiva.
 */
object VisionEngine {
    private const val TAG = "NEXUS_Vision"
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractTextFromImage(context: Context, imageUri: Uri): String {
        return try {
            Log.i(TAG, "Abriendo imagen y escaneando píxeles...")
            val image = InputImage.fromFilePath(context, imageUri)
            // Usamos await() gracias a kotlinx-coroutines-play-services
            val result = recognizer.process(image).await()
            val extracted = result.text.replace("\n", " ")
            
            Log.i(TAG, "Texto extraído con éxito: [$extracted]")
            extracted
        } catch (e: Exception) {
            Log.e(TAG, "Fallo al leer imagen: \${e.message}")
            ""
        }
    }
}
