package com.sponsorflow.agents.cognitive

import javax.inject.Inject

import android.content.Context
import android.util.Log
import com.sponsorflow.agents.TypedSponsorflowAgent
import com.sponsorflow.models.AgentPayload
import com.sponsorflow.models.AgentResponseData
import com.sponsorflow.models.AgentTask
import com.sponsorflow.models.SquadType
import com.sponsorflow.models.SwarmError
import com.sponsorflow.models.SwarmResult
import com.sponsorflow.models.SwarmTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// 1. Contratos Estrictos
data class PolicyPayload(
    val appContext: Context // Temporal as we will migrate to DI
) : AgentPayload

data class PolicyData(
    val policyContext: String
) : AgentResponseData

/**
 * [ACTUALIZACIÓN 2026 - MIGRADO A TIPADO ESTRICTO E INYECCIÓN]
 * POLICY AGENT (Escuadrón Cognitivo)
 * 
 * Agente especialista en conocimiento y reglas de la empresa.
 * Accede a preferencias encriptadas para conocer tiempos de entrega, ubicación, etc.
 * Ya no es un `object`. Se prepara para recibir DB inyectado o SharedPreferences.
 */
class PolicyAgent @Inject constructor() : TypedSponsorflowAgent<PolicyPayload, PolicyData>() {
    private const val TAG = "NEXUS_PolicyAgent"
    override val agentName: String = "PolicyAgent"
    override val squadron: SquadType = SquadType.COGNITIVE
    override val capabilities: List<String> = listOf("business_rules", "shipping_info", "schedule")

    override fun mapLegacyTaskToPayload(task: AgentTask): PolicyPayload {
        return PolicyPayload(appContext = task.message.context.applicationContext)
    }

    override suspend fun executeTypedInternal(task: SwarmTask<PolicyPayload>): SwarmResult<PolicyData, SwarmError> {
        return withContext(Dispatchers.IO) {
            try {
                val prefs = task.payload.appContext.getSharedPreferences("sponsorflow_settings", Context.MODE_PRIVATE)
                val companyRules = prefs.getString("company_knowledge", "Nuestras políticas estándar de atención al cliente aplican.") ?: ""
                
                SwarmResult.Success(
                    confidenceScore = 1.0, // Al ser conocimiento duro en texto, su certeza es siempre 100%
                    data = PolicyData(policyContext = companyRules)
                )
            } catch (e: Exception) {
                Log.e(TAG, "💥 Error leyendo preferencias bloqueadas desde PolicyAgent: \${e.message}")
                SwarmResult.Failure(SwarmError.InternalException("Error consultando policy context"))
            }
        }
    }
}
