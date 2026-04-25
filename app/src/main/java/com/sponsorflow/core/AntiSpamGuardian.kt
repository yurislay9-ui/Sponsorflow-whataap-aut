package com.sponsorflow.core

import javax.inject.Inject
import javax.inject.Singleton

import android.util.Log

/**
 * SISTEMA DE DEFENSA TÁCTICO (Anti-Ban y Anti-Bucles).
 * Evita que el bot responda infinitamente si interactúa con otro bot,
 * o si un humano hace spam, protegiendo la cuenta de WhatsApp/IG de ser baneada.
 */
@Singleton
class AntiSpamGuardian @Inject constructor(
    private val hybridIntentEngine: HybridIntentEngine
) {
    private const val TAG = "NEXUS_AntiSpam"
    
    // Memoria volátil de interacciones temporales: Mapea SenderID -> Lista de Timestamps
    private val interactionHistory = mutableMapOf<String, MutableList<Long>>()
    
    // Reglas paramétricas del Escudo
    private const val COOLDOWN_MS = 2500L // 2.5 segundos mínimo entre mensajes para simular ser humano
    private const val MAX_MSGS_PER_MINUTE = 6 // Máximo 6 interacciones por minuto
    private const val PENALTY_BOX_MS = 60000L * 5 // 5 minutos de castigo (Caja de arena) si hace spam

    private val penaltyBox = mutableMapOf<String, Long>()

    private var lastFullPruneTime = 0L

    @Synchronized
    fun isSafeToReply(senderId: String): Boolean {
        val now = System.currentTimeMillis()

        // 0. Autolimpieza pasiva profunda (Barrendero GARBAGE COLLECTOR Inline)
        // Cada 2 horas purgamos TODOS los diccionarios para evitar fugas de RAM masivas.
        if (now - lastFullPruneTime > 7200000L) {
            val historySizeBefore = interactionHistory.size
            penaltyBox.entries.removeIf { it.value < now }
            interactionHistory.entries.removeIf { (_, history) ->
                history.removeAll { now - it > 60000L }
                history.isEmpty()
            }
            lastFullPruneTime = now
            Log.i(TAG, "🧹 AntiSpam GC Ejecutado: Memoria purgada. (Limpiados \${historySizeBefore - interactionHistory.size} clientes fantasmas)")
        }

        // 1. Verificar si el objetivo está en la lista negra temporal
        if (penaltyBox.containsKey(senderId)) {
            val penaltyEnd = penaltyBox[senderId] ?: 0L
            if (now < penaltyEnd) {
                Log.w(TAG, "🚫 Bloqueando a \$senderId: Está en la Penalty Box (Spam o Loop bot detectado).")
                return false
            } else {
                penaltyBox.remove(senderId) // Cumplió condena
            }
        }

        // 2. Recuperar historial balístico del remitente
        val history = interactionHistory.getOrPut(senderId) { mutableListOf() }
        
        // Purgar historial viejo (mayor a 1 minuto)
        history.removeAll { now - it > 60000L }

        // 3. Regla de Contención 1: Anti-Metralleta (Cooldown estricto)
        if (history.isNotEmpty() && (now - history.last() < COOLDOWN_MS)) {
            Log.w(TAG, "⚠️ Cooldown Térmico Activo para \$senderId. Mensaje ahogado para simular latencia humana y evitar detección de Bot.")
            return false
        }

        // 4. Regla de Contención 2: Anti-Spam Machine / Bot vs Bot
        if (history.size >= MAX_MSGS_PER_MINUTE) {
            Log.e(TAG, "🚨 SPAM CRÍTICO DETECTADO de \$senderId. Aislándolo a la Penalty Box por 5 minutos.")
            penaltyBox[senderId] = now + PENALTY_BOX_MS
            return false
        }

        // 5. Interacción Lícita -> Registrar en log y permitir disparo
        history.add(now)
        return true
    }
    
    fun clearMemory() {
        interactionHistory.clear()
        penaltyBox.clear()
        hybridIntentEngine.purgeMemory()
        Log.i(TAG, "🧹 AntiSpam GC y Motor Híbrido purgados manualmente (Trigger Externo OS).")
    }
}
