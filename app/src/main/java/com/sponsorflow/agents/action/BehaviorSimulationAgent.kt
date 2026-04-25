package com.sponsorflow.agents.action

import javax.inject.Inject

import android.util.Log
import com.sponsorflow.agents.TypedSponsorflowAgent
import com.sponsorflow.models.AgentPayload
import com.sponsorflow.models.AgentResponseData
import com.sponsorflow.models.AgentTask
import com.sponsorflow.models.SquadType
import com.sponsorflow.models.SwarmError
import com.sponsorflow.models.SwarmResult
import com.sponsorflow.models.SwarmTask
import kotlinx.coroutines.delay
import kotlin.random.Random
import java.util.Calendar
data class BehaviorPayload(
    val inputLength: Int,
    val isFirstMessageOfDay: Boolean,
    val isNightOwl: Boolean
) : AgentPayload

data class BehaviorData(
    val appliedDelayMs: Long
) : AgentResponseData

/**
 * [ACTUALIZACIÓN 2026 - MIGRADO A TIPADO ESTRICTO E INYECCIÓN]
 * BEHAVIOR SIMULATION AGENT (Capa Stealth / Anti-Ban)
 * 
 * Reemplaza el StealthEngine estático con un modelo adaptativo y dinámico.
 * Simula de forma imperceptible los patrones erráticos humanos (delays matemáticos,
 * pausas lógicas, backoffs dinámicos) y orquesta la desvinculación a nivel de sistema.
 */

class BehaviorSimulationAgent @Inject constructor() : TypedSponsorflowAgent<BehaviorPayload, BehaviorData>() {
    private const val TAG = "NEXUS_BehaviorAgent"

    override val agentName: String = "BehaviorSimulationAgent"
    override val squadron: SquadType = SquadType.ACTION
    override val capabilities: List<String> = listOf("human_delay", "erratic_simulation", "adaptive_backoff")

    override fun mapLegacyTaskToPayload(task: AgentTask): BehaviorPayload {
        return BehaviorPayload(
            inputLength = task.message.text.length,
            isFirstMessageOfDay = task.metadata?.get("is_first_message") as? Boolean ?: false,
            isNightOwl = task.metadata?.get("user_pref_night_owl") as? Boolean ?: false
        )
    }

    override suspend fun executeTypedInternal(task: SwarmTask<BehaviorPayload>): SwarmResult<BehaviorData, SwarmError> {
        val inputLength = task.payload.inputLength
        val isFirstMessageOfDay = task.payload.isFirstMessageOfDay
        val isNightOwl = task.payload.isNightOwl

        try {
            val generatedDelay = calculateHumanizedDelay(inputLength, isFirstMessageOfDay, isNightOwl)
            
            Log.i(TAG, "🕵️ Simulación Orgánica: Aplicando pausa biomecánica de \${generatedDelay}ms antes de disparar al SO.")
            // Realizamos la pausa asíncrona (suspende la corrutina pero NO bloquea el Main Thread de Android
            // El Swarm Manager seguirá haciendo otras cosas mientras este hilo 'finge leer')
            delay(generatedDelay)

            return SwarmResult.Success(
                confidenceScore = 1.0,
                data = BehaviorData(appliedDelayMs = generatedDelay)
            )

        } catch (e: Exception) {
            Log.e(TAG, "🚨 Error en simulación de retraso humano: \${e.message}")
            return SwarmResult.Failure(SwarmError.InternalException("Fallo simulando pausas biológicas: \${e.message}"))
        }
    }

    /**
     * Calcula qué tanto tardaría un humano hipotético en leer el texto y sacar el celular.
     */
    private fun calculateHumanizedDelay(textLength: Int, isFirstMessageOfDay: Boolean, isNightOwl: Boolean = false): Long {
        val baseReactionTime = Random.nextLong(2500, 4500) // Segundos bases para ver la pantalla
        
        // 60 - 120 ms por carácter leído
        val readingTime = textLength * Random.nextLong(60, 120) 

        // Si es el primer mensaje de la mañana/tarde, el humano tarda más en encontrar el chat (cold start)
        var coldStartPenalty = if (isFirstMessageOfDay) Random.nextLong(5000, 12000) else 0L
        
        // Si el cliente es búho nocturno, aplicamos lentitud extra por sueño
        if (isNightOwl) {
            coldStartPenalty += Random.nextLong(2000, 4500)
        }

        // Random jitter
        val jitter = Random.nextLong(-800, 800)

        // Limita a un máximo de 14 segundos para no incurrir en Timeouts del DAG.
        val totalDelay = (baseReactionTime + readingTime + coldStartPenalty + jitter).coerceAtMost(14000L)
        return totalDelay
    }

    /**
     * ¿Deberíamos estar dormidos lógicamente? (Evita la inmovilidad del OOM Killer)
     */
    fun isLogicalSleepHour(): Boolean {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        // Dormir de 2:00 AM a 6:59 AM (Comportamiento Humano Estándar)
        return currentHour in 2..6
    }
}
