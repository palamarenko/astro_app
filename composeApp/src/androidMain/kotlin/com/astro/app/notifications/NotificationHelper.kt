package com.astro.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.astro.app.R
import com.astro.app.i18n.str

object NotificationHelper {

    private const val CHANNEL_ID = "horoscope_daily"
    private var channelCreated = false

    fun createChannel(context: Context) {
        if (channelCreated) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name        = str.notif_channel_name
            val description = str.notif_channel_desc
            val importance  = NotificationManager.IMPORTANCE_DEFAULT
            val channel     = NotificationChannel(CHANNEL_ID, name, importance).apply {
                this.description = description
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
        channelCreated = true
    }

    /**
     * Показывает уведомление «гороскоп готов».
     * @param signName  локализованное название знака зодиака (напр. «Лев»)
     * @param notifId   уникальный ID, чтобы не дублировать уведомления
     */
    fun showDailyHoroscope(context: Context, signName: String, notifId: Int = 1001) {
        val title = str.notif_daily_title
        val body  = str.notif_daily_body.format(signName)

        val largeBitmap = runCatching {
            BitmapFactory.decodeResource(context.resources, R.drawable.ic_witch)
        }.getOrNull()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)   // системная иконка-заглушка
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        if (largeBitmap != null) {
            builder.setLargeIcon(largeBitmap)
        }

        with(NotificationManagerCompat.from(context)) {
            // Разрешение уже проверено системой через POST_NOTIFICATIONS,
            // но lint требует явного try/catch
            try {
                notify(notifId, builder.build())
            } catch (_: SecurityException) { /* разрешение не выдано */ }
        }
    }
}
