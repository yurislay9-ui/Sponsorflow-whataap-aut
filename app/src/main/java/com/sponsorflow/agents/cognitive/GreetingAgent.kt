package com.sponsorflow.agents.cognitive

import javax.inject.Inject

import com.sponsorflow.agents.TypedSponsorflowAgent
import com.sponsorflow.models.AgentPayload
import com.sponsorflow.models.AgentResponseData
import com.sponsorflow.models.AgentTask
import com.sponsorflow.models.SquadType
import com.sponsorflow.models.SwarmError
import com.sponsorflow.models.SwarmResult
import com.sponsorflow.models.SwarmTask

data class GreetingPayload(val unused: Unit = Unit) : AgentPayload

data class GreetingData(val rawTemplates: List<String>) : AgentResponseData

/**
 * [ACTUALIZACIÓN 2026 - MIGRADO A TIPADO ESTRICTO E INYECCIÓN]
 * GREETING AGENT (Escuadrón Cognitivo)
 * 
 * Agente ultra-rápido encargado exclusivamente de la cortesía y saludos
 * cuando el usuario no ha especificado una intención clara de compra.
 * Cero consumo de BD, respuestas en < 1 milisegundo.
 */
class GreetingAgent @Inject constructor() : TypedSponsorflowAgent<GreetingPayload, GreetingData>() {
    override val agentName: String = "GreetingAgent"
    override val squadron: SquadType = SquadType.COGNITIVE
    override val capabilities: List<String> = listOf("small_talk", "greeting", "fallback")

    override fun mapLegacyTaskToPayload(task: AgentTask): GreetingPayload {
        return GreetingPayload()
    }

    override suspend fun executeTypedInternal(task: SwarmTask<GreetingPayload>): SwarmResult<GreetingData, SwarmError> {
        // Lógica de resiliencia: Textos precargados con Spintax para que no suene robótico
        val templates = listOf(
            "{¡Hola!|¡Qué tal!|¡Saludos!} ¿En qué {te puedo ayudar|te asisto} hoy con la tienda?",
            "{¡Un placer saludarte!|¡Hola!} ¿Buscas algún producto en especial o información de envíos?"
        )
        
        return SwarmResult.Success(
            confidenceScore = 1.0,
            data = GreetingData(rawTemplates = templates)
        )
    }
}
