package com.anurag.eduai.uikit.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.anurag.eduai.uikit.components.NotificationDot
import com.anurag.eduai.uikit.theme.EduAiTheme

data class EduBottomNavBadges(
    val quests: Boolean = false,
    val leagues: Boolean = false,
    val profile: Boolean = false,
)

/** Prototype-style: icons only, no labels, hairline top border. */
@Composable
fun EduBottomNavBar(
    currentRoute: String,
    badges: EduBottomNavBadges,
    onItemSelected: (EduBottomNavItem) -> Unit,
    modifier: Modifier = Modifier,
    /** Reports each tab's on-screen bounds (root coords) so a coach-mark tour can spotlight them. */
    onItemBounds: (EduBottomNavItem, Rect) -> Unit = { _, _ -> },
) {
    val colors = EduAiTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(thickness = 0.5.dp, color = colors.border)
        NavigationBar(
            containerColor = colors.surface2,
            tonalElevation = 0.dp,
            contentColor = colors.textMuted,
        ) {
            EduBottomNavItem.entries.forEach { item ->
                val selected = currentRoute == item.route
                val showDot =
                    when (item) {
                        EduBottomNavItem.Quests -> badges.quests
                        EduBottomNavItem.Leagues -> badges.leagues
                        EduBottomNavItem.Profile -> badges.profile
                        else -> false
                    }
                NavigationBarItem(
                    selected = selected,
                    onClick = { onItemSelected(item) },
                    icon = {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 4.dp, horizontal = 10.dp)
                                .onGloballyPositioned { onItemBounds(item, it.boundsInRoot()) },
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = if (selected) colors.accent else colors.textMuted,
                            )
                            if (showDot) {
                                NotificationDot(
                                    modifier = Modifier.align(Alignment.TopEnd),
                                    borderColor = colors.surface2,
                                )
                            }
                        }
                    },
                    label = null,
                    alwaysShowLabel = false,
                    colors =
                        NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent,
                            selectedIconColor = colors.accent,
                            unselectedIconColor = colors.textMuted,
                        ),
                )
            }
        }
    }
}
