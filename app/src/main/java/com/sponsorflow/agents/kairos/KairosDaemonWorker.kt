package com.sponsorflow.agents.kairos

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sponsorflow.models.AgentTask
import com.sponsorflow.models.InboundMessage
import com.sponsorflow.models.AgentResult

/**
 * KAIROS DAEMON WORKER (Integración Android SO)
 * 
 * Este es el anclaje seguro de Android (WorkManager) para ejecutar el Escuadrón KAIROS.
 * Se programa para ejecutarse únicamente cuando el dispositivo está en IDLE (inactivo)
 * y conectado al cargador, salvaguardando el 100% de la batería durante el uso diurno.
 */
class KairosDaemonWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.i("NEXUS_KairosDaemon", "🔋 Condiciones del sistema óptimas (Cargando & Idle). Despertando Escuadrón KAIROS...")
        
        return try {
            // Creamos un "Dummy Message" para cumplir el contrato de ejecución del Agente
            // La "mochila" se usará únicamente para pasar el Context.
            val dummyMessage = InboundMessage(
                context = context,
                sender = "SYSTEM_DAEMON",
                text = "AUTO_DREAM_TRIGGER",
                replyIntent = null,
                remoteInputKey = null
            )
            val taskPayload = AgentTask(message = dummyMessage)
            
            // Ejecutamos el agente de mantenimiento (AutoDream)
            // Llama a executeTask que ahora internamente tiene Try Catch Seguro.
            val memoryAgent = com.sponsorflow.agents.kairos.MemoryAgent()
            val result = memoryAgent.executeTask(taskPayload)
            
            if (result is AgentResult.Success) {
                Log.i("NEXUS_KairosDaemon", "✅ Mantenimiento Nocturno KAIROS Completado Exitosamente.")
                Result.success()
            } else {
                Log.w("NEXUS_KairosDaemon", "⚠️ Mantenimiento KAIROS completado con Warnings.")
                Result.retry() // Backoff automático nativo de Android
            }

        } catch (e: Exception) {
            Log.e("NEXUS_KairosDaemon", "💥 Error catastrófico en el Daemon. Se reintentará después.", e)
            Result.failure()
        }
    }
}
