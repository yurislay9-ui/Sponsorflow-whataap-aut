package com.sponsorflow.services

import android.app.PendingIntent
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * ENTIDAD DE LA COLA: Representa una tarea de respuesta pendiente.
 */
data class PendingReply(
    val sender: String,
    val textToSend: String,
    val intent: PendingIntent,
    val enqueueTime: Long = System.currentTimeMillis()
)

/**
 * SISTEMA DE ENCOLAMIENTO SEGURO (Message Queuing & Throttling)
 * Protege la infraestructura local y la cuenta del usuario regulando el flujo de salida.
 * Actúa como válvula de presión cuando llegan "Tormentas de Mensajes" (ej. 50 juntos).
 */
object MessageQueueManager {
    private const val TAG = "NEXUS_Queue"
    
    // Coroutine Scope que vive durante todo el ciclo de la app
    private val queueScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Lista física (Buffer) de las respuestas que Llama.cpp ya pensó pero que aún no se envían
    private val pendingQueue = mutableListOf<PendingReply>()
    private val mutex = Mutex() // Evita colisiones si Llama intenta meter 5 respuestas a la vez
    
    // Límite de velocidad matemático (Throttle): 1 mensaje por cada X milisegundos máximo
    private const val SAFE_DELAY_BETWEEN_MESSAGES_MS = 6000L // 6 segundos de separación física obligatoria

    init {
        // En cuanto arranca el objeto, iniciamos el "Trabajador" (Worker) infinito
        startQueueWorker()
    }

    /**
     * El Orquestador llama a este método cuando la IA ya tiene la respuesta.
     * NO la enviamos de golpe. La "formamos" en la cola.
     */
    fun enqueueReply(sender: String, textToSend: String, intent: PendingIntent) {
        queueScope.launch {
            mutex.withLock {
                pendingQueue.add(PendingReply(sender, textToSend, intent))
                Log.i(TAG, "📥 Mensaje encolado para [$sender]. Tamaño actual de la cola: ${pendingQueue.size}")
            }
        }
    }

    /**
     * El Trabajador de Fondo.
     * Revisa la cola constantemente. Si hay mensajes, envía uno y "descansa" por un
     * periodo seguro (Throttle) antes de enviar el siguiente.
     */
    private fun startQueueWorker() {
        queueScope.launch {
            while (isActive) {
                var replyTask: PendingReply? = null
                
                // Extraer el mensaje más viejo (FIFO: First In, First Out)
                mutex.withLock {
                    if (pendingQueue.isNotEmpty()) {
                        // Toma el primero
                        replyTask = pendingQueue.removeAt(0) 
                    }
                }
                
                // Si encontramos un trabajo, lo procesamos
                if (replyTask != null) {
                    processTask(replyTask!!)
                    // == THROTTLING (El Corazón Antispam) ==
                    // Espera forzosa para no saturar los Intent de Android ni a Meta
                    delay(SAFE_DELAY_BETWEEN_MESSAGES_MS) 
                } else {
                    // Si no hay nada en la cola, el trabajador duerme 1 segundo antes de volver a asomarse
                    delay(1000L)
                }
            }
        }
    }

    private suspend fun processTask(task: PendingReply) {
        try {
            Log.d(TAG, "🚀 Ejecutando respuesta de la cola para [${task.sender}].")
            
            // Aquí es donde en el futuro AccessibilityMimic agarrará textToSend.
            // Por ahora asuminos que el disparo del Intent notifica a AccessibilityService.
            com.sponsorflow.core.Orchestrator.pendingAutoReply = task.textToSend
            
            task.intent.send()
            Log.d(TAG, "✅ Disparo de Intent completado.")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al disparar respuesta encolada: ${e.message}")
        }
    }
}
