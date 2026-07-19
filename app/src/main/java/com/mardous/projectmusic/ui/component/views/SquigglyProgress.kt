package com.mardous.projectmusic.ui.component.views

/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import com.mardous.projectmusic.util.ProgressBarStyle

private const val TWO_PI = (Math.PI * 2f).toFloat()

@VisibleForTesting
internal const val DISABLED_ALPHA = 77

class SquigglyProgress : Drawable() {

    private val wavePaint = Paint()
    private val linePaint = Paint()
    private val glowPaint = Paint()
    private val path = Path()
    private var heightFraction = 1f
    private var heightAnimator: ValueAnimator? = null
    private var phaseOffset = 0f
    private var lastFrameTime = -1L

    var style: ProgressBarStyle = ProgressBarStyle.LINEAR
        set(value) {
            field = value
            invalidateSelf()
        }

    /* distance over which amplitude drops to zero, measured in wavelengths */
    private val transitionPeriods = 1.5f

    /* wave endpoint as percentage of bar when play position is zero */
    private val minWaveEndpoint = 0f

    /* wave endpoint as percentage of bar when play position matches wave endpoint */
    private val matchedWaveEndpoint = 0f

    // Horizontal length of the sine wave
    var waveLength = 55f

    // Height of each peak of the sine wave
    var lineAmplitude = 6f
        set(value) {
            field = value
            invalidateSelf()
        }

    // Line speed in px per second
    var phaseSpeed = 16f
        set(value) {
            field = value
            invalidateSelf()
        }

    // Progress stroke width, both for wave and solid line
    var strokeWidth = 8f
        set(value) {
            if (field == value) {
                return
            }
            field = value
            wavePaint.strokeWidth = value
            linePaint.strokeWidth = value
            invalidateSelf()
        }

    // Enables a transition region where the amplitude
    // of the wave is reduced linearly across it.
    var transitionEnabled = true
        set(value) {
            field = value
            invalidateSelf()
        }

    init {
        wavePaint.strokeCap = Paint.Cap.ROUND
        linePaint.strokeCap = Paint.Cap.ROUND
        wavePaint.strokeWidth = strokeWidth
        linePaint.strokeWidth = strokeWidth
        linePaint.style = Paint.Style.STROKE
        wavePaint.style = Paint.Style.STROKE
        linePaint.alpha = DISABLED_ALPHA

        glowPaint.style = Paint.Style.FILL
    }

