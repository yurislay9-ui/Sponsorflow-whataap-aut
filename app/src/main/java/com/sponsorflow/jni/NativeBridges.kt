package com.sponsorflow.jni

import androidx.annotation.Keep

/**
 * Enlaces JNI (Java Native Interface).
 * Este es el puente que permite a Kotlin (ligero) hablar con C++ (ultrarrápido).
 */
@Keep
object NativeBridges {
    init {
        try {
            System.loadLibrary("sponsorflow_native")
        } catch (e: UnsatisfiedLinkError) {
            android.util.Log.e("NEXUS_JNI", "Falla Letal: No se encontró libsponsorflow_native.so. Revisa la ABI y las dependencias zombie de C++.")
        }
    }

    // VECTOR 16 DEFENSA JNI: Forzar la conservación de firmas nativas para C++ JNIEnv
    // Funciones nativas para Llama.cpp (Agente de Cierre)
    @Keep
    external fun initLlamaModel(modelPath: String): Boolean
    
    @Keep
    external fun generateText(prompt: String): String
    
    @Keep
    external fun releaseLlamaModel()

    // Funciones nativas para Whisper.cpp (Agente de Voz)
    @Keep
    external fun initWhisperModel(modelPath: String): Boolean
    
    @Keep
    external fun transcribeAudio(audioPath: String): String
    
    @Keep
    external fun releaseWhisperModel()
}
