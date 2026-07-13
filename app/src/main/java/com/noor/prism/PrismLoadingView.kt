package com.noor.prism

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class PrismLoadingView(context: Context) : View(context) {

    private data class Star(val x: Float, val y: Float, val radius: Float, val phase: Float, val speed: Float)

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 22f * resources.displayMetrics.scaledDensity
        typeface = android.graphics.Typeface.create("sans", android.graphics.Typeface.NORMAL)
    }
    private val stars = List(72) {
        Star(
            x = Random.nextFloat(),
            y = Random.nextFloat(),
            radius = 0.7f + Random.nextFloat() * 2.2f,
            phase = Random.nextFloat() * (2f * PI.toFloat()),
            speed = 0.45f + Random.nextFloat() * 1.1f
        )
    }
    private var phase = 0f
    private val animator = ValueAnimator.ofFloat(0f, 2f * PI.toFloat()).apply {
        duration = 5200L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            phase = it.animatedValue as Float
            invalidate()
        }
    }

    init {
        isClickable = true
        isFocusable = false
        setBackgroundColor(Color.rgb(4, 8, 28))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!animator.isStarted) animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        backgroundPaint.shader = LinearGradient(
            0f, 0f, w, h,
            intArrayOf(Color.rgb(3, 8, 28), Color.rgb(16, 12, 55), Color.rgb(3, 12, 38)),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, backgroundPaint)

        glowPaint.shader = RadialGradient(
            w * 0.5f,
            h * 0.47f,
            minOf(w, h) * 0.24f,
            intArrayOf(Color.argb(58, 90, 215, 255), Color.argb(24, 205, 110, 255), Color.TRANSPARENT),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(w * 0.5f, h * 0.47f, minOf(w, h) * 0.24f, glowPaint)

        stars.forEach { star ->
            val alpha = (95 + 150 * ((sin(phase * star.speed + star.phase) + 1f) / 2f)).toInt()
            starPaint.color = Color.argb(alpha.coerceIn(50, 245), 225, 242, 255)
            canvas.drawCircle(star.x * w, star.y * h, star.radius * resources.displayMetrics.density, starPaint)
        }

        val pulse = 0.92f + 0.08f * ((sin(phase) + 1f) / 2f)
        textPaint.alpha = (210 + 45 * pulse).toInt().coerceIn(0, 255)
        canvas.drawText("Loading Prism…", w / 2f, h * 0.57f, textPaint)
    }
}
