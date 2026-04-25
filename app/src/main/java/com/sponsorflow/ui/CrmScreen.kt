package com.sponsorflow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sponsorflow.data.CustomerEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CrmScreen(viewModel: CrmViewModel) {
    val customers by viewModel.customers.collectAsState()
    
    // Filtros Kanban
    val nuevos = customers.filter { it.funnelStage == "NUEVO" }
    val negociando = customers.filter { it.funnelStage == "NEGOCIANDO" }
    val casiCierre = customers.filter { it.funnelStage == "CASI_CIERRE" }
    val compro = customers.filter { it.funnelStage == "COMPRO" }

    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF3F4F6))) {
        Text(
            text = "Embudo Kanban (Ventas Inteligentes)",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )
        
        // Scroll Horizontal para las Columnas de Trello
        Row(
            modifier = Modifier.fillMaxSize().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            KanbanColumn("Nuevos", Color(0xFF3B82F6), nuevos) { selectedCustomer = it; showDialog = true }
            KanbanColumn("Negociando", Color(0xFFF59E0B), negociando) { selectedCustomer = it; showDialog = true }
            KanbanColumn("Casi Cierre", Color(0xFF8B5CF6), casiCierre) { selectedCustomer = it; showDialog = true }
            KanbanColumn("Cerrados (Compró)", Color(0xFF10B981), compro) { selectedCustomer = it; showDialog = true }
        }
    }

    selectedCustomer?.let { customer ->
        if (showDialog) {
            CustomerOverlayConfig(
                customer = customer,
                onDismiss = { showDialog = false; selectedCustomer = null },
                onSave = { id, profile, tags, stage ->
                    viewModel.updateCustomer(id, profile, tags, stage)
                    showDialog = false
                    selectedCustomer = null
                }
            )
        }
    }
}

@Composable
fun KanbanColumn(
    title: String, 
    headerColor: Color, 
    customers: List<CustomerEntity>, 
    onCustomerClick: (CustomerEntity) -> Unit
) {
    Column(
        modifier = Modifier.width(300.dp).fillMaxHeight().padding(bottom = 16.dp).background(Color.White, RoundedCornerShape(12.dp))
    ) {
        Surface(color = headerColor, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)) {
            Text(
                text = "$title (${customers.size})", 
                color = Color.White, 
                fontWeight = FontWeight.Bold, 
                modifier = Modifier.padding(12.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(items = customers, key = { it.senderId }) { c ->
                KanbanCard(c) { onCustomerClick(c) }
            }
        }
    }
}

@Composable
fun KanbanCard(customer: CustomerEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(customer.senderId, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Text(customer.platform, color = Color(0xFF6366F1), fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
            }
            Text(customer.perfilCognitivo.take(60) + "...", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            if (customer.unconsolidatedHistory.isNotBlank()) {
                Text("⏳ Chats Pendientes", color = Color(0xFFF59E0B), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerOverlayConfig(
    customer: CustomerEntity,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var profileText by remember { mutableStateOf(customer.perfilCognitivo) }
    var tagsText by remember { mutableStateOf(customer.tags) }
    var selectedStage by remember { mutableStateOf(customer.funnelStage) }
    val stages = listOf("NUEVO", "NEGOCIANDO", "CASI_CIERRE", "COMPRO")
    var expanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(20.dp).fillMaxSize()) {
                Text("Intervención Humana", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Edición directa del cerebro lógico y pipeline.", color = Color.Gray, fontSize = 14.sp)
                
                Spacer(Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = customer.senderId,
                    onValueChange = {},
                    label = { Text("Identidad (Fijada)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false
                )
                
                Spacer(Modifier.height(12.dp))

                Text("Kanban Stage (Mover Tarjeta):", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Box {
                    Button(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))) {
                        Text(selectedStage, color = Color.White)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Cambiar", tint = Color.White)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        stages.forEach { stage ->
                            DropdownMenuItem(
                                text = { Text(stage) },
                                onClick = {
                                    selectedStage = stage
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = profileText,
                    onValueChange = { profileText = it },
                    label = { Text("Perfil Cognitivo (IA Override)") },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    singleLine = false
                )
                
                Spacer(Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = tagsText,
                    onValueChange = { tagsText = it },
                    label = { Text("Tags JSON [\"VIP\", \"Regateador\"]") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(Modifier.height(12.dp))

                if (customer.unconsolidatedHistory.isNotBlank()) {
                    Text("Búfer Chat Oculto (Hoy):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Surface(color = Color(0xFFF3F4F6), modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 4.dp), shape = RoundedCornerShape(8.dp)) {
                        Text(customer.unconsolidatedHistory, fontSize = 12.sp, modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState()))
                    }
                } else {
                    Text("Búfer temporal vacío.", color = Color.Gray, fontSize = 12.sp)
                }

                Spacer(Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onSave(customer.senderId, profileText, tagsText, selectedStage) }) {
                        Text("Actualizar y Guardar")
                    }
                }
            }
        }
    }
}
