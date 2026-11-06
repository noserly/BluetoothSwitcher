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
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel("watch", "Слежение за будильником", NotificationManager.IMPORTANCE_LOW)
        )
        val n = Notification.Builder(this, "watch")
            .setContentTitle("BT Alarm Off активен")
            .setContentText("Bluetooth выключится за ${BtUtil.MINUTES_BEFORE} мин до будильника")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .build()
        startForeground(1, n)

        // ACTION_NEXT_ALARM_CLOCK_CHANGED нельзя поймать из манифеста — только динамически
        registerReceiver(alarmChangedReceiver, IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED))
        schedule()
    }

    private fun pi(code: Int, cls: Class<*>, triggerAt: Long = 0L): PendingIntent =
        PendingIntent.getBroadcast(
            this, code,
            Intent(this, cls).putExtra("trigger_at", triggerAt),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun setAlarm(am: AlarmManager, at: Long, p: PendingIntent) {
        if (am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p)
    }

    private fun schedule() {
        val am = getSystemService(AlarmManager::class.java)
        am.cancel(pi(0, BtOffReceiver::class.java))
        am.cancel(pi(1, AutoOffReceiver::class.java))

        val next = am.nextAlarmClock ?: return
        val triggerAt = next.triggerTime
        val now = System.currentTimeMillis()
        if (triggerAt <= now) return

        // выключить приложение сразу после срабатывания будильника
        val prefs = getSharedPreferences(BtUtil.PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(BtUtil.KEY_AUTO_OFF, true)) {
            setAlarm(am, triggerAt + BtUtil.AUTO_OFF_DELAY_MS, pi(1, AutoOffReceiver::class.java, triggerAt))
        }

        if (BtUtil.handled(this) == triggerAt) return

        val fireAt = triggerAt - BtUtil.MINUTES_BEFORE * 60_000L
        if (fireAt <= now) {
            // до будильника уже меньше 10 минут — выключаем сразу, один раз
            BtUtil.disableBluetooth(this)
            BtUtil.markHandled(this, triggerAt)
            return
        }
        setAlarm(am, fireAt, pi(0, BtOffReceiver::class.java, triggerAt))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        schedule()
        return START_STICKY
    }

    override fun onDestroy() {
        val am = getSystemService(AlarmManager::class.java)
        am.cancel(pi(0, BtOffReceiver::class.java))
        am.cancel(pi(1, AutoOffReceiver::class.java))
        unregisterReceiver(alarmChangedReceiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
