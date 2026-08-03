package com.ncert7.aitutorandlab.ui.screens.youtube

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.anurag.eduai.uikit.theme.EduAiTheme
import com.ncert7.aitutorandlab.utils.HomeCopy
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

@Composable
fun YoutubePlayerDialog(
    videoId: String,
    title: String,
    languageCode: String,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
            ),
    ) {
        val colors = EduAiTheme.colors
        val lifecycleOwner = LocalLifecycleOwner.current
        val context = LocalContext.current
        var playbackFailed by remember(videoId) { mutableStateOf(false) }
        val playerView =
            remember {
                YouTubePlayerView(context).apply {
                    layoutParams =
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    enableAutomaticInitialization = false
                }
            }

        DisposableEffect(videoId, lifecycleOwner) {
            playbackFailed = false
            lifecycleOwner.lifecycle.addObserver(playerView)
            val playerOptions =
                IFramePlayerOptions.Builder(context)
                    .controls(1)
                    .build()
            playerView.initialize(
                object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        youTubePlayer.loadVideo(videoId, 0f)
                    }

                    override fun onError(
                        youTubePlayer: YouTubePlayer,
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
            }
        }

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
                    Text(
                        text = title,
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
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                ) {
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
                                    openYoutubeVideo(context, videoId)
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
        }
    }
}

private fun openYoutubeVideo(context: Context, videoId: String) {
    val uri = Uri.parse("https://www.youtube.com/watch?v=$videoId")
    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
}
