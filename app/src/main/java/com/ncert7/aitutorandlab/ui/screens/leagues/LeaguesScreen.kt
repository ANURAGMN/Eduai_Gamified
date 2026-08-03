package com.ncert7.aitutorandlab.ui.screens.leagues

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.anurag.eduai.uikit.screens.EduLeaguesScreen
import com.anurag.eduai.uikit.theme.EduAiTheme
import com.ncert7.aitutorandlab.service.analytics.ScreenName
import com.ncert7.aitutorandlab.service.analytics.TrackScreenEvent
import com.ncert7.aitutorandlab.ui.screens.leagues.viewmodel.LeaguesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaguesScreen(
    onNavigateBack: () -> Unit = {},
    showBackNavigation: Boolean = true,
) {
    TrackScreenEvent(screenName = ScreenName.LEAGUES)

    val viewModel: LeaguesViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    EduAiTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Leagues", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        if (showBackNavigation) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(),
                )
            },
        ) { padding ->
            if (isLoading && uiState.participants.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                EduLeaguesScreen(
                    state = uiState,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}
