package com.sponsorflow.agents.cognitive

import javax.inject.Inject

import android.util.Log
import com.sponsorflow.agents.TypedSponsorflowAgent
import com.sponsorflow.data.SponsorflowDatabase
import com.sponsorflow.models.AgentPayload
import com.sponsorflow.models.AgentResponseData
import com.sponsorflow.models.SquadType
import com.sponsorflow.models.SwarmError
import com.sponsorflow.models.SwarmResult
import com.sponsorflow.models.SwarmTask
import com.sponsorflow.models.AgentTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import android.content.Context

// 1. Contratos Estrictos
data class CatalogPayload(
    val queryToSearch: String,
    val appContext: Context // Temporal hasta conectar Dagger/Hilt
) : AgentPayload

data class CatalogData(val formattedCatalogString: String) : AgentResponseData

/**
 * [ACTUALIZACIÓN 2026 - MIGRADO A TIPADO ESTRICTO E INYECCIÓN]
 * CATALOG AGENT (Escuadrón Cognitivo)
 * 
 * Agente especialista en Inventario y Catálogo. 
 * El único componente autorizado a leer la SQLite (Room) en busca de productos.
 * Ya no es un `object`. Se prepara para recibir DB inyectado (Por ahora la toma del payload context).
 */
class CatalogAgent @Inject constructor() : TypedSponsorflowAgent<CatalogPayload, CatalogData>() {
    private const val TAG = "NEXUS_CatalogAgent"
    override val agentName: String = "CatalogAgent"
    override val squadron: SquadType = SquadType.COGNITIVE
    override val capabilities: List<String> = listOf("product_search", "price_check", "stock_check")

    // Puente para la migración legacy
    override fun mapLegacyTaskToPayload(task: AgentTask): CatalogPayload {
        return CatalogPayload(
            queryToSearch = task.message.text,
            appContext = task.message.context.applicationContext
        )
    }

    override suspend fun executeTypedInternal(task: SwarmTask<CatalogPayload>): SwarmResult<CatalogData, SwarmError> {
        return withContext(Dispatchers.IO) {
            try {
                // SRE: Timeout estricto de 3 segundos para aislar bloqueos.
                val dbResult = withTimeoutOrNull(3000L) {
                    val db = SponsorflowDatabase.getDatabase(task.payload.appContext)
                    val products = db.catalogDao().searchRelevantProducts(task.payload.queryToSearch)
                    
                    if (products.isEmpty()) {
                        "Por ahora no encontré ese producto exacto en la base. {Ofrece el catálogo general|Dime si buscas algo similar}."
                    } else {
                        products.joinToString("\n") { "- \${it.name}: $\${it.sellPrice}" }
                    }
                }

                if (dbResult == null) {
                    Log.w(TAG, "⚠️ Timeout al consultar la base de datos (Protección SRE Activada).")
                    return@withContext SwarmResult.Failure(SwarmError.LlmTimeout(3000L))
                }

                SwarmResult.Success(
                    confidenceScore = 0.90,
                    data = CatalogData(formattedCatalogString = dbResult)
                )

            } catch (e: Exception) {
                Log.e(TAG, "💥 Error crítico en BD desde CatalogAgent: \${e.message}")
                SwarmResult.Failure(SwarmError.InternalException("Excepción DB: \${e.message}"))
            }
        }
    }
}
