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
    const val KEY_BT_ON_AFTER = "bt_on_after"
    const val KEY_WE_DISABLED = "we_disabled"
    const val MINUTES_BEFORE = 10L
    const val AFTER_ALARM_DELAY_MS = 30_000L

    private fun hasPermission(ctx: Context) =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ctx.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** true, если Bluetooth был включён и мы его выключили */
    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    fun disableBluetooth(ctx: Context): Boolean {
        if (!hasPermission(ctx)) {
            Log.w("BtAlarm", "Нет разрешения BLUETOOTH_CONNECT")
            return false
        }
        val adapter = (ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (adapter != null && adapter.isEnabled) {
            val ok = adapter.disable()
            Log.i("BtAlarm", "Bluetooth disable() -> $ok")
            return ok
        }
        return false
    }

    /** Выключить Bluetooth перед будильником и запомнить, что это сделали мы */
    fun turnOffForAlarm(ctx: Context, triggerAt: Long) {
        val ok = disableBluetooth(ctx)
        prefs(ctx).edit()
            .putBoolean(KEY_WE_DISABLED, ok)
            .putLong(KEY_HANDLED, triggerAt)
            .apply()
    }

    /** Включить Bluetooth обратно — только если его выключило само приложение */
    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    fun restoreBluetooth(ctx: Context) {
        val p = prefs(ctx)
        if (!p.getBoolean(KEY_WE_DISABLED, false) || !hasPermission(ctx)) return
        val adapter = (ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (adapter != null && !adapter.isEnabled) {
            Log.i("BtAlarm", "Bluetooth enable() -> ${adapter.enable()}")
        }
        p.edit().putBoolean(KEY_WE_DISABLED, false).apply()
    }

    fun handled(ctx: Context): Long = prefs(ctx).getLong(KEY_HANDLED, 0L)
}
