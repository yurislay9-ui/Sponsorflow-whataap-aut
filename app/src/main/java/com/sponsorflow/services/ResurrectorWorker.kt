package com.sponsorflow.services

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sponsorflow.security.SecurityVault

/**
 * EL DESFIBRILADOR (SRE Vector 14: Resurrección de Servicios).
 * Si el Doze Mode mata al Ojo Omnicanal (Notification Listener), 
 * este Worker se encarga de reanimarlo periódicamente de las cenizas.
 */
class ResurrectorWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val vault = SecurityVault(applicationContext)
        if (!vault.isLicenseValid()) {
            return Result.failure() // No revivir si no hay licencia
        }

        Log.i("NEXUS_Resurrector", "⚡ Pulso de Vida WorkManager detectado...")

        // Usamos un Service Checker nativo para ver si la app tiene permiso y si el Listener sigue corriendo.
        val isActive = NotificationListener.isServiceRunning

        if (!isActive) {
            Log.w("NEXUS_Resurrector", "⚠️ Sistema Doze asesinó los sentidos. Forzando re-conexión al NotificationManager de Android.")
            
            // Toggle Component Hack para obligar a Android a revivir el NotificationListenerService:
            NotificationListener.requestRebind(applicationContext)
        } else {
            Log.i("NEXUS_Resurrector", "✅ Ojos y Manos estables.")
        }

        return Result.success()
    }
}
