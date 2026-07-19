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

package com.mardous.projectmusic.ui.component.compose.decoration

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import kotlin.math.*

@Composable
fun Modifier.animatedGradient(
    colors: List<Color>,
    animating: Boolean,
    beatPulse: Float = 1f,
    motionIntensity: Float = 1f,
    highQuality: Boolean = true
): Modifier = composed {
    val time = remember { Animatable(0f) }

    LaunchedEffect(animating, motionIntensity) {
        if (motionIntensity > 0.01f) {
            val period = (2f * PI.toFloat() * 10f)
            val speed = period / 120000f
            var lastFrameTime = -1L

            while (true) {
                val nextValue = withFrameMillis { frameTime ->
                    var value = time.value
                    if (lastFrameTime != -1L) {
                        val deltaMillis = frameTime - lastFrameTime
                        // Speed based on motion intensity and beat
                        val speedMultiplier = motionIntensity * (1f + (beatPulse - 1f) * 2f)
                        value = (time.value + (deltaMillis * speed * speedMultiplier)) % period
                    }
                    lastFrameTime = frameTime
                    value
                }
                time.snapTo(nextValue)
            }
        }
    }

    val effectiveTime = time.value
    val safeSize = colors.size.coerceAtLeast(1)
    val rawBase = colors.getOrNull(0) ?: Color.Transparent

    val targetBase = remember(colors, rawBase) {
        if (rawBase != Color.Transparent) adjustColorForBackground(rawBase, lightnessFactor = 0.8f) else rawBase
    }
    
    // 6 circles for high quality, 3 for high performance
    val circleCount = if (highQuality) 6 else 3
    val targetColors = List(circleCount) { i ->
        val c = colors.getOrElse(i % safeSize) { targetBase }
        if (c != Color.Transparent) adjustColorForBackground(c, 
            lightnessFactor = if (i % 2 == 0) 1.1f else 0.9f,
            saturationFactor = if (i % 3 == 0) 1.2f else 0.8f
        ) else c
    }

    val baseColor by animateColorAsState(targetBase, tween(1500), label = "baseColor")
    val animatedColors = targetColors.mapIndexed { i, color ->
        animateColorAsState(color, tween(1500 + i * 100), label = "color$i")
    }

    this.drawBehind {
        if (baseColor == Color.Transparent) return@drawBehind

        drawRect(baseColor)

        for (i in 0 until circleCount) {
            val phase = i * (PI.toFloat() / (circleCount / 2f))
            val color = animatedColors[i].value
            
            val x = (0.5f + 0.35f * sin(effectiveTime * (0.4f + i * 0.05f) + phase)).coerceIn(0f, 1f)
            val y = (0.5f + 0.35f * cos(effectiveTime * (0.3f + i * 0.07f) + phase * 1.5f)).coerceIn(0f, 1f)
            
            val baseRadius = size.minDimension * (0.7f + i * 0.1f)
            val pulseMultiplier = 1f + (beatPulse - 1f) * motionIntensity * (0.2f + i * 0.05f)
            val radius = baseRadius * pulseMultiplier
            
            // Advanced Blending for high quality "Hotspots"
            // Note: Standard DrawScope doesn't support BlendMode in drawCircle directly without Paint
            // We use simple alpha layering for now to keep performance high
            val alpha = (0.15f / (i * 0.2f + 1f)) * motionIntensity.coerceAtLeast(0.1f)
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = alpha), Color.Transparent),
                    center = Offset(x * size.width, y * size.height),
                    radius = radius
                ),
                center = Offset(x * size.width, y * size.height),
                radius = radius
            )
        }
    }
}

