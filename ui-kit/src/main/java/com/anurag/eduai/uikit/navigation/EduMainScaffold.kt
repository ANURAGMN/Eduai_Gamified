package com.anurag.eduai.uikit.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun EduMainScaffold(
    currentRoute: String,
    badges: EduBottomNavBadges,
    onItemSelected: (EduBottomNavItem) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            EduBottomNavBar(
                currentRoute = currentRoute,
                badges = badges,
                onItemSelected = onItemSelected,
            )
        },
        content = { innerPadding ->
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
            ) {
                content(innerPadding)
            }
        },
    )
}
