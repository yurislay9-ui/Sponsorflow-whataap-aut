package com.sponsorflow

import javax.inject.Inject

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Intent
import android.util.Log
import com.sponsorflow.core.ONNXSemanticEngine
import com.sponsorflow.security.SecurityVault
import com.sponsorflow.services.ImmortalSwarmService
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
/**
 * Escudo de Aplicación Base (SRE Level).
 * Contiene el interceptor de la Válvula de Escape de RAM.
 * [ACTUALIZACIÓN 2026: HiltAndroidApp inicializador del Grafo DI]
 */
@HiltAndroidApp
class SponsorflowApp : Application() {

    @Inject
    lateinit var securityVault: SecurityVault
    
    @Inject
    lateinit var onnxEngine: ONNXSemanticEngine

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        Log.i("NEXUS_SRE", "App Init: Sistema acorazado iniciado.")
        
        // Verifica la validez de la licencia al inicio
        if (!securityVault.isLicenseValid()) {
            Log.e("NEXUS_SRE", "Licencia inválida. Posible pirateo detectado.")
        }

        // Pre-warm ONNX memory models in background
        applicationScope.launch {
            Log.i("NEXUS_SRE", "Pre-warming ONNX Engine...")
            onnxEngine.preWarmSync()
        }

        // Inicialización silenciosa: Intentamos despertar el Swarm
        try {
            val serviceIntent = Intent(this, ImmortalSwarmService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e("NEXUS_SRE", "No se pudo auto-arrancar el ImmortalService por política estricta de Android 14+. Esperaremos a que MainActivity lo levante.", e)
        }
    }

    // VECTOR 6 DEFENSA: Control de OOM (Out Of Memory)
    // Si Android se queda sin RAM por Facebook/Chrome y nos va a matar, reaccionamos.
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                Log.w("NEXUS_SRE", "⚠️ RAM Crítica (Nivel: \$level). El Kernel nos asfixia. Purgando Cachés JNI...")
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
