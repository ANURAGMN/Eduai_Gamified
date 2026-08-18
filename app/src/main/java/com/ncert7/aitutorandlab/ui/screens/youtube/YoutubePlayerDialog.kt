package com.ncert7.aitutorandlab.ui.screens.youtube

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.anurag.eduai.uikit.screens.YoutubeVideoItem
import com.anurag.eduai.uikit.theme.EduAiTheme
import com.ncert7.aitutorandlab.utils.HomeCopy
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.launch

@Composable
fun YoutubePlayerDialog(
    videos: List<YoutubeVideoItem>,
    startIndex: Int,
    languageCode: String,
    onDismiss: () -> Unit,
) {
    if (videos.isEmpty()) {
        onDismiss()
        return
    }
    val safeStart = startIndex.coerceIn(0, videos.lastIndex)
    val pagerState = rememberPagerState(initialPage = safeStart, pageCount = { videos.size })
    val scope = rememberCoroutineScope()
    val colors = EduAiTheme.colors

    fun goRelative(delta: Int) {
        val target = (pagerState.settledPage + delta).coerceIn(0, videos.lastIndex)
        if (target != pagerState.settledPage) {
            scope.launch { pagerState.animateScrollToPage(target) }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
            ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(colors.surface1)
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterStart)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Close",
                            tint = colors.text,
                        )
                    }
                    val current = videos[pagerState.currentPage]
                    Text(
                        text = current.title,
                        color = colors.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier =
                            Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 48.dp, end = 16.dp),
                        maxLines = 1,
                    )
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    VerticalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 0,
                    ) { page ->
                        YoutubeFeedPage(
                            video = videos[page],
                            languageCode = languageCode,
                            isActive = page == pagerState.settledPage,
                            onPlaybackEnded = {
                                if (page < videos.lastIndex) {
                                    scope.launch { pagerState.animateScrollToPage(page + 1) }
                                }
                            },
                            onDismiss = onDismiss,
                        )
                    }

                    // YouTube's WebView eats most swipes — thin edge strips still flip the feed.
                    FeedSwipeEdge(
                        modifier =
                            Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .width(36.dp),
                        onSwipeUp = { goRelative(+1) },
                        onSwipeDown = { goRelative(-1) },
                        onSwipeLeft = { goRelative(+1) },
                        onSwipeRight = { goRelative(-1) },
                    )
                    FeedSwipeEdge(
                        modifier =
                            Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxHeight()
                                .width(28.dp),
                        onSwipeUp = { goRelative(+1) },
                        onSwipeDown = { goRelative(-1) },
                        onSwipeLeft = { goRelative(+1) },
                        onSwipeRight = { goRelative(-1) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedSwipeEdge(
    modifier: Modifier,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
) {
    var verticalAccum by remember { mutableFloatStateOf(0f) }
    var horizontalAccum by remember { mutableFloatStateOf(0f) }
    val threshold = 80f
    Box(
        modifier =
            modifier
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            when {
                                verticalAccum < -threshold -> onSwipeUp()
                                verticalAccum > threshold -> onSwipeDown()
                            }
                            verticalAccum = 0f
                        },
                        onVerticalDrag = { _, dragAmount -> verticalAccum += dragAmount },
                    )
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            when {
                                horizontalAccum < -threshold -> onSwipeLeft()
                                horizontalAccum > threshold -> onSwipeRight()
                            }
                            horizontalAccum = 0f
                        },
                        onHorizontalDrag = { _, dragAmount -> horizontalAccum += dragAmount },
                    )
                },
    )
}

@Composable
private fun YoutubeFeedPage(
    video: YoutubeVideoItem,
    languageCode: String,
    isActive: Boolean,
    onPlaybackEnded: () -> Unit,
    onDismiss: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var playbackFailed by remember(video.videoId) { mutableStateOf(false) }
    var youTubePlayer by remember(video.videoId) { mutableStateOf<YouTubePlayer?>(null) }
    val activeLatest by rememberUpdatedState(isActive)
    val onEndedLatest by rememberUpdatedState(onPlaybackEnded)

    val playerView =
        remember(video.videoId) {
            YouTubePlayerView(context).apply {
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                enableAutomaticInitialization = false
            }
        }

    DisposableEffect(video.videoId, lifecycleOwner) {
        playbackFailed = false
        lifecycleOwner.lifecycle.addObserver(playerView)
        val playerOptions =
            IFramePlayerOptions.Builder(context)
                .controls(1)
                .build()
        playerView.initialize(
            object : AbstractYouTubePlayerListener() {
                override fun onReady(player: YouTubePlayer) {
                    youTubePlayer = player
                    if (activeLatest) {
                        player.loadVideo(video.videoId, 0f)
                    } else {
                        player.cueVideo(video.videoId, 0f)
                    }
                }

                override fun onStateChange(
                    player: YouTubePlayer,
                    state: PlayerConstants.PlayerState,
                ) {
                    if (state == PlayerConstants.PlayerState.ENDED && activeLatest) {
                        onEndedLatest()
                    }
                }

                override fun onError(
                    player: YouTubePlayer,
                    error: PlayerConstants.PlayerError,
                ) {
                    playbackFailed = true
                }
            },
            playerOptions,
        )
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(playerView)
            playerView.release()
            youTubePlayer = null
        }
    }

    LaunchedEffect(isActive, youTubePlayer) {
        val player = youTubePlayer ?: return@LaunchedEffect
        if (isActive) {
            player.loadVideo(video.videoId, 0f)
        } else {
            player.pause()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { playerView },
            modifier = Modifier.fillMaxSize(),
        )
        if (playbackFailed) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.92f))
                        .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = HomeCopy.youtubePlaybackFailedMessage(languageCode),
                    color = Color.White,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                )
                Button(
                    onClick = {
                        openYoutubeVideo(context, video.videoId)
                        onDismiss()
                    },
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Text(HomeCopy.openInYoutubeLabel(languageCode))
                }
            }
        }
    }
}

private fun openYoutubeVideo(context: Context, videoId: String) {
    val uri = Uri.parse("https://www.youtube.com/watch?v=$videoId")
    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
}
