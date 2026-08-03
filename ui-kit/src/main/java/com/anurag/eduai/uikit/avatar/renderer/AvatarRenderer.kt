package com.anurag.eduai.uikit.avatar.renderer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import com.anurag.eduai.uikit.avatar.core.AvatarFrame
import com.anurag.eduai.uikit.avatar.core.AvatarState
import com.anurag.eduai.uikit.avatar.core.HandGesture
import com.anurag.eduai.uikit.avatar.core.Viseme
import com.anurag.eduai.uikit.avatar.ui.theme.AuroraTeal
import com.anurag.eduai.uikit.avatar.ui.theme.CheekBlush
import com.anurag.eduai.uikit.avatar.ui.theme.EyeBrown
import com.anurag.eduai.uikit.avatar.ui.theme.HairBrown
import com.anurag.eduai.uikit.avatar.ui.theme.HairHighlight
import com.anurag.eduai.uikit.avatar.ui.theme.LipColor
import com.anurag.eduai.uikit.avatar.ui.theme.OutfitAmber
import com.anurag.eduai.uikit.avatar.ui.theme.OutfitAmberDark
import com.anurag.eduai.uikit.avatar.ui.theme.OutfitCollar
import com.anurag.eduai.uikit.avatar.ui.theme.OutfitTeal
import com.anurag.eduai.uikit.avatar.ui.theme.OutfitTealDark
import com.anurag.eduai.uikit.avatar.ui.theme.SkinLight
import com.anurag.eduai.uikit.avatar.ui.theme.SkinMid
import com.anurag.eduai.uikit.avatar.ui.theme.SkinShadow
import kotlin.math.abs

/** Neckline/garment style for an outfit. */
private enum class CollarStyle { Crew, VNeck, Collared, Hoodie }

/** Colors + garment style that make up one outfit, shared by drawBody/drawArm. */
private data class OutfitStyle(
    val base: Color,
    val dark: Color,
    val collar: CollarStyle,
    val label: String
)

private val OutfitIndigo = Color(0xFF5A6BD8)
private val OutfitIndigoDark = Color(0xFF3B4AAE)
private val OutfitMaroon = Color(0xFF9E4759)
private val OutfitMaroonDark = Color(0xFF7A3143)

private val OutfitVariants = listOf(
    OutfitStyle(OutfitTeal, OutfitTealDark, CollarStyle.Crew, "Teal Tee"),
    OutfitStyle(OutfitAmber, OutfitAmberDark, CollarStyle.Collared, "Amber Shirt"),
    OutfitStyle(OutfitIndigo, OutfitIndigoDark, CollarStyle.Hoodie, "Indigo Hoodie"),
    OutfitStyle(OutfitMaroon, OutfitMaroonDark, CollarStyle.VNeck, "Maroon V-neck")
)

// Selectable hair colors (base + highlight) and spectacle frame colors.
private val HairPalette = listOf(
    HairBrown to HairHighlight,                // Brown
    Color(0xFF1A1A1E) to Color(0xFF3C3C44),    // Black
    Color(0xFF5A2E1E) to Color(0xFF7C4A34)     // Auburn
)
private val GlassPalette = listOf(
    Color(0xFF2B2B33),   // Black
    Color(0xFF5A3A22),   // Brown
    Color(0xFF29406B)    // Navy
)

/**
 * Shared layout so head, neck, torso, and arms share the same anchor points.
 *
 * Design rule: the neck belongs to the BODY (it never moves on its own), and the head
 * pivots on top of it. The head circle is drawn slightly oversized so its bottom edge
 * always overlaps the neck top, which is what keeps head turns from ever showing a gap
 * or "floating head" artifact.
 */
private object AvatarLayout {
    const val TORSO_TOP = 4f
    const val TORSO_WIDTH = 90f
    const val TORSO_HEIGHT = 118f
    const val SHOULDER_Y = 14f
    // Wide enough past the torso's own half-width (45) that the arm reads as a limb
    // beside the body instead of mostly overlapping/disappearing into the same-colored
    // shirt fabric (this was the "hands look attached with no visible arm" issue).
    const val SHOULDER_X = 51f

    // Neck: fixed to the body, tapers from shoulder width up to jaw width.
    const val NECK_TOP = -28f
    const val NECK_WIDTH_TOP = 26f
    const val NECK_WIDTH_BOTTOM = 36f

    // Head: pivots at (0, NECK_TOP) so it swivels naturally on the neck.
    const val HEAD_RADIUS = 44f
    const val HEAD_CY = -64f
    val HEAD_HALF_HEIGHT = HEAD_RADIUS * 1.15f

    // Arm: two segments (upper arm + forearm) so gestures can bend at the elbow.
    const val UPPER_ARM_LEN = 38f
    const val ARM_WIDTH = 17f
    const val HAND_RADIUS = 12f

    // Face metrics, derived from head position so everything stays in proportion.
    val eyeY = HEAD_CY + 6f
    val browY = eyeY - 14f
    val mouthY = HEAD_CY + 32f
    val eyeGapX = 21f
}

// Gestures/idle sway drive shoulder and elbow rotation quite a bit for the (removed) portrait
// hand overlay, but on this character it read as flailing. Scaling both down keeps gestures
// legible (a wave is still a wave) while keeping the arms mostly still, per explicit request
// for "very minimal arm movement".
// Idle sway stays gentle, but active gestures need enough travel to actually read as a
// clap / point / open-palm rather than a tiny twitch.
private const val ARM_ANGLE_DAMPING = 0.5f
private const val ELBOW_BEND_DAMPING = 0.55f

private enum class EmotionMouth { Auto, Smile, Frown, Wavy, Grin }

/** Per-emotion facial pose, so the face actually changes with the tutor's state. */
private class FaceExpr(
    val browRaise: Float,   // + = brows lifted
    val browInner: Float,   // + = inner ends pulled down/together (worried/cross)
    val browAsym: Boolean,  // one brow up (thinking / skeptical)
    val mouthMode: EmotionMouth,
    val mouthAmount: Float,
    val eyeOpen: Float,
    val cheek: Float,
    val gazeBiasY: Float    // nudge pupils up/down (thinking glances up)
)

