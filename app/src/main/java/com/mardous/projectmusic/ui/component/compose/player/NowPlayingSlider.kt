/*
 * Copyright (c) 2026 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mardous.projectmusic.ui.component.compose.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.mardous.projectmusic.util.ProgressBarStyle
import com.mardous.projectmusic.util.ThumbStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingSlider(
    sliderState: SliderState,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    progressStyle: ProgressBarStyle = ProgressBarStyle.WAVY,
    thumbStyle: ThumbStyle = ThumbStyle.CIRCLE,
    thumbSize: Float = 1.0f,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
) {
    val colors = SliderDefaults.colors(
        activeTrackColor = activeColor,
        thumbColor = activeColor,
        inactiveTrackColor = activeColor.copy(alpha = 0.24f)
    )
    Slider(
        state = sliderState,
        colors = colors,
        thumb = {
            StyleThumb(thumbStyle, thumbSize, sliderState.isDragging, enabled, activeColor, colors)
        },
        track = {
            StyleTrack(progressStyle, isPlaying, sliderState, enabled, activeColor, colors)
        },
        enabled = enabled,
        modifier = modifier
    )
}

@Composable
fun StyleThumb(
    style: ThumbStyle,
    scale: Float,
    isDragging: Boolean,
    enabled: Boolean,
    activeColor: Color,
    colors: SliderColors
) {
    val interactionSource = remember { MutableInteractionSource() }
    val thumbColor = if (enabled) activeColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    
    when (style) {
        ThumbStyle.NONE -> Box(Modifier.size(0.dp))
        ThumbStyle.CIRCLE -> {
            val size by animateDpAsState(if (isDragging) 24.dp * scale else 20.dp * scale)
            SliderDefaults.Thumb(
                interactionSource = interactionSource,
                thumbSize = DpSize(size, size),
                colors = colors
            )
        }
        ThumbStyle.PILL -> {
            val width by animateDpAsState(if (isDragging) 10.dp * scale else 6.dp * scale)
            val height by animateDpAsState(if (isDragging) 36.dp * scale else 28.dp * scale)
            SliderDefaults.Thumb(
                interactionSource = interactionSource,
                thumbSize = DpSize(width, height),
                colors = colors
            )
        }
        ThumbStyle.DIAMOND -> {
            val size by animateDpAsState(if (isDragging) 24.dp * scale else 20.dp * scale)
            Box(
                modifier = Modifier.size(size),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = Path().apply {
                        moveTo(size.toPx() / 2, 0f)
                        lineTo(size.toPx(), size.toPx() / 2)
                        lineTo(size.toPx() / 2, size.toPx())
                        lineTo(0f, size.toPx() / 2)
                        close()
                    }
                    drawPath(path, color = thumbColor)
                }
            }
        }
        ThumbStyle.LINE -> {
            val height by animateDpAsState(if (isDragging) 40.dp * scale else 32.dp * scale)
            Box(
                modifier = Modifier
                    .width(4.dp * scale)
                    .height(height)
                    .clip(CircleShape)
                    .background(thumbColor)
            )
        }
        ThumbStyle.SQUARE -> {
            val size by animateDpAsState(if (isDragging) 24.dp * scale else 20.dp * scale)
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(MaterialTheme.shapes.small)
                    .background(thumbColor)
            )
        }
        ThumbStyle.GLOW -> {
            val size by animateDpAsState(if (isDragging) 28.dp * scale else 22.dp * scale)
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size((size + 12.dp) * scale)
                        .blur(8.dp)
                        .clip(CircleShape)
                        .background(thumbColor.copy(alpha = 0.4f))
                )
                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(thumbColor)
                )
            }
        }
        ThumbStyle.ARROW -> {
            val size by animateDpAsState(if (isDragging) 24.dp * scale else 20.dp * scale)
            Canvas(modifier = Modifier.size(size)) {
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.toPx(), size.toPx() / 2)
                    lineTo(0f, size.toPx())
                    lineTo(size.toPx() * 0.3f, size.toPx() / 2)
                    close()
                }
                drawPath(path, color = thumbColor)
            }
        }
        ThumbStyle.DOT -> {
            val size by animateDpAsState(if (isDragging) 24.dp * scale else 20.dp * scale)
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(size)
                        .border(2.dp * scale, thumbColor, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(size / 2)
                        .clip(CircleShape)
                        .background(thumbColor)
                )
            }
        }
    }
}

@Composable
fun StyleTrack(
    style: ProgressBarStyle,
    isPlaying: Boolean,
    sliderState: SliderState,
    enabled: Boolean,
    activeColor: Color,
    colors: SliderColors
) {
    val progress = remember(sliderState.value, sliderState.valueRange) {
        val range = sliderState.valueRange.endInclusive - sliderState.valueRange.start
        if (range > 0f) (sliderState.value - sliderState.valueRange.start) / range else 0f
    }
    
    val activeTrackColor = if (enabled) activeColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val inactiveTrackColor = if (enabled) activeColor.copy(alpha = 0.24f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    when (style) {
        ProgressBarStyle.LINEAR -> SliderDefaults.Track(sliderState = sliderState, colors = colors)
        ProgressBarStyle.WAVY -> WavyTrack(isPlaying, sliderState)
        ProgressBarStyle.ROUNDED -> {
            val height by animateDpAsState(if (sliderState.isDragging) 16.dp else 12.dp)
            SliderDefaults.Track(
                sliderState = sliderState,
                colors = colors,
                modifier = Modifier.height(height.coerceAtMost(16.dp)),
                thumbTrackGapSize = 0.dp
            )
        }
        ProgressBarStyle.THIN -> {
            SliderDefaults.Track(
                sliderState = sliderState,
                colors = colors,
                modifier = Modifier.height(4.dp),
                drawStopIndicator = null
            )
        }
        ProgressBarStyle.THICK -> {
            val height by animateDpAsState(if (sliderState.isDragging) 24.dp else 20.dp)
            SliderDefaults.Track(
                sliderState = sliderState,
                colors = colors,
                modifier = Modifier.height(height.coerceAtMost(24.dp)),
                thumbTrackGapSize = 0.dp
            )
        }
        ProgressBarStyle.GRADIENT -> {
            val color1 = activeTrackColor
            val color2 = color1.shiftHue(30f)
            val color3 = color1.shiftHue(-30f)
            
            val brush = Brush.horizontalGradient(
                colors = listOf(color3, color1, color2)
            )
            Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(inactiveTrackColor)) {
                Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(brush))
            }
        }
        ProgressBarStyle.SEGMENTED -> {
            Canvas(modifier = Modifier.fillMaxWidth().height(12.dp)) {
                val width = size.width
                val height = 12.dp.toPx()
                val segmentWidth = 24.dp.toPx()
                val gap = 8.dp.toPx()
                val step = segmentWidth + gap
                
                var currentX = 0f
                while (currentX < width) {
                    val rectWidth = (segmentWidth).coerceAtMost(width - currentX)
                    val isActiveCenter = (currentX + rectWidth / 2) / width <= progress
                    val color = if (isActiveCenter) activeTrackColor else inactiveTrackColor
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(currentX, 0f),
                        size = Size(rectWidth, height),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                    currentX += step
                }
            }
        }
        ProgressBarStyle.DOTS -> {
            Canvas(modifier = Modifier.fillMaxWidth().height(10.dp)) {
                val width = size.width
                val centerY = size.height / 2
                val dotRadius = 5.dp.toPx()
                val gap = 15.dp.toPx()
                val step = dotRadius * 2 + gap
                
                var currentX = dotRadius
                while (currentX < width) {
                    val isActive = currentX / width <= progress
                    val color = if (isActive) activeTrackColor else inactiveTrackColor
                    drawCircle(color = color, radius = dotRadius, center = Offset(currentX, centerY))
                    currentX += step
                }
            }
        }
        ProgressBarStyle.GLOW -> {
            Canvas(modifier = Modifier.fillMaxWidth().height(32.dp)) {
                val width = size.width
                val centerY = size.height / 2
                val progressX = width * progress
                val trackHeight = 4.dp.toPx()
                
                // Track
                drawLine(
                    color = inactiveTrackColor,
                    start = Offset(0f, centerY),
                    end = Offset(width, centerY),
                    strokeWidth = trackHeight,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = activeTrackColor,
                    start = Offset(0f, centerY),
                    end = Offset(progressX, centerY),
                    strokeWidth = trackHeight,
                    cap = StrokeCap.Round
                )
                
                // Glow
                if (progressX > 0) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(activeTrackColor.copy(alpha = 0.6f), Color.Transparent),
                            center = Offset(progressX, centerY),
                            radius = 24.dp.toPx()
                        ),
                        radius = 24.dp.toPx(),
                        center = Offset(progressX, centerY)
                    )
                    drawCircle(
                        color = activeTrackColor,
                        radius = 4.dp.toPx(),
                        center = Offset(progressX, centerY)
                    )
                }
            }
        }
    }
}

private fun Color.shiftHue(amount: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(this.toArgb(), hsv)
    hsv[0] = (hsv[0] + amount) % 360
    return Color(android.graphics.Color.HSVToColor(android.graphics.Color.alpha(this.toArgb()), hsv))
}

@Composable
fun WavyTrack(
    isPlaying: Boolean,
    sliderState: SliderState
) {
    val animatedHeight by animateDpAsState(
        if (sliderState.isDragging) 7.dp else 4.dp
    )
    val trackStroke = Stroke(
        width =
            with(LocalDensity.current) {
                animatedHeight.toPx()
            },
        cap = StrokeCap.Round,
    )

    LinearWavyProgressIndicator(
        modifier = Modifier.fillMaxWidth(),
        progress = {
            val rangeLength = sliderState.valueRange.endInclusive - sliderState.valueRange.start
            if (rangeLength > 0f) {
                (sliderState.value - sliderState.valueRange.start) / rangeLength
            } else 0f
        },
        stopSize = 0.dp,
        trackStroke = trackStroke,
        amplitude = { if (isPlaying && !sliderState.isDragging) 1f else 0f }
    )
}

@Composable
fun StraightTrack(sliderState: SliderState) {
    SliderDefaults.Track(
        sliderState = sliderState,
        drawStopIndicator = null,
        modifier = Modifier.height(8.dp)
    )
}

@Composable
private fun rememberInteractionSource(): MutableInteractionSource {
    return remember { MutableInteractionSource() }
}
