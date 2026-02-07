package com.blanco.minominafamiliar.vista

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blanco.minominafamiliar.model.Frecuencia
import com.blanco.minominafamiliar.model.Gasto
import com.blanco.minominafamiliar.model.MetodoPago
import com.blanco.minominafamiliar.viewmodel.GastoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgregarGastoScreen(
    gastoId: String? = null, // Parámetro opcional para edición
    viewModel: GastoViewModel = viewModel(),
    onGastoGuardado: () -> Unit, // Callback para volver atrás al terminar
    onNavegacionAtras: () -> Unit = {} // Callback para botón atrás
) {
    // Manejar botón de sistema atrás
    BackHandler {
        onNavegacionAtras()
    }
    // ESTADO: Variables donde guardamos lo que el usuario escribe
    var nombre by remember { mutableStateOf("") }
    var monto by remember { mutableStateOf("") }
    var diaVencimiento by remember { mutableStateOf("5") } // Por defecto día 5

    // Listas para los selectores
    val cuentas = listOf("Mach", "Banco Estado", "Banco Chile", "Efectivo")
    var cuentaSeleccionada by remember { mutableStateOf(cuentas[0]) }
    var expandirCuentas by remember { mutableStateOf(false) }

    // Estado para Tarjeta de Crédito
    var esCredito by remember { mutableStateOf(false) }

    // Estado para Frecuencia (Mensual por defecto)
    var frecuenciaSeleccionada by remember { mutableStateOf(Frecuencia.MENSUAL) }

    // Determinar si estamos editando o creando
    val esEdicion = gastoId != null

    // Cargar datos si estamos editando
    LaunchedEffect(gastoId) {
        if (gastoId != null) {
            val gasto = viewModel.obtenerGastoPorId(gastoId)
            if (gasto != null) {
                nombre = gasto.nombre
                monto = gasto.monto.toInt().toString()
                diaVencimiento = gasto.diaVencimiento.toString()
                cuentaSeleccionada = gasto.cuentaOrigen
                esCredito = gasto.metodoPago == MetodoPago.CREDITO
                frecuenciaSeleccionada = gasto.frecuencia
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (esEdicion) "Editar Gasto" else "Nuevo Gasto Familiar") },
                navigationIcon = {
                    IconButton(onClick = onNavegacionAtras) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver atrás"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 1. NOMBRE DEL GASTO
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("¿Qué vamos a pagar? (Ej. Netflix)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // 2. MONTO
            OutlinedTextField(
                value = monto,
                onValueChange = { if (it.all { char -> char.isDigit() }) monto = it },
                label = { Text("Monto ($)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            // 3. DÍA DE VENCIMIENTO
            OutlinedTextField(
                value = diaVencimiento,
                onValueChange = { if (it.length <= 2) diaVencimiento = it },
                label = { Text("Día de vencimiento (1-31)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // 4. SELECTOR DE CUENTA (Dropdown)
            ExposedDropdownMenuBox(
                expanded = expandirCuentas,
                onExpandedChange = { expandirCuentas = !expandirCuentas }
            ) {
                OutlinedTextField(
                    value = cuentaSeleccionada,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("¿De dónde sale la plata?") },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandirCuentas,
                    onDismissRequest = { expandirCuentas = false }
                ) {
                    cuentas.forEach { cuenta ->
                        DropdownMenuItem(
                            text = { Text(cuenta) },
                            onClick = {
                                cuentaSeleccionada = cuenta
                                expandirCuentas = false
                            }
                        )
                    }
                }
            }

            // 5. SWITCH TARJETA DE CRÉDITO
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("¿Es compra con Tarjeta de Crédito?", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = esCredito, onCheckedChange = { esCredito = it })
            }

            // 6. SELECTOR DE FRECUENCIA (RadioButtons)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Frecuencia del Gasto:",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Opción Mensual
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { frecuenciaSeleccionada = Frecuencia.MENSUAL },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = frecuenciaSeleccionada == Frecuencia.MENSUAL,
                            onClick = { frecuenciaSeleccionada = Frecuencia.MENSUAL }
                        )
                        Text("Mensual (Recurrente)")
                    }

                    // Opción Único
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { frecuenciaSeleccionada = Frecuencia.UNICO },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = frecuenciaSeleccionada == Frecuencia.UNICO,
                            onClick = { frecuenciaSeleccionada = Frecuencia.UNICO }
                        )
                        Text("Único")
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f)) // Empuja el botón al final

            // 6. BOTÓN GUARDAR
            Button(
                onClick = {
                    if (nombre.isNotEmpty() && monto.isNotEmpty()) {
                        if (esEdicion && gastoId != null) {
                            // MODO EDICIÓN: Actualizar gasto existente
                            val gastoActualizado = Gasto(
                                id = gastoId, // Mantener el ID existente
                                nombre = nombre,
                                monto = monto.toDoubleOrNull() ?: 0.0,
                                diaVencimiento = diaVencimiento.toIntOrNull() ?: 1,
                                cuentaOrigen = cuentaSeleccionada,
                                metodoPago = if (esCredito) MetodoPago.CREDITO else MetodoPago.DEBITO,
                                isPagado = viewModel.obtenerGastoPorId(gastoId)?.isPagado ?: false,
                                frecuencia = frecuenciaSeleccionada
                            )

                            viewModel.actualizarGasto(
                                gasto = gastoActualizado,
                                onSuccess = { onGastoGuardado() },
                                onFailure = { /* Manejar error aquí */ }
                            )
                        } else {
                            // MODO CREACIÓN: Agregar nuevo gasto
                            val nuevoGasto = Gasto(
                                nombre = nombre,
                                monto = monto.toDoubleOrNull() ?: 0.0,
                                diaVencimiento = diaVencimiento.toIntOrNull() ?: 1,
                                cuentaOrigen = cuentaSeleccionada,
                                metodoPago = if (esCredito) MetodoPago.CREDITO else MetodoPago.DEBITO,
                                frecuencia = frecuenciaSeleccionada
                            )

                            viewModel.agregarGasto(
                                gasto = nuevoGasto,
                                onSuccess = { onGastoGuardado() },
                                onFailure = { /* Manejar error aquí */ }
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(if (esEdicion) "Actualizar Gasto" else "Guardar Gasto")
            }
        }
    }
}