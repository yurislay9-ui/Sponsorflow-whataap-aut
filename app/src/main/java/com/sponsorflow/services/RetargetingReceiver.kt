package com.sponsorflow.services

import javax.inject.Inject

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sponsorflow.agents.SwarmManager
import com.sponsorflow.models.InboundMessage
import dagger.hilt.android.AndroidEntryPoint
/**
 * RECEPTOR DEL FRANCOTIRADOR DE RETARGETING
 * 
 * Cuando el AlarmManager de Android despierta a la hora exacta configurada,
 * dispara este Broadcast. Pasa el payload como un comando a nivel de Terminal
 * para que el SwarmManager lo procese sin restricciones.
 */
@AndroidEntryPoint
class RetargetingReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var swarmManager: SwarmManager

    override fun onReceive(context: Context, intent: Intent) {
        val payload = intent.getStringExtra("RETARGETING_PAYLOAD") ?: return
        
        Log.i("NEXUS_Retargeting", "🎯 [FRANCOTIRADOR ACTIVADO] Disparando arma: \$payload")
        
        // Convertimos el payload a Comando Nativo (Terminal)
        // Ejemplo de payload: publish --platform ig --caption "Hola"
        val cliCommand = "> publish --platform ig --caption \"\$payload\""
        
        val systemMessage = InboundMessage(
            context = context,
            sender = "SYSTEM_FRANCOTIRADOR",
            text = cliCommand,
            replyIntent = null,
            remoteInputKey = null
        )
        
        // Re-inyectamos el comando al Cortex principal
        swarmManager.onNewMessageReceived(systemMessage)
    }
}
