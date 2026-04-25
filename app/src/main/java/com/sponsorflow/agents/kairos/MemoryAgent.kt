package com.sponsorflow.agents.kairos

import javax.inject.Inject

import android.util.Log
import com.sponsorflow.agents.TypedSponsorflowAgent
import com.sponsorflow.data.SponsorflowDatabase
import com.sponsorflow.models.AgentPayload
import com.sponsorflow.models.AgentResponseData
import com.sponsorflow.models.AgentResult
import com.sponsorflow.models.AgentTask
import com.sponsorflow.models.SquadType
import com.sponsorflow.models.SwarmError
import com.sponsorflow.models.SwarmResult
import com.sponsorflow.models.SwarmTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
data class MemoryPayload(val ignored: Unit = Unit) : AgentPayload
data class MemoryData(val freedBytesEstimate: Long) : AgentResponseData

/**
 * MEMORY AGENT (Escuadrón KAIROS - El Consolidador)
 * 
 * Sistema "AutoDream". Trabaja en background cuando el teléfono carga batería.
 * Revisa el buzón de interacciones de los clientes en la base de datos, 
 * consolida las preferencias de compra en un perfil corto y borra el historial 
 * basura antiguo para mantener la app ultra rápida y liviana (< 50MB Database).
 */
class MemoryAgent @Inject constructor() : TypedSponsorflowAgent<MemoryPayload, MemoryData>() {
    private const val TAG = "NEXUS_MemoryAgent"
    
    override val agentName: String = "MemoryAgent"
    override val squadron: SquadType = SquadType.KAIROS
    override val capabilities: List<String> = listOf("memory_consolidation", "database_cleanup", "auto_dream")

    override fun mapLegacyTaskToPayload(task: AgentTask): MemoryPayload {
        return MemoryPayload()
    }

    override suspend fun executeTypedInternal(task: SwarmTask<MemoryPayload>): SwarmResult<MemoryData, SwarmError> {
        return withContext(Dispatchers.IO) {
            Log.i(TAG, "🌙 Iniciando AutoDream (Consolidación de Memoria KAIROS)...")
            
            try {
                val db = SponsorflowDatabase.getDatabase(task.message.context)
                val businessDao = db.businessDao()
                
                // Obtener todos los clientes que necesitan ser consolidados
                val customers = businessDao.getCustomersNeedingConsolidation() 
                
                var freedBytesEstimate = 0L
                val startTime = System.currentTimeMillis()
                val TIME_LIMIT_MS = 8000L // Máximo 8 segundos para AutoDream (Prevenir ANR)
                var processedCount = 0

                for (customer in customers) {
                    if (System.currentTimeMillis() - startTime > TIME_LIMIT_MS) {
                        Log.w(TAG, "⏰ Límite térmico alcanzado en AutoDream (8s). Abortando compresión. Restantes ignorados para salvar CPU.")
                        break
                    }
                    
                    val rawHistory = customer.unconsolidatedHistory
                    if (rawHistory.length > 100) { // Un umbral razonable para consolidar
                        
                        // EXTRACCIÓN DE INSIGHTS DETERMINÍSTICA (Sin LLM pesados)
                        val interests = mutableSetOf<String>() // Set to avoid duplicates
                        if (rawHistory.contains("talla", ignoreCase = true)) interests.add("Mencionó tallas")
                        if (rawHistory.contains("precio", ignoreCase = true)) interests.add("Busca precios")
                        if (rawHistory.contains("descuento", ignoreCase = true)) interests.add("Busca ofertas")
                        if (rawHistory.contains("queja|mal|roto".toRegex(RegexOption.IGNORE_CASE))) interests.add("Tuvo problemas previos")
                        
                        // SRE Guard: Prevenir crecimiento infinito del perfil (OOM Guard)
                        val currentInterests = customer.perfilCognitivo.split(" | ")
                            .filter { it.isNotBlank() }
                            .toMutableSet()
                        
                        currentInterests.addAll(interests)
                        
                        // Amnesia Controlada: Solo guardamos los últimos 8 insights únicos.
                        val newCognitiveProfile = currentInterests.takeLast(8).joinToString(" | ")

                        // COMPRESIÓN (Borramos la basura y guardamos el perfil duro usando commitMechanicalMemory)
                        freedBytesEstimate += rawHistory.length
                        businessDao.commitMechanicalMemory(customer.senderId, newCognitiveProfile, customer.tags)
                        
                        Log.d(TAG, "🧠 Memoria de [${customer.senderId}] consolidada. Perfil: $newCognitiveProfile")
                    } else {
                        // Si el historial es tan corto que no vale la pena analizar, igual lo limpiamos
                        businessDao.commitMechanicalMemory(customer.senderId, customer.perfilCognitivo, customer.tags)
                    }
                }
                
                Log.i(TAG, "☀️ AutoDream finalizado. Se purgaron ~${freedBytesEstimate / 1024} KB de basura textual.")

                SwarmResult.Success(MemoryData(freedBytesEstimate), 1.0)

            } catch (e: Exception) {
                Log.e(TAG, "💥 Error durante la fase REM de MemoryAgent: ${e.message}")
                SwarmResult.Failure(SwarmError.InternalException("Error en AutoDream"))
            }
        }
    }
}
