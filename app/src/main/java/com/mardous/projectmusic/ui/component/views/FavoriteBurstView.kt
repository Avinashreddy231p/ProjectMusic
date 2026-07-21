package com.mardous.projectmusic.ui.component.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.graphics.toColorInt
import kotlin.math.cos
import kotlin.math.sin

class FavoriteBurstView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private var progress = 0f
    private var burstColor = "#FF4081".toColorInt() // Default accent pink/red

    fun setBurstColor(color: Int) {
        burstColor = color
        dotPaint.color = color
        ringPaint.color = color
    }

    fun startAnimation() {
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 450
        animator.interpolator = DecelerateInterpolator(1.5f)
        animator.addUpdateListener {
            progress = it.animatedValue as Float
            invalidate()
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                progress = 0f
                invalidate()
            }
        })
        animator.start()
    }

    override fun onDraw(canvas: Canvas) {
        if (progress <= 0f || progress >= 1f) return

        val centerX = width / 2f
        val centerY = height / 2f
        val maxRadius = width.coerceAtMost(height) / 2.5f

        // Draw ring
        if (progress < 0.4f) {
            val ringProgress = progress / 0.4f
            ringPaint.alpha = (255 * (1f - ringProgress)).toInt()
            val ringRadius = maxRadius * 0.8f * ringProgress
            canvas.drawCircle(centerX, centerY, ringRadius, ringPaint)
        }

        // Draw dots
        val dotCount = 7
        val dotMaxRadius = 7f
        val dotDistance = maxRadius * progress

        canvas.save()
        canvas.translate(centerX, centerY)
        for (i in 0 until dotCount) {
            val angle = i * 360f / dotCount
            canvas.save()
            canvas.rotate(angle)
            
            val x = dotDistance
            val y = 0f

            val dotAlpha = if (progress < 0.5f) 255 else (255 * (1f - progress) * 2f).toInt()
            dotPaint.alpha = dotAlpha
            
            val dotRadius = dotMaxRadius * (1f - progress)
            canvas.drawCircle(x, y, dotRadius, dotPaint)
            canvas.restore()
        }
        canvas.restore()
    }
}
