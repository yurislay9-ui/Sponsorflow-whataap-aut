package com.sponsorflow.agents.direction

import javax.inject.Inject

import android.util.Log
import com.sponsorflow.agents.TypedSponsorflowAgent
import com.sponsorflow.models.ActionIntent
import com.sponsorflow.models.AgentPayload
import com.sponsorflow.models.AgentResponseData
import com.sponsorflow.models.AgentTask
import com.sponsorflow.models.SquadType
import com.sponsorflow.models.SwarmError
import com.sponsorflow.models.SwarmResult
import com.sponsorflow.models.SwarmTask

data class BuddyReviewerPayload(
    val clientName: String,
    val hasGeneratedImage: Boolean,
    val isSalesClosing: Boolean,
    val originalAction: ActionIntent?
) : AgentPayload

data class BuddyReviewerData(
    val warnings: List<String>,
    val verifiedAction: ActionIntent?
) : AgentResponseData

/**
 * [ACTUALIZACIÓN 2026 - MIGRADO A TIPADO ESTRICTO E INYECCIÓN]
 * BUDDY REVIEWER AGENT (Capa 1: Auditoría y Calidad)
 * 
 * Basado en el Pilar 10 de CLAW-LITE: "Dos cerebros, una mente".
 * Analiza la respuesta final del Synthesizer ANTES de que el teléfono la envíe.
 */
class BuddyReviewerAgent @Inject constructor() : TypedSponsorflowAgent<BuddyReviewerPayload, BuddyReviewerData>() {
    private const val TAG = "NEXUS_BuddyReviewer"

    override val agentName: String = "BuddyReviewerAgent"
    override val squadron: SquadType = SquadType.DIRECTION
    override val capabilities: List<String> = listOf("quality_assurance", "fallback_injection")

    override fun mapLegacyTaskToPayload(task: AgentTask): BuddyReviewerPayload {
        return BuddyReviewerPayload(
            clientName = task.message.sender.split(":").lastOrNull() ?: "amigo",
            hasGeneratedImage = task.metadata?.get("generated_flyer_path") != null,
            isSalesClosing = task.metadata?.get("raw_intent_category") == "CODE_ORDER" || task.metadata?.get("order_ready") == true,
            originalAction = task.proposedAction
        )
    }

    override fun extractLegacyProposedAction(data: BuddyReviewerData): ActionIntent? {
        return data.verifiedAction
    }

    override suspend fun executeTypedInternal(task: SwarmTask<BuddyReviewerPayload>): SwarmResult<BuddyReviewerData, SwarmError> {
        val rawMessage = task.payload.originalAction?.payload

        if (rawMessage.isNullOrBlank()) {
            Log.e(TAG, "🚨 Alerta Crítica: El Synthesizer generó respuesta VACÍA. Bloqueando envío.")
            return SwarmResult.Failure(SwarmError.InternalException("Respuesta generada vacía"))
        }

        var finalScore = 1.0
        val warnings = mutableListOf<String>()

        // 1. CHEQUEO SINTÁCTICO: ¿Quedó algún código Spintax roto sin procesar en el texto?
        if (rawMessage.contains("{") || rawMessage.contains("}")) {
            finalScore -= 0.5
            warnings.add("Spintax roto o variables sin rellenar")
        }

        // 2. CHEQUEO DE INYECCIÓN DE PROMPT / SEGURIDAD (PII Básico)
        if (rawMessage.contains("JSON_ORDER") || rawMessage.contains("sys_prompt")) {
            finalScore -= 0.8
            warnings.add("Fuga potencial de datos internos (Prompt Injection o JSON volcado)")
        }
        
        // 3. HUMAN-IN-THE-LOOP (HITL) FLAG
        val hasGeneratedImage = task.payload.hasGeneratedImage
        val isSalesClosing = task.payload.isSalesClosing

        if (hasGeneratedImage || isSalesClosing) {
            Log.i(TAG, "✋ INTERCEPTO DE VENTA / FLYER. Marcando para Veredicto Final Humano.")
            warnings.add("Requiere Aprobación Humana (Imagen 2D Generada o Intento de Cierre de Venta)")
            finalScore = 0.5 // Forzamos NeedsReview
        }

        // 4. EVACUACIÓN A PRUEBA DE BALAS (Self-Healing)
        val safeMessageToDeliver = if (finalScore < 0.85 && !hasGeneratedImage && !isSalesClosing) {
            Log.w(TAG, "⚠️ BUDDY REVIEW: Respuesta rechazada. Score=\$finalScore. Fallos: \$warnings. Inyectando Fallback de Emergencia.")
            "Tuvimos un pequeño inconveniente procesando tu solicitud, \${task.payload.clientName}. Un asesor humano se contactará contigo en breve."
        } else {
            Log.i(TAG, "✅ BUDDY REVIEW Procesado. Score: \$finalScore")
            rawMessage
        }

        val verifiedActionObj = task.payload.originalAction?.copy(payload = safeMessageToDeliver) ?: task.payload.originalAction

        if (finalScore >= 0.85) {
            return SwarmResult.Success(
                confidenceScore = finalScore,
                data = BuddyReviewerData(
                    warnings = warnings,
                    verifiedAction = verifiedActionObj
                )
            )
        } else {
            return SwarmResult.NeedsReview(
                error = SwarmError.HumanInterventionRequired("Veredicto Final Requerido. Score: \$finalScore. Motivo: \$warnings")
            )
        }
    }
}
