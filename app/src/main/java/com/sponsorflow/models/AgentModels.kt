package com.sponsorflow.models

import android.app.PendingIntent
import android.content.Context

/**
 * Modelo de Entrada Estandarizado.
 * Todas las notificaciones o interceptaciones visuales se convierten
 * a este formato antes de entrar al ecosistema Swarm.
 */
data class InboundMessage(
    val context: Context,
    val sender: String,
    val text: String,
    val replyIntent: PendingIntent?,
    val remoteInputKey: String?
)

/**
 * Define el escuadrón al que pertenece un agente.
 */
enum class SquadType {
    DIRECTION, // Router, Buddy
    COGNITIVE, // Catalog, Policy, OrderParsing, Synthesizer
    ACTION,    // Publisher, Comms
    KAIROS     // Memory, Privacy
}

/**
 * Acción estructurada propuesta por un agente para que el sistema actúe.
 */
data class ActionIntent(
    val type: String, // Ej: "REPLY_TEXT", "DB_SAVE", "UI_CLICK"
    val payload: String
)

// ============================================================================
// 🧱 Refactorización Arquitectónica V4.5 (Estricto Tipado y Memoria Cero Fricción)
// ============================================================================

/**
 * [ACTUALIZACIÓN 2026 - MIGRANDO A TIPADO ESTRICTO]
 * Estructura de Datos Rígida para la Memoria de Trabajo del Swarm.
 * Evita el uso de Map<String, Any> que causa ClassCastException.
 */
data class SwarmMetadata(
    var traceId: String = "",
    var customerTone: String = "ESTANDAR",
    var rawIntentCategory: String = "GREETING",
    var routedNodes: List<String> = emptyList(),
    var safeText: String? = null,
    var catalogContext: String? = null,
    var policyContext: String? = null,
    var orderReady: Boolean = false,
    var orderName: String? = null,
    var orderAddress: String? = null,
    var orderProduct: String? = null,
    var generatedFlyerPath: String? = null,
    var lastError: String? = null,
    var userPrefShortText: Boolean = false,
    var userPrefEmojis: Boolean = false
) {
    /**
     * Método puente para importar datos de Agentes que aún retornan Map<String, Any>
     */
    fun importLegacyMap(map: Map<String, Any>) {
        if (map.containsKey("trace_id")) traceId = map["trace_id"] as? String ?: traceId
        if (map.containsKey("customer_tone")) customerTone = map["customer_tone"] as? String ?: customerTone
        if (map.containsKey("raw_intent_category")) rawIntentCategory = map["raw_intent_category"] as? String ?: rawIntentCategory
        if (map.containsKey("routed_nodes")) routedNodes = map["routed_nodes"] as? List<String> ?: routedNodes
        if (map.containsKey("safe_text")) safeText = map["safe_text"] as? String ?: safeText
        if (map.containsKey("catalog_context")) catalogContext = map["catalog_context"] as? String ?: catalogContext
        if (map.containsKey("policy_context")) policyContext = map["policy_context"] as? String ?: policyContext
        if (map.containsKey("order_ready")) orderReady = map["order_ready"] as? Boolean ?: orderReady
        if (map.containsKey("order_name")) orderName = map["order_name"] as? String ?: orderName
        if (map.containsKey("order_address")) orderAddress = map["order_address"] as? String ?: orderAddress
        if (map.containsKey("order_product")) orderProduct = map["order_product"] as? String ?: orderProduct
        if (map.containsKey("generated_flyer_path")) generatedFlyerPath = map["generated_flyer_path"] as? String ?: generatedFlyerPath
        if (map.containsKey("last_error")) lastError = map["last_error"] as? String ?: lastError
        if (map.containsKey("user_pref_short_text")) userPrefShortText = map["user_pref_short_text"] as? Boolean ?: userPrefShortText
        if (map.containsKey("user_pref_emojis")) userPrefEmojis = map["user_pref_emojis"] as? Boolean ?: userPrefEmojis
    }
    
    /**
     * Exportamos a mapa solo para agentes no refactorizados que lo necesiten.
     */
    fun toLegacyMap(): Map<String, Any> {
        val out = mutableMapOf<String, Any>()
        out["trace_id"] = traceId
        out["customer_tone"] = customerTone
        out["raw_intent_category"] = rawIntentCategory
        out["routed_nodes"] = routedNodes
        safeText?.let { out["safe_text"] = it }
        catalogContext?.let { out["catalog_context"] = it }
        policyContext?.let { out["policy_context"] = it }
        out["order_ready"] = orderReady
        orderName?.let { out["order_name"] = it }
        orderAddress?.let { out["order_address"] = it }
        orderProduct?.let { out["order_product"] = it }
        generatedFlyerPath?.let { out["generated_flyer_path"] = it }
        lastError?.let { out["last_error"] = it }
        out["user_pref_short_text"] = userPrefShortText
        out["user_pref_emojis"] = userPrefEmojis
        return out
    }
}

// 1. Contratos de Datos Seguros (Reemplazo de Map<String, Any>)
interface AgentPayload
interface AgentResponseData

// 2. Errores de Negocio Estrictos (Sin Throwables)
sealed interface SwarmError {
    data class InternalException(val reason: String) : SwarmError
    data class LlmTimeout(val ms: Long) : SwarmError
    data class InvalidSchema(val missingFields: List<String>) : SwarmError
    data class HumanInterventionRequired(val reason: String) : SwarmError
}

// 3. Tipado Genérico del Result (Mónada Either robusta para el DAG)
sealed interface SwarmResult<out D : AgentResponseData, out E : SwarmError> {
    data class Success<out D : AgentResponseData>(
        val data: D,
        val confidenceScore: Double
    ) : SwarmResult<D, Nothing>

    data class Failure<out E : SwarmError>(val error: E) : SwarmResult<Nothing, E>
    data class NeedsReview<out E : SwarmError>(val error: E) : SwarmResult<Nothing, E>

    // Función encadenable monádica
    inline fun <R> fold(
        onSuccess: (D, Double) -> R,
        onFailure: (E) -> R,
        onReview: (E) -> R
    ): R = when (this) {
        is Success -> onSuccess(data, confidenceScore)
        is Failure -> onFailure(error)
        is NeedsReview -> onReview(error)
    }
}

// 4. Nueva tarea genérica (Fuerte cohesión de Payload)
data class SwarmTask<out P : AgentPayload>(
    val messageId: String,
    val message: InboundMessage,
    val payload: P
)


// ============================================================================
// ⚠️ LEGACY BRIDGE (Se mantendrá hasta que todos los agentes sean migrados a Hilt/Genéricos)
// ============================================================================

data class LegacyAgentPayload(
    val metadata: Map<String, Any>? = null,
    val proposedAction: ActionIntent? = null
) : AgentPayload

@Deprecated("Usa SwarmTask<T> en nuevos agentes tipados")
data class AgentTask(
    val message: InboundMessage,
    val metadata: Map<String, Any>? = null,
    val proposedAction: ActionIntent? = null
)

@Deprecated("Migra los retornos a SwarmResult<D, E> usando contratos definidos")
sealed class AgentResult {
    data class Success(
        val confidenceScore: Double,
        val extractedData: Map<String, Any>? = null,
        val proposedAction: ActionIntent? = null
    ) : AgentResult()

    data class Failure(val errorReason: String) : AgentResult()

    data class Routed(
        val routedTo: List<String>,
        val extractedData: Map<String, Any>? = null
    ) : AgentResult()
    
    data class NeedsReview(val reason: String) : AgentResult()
}
