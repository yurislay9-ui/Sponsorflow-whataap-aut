package com.sponsorflow.agents.cognitive

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

data class ReasoningPayload(
    val previousError: String,
    val failedStep: String
) : AgentPayload

data class ReasoningData(
    val recoveryActive: Boolean,
    val originalError: String,
    val proposedAction: ActionIntent?
) : AgentResponseData

/**
 * [ACTUALIZACIÓN 2026 - MIGRADO A TIPADO ESTRICTO E INYECCIÓN]
 * REASONING AGENT (Mecanismo de Recuperación / Fallback)
 * 
 * Este agente NO se ejecuta en flujo normal. Se invoca SÓLO cuando un plan
 * pre-establecido falla (ej. cambió la UI, el botón no existe, el flujo se cortó).
 * Lee el estado actual del árbol de nodos de pantalla y razona una solución.
 */
import javax.inject.Inject

class ReasoningAgent @Inject constructor() : TypedSponsorflowAgent<ReasoningPayload, ReasoningData>() {
    private const val TAG = "NEXUS_ReasoningAgent"
    
    override val agentName: String = "ReasoningAgent"
    override val squadron: SquadType = SquadType.COGNITIVE
    override val capabilities: List<String> = listOf("fallback_recovery", "ui_inference", "dynamic_healing")

    override fun mapLegacyTaskToPayload(task: AgentTask): ReasoningPayload {
        return ReasoningPayload(
            previousError = task.metadata?.get("last_error") as? String ?: "desconocido",
            failedStep = task.metadata?.get("failed_step") as? String ?: "n/a"
        )
    }

    override fun extractLegacyProposedAction(data: ReasoningData): ActionIntent? {
        return data.proposedAction
    }

    override suspend fun executeTypedInternal(task: SwarmTask<ReasoningPayload>): SwarmResult<ReasoningData, SwarmError> {
        val previousError = task.payload.previousError
        val failedStep = task.payload.failedStep
        
        Log.w(TAG, "🚨 Activando Protocolo de Razonamiento (Recovery Mode)")
        Log.w(TAG, "Causa del fallo: \$previousError en el paso: \$failedStep")
        
        try {
            // Simulamos la lectura del árbol de accesibilidad (AccessibilityNodeInfo) visual.
            // Si estuviéramos conectando un LLM Vision o procesando ONNX:
            val proposedRecoveryAction = reasonFallbackStrategy(failedStep, previousError)
            
            if (proposedRecoveryAction == null) {
                // El razonamiento falló o no encontró una solución plausible.
                return SwarmResult.NeedsReview(SwarmError.HumanInterventionRequired("No se pudo razonar una salida para el error: \$previousError"))
            }
            
            Log.i(TAG, "💡 Estrategia de recuperación encontrada. Ejecutando acción alternativa.")
            
            return SwarmResult.Success(
                confidenceScore = 0.85,
                data = ReasoningData(
                    recoveryActive = true,
                    originalError = previousError,
                    proposedAction = proposedRecoveryAction
                )
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error crítico en ReasoningAgent: \${e.message}")
            return SwarmResult.Failure(SwarmError.InternalException("Fallo interno del agente de razonamiento."))
        }
    }
    
    /**
     * Motor de curación dinámica (Healing) usando inferencia basada en reglas / LLM.
     */
    private fun reasonFallbackStrategy(failedStep: String, error: String): ActionIntent? {
        // Lógica Heurística de ejemplo (El verdadero "Cerebro" procesaría esto dinámicamente)
        return when {
            error.contains("button not found") || error.contains("UI_SEND_MESSAGE") -> {
                Log.d(TAG, "Botón de envío no encontrado. Infiriendo alternativas: [Enter de teclado, Icono de avión de papel]")
                ActionIntent("UI_ENTER_FALLBACK", "Simular tecla ENTER")
            }
            error.contains("timeout") -> {
                Log.d(TAG, "Timeout detectado. Sugiriendo volver a intentar con backoff.")
                ActionIntent("RETRY_STEP", failedStep)
            }
            else -> {
                Log.d(TAG, "Error no mapeado, solicitando intervención humana.")
                null
            }
        }
    }
}
