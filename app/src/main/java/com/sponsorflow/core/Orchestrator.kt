package com.sponsorflow.core

import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.os.StatFs
import android.util.Log
import com.sponsorflow.data.CustomerEntity
import com.sponsorflow.data.OrderEntity
import com.sponsorflow.data.SponsorflowDatabase
import com.sponsorflow.security.SecurityVault
import com.sponsorflow.services.MessageQueueManager
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.File

// Objeto de paso Inbound
data class InboundMessage(
    val context: Context,
    val sender: String,
    val text: String,
    val replyIntent: PendingIntent?
)

/**
 * EL ORQUESTADOR (Singleton) - BLINDADO CONTRA CAOS.
 * Integra Channel Buffer, Debouncers y Mutex Neural para protección OOM.
 */
object Orchestrator {
    private const val TAG = "NEXUS_Orchestrator"
    
    // Cola de entrada para asimilar Tormentas DDoS (Max 50 mensajes apilados, descarta excedentes)
    private val inboundChannel = Channel<InboundMessage>(
        capacity = 50, 
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    
    // MUTEX NEURAL: Protege la RAM de 4GB aislando la inferencia JNI. NUNCA se cargará Llama y Whisper juntos.
    private val neuralMutex = Mutex()
    
    // Mapa rápido para descartar picos de alertas Android redundantes (Debounce)
    private val recentHashes = mutableMapOf<Int, Long>()

    var pendingAutoReply: String? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        // Arrancar el Obrero de Entrada Único
        startInboundWorker()
    }

    /**
     * Nivel SUPERFICIAL: Solo recibe notificaciones. No procesa inteligencia.
     */
    fun onNewMessageReceived(context: Context, sender: String, text: String, replyIntent: PendingIntent?) {
        if (text.isBlank() || text.contains("nuevos mensajes")) return
        
        // DEBUNCER: Ignora el mismo mensaje enviado al mismo segundo (Bug típico de notificaciones agrupadas Android)
        val msgHash = (sender + text).hashCode()
        val now = System.currentTimeMillis()
        if (now - (recentHashes[msgHash] ?: 0L) < 2000L) return
        recentHashes[msgHash] = now

        // Metemos el mensaje al embudo seguro
        val wasSent = inboundChannel.trySend(InboundMessage(context, sender, text, replyIntent)).isSuccess
        if (!wasSent) Log.w(TAG, "Sobrecarga extrema. Mensaje de $sender ignorado.")
    }

