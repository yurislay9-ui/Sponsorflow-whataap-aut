package com.sponsorflow

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
import com.sponsorflow.data.SponsorflowDatabase
import com.sponsorflow.security.SecurityVault
import com.sponsorflow.ui.CatalogScreen
import com.sponsorflow.ui.CatalogViewModel
import com.sponsorflow.ui.CatalogViewModelFactory
import androidx.compose.ui.text.AnnotatedString

/**
 * Pantalla Principal de Sponsorflow "ENTERPRISE".
 * Sistema de 4 Pestañas Integrado: Panel SaaS, Inventario, CRM/Métricas y Logística.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainAppHost()
            }
        }
    }
}

@Composable
fun MainAppHost() {
    var selectedTab by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val db = remember { SponsorflowDatabase.getDatabase(context) }
    val catalogViewModel: CatalogViewModel = viewModel(factory = CatalogViewModelFactory(db.catalogDao()))
    
    // States for CRM Analytics
    val totalLeads by db.businessDao().getTotalInteractionsFlow().collectAsState(initial = 0)
    val totalRevenue by db.businessDao().getTotalRevenueFlow().collectAsState(initial = 0.0)
    val ordersList by db.businessDao().getPendingOrdersFlow().collectAsState(initial = emptyList())

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Text("🛡️") }, label = { Text("App") })
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Text("📦") }, label = { Text("Stock") })
                NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Text("📉") }, label = { Text("Métricas") })
                NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3 }, icon = { Text("🚚") }, label = { Text("Pedidos") })
                NavigationBarItem(selected = selectedTab == 4, onClick = { selectedTab = 4 }, icon = { Text("🏭") }, label = { Text("Almacén") })
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> SponsorflowDashboard()
                1 -> CatalogScreen(viewModel = catalogViewModel)
                2 -> MetricsScreen(totalLeads, totalRevenue ?: 0.0)
                3 -> OrdersScreen(ordersList)
                4 -> ProviderConfigScreen()
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
            "🛍️ VALE DE VENTA\n\n- Nombre: {nombre}\n- Dirección: {direccion}\n- Producto: {producto}\n- Pago: {tipo_pago}") ?: "") 
    }

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Text("Configuración de Almacén", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Define la plantilla del vale de venta que la IA rellenará para que se lo envíes a tu proveedor.", color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp))
        
        OutlinedTextField(
            value = templateText,
            onValueChange = { templateText = it },
            label = { Text("Modelo de Vale (Usa corchetes como {nombre})") },
            modifier = Modifier.fillMaxWidth().height(250.dp),
            singleLine = false
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { 
                sharedPrefs.edit().putString("provider_template", templateText).apply()
                Toast.makeText(context, "Plantilla de Proveedor Actualizada", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar Formato")
        }
        
        Spacer(modifier = Modifier.height(30.dp))
        
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6))) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("ℹ️ Instrucciones:", fontWeight = FontWeight.Bold)
                Text("- La IA leerá la plantilla de arriba.")
                Text("- Cuando el cliente confirme la compra, la IA rellenará los datos y lo guardará en la pestaña 'Pedidos'.")
                Text("- Así solo tendrás que darle a 'Copiar' y enviárselo a tu chofer o distribuidor.")
            }
        }
    }
}

@Composable
fun MetricsScreen(leads: Int, revenue: Double) {
    val context = LocalContext.current
    
    Column(modifier = Modifier.padding(20.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Panel de Analíticas CRM", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF6366F1))) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Ganancias Generadas por IA", color = Color.White, fontWeight = FontWeight.Bold)
                Text("$revenue CUP", fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF59E0B))) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Leads (Clientes Contactados)", color = Color.White)
                Text("$leads Personas", fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        
        Button(
            onClick = { 
                if (leads > 0) {
                    Toast.makeText(context, "Lanzando campaña de goteo a $leads leads. La IA escribirá progresivamente...", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "No hay leads guardados todavía.", Toast.LENGTH_SHORT).show()
                }
            }, 
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Lanzar Promo a $leads Leads Fríos")
        }
    }
}

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
                items(orders) { order ->
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
fun SponsorflowDashboard() {
    val context = LocalContext.current
    var isNotifEnabled by remember { mutableStateOf(checkNotificationPermission(context)) }
    var isAccessEnabled by remember { mutableStateOf(checkAccessibilityPermission(context)) }
    
    val vault = remember { SecurityVault(context) }
    var remainingDays by remember { mutableStateOf(vault.getRemainingDays()) }
    var activationCode by remember { mutableStateOf("") }
    
    val deviceID = vault.getDeviceUUID()

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF3F4F6)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            
            // CABECERA
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Sponsorflow", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                    Text("Auto-Dropshipping AI", color = Color.Gray)
                }
                
                // Métrica de Salud
                val sysColor = if (isNotifEnabled && isAccessEnabled && remainingDays > 0) Color(0xFF10B981) else Color(0xFFEF4444)
                Box(modifier = Modifier.background(sysColor, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(if (sysColor == Color(0xFF10B981)) "ONLINE" else "OFFLINE", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            
            Divider()

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
                            if(vault.activateLicense(activationCode)) {
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
            
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    isNotifEnabled = checkNotificationPermission(context)
                    isAccessEnabled = checkAccessibilityPermission(context)
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
