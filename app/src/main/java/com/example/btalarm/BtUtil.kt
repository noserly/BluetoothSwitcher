package com.example.btalarm

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

object BtUtil {
    const val PREFS = "btalarm"
    const val KEY_HANDLED = "handled_trigger"
    const val KEY_ENABLED = "enabled"
    const val KEY_AUTO_OFF = "auto_off"
    const val AUTO_OFF_DELAY_MS = 30_000L
    const val MINUTES_BEFORE = 10L

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    fun disableBluetooth(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ctx.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("BtAlarm", "Нет разрешения BLUETOOTH_CONNECT")
            return
        }
        val adapter = (ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (adapter != null && adapter.isEnabled) {
            val ok = adapter.disable()
            Log.i("BtAlarm", "Bluetooth disable() -> $ok")
        }
    }

    fun markHandled(ctx: Context, triggerAt: Long) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong(KEY_HANDLED, triggerAt).apply()
    }

    fun handled(ctx: Context): Long =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_HANDLED, 0L)
}
