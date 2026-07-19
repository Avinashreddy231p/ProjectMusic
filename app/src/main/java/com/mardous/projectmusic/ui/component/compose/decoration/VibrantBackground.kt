package com.mardous.projectmusic.ui.component.compose.decoration

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.mardous.projectmusic.core.model.theme.VibrantBackgroundMode
import com.mardous.projectmusic.playback.processor.BeatAnalyzer
import com.mardous.projectmusic.util.Preferences
import kotlin.random.Random

@Composable
fun VibrantBackground(
    dominantColor: Color,
    mode: VibrantBackgroundMode,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false
) {
    // Live Preference Listeners
    var animationsEnabled by remember { mutableStateOf(Preferences.vibrantBackgroundAnimations) }
    var highQuality by remember { mutableStateOf(Preferences.vibrantBackgroundHighQuality) }
    var noiseLevel by remember { mutableStateOf(Preferences.vibrantBackgroundNoiseLevel) }
    
    DisposableEffect(Unit) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "vibrant_background_animations" -> animationsEnabled = Preferences.vibrantBackgroundAnimations
                "vibrant_background_high_quality" -> highQuality = Preferences.vibrantBackgroundHighQuality
                "vibrant_background_noise_level" -> noiseLevel = Preferences.vibrantBackgroundNoiseLevel
            }
        }
        Preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { Preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    // Audio Reactivity: Collect real-time amplitude
    val rawAmplitude by BeatAnalyzer.amplitude.collectAsState()
    
    // Smooth the amplitude for visuals
    val smoothedAmplitude by animateFloatAsState(
        targetValue = if (isPlaying) rawAmplitude else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "smoothedAmplitude"
    )
    
    // Non-linear mapping for beat pulse (drastic increase for high beats)
    val pulseValue = remember(smoothedAmplitude) {
        val base = 1f
        val linearPart = smoothedAmplitude * 0.1f
        val exponentialPart = if (smoothedAmplitude > 0.7f) {
            val extra = (smoothedAmplitude - 0.7f) * 2.0f
            extra * extra
        } else 0f
        base + linearPart + exponentialPart
    }
    
    // Motion Inertia: Slowly settle when paused
    val motionIntensity = remember { Animatable(if (isPlaying) 1f else 0f) }
    LaunchedEffect(isPlaying, animationsEnabled) {
        if (isPlaying && animationsEnabled) {
            motionIntensity.animateTo(1f, spring(stiffness = Spring.StiffnessLow))
        } else {
            // Settle slowly over 2 seconds
            motionIntensity.animateTo(if (animationsEnabled) 0.15f else 0f, 
                tween(2000, easing = LinearOutSlowInEasing))
        }
    }

    val intensityValue = motionIntensity.value

    val animatedColor by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(1000),
        label = "dominantColorTransition"
    )

    Box(modifier = modifier.fillMaxSize().clipToBounds()) {
        // Fix transparency: Use a vertical ambient gradient derived from album color on solid black
        val ambientBaseColor = remember(dominantColor) {
            dominantColor.copy(alpha = 0.35f) // Moderate Contrast
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(Color.Black)
            drawRect(
                brush = Brush.verticalGradient(
                    0f to ambientBaseColor,
                    0.7f to Color.Transparent,
                    startY = 0f,
                    endY = size.height
                )
            )
        }

        when (mode) {
            VibrantBackgroundMode.Solid -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            drawRect(color = animatedColor)
                            // Vignette breathing
                            val vignetteAlpha = (pulseValue - 1f) * intensityValue * 1.0f
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = (0.3f + (vignetteAlpha * 0.2f)).coerceIn(0f, 1f))),
                                    center = center,
                                    radius = size.maxDimension * 0.8f
                                )
                            )
                        }
                )
            }
            VibrantBackgroundMode.Gradient -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val kick = (pulseValue - 1f) * intensityValue * 0.05f
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0f to animatedColor.copy(alpha = 0.4f),
                                    (0.85f + kick).coerceIn(0f, 1f) to Color.Black,
                                    startY = 0f,
                                    endY = size.height
                                )
                            )
                        }
                )
            }
            VibrantBackgroundMode.GridLines -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0f to animatedColor,
                                    0.85f to Color.Black,
                                    startY = 0f,
                                    endY = size.height
                                )
                            )
                        }
                ) {
                    GridLinesOverlay(beatPulse = pulseValue, motionIntensity = intensityValue)
                }
            }
            VibrantBackgroundMode.Smoke -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .animatedGradient(
                            colors = listOf(animatedColor, animatedColor.copy(alpha = 0.3f), Color.Black),
                            animating = animationsEnabled,
                            beatPulse = pulseValue,
                            motionIntensity = intensityValue,
                            highQuality = highQuality
                        )
                )
            }
            VibrantBackgroundMode.Fluid -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .animatedGradient(
                            colors = listOf(animatedColor, animatedColor.copy(alpha = 0.5f), Color.Black),
                            animating = isPlaying || intensityValue > 0.01f,
                            beatPulse = pulseValue,
                            motionIntensity = intensityValue,
                            highQuality = highQuality
                        )
                )
            }
            VibrantBackgroundMode.Flow -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .flowingGradient(
                            dominantColor = animatedColor,
                            beatPulse = pulseValue,
                            motionIntensity = intensityValue
                        )
                )
            }
            VibrantBackgroundMode.Glow -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .glowBackground(
                            dominantColor = animatedColor,
                            beatPulse = pulseValue,
                            motionIntensity = intensityValue
                        )
                )
            }
            VibrantBackgroundMode.Particles -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0f to animatedColor.copy(alpha = 0.5f),
                                    0.9f to Color.Black,
                                    startY = 0f,
                                    endY = size.height
                                )
                            )
                        }
                ) {
                    ParticlesBackground(
                        dominantColor = animatedColor,
                        isPlaying = isPlaying,
                        beatPulse = pulseValue,
                        motionIntensity = intensityValue,
                        highQuality = highQuality
                    )
                }
            }
            VibrantBackgroundMode.Liquid -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .liquidBackground(
                            dominantColor = animatedColor,
                            beatPulse = pulseValue,
                            motionIntensity = intensityValue,
                            highQuality = highQuality
                        )
                )
            }
            VibrantBackgroundMode.Aurora -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .auroraBackground(
                            dominantColor = animatedColor,
                            beatPulse = pulseValue,
                            motionIntensity = intensityValue,
                            highQuality = highQuality
                        )
                )
            }
        }

        if (noiseLevel > 0) {
            NoiseLayer(noiseLevel)
        }
    }
}

