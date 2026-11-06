package com.example.btalarm

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var info: TextView
    private lateinit var toggle: Switch
    private var updating = false

    private val prefs get() = getSharedPreferences(BtUtil.PREFS, Context.MODE_PRIVATE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = TextView(this).apply {
            text = "BT Alarm Off"; textSize = 26f; gravity = Gravity.CENTER
        }
        info = TextView(this).apply {
            textSize = 17f; gravity = Gravity.CENTER; setPadding(0, 48, 0, 48)
        }
        toggle = Switch(this).apply {
            text = "Выключать Bluetooth за ${BtUtil.MINUTES_BEFORE} мин до будильника  "
            textSize = 16f
            setOnCheckedChangeListener { _, on -> if (!updating) onToggle(on) }
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            addView(title); addView(info); addView(toggle)
        })
    }

    override fun onResume() {
        super.onResume()
        updating = true
        toggle.isChecked = prefs.getBoolean(BtUtil.KEY_ENABLED, false)
        updating = false
        refreshInfo()
    }

    private fun refreshInfo() {
        val next = getSystemService(AlarmManager::class.java).nextAlarmClock
        val on = prefs.getBoolean(BtUtil.KEY_ENABLED, false)
        info.text = (if (next == null) "Будильников нет"
        else "Ближайший будильник:\n" +
            SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(next.triggerTime))) +
            "\n\nСтатус: " + if (on) "включено" else "выключено"
    }

    private fun onToggle(on: Boolean) {
        if (!on) {
            prefs.edit().putBoolean(BtUtil.KEY_ENABLED, false).apply()
            stopService(Intent(this, AlarmWatcherService::class.java))
            refreshInfo()
            return
        }
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 31) perms += Manifest.permission.BLUETOOTH_CONNECT
        if (Build.VERSION.SDK_INT >= 33) perms += Manifest.permission.POST_NOTIFICATIONS
        val missing = perms.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) enable() else requestPermissions(missing.toTypedArray(), 1)
    }

    override fun onRequestPermissionsResult(code: Int, p: Array<out String>, r: IntArray) {
        super.onRequestPermissionsResult(code, p, r)
        val btOk = Build.VERSION.SDK_INT < 31 ||
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        if (btOk) enable()
        else {
            updating = true; toggle.isChecked = false; updating = false
            info.text = "Без разрешения «Устройства поблизости» приложение не может выключать Bluetooth"
        }
    }

    private fun enable() {
        prefs.edit().putBoolean(BtUtil.KEY_ENABLED, true).apply()
        ContextCompat.startForegroundService(this, Intent(this, AlarmWatcherService::class.java))
        refreshInfo()
    }
}
