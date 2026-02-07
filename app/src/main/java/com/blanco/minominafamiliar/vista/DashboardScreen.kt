package com.blanco.minominafamiliar.vista

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blanco.minominafamiliar.model.Gasto
import com.blanco.minominafamiliar.model.MetodoPago
import com.blanco.minominafamiliar.viewmodel.GastoViewModel
import kotlinx.coroutines.delay
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

// Función de extensión para formatear números con separador de miles
fun Int.formatearConPuntos(): String {
    val symbols = DecimalFormatSymbols(Locale("es", "CL")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    val formatter = DecimalFormat("#,###", symbols)
    return formatter.format(this)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: GastoViewModel = viewModel(),
    onNavegarAAgregar: () -> Unit,
    onEditarGasto: (String) -> Unit = {} // Callback para editar con el ID del gasto
) {
    // Observamos la lista de gastos en tiempo real desde el ViewModel
    val listaGastos by viewModel.gastos.collectAsState()

    // Observar sueldo y saldo disponible
    val sueldo by viewModel.sueldo.collectAsState()
    val saldoDisponible by viewModel.saldoDisponible.collectAsState()

    // Estado para el diálogo de confirmación de eliminar
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }
    var gastoAEliminar by remember { mutableStateOf<Gasto?>(null) }

    // Estado para el diálogo de cerrar mes
    var mostrarDialogoCerrarMes by remember { mutableStateOf(false) }
    var mensajeCierreMes by remember { mutableStateOf("") }
    var mostrarMensajeExito by remember { mutableStateOf(false) }

    // Estado para el diálogo de editar sueldo
    var mostrarDialogoSueldo by remember { mutableStateOf(false) }
    var sueldoEditando by remember { mutableStateOf("") }

    // LÓGICA DE RESUMEN
    val porPagar = listaGastos.filter { !it.isPagado && it.metodoPago == MetodoPago.DEBITO }.sumOf { it.monto }.toInt()
    val deudaTarjeta = listaGastos.filter { !it.isPagado && it.metodoPago == MetodoPago.CREDITO }.sumOf { it.monto }.toInt()
    val totalGastado = listaGastos.filter { it.isPagado }.sumOf { it.monto }.toInt()
    val totalGastos = listaGastos.sumOf { it.monto }.toInt()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Nómina Familiar") },
                actions = {
                    // Botón Cerrar Mes
                    IconButton(onClick = { mostrarDialogoCerrarMes = true }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Cerrar Mes",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavegarAAgregar, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Gasto", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // TARJETA DE BALANCE (HEADER PRINCIPAL)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Sueldo con icono de edición
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Sueldo",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "$${sueldo.formatearConPuntos()}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        IconButton(onClick = {
                            sueldoEditando = sueldo.toString()
                            mostrarDialogoSueldo = true
                        }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Editar Sueldo",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    HorizontalDivider()

                    // Gastos Totales
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Gastos Totales",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "$${totalGastos.formatearConPuntos()}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    HorizontalDivider()

                    // Disponible (con color condicional)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Disponible",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "$${saldoDisponible.formatearConPuntos()}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (saldoDisponible < 0) Color.Red else Color(0xFF2E7D32) // Verde oscuro
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CABECERA DE RESUMEN (cards secundarias)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ResumenCard("Por Pagar", porPagar, Modifier.weight(1f), Color(0xFFE57373))
                ResumenCard("Tarjeta", deudaTarjeta, Modifier.weight(1f), Color(0xFFFFB74D))
                ResumenCard("Gastado", totalGastado, Modifier.weight(1f), Color(0xFF81C784))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Detalle de Gastos",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // LISTA DE GASTOS
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(listaGastos, key = { it.id }) { gasto ->
                    SwipeToDeleteItem(
                        gasto = gasto,
                        onDelete = {
                            gastoAEliminar = gasto
                            mostrarDialogoEliminar = true
                        },
                        onTogglePago = { viewModel.togglePago(gasto) },
                        onEditar = { onEditarGasto(gasto.id) }
                    )
                }
            }
        }

        // Diálogo de confirmación para eliminar
        if (mostrarDialogoEliminar && gastoAEliminar != null) {
            AlertDialog(
                onDismissRequest = {
                    mostrarDialogoEliminar = false
                    gastoAEliminar = null
                },
                icon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = {
                    Text("¿Eliminar gasto?")
                },
                text = {
                    Text("¿Estás seguro de que deseas eliminar \"${gastoAEliminar?.nombre}\" de $${gastoAEliminar?.monto?.toInt()?.formatearConPuntos()}?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            gastoAEliminar?.let { viewModel.eliminarGasto(it.id) }
                            mostrarDialogoEliminar = false
                            gastoAEliminar = null
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Eliminar")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            mostrarDialogoEliminar = false
                            gastoAEliminar = null
                        }
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Diálogo de confirmación para cerrar mes
        if (mostrarDialogoCerrarMes) {
            AlertDialog(
                onDismissRequest = {
                    mostrarDialogoCerrarMes = false
                },
                icon = {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    Text("¿Cerrar Mes Actual?")
                },
                text = {
                    Text("Esto borrará los gastos únicos, reiniciará los pagos mensuales y avanzará las cuotas. ¿Estás seguro?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            mostrarDialogoCerrarMes = false
                            // Llamar a cerrarMes
                            viewModel.cerrarMes(
                                onSuccess = {
                                    mensajeCierreMes = "✅ Mes cerrado exitosamente"
                                    mostrarMensajeExito = true
                                },
                                onFailure = { error ->
                                    mensajeCierreMes = "❌ Error: ${error.message}"
                                    mostrarMensajeExito = true
                                }
                            )
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Confirmar")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            mostrarDialogoCerrarMes = false
                        }
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Diálogo para editar sueldo
        if (mostrarDialogoSueldo) {
            AlertDialog(
                onDismissRequest = {
                    mostrarDialogoSueldo = false
                },
                icon = {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    Text("Editar Sueldo Mensual")
                },
                text = {
                    OutlinedTextField(
                        value = sueldoEditando,
                        onValueChange = { if (it.all { char -> char.isDigit() }) sueldoEditando = it },
                        label = { Text("Ingresa el sueldo ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val nuevoSueldo = sueldoEditando.toIntOrNull() ?: 0
                            viewModel.actualizarSueldo(
                                nuevoMonto = nuevoSueldo,
                                onSuccess = {
                                    mostrarDialogoSueldo = false
                                },
                                onFailure = { error ->
                                    // Manejar error si es necesario
                                }
                            )
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Guardar")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            mostrarDialogoSueldo = false
                        }
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Snackbar o mensaje de resultado
        if (mostrarMensajeExito) {
            LaunchedEffect(Unit) {
                delay(3000) // Mostrar por 3 segundos
                mostrarMensajeExito = false
                mensajeCierreMes = ""
            }
        }
    }
}

@Composable
fun ResumenCard(titulo: String, monto: Int, modifier: Modifier, colorFondo: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = colorFondo.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(titulo, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text("$${monto.formatearConPuntos()}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colorFondo)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteItem(
    gasto: Gasto,
    onDelete: () -> Unit,
    onTogglePago: () -> Unit,
    onEditar: () -> Unit
) {
    var isDeleted by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                isDeleted = true
                true
            } else {
                false
            }
        },
        positionalThreshold = { it * 0.5f }
    )

    LaunchedEffect(isDeleted) {
        if (isDeleted) {
            delay(300)
            onDelete()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Red)
                    .padding(16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    ) {
        GastoItem(
            gasto = gasto,
            onTogglePago = onTogglePago,
            onEditar = onEditar
        )
    }
}

@Composable
fun GastoItem(
    gasto: Gasto,
    onTogglePago: () -> Unit,
    onEditar: () -> Unit = {}
) {
    // Color de fondo especial para Crédito no pagado
    val backgroundColor = if (gasto.metodoPago == MetodoPago.CREDITO && !gasto.isPagado) {
        Color(0xFFFFF3E0) // Naranja muy suave
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditar() }, // Hacer clickable la Card para editar
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = gasto.nombre,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$${gasto.monto.toInt().formatearConPuntos()} • Vence día ${gasto.diaVencimiento}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                if (gasto.metodoPago == MetodoPago.CREDITO) {
                    Text(
                        text = "TARJETA CRÉDITO",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFEF6C00),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Botón de Editar
            IconButton(onClick = onEditar) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Editar Gasto",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Checkbox(
                checked = gasto.isPagado,
                onCheckedChange = { onTogglePago() }
            )
        }
    }
}