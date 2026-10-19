package com.example.candy.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.candy.model.CandyColor
import com.example.candy.model.CandyTile
import com.example.candy.model.SpecialCandyType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CandyTileView(
    tile: CandyTile,
    isSelected: Boolean,
    isHinted: Boolean,
    modifier: Modifier = Modifier
) {
    // 1. Custom Falling / Replacement Drop-in Animation
    val dropAnim = remember(tile.id) {
        Animatable(initialValue = -160f - (tile.row * 24f))
    }
    LaunchedEffect(tile.id) {
        dropAnim.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = 0.62f, // Bouncy playful landing
                stiffness = 380f
            )
        )
    }

    // 2. Custom Removal / Popping Animation
    val popAnim = remember(tile.isClearing) { Animatable(0f) }
    LaunchedEffect(tile.isClearing) {
        if (tile.isClearing) {
            popAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
            )
        } else {
            popAnim.snapTo(0f)
        }
    }

    // 3. Selection & Hint Pulses
    val infiniteTransition = rememberInfiniteTransition(label = "candy_anim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isSelected) 1.12f else if (isHinted) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isSelected) 400 else 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    // Compute dynamic removal transform
    val popProgress = popAnim.value
    val (candyScale, candyAlpha, candyRotation) = if (tile.isClearing) {
        val s = if (popProgress < 0.35f) {
            1f + (popProgress / 0.35f) * 0.3f // pop scale up to 1.3x
        } else {
            (1.3f * (1f - ((popProgress - 0.35f) / 0.65f))).coerceAtLeast(0f) // squash down to 0
        }
        val a = (1f - popProgress).coerceIn(0f, 1f)
        val r = popProgress * 40f
        Triple(s, a, r)
    } else {
        val s = if (isSelected || isHinted) pulseScale else 1f
        Triple(s, 1f, 0f)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = dropAnim.value
                scaleX = candyScale
                scaleY = candyScale
                alpha = candyAlpha
                rotationZ = candyRotation
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.5.dp)
        ) {
            val width = size.width
            val height = size.height

            // 1. Draw Jelly Layer underneath if present
            if (tile.hasJelly) {
                drawJelly(width, height)
            }

            // 2. Draw Selection or Hint Glow
            if (isSelected) {
                drawCircle(
                    color = Color(0xFFFFEB3B).copy(alpha = 0.5f),
                    radius = (width * 0.48f) * pulseScale,
                    center = Offset(width / 2, height / 2)
                )
                drawCircle(
                    color = Color.White,
                    radius = (width * 0.46f) * pulseScale,
                    center = Offset(width / 2, height / 2),
                    style = Stroke(width = 3.dp.toPx())
                )
            } else if (isHinted) {
                drawCircle(
                    color = Color(0xFF64FFDA).copy(alpha = 0.6f),
                    radius = (width * 0.46f) * pulseScale,
                    center = Offset(width / 2, height / 2),
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }

            // 3. Draw Base Candy Shape
            val center = Offset(width / 2, height / 2)
            val candyRadius = width * 0.38f

            when (tile.special) {
                SpecialCandyType.COLOR_BOMB -> {
                    drawColorBomb(center, candyRadius, spinAngle)
                }
                else -> {
                    drawStandardCandy(tile.color, center, candyRadius)

                    // 4. Draw Special Overlays
                    when (tile.special) {
                        SpecialCandyType.STRIPED_HORIZONTAL -> drawStripes(center, candyRadius, isHorizontal = true)
                        SpecialCandyType.STRIPED_VERTICAL -> drawStripes(center, candyRadius, isHorizontal = false)
                        SpecialCandyType.WRAPPED -> drawWrappedTruffle(center, candyRadius)
                        else -> {}
                    }
                }
            }

            // 5. Draw removal flash and bursting shockwave ring during clearing
            if (tile.isClearing && popProgress > 0f) {
                val flashAlpha = (sin(popProgress * PI.toFloat()) * 0.85f).coerceIn(0f, 1f)
                drawCircle(
                    color = Color.White.copy(alpha = flashAlpha),
                    radius = candyRadius * 1.15f,
                    center = center
                )
                // Expanding shockwave ring
                val ringRadius = candyRadius * (0.8f + popProgress * 1.6f)
                val ringAlpha = (1f - popProgress).coerceIn(0f, 1f)
                drawCircle(
                    color = Color.White.copy(alpha = ringAlpha * 0.7f),
                    radius = ringRadius,
                    center = center,
                    style = Stroke(width = 2.5.dp.toPx() * (1f - popProgress))
                )
            }
        }
    }
}

private fun DrawScope.drawJelly(width: Float, height: Float) {
    // Translucent pink gelatin base with beveled shine
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFF80AB).copy(alpha = 0.85f), Color(0xFFC2185B).copy(alpha = 0.7f)),
            center = Offset(width * 0.4f, height * 0.35f),
            radius = width * 0.7f
        ),
        size = Size(width, height),
        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.5f),
        size = Size(width, height),
        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
        style = Stroke(width = 1.5.dp.toPx())
    )
}

