package com.anurag.eduai.uikit.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.ui.graphics.vector.ImageVector

enum class EduBottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Home("home", "Home", Icons.Outlined.Home),
    Plan("plan", "Plan", Icons.Outlined.Assignment),
    Quests("quests", "Quests", Icons.Outlined.TrackChanges),
    Leagues("leagues", "Leagues", Icons.Outlined.EmojiEvents),
    Avatar("avatar", "Avatar", Icons.Outlined.Face),
    Profile("profile", "Profile", Icons.Outlined.Person),
}
