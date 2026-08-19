package com.ncert7.aitutorandlab.ui.screens.reels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncert7.aitutorandlab.domain.reels.ReelsGrid
import com.ncert7.aitutorandlab.domain.reels.ReelsGridSelector
import com.ncert7.aitutorandlab.domain.reels.ReelsSearch
import com.ncert7.aitutorandlab.domain.reels.analytics.ReelSection
import com.ncert7.aitutorandlab.domain.youtube.YoutubeVideo
import com.ncert7.aitutorandlab.repository.YoutubeVideoRepository
import com.ncert7.aitutorandlab.service.analytics.ReelsAnalyticsTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Orchestrates the reels explore screen from [YoutubeVideoRepository] using the pure, unit-tested
 * [ReelsGridSelector] and [ReelsSearch].
 *
 * Made-for-kids gating is on by default ([ReelsGridSelector.REQUIRE_MADE_FOR_KIDS_DEFAULT]) so only
 * kids-safe videos surface in the explore grid and player.
 */
@HiltViewModel
class ReelsViewModel @Inject constructor(
    private val repository: YoutubeVideoRepository,
) : ViewModel() {

    private val _videos = MutableStateFlow<List<YoutubeVideo>>(emptyList())
    private val _query = MutableStateFlow("")
    private val _loading = MutableStateFlow(true)

    val query: StateFlow<String> = _query.asStateFlow()
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val catalog: StateFlow<List<YoutubeVideo>> =
        _videos
            .map { list -> list.filter { it.videoId.isNotBlank() } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val grid: StateFlow<ReelsGrid> =
        catalog
            .map { ReelsGridSelector.select(it) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, ReelsGrid(emptyList(), emptyList()))

    val searchResults: StateFlow<List<YoutubeVideo>> =
        combine(catalog, _query) { list, q -> ReelsSearch.filter(list, q) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            _videos.value = repository.fetchVideos()
            _loading.value = false
        }
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    /** A tile was tapped — record which video, from which section, at what 0-based [position]. */
    fun onReelOpened(video: YoutubeVideo, section: ReelSection, position: Int) {
        ReelsAnalyticsTracker.trackOpen(
            videoId = video.videoId,
            section = section,
            position = position,
            query = _query.value.takeIf { section == ReelSection.SEARCH },
        )
    }

    /** A search was committed from the keyboard (IME action), yielding [resultCount] results. */
    fun onSearchCommitted(resultCount: Int) {
        ReelsAnalyticsTracker.trackSearch(_query.value, resultCount)
    }

    fun localizedTitle(video: YoutubeVideo, languageCode: String): String =
        repository.localizedTitle(video, languageCode)

    fun thumbnailUrl(video: YoutubeVideo): String =
        repository.thumbnailUrl(video.videoId)
}
