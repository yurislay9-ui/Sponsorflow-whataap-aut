package com.sponsorflow.services

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log

/**
 * LA CÁMARA DE ESTASIS (Immortal Swarm Service)
 * 
 * Un Servicio en Primer Plano (Foreground Service) que ancla la aplicación a la memoria RAM.
 * Evita que el Garbage Collector (OOM Killer) o sistemas como MIUI/EMUI asesinen
 * a nuestros agentes que corren en Background.
 */
class ImmortalSwarmService : Service() {

    private val CHANNEL_ID = "Sponsorflow_Immortal_Channel"
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        Log.i("NEXUS_Immortal", "🛡️ Cámara de Estasis Activada: Servicio Inmortalizando el Enjambre.")
        createNotificationChannel()
        acquireWakeLock()
    }

    // Quitamos 'Ongoing' rígido y rebajamos prioridad para evitar escaneo agresivo de Samsung/Xiaomi.
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel() // Ensure channel is created before using it
        
        val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Sponsorflow Engine")
                .setContentText("Sincronización Inteligente.")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) 
                .setPriority(Notification.PRIORITY_LOW) // BAJADO de alerta a LOW (Pasivo)
                .setOngoing(false) // Permite deslizar si el SO insiste (menos sospechoso)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("Sponsorflow Engine")
                .setContentText("Sincronización Inteligente.")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setPriority(Notification.PRIORITY_LOW)
                .setOngoing(false)
                .build()
        }

        try {
            // Foreground service data sync es menos agresivo para OEMs que "Special Use"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1984, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(1984, notification)
            }
        } catch (e: Exception) {
            Log.e("NEXUS_Immortal", "Fallo elevando a Foreground: \${e.message}. Ejecutando Fallback.")
            startForeground(1984, notification)
        }

        // START_NOT_STICKY o START_STICKY dependiento de policy. 
        // Emplearemos START_STICKY porque aún necesitamos reanimación si hay purga normal,
        // pero quitamos el WakeLock agresivo en onCreate para evadir el OEM Killer
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // No queremos enlazarnos directamente (Bind), es fuego y olvido.
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        Log.w("NEXUS_Immortal", "💀 Peligro: El Servicio Inmortal ha sido Destruido.")
        // NOTA DE FUERZA BRUTA: En V4 podríamos lanzar un Broadcast a nosotros mismos aquí para reabrirnos.
    }

    /**
     * Moderación Táctica de CPU.
     * Ya NO usamos PARTIAL_WAKE_LOCK con timeouts estáticos masivos que enojan a Doze Mode.
     * Solo lo adquirimos si es estrictamente necesario, pero lo limitamos a 5 segundos 
     * (lo que dura despachar un agente en respuesta rápida).
     */
    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Sponsorflow::SwarmWakeLock"
            )
            // LÍMITE DE SEGURIDAD (SRE Guard Nivel 5): Nunca más de 12 segundos.
            wakeLock?.acquire(12 * 1000L) 
        } catch (e: Exception) {
            Log.e("NEXUS_Immortal", "No se pudo obtener el WakeLock: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e("NEXUS_Immortal", "Error liberando el WakeLock: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Sponsorflow Estasis",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Canal invisible para mantener el agente vivo."
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    companion object {
        /**
         * Puntero exacto para el Francotirador de Retargeting (Temporal Engagement Agent).
         * Usamos setExactAndAllowWhileIdle para programar misiones de rescate incluso en DOZE profundo.
         */
        fun scheduleExactMission(context: Context, timeInMillis: Long, payload: String, requestCode: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            // Creamos el misil a disparar a futuro (El Francotirador disparará esto)
            val intent = Intent(context, RetargetingReceiver::class.java).apply {
                putExtra("RETARGETING_PAYLOAD", payload)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Permiso exacto de calendario (Android 12+ lo exige explícitamente en Ajustes)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                )
                Log.i("NEXUS_Immortal", "⏰ Misión de francotirador programada exacto para: $timeInMillis")
            } else {
                // Legacy Android
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            }
        }
    }
}
