package com.sponsorflow.services

import javax.inject.Inject

import android.app.Notification
import android.content.Context
import android.content.ComponentName
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.sponsorflow.agents.SwarmManager
import com.sponsorflow.models.InboundMessage
import dagger.hilt.android.AndroidEntryPoint
/**
 * Agente de Lectura (Modo Invisible y OMNICANAL).
 * Código de Producción: Intercepta chats en WA, WA Business, IG y FB Messenger.
 */
@AndroidEntryPoint
class NotificationListener : NotificationListenerService() {
    
    @Inject
    lateinit var swarmManager: SwarmManager
    
    companion object {
        var isServiceRunning = false
            private set

        fun requestRebind(context: Context) {
            val componentName = ComponentName(context, NotificationListener::class.java)
            val pm = context.packageManager
            pm.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            pm.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            Log.i("Sponsorflow_Eye", "Toggle de componente realizado para solicitar Rebind a Android.")
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isServiceRunning = true
        Log.i("Sponsorflow_Eye", "Listener enlazado y monitoreando.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isServiceRunning = false
        Log.w("Sponsorflow_Eye", "Listener desenlazado por Android OS (Peligro de Doze Mode).")
    }
    
    // El listado Multi-Plataforma Omnicanal (Actualizado para Clones y Multicuentas)
    private val TARGET_APPS = listOf(
        "com.whatsapp",            // WhatsApp Normal
        "com.whatsapp.w4b",        // WhatsApp Business
        "com.instagram.android",   // Instagram Oficial
        "com.instagram.lite",      // Instagram Lite (Para la cuenta 2)
        "com.facebook.orca",       // FB Messenger Oficial
        "com.facebook.mlite",      // FB Messenger Lite
        "com.facebook.pages.app",  // Meta Business Suite (Páginas y Comentarios)
        "com.google.android.youtube" // YouTube (Comentarios de Shorts/Videos)
    )

    // Agentes clonadores más populares (Para que Sponsorflow escuche versiones clonadas)
    private val CLONE_PREFIXES = listOf("com.lbe.parallel", "com.excelliance", "com.multi.space")

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        
        val packageName = sbn.packageName
        
        // Verificamos si es una app objetivo o un clon de una app objetivo
        val isTargetApp = TARGET_APPS.contains(packageName) || CLONE_PREFIXES.any { packageName.contains(it) }
        
        if (isTargetApp) {
            val extras = sbn.notification.extras
            
            // Ignorar notificaciones de resumen de Android (Group Summaries)
            val isGroupSummary = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false)
            if (isGroupSummary) return

            val sender = extras.getString(Notification.EXTRA_TITLE) ?: return
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return
            
            // Si el texto incluye palabras clave de interacción (likes/reacciones), las ignoramos.
            if (text.contains("le gusta tu") || text.contains("reaccionó a") || text.contains("liked your")) return
            
            // Tratamiento según la plataforma (Para identificar de dónde viene en el CRM)
            val platform = when {
                packageName.contains("instagram") -> "IG"
                packageName.contains("facebook") || packageName.contains("orca") -> "FB"
                packageName.contains("youtube") -> "YT"
                else -> "WA"
            }
            
            var replyPendingIntent: android.app.PendingIntent? = null
            var remoteInputKey: String? = null

            val actions = sbn.notification.actions
            if (actions != null) {
                for (action in actions) {
                    val inputs = action.remoteInputs
                    if (inputs != null) {
                        for (input in inputs) {
                            if (input.resultKey != null) {
                                remoteInputKey = input.resultKey
                                replyPendingIntent = action.actionIntent
                                break
                            }
                        }
                    }
                    if (remoteInputKey != null) break
                }
            }

            if (replyPendingIntent == null || remoteInputKey == null) {
                Log.w("Sponsorflow_Eye", "Sin Inline-Reply para [$platform:$sender] - Puede ser una notificación estática de comentario.")
                // Nota: Meta Business Suite sí permite responder comentarios anidados desde las notificaciones si está actualizado.
                return
            }

            Log.i("Sponsorflow_Eye", "Captura Exitosa [$platform] -> Sender: $sender")
            
            // Empaquetamos el modelo universal de entrada y lo entregamos al Cuartel General
            val inboundMessage = InboundMessage(
                context = this,
                sender = "\$platform:\$sender",
                text = text,
                replyIntent = replyPendingIntent,
                remoteInputKey = remoteInputKey
            )
            swarmManager.onNewMessageReceived(inboundMessage)
        }
    }
}