@Composable
fun NoiseLayer(level: Int) {
    if (level <= 0) return
    val alpha = (level / 100f) * 0.4f
    val noiseBitmap = remember {
        val bitmap = android.graphics.Bitmap.createBitmap(128, 128, android.graphics.Bitmap.Config.ARGB_8888)
        val random = java.util.Random()
        for (x in 0 until 128) {
            for (y in 0 until 128) {
                val v = random.nextInt(256)
                bitmap.setPixel(x, y, android.graphics.Color.argb(v, 255, 255, 255))
            }
        }
        bitmap.asImageBitmap()
    }
    
    val shader = remember(noiseBitmap) {
        ImageShader(noiseBitmap, TileMode.Repeated, TileMode.Repeated)
    }
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = ShaderBrush(shader),
            alpha = alpha,
            blendMode = BlendMode.Overlay
        )
    }
}

@Composable
fun ParticlesBackground(
    dominantColor: Color,
    isPlaying: Boolean,
    beatPulse: Float = 1f,
    motionIntensity: Float = 1f,
    highQuality: Boolean = true
) {
    val particleCount = if (highQuality) 35 else 20
    val particles = remember(particleCount) {
        List(particleCount) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 4.0f + 3.0f, // Ant-sized
                speed = Random.nextFloat() * 0.0002f + 0.00005f, // Slow base speed
                alpha = Random.nextFloat() * 0.2f + 0.4f // Moderate contrast alpha
            )
        }
    }

    // Tinted color derived from album art with moderate contrast
    val particleColor = remember(dominantColor) {
        val luminance = ColorUtils.calculateLuminance(dominantColor.toArgb())
        if (luminance < 0.1) {
            Color(ColorUtils.blendARGB(dominantColor.toArgb(), Color.White.toArgb(), 0.4f))
        } else {
            dominantColor
        }.copy(alpha = 0.65f)
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // Birth Rate: Link number of visible particles to beat intensity
        val visibleThreshold = (1f - ((beatPulse - 1f) * 0.8f)).coerceIn(0f, 1f)
        
        // River Tier Stretch: Particles stretch into lines on high beats
        val beatImpact = (beatPulse - 1f)
        val stretchFactor = if (beatImpact > 0.4f) (beatImpact - 0.4f) * 150f else 0f
        
        particles.forEachIndexed { index, particle ->
            // Skip drawing some particles if beat is low
            if (index.toFloat() / particleCount > (1f - visibleThreshold).coerceIn(0.4f, 1f)) return@forEachIndexed

            // Bloom effect on beat (Subtle)
            val kick = (beatPulse - 1f) * motionIntensity * 1.2f
            val effectiveSize = particle.size * (1f + kick * 0.2f)
            val effectiveAlpha = (particle.alpha * (1f + kick)).coerceAtMost(0.9f) * motionIntensity.coerceAtLeast(0.3f)
            
            if (stretchFactor > 2f) {
                // Lightning Lines: Draw vertical streaks on high beats
                val lineLength = (effectiveSize * 2f + stretchFactor).coerceAtMost(height * 0.2f)
                drawLine(
                    color = particleColor.copy(alpha = effectiveAlpha.coerceIn(0f, 1f)),
                    start = Offset(particle.x * width, particle.y * height),
                    end = Offset(particle.x * width, particle.y * height - lineLength),
                    strokeWidth = effectiveSize * 0.8f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            } else {
                // Normal Ants: Draw circles
                drawCircle(
                    color = particleColor.copy(alpha = effectiveAlpha.coerceIn(0f, 1f)),
                    radius = effectiveSize,
                    center = Offset(particle.x * width, particle.y * height)
                )
                
                // High Quality: Soft subtle glow
                if (highQuality && kick > 0.02f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(particleColor.copy(alpha = effectiveAlpha * 0.25f), Color.Transparent),
                            center = Offset(particle.x * width, particle.y * height),
                            radius = effectiveSize * 3.5f
                        ),
                        radius = effectiveSize * 3.5f,
                        center = Offset(particle.x * width, particle.y * height)
                    )
                }
            }
        }
    }

    LaunchedEffect(isPlaying, motionIntensity) {
        if (motionIntensity > 0.01f) {
            var momentum = 0f
            while (true) {
                withFrameMillis { frameTime ->
                    val beatImpact = (beatPulse - 1f)
                    
                    // High beat accumulation logic
                    if (beatImpact > 0.25f) {
                        momentum = (momentum + beatImpact * 0.05f).coerceAtMost(2.0f)
                    } else {
                        momentum = (momentum * 0.95f).coerceAtLeast(0f)
                    }

                    particles.forEach { particle ->
                        // River Physics Tier: Smooth, Normal, Drastic, or RIVER RUSH
                        val speedMultiplier = motionIntensity * when {
                            beatImpact < 0.1f -> 1.0f + (beatImpact * 1.5f) // Small: Smooth
                            beatImpact < 0.25f -> 1.2f + (beatImpact * 4f) // Medium: Normal
                            beatImpact < 0.5f -> 1.5f + (beatImpact * 25f) + (momentum * 15f) // High: Drastic
                            else -> 3.0f + (beatImpact * 80f) + (momentum * 50f) // RIVER TIER: Lightning Rush
                        }
                        
                        particle.y -= particle.speed * 20f * speedMultiplier
                        
                        if (particle.y < -0.2f) { // Extra margin for streaks
                            particle.y = 1.1f
                            particle.x = Random.nextFloat()
                        }
                    }
                }
            }
        }
    }
}

class Particle(
    var x: Float,
    var y: Float,
    val size: Float,
    val speed: Float,
    val alpha: Float
)

@Composable
fun GridLinesOverlay(
    beatPulse: Float = 1f,
    motionIntensity: Float = 1f
) {
    Canvas(modifier = Modifier.fillMaxSize().alpha(0.2f)) {
        val gridSize = 45.dp.toPx()
        val width = size.width
        val height = size.height
        
        // Warping effect on beat
        val kick = (beatPulse - 1f) * 40f * motionIntensity
        val strokeWidth = 1.2f + (kick * 0.2f)

        var x = 0f
        while (x < width) {
            val offsetX = kick * kotlin.math.sin(x * 0.01f)
            drawLine(
                color = Color.White,
                start = Offset(x + offsetX, 0f),
                end = Offset(x + offsetX, height),
                strokeWidth = strokeWidth
            )
            x += gridSize
        }

        var y = 0f
        while (y < height) {
            val offsetY = kick * kotlin.math.cos(y * 0.01f)
            drawLine(
                color = Color.White,
                start = Offset(0f, y + offsetY),
                end = Offset(width, y + offsetY),
                strokeWidth = strokeWidth
            )
            y += gridSize
        }
    }
}