private fun faceExprFor(frame: AvatarFrame, smile: Float): FaceExpr = when (frame.state) {
    AvatarState.Happy -> FaceExpr(0.4f, -0.2f, false, EmotionMouth.Smile, 0.8f, 0.9f, 0.55f, 0f)
    AvatarState.Celebrating -> FaceExpr(0.7f, -0.3f, false, EmotionMouth.Grin, 1f, 0.85f, 0.65f, 0f)
    AvatarState.Thinking -> FaceExpr(0.2f, 0.15f, true, EmotionMouth.Smile, 0.2f, 1f, 0.12f, -5f)
    AvatarState.Confused -> FaceExpr(-0.1f, 0.5f, false, EmotionMouth.Wavy, 0.5f, 1f, 0.12f, 2f)
    AvatarState.Listening -> FaceExpr(0.35f, -0.1f, false, EmotionMouth.Smile, 0.4f, 1f, 0.22f, 0f)
    AvatarState.Speaking, AvatarState.Explaining ->
        FaceExpr(0.25f, 0f, false, EmotionMouth.Auto, 0.5f, 1f, 0.18f + smile * 0.2f, 0f)
    AvatarState.Idle -> FaceExpr(0.15f, 0f, false, EmotionMouth.Auto, 0.4f + smile * 0.3f, 1f, 0.12f + smile * 0.2f, 0f)
}

/** Manually chosen mood for preview: 1 Happy, 2 Angry, 3 Thinking, 4 Surprised, 5 Sad. */
private fun moodExpr(mood: Int): FaceExpr = when (mood) {
    1 -> FaceExpr(0.5f, -0.2f, false, EmotionMouth.Smile, 0.9f, 0.9f, 0.6f, 0f)   // Happy
    2 -> FaceExpr(-0.3f, 0.8f, false, EmotionMouth.Frown, 0.6f, 0.82f, 0f, 3f)    // Angry (brows down+in, frown)
    3 -> FaceExpr(0.2f, 0.15f, true, EmotionMouth.Smile, 0.2f, 1f, 0.12f, -6f)    // Thinking (one brow up, glance up)
    4 -> FaceExpr(0.8f, -0.1f, false, EmotionMouth.Grin, 0.7f, 1.25f, 0.2f, 0f)   // Surprised (brows up, wide eyes, open)
    else -> FaceExpr(-0.15f, 0.45f, false, EmotionMouth.Frown, 0.35f, 0.95f, 0f, 3f) // Sad
}

