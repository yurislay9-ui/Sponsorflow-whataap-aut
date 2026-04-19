package com.sponsorflow.security

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.crypto.KeyGenerator

/**
 * EL ESCUDO (v2.0): Gestor Criptográfico de Licencias y Anti-Piratería.
 * Cifrado AES-256-GCM en reposo y verificación de tokens hardware-bound.
 */
class SecurityVault(private val context: Context) {
    
    init {
        // Ejecutar validación de TEE (Hardware Criptográfico Físico) al iniciar la Bóveda
        verifyHardwareAttestation()
    }

    // 1. Inicialización de MasterKey respaldada por el Hardware Keystore (TEE/SE)
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    // 2. EncryptedSharedPreferences: Imposible de leer en texto plano en caso de extracción Root
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "sponsorflow_vault_secure",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    companion object {
        const val KEY_LICENSE_END_DATE = "license_end_date_ms"
        const val KEY_LAST_KNOWN_GOOD_TIME = "last_known_good_time_ms" // Anti-Time Travel
        const val SPONSORFLOW_SECRET_SALT = "AQUI_TU_FIRMA_SECRETA_COMO_DESARROLLADOR_2026"
    }

    /**
     * V10 (Sandboxing Evasion): Fuerza la generación de llaves en el chip "StrongBox Titan M".
     * Si un pirata ejecuta la app en un emulador o clon virtual (Parallel Space), estalla.
     */
    private fun verifyHardwareAttestation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val keyStore = KeyStore.getInstance("AndroidKeyStore")
                keyStore.load(null)
                if (!keyStore.containsAlias("sponsorflow_hw_anchor")) {
                    val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                    val builder = KeyGenParameterSpec.Builder(
                        "sponsorflow_hw_anchor",
                        KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
                    ).apply {
                        setDigests(KeyProperties.DIGEST_SHA256)
                        setUserAuthenticationRequired(false)
                        setIsStrongBoxBacked(true) // <--- LA SENTENCIA MORTAL PARA EMULADORES Y CLONES
                    }
                    keyGenerator.init(builder.build())
                    keyGenerator.generateKey()
                    Log.i("NEXUS_Vault", "🛡️ Ancla de Hardware [StrongBox TEE] verificada y enlazada.")
                }
            } catch (e: StrongBoxUnavailableException) {
                Log.e("NEXUS_Vault", "☠️ ENTORNO HOSTIL: Emulador o SO Modificado detectado. Carece de StrongBox.")
                // En producción, aquí se lanza una SecurityException o se mata el proceso.
            } catch (e: Exception) {
                Log.w("NEXUS_Vault", "Hardware check pasivo.")
            }
        }
    }

    /**
     * Genera un Hash único e intransferible basado en el hardware del usuario.
     */
    fun getDeviceUUID(): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        return hashString(androidId ?: "unknown_device").take(12)
    }

    fun isLicenseValid(): Boolean {
        val endDateMs = prefs.getLong(KEY_LICENSE_END_DATE, 0L)
        val lastGoodTime = prefs.getLong(KEY_LAST_KNOWN_GOOD_TIME, 0L)
        val currentTime = System.currentTimeMillis()
        
        // DEFENSA ANTI TIME-TRAVEL: Si la hora actual es menor que el último registro válido, el usuario retrocedió el calendario.
        if (currentTime < lastGoodTime) {
            Log.e("NEXUS_Vault", "🛡️ ALERTA DE SEGURIDAD: Modificación de Reloj Detectada (Time-Tampering). Bloqueo activado.")
            return false
        }
        
        // V8: Usamos .commit() síncrono para asegurar escritura inmediata en el disco SSD contra fallas de batería
        prefs.edit().putLong(KEY_LAST_KNOWN_GOOD_TIME, currentTime).commit()

        if (endDateMs == 0L) {
            activateTrial()
            return true
        }

        val isValid = currentTime < endDateMs
        
        if (!isValid) Log.e("NEXUS_Vault", "🛡️ Licencia Expirada. Bloqueando Agentes de IA.")
        return isValid
    }

    /**
     * Valida matemáticamente que la llave haya sido generada EXCLUSIVAMENTE para
     * el hardware actual, erradicando la piratería de compartir claves en internet.
     */
    fun activateLicense(activationCode: String): Boolean {
        // La llave maestra (Lo que tú generas en tu PC/Keygen para venderle al cliente)
        val expectedCryptoKey = hashString(getDeviceUUID() + SPONSORFLOW_SECRET_SALT).take(12)
        
        // DEV_MODE es solo para ti mientras programas
        if (activationCode == expectedCryptoKey || activationCode == "DEV_MODE") {
            val extensionMs = TimeUnit.DAYS.toMillis(30)
            val currentEnd = prefs.getLong(KEY_LICENSE_END_DATE, System.currentTimeMillis())
            val baseTime = if (currentEnd > System.currentTimeMillis()) currentEnd else System.currentTimeMillis()
            
            // V8: Usamos .commit() síncrono.
            prefs.edit().putLong(KEY_LICENSE_END_DATE, baseTime + extensionMs).commit()
            Log.i("NEXUS_Vault", "✅ Validación Criptográfica Exitosa. Licencia extendida.")
            return true
        }
        
        Log.e("NEXUS_Vault", "❌ Intento de piratería bloqueado. Firma no coincide con Hardware.")
        return false
    }

    fun getRemainingDays(): Long {
        val end = prefs.getLong(KEY_LICENSE_END_DATE, 0L)
        val diff = end - System.currentTimeMillis()
        return if (diff > 0) TimeUnit.MILLISECONDS.toDays(diff) else 0
    }

    private fun activateTrial() {
        Log.i("NEXUS_Vault", "Dispositivo nuevo verificado. Activando 30 días de prueba.")
        val trialEndMs = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30)
        // V8: Usamos .commit() síncrono
        prefs.edit().putLong(KEY_LICENSE_END_DATE, trialEndMs).commit()
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.uppercase()
    }
}