@Composable
fun Modifier.flowingGradient(
    dominantColor: Color,
    beatPulse: Float = 1f,
    motionIntensity: Float = 1f
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "flowingGradient")
    
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    val color1 = remember(dominantColor) { adjustColorForBackground(dominantColor, lightnessFactor = 1.3f) }
    val color2 = remember(dominantColor) { adjustColorForBackground(dominantColor, lightnessFactor = 0.7f) }
    val color3 = remember(dominantColor) { adjustColorForBackground(dominantColor, saturationFactor = 1.4f) }
    val color4 = remember(dominantColor) { adjustColorForBackground(dominantColor, lightnessFactor = 0.5f, saturationFactor = 0.6f) }
    
    val aColor1 by animateColorAsState(color1, tween(1500), label = "c1")
    val aColor2 by animateColorAsState(color2, tween(1500), label = "c2")
    val aColor3 by animateColorAsState(color3, tween(1500), label = "c3")
    val aColor4 by animateColorAsState(color4, tween(1500), label = "c4")

    this.drawBehind {
        val width = size.width
        val height = size.height
        
        // "Kick" the offset on beat
        val beatKick = (beatPulse - 1f) * motionIntensity * 0.05f
        val effectiveOffset = (offset * motionIntensity) + beatKick

        val brush = Brush.linearGradient(
            colors = listOf(
                aColor1, aColor2, aColor3, aColor4, aColor1
            ),
            start = Offset(width * (effectiveOffset - 1f), height * (effectiveOffset - 1f)),
            end = Offset(width * (effectiveOffset + 1f), height * (effectiveOffset + 1f)),
            tileMode = TileMode.Mirror
        )
        
        drawRect(brush = brush)
    }
}

@Composable
fun Modifier.glowBackground(
    dominantColor: Color,
    beatPulse: Float = 1f,
    motionIntensity: Float = 1f
): Modifier = composed {
    val animatedColor by animateColorAsState(
        targetValue = adjustColorForBackground(dominantColor, lightnessFactor = 0.5f),
        animationSpec = tween(1500),
        label = "glowColor"
    )

    this.drawBehind {
        drawRect(Color.Black)
        
        for (i in 1..3) {
            val layerPulse = 1f + (beatPulse - 1f) * motionIntensity * (0.1f * i)
            val radius = size.minDimension * (1.0f + i * 0.3f) * layerPulse
            val alpha = (0.3f / i) * motionIntensity.coerceAtLeast(0.1f)
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(animatedColor.copy(alpha = alpha.coerceIn(0f, 1f)), Color.Transparent),
                    center = center,
                    radius = radius
                ),
                center = center,
                radius = radius
            )
        }
    }
}

private fun adjustColorForBackground(
    color: Color, 
    lightnessFactor: Float = 1.0f,
    saturationFactor: Float = 1.0f
): Color {
    val r = color.red
    val g = color.green
    val b = color.blue

    val max = maxOf(r, maxOf(g, b))
    val min = minOf(r, minOf(g, b))
    val delta = max - min

    var h = 0f
    var s = 0f
    val l = (max + min) / 2f

    if (max != min) {
        s = if (l < 0.5f) delta / (max + min) else delta / (2f - max - min)
        h = when (max) {
            r -> (g - b) / delta + (if (g < b) 6f else 0f)
            g -> (b - r) / delta + 2f
            else -> (r - g) / delta + 4f
        }
        h *= 60f
    }

    val targetS = (s * saturationFactor).coerceIn(0.10f, 0.25f)
    val targetL = (l * lightnessFactor).coerceIn(0.08f, 0.20f)

    return hslToColor(h, targetS, targetL, color.alpha)
}

private fun hslToColor(h: Float, s: Float, l: Float, a: Float): Color {
    if (s == 0f) return Color(red = l, green = l, blue = l, alpha = a)

    val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
    val p = 2f * l - q

    val hNorm = h / 360f
    val r = hueToRgb(p, q, hNorm + 1f / 3f)
    val g = hueToRgb(p, q, hNorm)
    val b = hueToRgb(p, q, hNorm - 1f / 3f)

    return Color(red = r, green = g, blue = b, alpha = a)
}

