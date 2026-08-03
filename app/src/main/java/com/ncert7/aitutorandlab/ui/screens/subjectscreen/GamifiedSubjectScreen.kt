package com.ncert7.aitutorandlab.ui.screens.subjectscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import com.anurag.eduai.uikit.components.subjectMaterialIcon
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anurag.eduai.uikit.components.EduScreenTopBar
import com.anurag.eduai.uikit.components.SubjectTile
import com.anurag.eduai.uikit.components.pressScaleClickable
import com.anurag.eduai.uikit.theme.EduAiTheme
import com.anurag.eduai.uikit.theme.EduChipRole
import com.anurag.eduai.uikit.theme.forRole
import com.ncert7.aitutorandlab.service.analytics.ScreenName
import com.ncert7.aitutorandlab.service.analytics.TrackScreenEvent
import com.ncert7.aitutorandlab.ui.screens.subjectscreen.viewmodel.SubjectViewModel
import com.ncert7.aitutorandlab.utils.SubjectIds
import com.ncert7.aitutorandlab.utils.SubjectIconUrls
import com.ncert7.aitutorandlab.utils.getCurrentLanguageCode
import com.ncert7.aitutorandlab.utils.isKannadaLanguage
import com.ncert7.aitutorandlab.utils.resolveSubjectIconUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamifiedSubjectScreen(
    onNavigateBack: () -> Unit,
    onSubjectClick: (String) -> Unit,
    viewModel: SubjectViewModel = hiltViewModel(),
) {
    TrackScreenEvent(screenName = ScreenName.SUBJECT)
    val state by viewModel.state.collectAsState()
    val colors = EduAiTheme.colors
    val languageCode = getCurrentLanguageCode()

    val tiles =
        if (state.subjects.isNotEmpty()) {
            state.subjects.map { subject ->
                SubjectTile(
                    name = subject.name,
                    role = chipRoleForName(subject.name),
                    subjectId = subject.id,
                    iconUrl = resolveSubjectIconUrl(subject.id, subject.name, subject.iconUrl),
                )
            }
        } else if (!state.isLoading) {
            defaultSubjectTiles(languageCode)
        } else {
            emptyList()
        }

    EduAiTheme {
        Scaffold(
            topBar = {
                EduScreenTopBar(
                    title = "Choose subject",
                    showBack = true,
                    onBack = onNavigateBack,
                )
            },
        ) { padding ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(colors.surface1)
                        .padding(padding),
            ) {
                when {
                    state.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    state.error != null -> {
                        Text(
                            text = "Unable to load subjects. Try again.",
                            color = colors.textSecondary,
                            modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        )
                    }
                    else -> {
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            Text(
                                text = "Class ${state.classLevel} · NCERT",
                                color = colors.textMuted,
                                fontSize = 12.sp,
                            )
                            Text(
                                text = "Pick a subject to explore chapters",
                                color = colors.text,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                            )
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(tiles, key = { it.subjectId.ifBlank { it.name } }) { tile ->
                                    GamifiedSubjectTile(
                                        tile = tile,
                                        onClick = {
                                            if (tile.subjectId.isNotBlank()) {
                                                viewModel.onSubjectSelected(tile.subjectId)
                                                onSubjectClick(tile.subjectId)
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun GamifiedSubjectTile(
    tile: SubjectTile,
    onClick: () -> Unit,
) {
    val colors = EduAiTheme.colors
    val (fg, bg) = colors.forRole(tile.role)

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .pressScaleClickable(onClick = onClick, pressedScale = 0.96f),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg),
            contentAlignment = Alignment.Center,
        ) {
            if (!tile.iconUrl.isNullOrEmpty()) {
                GlideImage(
                    model = tile.iconUrl,
                    contentDescription = tile.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(48.dp),
                )
            } else {
                Icon(
                    imageVector = subjectMaterialIcon(tile.name),
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = tile.name,
            color = colors.text,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
        )
    }
}

private fun chipRoleForName(name: String): EduChipRole =
    when {
        name.contains("math", ignoreCase = true) ||
            name.contains("ಗಣಿತ", ignoreCase = false) -> EduChipRole.Pro
        name.contains("science", ignoreCase = true) ||
            name.contains("ವಿಜ್ಞಾನ", ignoreCase = false) -> EduChipRole.Accent
        else -> EduChipRole.Pro
    }

private fun defaultSubjectTiles(languageCode: String): List<SubjectTile> {
    val kannada = isKannadaLanguage(languageCode)
    return listOf(
        SubjectTile(
            name = if (kannada) "ಗಣಿತ" else "Math",
            role = EduChipRole.Pro,
            subjectId = SubjectIds.MATH,
            iconUrl = SubjectIconUrls.MATH,
        ),
        SubjectTile(
            name = if (kannada) "ವಿಜ್ಞಾನ" else "Science",
            role = EduChipRole.Accent,
            subjectId = SubjectIds.SCIENCE,
            iconUrl = SubjectIconUrls.SCIENCE,
        ),
    )
}
