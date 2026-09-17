package de.msjones.android.alarmapp.util

import android.app.NotificationManager
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Unit-Tests für sichtbare Status-Notifications.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class NotificationHelperTest {

    /**
     * Liefert den Robolectric-Application-Context.
     *
     * @return Test-Context
     */
    private fun context(): Context = RuntimeEnvironment.getApplication()

    /** Eine Statusmeldung erscheint als Android-Notification. */
    @Test
    fun showStatusNotification_postsNotificationWithTitleAndText() {
        val helper = NotificationHelper(context())
        helper.showStatusNotification(
            connectionId = "conn-1",
            title = "Server nicht erreichbar",
            message = "Broker unter tcp://mqtt.local:1883 nicht erreichbar"
        )

        val notification = activeNotification("conn-1")
        assertNotNull(notification)
        assertEquals("Server nicht erreichbar", notification!!.extras.getString("android.title"))
        assertEquals(
            "Broker unter tcp://mqtt.local:1883 nicht erreichbar",
            notification.extras.getString("android.text")
        )
    }

    /** Die Service-Notification enthält die Verbindungsübersicht. */
    @Test
    fun updateServiceNotification_postsSummaryText() {
        val helper = NotificationHelper(context())
        helper.updateServiceNotification("1 von 2 Verbindungen aktiv")

        val manager = context().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = manager.activeNotifications
            .first { it.id == NotificationHelper.SERVICE_NOTIFICATION_ID }
            .notification

        assertEquals("JF Alarm", notification.extras.getString("android.title"))
        assertEquals("1 von 2 Verbindungen aktiv", notification.extras.getString("android.text"))
    }

    /** Eine Status-Notification kann nach erfolgreicher Verbindung entfernt werden. */
    @Test
    fun cancelStatusNotification_removesPostedNotification() {
        val helper = NotificationHelper(context())
        helper.showStatusNotification("conn-1", "Server nicht erreichbar", "nicht erreichbar")
        helper.cancelStatusNotification("conn-1")
        assertNull(activeNotification("conn-1"))
    }

    /**
     * Findet die Status-Notification zur angegebenen Verbindung.
     *
     * @param connectionId Kennung der Verbindung
     * @return Notification oder null
     */
    private fun activeNotification(connectionId: String): android.app.Notification? {
        val expectedId = NotificationHelper.STATUS_NOTIFICATION_ID_BASE +
            (connectionId.hashCode() and 0x7FFF)
        val manager = context().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return manager.activeNotifications.firstOrNull { it.id == expectedId }?.notification
    }
}
