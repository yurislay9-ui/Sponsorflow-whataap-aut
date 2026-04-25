package com.sponsorflow.utils

import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import java.security.MessageDigest

/**
 * MOTOR DE IMPORTACIÓN SEGURO (Anti-Fuzz / Checksum Validation)
 * Analiza y valida archivos de configuración externos (JSON) antes de
 * inyectarlos a la base de datos o preferencias locales.
 */
object ConfigurationImporter {
    private const val TAG = "NEXUS_ConfigImport"
    private const val CURRENT_VERSION_CODE = "v4.0" // Solo aceptamos backups generados por la v4.0

    class ImportException(message: String) : Exception(message)

    /**
     * Intenta parsear un JSON externo (Simulando Restore desde archivo).
     * Ejecuta validaciones de Integridad y Versionado.
     */
    fun parseConfigFileSafely(jsonRawString: String, expectedChecksum: String): ImportResult {
        try {
            // 1. Zero-Trust Checksum Verification (Integridad)
            val computedChecksum = generateSHA256(jsonRawString)
            if (computedChecksum != expectedChecksum && expectedChecksum.isNotEmpty()) {
                Log.e(TAG, "FALLO DE CHECKSUM. Archivo posiblemente alterado por terceros en el disco.")
                throw ImportException("Checksum fallido. El archivo está corrupto o fue manipulado.")
            }

            // 2. Parseo JSON bajo try-catch blindado
            val jsonObj = JSONObject(jsonRawString)

            // 3. Control estricto de versionamiento (Colisión de Versiones)
            val fileVersion = jsonObj.optString("export_version", "v1.0")
            if (fileVersion != CURRENT_VERSION_CODE) {
                Log.e(TAG, "Colisión de Versiones: Intentaron inyectar \$fileVersion en $CURRENT_VERSION_CODE")
                throw ImportException("Incompatibilidad de versión. Se requiere v4.0. No hay script de migración activo.")
            }

            // 4. Sanitización de Spintax / Reglas de Empresa (Inyección XSS/Scripts Maliciosos)
            // Extraeremos de forma segura sin permitir NULOS usando los Opts
            val rawSpintax = jsonObj.optString("spintax_template", "")
            val cleanSpintax = sanitizeSpintaxAgainstInjection(rawSpintax)

            // 5. Validación de Tipado Estricto (Fuzz Testing Defense)
            // Extraemos un supuesto booleano, si el hacker mandó un String Inmenso de 10MB buscando un OOM,
            // optBoolean intentará parsearlo, si no es estrictamente true/false, devolverá fallback false tranquilamente.
            val strictBooleanMode = jsonObj.optBoolean("stealth_mode_active", false)
            
            // Limitador de Memoria Fuerte para Prevención de OOM
            val rawRulesText = jsonObj.optString("company_rules", "")
            val safeRules = if(rawRulesText.length > 5000) rawRulesText.substring(0, 5000) else rawRulesText

            Log.i(TAG, "Archivo JSON validado con éxito.")
            return ImportResult.Success(
                template = cleanSpintax,
                rules = safeRules,
                stealth = strictBooleanMode
            )

        } catch (e: JSONException) {
            Log.e(TAG, "JSON Malformado: \${e.message}")
            return ImportResult.Failure("El archivo no es un JSON válido o está incompleto.")
        } catch (e: ImportException) {
            return ImportResult.Failure(e.message ?: "Error desconocido durante importación.")
        } catch (e: OutOfMemoryError) {
             Log.e(TAG, "Ataque OOM Detectado! Archivo demasiado grande.")
             return ImportResult.Failure("El archivo excede los límites de memoria locales.")
        }
    }

    /**
     * Limpia la sintaxis de plantillas.
     * Elimina scripts o tags HTML/JS invisibles que intenten comprometer la interfaz de usuario.
     */
    private fun sanitizeSpintaxAgainstInjection(input: String): String {
        var clean = input.replace("<script[\\\\s\\\\S]*?>[\\\\s\\\\S]*?<\\\\/script>".toRegex(RegexOption.IGNORE_CASE), "[SCRIPT REMOVIDO]")
        clean = clean.replace("javascript:", "[INYECCION REMOVIDA]")
        clean = clean.replace("onload=", "[ONLOAD ATTACK REMOVIDO]")
        return clean.trim()
    }

    private fun generateSHA256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = input.toByteArray()
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}

sealed class ImportResult {
    data class Success(val template: String, val rules: String, val stealth: Boolean) : ImportResult()
    data class Failure(val reason: String) : ImportResult()
}
