package com.sponsorflow.agents.kairos

import javax.inject.Inject

import android.util.Log
import com.sponsorflow.agents.TypedSponsorflowAgent
import com.sponsorflow.models.AgentPayload
import com.sponsorflow.models.AgentResponseData
import com.sponsorflow.models.AgentTask
import com.sponsorflow.models.SquadType
import com.sponsorflow.models.SwarmError
import com.sponsorflow.models.SwarmResult
import com.sponsorflow.models.SwarmTask

data class PrivacyPayload(val originalText: String) : AgentPayload

data class PrivacyData(
    val safeText: String,
    val piiIntercepted: Boolean
) : AgentResponseData

/**
 * [ACTUALIZACIÓN 2026 - MIGRADO A TIPADO ESTRICTO E INYECCIÓN]
 * PRIVACY AGENT (Escuadrón KAIROS - El Censor)
 * 
 * Pilar 4: Privacidad Absoluta "Zero-Data-Exfiltration".
 * Actúa como Middleware. Analiza los textos crudos y enmascara PII 
 * (Personally Identifiable Information) como tarjetas de crédito o correos,
 * ANTES de que el enjambre o la base de datos los procesen.
 */
class PrivacyAgent @Inject constructor() : TypedSponsorflowAgent<PrivacyPayload, PrivacyData>() {
    private const val TAG = "NEXUS_PrivacyAgent"
    
    override val agentName: String = "PrivacyAgent"
    override val squadron: SquadType = SquadType.KAIROS
    override val capabilities: List<String> = listOf("pii_masking", "security_filter")

    // Patrones de Seguridad (Regex)
    private val emailRegex = "[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}".toRegex()
    private val creditCardRegex = "\\b(?:\\d[ -]*?){13,16}\\b".toRegex()
    // Añadiremos más capas según el nivel de seguridad requerido por el usuario (Passwords, SSN, etc)

    override fun mapLegacyTaskToPayload(task: AgentTask): PrivacyPayload {
        return PrivacyPayload(originalText = task.message.text)
    }

    override suspend fun executeTypedInternal(task: SwarmTask<PrivacyPayload>): SwarmResult<PrivacyData, SwarmError> {
        val originalText = task.payload.originalText
        
        if (originalText.isBlank()) {
            return SwarmResult.Success(
                confidenceScore = 1.0, 
                data = PrivacyData(safeText = "", piiIntercepted = false)
            )
        }

        try {
            var sanitizedText = originalText
            var piiFound = false

            // 1. Detección y Enmascaramiento de Tarjetas de Crédito
            if (creditCardRegex.containsMatchIn(sanitizedText)) {
                sanitizedText = sanitizedText.replace(creditCardRegex, "[TARJETA_CENSURADA]")
                piiFound = true
            }

            // 2. Detección y Enmascaramiento de Correos Electrónicos
            if (emailRegex.containsMatchIn(sanitizedText)) {
                sanitizedText = sanitizedText.replace(emailRegex, "[CORREO_CENSURADO]")
                piiFound = true
            }

            if (piiFound) {
                Log.w(TAG, "🛡️ PII Detectado e interceptado. Datos sensibles eliminados del stream.")
            }

            return SwarmResult.Success(
                confidenceScore = 1.0,
                data = PrivacyData(
                    safeText = sanitizedText,
                    piiIntercepted = piiFound
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error en regex del PrivacyAgent. Usando Fail-Safe.", e)
            // FAIL-SAFE: En caso de error de Regex catastrófico
            return SwarmResult.Failure(SwarmError.InternalException("Error en regex del PrivacyAgent catastrófico: \${e.message}"))
        }
    }
}
