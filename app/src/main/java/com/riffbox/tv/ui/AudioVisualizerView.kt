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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Premium Analog-Style Audio Spectrum Visualizer
 *
 * Implements smooth harmonic wave physics and musical rhythm (120 BPM beat envelope)
 * with attack/decay inertia, providing an organic, mesmerizing McIntosh / vacuum-tube
 * VU-meter aesthetic without requiring intrusive microphone permissions.
 */
class AudioVisualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val barCount = 28
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val barRect = RectF()

    // Current displayed bar heights [0f..1f]
    private val currentHeights = FloatArray(barCount) { 0.15f }
    // Target heights calculated from wave harmonics
    private val targetHeights = FloatArray(barCount) { 0.15f }

    private var timeSeconds = 0f
    private var visualizerAnimator: ValueAnimator? = null

    init {
        startRhythmicEngine()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Warm fiery orange to neon crimson and deep cyan gradient
        val shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(0xFFFF6B2B.toInt(), 0xFFE6264A.toInt(), 0xFF00E5FF.toInt()),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        barPaint.shader = shader
    }

    fun linkAudioSession(audioSessionId: Int) {
        startRhythmicEngine()
    }

    private fun startRhythmicEngine() {
        if (visualizerAnimator?.isRunning == true) return

        // 60 FPS smooth physics loop
        visualizerAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                updateHarmonics(0.024f) // ~24ms timestep for liquid smoothness
                invalidate()
            }
            start()
        }
    }

    private fun updateHarmonics(dt: Float) {
        timeSeconds += dt

        // Musical 120 BPM base tempo (2 Hz beat frequency)
        val beatPhase = timeSeconds * 2.0f * Math.PI.toFloat()
        // Strong rhythmic bass kick pulse on every quarter note
        val kickPulse = max(0f, sin(beatPhase)).let { it * it * it }

        for (i in 0 until barCount) {
            val normalizedIndex = i.toFloat() / (barCount - 1) // 0.0 (bass) to 1.0 (treble)

            // 1. Bass Frequency Region (Bars 0..7): Heavy kick bounce and low rumble
            val bassComponent = if (i < 8) {
                val bassWeight = (1.0f - (i / 8f))
                kickPulse * 0.65f * bassWeight + abs(sin(timeSeconds * 3.5f + i * 0.4f)) * 0.35f
            } else 0f

            // 2. Midrange Frequency Region (Bars 6..19): Vocal & Guitar harmonic traveling waves
            val midWeight = 1.0f - abs(normalizedIndex - 0.45f) * 2f
            val midWave = if (midWeight > 0f) {
                (sin(timeSeconds * 5.2f + i * 0.35f) * 0.5f + 0.5f) *
                (cos(timeSeconds * 2.8f - i * 0.2f) * 0.5f + 0.5f) * midWeight * 0.75f
            } else 0f

            // 3. High Frequency Region (Bars 18..27): Shimmering hi-hats & cymbals
            val trebleWeight = max(0f, (normalizedIndex - 0.6f) / 0.4f)
            val trebleShimmer = if (trebleWeight > 0f) {
                (sin(timeSeconds * 8.5f + i * 0.8f) * 0.5f + 0.5f) * trebleWeight * 0.55f
            } else 0f

            // Ambient breathing baseline so bars never completely collapse
            val baseline = 0.12f + sin(timeSeconds * 1.5f + i * 0.2f) * 0.04f

            // Composite target height
            val rawTarget = min(0.96f, max(baseline, bassComponent + midWave + trebleShimmer))
            targetHeights[i] = rawTarget

            // Analog VU-Meter Inertia (Instant Attack, Fluid Exponential Decay)
            val current = currentHeights[i]
            if (rawTarget > current) {
                // Fast Attack (reacts instantly to musical hits)
                currentHeights[i] = current + (rawTarget - current) * 0.38f
            } else {
                // Smooth Liquid Decay (drops gently with inertia)
                currentHeights[i] = current + (rawTarget - current) * 0.16f
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val totalSpacing = w * 0.25f
        val barWidth = (w - totalSpacing) / barCount
        val spacing = totalSpacing / (barCount - 1)

        for (i in 0 until barCount) {
            val left = i * (barWidth + spacing)
            val barH = h * currentHeights[i]
            val top = h - barH
            val right = left + barWidth
            val bottom = h

            barRect.set(left, top, right, bottom)
            // Rounded capsule style bars
            val radius = barWidth * 0.45f
            canvas.drawRoundRect(barRect, radius, radius, barPaint)
        }
    }

    fun release() {
        visualizerAnimator?.cancel()
        visualizerAnimator = null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        release()
    }
}
