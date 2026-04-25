package com.sponsorflow.agents.cognitive

import javax.inject.Inject

import android.util.Log
import com.sponsorflow.agents.TypedSponsorflowAgent
import com.sponsorflow.models.AgentPayload
import com.sponsorflow.models.AgentResponseData
import com.sponsorflow.models.SquadType
import com.sponsorflow.models.SwarmError
import com.sponsorflow.models.SwarmResult
import com.sponsorflow.models.SwarmTask
import com.sponsorflow.models.AgentTask

// 1. Contratos Estrictos
data class OrderParsingPayload(val textToAnalyze: String) : AgentPayload

data class OrderParsingData(
    val isOrderReady: Boolean,
    val fullName: String,
    val address: String,
    val product: String
) : AgentResponseData

/**
 * [ACTUALIZACIÓN 2026 - MIGRADO A TIPADO ESTRICTO E INYECCIÓN]
 * ORDER PARSING AGENT (Escuadrón Cognitivo - El Cajero)
 * 
 * Reemplaza la antigua extracción de pedidos por lógica heurística.
 * Ya no es un `object` Singleton, ahora es instanciable para evitar Memory Leaks.
 */
class OrderParsingAgent @Inject constructor() : TypedSponsorflowAgent<OrderParsingPayload, OrderParsingData>() {
    private const val TAG = "NEXUS_OrderParsingAgent"
    
    override val agentName: String = "OrderParsingAgent"
    override val squadron: SquadType = SquadType.COGNITIVE
    override val capabilities: List<String> = listOf("entity_extraction", "order_parsing", "ner")

    override fun mapLegacyTaskToPayload(task: AgentTask): OrderParsingPayload {
        return OrderParsingPayload(textToAnalyze = task.message.text)
    }

    // Adaptador exclusivo para que AgentTask legada reciba la estructura en Map
    override suspend fun executeInternal(taskPayload: AgentTask): com.sponsorflow.models.AgentResult {
        return when (val typedRes = super.executeInternal(taskPayload)) {
            is com.sponsorflow.models.AgentResult.Success -> {
                 val data = typedRes.extractedData?.get("typed_data") as? OrderParsingData
                 if (data != null) {
                     com.sponsorflow.models.AgentResult.Success(
                         confidenceScore = typedRes.confidenceScore,
                         extractedData = mapOf(
                             "order_ready" to data.isOrderReady,
                             "order_name" to data.fullName,
                             "order_address" to data.address,
                             "order_product" to data.product
                         )
                     )
                 } else {
                     com.sponsorflow.models.AgentResult.Failure("Fallo de casteo de OrderParsingData")
                 }
            }
            else -> typedRes
        }
    }

    override suspend fun executeTypedInternal(task: SwarmTask<OrderParsingPayload>): SwarmResult<OrderParsingData, SwarmError> {
        val text = task.payload.textToAnalyze
        
        // 1. EVALUACIÓN CORTOCIRCUITO (Short-Circuit)
        val targetKeywords = listOf("nombre", "dirección", "direccion", "producto", "pedido", "enviar a")
        val matchCount = targetKeywords.count { text.lowercase().contains(it) }
        
        if (matchCount < 2) {
            return SwarmResult.Failure(SwarmError.InternalException("No reconoció palabras clave suficientes"))
        }

        Log.i(TAG, "📦 Posible estructura de Venta detectada. Extrayendo entidades (NER)...")
        
        // 2. EXTRACCIÓN MILITAR (Regex Seguro)
        val nameRegex = "(?i)(?:nombre|cliente)[:\\s]+([A-Za-zñÑáéíóúÁÉÍÓÚ\\s]+)(?=\\n|direcci|celular|producto|\$)".toRegex()
        val addressRegex = "(?i)(?:dirección|direccion|enviar a)[:\\s]+([^\\n]+)".toRegex()
        val productRegex = "(?i)(?:producto|pedido|modelo)[:\\s]+([^\\n]+)".toRegex()

        val fullName = nameRegex.find(text)?.groupValues?.getOrNull(1)?.trim() ?: "Cliente Omitido"
        val address = addressRegex.find(text)?.groupValues?.getOrNull(1)?.trim() ?: "Dirección Pendiente"
        val product = productRegex.find(text)?.groupValues?.getOrNull(1)?.trim() ?: "Catálogo General"
        
        // SRE Guard: Evitar inyección de pedidos silenciosos e incompletos o comandos trampa de 2 letras
        if (fullName.length < 3 || address.length < 5 || product.length < 3) {
             Log.e(TAG, "🚨 Orden ignorada por estructuración pobre (Intento de Inyección de Fake Order o datos rotos).")
             return SwarmResult.Failure(SwarmError.InvalidSchema(listOf("fullName", "address", "product")))
        }

        val responseData = OrderParsingData(
            isOrderReady = true,
            fullName = fullName,
            address = address,
            product = product
        )
        
        Log.i(TAG, "💸 ENTIDADES EXTRAÍDAS: \$responseData")

        return SwarmResult.Success(
            confidenceScore = 0.95, // Alta seguridad
            data = responseData
        )
    }
}