@Composable
fun AvatarRenderer(
    frame: AvatarFrame,
    modifier: Modifier = Modifier,
    outfitVariant: Int = 0,
    hairStyle: Int = 0,
    hairColor: Int = 0,
    glassesStyle: Int = 0,
    glassesColor: Int = 0,
    neckStyle: Int = 0,
    underEyeLine: Boolean = false,
    cheekShading: Boolean = true,
    moodOverride: Int = 0,
    gestureOverride: Int = 0,
    spinTrigger: Int = 0
) {
    val interaction = rememberAvatarInteraction()

    // One-shot 360° spin when the spin button is pressed (spinTrigger increments).
    val spin = remember { Animatable(0f) }
    LaunchedEffect(spinTrigger) {
        if (spinTrigger > 0) {
            spin.snapTo(0f)
            spin.animateTo(360f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
        }
    }
    // Softens the otherwise-instant snap between discrete viseme mouth shapes. Kept gentle
    // (small pop, quick settle) so rapid speech doesn't read as a vibrating mouth.
    val mouthPop = rememberPopOnChange(frame.viseme, restScale = 0.92f, durationMs = 70)
    val outfit = OutfitVariants[outfitVariant.coerceIn(0, OutfitVariants.lastIndex)]

    Box(
        modifier = modifier.then(interaction.modifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val pulse = interaction.pulse
            val scaleFactor = (size.minDimension / 320f) * (1f + pulse * 0.06f)
            val cx = size.width / 2f
            val cy = size.height * 0.52f

            // A tap makes the avatar glance at you, smile a bit more, and give a little wave.
            val effectivePupilX = frame.pupilOffsetX + interaction.gazeX * 3.4f
            val effectivePupilY = frame.pupilOffsetY + interaction.gazeY * 2.6f
            val effectiveHeadY = frame.headRotationY + interaction.gazeX * 6f
            val effectiveHeadX = frame.headRotationX + interaction.gazeY * 3.5f
            val effectiveSmile = (frame.smileAmount + pulse * 0.55f).coerceIn(0f, 1f)
            val effectiveBrow = (frame.eyebrowOffset + pulse * 0.4f).coerceIn(-1f, 1f)
            // Facial expression: engine-driven, unless a mood is manually chosen for preview.
            val expr = if (moodOverride == 0) faceExprFor(frame, effectiveSmile) else moodExpr(moodOverride)

            val effectiveRightGesture = if (pulse > 0.15f) HandGesture.Wave else frame.rightHandGesture
            val effectiveRightArmAngle = (frame.rightArmAngle + pulse * 26f) * ARM_ANGLE_DAMPING
            val effectiveLeftArmAngle = frame.leftArmAngle * ARM_ANGLE_DAMPING

            // Manual gesture override (preview): pose one or both arms in a chosen gesture.
            var leftGesture = frame.leftHandGesture
            var leftAngle = effectiveLeftArmAngle
            var rightGesture = effectiveRightGesture
            var rightAngle = effectiveRightArmAngle
            if (gestureOverride != 0) {
                val lift = 15f
                when (gestureOverride) {
                    1 -> { leftGesture = HandGesture.Wave; rightGesture = HandGesture.Wave; leftAngle = lift; rightAngle = lift }
                    2 -> { leftGesture = HandGesture.Clap; rightGesture = HandGesture.Clap; leftAngle = lift; rightAngle = lift }
                    3 -> { leftGesture = HandGesture.PointForward; rightGesture = HandGesture.None; leftAngle = lift; rightAngle = 0f }
                    4 -> { leftGesture = HandGesture.OpenPalm; rightGesture = HandGesture.OpenPalm; leftAngle = 13f; rightAngle = lift }
                    else -> { leftGesture = HandGesture.Think; rightGesture = HandGesture.None; leftAngle = lift; rightAngle = 0f }
                }
            }
            // A wider head turn narrows the silhouette a touch, faking perspective on a 2D face.
            val headTurnSqueeze = 1f - (abs(effectiveHeadY) / 90f * 0.16f).coerceIn(0f, 0.16f)

            translate(left = cx, top = cy - frame.bodyBounce * 0.9f) {
              rotate(spin.value, pivot = Offset.Zero) {
                withUniformScale(scaleFactor = scaleFactor, pivot = Offset.Zero) {
                    drawStageShadow()

                    rotate(frame.bodySway + frame.bodyLean, pivot = Offset(0f, AvatarLayout.SHOULDER_Y + 40f)) {
                        // Neck first, so the body/collar is drawn IN FRONT of the neck base
                        // (the shirt sits over the neck, not the other way around).
                        drawNeck(neckStyle)
                        drawBody(frame.bodyBreathing, outfit)
                        drawArm(-AvatarLayout.SHOULDER_X, leftAngle, leftGesture, isLeft = true, outfit = outfit)
                        drawArm(AvatarLayout.SHOULDER_X, rightAngle, rightGesture, isLeft = false, outfit = outfit)
                    }

                    rotate(frame.headRotationZ, pivot = Offset(0f, AvatarLayout.NECK_TOP)) {
                        rotate(effectiveHeadX, pivot = Offset(0f, AvatarLayout.NECK_TOP)) {
                            rotate(effectiveHeadY, pivot = Offset(0f, AvatarLayout.NECK_TOP)) {
                                scale(scaleX = headTurnSqueeze, scaleY = 1f, pivot = Offset(0f, AvatarLayout.NECK_TOP)) {
                                    drawHead(cheekShading)
                                    drawHair(hairStyle, hairColor)
                                    drawEars()
                                    if (cheekShading) {
                                        drawCheeks((expr.cheek + effectiveSmile * 0.3f).coerceIn(0f, 1f))
                                    }
                                    drawEyebrows(expr, effectiveBrow)
                                    drawNose()
                                    drawEyes(frame, expr, effectivePupilX, effectivePupilY + expr.gazeBiasY, underEyeLine)
                                    drawGlasses(glassesStyle, glassesColor)
                                    scale(scaleX = mouthPop, scaleY = mouthPop, pivot = Offset(0f, AvatarLayout.mouthY)) {
                                        drawMouth(frame, expr, effectiveSmile)
                                    }
                                }
                            }
                        }
                    }
                }
              }
            }

            interaction.tapPosition?.let { pos ->
                if (pulse > 0.01f) {
                    drawCircle(
                        color = AuroraTeal.copy(alpha = pulse * 0.4f),
                        radius = 26f + (1f - pulse) * 46f,
                        center = pos,
                        style = Stroke(width = 2.5f + pulse * 1.5f)
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawStageShadow() {
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color.Black.copy(alpha = 0.22f), Color.Transparent),
            center = Offset(0f, 95f),
            radius = 70f
        ),
        topLeft = Offset(-70f, 85f),
        size = Size(140f, 28f)
    )
}

private fun DrawScope.drawBody(breathing: Float, outfit: OutfitStyle) {
    val halfW = AvatarLayout.TORSO_WIDTH / 2f
    val top = AvatarLayout.TORSO_TOP + breathing * 0.2f

    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(outfit.base, outfit.dark),
            startY = top,
            endY = top + AvatarLayout.TORSO_HEIGHT
        ),
        topLeft = Offset(-halfW + breathing * 0.15f, top),
        size = Size(AvatarLayout.TORSO_WIDTH + breathing * 0.3f, AvatarLayout.TORSO_HEIGHT),
        cornerRadius = CornerRadius(22f, 22f)
    )

    // Shoulder caps blend arms into the torso, with a soft seam shadow behind each one
    // so the sleeve reads as attached rather than pasted flat on top.
    listOf(-AvatarLayout.SHOULDER_X, AvatarLayout.SHOULDER_X).forEach { shoulderX ->
        drawCircle(
            color = outfit.dark.copy(alpha = 0.4f),
            radius = 13f,
            center = Offset(shoulderX, AvatarLayout.SHOULDER_Y + 2f)
        )
        drawCircle(
            color = outfit.base,
            radius = 12f,
            center = Offset(shoulderX, AvatarLayout.SHOULDER_Y)
        )
    }

    drawCollar(top, outfit)

    // Soft fabric highlight for a touch of depth instead of a flat fill.
    drawArc(
        color = Color.White.copy(alpha = 0.08f),
        startAngle = 200f,
        sweepAngle = 100f,
        useCenter = false,
        topLeft = Offset(-halfW * 0.7f, top + 6f),
        size = Size(halfW * 1.4f, 40f),
        style = Stroke(width = 6f, cap = StrokeCap.Round)
    )
}

/** Draws the neckline/garment detail that distinguishes each outfit style. */
private fun DrawScope.drawCollar(top: Float, outfit: OutfitStyle) {
    when (outfit.collar) {
        CollarStyle.Crew -> {
            val collarPath = Path().apply {
                moveTo(-27f, top + 1f)
                quadraticTo(0f, top + 17f, 27f, top + 1f)
                lineTo(27f, top - 3f)
                quadraticTo(0f, top + 9f, -27f, top - 3f)
                close()
            }
            drawPath(collarPath, color = OutfitCollar)
        }
        CollarStyle.VNeck -> {
            // Darker fabric V dipping from the collar bones to a point.
            val v = Path().apply {
                moveTo(-26f, top - 3f)
                lineTo(0f, top + 34f)
                lineTo(26f, top - 3f)
                lineTo(20f, top - 3f)
                lineTo(0f, top + 24f)
                lineTo(-20f, top - 3f)
                close()
            }
            drawPath(v, color = outfit.dark)
        }
        CollarStyle.Collared -> {
            // White inner placket + two collar flaps.
            val inner = Path().apply {
                moveTo(-20f, top - 2f)
                quadraticTo(0f, top + 18f, 20f, top - 2f)
                lineTo(20f, top - 5f)
                quadraticTo(0f, top + 11f, -20f, top - 5f)
                close()
            }
            drawPath(inner, OutfitCollar)
            val leftFlap = Path().apply { moveTo(-22f, top - 3f); lineTo(-3f, top + 13f); lineTo(-19f, top + 15f); close() }
            val rightFlap = Path().apply { moveTo(22f, top - 3f); lineTo(3f, top + 13f); lineTo(19f, top + 15f); close() }
            drawPath(leftFlap, OutfitCollar)
            drawPath(rightFlap, OutfitCollar)
            drawPath(leftFlap, color = outfit.dark.copy(alpha = 0.35f), style = Stroke(width = 1.5f))
            drawPath(rightFlap, color = outfit.dark.copy(alpha = 0.35f), style = Stroke(width = 1.5f))
        }
        CollarStyle.Hoodie -> {
            // Gathered hood band around the neck + drawstrings.
            drawRoundRect(
                color = outfit.dark,
                topLeft = Offset(-32f, top - 6f),
                size = Size(64f, 22f),
                cornerRadius = CornerRadius(12f)
            )
            val collarPath = Path().apply {
                moveTo(-22f, top + 3f)
                quadraticTo(0f, top + 15f, 22f, top + 3f)
                lineTo(22f, top - 1f)
                quadraticTo(0f, top + 9f, -22f, top - 1f)
                close()
            }
            drawPath(collarPath, color = SkinShadow.copy(alpha = 0.55f))
            listOf(-1f, 1f).forEach { s ->
                drawLine(OutfitCollar, Offset(s * 7f, top + 12f), Offset(s * 9f, top + 42f), strokeWidth = 2.5f, cap = StrokeCap.Round)
                drawCircle(OutfitCollar, radius = 2.6f, center = Offset(s * 9f, top + 44f))
            }
        }
    }
}

/**
 * Neck is anchored to the body/shoulders — it never rotates on its own, so it never
 * detaches from the head. [shape]: 0 = regular, 1 = slim, 2 = broad.
 */
private fun DrawScope.drawNeck(shape: Int) {
    val topY = AvatarLayout.NECK_TOP
    val bottomY = AvatarLayout.TORSO_TOP + 10f
    val widthScale = when (shape) {
        1 -> 0.78f
        2 -> 1.28f
        else -> 1f
    }
    val topHalf = AvatarLayout.NECK_WIDTH_TOP / 2f * widthScale
    val bottomHalf = AvatarLayout.NECK_WIDTH_BOTTOM / 2f * widthScale

    val path = Path().apply {
        moveTo(-topHalf, topY)
        lineTo(topHalf, topY)
        lineTo(bottomHalf, bottomY)
        lineTo(-bottomHalf, bottomY)
        close()
    }
    drawPath(
        path,
        brush = Brush.verticalGradient(listOf(SkinMid, SkinShadow), startY = topY, endY = bottomY)
    )
    drawLine(
        color = SkinShadow.copy(alpha = 0.35f),
        start = Offset(0f, topY + 4f),
        end = Offset(0f, bottomY - 2f),
        strokeWidth = 1.5f
    )
}

private fun DrawScope.drawHead(showShading: Boolean) {
    val r = AvatarLayout.HEAD_RADIUS
    val cy = AvatarLayout.HEAD_CY
    val halfH = AvatarLayout.HEAD_HALF_HEIGHT

    // Slightly egg-shaped (taller than wide) so it reads as a face, not a balloon.
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(SkinLight, SkinMid, SkinShadow),
            center = Offset(-r * 0.2f, cy - halfH * 0.35f),
            radius = r * 1.6f
        ),
        topLeft = Offset(-r, cy - halfH),
        size = Size(r * 2f, halfH * 2f)
    )
    // Jaw shading for definition — the soft skin-shadow patches along the lower cheeks/jaw.
    // Optional (some prefer a flatter, cleaner face without them).
    if (showShading) {
        drawArc(
            color = SkinShadow.copy(alpha = 0.22f),
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(-r * 0.9f, cy + halfH * 0.05f),
            size = Size(r * 1.8f, halfH * 0.9f),
            style = Stroke(width = r * 0.22f)
        )
    }
    // Soft forehead/temple highlight for a touch of roundness and depth.
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
            center = Offset(-r * 0.32f, cy - halfH * 0.42f),
            radius = r * 0.7f
        ),
        topLeft = Offset(-r * 0.85f, cy - halfH * 0.85f),
        size = Size(r * 1.1f, halfH * 0.8f)
    )
}

