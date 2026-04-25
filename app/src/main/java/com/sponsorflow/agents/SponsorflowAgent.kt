package com.sponsorflow.agents

import com.sponsorflow.models.AgentResult
import com.sponsorflow.models.AgentTask
import com.sponsorflow.models.SquadType
import com.sponsorflow.models.AgentPayload
import com.sponsorflow.models.AgentResponseData
import com.sponsorflow.models.SwarmResult
import com.sponsorflow.models.SwarmError
import com.sponsorflow.models.SwarmTask
import com.sponsorflow.models.LegacyAgentPayload
import com.sponsorflow.models.ActionIntent
import android.util.Log

/**
 * @Deprecated Contrato Universal (El ADN Legacy) para agentes no migrados aún.
 */
@Deprecated("Migrating to TypedSponsorflowAgent<P, R>")
abstract class SponsorflowAgent {
    abstract val agentName: String
    abstract val squadron: SquadType
    abstract val capabilities: List<String>
    
    protected abstract suspend fun executeInternal(taskPayload: AgentTask): AgentResult

    suspend fun executeTask(taskPayload: AgentTask): AgentResult {
        return try {
            executeInternal(taskPayload)
        } catch (e: Exception) {
            Log.e("NEXUS_AgentSystem", "💥 Caída de Agente Clásico \$agentName: \${e.message}", e)
            AgentResult.Failure("Fallo interno en \$agentName: \${e.localizedMessage}")
        }
    }
}

/**
 * [ACTUALIZACIÓN 2026 - TIPADO ESTRICTO]
 * Nuevo Contrato Universal tipado por Genéricos para inyección segura.
 * Reemplazará al `SponsorflowAgent` desactualizado progresivamente.
 * Nota: Temporalmente hereda de SponsorflowAgent para no romper el SwarmManager Registry.
 */
abstract class TypedSponsorflowAgent<out P : AgentPayload, out R : AgentResponseData> : SponsorflowAgent() {
    
    // Lógica cruda del agente a ser implementada, sin Throwables, monádica.
    protected abstract suspend fun executeTypedInternal(task: SwarmTask<@UnsafeVariance P>): SwarmResult<@UnsafeVariance R, SwarmError>

    suspend fun executeTypedTask(task: SwarmTask<@UnsafeVariance P>): SwarmResult<@UnsafeVariance R, SwarmError> {
        return try {
            executeTypedInternal(task)
        } catch (e: Exception) {
            Log.e("NEXUS_AgentSystem", "💥 Caída del Agente Tipado \$agentName: \${e.message}", e)
            SwarmResult.Failure(SwarmError.InternalException("Fallo interno en \$agentName: \${e.localizedMessage}"))
        }
    }

    /**
     * Puente Abstracto: Los agentes nuevos deben definir cómo extraer su Payload tipado
     * a partir de la antigua tarea 'AgentTask' mientras termina la migración.
     */
    protected abstract fun mapLegacyTaskToPayload(task: AgentTask): P
    
    /**
     * Puente Abstracto Opcional: Permite extraer un ActionIntent de la respuesta Data para el viejo Pipeline.
     */
    protected open fun extractLegacyProposedAction(data: @UnsafeVariance R): ActionIntent? = null

    // ====== PUENTE DE REGRESIÓN (BRIDGE) PARA SWARM MANAGER LEGACY ======
    // Todo agente migrado a Typed es compatible nativamente con el motor viejo.
    override suspend fun executeInternal(taskPayload: AgentTask): AgentResult {
        val safePayload = try {
            mapLegacyTaskToPayload(taskPayload)
        } catch (e: Exception) {
            return AgentResult.Failure("Error de Mapeo Legacy en \$agentName: \${e.message}")
        }

        val typedTask = SwarmTask(
            messageId = "legacy-\${System.currentTimeMillis()}",
            message = taskPayload.message,
            payload = safePayload
        )

        return when (val res = executeTypedTask(typedTask)) {
            is SwarmResult.Success -> {
                val responseMap = mapOf(
                    "isTypedResponseReady" to true,
                    "typed_data" to res.data,
                    "confidence" to res.confidenceScore
                )
                AgentResult.Success(res.confidenceScore, responseMap, extractLegacyProposedAction(res.data))
            }
            is SwarmResult.Failure -> {
                val errorMsg = when(res.error) {
                    is SwarmError.InvalidSchema -> "Invalid Schema: \${res.error.missingFields}"
                    is SwarmError.LlmTimeout -> "Timeout: \${res.error.ms} ms"
                    is SwarmError.InternalException -> "Internal: \${res.error.reason}"
                    is SwarmError.HumanInterventionRequired -> "Human: \${res.error.reason}"
                }
                AgentResult.Failure(errorMsg)
            }
            is SwarmResult.NeedsReview -> AgentResult.NeedsReview(res.error.toString())
        }
    }
}