    /**
     * VECTOR 13: Defensa de Almacenamiento Crítico (Disk Full Blocker)
     */
    private fun preventStorageExhaustion(context: Context) {
        try {
            val stat = StatFs(context.cacheDir.path)
            val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
            val megabytesAvailable = bytesAvailable / (1024 * 1024)

            if (megabytesAvailable < 150) { // Menos de 150MB libres
                Log.w(TAG, "🧹 CACHÉ CRÍITICA: El disco está lleno. Purgando audios TTS y caché virtual de Sponsorflow...")
                context.cacheDir.deleteRecursively()
                val ttsDir = File(context.filesDir, "tts_audio")
                if(ttsDir.exists()) ttsDir.deleteRecursively()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error limpiando almacenamiento: ${e.message}")
        }
    }

    /**
     * Nivel PROFUNDO: Trabajador secuencial que extrae del Canal Inbound uno por uno,
     * garantizando que el Hardware (CPU/RAM) nunca colapse por multihilo agresivo.
     */
    private fun startInboundWorker() {
        scope.launch {
            for (msg in inboundChannel) {
                try {
                    preventStorageExhaustion(msg.context)
                    processMessageSafely(msg)
                } catch (e: Exception) {
                    Log.e(TAG, "Error fatal en procesamiento aisalado: ${e.message}")
                }
            }
        }
    }

    private suspend fun processMessageSafely(msg: InboundMessage) {
        // V19 (Memory Leaks): Jamás guardar ni usar referencias de UI BaseContexts en hilos largos. 
        // Obligamos al orquestador a amarrarse PURAMENTE al Context del Componente Hardware.
        val globalContext = msg.context.applicationContext
        
        val vault = SecurityVault(globalContext)
        if (!vault.isLicenseValid()) return
        if (StealthEngine.isSleepingTime()) return

        StealthEngine.emulateHumanReading(msg.text.length)

        val db = SponsorflowDatabase.getDatabase(globalContext)
        val businessDao = db.businessDao()
        
        // V17 (Logic Collision): Usar el Use Case centralizado para uniformizar remitentes extraídos por Accesibility y NotificationListener.
        val normalizedSender = com.sponsorflow.utils.DataSanitizer.normalizeSender(msg.sender)
        val platform = normalizedSender.platform
        val rawSender = normalizedSender.clientId
        
        businessDao.saveCustomerIfNotExists(CustomerEntity(senderId = rawSender, platform = platform))

        var processedText = msg.text
        var fullResponse: String = ""

        // === ZONA CERRADA DE EXPANSIÓN NEURAL (PROTECCIÓN OOM) ===
        // Aquí adentro, el hilo se apropia de todos los recursos C++. Ningun otro mensaje puede entrar hasta que termine.
        neuralMutex.withLock {
            Log.d(TAG, "🔒 Mutex Neural Adquirido por [$rawSender]")
            
            // 1. Sentidos Periféricos
            if (msg.text.contains("📷 Foto") || msg.text.contains("imagen adjunta")) {
                val fakeUri = Uri.parse("content://fake_image_path")
                processedText = "Foto dice: " + VisionEngine.extractTextFromImage(globalContext, fakeUri)
            } else if (msg.text.contains("🎤 Mensaje de voz") || msg.text.contains("audio")) {
                val cachedAudioPath = "/storage/emulated/0/WhatsApp/Media/WhatsApp Voice Notes/fake.ogg"
                processedText = "Audio dice: " + VoiceEngine.transcribeAudio(cachedAudioPath)
            }

            // 2. Extracción SQL
            val relevantProducts = db.catalogDao().searchRelevantProducts(processedText)
            val catalogContextString = if (relevantProducts.isNotEmpty()) {
                relevantProducts.joinToString("\n") { "- ${it.name}: \$${it.sellPrice} (Desc: ${it.description})" }
            } else "No hay encaje perfecto, ofrece catálogo genérico."

            // 3. Obtener plantilla manual del proveedor
            val sharedPrefs = globalContext.getSharedPreferences("sponsorflow_settings", Context.MODE_PRIVATE)
            val templateAlmacen = sharedPrefs.getString("provider_template", "Nombre: {nombre}\nProducto: {producto}\nDireccion: {direccion}") ?: ""
            
            // 4. Inferencia Central Llama
            fullResponse = LlamaEngine.thinkAndReply(rawSender, processedText, catalogContextString, templateAlmacen)
            
            Log.d(TAG, "🔓 Liberando Mutex Neural")
        } // FIN ZONA CERRADA

        // Extracción Logística (CRM Validation Guard)
        var cleanResponse = fullResponse
        if (fullResponse.contains("JSON_ORDER:")) {
            try {
                val regex = "JSON_ORDER:\\s*\\{([^}]+)\\}".toRegex()
                val matchResult = regex.find(fullResponse)
                
                if (matchResult != null) {
                    val jsonStr = "{" + matchResult.groupValues[1] + "}"
                    val orderJson = JSONObject(jsonStr)
                    
                    val totalVenta = com.sponsorflow.utils.DataSanitizer.extractSafeDouble(orderJson, "total", 0.0)
                    
                    if (totalVenta <= 0.0 || orderJson.optString("producto", "").isBlank()) {
                        Log.e(TAG, "🚨 PROMPT INJECTION O JSON INVALIDO: Usuario intentó inyectar orden anómala. Transacción abortada.")
                    } else {
                        val clientName = orderJson.optString("nombre", rawSender)
                        val product = orderJson.optString("producto", "General")
                        val generatedTicket = orderJson.optString("vale", "Vale no generado por la IA")
                        val newOrder = OrderEntity(
                            clientName = clientName,
                            address = orderJson.optString("direccion", "Sin dirección"),
                            productDetails = product,
                            totalAmount = totalVenta,
                            providerTicket = generatedTicket
                        )
                        businessDao.saveOrder(newOrder) // WAL Mode nos asegura que no hay Deadlocks
                        Log.i(TAG, "📦 ¡NUEVA ORDEN VALIDADA CIBERNÉTICAMENTE Y EXTRAÍDA!")
                        
                        // NOTIFICAR AL DUEÑO DE LA APP (Usuario Humano)
                        // Enviamos una alerta visual al teléfono del dueño indicando que debe dar el Visto Final y procesar cobro.
                        try {
                            val notifManager = globalContext.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                            val channelId = "sponsorflow_orders"
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                val channel = android.app.NotificationChannel(channelId, "Nuevos Pedidos IA", android.app.NotificationManager.IMPORTANCE_HIGH)
                                notifManager.createNotificationChannel(channel)
                            }
                            
                            val notifBuilder = androidx.core.app.NotificationCompat.Builder(globalContext, channelId)
                                .setSmallIcon(android.R.drawable.ic_dialog_info)
                                .setContentTitle("💰 Posible Venta: $clientName")
                                .setContentText("Quiere: $product ($totalVenta CUP). ¡Entra a la App para dar el visto final y cobrar!")
                                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                                .setAutoCancel(true)
                                
                            notifManager.notify(newOrder.hashCode(), notifBuilder.build())
                        } catch (e: Exception) {
                            Log.e(TAG, "No se pudo lanzar notificación visual al dueño: \${e.message}")
                        }
                    }
                }
                cleanResponse = fullResponse.substring(0, fullResponse.indexOf("JSON_ORDER:")).trim()
            } catch(e: Exception) {}
        }

        // Emitir a la cola reguladora (Throttling Outbound)
        if (msg.replyIntent != null) {
            MessageQueueManager.enqueueReply(rawSender, cleanResponse, msg.replyIntent)
        }
    }
    
    fun clearPendingReply() {
        pendingAutoReply = null
    }
}
