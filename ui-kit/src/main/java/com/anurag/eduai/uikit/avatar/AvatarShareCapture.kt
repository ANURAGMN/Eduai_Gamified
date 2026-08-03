package com.anurag.eduai.uikit.avatar

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.view.drawToBitmap
import com.anurag.eduai.uikit.avatar.core.AvatarState
import com.anurag.eduai.uikit.theme.EduAiTheme
import com.anurag.eduai.uikit.theme.EduThemeMode

private const val SHARE_SIZE_PX = 720
private const val NOTIFICATION_ICON_PX = 256

/** How much of the tutor to include in a captured bitmap. */
enum class AvatarCaptureFraming {
    /** Full tutor with share-card padding — used for social share exports. */
    FullBody,
    /** Face-forward crop — matches small agent avatar circles (~120dp). */
    FaceCloseUp,
}

private data class AvatarFaceCrop(val zoom: Float, val offsetYFraction: Float)

private val NotificationFaceCrop = AvatarFaceCrop(zoom = 2.12f, offsetYFraction = 0.20f)

/**
 * Renders the tutor into an off-screen [ComposeView], waits one frame for layout,
 * then returns a PNG-ready bitmap. Must be invoked on the main thread.
 */
/**
 * Renders the saved tutor look at notification large-icon size. Requires an [Activity] context.
 */
fun captureTutorNotificationIcon(
    context: Context,
    config: TutorConfig,
    onReady: (Bitmap) -> Unit,
    onError: () -> Unit = {},
) {
    captureTutorShareBitmap(
        context = context,
        config = config,
        framing = AvatarCaptureFraming.FaceCloseUp,
        onReady = { bitmap ->
            onReady(
                Bitmap.createScaledBitmap(bitmap, NOTIFICATION_ICON_PX, NOTIFICATION_ICON_PX, true).also {
                    if (it !== bitmap) bitmap.recycle()
                },
            )
        },
        onError = onError,
    )
}

internal fun captureTutorShareBitmap(
    context: Context,
    config: TutorConfig,
    state: AvatarState = AvatarState.Happy,
    framing: AvatarCaptureFraming = AvatarCaptureFraming.FullBody,
    onReady: (Bitmap) -> Unit,
    onError: () -> Unit = {},
) {
    val activity = context.findActivity()
    if (activity == null) {
        onError()
        return
    }

    val composeView =
        ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        }

    val container =
        FrameLayout(activity).apply {
            visibility = android.view.View.INVISIBLE
            layoutParams = ViewGroup.LayoutParams(SHARE_SIZE_PX, SHARE_SIZE_PX)
        }
    container.addView(
        composeView,
        FrameLayout.LayoutParams(SHARE_SIZE_PX, SHARE_SIZE_PX),
    )

    val decor = activity.window.decorView as ViewGroup
    decor.addView(container)

    container.post {
        if (!container.isAttachedToWindow) {
            decor.removeView(container)
            onError()
            return@post
        }
        composeView.setContent {
            EduAiTheme(themeMode = EduThemeMode.Light) {
                ShareAvatarCard(config = config, state = state, framing = framing)
            }
        }
        composeView.post {
            if (!composeView.isAttachedToWindow) {
                decor.removeView(container)
                onError()
                return@post
            }
            composeView.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(SHARE_SIZE_PX, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(SHARE_SIZE_PX, android.view.View.MeasureSpec.EXACTLY),
            )
            composeView.layout(0, 0, SHARE_SIZE_PX, SHARE_SIZE_PX)
            composeView.post {
                try {
                    onReady(composeView.drawToBitmap(Bitmap.Config.ARGB_8888))
                } catch (_: Exception) {
                    onError()
                } finally {
                    decor.removeView(container)
                }
            }
        }
    }
}

@Composable
private fun ShareAvatarCard(
    config: TutorConfig,
    state: AvatarState,
    framing: AvatarCaptureFraming = AvatarCaptureFraming.FullBody,
) {
    val faceCrop =
        when (framing) {
            AvatarCaptureFraming.FullBody -> null
            AvatarCaptureFraming.FaceCloseUp -> NotificationFaceCrop
        }
    val avatarPadding = if (framing == AvatarCaptureFraming.FullBody) 48.dp else 8.dp

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFFF0F4F8)),
        contentAlignment = Alignment.Center,
    ) {
        EduTutorAvatar(
            character = config.character,
            state = state,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(avatarPadding)
                    .let { modifier ->
                        faceCrop?.let {
                            modifier.avatarFaceZoom(it.zoom, it.offsetYFraction)
                        } ?: modifier
                    },
            outfitVariant = config.outfit,
            hairStyle = config.hair,
            hairColor = config.hairColor,
            glassesStyle = config.glasses,
            glassesColor = config.frameColor,
            neckStyle = config.neck,
            underEyeLine = config.eyeLine,
            cheekShading = config.cheeks,
        )
    }
}

internal fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
