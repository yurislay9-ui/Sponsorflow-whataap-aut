package com.sponsorflow.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sponsorflow.data.BusinessDao
import com.sponsorflow.data.CustomerEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * CRM ViewModel (Pilar 2)
 * Sirve como puente bidireccional entre la interfaz gráfica y la Base de Datos.
 */
class CrmViewModel(private val dao: BusinessDao) : ViewModel() {
    
    // Flujo en tiempo real: Si Qwen de noche actualiza la DB, la UI se mueve automáticamente.
    val customers = dao.getAllCustomersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    fun updateCustomer(senderId: String, newProfile: String, newTags: String, newStage: String) {
        viewModelScope.launch {
            dao.manuallyUpdateCustomer(senderId, newProfile, newTags, newStage)
        }
    }
}

class CrmViewModelFactory(private val dao: BusinessDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        if (modelClass.isAssignableFrom(CrmViewModel::class.java)) {
            return CrmViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