private fun hueToRgb(p: Float, q: Float, t: Float): Float {
    var tt = t
    if (tt < 0f) tt += 1f
    if (tt > 1f) tt -= 1f
    return when {
        tt < 1f / 6f -> p + (q - p) * 6f * tt
        tt < 1f / 2f -> q
        tt < 2f / 3f -> p + (q - p) * (2f / 3f - tt) * 6f
        else -> p
    }
}

@Composable
fun Modifier.liquidBackground(
    dominantColor: Color,
    beatPulse: Float = 1f,
    motionIntensity: Float = 1f,
    highQuality: Boolean = true
): Modifier = composed {
    val timeValue = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(motionIntensity) {
        val period = (2f * PI.toFloat() * 10f)
        val speed = period / 180000f
        var lastFrameTime = -1L
        while (true) {
            withFrameMillis { frameTime ->
                if (lastFrameTime != -1L) {
                    val delta = frameTime - lastFrameTime
                    val speedMultiplier = motionIntensity * (1f + (beatPulse - 1f) * 1.5f)
                    timeValue.floatValue = (timeValue.floatValue + delta * speed * speedMultiplier) % period
                }
                lastFrameTime = frameTime
            }
        }
    }

    val animatedColor by animateColorAsState(dominantColor, tween(1500), label = "liquidColor")
    val circles = remember(animatedColor) {
        List(if (highQuality) 8 else 5) { i ->
            val hueShift = (i * 45f) % 360f
            val saturationFactor = if (i % 2 == 0) 1.2f else 0.8f
            val lightnessFactor = if (i % 3 == 0) 1.1f else 0.7f
            adjustColorForLiquid(animatedColor, hueShift, saturationFactor, lightnessFactor)
        }
    }

    val blurRadius = if (highQuality) 120.dp else 60.dp

    this
        .blur(blurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded)
        .drawBehind {
            drawRect(Color.Black)
            
            val t = timeValue.floatValue
            circles.forEachIndexed { i, color ->
                val phase = i * (2 * PI / circles.size).toFloat()
                val x = (0.5f + 0.4f * sin(t * (0.5f + i * 0.05f) + phase)).coerceIn(0f, 1f)
                val y = (0.5f + 0.4f * cos(t * (0.3f + i * 0.07f) + phase * 1.2f)).coerceIn(0f, 1f)
                
                val radiusBase = size.minDimension * (0.6f + i * 0.1f)
                val radius = radiusBase * (1f + (beatPulse - 1f) * 0.2f)
                
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.4f), Color.Transparent),
                        center = Offset(x * size.width, y * size.height),
                        radius = radius
                    ),
                    center = Offset(x * size.width, y * size.height),
                    radius = radius
                )
            }
            
            // Aurora Layer
            val auroraColor = circles.last().copy(alpha = 0.15f * motionIntensity)
            val auroraY = (0.3f + 0.1f * sin(t * 0.2f)) * size.height
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, auroraColor, Color.Transparent),
                    startY = auroraY - 100.dp.toPx(),
                    endY = auroraY + 100.dp.toPx()
                )
            )

            // Vignette / Bottom Dimming
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.2f),
                    0.4f to Color.Transparent,
                    0.8f to Color.Black.copy(alpha = 0.5f),
                    startY = 0f,
                    endY = size.height
                )
            )
        }
}

private fun adjustColorForLiquid(
    color: Color,
    hueShift: Float,
    saturationFactor: Float,
    lightnessFactor: Float
): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(color.toArgb(), hsl)
    hsl[0] = (hsl[0] + hueShift) % 360f
    hsl[1] = (hsl[1] * saturationFactor).coerceIn(0.2f, 0.8f)
    hsl[2] = (hsl[2] * lightnessFactor).coerceIn(0.1f, 0.4f)
    return Color(ColorUtils.HSLToColor(hsl))
}

