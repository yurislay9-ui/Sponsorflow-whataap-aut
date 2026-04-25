package com.sponsorflow.agents.action

import javax.inject.Inject

import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.sponsorflow.agents.TypedSponsorflowAgent
import com.sponsorflow.models.AgentPayload
import com.sponsorflow.models.AgentResponseData
import com.sponsorflow.models.AgentTask
import com.sponsorflow.models.SquadType
import com.sponsorflow.models.SwarmError
import com.sponsorflow.models.SwarmResult
import com.sponsorflow.models.SwarmTask
import com.sponsorflow.models.InboundMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

data class CommsPayload(
    val textToSend: String?,
    val messageContext: InboundMessage
) : AgentPayload

data class CommsData(
    val sentSuccessfully: Boolean
) : AgentResponseData

/**
 * [ACTUALIZACIÓN 2026 - MIGRADO A TIPADO ESTRICTO E INYECCIÓN]
 * COMMS AGENT (Capa 3: El Cartero)
 * 
 * Reemplaza el antiguo y pesado `MessageQueueManager`.
 * Responsable de la entrega física del texto hacia WhatsApp/Instagram.
 * Incluye un Throttler (Regulador de Peticiones) para evitar el baneo de las cuentas por SPAM.
 */

class CommsAgent @Inject constructor() : TypedSponsorflowAgent<CommsPayload, CommsData>() {
    private const val TAG = "NEXUS_CommsAgent"
    
    override val agentName: String = "CommsAgent"
    override val squadron: SquadType = SquadType.ACTION
    override val capabilities: List<String> = listOf("message_dispatch", "anti_spam_throttler")

    companion object {
        // Memoria volátil para Anti-Spam (Compartida globalmente entre peticiones).
        private val messageThrottler = ConcurrentHashMap<String, Long>()
        private const val MIN_DELAY_BETWEEN_REPLIES_MS = 5000L
    }

    override fun mapLegacyTaskToPayload(task: AgentTask): CommsPayload {
        return CommsPayload(
            textToSend = task.proposedAction?.payload,
            messageContext = task.message
        )
    }

    override suspend fun executeTypedInternal(task: SwarmTask<CommsPayload>): SwarmResult<CommsData, SwarmError> {
        val textToSend = task.payload.textToSend
        val messageContext = task.payload.messageContext
        
        if (textToSend.isNullOrBlank() || messageContext.replyIntent == null || messageContext.remoteInputKey == null) {
            Log.e(TAG, "❌ Faltan datos críticos para enviar el mensaje (Intent, Clave o Texto vacío).")
            return SwarmResult.Failure(SwarmError.InternalException("Faltan datos críticos para enviar el mensaje"))
        }

        return withContext(Dispatchers.IO) {
            val senderKey = messageContext.sender
            val lastSentMs = messageThrottler[senderKey] ?: 0L
            val nowMs = System.currentTimeMillis()

            // THROTTLER TÁCTICO
            val timeSinceLastMs = nowMs - lastSentMs
            if (timeSinceLastMs < MIN_DELAY_BETWEEN_REPLIES_MS) {
                val imposedDelay = MIN_DELAY_BETWEEN_REPLIES_MS - timeSinceLastMs
                Log.w(TAG, "⏱️ Throttler Activado. Pausando envío a [\$senderKey] por \$imposedDelay ms para evitar Ban.")
                delay(imposedDelay)
            }

            try {
                // Inyectar el texto en el Intent original que nos dio Android (NotificationListener)
                val localIntent = Intent().apply {
                    addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                }
                val bundle = Bundle().apply {
                    putCharSequence(messageContext.remoteInputKey, textToSend)
                }
                
                RemoteInput.addResultsToIntent(
                    arrayOf(RemoteInput.Builder(messageContext.remoteInputKey).build()), 
                    localIntent, 
                    bundle
                )

                // Disparo físico del mensaje ("Send" silencioso en background)
                messageContext.replyIntent.send(messageContext.context, 0, localIntent)

                // Actualizamos el reloj del Throttler
                messageThrottler[senderKey] = System.currentTimeMillis()
                
                Log.i(TAG, "📤 Mensaje Entregado Físicamente a [\$senderKey]: \$textToSend")

                SwarmResult.Success(
                    confidenceScore = 1.0,
                    data = CommsData(sentSuccessfully = true)
                )

            } catch (e: android.app.PendingIntent.CanceledException) {
                Log.e(TAG, "💥 El PendingIntent fue cancelado por el SO (la notificación ya expiró).")
                SwarmResult.Failure(SwarmError.InternalException("PendingIntent cancelado"))
            } catch (e: Exception) {
                Log.e(TAG, "💥 Error crítico desconido enviando respuesta: \${e.message}")
                SwarmResult.Failure(SwarmError.InternalException("Exception: \${e.message}"))
            }
        }
    }
}
