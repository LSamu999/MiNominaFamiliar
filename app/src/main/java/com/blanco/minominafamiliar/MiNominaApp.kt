package com.blanco.minominafamiliar

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.blanco.minominafamiliar.worker.NotificationWorker
import java.util.concurrent.TimeUnit

class MiNominaApp : Application() {

    companion object {
        private const val NOTIFICATION_WORK_NAME = "gastos_notification_work"
    }

    override fun onCreate() {
        super.onCreate()

        // Programar el Worker periódico
        programarNotificacionesDiarias()
    }

    private fun programarNotificacionesDiarias() {
        // Definir restricciones
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true) // Batería suficiente
            .setRequiresCharging(false)      // No requiere estar cargando
            .build()

        // Crear trabajo periódico (cada 24 horas)
        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        // Encolar el trabajo con política KEEP (no duplicar)
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            NOTIFICATION_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