private fun DrawScope.drawStandardCandy(candyColor: CandyColor, center: Offset, radius: Float) {
    // Shadow
    drawCircle(
        color = Color(0x33000000),
        radius = radius * 0.95f,
        center = center.copy(y = center.y + radius * 0.15f)
    )

    when (candyColor) {
        CandyColor.RED -> {
            // Heart / Teardrop Candy
            val path = Path().apply {
                moveTo(center.x, center.y + radius * 0.85f)
                cubicTo(
                    center.x - radius * 1.1f, center.y - radius * 0.2f,
                    center.x - radius * 0.9f, center.y - radius * 0.9f,
                    center.x, center.y - radius * 0.4f
                )
                cubicTo(
                    center.x + radius * 0.9f, center.y - radius * 0.9f,
                    center.x + radius * 1.1f, center.y - radius * 0.2f,
                    center.x, center.y + radius * 0.85f
                )
                close()
            }
            drawPath(
                path = path,
                brush = Brush.radialGradient(
                    colors = listOf(candyColor.lightColor, candyColor.primaryColor, candyColor.darkColor),
                    center = Offset(center.x - radius * 0.25f, center.y - radius * 0.25f),
                    radius = radius * 1.1f
                )
            )
            // Specular shine
            drawCircle(
                color = Color.White.copy(alpha = 0.65f),
                radius = radius * 0.22f,
                center = Offset(center.x - radius * 0.35f, center.y - radius * 0.35f)
            )
        }
        CandyColor.YELLOW -> {
            // Diamond Lemon Drop
            val path = Path().apply {
                moveTo(center.x, center.y - radius)
                lineTo(center.x + radius, center.y)
                lineTo(center.x, center.y + radius)
                lineTo(center.x - radius, center.y)
                close()
            }
            drawPath(
                path = path,
                brush = Brush.radialGradient(
                    colors = listOf(candyColor.lightColor, candyColor.primaryColor, candyColor.darkColor),
                    center = Offset(center.x - radius * 0.2f, center.y - radius * 0.2f),
                    radius = radius * 1.2f
                )
            )
            // Glossy corner shine
            drawPath(
                path = Path().apply {
                    moveTo(center.x, center.y - radius * 0.85f)
                    lineTo(center.x + radius * 0.4f, center.y - radius * 0.45f)
                    lineTo(center.x, center.y - radius * 0.1f)
                    lineTo(center.x - radius * 0.4f, center.y - radius * 0.45f)
                    close()
                },
                color = Color.White.copy(alpha = 0.6f)
            )
        }
        CandyColor.GREEN -> {
            // Beveled Square Mint
            val rectSize = radius * 1.6f
            val topLeft = Offset(center.x - rectSize / 2, center.y - rectSize / 2)
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(candyColor.lightColor, candyColor.primaryColor, candyColor.darkColor),
                    center = Offset(center.x - radius * 0.2f, center.y - radius * 0.2f),
                    radius = radius * 1.3f
                ),
                topLeft = topLeft,
                size = Size(rectSize, rectSize),
                cornerRadius = CornerRadius(radius * 0.35f, radius * 0.35f)
            )
            // Inner rim shine
            drawRoundRect(
                color = Color.White.copy(alpha = 0.55f),
                topLeft = Offset(topLeft.x + 3.dp.toPx(), topLeft.y + 3.dp.toPx()),
                size = Size(rectSize * 0.45f, rectSize * 0.3f),
                cornerRadius = CornerRadius(radius * 0.2f, radius * 0.2f)
            )
        }
        CandyColor.ORANGE -> {
            // Glossy Orange Oval / Citrus slice
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(candyColor.lightColor, candyColor.primaryColor, candyColor.darkColor),
                    center = Offset(center.x - radius * 0.25f, center.y - radius * 0.25f),
                    radius = radius * 1.1f
                ),
                topLeft = Offset(center.x - radius * 0.95f, center.y - radius * 0.85f),
                size = Size(radius * 1.9f, radius * 1.7f)
            )
            // Specular crescent
            drawCircle(
                color = Color.White.copy(alpha = 0.65f),
                radius = radius * 0.24f,
                center = Offset(center.x - radius * 0.35f, center.y - radius * 0.3f)
            )
        }
        CandyColor.BLUE -> {
            // Blueberry Pop Sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(candyColor.lightColor, candyColor.primaryColor, candyColor.darkColor),
                    center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f),
                    radius = radius * 1.1f
                ),
                radius = radius,
                center = center
            )
            // Dual specular reflections
            drawCircle(
                color = Color.White.copy(alpha = 0.75f),
                radius = radius * 0.26f,
                center = Offset(center.x - radius * 0.32f, center.y - radius * 0.32f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                radius = radius * 0.12f,
                center = Offset(center.x + radius * 0.3f, center.y + radius * 0.3f)
            )
        }
        CandyColor.PURPLE -> {
            // Grape 6-point Star / Hexagon
            val path = Path()
            val points = 6
            for (i in 0 until points * 2) {
                val r = if (i % 2 == 0) radius else radius * 0.65f
                val angle = i * PI / points - PI / 2
                val px = center.x + (r * cos(angle)).toFloat()
                val py = center.y + (r * sin(angle)).toFloat()
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()

            drawPath(
                path = path,
                brush = Brush.radialGradient(
                    colors = listOf(candyColor.lightColor, candyColor.primaryColor, candyColor.darkColor),
                    center = Offset(center.x - radius * 0.2f, center.y - radius * 0.2f),
                    radius = radius * 1.2f
                )
            )
            // Star center gleam
            drawCircle(
                color = Color.White.copy(alpha = 0.7f),
                radius = radius * 0.22f,
                center = Offset(center.x - radius * 0.15f, center.y - radius * 0.15f)
            )
        }
    }
}

