package com.blanco.minominafamiliar.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.blanco.minominafamiliar.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val CHANNEL_ID = "gastos_channel"
        private const val CHANNEL_NAME = "Recordatorios de Gastos"
        private const val CHANNEL_DESCRIPTION = "Notificaciones de gastos próximos a vencer"
    }

    override suspend fun doWork(): Result {
        return try {
            // Verificar si hay usuario autenticado
            val userId = auth.currentUser?.uid
            if (userId == null) {
                // Si no hay usuario logueado, no hacer nada
                return Result.success()
            }

            // Crear canal de notificación si es necesario
            createNotificationChannel()

            // Obtener gastos no pagados desde la colección privada del usuario
            val gastosCollection = db.collection("usuarios")
                .document(userId)
                .collection("gastos")

            val snapshot = gastosCollection
                .whereEqualTo("isPagado", false)
                .get()
                .await()

            // Obtener el día de mañana
            val manana = LocalDate.now().plusDays(1).dayOfMonth

            // Filtrar gastos que vencen mañana
            val gastosVencenManana = snapshot.documents.mapNotNull { doc ->
                val diaVencimiento = doc.getLong("diaVencimiento")?.toInt()
                val nombre = doc.getString("nombre") ?: ""
                val monto = doc.getDouble("monto") ?: 0.0

                if (diaVencimiento == manana) {
                    Triple(nombre, monto, diaVencimiento)
                } else {
                    null
                }
            }

            // Lanzar notificación por cada gasto que vence mañana
            gastosVencenManana.forEachIndexed { index, (nombre, monto, _) ->
                enviarNotificacion(
                    notificationId = index,
                    titulo = "¡Vence Mañana!",
                    texto = "Recuerda pagar $nombre de $$${monto.toInt()}"
                )
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }

            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun enviarNotificacion(notificationId: Int, titulo: String, texto: String) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext)
                .notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Permiso no concedido
            e.printStackTrace()
        }
    }
}
