package com.sponsorflow.agents.cognitive

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
import org.json.JSONArray
import org.json.JSONObject

data class PlanningPayload(val userInput: String) : AgentPayload

data class PlanningData(
    val isComplexPlan: Boolean,
    val executionPlanJsonStr: String,
    val proposedAction: ActionIntent?
) : AgentResponseData

/**
 * [ACTUALIZACIÓN 2026 - MIGRADO A TIPADO ESTRICTO E INYECCIÓN]
 * PLANNING AGENT (El "Jefe" Orchestrator)
 * 
 * Actúa como la capa de Cerebro de orquestación. Convierte peticiones complejas en
 * un plan secuencial de acciones (Array estructurado). Este agente NO ejecuta,
 * solo estructura el trabajo para que el Músculo (Motor V4.0) actúe.
 */
class PlanningAgent @Inject constructor() : TypedSponsorflowAgent<PlanningPayload, PlanningData>() {
    private const val TAG = "NEXUS_PlanningAgent"
    
    override val agentName: String = "PlanningAgent"
    override val squadron: SquadType = SquadType.COGNITIVE
    override val capabilities: List<String> = listOf("planning", "orchestration", "task_division")

    override fun mapLegacyTaskToPayload(task: AgentTask): PlanningPayload {
        return PlanningPayload(userInput = task.message.text)
    }

    override fun extractLegacyProposedAction(data: PlanningData): ActionIntent? {
        return data.proposedAction
    }

    override suspend fun executeTypedInternal(task: SwarmTask<PlanningPayload>): SwarmResult<PlanningData, SwarmError> {
        val userInput = task.payload.userInput
        
        Log.i(TAG, "🧠 Analizando intención compleja: \$userInput")
        
        // 1. Filtrado básico: si es muy corto, no necesita planificación avanzada.
        if (userInput.length < 10) {
            return SwarmResult.Failure(SwarmError.InternalException("El input es demasiado corto para requerir un plan maestro."))
        }
        
        try {
            // Simulamos la lógica de razonamiento o llamada a LLM/ONNX para generar un plan.
            val planArray = generateExecutionPlan(userInput)
            
            if (planArray.length() == 0) {
                return SwarmResult.Failure(SwarmError.InternalException("No se pudo generar un plan de acción viable."))
            }
            
            Log.i(TAG, "🗺️ Plan Maestro Creado con \${planArray.length()} pasos.")
            
            return SwarmResult.Success(
                confidenceScore = 0.90,
                data = PlanningData(
                    isComplexPlan = true,
                    executionPlanJsonStr = planArray.toString(),
                    proposedAction = ActionIntent("EXECUTE_PLAN", planArray.toString())
                )
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "🚨 Error generando plan de ejecución: \${e.message}")
            return SwarmResult.Failure(SwarmError.InternalException("Excepción en Planning Agent"))
        }
    }
    
    /**
     * Módulo de Generación de Planes (Híbrido Determinista / IA)
     */
    private fun generateExecutionPlan(input: String): JSONArray {
        val plan = JSONArray()
        val textLower = input.lowercase()
        
        // Heurística de demostración
        if (textLower.contains("enviar") || textLower.contains("reporte") || textLower.contains("patrocinadores")) {
            // Paso 1
            plan.put(JSONObject().apply {
                put("step", 1)
                put("action", "PARSE_DATA")
                put("target", "reporte")
            })
            // Paso 2
            plan.put(JSONObject().apply {
                put("step", 2)
                put("action", "FETCH_MEMBERS")
                put("target", "patrocinadores_vip")
            })
            // Paso 3
            plan.put(JSONObject().apply {
                put("step", 3)
                put("action", "UI_SEND_MESSAGE")
                put("payload", "Enviando reporte cíclico.")
            })
        } else {
            // Plan genérico por defecto
            plan.put(JSONObject().apply {
                put("step", 1)
                put("action", "ANALYZE_INTENT") 
            })
        }
        
        return plan
    }
}
