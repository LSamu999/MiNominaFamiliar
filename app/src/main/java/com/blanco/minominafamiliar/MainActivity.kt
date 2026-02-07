package com.blanco.minominafamiliar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.blanco.minominafamiliar.ui.theme.MiNominaFamiliarTheme
import com.blanco.minominafamiliar.vista.AgregarGastoScreen
import com.blanco.minominafamiliar.vista.DashboardScreen
import com.blanco.minominafamiliar.vista.LoginScreen
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    // Launcher para solicitar permiso de notificaciones
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permiso concedido
            println("Permiso de notificaciones concedido")
        } else {
            // Permiso denegado
            println("Permiso de notificaciones denegado")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Solicitar permiso de notificaciones en Android 13+
        solicitarPermisoNotificaciones()

        setContent {
            MiNominaFamiliarTheme {
                // Verificar si hay usuario autenticado
                val auth = remember { FirebaseAuth.getInstance() }
                var usuarioAutenticado by remember { mutableStateOf(auth.currentUser != null) }

                // Navegación simple entre pantallas
                var pantallaActual by remember { mutableStateOf("dashboard") }
                var gastoIdEditar by remember { mutableStateOf<String?>(null) }

                if (!usuarioAutenticado) {
                    // Mostrar pantalla de login si no hay usuario autenticado
                    LoginScreen(
                        onLoginExitoso = {
                            usuarioAutenticado = true
                        }
                    )
                } else {
                    // Mostrar la app normal si está autenticado
                    when (pantallaActual) {
                        "dashboard" -> {
                            DashboardScreen(
                                onNavegarAAgregar = {
                                    gastoIdEditar = null // Modo creación
                                    pantallaActual = "agregar"
                                },
                                onEditarGasto = { id ->
                                    gastoIdEditar = id // Modo edición con el ID
                                    pantallaActual = "agregar"
                                }
                            )
                        }
                        "agregar" -> {
                            AgregarGastoScreen(
                                gastoId = gastoIdEditar, // Pasar el ID (null para crear, valor para editar)
                                onGastoGuardado = {
                                    gastoIdEditar = null // Limpiar después de guardar
                                    pantallaActual = "dashboard"
                                },
                                onNavegacionAtras = {
                                    gastoIdEditar = null // Limpiar al cancelar
                                    pantallaActual = "dashboard"
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun solicitarPermisoNotificaciones() {
        // Solo solicitar permiso en Android 13 (Tiramisu) o superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permiso ya concedido
                    println("Permiso de notificaciones ya concedido")
                }
                else -> {
                    // Solicitar el permiso
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}