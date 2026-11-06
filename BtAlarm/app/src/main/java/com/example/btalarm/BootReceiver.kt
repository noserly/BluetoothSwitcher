package com.example.btalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val on = context.getSharedPreferences(BtUtil.PREFS, Context.MODE_PRIVATE).getBoolean(BtUtil.KEY_ENABLED, false)
        if (on && intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ContextCompat.startForegroundService(context, Intent(context, AlarmWatcherService::class.java))
        }
    }
}
