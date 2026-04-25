package com.sponsorflow.agents.direction

import javax.inject.Inject

import android.content.Context
import android.util.Log
import com.sponsorflow.agents.TypedSponsorflowAgent
import com.sponsorflow.core.HybridIntentEngine
import com.sponsorflow.models.AgentPayload
import com.sponsorflow.models.AgentResponseData
import com.sponsorflow.models.AgentTask
import com.sponsorflow.models.SquadType
import com.sponsorflow.models.SwarmError
import com.sponsorflow.models.SwarmResult
import com.sponsorflow.models.SwarmTask

data class RouterPayload(
    val inputText: String,
    val appContext: Context
) : AgentPayload

data class RouterData(
    val routedTo: List<String>,
    val rawIntent: String,
    val rawIntentCategory: String,
    val customerTone: String
) : AgentResponseData

/**
 * [ACTUALIZACIÓN 2026 - MIGRADO A TIPADO ESTRICTO E INYECCIÓN]
 * ROUTER AGENT (Capa 1: El Despachador)
 * 
 * Actualizado a Mythos v4.0. Ya no adivina palabras, invoca al 
 * HybridIntentEngine (Regex -> Fuzzy -> Semántico).
 */
class RouterAgent @Inject constructor(
    private val hybridIntentEngine: HybridIntentEngine
) : TypedSponsorflowAgent<RouterPayload, RouterData>() {
    private const val TAG = "NEXUS_RouterAgent"

    override val agentName: String = "RouterAgent"
    override val squadron: SquadType = SquadType.DIRECTION
    override val capabilities: List<String> = listOf("intent_detection", "adaptive_thinking", "semantic_routing")

    override fun mapLegacyTaskToPayload(task: AgentTask): RouterPayload {
        return RouterPayload(
            inputText = task.message.text,
            appContext = task.message.context.applicationContext
        )
    }

    override suspend fun executeTypedInternal(task: SwarmTask<RouterPayload>): SwarmResult<RouterData, SwarmError> {
        val input = task.payload.inputText
        val appContext = task.payload.appContext
        
        Log.i(TAG, "🚦 Router analizando con Motor Híbrido: [\${task.message.sender}] -> \$input")

        // 0. TERMINAL OVERRIDE (Nivel Dios).
        // Si el Input arranca con '>', salteamos las reglas comerciales y disparamos línea de comandos directa.
        if (input.startsWith("> ")) {
            Log.i(TAG, "💻 Comando de Terminal Interceptado. Desviando Motor al Intérprete (CommandHandlerAgent).")
            return SwarmResult.Success(
                confidenceScore = 1.0,
                data = RouterData(
                    routedTo = listOf("CommandHandlerAgent"),
                    rawIntent = input,
                    rawIntentCategory = "TERMINAL_COMMAND",
                    customerTone = "ESTANDAR"
                )
            )
        }

        // 1. ANÁLISIS DE INTENCIÓN (4 NIVELES) Y TONO (EMOCIÓN)
        val intentResult = try {
            hybridIntentEngine.analyzeQuery(input, userIsAdmin = false)
        } catch (e: Exception) {
            return SwarmResult.Failure(SwarmError.InternalException("Fallback de Motor Híbrido falló en RouterAgent: \${e.message}"))
        }

        val activeAgents = mutableSetOf<String>()
        
        // AGENTE DE APRENDIZAJE: Siempre activo para recolectar métricas del usuario
        activeAgents.add("UserLearningAgent")

        // 2. ENRUTAMIENTO INTELIGENTE (Action Dispatch)
        when (intentResult.intentAction) {
            "SEARCH_CATALOG_POLICY" -> {
                activeAgents.add("CatalogAgent")
                activeAgents.add("PolicyAgent")
            }
            "CODE_ORDER" -> {
                activeAgents.add("OrderParsingAgent")
            }
            "OBJECTION_PRICE", "OBJECTION_TRUST", "OBJECTION_THINKING" -> {
                // Las Objeciones van directo al Synthesizer, no requieren DB lookup.
                // El Synthesizer usará la FSM O(1) Matrix para contestar.
            }
            "BLOCKED_SECURITY" -> {
                Log.w(TAG, "🚨 ALERT: Intento de Inyección o comando prohibido detectado.")
                return SwarmResult.Failure(SwarmError.InternalException("Intento de Inyección o comando prohibido detectado."))
                // TODO: En Fase 4 levantaríamos al SecurityAgent de la documentación.
            }
            else -> { // GREETING O FALLBACK
                activeAgents.add("GreetingAgent")
            }
        }
        
        // ORQUESTACIÓN AVANZADA: Si detectamos múltiples mandos o peticiones complejas (arquitectura híbrida)
        if (input.lowercase().contains("planifica") || input.lowercase().contains("reporte") || input.length > 50) {
            activeAgents.add("PlanningAgent")
        }

        Log.i(TAG, "🚥 Enrutamiento Semántico completado. Tono Detectado: \${intentResult.tone}. Despertando a: \$activeAgents")

        return SwarmResult.Success(
            confidenceScore = 0.95,
            data = RouterData(
                routedTo = activeAgents.toList(),
                rawIntent = input,
                rawIntentCategory = intentResult.intentAction,
                customerTone = intentResult.tone
            )
        )
    }

    // Adaptador exclusivo para que AgentTask legada reciba la estructura rara `.Routed(routedTo, extractedData)`.
    override suspend fun executeInternal(taskPayload: AgentTask): AgentResult {
        return when (val typedRes = super.executeInternal(taskPayload)) {
            is AgentResult.Success -> {
                 val data = typedRes.extractedData?.get("typed_data") as? RouterData
                 if (data != null) {
                     AgentResult.Routed(
                         routedTo = data.routedTo,
                         extractedData = mapOf(
                             "raw_intent" to data.rawIntent,
                             "raw_intent_category" to data.rawIntentCategory,
                             "customer_tone" to data.customerTone
                         )
                     )
                 } else {
                     AgentResult.Failure("Fallo de casteo de RouterData")
                 }
            }
            else -> typedRes
        }
    }
}
