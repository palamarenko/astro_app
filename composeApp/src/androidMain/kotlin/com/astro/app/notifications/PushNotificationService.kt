package com.astro.app.notifications

import com.astro.app.data.UserStorage
import com.astro.app.i18n.LanguageManager
import com.astro.app.i18n.str
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService

/**
 * Получает data-сообщения от Firebase и показывает локальное уведомление.
 *
 * Ожидаемый формат data-payload:
 *   {
 *     "type": "daily_horoscope"   // тип уведомления
 *   }
 *
 * Всё содержимое уведомления генерируется на клиенте.
 */
class PushNotificationService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val type = message.data["type"] ?: return

        when (type) {
            "daily_horoscope" -> showDailyHoroscope()
            // Сюда можно добавить другие типы уведомлений
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Подписываемся на топик, чтобы admin panel могла отправлять всем разом
        val profile = UserStorage.load()
        if (profile?.pushNotificationsEnabled == true) {
            subscribeToTopic()
        }
    }

    // ── Topic subscription ────────────────────────────────────────────────────

    companion object {
        fun subscribeToTopic() {
            FirebaseMessaging.getInstance()
                .subscribeToTopic(PushAdminService.TOPIC)
        }

        fun unsubscribeFromTopic() {
            FirebaseMessaging.getInstance()
                .unsubscribeFromTopic(PushAdminService.TOPIC)
        }
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    private fun showDailyHoroscope() {
        val profile  = UserStorage.load() ?: return
        if (!profile.pushNotificationsEnabled) return

        // Получаем локализованное название знака
        val signName = getLocalizedSignName(profile.signId)

        NotificationHelper.createChannel(applicationContext)
        NotificationHelper.showDailyHoroscope(
            context   = applicationContext,
            signName  = signName,
            notifId   = 1001,
        )
    }

    private fun getLocalizedSignName(signId: String): String {
        return when (signId) {
            "aries"       -> str.sign_aries
            "taurus"      -> str.sign_taurus
            "gemini"      -> str.sign_gemini
            "cancer"      -> str.sign_cancer
            "leo"         -> str.sign_leo
            "virgo"       -> str.sign_virgo
            "libra"       -> str.sign_libra
            "scorpio"     -> str.sign_scorpio
            "sagittarius" -> str.sign_sagittarius
            "capricorn"   -> str.sign_capricorn
            "aquarius"    -> str.sign_aquarius
            "pisces"      -> str.sign_pisces
            else          -> signId
        }
    }
}
