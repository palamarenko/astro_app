package com.iruna.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.iruna.app.MainActivity
import com.iruna.app.R
import com.iruna.app.i18n.str

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
            val src = BitmapFactory.decodeResource(context.resources, R.drawable.iruna)
            addPadding(src, paddingPercent = 0.12f)
        }.getOrNull()

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

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

    /**
     * Добавляет равномерные отступы вокруг bitmap, чтобы Android не обрезал
     * края изображения при отображении large icon в виде круга.
     * [paddingPercent] — отступ как доля от большей стороны (0.12 = 12%).
     */
    private fun addPadding(src: Bitmap, paddingPercent: Float): Bitmap {
        val pad = (maxOf(src.width, src.height) * paddingPercent).toInt()
        val dst = Bitmap.createBitmap(
            src.width  + pad * 2,
            src.height + pad * 2,
            Bitmap.Config.ARGB_8888,
        )
        Canvas(dst).drawBitmap(src, pad.toFloat(), pad.toFloat(), null)
        return dst
    }
}
