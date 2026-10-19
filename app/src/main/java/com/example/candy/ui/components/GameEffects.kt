package com.example.candy.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.candy.game.GameEngine
import com.example.candy.model.*
import com.example.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Modern Combo Banner with glowing aura, starburst rays, and bouncy spring entrance
 */
@Composable
fun ComboAnnouncementBanner(
    text: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = text != null,
        enter = scaleIn(
            initialScale = 0.3f,
            animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium)
        ) + fadeIn(),
        exit = scaleOut(targetScale = 1.35f) + fadeOut(animationSpec = tween(220)),
        modifier = modifier
    ) {
        if (text != null) {
            val infiniteTransition = rememberInfiniteTransition(label = "banner_rays")
            val rayRotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(6000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotation"
            )

            Box(contentAlignment = Alignment.Center) {
                // Background Radial Starburst
                Canvas(
                    modifier = Modifier
                        .size(260.dp, 100.dp)
                        .rotate(rayRotation)
                ) {
                    val count = 12
                    for (i in 0 until count) {
                        val angle = (i * 360f / count) * (PI / 180f)
                        val endX = center.x + cos(angle).toFloat() * 120f
                        val endY = center.y + sin(angle).toFloat() * 120f
                        drawLine(
                            color = Color(0x33FFEB3B),
                            start = center,
                            end = Offset(endX, endY),
                            strokeWidth = 14f,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Main Pill Badge with Modern Gradients
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFF1744),
                                    Color(0xFFFF007F),
                                    Color(0xFFFF9100),
                                    Color(0xFFFFD600)
                                )
                            ),
                            shape = RoundedCornerShape(32.dp)
                        )
                        .border(
                            width = 3.5.dp,
                            brush = Brush.linearGradient(
                                listOf(Color.White, Color(0xFFFFF9C4), Color.White)
                            ),
                            shape = RoundedCornerShape(32.dp)
                        )
                        .padding(horizontal = 32.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = text,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 2.5.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Modern Particle Sparks, Confetti, and Star Burst Overlay
 */
@Composable
fun ParticlesOverlay(
    particles: List<Particle>,
    modifier: Modifier = Modifier
) {
    if (particles.isEmpty()) return

    // Animate particle lifetime progress smoothly
    val progress = remember { Animatable(0f) }
    LaunchedEffect(particles) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 420, easing = LinearOutSlowInEasing)
        )
    }

    val t = progress.value

    Canvas(modifier = modifier.fillMaxSize()) {
        val boardW = size.width
        val boardH = size.height

        for (p in particles) {
            val curX = p.x + p.vx * t * 2.2f
            val curY = p.y + p.vy * t * 2.2f + (t * t * 25f) // subtle gravity
            val currentAlpha = ((1f - t) * p.alpha).coerceIn(0f, 1f)
            val currentSize = (p.size * (1f - (t * 0.45f))).coerceAtLeast(1f)

            if (curX in 0f..boardW && curY in 0f..boardH) {
                when (p.shape) {
                    ParticleShape.CIRCLE -> {
                        drawCircle(
                            color = p.color.copy(alpha = currentAlpha),
                            radius = currentSize,
                            center = Offset(curX, curY)
                        )
                    }
                    ParticleShape.STAR -> {
                        drawStar(
                            center = Offset(curX, curY),
                            radius = currentSize * 1.3f,
                            color = p.color.copy(alpha = currentAlpha),
                            rotationDeg = t * 180f
                        )
                    }
                    ParticleShape.SPARKLE -> {
                        drawSparkle(
                            center = Offset(curX, curY),
                            radius = currentSize * 1.4f,
                            color = p.color.copy(alpha = currentAlpha)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modern Shockwave expanding blast rings for matches and wrapped explosions
 */
@Composable
fun ShockwavesOverlay(
    shockwaves: List<Shockwave>,
    modifier: Modifier = Modifier
) {
    if (shockwaves.isEmpty()) return

    val progress = remember { Animatable(0f) }
    LaunchedEffect(shockwaves) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
        )
    }

    val t = progress.value

    Canvas(modifier = modifier.fillMaxSize()) {
        val cellSize = size.width / GameEngine.COLS

        for (sw in shockwaves) {
            val centerX = sw.col * cellSize + (cellSize / 2f)
            val centerY = sw.row * cellSize + (cellSize / 2f)
            val maxR = cellSize * 2.2f
            val curR = maxR * t
            val alpha = ((1f - t) * 0.85f).coerceIn(0f, 1f)

            // Outer Shockwave Ring
            drawCircle(
                color = sw.color.copy(alpha = alpha),
                radius = curR,
                center = Offset(centerX, centerY),
                style = Stroke(width = (8f * (1f - t)).coerceAtLeast(1.5f))
            )

            // Inner Core Flash
            if (t < 0.4f) {
                val coreAlpha = ((0.4f - t) / 0.4f).coerceIn(0f, 0.7f)
                drawCircle(
                    color = Color.White.copy(alpha = coreAlpha),
                    radius = curR * 0.45f,
                    center = Offset(centerX, centerY)
                )
            }
        }
    }
}

/**
 * Laser / Electric Beams Overlay when Striped Candies explode
 */
@Composable
fun LaserBeamsOverlay(
    lasers: List<LaserBeam>,
    modifier: Modifier = Modifier
) {
    if (lasers.isEmpty()) return

    val progress = remember { Animatable(0f) }
    LaunchedEffect(lasers) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
        )
    }

    val t = progress.value

    Canvas(modifier = modifier.fillMaxSize()) {
        val cellSize = size.width / GameEngine.COLS
        val alpha = ((1f - t) * 0.95f).coerceIn(0f, 1f)

        for (laser in lasers) {
            if (laser.isHorizontal) {
                val cy = laser.lineIndex * cellSize + (cellSize / 2f)
                // Outer Glow Beam
                drawLine(
                    color = laser.color.copy(alpha = alpha * 0.6f),
                    start = Offset(0f, cy),
                    end = Offset(size.width, cy),
                    strokeWidth = cellSize * 0.75f * (1f - (t * 0.3f)),
                    cap = StrokeCap.Round
                )
                // Bright White Inner Core Beam
                drawLine(
                    color = Color.White.copy(alpha = alpha),
                    start = Offset(0f, cy),
                    end = Offset(size.width, cy),
                    strokeWidth = cellSize * 0.22f,
                    cap = StrokeCap.Round
                )
            } else {
                val cx = laser.lineIndex * cellSize + (cellSize / 2f)
                // Outer Glow Beam
                drawLine(
                    color = laser.color.copy(alpha = alpha * 0.6f),
                    start = Offset(cx, 0f),
                    end = Offset(cx, size.height),
                    strokeWidth = cellSize * 0.75f * (1f - (t * 0.3f)),
                    cap = StrokeCap.Round
                )
                // Bright White Inner Core Beam
                drawLine(
                    color = Color.White.copy(alpha = alpha),
                    start = Offset(cx, 0f),
                    end = Offset(cx, size.height),
                    strokeWidth = cellSize * 0.22f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * Rainbow Energy Lightning Tentacles connecting Color Bomb to matched candies
 */
@Composable
fun RainbowTentaclesOverlay(
    tentacles: List<RainbowTentacle>,
    modifier: Modifier = Modifier
) {
    if (tentacles.isEmpty()) return

    val progress = remember { Animatable(0f) }
    LaunchedEffect(tentacles) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing)
        )
    }

    val t = progress.value

    Canvas(modifier = modifier.fillMaxSize()) {
        val cellSize = size.width / GameEngine.COLS

        for (tentacle in tentacles) {
            val startX = tentacle.fromPos.col * cellSize + (cellSize / 2f)
            val startY = tentacle.fromPos.row * cellSize + (cellSize / 2f)
            val targetX = tentacle.toPos.col * cellSize + (cellSize / 2f)
            val targetY = tentacle.toPos.row * cellSize + (cellSize / 2f)

            // Current beam endpoint interpolating towards target
            val curTargetX = startX + (targetX - startX) * t.coerceAtMost(1f)
            val curTargetY = startY + (targetY - startY) * t.coerceAtMost(1f)
            val alpha = ((1f - t) * 0.9f).coerceIn(0f, 1f)

            // Glow path with jitter
            val midX = (startX + curTargetX) / 2f + (if (t < 0.8f) (sin(t * 20f) * 12f) else 0f)
            val midY = (startY + curTargetY) / 2f + (if (t < 0.8f) (cos(t * 20f) * 12f) else 0f)

            val path = Path().apply {
                moveTo(startX, startY)
                quadraticTo(midX, midY, curTargetX, curTargetY)
            }

            // Glow lightning
            drawPath(
                path = path,
                color = tentacle.color.copy(alpha = alpha * 0.7f),
                style = Stroke(width = 10f * (1f - (t * 0.5f)), cap = StrokeCap.Round)
            )
            // Core white arc
            drawPath(
                path = path,
                color = Color.White.copy(alpha = alpha),
                style = Stroke(width = 4f, cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * Floating Upward Scores Overlay (+60, +120, +500)
 */
@Composable
fun FloatingScoresOverlay(
    scores: List<FloatingScore>,
    modifier: Modifier = Modifier
) {
    if (scores.isEmpty()) return

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val cellSize = maxWidth / GameEngine.COLS

        for (score in scores) {
            val animProgress = remember(score.id) { Animatable(0f) }
            LaunchedEffect(score.id) {
                animProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 650, easing = LinearOutSlowInEasing)
                )
            }

            val p = animProgress.value
            val floatOffsetY = -p * 50f
            val scale = if (p < 0.2f) (p / 0.2f) * 1.25f else 1.25f - ((p - 0.2f) * 0.35f)
            val alpha = (1f - p).coerceIn(0f, 1f)

            val xPos = (cellSize * score.col) + (cellSize * 0.1f)
            val yPos = (cellSize * score.row) + (cellSize * 0.1f)

            Box(
                modifier = Modifier
                    .offset(x = xPos, y = yPos + floatOffsetY.dp)
                    .scale(scale)
            ) {
                Text(
                    text = score.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = score.color.copy(alpha = alpha)
                )
            }
        }
    }
}

private fun DrawScope.drawStar(center: Offset, radius: Float, color: Color, rotationDeg: Float = 0f) {
    val path = Path()
    val points = 5
    val radRot = rotationDeg * (PI.toFloat() / 180f)
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) radius else radius * 0.5f
        val angle = (i * PI / points - PI / 2 + radRot).toFloat()
        val px = center.x + (r * cos(angle))
        val py = center.y + (r * sin(angle))
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    drawPath(path, color)
}

private fun DrawScope.drawSparkle(center: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        cubicTo(center.x, center.y, center.x, center.y, center.x + radius, center.y)
        cubicTo(center.x, center.y, center.x, center.y, center.x, center.y + radius)
        cubicTo(center.x, center.y, center.x, center.y, center.x - radius, center.y)
        cubicTo(center.x, center.y, center.x, center.y, center.x, center.y - radius)
        close()
    }
    drawPath(path, color)
}
