package com.mardous.projectmusic.ui.component.views

import android.graphics.*
import android.graphics.drawable.Drawable
import com.mardous.projectmusic.util.ThumbStyle

class MusicThumbDrawable : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var thumbStyle: ThumbStyle = ThumbStyle.CIRCLE
    private var color: Int = Color.WHITE
    private var size: Float = 48f
    private var scaleFactor: Float = 1.0f

    fun setStyle(style: ThumbStyle) {
        this.thumbStyle = style
        invalidateSelf()
    }

    fun setColor(color: Int) {
        this.color = color
        paint.color = color
        invalidateSelf()
    }

    fun setSize(size: Float) {
        this.size = size
        invalidateSelf()
    }

    fun setScale(scale: Float) {
        this.scaleFactor = scale
        invalidateSelf()
    }

    override fun draw(canvas: Canvas) {
        val centerX = bounds.centerX().toFloat()
        val centerY = bounds.centerY().toFloat()
        val width = bounds.width().toFloat() * scaleFactor
        val height = bounds.height().toFloat() * scaleFactor

        when (thumbStyle) {
            ThumbStyle.NONE -> {}
            ThumbStyle.CIRCLE -> {
                canvas.drawCircle(centerX, centerY, width / 2.5f, paint)
            }
            ThumbStyle.PILL -> {
                val pillWidth = width * 0.3f
                val pillHeight = height * 0.8f
                val rect = RectF(centerX - pillWidth / 2, centerY - pillHeight / 2, centerX + pillWidth / 2, centerY + pillHeight / 2)
                canvas.drawRoundRect(rect, pillWidth / 2, pillWidth / 2, paint)
            }
            ThumbStyle.DIAMOND -> {
                val path = Path().apply {
                    moveTo(centerX, centerY - height / 2)
                    lineTo(centerX + width / 2, centerY)
                    lineTo(centerX, centerY + height / 2)
                    lineTo(centerX - width / 2, centerY)
                    close()
                }
                canvas.drawPath(path, paint)
            }
            ThumbStyle.LINE -> {
                val lineWidth = 4f
                val rect = RectF(centerX - lineWidth / 2, centerY - height * 0.8f / 2, centerX + lineWidth / 2, centerY + height * 0.8f / 2)
                canvas.drawRect(rect, paint)
            }
            ThumbStyle.SQUARE -> {
                val size = width * 0.6f
                val rect = RectF(centerX - size / 2, centerY - size / 2, centerX + size / 2, centerY + size / 2)
                canvas.drawRoundRect(rect, 8f, 8f, paint)
            }
            ThumbStyle.GLOW -> {
                glowPaint.shader = RadialGradient(
                    centerX, centerY, width / 1.5f,
                    intArrayOf(color.withAlpha(0.4f), Color.TRANSPARENT),
                    null, Shader.TileMode.CLAMP
                )
                canvas.drawCircle(centerX, centerY, width / 1.5f, glowPaint)
                canvas.drawCircle(centerX, centerY, width / 2.5f, paint)
            }
            ThumbStyle.ARROW -> {
                val path = Path().apply {
                    moveTo(centerX - width / 2, centerY - height / 2)
                    lineTo(centerX + width / 2, centerY)
                    lineTo(centerX - width / 2, centerY + height / 2)
                    lineTo(centerX - width / 4, centerY)
                    close()
                }
                canvas.drawPath(path, paint)
            }
            ThumbStyle.DOT -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                canvas.drawCircle(centerX, centerY, width / 3f, paint)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(centerX, centerY, width / 6f, paint)
            }
        }
    }

    private fun Int.withAlpha(alpha: Float): Int {
        val a = (Color.alpha(this) * alpha).toInt().coerceIn(0, 255)
        return Color.argb(a, Color.red(this), Color.green(this), Color.blue(this))
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun getIntrinsicWidth(): Int = size.toInt()
    override fun getIntrinsicHeight(): Int = size.toInt()
}
