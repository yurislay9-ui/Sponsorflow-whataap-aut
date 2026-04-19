package com.sponsorflow.services

import android.app.Notification
import android.content.Context
import android.content.ComponentName
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.sponsorflow.core.Orchestrator

/**
 * Agente de Lectura (Modo Invisible y OMNICANAL).
 * Código de Producción: Intercepta chats en WA, WA Business, IG y FB Messenger.
 */
class NotificationListener : NotificationListenerService() {
    
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
    
    // El listado Multi-Plataforma Omnicanal
    private val TARGET_APPS = listOf(
        "com.whatsapp",            // WhatsApp Normal
        "com.whatsapp.w4b",        // WhatsApp Business
        "com.instagram.android",   // Instagram Direct
        "com.facebook.orca"        // FB Messenger
    )
    
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        
        val packageName = sbn.packageName
        if (TARGET_APPS.contains(packageName)) {
            val extras = sbn.notification.extras
            
            // Ignorar notificaciones de resumen de Android (Group Summaries)
            val isGroupSummary = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false)
            if (isGroupSummary) return

            val sender = extras.getString(Notification.EXTRA_TITLE) ?: return
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return
            
            // Tratamiento según la plataforma (Para identificar de dónde viene en el CRM)
            val platform = when(packageName) {
                "com.instagram.android" -> "IG"
                "com.facebook.orca" -> "FB"
                else -> "WA"
            }
            
            val intent = sbn.notification.contentIntent
            
            Log.i("Sponsorflow_Eye", "Captura Exitosa [$platform] -> Sender: $sender")
            
            // Enviamos el identificador compuesto por plataforma + remitente (ej: "IG: Cliente")
            Orchestrator.onNewMessageReceived(this, "$platform:$sender", text, intent)
        }
    }
}
