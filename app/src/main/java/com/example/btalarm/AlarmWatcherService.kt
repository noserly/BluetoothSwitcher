package com.example.btalarm

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder

class AlarmWatcherService : Service() {

    private val alarmChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = schedule()
    }

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("watch", "Слежение за будильником", NotificationManager.IMPORTANCE_LOW)
        )
        val n = Notification.Builder(this, "watch")
            .setContentTitle("BT Alarm Off работает")
            .setContentText("Bluetooth выключится за ${BtUtil.MINUTES_BEFORE} мин до будильника")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .build()
        startForeground(1, n)

        // ACTION_NEXT_ALARM_CLOCK_CHANGED нельзя поймать из манифеста — только динамически
        registerReceiver(alarmChangedReceiver, IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED))
        schedule()
    }

    private fun schedule() {
        val am = getSystemService(AlarmManager::class.java)
        val pi = { triggerAt: Long ->
            PendingIntent.getBroadcast(
                this, 0,
                Intent(this, BtOffReceiver::class.java).putExtra("trigger_at", triggerAt),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        // сбрасываем ранее поставленное срабатывание
        am.cancel(pi(0L))

        val next = am.nextAlarmClock ?: return
        val triggerAt = next.triggerTime
        val now = System.currentTimeMillis()
        if (triggerAt <= now || BtUtil.handled(this) == triggerAt) return

        val fireAt = triggerAt - BtUtil.MINUTES_BEFORE * 60_000L
        if (fireAt <= now) {
            // до будильника уже меньше 10 минут — выключаем сразу, один раз
            BtUtil.disableBluetooth(this)
            BtUtil.markHandled(this, triggerAt)
            return
        }
        val p = pi(triggerAt)
        if (am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, p)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, p)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        schedule()
        return START_STICKY
    }

    override fun onDestroy() {
        // при выключении переключателя отменяем запланированное отключение Bluetooth
        getSystemService(AlarmManager::class.java).cancel(
            PendingIntent.getBroadcast(
                this, 0, Intent(this, BtOffReceiver::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        unregisterReceiver(alarmChangedReceiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
