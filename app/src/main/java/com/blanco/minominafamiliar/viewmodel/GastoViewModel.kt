package com.blanco.minominafamiliar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blanco.minominafamiliar.model.Gasto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class GastoViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Obtener UID del usuario actual (si existe)
    private val userId: String?
        get() = auth.currentUser?.uid

    // Colecciones privadas por usuario
    private val gastosCollection
        get() = userId?.let { db.collection("usuarios").document(it).collection("gastos") }

    private val configuracionCollection
        get() = userId?.let { db.collection("usuarios").document(it).collection("configuracion") }

    // 1. Exponer la lista de gastos reactiva (StateFlow)
    private val _gastos = MutableStateFlow<List<Gasto>>(emptyList())
    val gastos: StateFlow<List<Gasto>> = _gastos.asStateFlow()

    // StateFlow para el sueldo mensual
    private val _sueldo = MutableStateFlow(0)
    val sueldo: StateFlow<Int> = _sueldo.asStateFlow()

    // StateFlow para el saldo disponible (calculado)
    val saldoDisponible: StateFlow<Int> = combine(_sueldo, _gastos) { sueldoActual, listaGastos ->
        val totalGastos = listaGastos.sumOf { it.monto }.toInt()
        sueldoActual - totalGastos
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    init {
        // 2. Iniciar la escucha en tiempo real al instanciar el ViewModel
        configurarSnapshotListener()
        configurarListenerSueldo()
    }

    private fun configurarSnapshotListener() {
        gastosCollection?.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Manejar error (puedes imprimirlo en Logcat)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                // Convertir documentos a objetos Gasto e inyectar el ID del documento
                val listaActualizada = snapshot.documents.mapNotNull { doc ->
                    doc.toObject<Gasto>()?.copy(id = doc.id)
                }
                _gastos.value = listaActualizada
            }
        }
    }

    private fun configurarListenerSueldo() {
        configuracionCollection?.document("presupuesto_general")
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Manejar error
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val sueldoFirestore = snapshot.getLong("sueldo")?.toInt() ?: 0
                    _sueldo.value = sueldoFirestore
                } else {
                    // Si el documento no existe, mantener el valor por defecto (0)
                    _sueldo.value = 0
                }
            }
    }

    fun agregarGasto(gasto: Gasto, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        if (gastosCollection == null) {
            onFailure(Exception("Usuario no autenticado"))
            return
        }

        viewModelScope.launch {
            val nuevoGasto = hashMapOf(
                "nombre" to gasto.nombre,
                "monto" to gasto.monto,
                "isPagado" to gasto.isPagado,
                "diaVencimiento" to gasto.diaVencimiento,
                "cuentaOrigen" to gasto.cuentaOrigen,
                "metodoPago" to gasto.metodoPago.name,
                "frecuencia" to gasto.frecuencia.name,
                "esCuota" to gasto.esCuota,
                "cuotasRestantes" to gasto.cuotasRestantes
            )
            try {
                gastosCollection!!.add(nuevoGasto).await()
                onSuccess()
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }
    // 3. Función nueva: Toggle (cambiar estado de pago)
    fun togglePago(gasto: Gasto) {
        if (gasto.id.isEmpty() || gastosCollection == null) return

        viewModelScope.launch {
            try {
                // Actualizamos solo el campo 'isPagado' con el valor inverso
                gastosCollection!!.document(gasto.id)
                    .update("isPagado", !gasto.isPagado)
                    .await()
            } catch (e: Exception) {
                // Manejar error de actualización
            }
        }
    }

    // 4. Función para eliminar gasto
    fun eliminarGasto(id: String, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        if (id.isEmpty() || gastosCollection == null) return

        viewModelScope.launch {
            try {
                gastosCollection!!.document(id)
                    .delete()
                    .await()
                onSuccess()
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    // 5. Función para actualizar gasto existente
    fun actualizarGasto(gasto: Gasto, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        if (gasto.id.isEmpty() || gastosCollection == null) return

        viewModelScope.launch {
            try {
                val gastoActualizado = hashMapOf(
                    "nombre" to gasto.nombre,
                    "monto" to gasto.monto,
                    "isPagado" to gasto.isPagado,
                    "diaVencimiento" to gasto.diaVencimiento,
                    "cuentaOrigen" to gasto.cuentaOrigen,
                    "metodoPago" to gasto.metodoPago.name,
                    "frecuencia" to gasto.frecuencia.name,
                    "esCuota" to gasto.esCuota,
                    "cuotasRestantes" to gasto.cuotasRestantes
                )
                gastosCollection!!.document(gasto.id)
                    .set(gastoActualizado)
                    .await()
                onSuccess()
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    // 6. Función para obtener un gasto por ID desde la lista en memoria
    fun obtenerGastoPorId(id: String): Gasto? {
        return _gastos.value.find { it.id == id }
    }

    // 7. Función para cerrar el mes
    fun cerrarMes(onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        if (gastosCollection == null) {
            onFailure(Exception("Usuario no autenticado"))
            return
        }

        viewModelScope.launch {
            try {
                // Obtener todos los gastos actuales desde Firestore
                val snapshot = gastosCollection!!.get().await()

                // Listas para clasificar operaciones
                val gastosAEliminar = mutableListOf<String>() // IDs
                val gastosAActualizar = mutableListOf<Pair<String, Map<String, Any>>>() // ID + Datos

                // Iterar sobre cada gasto
                snapshot.documents.forEach { doc ->
                    val id = doc.id
                    val frecuencia = doc.getString("frecuencia") ?: "MENSUAL"
                    val esCuota = doc.getBoolean("esCuota") ?: false
                    val cuotasRestantes = doc.getLong("cuotasRestantes")?.toInt() ?: 0

                    when {
                        // Caso 1: Gasto ÚNICO - Eliminar
                        frecuencia == "UNICO" -> {
                            gastosAEliminar.add(id)
                        }

                        // Caso 2: Gasto con CUOTAS
                        esCuota -> {
                            if (cuotasRestantes <= 1) {
                                // Última cuota - Eliminar
                                gastosAEliminar.add(id)
                            } else {
                                // Aún hay cuotas - Actualizar
                                val datosActualizados = mapOf(
                                    "isPagado" to false,
                                    "cuotasRestantes" to (cuotasRestantes - 1)
                                )
                                gastosAActualizar.add(id to datosActualizados)
                            }
                        }

                        // Caso 3: Gasto MENSUAL normal - Resetear isPagado
                        frecuencia == "MENSUAL" -> {
                            val datosActualizados = mapOf(
                                "isPagado" to false
                            )
                            gastosAActualizar.add(id to datosActualizados)
                        }
                    }
                }

                // Ejecutar todas las operaciones usando batch write
                val batch = db.batch()

                // Agregar eliminaciones al batch
                gastosAEliminar.forEach { id ->
                    val docRef = gastosCollection!!.document(id)
                    batch.delete(docRef)
                }

                // Agregar actualizaciones al batch
                gastosAActualizar.forEach { (id, datos) ->
                    val docRef = gastosCollection!!.document(id)
                    batch.update(docRef, datos)
                }

                // Commit del batch (todas las operaciones en una transacción)
                batch.commit().await()

                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                onFailure(e)
            }
        }
    }

    // 8. Función para actualizar el sueldo mensual
    fun actualizarSueldo(nuevoMonto: Int, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        if (configuracionCollection == null) {
            onFailure(Exception("Usuario no autenticado"))
            return
        }

        viewModelScope.launch {
            try {
                val datos = hashMapOf(
                    "sueldo" to nuevoMonto
                )
                configuracionCollection!!.document("presupuesto_general")
                    .set(datos)
                    .await()
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                onFailure(e)
            }
        }
    }
}