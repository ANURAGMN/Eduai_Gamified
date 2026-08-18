package com.ncert7.aitutorandlab.ui.screens.reels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.ncert7.aitutorandlab.domain.reels.ViewCountFormatter
import com.ncert7.aitutorandlab.domain.reels.analytics.ReelSection
import com.ncert7.aitutorandlab.domain.youtube.YoutubeVideo
import com.ncert7.aitutorandlab.service.analytics.ScreenName
import com.ncert7.aitutorandlab.service.analytics.TrackScreenEvent
import com.ncert7.aitutorandlab.utils.ReelsCopy
import com.ncert7.aitutorandlab.utils.getCurrentLanguageCode

/**
 * Reels explore — search bar + 3×4 grid (Newest / Most watched from [ReelsViewModel]). When a query
 * is present it shows ranked search results instead. Tapping a tile calls [onPlay]. Kids-safe by
 * construction (the view model filters to Made-for-kids). Reached from the Reels tab and the Home
 * "See all". Gate the entry points with `ReelsFeatureFlags.isReelsEnabled()`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsExploreScreen(
    onPlay: (YoutubeVideo) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: ReelsViewModel = hiltViewModel(),
) {
    val lang = getCurrentLanguageCode()
    val query by viewModel.query.collectAsState()
    val grid by viewModel.grid.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val searching = query.isNotBlank()

    // Time spent in the reels tab → app_analytics (Room) → GA4 → Firestore (when the mirror is on).
    TrackScreenEvent(ScreenName.REELS)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ReelsCopy.title(lang)) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searching) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                placeholder = { Text(ReelsCopy.searchHint(lang)) },
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.onSearchCommitted(results.size) }),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            )

            if (loading && grid.isEmpty) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (searching) {
                    if (results.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) { EmptyState(ReelsCopy.noMatches(lang)) }
                    } else {
                        itemsIndexed(results, key = { _, it -> it.videoId }) { index, video ->
                            ReelTile(video, ReelSection.SEARCH, index, viewModel, lang, onPlay)
                        }
                    }
                } else if (grid.isEmpty) {
                    item(span = { GridItemSpan(maxLineSpan) }) { EmptyState(ReelsCopy.emptyState(lang)) }
                } else {
                    if (grid.newest.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            SectionHeader(ReelsCopy.newest(lang), Icons.Default.Schedule)
                        }
                        itemsIndexed(grid.newest, key = { _, it -> "n_" + it.videoId }) { index, video ->
                            ReelTile(video, ReelSection.NEWEST, index, viewModel, lang, onPlay)
                        }
                    }
                    if (grid.mostWatched.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            SectionHeader(ReelsCopy.mostWatched(lang), Icons.Default.Whatshot)
                        }
                        itemsIndexed(grid.mostWatched, key = { _, it -> "m_" + it.videoId }) { index, video ->
                            ReelTile(video, ReelSection.MOST_WATCHED, index, viewModel, lang, onPlay)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun ReelTile(
    video: YoutubeVideo,
    section: ReelSection,
    position: Int,
    viewModel: ReelsViewModel,
    lang: String,
    onPlay: (YoutubeVideo) -> Unit,
) {
    val title = viewModel.localizedTitle(video, lang)
    Column(
        modifier = Modifier.clickable {
            viewModel.onReelOpened(video, section, position)
            onPlay(video)
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.62f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF222222)),
        ) {
            GlideImage(
                model = viewModel.thumbnailUrl(video),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.align(Alignment.Center).size(28.dp),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                Spacer(Modifier.width(3.dp))
                Text(ViewCountFormatter.format(video.viewCount), color = Color.White, fontSize = 10.sp)
            }
        }
        Text(
            text = title,
            fontSize = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
