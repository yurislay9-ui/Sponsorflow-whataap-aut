package com.sponsorflow.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sponsorflow.data.CatalogDao
import com.sponsorflow.data.ProductEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel del Catálogo.
 * Intermediario que conecta la UI de Compose con la Base de Datos Room sin bloquear la pantalla.
 */
class CatalogViewModel(private val dao: CatalogDao) : ViewModel() {

    private val _products = MutableStateFlow<List<ProductEntity>>(emptyList())
    val products: StateFlow<List<ProductEntity>> = _products

    init {
        loadCatalog()
    }

    private fun loadCatalog() {
        viewModelScope.launch {
            // Reutilizamos la consulta general para pintar la UI
            val catalog = dao.getAvailableCatalogOverview()
            _products.value = catalog
        }
    }

    fun addProduct(name: String, desc: String, cost: Double, sell: Double) {
        viewModelScope.launch {
            val newProduct = ProductEntity(
                name = name,
                description = desc,
                costPrice = cost,
                sellPrice = sell,
                stockAvailable = true
            )
            dao.insertProduct(newProduct)
            loadCatalog() // Refrescar la lista en la pantalla
        }
    }
}

// Factoría para inyectar el DAO en el ViewModel
class CatalogViewModelFactory(private val dao: CatalogDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CatalogViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CatalogViewModel(dao) as T
        }
        throw IllegalArgumentException("ViewModel desconocido")
    }
}
