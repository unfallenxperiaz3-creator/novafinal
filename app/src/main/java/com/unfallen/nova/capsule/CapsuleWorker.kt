package com.unfallen.nova.capsule

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.unfallen.nova.MainActivity
import com.unfallen.nova.R
import com.unfallen.nova.data.Storage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Programa y entrega las cápsulas del tiempo (sobrevive a reinicios del móvil gracias a WorkManager). */
object CapsuleScheduler {
    const val EXTRA_OPEN_CAPSULES = "open_capsules"
    private const val CHANNEL_ID = "capsules"

    fun schedule(context: Context, id: String, openAt: Long) {
        val delay = (openAt - System.currentTimeMillis()).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<CapsuleWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("id" to id))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("capsule-$id", ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context, id: String) {
        WorkManager.getInstance(context).cancelUniqueWork("capsule-$id")
    }

    fun notifyReady(context: Context, id: String, createdAt: Long) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Cápsulas del tiempo", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Avisos cuando una cápsula del tiempo está lista para abrirse"
                }
            )
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_OPEN_CAPSULES, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pending = PendingIntent.getActivity(
            context, id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val written = SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-ES")).format(Date(createdAt))
        // Por privacidad, la notificación nunca muestra el contenido de la cápsula
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_nova)
            .setContentTitle("⏳ Tu cápsula del tiempo está lista")
            .setContentText("Te la escribiste el $written. Ábrela en NOVA.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                NotificationManagerCompat.from(context).notify(id.hashCode(), notification)
            }
        } catch (e: SecurityException) {
            // Sin permiso de notificaciones: la cápsula aparecerá igualmente como "lista" al abrir la app
        }
    }
}

class CapsuleWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val id = inputData.getString("id") ?: return Result.success()
        val capsule = Storage(applicationContext).loadCapsules().firstOrNull { it.id == id }
            ?: return Result.success() // la borraste antes de tiempo
        if (!capsule.opened) CapsuleScheduler.notifyReady(applicationContext, capsule.id, capsule.createdAt)
        return Result.success()
    }
}
