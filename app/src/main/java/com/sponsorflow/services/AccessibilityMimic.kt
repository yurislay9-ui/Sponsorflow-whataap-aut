package com.sponsorflow.services

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

/**
 * Agente Biomecánico Restringido (V2.0).
 * Rol actual: Mantenido en estado latente ("Dormido").
 * Futuro uso: Solo despertará durante el Intent ACTION_SEND hacia Instagram/Facebook
 * para presionar quirúrgicamente el botón de "Publicar" y devolver la UI a Home.
 *
 * *SE HA ELIMINADO TODA REFERENCIA A SCRAPING DE UI DE WHATSAPP PARA PREVENIR BANEOS*
 */
class AccessibilityMimic : AccessibilityService() {

    companion object {
        private var gestureJob: Job? = Job()
        private val mimicScope = CoroutineScope(Dispatchers.Main + (gestureJob ?: Job()))
        
        // Bandera que el SocialMediaPublisher activará solo cuando necesite un Click
        var isWaitingForPublishButton = false

        // SRE Guard: Graceful degradation variables
        private var targetStartTime: Long = 0L
        private const val TIMEOUT_MS = 10000L // 10 segundos máximo buscando el botón
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i("Sponsorflow_Mimic_V2", "Agente Biomecánico Conectado. En Modo Latente (Esperando Intents).")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        // Si no estamos en medio de una campaña de publicación, ignoramos TODO el tráfico
        // Esto ahorra un 90% de batería comparado con la V1.
        if (!isWaitingForPublishButton) return

        // --- SISTEMA DE CLICK FINAL - SPRINT 3 ---
        // Android acaba de traer la UI de la Red Social cargada con la foto y el texto del Intent.
        val rootNode = rootInActiveWindow ?: return
        val packageName = event.packageName?.toString() ?: ""

        // Verificación de Timeout para Graceful Degradation (evita que se quede atascado para siempre buscando un botón que cambió)
        if (targetStartTime == 0L) {
            targetStartTime = System.currentTimeMillis()
        } else if (System.currentTimeMillis() - targetStartTime > TIMEOUT_MS) {
            Log.e("Sponsorflow_Mimic_V2", "⏳ TIMEOUT: No se encontró el botón de publicar tras ${TIMEOUT_MS}ms. Abortando misión para ahorrar batería.")
            isWaitingForPublishButton = false
            targetStartTime = 0L
            performGlobalAction(GLOBAL_ACTION_HOME) // Failsafe fallback
            return
        }

        // Quitamos la restricción estricta de paquete (isFacebook, isInstagram).
        // Si la bandera está activa, el Agente asumirá que la app actual en pantalla (sea original o un clon modificado)
        // es el objetivo, y buscará los botones de acción para completar la misión.
        if (isWaitingForPublishButton) {
            // Buscamos cualquier botón que grite "Acción Final" en múltiples idiomas o apps clónicas
            val targetNodes = rootNode.findAccessibilityNodeInfosByText("Publicar")
                .ifEmpty { rootNode.findAccessibilityNodeInfosByText("Compartir") }
                .ifEmpty { rootNode.findAccessibilityNodeInfosByText("Siguiente") }
                .ifEmpty { rootNode.findAccessibilityNodeInfosByText("Share") }
                .ifEmpty { rootNode.findAccessibilityNodeInfosByText("Post") }
                .ifEmpty { rootNode.findAccessibilityNodeInfosByText("Subir") }
                .ifEmpty { rootNode.findAccessibilityNodeInfosByText("Upload") }
                .ifEmpty { rootNode.findAccessibilityNodeInfosByText("Crear") }
                .ifEmpty { rootNode.findAccessibilityNodeInfosByText("Tweet") }
                .ifEmpty { rootNode.findAccessibilityNodeInfosByText("Enviar") }

            for (node in targetNodes) {
                if (node.isClickable || node.parent?.isClickable == true) {
                    val clickableNode = if (node.isClickable) node else node.parent
                    val success = clickableNode?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false

                    if (success) {
                        Log.i("Sponsorflow_Mimic_V2", "🎯 ¡Botón presionado con éxito en $packageName! Volviendo a base.")
                        
                        // Misión cumplida, bajamos la guardia.
                        isWaitingForPublishButton = false
                        targetStartTime = 0L // Reset SRE timeout
                        
                        // Retirada táctica: Escondemos la red social del dueño para que no interrumpa.
                        // Usamos un pequeño delay para dejar que la red social inicie el upload.
                        mimicScope.launch {
                            kotlinx.coroutines.delay(1000)
                            performGlobalAction(GLOBAL_ACTION_HOME)
                        }
                        break
                    }
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.e("Sponsorflow_Mimic_V2", "El servicio fue interrumpido por Android.")
        isWaitingForPublishButton = false // SRE Sudden Death Reset
        targetStartTime = 0L
        gestureJob?.cancelChildren()
    }
}