    var animate: Boolean = true
        set(value) {
            if (field == value) {
                return
            }
            field = value
            if (field) {
                lastFrameTime = SystemClock.uptimeMillis()
            }
            heightAnimator?.cancel()
            heightAnimator =
                ValueAnimator.ofFloat(heightFraction, if (animate) 1f else 0f).apply {
                    if (animate) {
                        startDelay = 60
                        duration = 800
//                        interpolator = Interpolators.EMPHASIZED_DECELERATE
                    } else {
                        duration = 550
//                        interpolator = Interpolators.STANDARD_DECELERATE
                    }
                    addUpdateListener {
                        heightFraction = it.animatedValue as Float
                        invalidateSelf()
                    }
                    addListener(
                        object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                heightAnimator = null
                            }
                        }
                    )
                    start()
                }
        }

    override fun draw(canvas: Canvas) {
        if (animate && style == ProgressBarStyle.WAVY) {
            invalidateSelf()
            val now = SystemClock.uptimeMillis()
            phaseOffset += (now - lastFrameTime) / 1000f * phaseSpeed
            phaseOffset %= waveLength
            lastFrameTime = now
        }

        val progress = level / 10_000f
        val totalWidth = bounds.width().toFloat()
        val totalProgressPx = totalWidth * progress

        canvas.save()
        canvas.translate(bounds.left.toFloat(), bounds.centerY().toFloat())

        when (style) {
            ProgressBarStyle.LINEAR -> drawLinear(canvas, totalWidth, totalProgressPx)
            ProgressBarStyle.WAVY -> drawWavy(canvas, totalWidth, totalProgressPx, progress)
            ProgressBarStyle.ROUNDED -> drawRounded(canvas, totalWidth, totalProgressPx)
            ProgressBarStyle.THIN -> drawThin(canvas, totalWidth, totalProgressPx)
            ProgressBarStyle.THICK -> drawThick(canvas, totalWidth, totalProgressPx)
            ProgressBarStyle.GRADIENT -> drawGradient(canvas, totalWidth, totalProgressPx)
            ProgressBarStyle.SEGMENTED -> drawSegmented(canvas, totalWidth, totalProgressPx)
            ProgressBarStyle.DOTS -> drawDots(canvas, totalWidth, totalProgressPx)
            ProgressBarStyle.GLOW -> drawGlow(canvas, totalWidth, totalProgressPx)
        }

        canvas.restore()
    }

    override fun onLevelChange(level: Int): Boolean {
        invalidateSelf()
        return true
    }

    private fun drawLinear(canvas: Canvas, totalWidth: Float, totalProgressPx: Float) {
        canvas.drawLine(0f, 0f, totalProgressPx, 0f, wavePaint)
        canvas.drawLine(totalProgressPx, 0f, totalWidth, 0f, linePaint)
    }

    private fun drawWavy(canvas: Canvas, totalWidth: Float, totalProgressPx: Float, progress: Float) {
        var effectiveTotalWidth = totalWidth
        if (transitionEnabled) {
            effectiveTotalWidth -= transitionPeriods * waveLength
        }
        
        val waveProgressPx =
            effectiveTotalWidth *
                    (if (!transitionEnabled || progress > matchedWaveEndpoint) progress
                    else
                        lerp(
                            minWaveEndpoint,
                            matchedWaveEndpoint,
                            lerpInv(0f, matchedWaveEndpoint, progress)
                        ))

        // Build Wiggly Path
        val waveStart = -phaseOffset - waveLength / 2f
        val waveEnd = if (transitionEnabled) totalWidth else waveProgressPx

        // helper function, computes amplitude for wave segment
        val computeAmplitude: (Float, Float) -> Float = { x, sign ->
            if (transitionEnabled) {
                val length = transitionPeriods * waveLength
                val coeff =
                    lerpInvSat(waveProgressPx + length / 2f, waveProgressPx - length / 2f, x)
                sign * heightFraction * lineAmplitude * coeff
            } else {
                sign * heightFraction * lineAmplitude
            }
        }

        // Reset path object to the start
        path.rewind()
        path.moveTo(waveStart, 0f)

        // Build the wave, incrementing by half the wavelength each time
        var currentX = waveStart
        var waveSign = 1.2f
        var currentAmp = computeAmplitude(currentX, waveSign)
        val dist = waveLength / 2f
        while (currentX < waveEnd) {
            waveSign = -waveSign
            val nextX = currentX + dist
            val midX = currentX + dist / 2
            val nextAmp = computeAmplitude(nextX, waveSign)
            path.cubicTo(midX, currentAmp, midX, nextAmp, nextX, nextAmp)
            currentAmp = nextAmp
            currentX = nextX
        }

        val clipTop = lineAmplitude + strokeWidth
        // Draw path up to progress position
        canvas.save()
        canvas.clipRect(0f, -1f * clipTop, totalProgressPx, clipTop)
        canvas.drawPath(path, wavePaint)
        canvas.restore()

        if (transitionEnabled) {
            canvas.save()
            canvas.clipRect(totalProgressPx, -1f * clipTop, totalWidth, clipTop)
            canvas.drawPath(path, linePaint)
            canvas.restore()
        } else {
            canvas.drawLine(totalProgressPx, 0f, totalWidth, 0f, linePaint)
        }
    }

    private fun drawRounded(canvas: Canvas, totalWidth: Float, totalProgressPx: Float) {
        val height = strokeWidth.coerceAtMost(16f) // Cap height for elegance
        val rect = RectF(0f, -height / 2, totalWidth, height / 2)
        
        // Background
        canvas.drawRoundRect(rect, height / 2, height / 2, linePaint)
        
        // Progress with subtle glow
        if (totalProgressPx > 0) {
            val progressRect = RectF(0f, -height / 2, totalProgressPx, height / 2)
            
            // Draw a subtle outer glow
            if (animate) {
                glowPaint.shader = RadialGradient(
                    totalProgressPx, 0f, 20f,
                    intArrayOf(wavePaint.color.withAlpha(0.3f), Color.TRANSPARENT),
                    null, Shader.TileMode.CLAMP
                )
                canvas.drawCircle(totalProgressPx, 0f, 20f, glowPaint)
            }
            
            canvas.drawRoundRect(progressRect, height / 2, height / 2, wavePaint)
        }
    }

    private fun drawThin(canvas: Canvas, totalWidth: Float, totalProgressPx: Float) {
        val thinWidth = 4f
        val originalWidth = wavePaint.strokeWidth
        wavePaint.strokeWidth = thinWidth
        linePaint.strokeWidth = thinWidth
        
        canvas.drawLine(0f, 0f, totalProgressPx, 0f, wavePaint)
        canvas.drawLine(totalProgressPx, 0f, totalWidth, 0f, linePaint)
        
        wavePaint.strokeWidth = originalWidth
        linePaint.strokeWidth = originalWidth
    }

    private fun drawThick(canvas: Canvas, totalWidth: Float, totalProgressPx: Float) {
        val height = strokeWidth.coerceAtMost(24f) // Expressive thickness cap
        val rect = RectF(0f, -height / 2, totalWidth, height / 2)
        
        // Background
        canvas.drawRoundRect(rect, 8f, 8f, linePaint)
        
        // Progress
        if (totalProgressPx > 0) {
            val progressRect = RectF(0f, -height / 2, totalProgressPx, height / 2)
            canvas.drawRoundRect(progressRect, 8f, 8f, wavePaint)
        }
    }

    private fun drawGradient(canvas: Canvas, totalWidth: Float, totalProgressPx: Float) {
        val color1 = wavePaint.color
        val color2 = color1.shiftHue(30f)
        val color3 = color1.shiftHue(-30f)
        
        val colors = intArrayOf(color3, color1, color2)
        val gradient = LinearGradient(0f, 0f, totalProgressPx.coerceAtLeast(1f), 0f, colors, null, Shader.TileMode.CLAMP)
        
        val originalShader = wavePaint.shader
        wavePaint.shader = gradient
        
        canvas.drawLine(0f, 0f, totalProgressPx, 0f, wavePaint)
        canvas.drawLine(totalProgressPx, 0f, totalWidth, 0f, linePaint)
        
        wavePaint.shader = originalShader
    }

    private fun drawSegmented(canvas: Canvas, totalWidth: Float, totalProgressPx: Float) {
        val height = strokeWidth.coerceAtMost(16f)
        val segmentWidth = 24f
        val gap = 8f
        val step = segmentWidth + gap
        
        var currentX = 0f
        while (currentX < totalWidth) {
            val endX = (currentX + segmentWidth).coerceAtMost(totalWidth)
            val rect = RectF(currentX, -height / 2, endX, height / 2)
            
            val paint = if (currentX + segmentWidth / 2 <= totalProgressPx) wavePaint else linePaint
            canvas.drawRoundRect(rect, 4f, 4f, paint)
            currentX += step
        }
    }

    private fun drawDots(canvas: Canvas, totalWidth: Float, totalProgressPx: Float) {
        val dotRadius = 5f
        val gap = 15f
        val step = dotRadius * 2 + gap
        
        var currentX = dotRadius
        while (currentX < totalWidth) {
            val paint = if (currentX <= totalProgressPx) wavePaint else linePaint
            canvas.drawCircle(currentX, 0f, dotRadius, paint)
            currentX += step
        }
    }

    private fun drawGlow(canvas: Canvas, totalWidth: Float, totalProgressPx: Float) {
        val trackHeight = 4f
        
        // Draw standard background
        linePaint.strokeWidth = trackHeight
        canvas.drawLine(totalProgressPx, 0f, totalWidth, 0f, linePaint)
        
        // Draw standard progress
        wavePaint.strokeWidth = trackHeight
        canvas.drawLine(0f, 0f, totalProgressPx, 0f, wavePaint)
        
        // Draw Glow at the tip
        if (totalProgressPx > 0) {
            val glowRadius = 24f
            val gradient = RadialGradient(
                totalProgressPx, 0f, glowRadius,
                intArrayOf(wavePaint.color.withAlpha(0.6f), Color.TRANSPARENT),
                null, Shader.TileMode.CLAMP
            )
            glowPaint.shader = gradient
            canvas.drawCircle(totalProgressPx, 0f, glowRadius, glowPaint)
            
            // Bright spot at the tip
            canvas.drawCircle(totalProgressPx, 0f, 4f, wavePaint)
        }
        
        // Reset stroke widths
        wavePaint.strokeWidth = strokeWidth
        linePaint.strokeWidth = strokeWidth
    }

    override fun getIntrinsicHeight(): Int {
        return (lineAmplitude * 2 + strokeWidth).toInt().coerceAtLeast(32)
    }

    override fun getIntrinsicWidth(): Int {
        return -1
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        wavePaint.colorFilter = colorFilter
        linePaint.colorFilter = colorFilter
    }

    override fun setAlpha(alpha: Int) {
        updateColors(wavePaint.color, alpha)
    }

    override fun getAlpha(): Int {
        return wavePaint.alpha
    }

    override fun setTint(tintColor: Int) {
        updateColors(tintColor, alpha)
    }

    override fun setTintList(tint: ColorStateList?) {
        if (tint == null) {
            return
        }
        updateColors(tint.defaultColor, alpha)
    }

    private fun updateColors(tintColor: Int, alpha: Int) {
        wavePaint.color = tintColor.withAlpha(alpha / 255f)
        linePaint.color = tintColor.withAlpha((DISABLED_ALPHA * (alpha / 255f)) / 255f)
    }

    private fun Int.withAlpha(alpha: Float): Int {
        val a = (Color.alpha(this) * alpha).toInt().coerceIn(0, 255)
        return Color.argb(a, Color.red(this), Color.green(this), Color.blue(this))
    }

    private fun Int.shiftHue(amount: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(this, hsv)
        hsv[0] = (hsv[0] + amount) % 360
        return Color.HSVToColor(Color.alpha(this), hsv)
    }

    fun constrain(amount: Float, low: Float, high: Float): Float {
        return if (amount < low) low else if (amount > high) high else amount
    }

    private fun lerp(start: Float, stop: Float, amount: Float): Float {
        return start + (stop - start) * amount
    }

    /**
     * Returns the interpolation scalar (s) that satisfies the equation: `value = `[ ][.lerp]`(a, b, s)`
     *
     *
     * If `a == b`, then this function will return 0.
     */
    fun lerpInv(a: Float, b: Float, value: Float): Float {
        return if (a != b) (value - a) / (b - a) else 0.0f
    }

    /** Returns the single argument constrained between [0.0, 1.0].  */
    fun saturate(value: Float): Float {
        return constrain(value, 0.0f, 1.0f)
    }

    /** Returns the saturated (constrained between [0, 1]) result of [.lerpInv].  */
    fun lerpInvSat(a: Float, b: Float, value: Float): Float {
        return saturate(lerpInv(a, b, value))
    }
}