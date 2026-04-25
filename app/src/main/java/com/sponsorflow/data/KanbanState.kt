package com.sponsorflow.data

/**
 * Estados del Kanban usando Sealed Classes para garantizar Typesafety y exhaustividad.
 */
sealed class KanbanState(val id: String, val displayName: String) {
    object ContactoInicial : KanbanState("CONTACTO_INICIAL", "Nuevo Contacto")
    object Negociando : KanbanState("NEGOCIANDO", "Negociando")
    object CasiCierre : KanbanState("CASI_CIERRE", "Casi Cierre")
    object Compro : KanbanState("COMPRO", "Compró")
    object Perdido : KanbanState("PERDIDO", "Perdido")

    companion object {
        fun fromId(id: String?): KanbanState {
            return when (id) {
                "CONTACTO_INICIAL" -> ContactoInicial
                "NEGOCIANDO" -> Negociando
                "CASI_CIERRE" -> CasiCierre
                "COMPRO" -> Compro
                "PERDIDO" -> Perdido
                else -> ContactoInicial // Default fallback para Unknown states o nulls
            }
        }
    }
}
