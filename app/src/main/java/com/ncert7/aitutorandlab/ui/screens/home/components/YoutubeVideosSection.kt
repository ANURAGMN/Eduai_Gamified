package com.ncert7.aitutorandlab.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anurag.eduai.uikit.components.HorizontalRail
import com.anurag.eduai.uikit.components.RailCard
import com.anurag.eduai.uikit.components.pressScaleClickable
import com.anurag.eduai.uikit.screens.YoutubeVideoItem
import com.anurag.eduai.uikit.theme.EduAiDimens
import com.anurag.eduai.uikit.theme.EduAiTheme
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun YoutubeVideosSection(
    title: String,
    videos: List<YoutubeVideoItem>,
    onVideoClick: (YoutubeVideoItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (videos.isEmpty()) return
    val colors = EduAiTheme.colors
    Column(modifier = modifier.fillMaxWidth().padding(top = 18.dp)) {
        Text(
            text = title,
            color = colors.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        HorizontalRail {
            videos.forEach { video ->
                RailCard(
                    onClick = { onVideoClick(video) },
                    modifier = Modifier.width(168.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(94.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.surface2),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (video.thumbnailUrl.isNotBlank()) {
                            GlideImage(
                                model = video.thumbnailUrl,
                                contentDescription = video.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                        Box(
                            modifier =
                                Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xCCFF0000)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                    Text(
                        text = video.title,
                        color = colors.text,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}
