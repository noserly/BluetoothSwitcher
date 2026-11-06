package com.example.btalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Срабатывает сразу после будильника: выключает слежение и сам переключатель. */
class AutoOffReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        context.getSharedPreferences(BtUtil.PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(BtUtil.KEY_ENABLED, false).apply()
        context.stopService(Intent(context, AlarmWatcherService::class.java))
    }
}
