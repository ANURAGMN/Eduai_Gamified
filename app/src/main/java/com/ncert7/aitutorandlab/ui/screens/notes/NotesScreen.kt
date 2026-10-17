package com.ncert7.aitutorandlab.ui.screens.notes

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.anurag.eduai.uikit.theme.EduAiTheme
import com.ncert7.aitutorandlab.R
import com.ncert7.aitutorandlab.data.local.entities.ChapterNoteEntity
import com.ncert7.aitutorandlab.ui.theme.BrandPrimary
import com.ncert7.aitutorandlab.ui.theme.ColorError
import com.ncert7.aitutorandlab.ui.theme.TextOnPrimary
import java.io.File
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    subjectName: String,
    onBackClick: () -> Unit,
    viewModel: NotesViewModel = hiltViewModel()
) {
    val notesList by viewModel.notes.collectAsState()
    val colors = EduAiTheme.colors
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(viewModel.screenTitle, fontWeight = FontWeight.SemiBold, color = TextOnPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextOnPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandPrimary)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = colors.surface1
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(notesList, key = { it.chapterId }) { item ->
                NoteRow(
                    item = item,
                    viewModel = viewModel,
                    onNoPdf = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                )
            }
        }
    }
}

@Composable
fun NoteRow(
    item: ChapterNoteEntity,
    viewModel: NotesViewModel,
    onNoPdf: (String) -> Unit
) {
    val colors = EduAiTheme.colors
    val context = LocalContext.current
    val isKannada = viewModel.isKannada

    val hasLanguagePdf = if (isKannada) {
        !item.remotePdfUrlKn.isNullOrBlank()
    } else {
        !item.remotePdfUrl.isNullOrBlank()
    }

    val progress = if (isKannada && !hasLanguagePdf) 0 else item.downloadProgress
    val isDownloaded = if (isKannada && !hasLanguagePdf) false else item.isDownloaded

    val onCardClick = {
        if (!hasLanguagePdf) {
            val msg = if (isKannada) "Kannada PDF not available" else "PDF link missing in database"
            onNoPdf(msg)
        } else if (isDownloaded && !item.localFilePath.isNullOrBlank()) {
            val file = File(item.localFilePath)
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Open PDF"))
            } else {
                viewModel.startDownload(item)
            }
        } else {
            viewModel.startDownload(item)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface2)
            .clickable { onCardClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surface1),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Ch", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Text("${item.chapterNumber}", color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = item.conceptName.orEmpty().ifBlank { "Chapter ${item.chapterNumber}" },
                color = colors.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )

            if (hasLanguagePdf) {
                if (isDownloaded) {
                    Text(
                        "Saved · tap to open",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                } else if (progress in 1..99) {
                    Text(
                        "Tap to restart if stuck",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        when {
            isDownloaded -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(colors.successBg)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = colors.success, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.state_downloaded), color = colors.success, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = { viewModel.deleteDownload(item) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = ColorError, modifier = Modifier.size(20.dp))
                    }
                }
            }
            progress in 1..99 -> {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.accent.copy(alpha = 0.1f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(progress = progress / 100f, modifier = Modifier.size(16.dp), color = colors.accent, strokeWidth = 2.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("$progress%", color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            else -> {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, colors.accent, RoundedCornerShape(24.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.FileDownload, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.state_download), color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}