private fun DrawScope.drawStripes(center: Offset, radius: Float, isHorizontal: Boolean) {
    val stripeWidth = 4.dp.toPx()
    val count = 3
    for (i in -count..count) {
        val offset = i * (radius * 0.45f)
        if (isHorizontal) {
            drawLine(
                color = Color.White.copy(alpha = 0.85f),
                start = Offset(center.x - radius * 0.9f, center.y + offset),
                end = Offset(center.x + radius * 0.9f, center.y + offset),
                strokeWidth = stripeWidth
            )
        } else {
            drawLine(
                color = Color.White.copy(alpha = 0.85f),
                start = Offset(center.x + offset, center.y - radius * 0.9f),
                end = Offset(center.x + offset, center.y + radius * 0.9f),
                strokeWidth = stripeWidth
            )
        }
    }
}

private fun DrawScope.drawWrappedTruffle(center: Offset, radius: Float) {
    // Wrapper wings on left and right
    val wingPathLeft = Path().apply {
        moveTo(center.x - radius * 0.7f, center.y)
        lineTo(center.x - radius * 1.25f, center.y - radius * 0.6f)
        lineTo(center.x - radius * 1.15f, center.y)
        lineTo(center.x - radius * 1.25f, center.y + radius * 0.6f)
        close()
    }
    val wingPathRight = Path().apply {
        moveTo(center.x + radius * 0.7f, center.y)
        lineTo(center.x + radius * 1.25f, center.y - radius * 0.6f)
        lineTo(center.x + radius * 1.15f, center.y)
        lineTo(center.x + radius * 1.25f, center.y + radius * 0.6f)
        close()
    }

    drawPath(wingPathLeft, Color.White.copy(alpha = 0.8f))
    drawPath(wingPathRight, Color.White.copy(alpha = 0.8f))

    // Wrapped foil ring around candy
    drawCircle(
        color = Color.White.copy(alpha = 0.9f),
        radius = radius * 0.75f,
        center = center,
        style = Stroke(width = 3.dp.toPx())
    )
}

private fun DrawScope.drawColorBomb(center: Offset, radius: Float, spinAngle: Float) {
    // Chocolate sphere
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF5D4037), Color(0xFF3E2723), Color(0xFF1B0000)),
            center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f),
            radius = radius * 1.1f
        ),
        radius = radius,
        center = center
    )

    // Rainbow sprinkles orbiting around the chocolate ball
    val sprinkleColors = listOf(
        Color(0xFFFF1744), Color(0xFFFFD600), Color(0xFF00E676),
        Color(0xFF00B0FF), Color(0xFFAA00FF), Color(0xFFFF6D00),
        Color(0xFFFFFFFF), Color(0xFFFF4081)
    )

    for (i in 0 until 14) {
        val sprinkleAngle = (i * (360f / 14) + spinAngle) * (PI.toFloat() / 180f)
        val dist = radius * (0.35f + (i % 3) * 0.22f)
        val sx = center.x + cos(sprinkleAngle) * dist
        val sy = center.y + sin(sprinkleAngle) * dist
        val color = sprinkleColors[i % sprinkleColors.size]

        drawCircle(
            color = color,
            radius = 3.2.dp.toPx(),
            center = Offset(sx, sy)
        )
    }

    // Outer swirling rainbow halo ring
    drawCircle(
        brush = Brush.sweepGradient(
            colors = listOf(
                Color(0xFFFF1744), Color(0xFFFFD600), Color(0xFF00E676),
                Color(0xFF00B0FF), Color(0xFFAA00FF), Color(0xFFFF1744)
            ),
            center = center
        ),
        radius = radius * 1.08f,
        center = center,
        style = Stroke(width = 2.dp.toPx())
    )
}
