package com.mardous.projectmusic.ui.component.views

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.SeekBar
import android.content.SharedPreferences
import androidx.core.content.withStyledAttributes
import com.google.android.material.slider.Slider
import com.mardous.projectmusic.R
import com.mardous.projectmusic.extensions.resources.applyColor
import com.mardous.projectmusic.extensions.resources.primaryColor
import com.mardous.projectmusic.util.Preferences
import com.mardous.projectmusic.util.THUMB_SIZE
import com.mardous.projectmusic.util.THUMB_STYLE
import com.mardous.projectmusic.util.PROGRESS_BAR_STYLE
import com.mardous.projectmusic.util.PROGRESS_CONTROL_STYLE
import com.mardous.projectmusic.util.SQUIGGLY_SEEK_BAR
import com.mardous.projectmusic.util.ProgressBarStyle
import com.mardous.projectmusic.util.ProgressControlStyle
import com.mardous.projectmusic.util.ThumbStyle

/**
 * @author Christians M.A. (mardous)
 */
class MusicSlider @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), SharedPreferences.OnSharedPreferenceChangeListener {

    private var listener: Listener? = null
    private var internalView: View? = null
    val progressView get() = internalView

    private val seekBar get() = internalView as? SeekBar
    private val slider get() = internalView as? Slider

    private var thumbHeight = -1
    private var trackHeight = -1
    private var progressBarStyle: ProgressBarStyle = ProgressBarStyle.LINEAR
    private var thumbStyle: ThumbStyle = ThumbStyle.CIRCLE
    private var progressControlStyle: ProgressControlStyle = ProgressControlStyle.CLASSIC
    private var _currentColor: Int = Color.TRANSPARENT

    var isTrackingTouch: Boolean = false
        private set

    var currentColor: Int
        set(value) {
            val fallback = context.primaryColor()
            val color = if (value == Color.TRANSPARENT || value == 0) fallback else value
            _currentColor = color
            seekBar?.applyColor(color)
            (seekBar?.thumb as? MusicThumbDrawable)?.let {
                it.setColor(color)
                seekBar?.thumbTintList = null
            }
            slider?.applyColor(color)
        }
        get() = if (_currentColor != Color.TRANSPARENT && _currentColor != 0) _currentColor else context.primaryColor()

    var animateSquigglyProgress: Boolean
        get() = (seekBar?.progressDrawable as? SquigglyProgress)?.animate == true
        set(value) {
            (seekBar?.progressDrawable as? SquigglyProgress)?.animate = value
        }

    fun setProgressBarStyle(style: ProgressBarStyle) {
        if (this.progressBarStyle != style) {
            val previousState = detachInternalView()
            this.progressBarStyle = style
            inflateSliderView(style, progressControlStyle, thumbStyle, previousState)
        }
    }

    fun setThumbStyle(style: ThumbStyle) {
        if (this.thumbStyle != style) {
            val previousState = detachInternalView()
            this.thumbStyle = style
            inflateSliderView(progressBarStyle, progressControlStyle, style, previousState)
        }
    }

    fun setProgressControlStyle(style: ProgressControlStyle) {
        if (this.progressControlStyle != style) {
            val previousState = detachInternalView()
            this.progressControlStyle = style
            inflateSliderView(progressBarStyle, style, thumbStyle, previousState)
        }
    }

    var valueFrom: Int
        set(valueFrom) {
            seekBar?.min = valueFrom
            slider?.valueFrom = valueFrom.toFloat()
        }
        get() = seekBar?.min ?: slider?.valueFrom?.toInt() ?: 0

    var valueTo: Int
        set(valueTo) {
            seekBar?.max = valueTo
            slider?.valueTo = valueTo.toFloat().coerceAtLeast(1f)
        }
        get() = seekBar?.max ?: slider?.valueTo?.toInt() ?: 0

    var value: Int
        set(value) {
            seekBar?.progress = value
            slider?.let { slider ->
                slider.value = value.toFloat().coerceIn(slider.valueFrom, slider.valueTo)
            }
        }
        get() = seekBar?.progress ?: slider?.value?.toInt() ?: 0

