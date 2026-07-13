package com.noor.prism

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Native, fully offline Prism intro. */
class PrismLoadingView(context: Context) : View(context) {

    private data class Star(
        val x: Float,
        val y: Float,
        val radius: Float,
        val phase: Float,
        val speed: Float,
        val drift: Float
    )

    private val density = resources.displayMetrics.density
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.2f * density
    }
    private val logoPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val logoBitmap = BitmapFactory.decodeResource(resources, R.drawable.app_icon)
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 22f * resources.displayMetrics.scaledDensity
        typeface = android.graphics.Typeface.create("sans", android.graphics.Typeface.NORMAL)
        letterSpacing = 0.025f
    }

    private val stars = List(96) {
        Star(
            x = Random.nextFloat(),
            y = Random.nextFloat(),
            radius = 0.7f + Random.nextFloat() * 2.8f,
            phase = Random.nextFloat() * (2f * PI.toFloat()),
            speed = 0.55f + Random.nextFloat() * 1.65f,
            drift = -0.012f + Random.nextFloat() * 0.024f
        )
    }

    private var phase = 0f
    private val animator = ValueAnimator.ofFloat(0f, 2f * PI.toFloat()).apply {
        duration = 4600L
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
        setBackgroundColor(Color.rgb(3, 7, 25))
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
        if (w <= 0f || h <= 0f) return

        backgroundPaint.shader = LinearGradient(
            0f, 0f, w, h,
            intArrayOf(
                Color.rgb(2, 7, 26),
                Color.rgb(17, 10, 57),
                Color.rgb(4, 17, 48),
                Color.rgb(3, 8, 29)
            ),
            floatArrayOf(0f, 0.38f, 0.72f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, backgroundPaint)

        drawStars(canvas, w, h)
        drawShootingStars(canvas, w, h)
        drawPrism(canvas, w, h)
        drawParticles(canvas, w, h)
        drawLoadingText(canvas, w, h)
    }

    private fun drawStars(canvas: Canvas, w: Float, h: Float) {
        stars.forEachIndexed { index, star ->
            val twinkle = ((sin(phase * star.speed + star.phase) + 1f) / 2f)
            val alpha = (80 + 175 * twinkle).toInt().coerceIn(55, 255)
            val x = ((star.x + star.drift * phase) % 1f + 1f) % 1f * w
            val y = star.y * h
            val radius = star.radius * density * (0.85f + twinkle * 0.45f)
            starPaint.color = when (index % 5) {
                0 -> Color.argb(alpha, 185, 232, 255)
                1 -> Color.argb(alpha, 236, 216, 255)
                else -> Color.argb(alpha, 245, 249, 255)
            }
            canvas.drawCircle(x, y, radius, starPaint)
            if (radius > 2.2f * density && twinkle > 0.65f) {
                starPaint.strokeWidth = 0.8f * density
                canvas.drawLine(x - radius * 2.3f, y, x + radius * 2.3f, y, starPaint)
                canvas.drawLine(x, y - radius * 2.3f, x, y + radius * 2.3f, starPaint)
            }
        }
    }

    private fun drawShootingStars(canvas: Canvas, w: Float, h: Float) {
        val cycle = (phase / (2f * PI.toFloat()))
        repeat(2) { index ->
            val local = (cycle + index * 0.47f) % 1f
            if (local < 0.24f) {
                val progress = local / 0.24f
                val startX = if (index == 0) w * 0.18f else w * 0.68f
                val startY = if (index == 0) h * 0.18f else h * 0.29f
                val x = startX + progress * w * 0.25f
                val y = startY + progress * h * 0.11f
                val fade = sin(progress * PI).toFloat().coerceAtLeast(0f)
                particlePaint.shader = LinearGradient(
                    x - w * 0.09f, y - h * 0.04f, x, y,
                    Color.TRANSPARENT,
                    Color.argb((235 * fade).toInt(), 190, 235, 255),
                    Shader.TileMode.CLAMP
                )
                particlePaint.strokeWidth = 2f * density
                canvas.drawLine(x - w * 0.09f, y - h * 0.04f, x, y, particlePaint)
                particlePaint.shader = null
                particlePaint.color = Color.argb((255 * fade).toInt(), 245, 250, 255)
                canvas.drawCircle(x, y, 2.4f * density, particlePaint)
            }
        }
    }

    private fun drawPrism(canvas: Canvas, w: Float, h: Float) {
        val cx = w * 0.5f
        val cy = h * 0.43f
        val base = minOf(w, h) * 0.145f
        val pulse = 0.94f + 0.065f * ((sin(phase * 1.15f) + 1f) / 2f)
        val radius = base * pulse

        glowPaint.shader = RadialGradient(
            cx, cy, radius * 2.25f,
            intArrayOf(
                Color.argb(95, 81, 209, 255),
                Color.argb(55, 185, 86, 255),
                Color.argb(20, 255, 191, 92),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.35f, 0.67f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, radius * 2.25f, glowPaint)

        val ringRect = RectF(cx - radius * 1.35f, cy - radius * 1.35f, cx + radius * 1.35f, cy + radius * 1.35f)
        ringPaint.shader = LinearGradient(
            ringRect.left, ringRect.top, ringRect.right, ringRect.bottom,
            intArrayOf(
                Color.rgb(92, 229, 255),
                Color.rgb(195, 111, 255),
                Color.rgb(255, 191, 92),
                Color.rgb(92, 229, 255)
            ),
            null,
            Shader.TileMode.CLAMP
        )
        ringPaint.alpha = (155 + 85 * ((sin(phase * 1.4f) + 1f) / 2f)).toInt()
        canvas.save()
        canvas.rotate((phase * 180f / PI.toFloat()) * 0.18f, cx, cy)
        canvas.drawArc(ringRect, 12f, 125f, false, ringPaint)
        canvas.drawArc(ringRect, 175f, 105f, false, ringPaint)
        canvas.restore()

        val logoRadius = radius * 1.08f
        val logoRect = RectF(
            cx - logoRadius,
            cy - logoRadius,
            cx + logoRadius,
            cy + logoRadius
        )
        logoPaint.alpha = (225 + 30 * ((sin(phase * 1.15f) + 1f) / 2f)).toInt().coerceIn(0, 255)
        canvas.drawBitmap(logoBitmap, null, logoRect, logoPaint)
    }

    private fun drawParticles(canvas: Canvas, w: Float, h: Float) {
        val cx = w * 0.5f
        val cy = h * 0.43f
        repeat(14) { i ->
            val angle = phase * (0.16f + i * 0.006f) + i * (2f * PI.toFloat() / 14f)
            val orbit = minOf(w, h) * (0.19f + (i % 3) * 0.018f)
            val x = cx + cos(angle) * orbit
            val y = cy + sin(angle) * orbit * 0.62f
            val alpha = 95 + (i % 4) * 32
            particlePaint.color = when (i % 3) {
                0 -> Color.argb(alpha, 126, 225, 255)
                1 -> Color.argb(alpha, 220, 155, 255)
                else -> Color.argb(alpha, 255, 220, 146)
            }
            canvas.drawCircle(x, y, (1.1f + (i % 3) * 0.45f) * density, particlePaint)
        }
    }

    private fun drawLoadingText(canvas: Canvas, w: Float, h: Float) {
        val shimmer = ((sin(phase * 1.45f) + 1f) / 2f)
        textPaint.alpha = (205 + 50 * shimmer).toInt().coerceIn(0, 255)
        textPaint.setShadowLayer((8f + 5f * shimmer) * density, 0f, 0f, Color.argb(170, 107, 218, 255))
        canvas.drawText("Loading Prism…", w / 2f, h * 0.665f, textPaint)
        textPaint.clearShadowLayer()
    }
}
