package com.sponsorflow.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sponsorflow.agents.action.PublisherAgent
import com.sponsorflow.models.AgentTask
import com.sponsorflow.models.InboundMessage
import com.sponsorflow.models.ActionIntent
import kotlinx.coroutines.launch
import com.sponsorflow.utils.ClickDebouncer
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignsScreen() {
    val context = LocalContext.current
    var campaignText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedPlatform by remember { mutableStateOf("instagram") } 
    var scheduledTime by remember { mutableStateOf(0L) }
    
    val calendar = remember { Calendar.getInstance() }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val coroutineScope = rememberCoroutineScope()

    // Launcher fotográfico seguro nativo
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> selectedImageUri = uri }

    val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        scheduledTime = calendar.timeInMillis
    }

    val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month)
        calendar.set(Calendar.DAY_OF_MONTH, day)
        TimePickerDialog(context, timeSetListener, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Laboratorio de Lanzamiento", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Text("El Gestor Proactivo. Agenda un post y Sponsorflow abrirá la app y hará click automáticamente.", color = Color.Gray, modifier = Modifier.padding(bottom = 20.dp))

        Text("1. Motor Multi-Cuentas (Keyword del Clon)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("Escribe la palabra clave de tu app. El motor buscará TODAS las apps instaladas que contengan esa palabra (incluyendo tus clones y dual apps) y publicará en todas secuencialmente.", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
        
        OutlinedTextField(
            value = selectedPlatform,
            onValueChange = { selectedPlatform = it },
            label = { Text("Palabra Clave (Ej: facebook, instagram, clon)") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF6366F1))
        )
        
        // Fila 1: Oficiales Normales
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PlatformButton("Todas las Instagram", selectedPlatform == "instagram") { selectedPlatform = "instagram" }
            PlatformButton("Todas las Facebook", selectedPlatform == "facebook") { selectedPlatform = "facebook" }
        }
        
        // Fila 2: Versiones Lite
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PlatformButton("Todas las YouTube", selectedPlatform == "youtube") { selectedPlatform = "youtube" }
            PlatformButton("Todas las TikTok", selectedPlatform == "tiktok") { selectedPlatform = "tiktok" }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. El Copy
        Text("2. Copy (Generado por IA o Manual)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        OutlinedTextField(
            value = campaignText,
            onValueChange = { campaignText = it },
            modifier = Modifier.fillMaxWidth().height(150.dp).padding(top = 8.dp),
            placeholder = { Text("Ej: ¡Nueva colección de Zapatos!...") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF6366F1),
                unfocusedBorderColor = Color.LightGray
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Activo Multimedia
        Text("3. Multimedia", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Card(
            modifier = Modifier.fillMaxWidth().height(80.dp).padding(top = 8.dp).clickable { imagePickerLauncher.launch("image/*") },
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = if (selectedImageUri != null) "✅ Imagen adjuntada exitosamente" else "📸 Tocar para seleccionar foto de Galería",
                    color = if (selectedImageUri != null) Color(0xFF10B981) else Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Reloj de Lanzamiento
        Text("4. Hora Cero (AlarmManager CRON)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        OutlinedButton(
            onClick = {
                DatePickerDialog(
                    context, 
                    dateSetListener, 
                    calendar.get(Calendar.YEAR), 
                    calendar.get(Calendar.MONTH), 
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Icon(Icons.Default.DateRange, contentDescription = null, tint = Color(0xFF6366F1))
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (scheduledTime == 0L) "Seleccionar Fecha y Hora Exacta" else dateFormatter.format(Date(scheduledTime)), color = Color.Black)
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Botón de Ejecución
        Button(
            onClick = {
                ClickDebouncer.withDebounce {
                    if (campaignText.isBlank() || scheduledTime == 0L) {
                        Toast.makeText(context, "Falta texto u hora de publicación", Toast.LENGTH_SHORT).show()
                        return@withDebounce
                    }
                    
                    // Si la hora programada es en el futuro, usamos el francotirador.
                    // Si es inmediato, lo invocamos ahora mismo.
                    if (scheduledTime > System.currentTimeMillis()) {
                        com.sponsorflow.services.ImmortalSwarmService.scheduleExactMission(
                            context = context,
                            timeInMillis = scheduledTime,
                            payload = campaignText,
                            requestCode = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
                        )
                        Toast.makeText(context, "Misión Francotirador Programada a las ${dateFormatter.format(Date(scheduledTime))}.", Toast.LENGTH_LONG).show()
                    } else {
                        // Invocamos al Publisher Agent directo (Acción inmediata para el Swarm V5)
                        coroutineScope.launch {
                            val dummyMessage = InboundMessage(
                                context = context,
                                sender = "UI_DASHBOARD",
                                text = "MANUAL_POST",
                                replyIntent = null,
                                remoteInputKey = null
                            )
                            val task = AgentTask(
                                message = dummyMessage,
                                proposedAction = ActionIntent(type = "PUBLISH_POST", payload = campaignText)
                            )
                            com.sponsorflow.agents.action.PublisherAgent.executeTask(task)
                        }
                        
                        Toast.makeText(context, "Misil disparado por el PublisherAgent.", Toast.LENGTH_LONG).show()
                    }
                    
                    // Limpieza post setup
                    campaignText = ""
                    scheduledTime = 0L
                    selectedImageUri = null
                }
            },
            modifier = Modifier.fillMaxWidth().height(55.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Armar Inyector de Post (Programar)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RowScope.PlatformButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.weight(1f).clickable { onClick() },
        color = if (isSelected) Color(0xFFEFF6FF) else Color.White,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFF3B82F6) else Color.LightGray)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(12.dp)) {
            Text(label, color = if (isSelected) Color(0xFF1D4ED8) else Color.Gray, fontWeight = FontWeight.Bold)
        }
    }
}
