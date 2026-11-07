package com.example.btalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Срабатывает сразу после будильника: возвращает Bluetooth и/или выключает приложение. */
class AfterAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val p = context.getSharedPreferences(BtUtil.PREFS, Context.MODE_PRIVATE)
        if (p.getBoolean(BtUtil.KEY_BT_ON_AFTER, false)) BtUtil.restoreBluetooth(context)
        if (p.getBoolean(BtUtil.KEY_AUTO_OFF, true)) {
            p.edit().putBoolean(BtUtil.KEY_ENABLED, false).apply()
            context.stopService(Intent(context, AlarmWatcherService::class.java))
        }
    }
}