/** Selectable hairstyle (0 tousled, 1 side-part, 2 curly) + color index into HairPalette. */
private fun DrawScope.drawHair(style: Int, colorIdx: Int) {
    val r = AvatarLayout.HEAD_RADIUS
    val headTop = AvatarLayout.HEAD_CY - AvatarLayout.HEAD_HALF_HEIGHT
    val (base, hl) = HairPalette[colorIdx.coerceIn(0, HairPalette.lastIndex)]
    when (style) {
        1 -> drawHairSidePart(r, headTop, base, hl)
        2 -> drawHairCurly(r, headTop, base, hl)
        else -> drawHairTousled(r, headTop, base, hl)
    }
}

private fun DrawScope.drawHairHighlight(r: Float, headTop: Float, hl: Color) {
    drawArc(
        color = hl.copy(alpha = 0.4f),
        startAngle = 200f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(-r * 0.75f, headTop - r * 0.1f),
        size = Size(r * 1.0f, r * 0.75f),
        style = Stroke(width = r * 0.09f)
    )
}

private fun DrawScope.drawHairTousled(r: Float, headTop: Float, base: Color, hl: Color) {
    val path = Path().apply {
        moveTo(-r * 1.08f, headTop + r * 0.55f)
        cubicTo(-r * 1.15f, headTop - r * 0.55f, -r * 0.35f, headTop - r * 0.68f, 0f, headTop - r * 0.5f)
        cubicTo(r * 0.35f, headTop - r * 0.68f, r * 1.15f, headTop - r * 0.55f, r * 1.08f, headTop + r * 0.55f)
        cubicTo(r * 1.02f, headTop + r * 0.85f, r * 0.7f, headTop + r * 1.0f, r * 0.55f, headTop + r * 0.7f)
        lineTo(r * 0.5f, headTop + r * 0.5f)
        cubicTo(r * 0.3f, headTop + r * 0.75f, -r * 0.3f, headTop + r * 0.75f, -r * 0.5f, headTop + r * 0.5f)
        lineTo(-r * 0.55f, headTop + r * 0.7f)
        cubicTo(-r * 0.7f, headTop + r * 1.0f, -r * 1.02f, headTop + r * 0.85f, -r * 1.08f, headTop + r * 0.55f)
        close()
    }
    drawPath(path, base)
    drawHairHighlight(r, headTop, hl)
}

private fun DrawScope.drawHairSidePart(r: Float, headTop: Float, base: Color, hl: Color) {
    // Smooth combed cap with a side-swept fringe crossing the forehead — neat/professional.
    val path = Path().apply {
        moveTo(-r * 1.05f, headTop + r * 0.75f)
        cubicTo(-r * 1.12f, headTop - r * 0.22f, -r * 0.45f, headTop - r * 0.4f, 0f, headTop - r * 0.36f)
        cubicTo(r * 0.55f, headTop - r * 0.4f, r * 1.12f, headTop - r * 0.15f, r * 1.05f, headTop + r * 0.75f)
        // Right side sweeps down and the fringe dips across to the left (the part).
        cubicTo(r * 0.98f, headTop + r * 0.3f, r * 0.55f, headTop + r * 0.32f, r * 0.28f, headTop + r * 0.5f)
        cubicTo(-r * 0.05f, headTop + r * 0.7f, -r * 0.2f, headTop + r * 0.42f, -r * 0.2f, headTop + r * 0.58f)
        cubicTo(-r * 0.35f, headTop + r * 0.3f, -r * 0.78f, headTop + r * 0.34f, -r * 1.05f, headTop + r * 0.75f)
        close()
    }
    drawPath(path, base)
    // Parting sheen line.
    drawLine(
        color = hl.copy(alpha = 0.5f),
        start = Offset(r * 0.1f, headTop + r * 0.05f),
        end = Offset(r * 0.62f, headTop - r * 0.18f),
        strokeWidth = r * 0.06f,
        cap = StrokeCap.Round
    )
    drawHairHighlight(r, headTop, hl)
}

