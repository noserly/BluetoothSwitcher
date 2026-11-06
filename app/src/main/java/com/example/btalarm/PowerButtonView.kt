package com.example.btalarm

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.MotionEvent
import android.view.View

class PowerButtonView(ctx: Context) : View(ctx) {

    private val offColor = 0xFF6B6B72.toInt()
    private val onColor = 0xFF4ADE80.toInt()
    private var ringColor = offColor
    private var glow = 0f
    var active = false
        private set

    private fun dp(v: Int) = v * resources.displayMetrics.density

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = dp(4) }
    private val icon = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeWidth = dp(8)
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null) // нужно для свечения (shadow layer)
        setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(90).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
            }
            false
        }
    }

    fun setActive(on: Boolean, animate: Boolean) {
        active = on
        val target = if (on) onColor else offColor
        val glowTarget = if (on) 1f else 0f
        if (!animate) {
            ringColor = target; glow = glowTarget; invalidate(); return
        }
        val from = ringColor
        val g0 = glow
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            addUpdateListener {
                val t = it.animatedFraction
                ringColor = ArgbEvaluator().evaluate(t, from, target) as Int
                glow = g0 + (glowTarget - g0) * t
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(c: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = minOf(width, height) / 2f - dp(28)

        fill.shader = RadialGradient(cx, cy - r / 3, r * 1.5f, 0xFF36363C.toInt(), 0xFF1B1B1F.toInt(), Shader.TileMode.CLAMP)
        if (glow > 0f) {
            val a = (0xB0 * glow).toInt()
            fill.setShadowLayer(dp(26) * glow, 0f, 0f, (a shl 24) or (ringColor and 0xFFFFFF))
        } else fill.clearShadowLayer()
        c.drawCircle(cx, cy, r, fill)
        fill.clearShadowLayer()

        ring.color = ringColor
        c.drawCircle(cx, cy, r, ring)

        icon.color = ringColor
        val ir = r * 0.36f
        c.drawArc(RectF(cx - ir, cy - ir, cx + ir, cy + ir), -55f, 290f, false, icon)
        c.drawLine(cx, cy - ir * 1.15f, cx, cy - ir * 0.1f, icon)
    }
}
