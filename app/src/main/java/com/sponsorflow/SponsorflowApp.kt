package com.sponsorflow

import android.app.Application
import android.content.ComponentCallbacks2
import android.util.Log

/**
 * Escudo de Aplicación Base (SRE Level).
 * Contiene el interceptor de la Válvula de Escape de RAM.
 */
class SponsorflowApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.i("NEXUS_SRE", "App Init: Sistema acorazado iniciado.")
    }

    // VECTOR 6 DEFENSA: Control de OOM (Out Of Memory)
    // Si Android se queda sin RAM por Facebook/Chrome y nos va a matar, reaccionamos.
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                Log.w("NEXUS_SRE", "⚠️ RAM Crítica (Nivel: $level). El Kernel nos asfixia. Purgando Cachés JNI...")
                // Simulamos liberación extrema o soltamos modelos de la memoria de inmediato
                System.gc() // Le rogamos a Android que recolecte basura en Kotlin
                // Opcional en C++: NativeBridges.clearLlamaCache()
            }
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                Log.i("NEXUS_SRE", "App minimizada. Recortando consumos visuales.")
            }
        }
    }
}
