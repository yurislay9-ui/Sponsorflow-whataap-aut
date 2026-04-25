package com.sponsorflow

import javax.inject.Inject

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.*
import com.sponsorflow.data.SponsorflowDatabase
import com.sponsorflow.security.SecurityVault
import com.sponsorflow.agents.kairos.KairosDaemonWorker
import com.sponsorflow.agents.SwarmManager
import dagger.hilt.android.AndroidEntryPoint
import com.sponsorflow.ui.CampaignsScreen
import com.sponsorflow.ui.CatalogScreen
import com.sponsorflow.ui.CatalogViewModel
import com.sponsorflow.ui.CatalogViewModelFactory
import com.sponsorflow.ui.CrmScreen
import com.sponsorflow.ui.CrmViewModel
import com.sponsorflow.ui.CrmViewModelFactory
import androidx.compose.ui.text.AnnotatedString
import android.os.PowerManager
import android.net.Uri
import java.util.concurrent.TimeUnit

/**
 * Pantalla Principal de Sponsorflow "ENTERPRISE".
 * Sistema de 4 Pestañas Integrado: Panel SaaS, Inventario, CRM/Métricas y Logística.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var swarmManager: SwarmManager
    
    @Inject
    lateinit var antiSpamGuardian: com.sponsorflow.core.AntiSpamGuardian

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // CORTAFUEGOS OS-LEVEL (Low Memory Killer Evasion)
        registerComponentCallbacks(object : ComponentCallbacks2 {
            override fun onTrimMemory(level: Int) {
                if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL || level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
                    Log.w("NEXUS_Memory", "OS bajo presión extrema. Purgando diccionarios de memoria volátil y llamando Garbage Collector.")
                    antiSpamGuardian.clearMemory()
                    System.gc()
                }
            }
            override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {}
            override fun onLowMemory() {
                antiSpamGuardian.clearMemory()
                System.gc()
            }
        })

        // VECTOR 21: Inicialización del Agente Nocturno (CRON)
        setupNightWorker()

        // PROTECCIÓN V4.0: Forzamos el arranque del Motor Inmortal al abrir la app.
        try {
            val serviceIntent = Intent(this, com.sponsorflow.services.ImmortalSwarmService::class.java)
            androidx.core.content.ContextCompat.startForegroundService(this, serviceIntent)
            Log.i("NEXUS_Main", "🛡️ Cámara de Estasis Inmortal invocada desde Splash/Boot.")
        } catch (e: Exception) {
            Log.e("NEXUS_Main", "Fallo al iniciar Motor Inmortal en Boot: ${e.message}")
        }

        setContent {
            MaterialTheme {
                MainAppHost(swarmManager)
            }
        }
    }

    private fun setupNightWorker() {
        val constraints = Constraints.Builder()
            .setRequiresDeviceIdle(true) // Espera a que nadie use el teléfono
            .setRequiresCharging(true)   // Cuida la batería
            .setRequiredNetworkType(NetworkType.CONNECTED) // Qwen local no usa red, pero evitamos overhead sin wifi
            .build()

        val nightJob = PeriodicWorkRequestBuilder<KairosDaemonWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
            
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SponsorflowKairosDaemon",
            ExistingPeriodicWorkPolicy.KEEP,
            nightJob
        )
    }
}

@Composable
fun MainAppHost(swarmManager: SwarmManager) {
    var selectedTab by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val db = remember { SponsorflowDatabase.getDatabase(context) }
    val catalogViewModel: CatalogViewModel = viewModel(factory = CatalogViewModelFactory(db.catalogDao()))
    val crmViewModel: CrmViewModel = viewModel(factory = CrmViewModelFactory(db.businessDao()))
    
    // States for analytics
    val totalLeads by db.businessDao().getTotalInteractionsFlow().collectAsState(initial = 0)
    val totalRevenue by db.businessDao().getTotalRevenueFlow().collectAsState(initial = 0.0)
    val ordersList by db.businessDao().getPendingOrdersFlow().collectAsState(initial = emptyList())

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Text("🛡️") }, label = { Text("App") })
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Text("👥") }, label = { Text("CRM") })
                NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Text("🚀") }, label = { Text("Mktg") })
                NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3 }, icon = { Text("📦") }, label = { Text("Stock") })
                NavigationBarItem(selected = selectedTab == 4, onClick = { selectedTab = 4 }, icon = { Text("🚚") }, label = { Text("Pedidos") })
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> {
                    // Integrando Configuración dentro de la pestaña general para no perder el espacio.
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) { SponsorflowDashboard(db.businessDao(), swarmManager) }
                    }
                }
                1 -> CrmScreen(viewModel = crmViewModel)
                2 -> CampaignsScreen()
                3 -> CatalogScreen(viewModel = catalogViewModel)
                4 -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) { OrdersScreen(ordersList) }
                        Divider(thickness = 4.dp, color = Color(0xFFE5E7EB))
                        Box(modifier = Modifier.height(350.dp)) { ProviderConfigScreen() }
                    }
                }
            }
        }
    }
}

@Composable
fun ProviderConfigScreen() {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("sponsorflow_settings", Context.MODE_PRIVATE) }
    
    // Leer el valor guardado o inicializar con un ejemplo predeterminado
    var templateText by remember { 
        mutableStateOf(sharedPrefs.getString("provider_template", 
            "🛍️ DATOS DE ENTREGA (PARA PROVEEDOR)\n\n- Cliente: {nombre}\n- Envío/Recogida: {direccion}\n- Producto: {producto}") ?: "") 
    }
    
    var companyKnowledge by remember { 
        mutableStateOf(sharedPrefs.getString("company_knowledge", "REGLA DE ORO: Eres un asistente recolector. Solo recopilas datos del cliente (Nombre, Modelo, y si quiere Domicilio o Recogida en Local). NUNCA mandes links de pago ni pidas tarjetas, el pago SIEMPRE es en efectivo contra-entrega o transferencia acordada después.") ?: "") 
    }

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Text("Cerebro y Logística", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Configura cómo la IA recolecta los datos de entrega y las reglas del negocio.", color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp))
        
        OutlinedTextField(
            value = companyKnowledge,
            onValueChange = { companyKnowledge = it },
            label = { Text("Reglas del Negocio (Cerebro de Empresa)") },
            placeholder = { Text("Ej: Abrimos de 9am a 6pm. Solo envío en la Habana. No reembolsos.") },
            modifier = Modifier.fillMaxWidth().height(140.dp),
            singleLine = false
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = templateText,
            onValueChange = { templateText = it },
            label = { Text("Modelo de Vale (Usa corchetes como {nombre})") },
            modifier = Modifier.fillMaxWidth().height(140.dp),
            singleLine = false
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { 
                sharedPrefs.edit().putString("provider_template", templateText).putString("company_knowledge", companyKnowledge).apply()
                Toast.makeText(context, "Configuración Ciber-Logística Guardada", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar Formato y Reglas")
        }
    }
}

// Dead code metrics removed

@Composable
fun OrdersScreen(orders: List<com.sponsorflow.data.OrderEntity>) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Text("Órdenes y Aprobación", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Pedidos planificados por la IA, esperando tu visto bueno", color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp))
        
        if (orders.isEmpty()) {
            Text("No hay pedidos pendientes de aprobación.", color = Color.Gray)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items = orders, key = { it.id }) { order ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(order.clientName, fontWeight = FontWeight.Bold)
                                Text("${order.totalAmount} CUP", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                            }
                            Text("📍 ${order.address}", color = Color.Gray, fontSize = 12.sp)
                            Spacer(Modifier.height(8.dp))
                            
                            // Visualizar el vale formateado de fábrica
                            if (order.providerTicket.isNotBlank()) {
                                Surface(color = Color(0xFFEFF6FF), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("📦 Vale de Almacén", fontSize = 12.sp, color = Color(0xFF1D4ED8), fontWeight = FontWeight.Bold)
                                        Text(order.providerTicket, fontSize = 14.sp, color = Color.DarkGray, modifier = Modifier.padding(top = 4.dp))
                                        Button(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(order.providerTicket))
                                                Toast.makeText(context, "Vale copiado al portapapeles", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.padding(top = 8.dp).height(32.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("Copiar para Enviar", fontSize = 12.sp)
                                        }
                                    }
                                }
                            } else {
                                Text("Producto: ${order.productDetails}", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SponsorflowDashboard(businessDao: com.sponsorflow.data.BusinessDao, swarmManager: SwarmManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isNotifEnabled by remember { mutableStateOf(checkNotificationPermission(context)) }
    var isAccessEnabled by remember { mutableStateOf(checkAccessibilityPermission(context)) }
    
    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
    var isIgnoringBattery by remember { mutableStateOf(powerManager.isIgnoringBatteryOptimizations(context.packageName)) }
    
    val vault = remember { SecurityVault(context) }
    var remainingDays by remember { mutableStateOf(vault.getRemainingDays()) }
    var activationCode by remember { mutableStateOf("") }
    
    val deviceID = vault.getDeviceUUID()
    
    var isAdminModeUnlocked by remember { mutableStateOf(false) } // Estado de la Sala Oculta de Administración

    // Query para saber si hay clientes en cola de consolidación nocturna
    val customersInQueue by businessDao.getPendingOrdersFlow().collectAsState(initial = emptyList()) // just dummy trigger for now
    var forcedConsolidationTriggered by remember { mutableStateOf(false) }
    
    val sharedPrefs = remember { context.getSharedPreferences("sponsorflow_settings", Context.MODE_PRIVATE) }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF3F4F6)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            
            val radarEvents by swarmManager.radarLogs.collectAsState()

            // CABECERA
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Sponsorflow", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                    Text("Motor Híbrido RAG-M (Fénix)", color = Color.Gray)
                }
                
                // Métrica de Salud
                val sysColor = if (isNotifEnabled && isAccessEnabled && remainingDays > 0) Color(0xFF10B981) else Color(0xFFEF4444)
                Box(modifier = Modifier.background(sysColor, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(if (sysColor == Color(0xFF10B981)) "ONLINE" else "OFFLINE", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            
            Divider()

            // OBSERVABILIDAD: Radar del Swarm en Tiempo Real
            Card(
                modifier = Modifier.fillMaxWidth().height(160.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))
            ) {
                Column(modifier = Modifier.padding(12.dp).fillMaxSize()) {
                    Text("Radar Swarm (Tiempo Real)", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    if (radarEvents.isEmpty()) {
                        Text("Esperando eventos del clúster neuronál...", color = Color.Gray, fontSize = 12.sp)
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(items = radarEvents) { event ->
                                val timeFormat = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
                                val timeString = timeFormat.format(java.util.Date(event.timestamp))
                                val statusColor = when(event.status) {
                                    "FAILED", "FATAL_ERROR" -> Color(0xFFEF4444)
                                    "COMPLETED", "SWARM_FINISHED" -> Color(0xFF3B82F6)
                                    else -> Color.LightGray
                                }
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                    Text("[$timeString]", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.width(70.dp))
                                    Text("[${event.traceId}]", color = Color(0xFF6366F1), fontSize = 10.sp, modifier = Modifier.width(65.dp))
                                    Text("${event.nodeName}: ", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(event.status, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // CONSOLA CENTRAL DEL AGENTE
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Consola del Centinela", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Motor Determinístico (<2ms)", color = Color.White.copy(alpha=0.7f), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = { 
                            forcedConsolidationTriggered = true
                            
                            // 1. FORZAR KAIROS DAEMON
                            val oneTimeWork = OneTimeWorkRequestBuilder<com.sponsorflow.agents.kairos.KairosDaemonWorker>().build()
                            WorkManager.getInstance(context).enqueue(oneTimeWork)
                            
                            // 2. ENCENDER MOTOR INMORTAL (Foreground Service)
                            val serviceIntent = android.content.Intent(context, com.sponsorflow.services.ImmortalSwarmService::class.java)
                            androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
                            
                            Toast.makeText(context, "🛡️ Motor Inmortal Activado. Escudos arriba.", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Forzar Consolidación Nocturna", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // PANEL DE LICENCIA (SaaS)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (remainingDays > 5) Color(0xFF1E3A8A) else Color(0xFFB91C1C))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Licencia Activa", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("$remainingDays Días Restantes", fontSize = 24.sp, color = Color.White)
                    Text("ID Dispositivo: $deviceID", color = Color.LightGray, fontSize = 12.sp)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = activationCode,
                        onValueChange = { activationCode = it },
                        label = { Text("Ingresar Clave", color = Color.White) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Button(
                        onClick = { 
                            if (vault.isMasterAdmin(activationCode)) {
                                isAdminModeUnlocked = true
                                activationCode = ""
                                Toast.makeText(context, "SALA ADMINISTRACIÓN DESBLOQUEADA", Toast.LENGTH_LONG).show()
                            } else if(vault.activateLicense(activationCode)) {
                                remainingDays = vault.getRemainingDays()
                                activationCode = ""
                                Toast.makeText(context, "Sponsorflow Recargado!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Clave Inválida", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Text("Activar Llave", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // SALA DE ADMINISTRACIÓN OCULTA (Solo visible para Ti)
            if (isAdminModeUnlocked) {
                AdminConsole(vault = vault, onLicenseGranted = {
                    remainingDays = vault.getRemainingDays()
                })
            }

            // PERMISOS DE HARDWARE
            Text("Conexiones del Enjambre:", fontWeight = FontWeight.Bold)
            PermissionCard(
                title = "Ojo Fantasma (WhatsApp, IG, FB)",
                isEnabled = isNotifEnabled,
                onGrantClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
            )
            PermissionCard(
                title = "Mano Fantasma (Accesibilidad)",
                isEnabled = isAccessEnabled,
                onGrantClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            )
            PermissionCard(
                title = "Energía Inmortal (Anti-Doze Kernel)",
                isEnabled = isIgnoringBattery,
                onGrantClick = { 
                    val intent = Intent().apply {
                        action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            )
            
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    isNotifEnabled = checkNotificationPermission(context)
                    isAccessEnabled = checkAccessibilityPermission(context)
                    isIgnoringBattery = powerManager.isIgnoringBatteryOptimizations(context.packageName)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Refrescar Sensores")
            }
        }
    }
}

@Composable
fun PermissionCard(title: String, isEnabled: Boolean, onGrantClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Color.Black)
                Text(if (isEnabled) "✅" else "❌")
            }
            if (!isEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onGrantClick, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF374151))) {
                    Text("Reparar Conexión")
                }
            }
        }
    }
}

fun checkNotificationPermission(context: Context): Boolean {
    val listeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return listeners?.contains(context.packageName) == true
}

fun checkAccessibilityPermission(context: Context): Boolean {
    val services = Settings.Secure.getString(context.contentResolver, "enabled_accessibility_services")
    return services?.contains(context.packageName) == true
}

@Composable
fun AdminConsole(vault: SecurityVault, onLicenseGranted: () -> Unit) {
    var clientUUID by remember { mutableStateOf("") }
    var generatedKey by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("👑 SALA DE ADMINISTRACIÓN 👑", color = Color(0xFFFFD700), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            Text("Prohibido el paso. Acceso encriptado exclusivo para el propietario.", color = Color.Gray, fontSize = 12.sp)
            
            Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 12.dp))
            
            // 1. Acceso Vitalicio (Para el dueño)
            Text("Modo Desarrollador", color = Color.White, fontWeight = FontWeight.Bold)
            Button(
                onClick = { 
                    vault.grantAdminUnlimitedLicense()
                    onLicenseGranted()
                    Toast.makeText(context, "Privilegio de Inmortalidad Concedido (9999 Días)", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B))
            ) {
                Text("Inyectar Acceso Ilimitado (Eterno)", color = Color.White, fontWeight = FontWeight.Bold)
            }
            
            Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 12.dp))
            
            // 2. Modulo Keygen (Generador de Licencias para Clientes)
            Text("Fábrica de Licencias para Clientes (Keygen)", color = Color.White, fontWeight = FontWeight.Bold)
            Text("Para vender Sponsorflow, el cliente te dará su 'ID Dispositivo'. Ingrésalo aquí y envíale la clave matemática.", color = Color.LightGray, fontSize = 12.sp)
            
            OutlinedTextField(
                value = clientUUID,
                onValueChange = { clientUUID = it },
                label = { Text("ID Dispositivo del Cliente", color = Color.White) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, 
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFFFD700),
                    unfocusedBorderColor = Color.Gray
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            
            Button(
                onClick = {
                    if (clientUUID.isNotBlank()) {
                        generatedKey = vault.generateClientKey(clientUUID)
                    } else {
                        Toast.makeText(context, "Ingresa un UUID válido", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4B5563))
            ) {
                Text("Cifrar Llave Comercial", color = Color.White)
            }
            
            if (generatedKey.isNotEmpty()) {
                Surface(color = Color(0xFF1F2937), modifier = Modifier.fillMaxWidth().padding(top = 12.dp), shape = RoundedCornerShape(8.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("LLAVE DE LICENCIA CLIENTE:", color = Color.Gray, fontSize = 10.sp)
                        Text(generatedKey, color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(generatedKey))
                                Toast.makeText(context, "Llave Copiada. Lista para vender.", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF374151))
                        ) {
                            Text("Copiar Llave Generada", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
