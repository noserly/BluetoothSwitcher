package com.example.btalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BtOffReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val triggerAt = intent.getLongExtra("trigger_at", 0L)
        BtUtil.disableBluetooth(context)
        BtUtil.markHandled(context, triggerAt)
    }
}
