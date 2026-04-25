package com.sponsorflow.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sponsorflow.utils.DataSanitizer
import android.widget.Toast
import com.sponsorflow.utils.ClickDebouncer
import androidx.compose.ui.platform.LocalContext
import com.sponsorflow.data.ProductEntity

@Composable
fun CatalogScreen(viewModel: CatalogViewModel) {
    val products by viewModel.products.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF10B981)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Añadir Producto", tint = Color.White)
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp).fillMaxSize()) {
            Text("Sponsorflow Inventario", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Entrena la memoria de tu Agente IA", color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp))
            
            if (products.isEmpty()) {
                Text("No hay productos. El Agente IA no tiene qué vender.", color = Color.Red)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items = products, key = { it.id }) { product ->
                        ProductCard(product)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddProductDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, desc, cost, sell ->
                viewModel.addProduct(name, desc, cost, sell)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ProductCard(product: ProductEntity) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(product.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(product.description, color = Color.DarkGray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Costo: ${product.costPrice} CUP", color = Color.Gray, fontSize = 12.sp)
                Text("Venta a: ${product.sellPrice} CUP", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AddProductDialog(onDismiss: () -> Unit, onConfirm: (String, String, Double, Double) -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var costPrice by remember { mutableStateOf("") }
    var sellPrice by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Producto AI") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre (Ej. Zapatillas)") }, singleLine = true)
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Info (Ej. Color negro, tallas...)") })
                OutlinedTextField(value = costPrice, onValueChange = { costPrice = it }, label = { Text("Costo (en CUP)") }, singleLine = true)
                OutlinedTextField(value = sellPrice, onValueChange = { sellPrice = it }, label = { Text("Precio de Venta (en CUP)") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = {
                ClickDebouncer.withDebounce {
                    // Validación Estricta Anti-Crash (Edge Cases)
                    val safeName = DataSanitizer.sanitizeEntityText(name, maxLength = 80)
                    val safeDesc = DataSanitizer.sanitizeEntityText(desc, maxLength = 500)
                    val safeCost = DataSanitizer.parseSafePositivePrice(costPrice)
                    val safeSell = DataSanitizer.parseSafePositivePrice(sellPrice)
    
                    if (safeName.isBlank()) {
                        Toast.makeText(context, "⛔ El nombre no puede estar vacío o tener solo símbolos raros.", Toast.LENGTH_SHORT).show()
                        return@withDebounce
                    }
                    if (safeCost == null || safeSell == null) {
                        Toast.makeText(context, "⛔ Precios inválidos. Usa números positivos (Ej. 100.50). Nada de emojis o negativos.", Toast.LENGTH_LONG).show()
                        return@withDebounce
                    }
    
                    onConfirm(safeName, safeDesc, safeCost, safeSell)
                }
            }) { Text("Guardar en Cerebro") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
