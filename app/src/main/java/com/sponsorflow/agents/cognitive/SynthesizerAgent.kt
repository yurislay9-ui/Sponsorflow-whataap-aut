package com.sponsorflow.agents.cognitive

import javax.inject.Inject

import android.util.Log
import com.sponsorflow.agents.TypedSponsorflowAgent
import com.sponsorflow.core.FSMDatabase
import com.sponsorflow.core.IntentCategory
import com.sponsorflow.core.ToneCategory
import com.sponsorflow.models.ActionIntent
import com.sponsorflow.models.AgentPayload
import com.sponsorflow.models.AgentResponseData
import com.sponsorflow.models.AgentTask
import com.sponsorflow.models.SquadType
import com.sponsorflow.models.SwarmError
import com.sponsorflow.models.SwarmResult
import com.sponsorflow.models.SwarmTask
import kotlin.random.Random

data class SynthesizerPayload(
    val collectedData: Map<String, Any>
) : AgentPayload

data class SynthesizerData(
    val synthesizedText: String,
    val appliedTone: String,
    val proposedAction: ActionIntent?
) : AgentResponseData

/**
 * [ACTUALIZACIÓN 2026 - MIGRADO A TIPADO ESTRICTO E INYECCIÓN]
 * SYNTHESIZER AGENT (Escuadrón Cognitivo - El Creador)
 * 
 * Re-ingeniería Mythos v4.0: Integra Self-Refining Reasoning.
 * Usa FSMDatabase para un ensamblaje matricial O(1) extremo, sin bloques if/else pesados.
 */
class SynthesizerAgent @Inject constructor() : TypedSponsorflowAgent<SynthesizerPayload, SynthesizerData>() {
    private const val TAG = "NEXUS_Synthesizer"
    override val agentName: String = "SynthesizerAgent"
    override val squadron: SquadType = SquadType.COGNITIVE
    override val capabilities: List<String> = listOf("response_generation", "adaptive_thinking", "fsm_tonal_matching")

    override fun mapLegacyTaskToPayload(task: AgentTask): SynthesizerPayload {
        return SynthesizerPayload(collectedData = task.metadata ?: emptyMap())
    }

    override fun extractLegacyProposedAction(data: SynthesizerData): ActionIntent? {
        return data.proposedAction
    }

    override suspend fun executeTypedInternal(task: SwarmTask<SynthesizerPayload>): SwarmResult<SynthesizerData, SwarmError> {
        val collectedData = task.payload.collectedData
        
        // 1. OBTENER TONO E INTENCIÓN (Nivel 2/3)
        val rawTone = collectedData["customer_tone"] as? String ?: "ESTANDAR"
        val rawIntent = collectedData["raw_intent_category"] as? String ?: "GREETING"
        
        val toneEnum = try { ToneCategory.valueOf(rawTone) } catch (e: Exception) { ToneCategory.ESTANDAR }
        Log.i(TAG, "🧪 Sintetizando respuesta mediante FSM Mátrix O(1) (Tono: \$toneEnum)")
        
        val catalogInfo = collectedData["catalog_context"] as? String ?: ""
        val policyInfo = collectedData["policy_context"] as? String ?: ""
        
        // Detección Crítica de Ventas
        val isOrderReady = collectedData["order_ready"] as? Boolean ?: false
        val orderProduct = collectedData["order_product"] as? String ?: "este producto"
        
        // SRE Guard: Validador de Schema
        if (catalogInfo.length > 5000 || policyInfo.length > 3000) {
            Log.e(TAG, "🚨 ABORTANDO SINTESIS: La Metadata devuelta por los agentes cognitivos es excesivamente larga (>5k chars).")
            return SwarmResult.Failure(SwarmError.InternalException("Safety Guard: Data length exceeded memory limits during synthesis."))
        }

        // 2. FSM MATRIX LOOKUP (O(1) Memory Access)
        val targetIntent = when {
            isOrderReady -> IntentCategory.CODE_ORDER
            catalogInfo.isNotEmpty() || policyInfo.isNotEmpty() -> IntentCategory.SEARCH_CATALOG_POLICY
            rawIntent.startsWith("OBJECTION_") -> try { IntentCategory.valueOf(rawIntent) } catch (e: Exception) { IntentCategory.GREETING }
            else -> IntentCategory.GREETING
        }

        val possibleTemplates = FSMDatabase.matrix[targetIntent.ordinal][toneEnum.ordinal]
        val baseTemplate = if (possibleTemplates.isNotEmpty()) possibleTemplates.random() else "Hola, ¿en qué te puedo ayudar?"

        // 3. RELLENO DE VARIABLES RÁPIDO
        var assembledResponse = baseTemplate.replace("{var_prod}", orderProduct)
        
        val prefShort = collectedData["user_pref_short_text"] as? Boolean ?: false
        val prefEmoji = collectedData["user_pref_emojis"] as? Boolean ?: false
        
        if (targetIntent == IntentCategory.SEARCH_CATALOG_POLICY) {
            val suffix = if (prefShort) {
                "\n\$catalogInfo"
            } else if (catalogInfo.isNotEmpty() && policyInfo.isNotEmpty()) {
                "\n\$catalogInfo\n\nInfo de Tienda:\n\$policyInfo"
            } else {
                "\n\$catalogInfo\$policyInfo\n\n¿Te puedo ayudar con algo más?"
            }
            assembledResponse += suffix
        }

        if (!prefEmoji) {
            assembledResponse = assembledResponse.replace("📦", "").replace("🔥", "").replace("🚀", "")
        } else {
            if (!assembledResponse.contains("✌️")) assembledResponse += " ✌️"
        }
        
        // 4. POLISH
        val finalResponse = parseSpintax(assembledResponse)
        
        return SwarmResult.Success(
            confidenceScore = 1.0,
            data = SynthesizerData(
                synthesizedText = finalResponse,
                appliedTone = toneEnum.name,
                proposedAction = ActionIntent(type = "READY_TO_REVIEW", payload = finalResponse)
            )
        )
    }

    private fun parseSpintax(text: String): String {
        return try {
            val regex = "\\{([^}]+)\\}".toRegex()
            var result = text
            var match = regex.find(result)
            var safetyIterations = 0
            while (match != null && safetyIterations < 100) {
                val options = match.groupValues[1].split("|")
                val replacement = if (options.isNotEmpty()) options[Random.nextInt(options.size)] else ""
                result = result.replaceRange(match.range, replacement)
                match = regex.find(result)
                safetyIterations++
            }
            result
        } catch (e: Exception) {
            "Hola, ¿en qué te puedo ayudar hoy?"
        }
    }
}