@Composable
fun Modifier.auroraBackground(
    dominantColor: Color,
    beatPulse: Float = 1f,
    motionIntensity: Float = 1f,
    highQuality: Boolean = true
): Modifier = composed {
    val timeValue = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(motionIntensity) {
        val period = (2f * PI.toFloat() * 10f)
        val speed = period / 150000f
        var lastFrameTime = -1L
        while (true) {
            withFrameMillis { frameTime ->
                if (lastFrameTime != -1L) {
                    val delta = frameTime - lastFrameTime
                    val speedMultiplier = motionIntensity * (1f + (beatPulse - 1f) * 1.8f)
                    timeValue.floatValue = (timeValue.floatValue + delta * speed * speedMultiplier) % period
                }
                lastFrameTime = frameTime
            }
        }
    }

    val animatedColor by animateColorAsState(dominantColor, tween(1500), label = "auroraColor")
    
    val colors = remember(animatedColor) {
        listOf(
            animatedColor,
            adjustColorForAurora(animatedColor, hueShift = -30f, saturationFactor = 1.2f, lightnessFactor = 0.8f),
            adjustColorForAurora(animatedColor, hueShift = 30f, saturationFactor = 0.8f, lightnessFactor = 0.6f),
            adjustColorForAurora(animatedColor, hueShift = 180f, saturationFactor = 1.1f, lightnessFactor = 0.7f),
            adjustColorForAurora(animatedColor, hueShift = 0f, saturationFactor = 0.5f, lightnessFactor = 0.5f)
        )
    }

    this
        .blur(if (highQuality) 150.dp else 80.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
        .drawBehind {
            drawRect(Color.Black)
            
            val t = timeValue.floatValue
            
            colors.forEachIndexed { i, color ->
                val phase = i * (2 * PI / colors.size).toFloat()
                val x = (0.5f + 0.45f * sin(t * (0.4f + i * 0.03f) + phase)).coerceIn(-0.2f, 1.2f)
                val y = (0.5f + 0.45f * cos(t * (0.35f + i * 0.04f) + phase * 1.3f)).coerceIn(-0.2f, 1.2f)
                
                val radiusBase = size.minDimension * (0.8f + i * 0.15f)
                val radius = radiusBase * (1f + (beatPulse - 1f) * 0.15f)
                
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.5f), Color.Transparent),
                        center = Offset(x * size.width, y * size.height),
                        radius = radius
                    ),
                    center = Offset(x * size.width, y * size.height),
                    radius = radius
                )
            }

            val centerGlowRadius = size.minDimension * 0.8f * (1f + (beatPulse - 1f) * 0.2f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(animatedColor.copy(alpha = 0.3f * motionIntensity), Color.Transparent),
                    center = center,
                    radius = centerGlowRadius
                ),
                center = center,
                radius = centerGlowRadius
            )
            
            val auroraT = t * 0.5f
            val path = Path()
            val points = 20
            for (i in 0..points) {
                val fraction = i.toFloat() / points
                val px = fraction * size.width
                val py = size.height * 0.4f + 
                         sin(fraction * 2 * PI.toFloat() + auroraT) * 100.dp.toPx() +
                         cos(fraction * PI.toFloat() * 1.5f + auroraT * 0.7f) * 50.dp.toPx()
                
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, animatedColor.copy(alpha = 0.2f * motionIntensity), Color.Transparent),
                    startY = size.height * 0.3f,
                    endY = size.height * 0.6f
                ),
                style = Stroke(width = 250.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.4f),
                    0.5f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.7f),
                    startY = 0f,
                    endY = size.height
                )
            )
            
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f)),
                    center = center,
                    radius = size.maxDimension * 0.7f
                )
            )
        }
}

private fun adjustColorForAurora(
    color: Color,
    hueShift: Float,
    saturationFactor: Float,
    lightnessFactor: Float
): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(color.toArgb(), hsl)
    hsl[0] = (hsl[0] + hueShift) % 360f
    if (hsl[0] < 0) hsl[0] += 360f
    hsl[1] = (hsl[1] * saturationFactor).coerceIn(0.3f, 0.9f)
    hsl[2] = (hsl[2] * lightnessFactor).coerceIn(0.15f, 0.5f)
    return Color(ColorUtils.HSLToColor(hsl))
}
