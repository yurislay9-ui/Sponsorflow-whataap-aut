package com.sponsorflow.services

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.sponsorflow.core.Orchestrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Agente de Acción Biomecánica.
 * Código de Producción: Atraviesa el DOM UI nativo de Android buscando inputs.
 * INCLUYE VECTOR 9: Seguro de Interrupción de Usuario (Ghost Input Blocker).
 */
class AccessibilityMimic : AccessibilityService() {

    companion object {
        // Variable concurrente para señalar toma de control físico
        val isHumanIntervening = AtomicBoolean(false)
        private var gestureJob: Job? = Job()
        private val mimicScope = CoroutineScope(Dispatchers.Main + gestureJob!!)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i("Sponsorflow_Mimic", "Agente Mimic Conectado y Listo.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        // VECTOR 9 DEFENSA: Detección de dedos humanos
        val humanInterventionEvents = listOf(
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        )
        
        if (humanInterventionEvents.contains(event.eventType)) {
            Log.w("NEXUS_Accessibility", "⚠️ Intervención Física Detectada. Cancelando control IA.")
            isHumanIntervening.set(true)
            // Cortamos de manera inmediata la corrutina de gestos
            gestureJob?.cancelChildren()
            
            // Permitimos que la persona tome el control por 10 segundos antes de considerar volver a intervenir
            mimicScope.launch {
                delay(10000)
                isHumanIntervening.set(false)
            }
        }

        // Si el humano está escribiendo, nos callamos.
        if (isHumanIntervening.get()) return

        // Si no hay orden del Orquestador, nos quedamos dormidos
        val textToType = Orchestrator.pendingAutoReply ?: return
        
        // Android cambió de ventana
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            
            val rootNode = rootInActiveWindow ?: return
            
            // Lanzamos ejecución asíncrona pero amarrada al gestureJob para poder ser cancelada
            mimicScope.launch {
                try {
                    val injected = injectTextAndSend(rootNode, textToType)
                    if (injected) {
                        Orchestrator.clearPendingReply()
                        delay(1000) // Delay largo para asegurar el envío de red visual
                        performGlobalAction(GLOBAL_ACTION_BACK)
                        delay(200)
                        performGlobalAction(GLOBAL_ACTION_HOME)
                        Log.i("Sponsorflow_Mimic", "Misión Ejecutada y Confirmada. Regresando a las sombras.")
                    } else {
                         Log.e("Sponsorflow_Mimic", "Latencia o Conflicto en UI detectado. Falló la verificación de envío.")
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    Log.w("NEXUS_Accessibility", "💥 Abortado por choque biométrico.")
                }
            }
        }
    }

    /**
     * V15 DEFENSA DE DENSIDAD ESPACIAL:
     * Siempre usa "NodeInfo" (Id, Etiquetas) en vez de coordenadas "X,Y". 
     * Eso garantiza que funcionará igual de bien en Pantalla Dividida, Foldables o tablets.
     */
    private suspend fun injectTextAndSend(root: AccessibilityNodeInfo, text: String): Boolean {
        val inputNodes = root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/entry")
            .ifEmpty { findNodesByClass(root, "android.widget.EditText") }

        if (inputNodes.isNotEmpty()) {
            val inputBox = inputNodes[0]
            val arguments = android.os.Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) }
            val inputSuccess = inputBox.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            
            if (inputSuccess) {
                // Simula que la persona está escribiendo. Si el humano toca la pantalla aquí, esto lanzará CancellationException
                val typingDelay = Math.min((text.length * 50).toLong(), 2000L)
                delay(typingDelay)
                
                val sendNodes = root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send")
                if (sendNodes.isNotEmpty()) {
                    sendNodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    
                    // VECTOR 12 DEFENSA: Sincronización Fantasma (Red Lenta)
                    // No podemos asumir que fue "enviado". Debemos VERIFICAR que la caja de texto se limpió,
                    // lo cual Android solo hace cuando la Payload entró al bus de red.
                    delay(300) // Da tiempo al motor de UI
                    return inputBox.text.isNullOrEmpty() || inputBox.text.toString() == "Mensaje"
                }
            }
        }
        return false
    }

    private fun findNodesByClass(rootNode: AccessibilityNodeInfo?, className: String): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        if (rootNode == null) return result
        if (rootNode.className?.toString()?.contains(className, ignoreCase = true) == true) {
            result.add(rootNode)
        }
        for (i in 0 until rootNode.childCount) {
            result.addAll(findNodesByClass(rootNode.getChild(i), className))
        }
        return result
    }

    override fun onInterrupt() {
        Log.e("Sponsorflow_Mimic", "El servicio fue interrumpido por Android.")
        gestureJob?.cancelChildren()
    }
}
