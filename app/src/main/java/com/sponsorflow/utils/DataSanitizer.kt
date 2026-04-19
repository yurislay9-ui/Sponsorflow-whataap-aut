package com.sponsorflow.utils

import android.util.Log
import org.json.JSONObject

/**
 * VECTOR 17 & 18 DEDICATED SOLVER.
 * Centraliza la limpieza de Strings y evita crashes de SQLite por JSON mal tipado ("$50").
 */
object DataSanitizer {

    private const val TAG = "NEXUS_Sanitizer"

    data class ParsedSender(val platform: String, val clientId: String)

    /**
     * V17: Centralización Lógica de Strings.
     * Si "ig: mariano_22" entra, siempre saldrá ("IG", "mariano_22").
     */
    fun normalizeSender(rawInput: String): ParsedSender {
        val parts = rawInput.split(":", limit = 2)
        val platform = if (parts.size > 1) parts[0].uppercase().trim() else "WA"
        val clientId = if (parts.size > 1) parts[1].trim() else rawInput.trim()
        return ParsedSender(platform, clientId)
    }

    /**
     * V18: Type-Safe JSON Extraction.
     * Si el LLM alucina y manda {"total": "$50 USD"} en lugar de {"total": 50.0},
     * esta función limpia todo carácter no numérico, evitando un SQLite TypeMismatchCrash.
     */
    fun extractSafeDouble(json: JSONObject, key: String, fallback: Double = 0.0): Double {
        return try {
            if (json.has(key)) {
                // Si el LLM lo mandó bien como Double:
                val rawVal = json.get(key)
                if (rawVal is Number) return rawVal.toDouble()
                
                // Si el LLM lo mandó como String ("$50.50"):
                val rawString = rawVal.toString()
                val cleanString = rawString.replace(Regex("[^0-9.]"), "")
                if (cleanString.isNotEmpty()) return cleanString.toDouble()
            }
            fallback
        } catch (e: Exception) {
            Log.w(TAG, "Fallo al sanitizar la llave JSON '$key'. Retornando fallback: $fallback")
            fallback
        }
    }
}
