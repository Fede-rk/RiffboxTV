package com.riffbox.tv.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.random.Random

class AudioVisualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val barCount = 28
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val barRect = RectF()
    private val barHeights = FloatArray(barCount) { 0.2f }

    private var simulatedAnimator: ValueAnimator? = null

    init {
        startSimulatedBeat()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(0xFFFF6B2B.toInt(), 0xFFE6264A.toInt(), 0xFF00E5FF.toInt()),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        barPaint.shader = shader
    }

    fun linkAudioSession(audioSessionId: Int) {
        startSimulatedBeat()
    }

    private fun startSimulatedBeat() {
        if (simulatedAnimator?.isRunning == true) return
        simulatedAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 140
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                for (i in 0 until barCount) {
                    val rand = Random.nextFloat() * 0.85f + 0.15f
                    barHeights[i] = barHeights[i] * 0.6f + rand * 0.4f
                }
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val totalSpacing = w * 0.25f
        val barWidth = (w - totalSpacing) / barCount
        val spacing = totalSpacing / (barCount - 1)

        for (i in 0 until barCount) {
            val left = i * (barWidth + spacing)
            val barH = h * barHeights[i]
            val top = h - barH
            val right = left + barWidth
            val bottom = h

            barRect.set(left, top, right, bottom)
            canvas.drawRoundRect(barRect, 6f, 6f, barPaint)
        }
    }

    fun release() {
        simulatedAnimator?.cancel()
        simulatedAnimator = null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        release()
    }
}