private fun DrawScope.drawHairCurly(r: Float, headTop: Float, base: Color, hl: Color) {
    // Voluminous curls: a rounded crown mass with a bumpy top made of overlapping puffs.
    drawOval(
        color = base,
        topLeft = Offset(-r * 1.12f, headTop - r * 0.2f),
        size = Size(r * 2.24f, r * 1.5f)
    )
    // Curl puffs around the upper silhouette.
    val puffs = listOf(
        -0.85f to 0.28f, -0.6f to -0.05f, -0.3f to -0.24f, 0f to -0.3f,
        0.3f to -0.24f, 0.6f to -0.05f, 0.85f to 0.28f, -0.95f to 0.55f, 0.95f to 0.55f
    )
    puffs.forEach { (fx, fy) ->
        drawCircle(color = base, radius = r * 0.32f, center = Offset(r * fx, headTop + r * (fy + 0.35f)))
    }
    // A couple of soft highlight curls.
    listOf(-0.4f, 0.15f).forEach { fx ->
        drawCircle(
            color = hl.copy(alpha = 0.35f),
            radius = r * 0.12f,
            center = Offset(r * fx, headTop + r * 0.18f)
        )
    }
}

private fun DrawScope.drawEars() {
    val r = AvatarLayout.HEAD_RADIUS
    val earY = AvatarLayout.HEAD_CY + r * 0.05f
    listOf(-r * 0.98f, r * 0.98f).forEach { x ->
        drawOval(color = SkinMid, topLeft = Offset(x - 7f, earY - 10f), size = Size(14f, 20f))
        drawOval(
            color = SkinShadow.copy(alpha = 0.5f),
            topLeft = Offset(x - 4f, earY - 6f),
            size = Size(8f, 12f),
            style = Stroke(width = 1.5f)
        )
    }
}

private fun DrawScope.drawCheeks(smile: Float) {
    if (smile < 0.15f) return
    val cheekY = AvatarLayout.eyeY + 20f
    listOf(-27f, 27f).forEach { x ->
        drawCircle(
            color = CheekBlush.copy(alpha = 0.15f + smile * 0.25f),
            radius = 9f + smile * 3f,
            center = Offset(x, cheekY)
        )
    }
}