    init {
        context.withStyledAttributes(attrs, R.styleable.MusicSlider) {
            thumbHeight = getDimensionPixelSize(R.styleable.MusicSlider_musicSliderThumbHeight, -1)
            trackHeight = getDimensionPixelSize(R.styleable.MusicSlider_musicSliderTrackHeight, -1)
            progressBarStyle = Preferences.progressBarStyle
            thumbStyle = Preferences.thumbStyle
            progressControlStyle = Preferences.progressControlStyle
            inflateSliderView(
                style = progressBarStyle,
                controlStyle = progressControlStyle,
                thumbStyle = thumbStyle,
                previousState = ProgressViewState.from(this@MusicSlider)
            )
        }
        Preferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Preferences.unregisterOnSharedPreferenceChangeListener(this)
        detachInternalView()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            THUMB_SIZE, THUMB_STYLE, PROGRESS_BAR_STYLE, PROGRESS_CONTROL_STYLE, SQUIGGLY_SEEK_BAR -> {
                val previousState = ProgressViewState.from(this)
                inflateSliderView(
                    style = Preferences.progressBarStyle,
                    controlStyle = Preferences.progressControlStyle,
                    thumbStyle = Preferences.thumbStyle,
                    previousState = previousState
                )
            }
        }
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    fun setUseSquiggly(useSquiggly: Boolean) {
        setProgressBarStyle(if (useSquiggly) ProgressBarStyle.WAVY else ProgressBarStyle.LINEAR)
    }

    private fun detachInternalView(): ProgressViewState {
        val state = ProgressViewState.from(this)
        removeAllViews()
        slider?.clearOnChangeListeners()
        slider?.clearOnSliderTouchListeners()
        seekBar?.setOnSeekBarChangeListener(null)
        internalView = null
        return state
    }

    private fun inflateSliderView(
        style: ProgressBarStyle,
        controlStyle: ProgressControlStyle,
        thumbStyle: ThumbStyle,
        previousState: ProgressViewState?
    ) {
        val density = context.resources.displayMetrics.density
        minimumHeight = (32 * density).toInt()
        
        // Always use SeekBar (music_squiggly_slider) to support custom thumbs and all progress styles
        internalView = LayoutInflater.from(context).inflate(R.layout.music_squiggly_slider, this, false).apply {
            val isExpressive = controlStyle == ProgressControlStyle.EXPRESSIVE
            
            (this as? SeekBar)?.let { sb ->
                val progressDrawable = sb.progressDrawable as? SquigglyProgress
                progressDrawable?.style = style
                
                if (isExpressive) {
                    progressDrawable?.strokeWidth = 24 * density
                } else if (trackHeight != -1) {
                    progressDrawable?.strokeWidth = trackHeight.toFloat()
                }

                sb.thumb = MusicThumbDrawable().apply {
                    setStyle(thumbStyle)
                    setColor(currentColor)
                    setScale(Preferences.thumbSize)
                    if (isExpressive) {
                        setSize(36 * density)
                    } else if (thumbHeight != -1) {
                        setSize(thumbHeight.toFloat())
                    }
                }
                
                if (currentColor != Color.TRANSPARENT) {
                    sb.thumbTintList = null
                }

                if (isExpressive) {
                    sb.thumbOffset = (18 * density).toInt() // Half of thumb height
                }
            }
        }

        if (previousState != null) {
            this.valueFrom = previousState.min
            this.valueTo = previousState.max
            this.value = previousState.progress
            if (previousState.currentColor != Color.TRANSPARENT) {
                this.currentColor = previousState.currentColor
            }
        }
        addView(internalView)
        setupInternalListener()
    }

    private fun setupInternalListener() {
        seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                listener?.onProgressChanged(this@MusicSlider, progress, fromUser)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isTrackingTouch = true
                listener?.onStartTrackingTouch(this@MusicSlider)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isTrackingTouch = false
                listener?.onStopTrackingTouch(this@MusicSlider)
            }
        })
        slider?.addOnChangeListener { slider, value, fromUser ->
            listener?.onProgressChanged(this, value.toInt(), fromUser)
        }
        slider?.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                isTrackingTouch = true
                listener?.onStartTrackingTouch(this@MusicSlider)
            }

            override fun onStopTrackingTouch(slider: Slider) {
                isTrackingTouch = false
                listener?.onStopTrackingTouch(this@MusicSlider)
            }
        })
    }

    interface Listener {
        fun onProgressChanged(slider: MusicSlider, progress: Int, fromUser: Boolean)
        fun onStartTrackingTouch(slider: MusicSlider)
        fun onStopTrackingTouch(slider: MusicSlider)
    }

    private class ProgressViewState(
        val max: Int,
        val min: Int,
        val progress: Int,
        val currentColor: Int = Color.TRANSPARENT
    ) {
        companion object {
            fun from(slider: MusicSlider) = ProgressViewState(
                max = slider.valueTo,
                min = slider.valueFrom,
                progress = slider.value,
                currentColor = slider.currentColor
            )

            fun from(a: TypedArray) = ProgressViewState(
                max = a.getInt(R.styleable.MusicSlider_android_max, 100),
                min = a.getInt(R.styleable.MusicSlider_android_min, 0),
                progress = a.getInt(R.styleable.MusicSlider_android_progress, 0)
            )
        }
    }
}
