package com.blanco.minominafamiliar.model

import com.google.firebase.firestore.DocumentId

data class Gasto(
    val id: String = "",
    val nombre: String = "",
    val monto: Double = 0.0,
    @field:JvmField
    val isPagado: Boolean = false,
    val diaVencimiento: Int = 1,
    val cuentaOrigen: String = "",
    val metodoPago: MetodoPago = MetodoPago.DEBITO,
    val frecuencia: Frecuencia = Frecuencia.MENSUAL,
    @field:JvmField
    val esCuota: Boolean = false,
    val cuotasRestantes: Int = 0
)

enum class MetodoPago {
    DEBITO,
    CREDITO
}

enum class Frecuencia {
    MENSUAL,
    UNICO
}