private fun DrawScope.drawEyebrows(expr: FaceExpr, offset: Float) {
    val gap = AvatarLayout.eyeGapX
    val baseRaise = expr.browRaise * 8f + offset * 6f
    listOf(-1f, 1f).forEach { side ->
        // One brow lifts higher for the "thinking / skeptical" look.
        val raise = if (expr.browAsym && side > 0f) baseRaise + 6f else baseRaise
        val y = AvatarLayout.browY - raise
        val innerX = side * (gap - 12f)
        val outerX = side * (gap + 10f)
        // browInner > 0 pulls the inner ends down/together (worried, cross); < 0 lifts them.
        val innerY = y - 4f + expr.browInner * 6f
        drawLine(
            color = HairBrown,
            start = Offset(outerX, y),
            end = Offset(innerX, innerY),
            strokeWidth = 3.4f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawNose() {
    val y = (AvatarLayout.eyeY + AvatarLayout.mouthY) / 2f + 2f
    val bridge = Path().apply {
        moveTo(-2.5f, y - 10f)
        quadraticTo(-5f, y, 0f, y + 3f)
    }
    drawPath(bridge, color = SkinShadow.copy(alpha = 0.55f), style = Stroke(width = 1.6f, cap = StrokeCap.Round))
    drawOval(color = SkinShadow.copy(alpha = 0.35f), topLeft = Offset(-4.5f, y + 1f), size = Size(4f, 2.4f))
    drawOval(color = SkinShadow.copy(alpha = 0.35f), topLeft = Offset(0.5f, y + 1f), size = Size(4f, 2.4f))
}

private fun DrawScope.drawEyes(frame: AvatarFrame, expr: FaceExpr, pupilOffsetX: Float, pupilOffsetY: Float, showUnderEyeLine: Boolean) {
    val eyeY = AvatarLayout.eyeY
    // Continuous close/open instead of a hard on/off snap; expression can narrow them too.
    val blink = (1f - (frame.eyeBlinkProgress * 0.92f + frame.eyeSquint * 0.35f))
        .coerceIn(0.08f, 1f) * expr.eyeOpen
    val surprise = 1f + frame.eyeSurprise * 0.35f
    val gap = AvatarLayout.eyeGapX

    listOf(-gap, gap).forEach { eyeX ->
        val w = 23f * surprise
        val h = 15f * blink * surprise

        drawOval(
            color = Color.White,
            topLeft = Offset(eyeX - w / 2f, eyeY - h / 2f),
            size = Size(w, h)
        )

        if (blink > 0.35f) {
            val irisRadius = 7.4f * surprise
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(EyeBrown, Color(0xFF1A0F0A)),
                    center = Offset(eyeX + pupilOffsetX, eyeY + pupilOffsetY),
                    radius = irisRadius
                ),
                radius = irisRadius * 0.78f,
                center = Offset(eyeX + pupilOffsetX * 0.8f, eyeY + pupilOffsetY * 0.8f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = 2f,
                center = Offset(eyeX + pupilOffsetX * 0.5f - 2f, eyeY + pupilOffsetY * 0.5f - 3f)
            )
        }

        // Eyelid crease — keeps the eye looking sculpted even when wide open.
        drawArc(
            color = SkinShadow.copy(alpha = 0.4f),
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(eyeX - w / 2f - 1f, eyeY - h / 2f - 5f),
            size = Size(w + 2f, h + 8f),
            style = Stroke(width = 1.6f, cap = StrokeCap.Round)
        )
        // Optional, softened under-eye line (off by default — it read too strong).
        if (showUnderEyeLine) {
            drawLine(
                color = Color(0xFF3A2A22).copy(alpha = 0.28f),
                start = Offset(eyeX - w * 0.36f, eyeY + h / 2f + 1f),
                end = Offset(eyeX + w * 0.36f, eyeY + h / 2f + 1f),
                strokeWidth = 1f
            )
        }
    }
}

private val MouthCavity = Color(0xFF5C3330)
private val TeethColor = Color(0xFFFFF6EC)
private val TongueColor = Color(0xFFE8927E)
private val FvLipShadow = Color(0xFF8B4A42)

/** Rounded spectacles over the eyes — a friendly "tutor" cue. */
private fun DrawScope.drawGlasses(style: Int, colorIdx: Int) {
    if (style == 2) return
    val frame = GlassPalette[colorIdx.coerceIn(0, GlassPalette.lastIndex)]
    val eyeY = AvatarLayout.eyeY
    val gap = AvatarLayout.eyeGapX
    val r = AvatarLayout.HEAD_RADIUS
    val lensHalf: Float
    if (style == 1) {
        // Round lenses.
        lensHalf = 15f
        listOf(-gap, gap).forEach { eyeX ->
            drawCircle(Color.White.copy(alpha = 0.08f), radius = lensHalf, center = Offset(eyeX, eyeY))
            drawCircle(frame, radius = lensHalf, center = Offset(eyeX, eyeY), style = Stroke(width = 2.2f))
        }
    } else {
        // Classic rounded-rectangle lenses.
        val lensW = 28f
        val lensH = 22f
        lensHalf = lensW / 2f
        listOf(-gap, gap).forEach { eyeX ->
            drawRoundRect(
                color = Color.White.copy(alpha = 0.08f),
                topLeft = Offset(eyeX - lensW / 2f, eyeY - lensH / 2f),
                size = Size(lensW, lensH),
                cornerRadius = CornerRadius(7f)
            )
            drawRoundRect(
                color = frame,
                topLeft = Offset(eyeX - lensW / 2f, eyeY - lensH / 2f),
                size = Size(lensW, lensH),
                cornerRadius = CornerRadius(7f),
                style = Stroke(width = 2.2f)
            )
        }
    }
    // Bridge + temple arms (shared).
    drawLine(frame, Offset(-gap + lensHalf - 1f, eyeY - 2f), Offset(gap - lensHalf + 1f, eyeY - 2f), strokeWidth = 2.2f, cap = StrokeCap.Round)
    drawLine(frame, Offset(-gap - lensHalf, eyeY - 3f), Offset(-r * 0.95f, eyeY - 1f), strokeWidth = 2.2f, cap = StrokeCap.Round)
    drawLine(frame, Offset(gap + lensHalf, eyeY - 3f), Offset(r * 0.95f, eyeY - 1f), strokeWidth = 2.2f, cap = StrokeCap.Round)
}

/**
 * Chooses what the mouth does: while actively speaking it lip-syncs the viseme; otherwise it
 * shows the current emotion's mouth (smile, grin, frown, confused squiggle). This is what
 * lets the face read as happy / thinking / confused rather than a single fixed mouth.
 */
private fun DrawScope.drawMouth(frame: AvatarFrame, expr: FaceExpr, smile: Float) {
    // Lip-sync whenever actually speaking — in ANY emotion (happy/celebrating/confused), not
    // just Speaking/Explaining. Otherwise sentences like "Let's see..." (which map to Happy)
    // showed a static smile with no mouth movement.
    if (frame.isSpeaking) {
        drawVisemeMouth(frame.viseme, smile)
        return
    }
    val y = AvatarLayout.mouthY
    val lip = LipColor
    when (expr.mouthMode) {
        EmotionMouth.Grin -> {
            val mouth = Path().apply {
                moveTo(-14f, y - 2f)
                cubicTo(-7f, y - 8f, 7f, y - 8f, 14f, y - 2f)
                cubicTo(10f, y + 13f, -10f, y + 13f, -14f, y - 2f)
                close()
            }
            drawPath(mouth, MouthCavity)
            drawArc(color = TongueColor, startAngle = 0f, sweepAngle = 180f, useCenter = true, topLeft = Offset(-6f, y + 5f), size = Size(12f, 9f))
            drawRoundRect(color = TeethColor, topLeft = Offset(-9f, y - 3f), size = Size(18f, 4f), cornerRadius = CornerRadius(2f))
        }
        EmotionMouth.Frown -> drawArc(
            color = lip, startAngle = 195f, sweepAngle = 150f, useCenter = false,
            topLeft = Offset(-11f, y + 2f), size = Size(22f, 10f),
            style = Stroke(width = 2.6f, cap = StrokeCap.Round)
        )
        EmotionMouth.Wavy -> {
            val wave = Path().apply {
                moveTo(-12f, y)
                cubicTo(-6f, y - 5f, -3f, y + 5f, 0f, y)
                cubicTo(3f, y - 5f, 6f, y + 5f, 12f, y)
            }
            drawPath(wave, color = lip, style = Stroke(width = 2.6f, cap = StrokeCap.Round))
        }
        EmotionMouth.Smile, EmotionMouth.Auto -> {
            val s = smile.coerceIn(0f, 1f).coerceAtLeast(expr.mouthAmount)
            if (s > 0.2f) {
                drawArc(
                    color = lip, startAngle = 15f, sweepAngle = 150f, useCenter = false,
                    topLeft = Offset(-13f, y - 5f - s * 3f), size = Size(26f, 10f + s * 6f),
                    style = Stroke(width = 2.3f, cap = StrokeCap.Round)
                )
            } else {
                drawLine(lip, Offset(-10f, y), Offset(10f, y), strokeWidth = 2.3f, cap = StrokeCap.Round)
            }
        }
    }
}

/**
 * Full viseme set so the Free avatar shows the same phoneme detail (F/V lip-bite, TH tongue,
 * pursed round/kiss shapes) the engine already computes.
 */
private fun DrawScope.drawVisemeMouth(viseme: Viseme, smile: Float) {
    val y = AvatarLayout.mouthY
    val lip = LipColor

    when (viseme) {
        Viseme.Rest -> {
            val s = smile.coerceIn(0f, 1f)
            if (s > 0.2f) {
                drawArc(
                    color = lip,
                    startAngle = 15f,
                    sweepAngle = 150f,
                    useCenter = false,
                    topLeft = Offset(-13f, y - 5f - s * 3f),
                    size = Size(26f, 10f + s * 6f),
                    style = Stroke(width = 2.3f, cap = StrokeCap.Round)
                )
            } else {
                drawLine(lip, Offset(-10f, y), Offset(10f, y), strokeWidth = 2.3f, cap = StrokeCap.Round)
            }
        }
        Viseme.Closed -> drawRoundRect(
            color = lip,
            topLeft = Offset(-11f, y - 3f),
            size = Size(22f, 7f),
            cornerRadius = CornerRadius(3.5f)
        )
        Viseme.Wide -> {
            drawOval(color = lip, topLeft = Offset(-16f, y - 6f), size = Size(32f, 13f))
            drawRoundRect(
                color = TeethColor.copy(alpha = 0.9f),
                topLeft = Offset(-10f, y - 3f),
                size = Size(20f, 4f),
                cornerRadius = CornerRadius(2f)
            )
        }
        Viseme.Open -> {
            drawOval(color = lip, topLeft = Offset(-14f, y - 9f), size = Size(28f, 20f))
            drawOval(color = MouthCavity, topLeft = Offset(-9f, y - 3f), size = Size(18f, 11f))
            // Upper teeth tucked just under the top lip (not floating at the very top).
            drawRoundRect(
                color = TeethColor.copy(alpha = 0.85f),
                topLeft = Offset(-8f, y - 6f),
                size = Size(16f, 3.5f),
                cornerRadius = CornerRadius(1.5f)
            )
        }
        Viseme.Round -> {
            drawOval(color = lip, topLeft = Offset(-8f, y - 9f), size = Size(16f, 20f))
            drawOval(color = MouthCavity, topLeft = Offset(-4.5f, y - 3f), size = Size(9f, 10f))
        }
        Viseme.FV -> {
            // Upper teeth resting lightly on the lower lip.
            drawPath(
                Path().apply {
                    moveTo(-12f, y + 3f)
                    quadraticTo(0f, y - 6f, 12f, y + 3f)
                },
                color = FvLipShadow,
                style = Stroke(width = 2f, cap = StrokeCap.Round)
            )
            drawRoundRect(color = lip, topLeft = Offset(-9f, y), size = Size(18f, 4f), cornerRadius = CornerRadius(2f))
        }
        Viseme.Th -> {
            drawRoundRect(color = lip, topLeft = Offset(-8f, y - 1f), size = Size(16f, 7f), cornerRadius = CornerRadius(3.5f))
            drawRoundRect(
                color = TongueColor,
                topLeft = Offset(-3.5f, y - 7f),
                size = Size(7f, 7f),
                cornerRadius = CornerRadius(3f)
            )
        }
        Viseme.Smush -> drawOval(color = lip, topLeft = Offset(-13f, y - 4f), size = Size(26f, 11f))
        Viseme.Kiss -> drawOval(color = lip, topLeft = Offset(-7f, y - 4f), size = Size(14f, 11f))
    }
}

/**
 * How much the forearm bends at the elbow for each gesture. Negative = swings up/forward.
 *
 * Wave/Clap/Explain drive BOTH arms at once, and this bend stacks on top of the engine's
 * own shoulder lift (up to +30 degrees) — the original values (-60 to -70) compounded
 * enough to swing a hand across the body and land it on the chest instead of gesturing
 * outward. Trimmed down so the hands stay out at the sides.
 *
 * ThumbsUp is no longer planned by GesturePlanner (dropped — reads as offensive in some
 * cultures) but the enum case is kept alive here, grouped with Clap, so this stays
 * exhaustive without a dead special-cased pose.
 */
private fun elbowBendFor(gesture: HandGesture): Float = when (gesture) {
    HandGesture.None, HandGesture.Relaxed -> 8f
    HandGesture.PointDown -> 10f
    HandGesture.PointForward -> -22f
    HandGesture.PointLeft -> -18f
    HandGesture.Clap, HandGesture.ThumbsUp -> -30f
    HandGesture.Wave -> -42f
    HandGesture.Explain -> -20f
    HandGesture.OpenPalm -> 16f   // positive so, once mirrored, both palms open OUTWARD
    HandGesture.Think -> -48f
}

private fun DrawScope.drawArm(
    shoulderX: Float,
    angle: Float,
    gesture: HandGesture,
    isLeft: Boolean,
    outfit: OutfitStyle
) {
    val pivot = Offset(shoulderX, AvatarLayout.SHOULDER_Y)

    // At rest the arm relaxes outward along the body and bends a little at the elbow,
    // instead of hanging bolt-straight down the center — that's what read as "pasted on"
    // rather than a real limb. Active gestures use the engine's angle untouched so their
    // timing/lift isn't affected.
    val isResting = gesture == HandGesture.Relaxed || gesture == HandGesture.None
    val restSplay = if (isResting) 11f else 0f
    // Only a gentle elbow bend at rest so the forearm hangs down beside the body and the
    // hand rests low, instead of tucking up/across the torso (which read as stubby).
    val restBend = if (isResting) 6f else 0f
    // A resting arm ALWAYS hangs from the same clean pose — ignore any leftover angle the
    // controller left on the idle side of a one-handed gesture (that was swinging the
    // non-pointing arm out awkwardly).
    val poseAngle = if (isResting) 0f else angle
    val shoulderAngle = poseAngle - restSplay

    // Pointing is one-handed (always the left arm here) and should swing OUTWARD to the
    // side so it reads as a clear point, not fold inward toward rest like the mirrored
    // two-handed gestures do.
    val pointing = gesture == HandGesture.PointForward ||
        gesture == HandGesture.PointLeft ||
        gesture == HandGesture.PointDown
    val shoulderRot = when {
        pointing -> shoulderAngle          // swing the pointing arm out to its own side
        isLeft -> -shoulderAngle
        else -> shoulderAngle
    }
    rotate(shoulderRot, pivot = pivot) {
        val elbow = Offset(shoulderX, AvatarLayout.SHOULDER_Y + AvatarLayout.UPPER_ARM_LEN)
        val upperHalfTop = AvatarLayout.ARM_WIDTH / 2f
        val upperHalfBottom = AvatarLayout.ARM_WIDTH * 0.4f

        // Upper arm — tapers slightly toward the elbow instead of a uniform rectangle.
        val upperArmPath = Path().apply {
            moveTo(shoulderX - upperHalfTop, AvatarLayout.SHOULDER_Y)
            lineTo(shoulderX + upperHalfTop, AvatarLayout.SHOULDER_Y)
            lineTo(shoulderX + upperHalfBottom, elbow.y)
            lineTo(shoulderX - upperHalfBottom, elbow.y)
            close()
        }
        drawPath(
            upperArmPath,
            brush = Brush.verticalGradient(
                listOf(outfit.base, outfit.dark),
                startY = AvatarLayout.SHOULDER_Y,
                endY = elbow.y
            )
        )

        // Mirror the elbow bend for the left arm so two-handed gestures are symmetric —
        // both forearms angle the same way relative to the body (hands come together for a
        // clap, open outward for open-palms) instead of both swinging the same screen
        // direction, which read as uncoordinated.
        val bendDir = if (isLeft) -1f else 1f
        rotate((elbowBendFor(gesture) * ELBOW_BEND_DAMPING + restBend) * bendDir, pivot = elbow) {
            val forearmTopHalf = AvatarLayout.ARM_WIDTH * 0.38f
            val forearmBottomHalf = AvatarLayout.ARM_WIDTH * 0.27f
            val sleeveLen = 28f
            val wristLen = 10f
            val sleeveEndY = elbow.y + sleeveLen
            val handCenter = Offset(shoulderX, sleeveEndY + wristLen + AvatarLayout.HAND_RADIUS * 0.4f)

            // Elbow joint — a small darker bump so the two segments read as an actual
            // hinge rather than one continuous stick.
            drawCircle(
                color = outfit.dark.copy(alpha = 0.55f),
                radius = AvatarLayout.ARM_WIDTH * 0.42f,
                center = elbow
            )

            // Forearm — tapers further down toward the wrist.
            val forearmPath = Path().apply {
                moveTo(shoulderX - forearmTopHalf, elbow.y)
                lineTo(shoulderX + forearmTopHalf, elbow.y)
                lineTo(shoulderX + forearmBottomHalf, sleeveEndY)
                lineTo(shoulderX - forearmBottomHalf, sleeveEndY)
                close()
            }
            drawPath(
                forearmPath,
                brush = Brush.verticalGradient(
                    listOf(outfit.dark, outfit.base),
                    startY = elbow.y,
                    endY = sleeveEndY
                )
            )

            // Wrist bridges sleeve to hand.
            drawRoundRect(
                color = SkinMid,
                topLeft = Offset(shoulderX - 6f, sleeveEndY - 2f),
                size = Size(12f, wristLen),
                cornerRadius = CornerRadius(5f)
            )

            drawHand(handCenter, gesture, isLeft)
        }
    }
}

/**
 * Hands are drawn as smooth rounded "mitten" blobs with no individual finger segments.
 * At this render size, thin rotated finger rectangles read as jagged claw marks rather
 * than digits — a single soft shape reads far cleaner, and gestures are communicated by
 * the hand's overall silhouette/orientation plus the arm pose instead of finger detail.
 */
private fun DrawScope.drawHand(center: Offset, gesture: HandGesture, isLeft: Boolean) {
    val dir = if (isLeft) -1f else 1f
    val r = AvatarLayout.HAND_RADIUS

    // `center` is the wrist point where the forearm ends. The arm hangs down toward
    // increasing y, so the hand must sit BELOW it (larger y), not back up into it.
    val palmCenterY = center.y + r * 0.6f
    val palmCenter = Offset(center.x, palmCenterY)

    when (gesture) {
        HandGesture.PointDown, HandGesture.PointForward, HandGesture.PointLeft -> {
            // Elongated paddle instead of an extended finger — direction reads from the
            // stretched silhouette plus the arm/elbow angle, not a poking-out digit.
            // Biased further from the wrist so the elongation projects outward instead
            // of centering on the hand and doubling back up into the sleeve.
            val pointCenter = Offset(palmCenter.x, palmCenter.y + r * 0.55f)
            drawMittenBlob(pointCenter, r, widthScale = 0.58f, heightScale = 1.5f)
        }
        HandGesture.Wave, HandGesture.Clap, HandGesture.Explain, HandGesture.ThumbsUp,
        HandGesture.OpenPalm, HandGesture.Think ->
            drawMittenBlob(palmCenter, r, widthScale = 1.3f, heightScale = 1.1f)
        HandGesture.Relaxed, HandGesture.None ->
            drawMittenBlob(palmCenter, r, widthScale = 1.02f, heightScale = 1.02f)
    }

    // Thumb is the one digit worth keeping — a single smooth shape reads fine and is
    // what makes a relaxed hand recognizable at a glance.
    when (gesture) {
        HandGesture.Wave, HandGesture.Clap, HandGesture.Explain, HandGesture.ThumbsUp,
        HandGesture.OpenPalm, HandGesture.Think -> Unit
        else -> drawThumb(
            base = Offset(center.x + dir * r * 0.66f, palmCenterY - r * 0.05f),
            angleDeg = dir * 55f,
            r = r,
            prominent = false
        )
    }
}

private fun DrawScope.drawMittenBlob(center: Offset, r: Float, widthScale: Float, heightScale: Float) {
    val w = r * 2f * widthScale
    val h = r * 2f * heightScale
    val topLeft = Offset(center.x - w / 2f, center.y - h / 2f)
    val size = Size(w, h)
    val corner = minOf(w, h) * 0.5f
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(SkinLight, SkinMid),
            center = Offset(center.x - w * 0.12f, center.y - h * 0.22f),
            radius = maxOf(w, h) * 0.75f
        ),
        topLeft = topLeft,
        size = size,
        cornerRadius = CornerRadius(corner)
    )
    drawRoundRect(
        color = SkinShadow.copy(alpha = 0.28f),
        topLeft = topLeft,
        size = size,
        cornerRadius = CornerRadius(corner),
        style = Stroke(width = 1f)
    )
}

/** A single smooth, tapered thumb — the only individual digit kept, since one shape reads cleanly where five did not. */
private fun DrawScope.drawThumb(base: Offset, angleDeg: Float, r: Float, prominent: Boolean) {
    val length = if (prominent) r * 1f else r * 0.6f
    val width = r * (if (prominent) 0.44f else 0.36f)
    rotate(angleDeg, pivot = base) {
        val tip = base.y + length
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(SkinMid, SkinLight), startY = base.y, endY = tip),
            topLeft = Offset(base.x - width / 2f, base.y),
            size = Size(width, length),
            cornerRadius = CornerRadius(width / 2f)
        )
    }
}

private fun DrawScope.withUniformScale(scaleFactor: Float, pivot: Offset, block: DrawScope.() -> Unit) {
    withTransform({
        translate(pivot.x, pivot.y)
        scale(scaleFactor, scaleFactor, pivot = Offset.Zero)
        translate(-pivot.x, -pivot.y)
    }, block)
}
