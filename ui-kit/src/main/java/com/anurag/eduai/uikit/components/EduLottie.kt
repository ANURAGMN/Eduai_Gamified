package com.anurag.eduai.uikit.components

import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

/**
 * Renders a Lottie animation from `res/raw`, but degrades gracefully: while the
 * JSON is loading — or if it fails to parse — the [fallback] content is shown
 * instead. This keeps the build green and the UI safe even when the bundled
 * placeholder JSONs are swapped for (or missing) real LottieFiles assets.
 *
 * To upgrade the visuals, replace the files in `ui-kit/src/main/res/raw/`
 * (e.g. `eduai_flame.json`, `eduai_success.json`) with polished animations from
 * lottiefiles.com — no code change needed.
 */
@Composable
fun EduLottie(
    @RawRes resId: Int,
    modifier: Modifier = Modifier,
    iterations: Int = LottieConstants.IterateForever,
    fallback: @Composable () -> Unit = {},
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(resId))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = iterations,
    )
    if (composition == null) {
        fallback()
    } else {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = modifier,
        )
    }
}
