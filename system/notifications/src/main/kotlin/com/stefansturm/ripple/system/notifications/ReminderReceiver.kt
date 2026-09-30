package com.stefansturm.ripple.system.notifications

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.graphics.drawable.Icon
import com.stefansturm.ripple.core.domain.ReminderRule
import com.stefansturm.ripple.core.storage.RoomRippleRepository
import com.stefansturm.ripple.core.storage.createRippleDatabase
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val database = createRippleDatabase(context.applicationContext)
            try {
                val repository = RoomRippleRepository(database)
                repository.ensureSeeded()
                val reminder = repository.observeReminder().first()
                if (reminder.enabled && reminder.isActiveNow()) {
                    val snapshot = repository.observeToday().first()
                    postNotification(context.applicationContext, snapshot.defaultAddMl.value, snapshot.remainingMl.value, snapshot.goalMl.value)
                }
            } finally {
                database.close()
                pendingResult.finish()
            }
        }
    }

    private fun postNotification(context: Context, amountMl: Int, remainingMl: Int, goalMl: Int) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_channel), NotificationManager.IMPORTANCE_DEFAULT)
        )
        val logIntent = Intent(ACTION_LOG_DEFAULT).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_SOURCE, "notification")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val openIntent = Intent(ACTION_OPEN_TODAY).apply {
            setPackage(context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val logPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_LOG,
            logIntent.setClassName(context.packageName, "com.stefansturm.ripple.MainActivity"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_OPEN,
            openIntent.setClassName(context.packageName, "com.stefansturm.ripple.MainActivity"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_body, remainingMl, goalMl))
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, android.R.drawable.ic_input_add),
                    context.getString(R.string.notification_log, amountMl),
                    logPendingIntent
                ).build()
            )
            .build()
        runCatching { manager.notify(NOTIFICATION_ID, notification) }
    }

    private companion object {
        const val CHANNEL_ID = "ripple_reminders"
        const val NOTIFICATION_ID = 4101
        const val REQUEST_LOG = 4102
        const val REQUEST_OPEN = 4103
        const val ACTION_OPEN_TODAY = "de.stefansturm.ripple.OPEN_TODAY"
        const val ACTION_LOG_DEFAULT = "de.stefansturm.ripple.LOG_DEFAULT"
        const val EXTRA_SOURCE = "source"
    }
}

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(rule: ReminderRule) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, ReminderReceiver::class.java).setAction(ACTION_REMINDER),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        if (!rule.enabled) return
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val first = now.withHour(rule.start.hour).withMinute(rule.start.minute).withSecond(0).withNano(0)
            .let { candidate -> if (candidate.isAfter(now)) candidate else candidate.plusDays(1) }
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            first.toInstant().toEpochMilli(),
            rule.intervalMinutes * 60_000L,
            pendingIntent
        )
    }

    private companion object {
        const val REQUEST_CODE = 4104
        const val ACTION_REMINDER = "de.stefansturm.ripple.REMINDER"
    }
}

private fun ReminderRule.isActiveNow(): Boolean {
    val now = LocalTime.now()
    val current = now.hour * 60 + now.minute
    return current in start.totalMinutes()..end.totalMinutes()
}
