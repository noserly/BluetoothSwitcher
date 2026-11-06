package com.example.btalarm

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private val bg = 0xFF121214.toInt()
    private val card = 0xFF1E1E22.toInt()
    private val textMain = 0xFFF2F2F5.toInt()
    private val textDim = 0xFF8E8E93.toInt()
    private val accent = 0xFF4ADE80.toInt()

    private lateinit var power: PowerButtonView
    private lateinit var status: TextView
    private lateinit var sub: TextView
    private lateinit var alarmValue: TextView
    private lateinit var autoOff: Switch

    private val prefs get() = getSharedPreferences(BtUtil.PREFS, Context.MODE_PRIVATE)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val fmtFull = SimpleDateFormat("dd.MM  HH:mm", Locale.getDefault())

    private fun tv(text: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(color)
        if (bold) typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg

        val title = tv("BT ALARM OFF", 14f, textDim, true).apply {
            letterSpacing = 0.25f; gravity = Gravity.CENTER
        }

        power = PowerButtonView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(260), dp(260))
            setOnClickListener { this@MainActivity.toggleWatching(!active) }
        }
        status = tv("", 26f, textMain, true).apply { gravity = Gravity.CENTER }
        sub = tv("", 15f, textDim).apply { gravity = Gravity.CENTER; setPadding(dp(24), dp(6), dp(24), 0) }

        val center = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            addView(power); addView(status); addView(sub)
        }

        // карточка внизу
        alarmValue = tv("", 18f, textMain, true)
        autoOff = Switch(this).apply {
            thumbTintList = ColorStateList.valueOf(Color.WHITE)
            trackTintList = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(accent, 0xFF4A4A50.toInt())
            )
            setOnCheckedChangeListener { b, on ->
                if (!b.isPressed) return@setOnCheckedChangeListener
                prefs.edit().putBoolean(BtUtil.KEY_AUTO_OFF, on).apply()
                if (prefs.getBoolean(BtUtil.KEY_ENABLED, false))
                    ContextCompat.startForegroundService(this@MainActivity, Intent(this@MainActivity, AlarmWatcherService::class.java))
            }
        }
        val divider = View(this).apply {
            setBackgroundColor(0xFF2C2C31.toInt())
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)).apply {
                topMargin = dp(14); bottomMargin = dp(14)
            }
        }
        val autoRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                addView(tv("Выключаться после будильника", 16f, textMain))
                addView(tv("Приложение само отключится, когда будильник прозвонит", 12f, textDim))
            })
            addView(autoOff)
        }
        val cardView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(18))
            background = GradientDrawable().apply { setColor(card); cornerRadius = dp(22).toFloat() }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(24)
            }
            addView(tv("Ближайший будильник", 13f, textDim))
            addView(alarmValue.apply { setPadding(0, dp(4), 0, 0) })
            addView(divider)
            addView(autoRow)
        }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(dp(20), dp(56), dp(20), dp(0))
            addView(title); addView(center); addView(cardView)
        })
    }

    override fun onResume() {
        super.onResume()
        autoOff.isChecked = prefs.getBoolean(BtUtil.KEY_AUTO_OFF, true)
        power.setActive(prefs.getBoolean(BtUtil.KEY_ENABLED, false), false)
        updateTexts()
    }

    private fun updateTexts() {
        val on = prefs.getBoolean(BtUtil.KEY_ENABLED, false)
        val next = getSystemService(AlarmManager::class.java).nextAlarmClock
        alarmValue.text = if (next == null) "Не установлен" else fmtFull.format(Date(next.triggerTime))
        status.text = if (on) "Включено" else "Выключено"
        status.setTextColor(if (on) accent else textMain)
        sub.text = when {
            !on -> "Нажми кнопку, чтобы включить"
            next == null -> "Жду, пока ты поставишь будильник"
            else -> "Bluetooth выключится в " +
                fmt.format(Date(next.triggerTime - BtUtil.MINUTES_BEFORE * 60_000L))
        }
    }

    private fun toggleWatching(on: Boolean) {
        if (!on) {
            prefs.edit().putBoolean(BtUtil.KEY_ENABLED, false).apply()
            stopService(Intent(this, AlarmWatcherService::class.java))
            power.setActive(false, true)
            updateTexts()
            return
        }
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 31) perms += Manifest.permission.BLUETOOTH_CONNECT
        if (Build.VERSION.SDK_INT >= 33) perms += Manifest.permission.POST_NOTIFICATIONS
        val missing = perms.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) startWatching() else requestPermissions(missing.toTypedArray(), 1)
    }

    override fun onRequestPermissionsResult(code: Int, p: Array<out String>, r: IntArray) {
        super.onRequestPermissionsResult(code, p, r)
        val btOk = Build.VERSION.SDK_INT < 31 ||
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        if (btOk) startWatching()
        else sub.text = "Нужно разрешение «Устройства поблизости», иначе Bluetooth не выключить"
    }

    private fun startWatching() {
        prefs.edit().putBoolean(BtUtil.KEY_ENABLED, true).apply()
        ContextCompat.startForegroundService(this, Intent(this, AlarmWatcherService::class.java))
        power.setActive(true, true)
        updateTexts()
    }
}
