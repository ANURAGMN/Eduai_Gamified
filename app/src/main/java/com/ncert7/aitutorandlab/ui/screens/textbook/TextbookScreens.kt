package com.ncert7.aitutorandlab.ui.screens.textbook

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ncert7.aitutorandlab.ui.screens.conceptscreen.components.SimulationHeader
import com.ncert7.aitutorandlab.utils.HomeCopy
import com.ncert7.aitutorandlab.utils.getCurrentLanguageCode
import com.ncert7.aitutorandlab.utils.isKannada

/** An NCERT class-7 textbook the student can open in the in-app browser. */
data class NcertBook(val title: String, val subtitle: String, val url: String)

/**
 * The NCERT textbooks to show for the current app language. English has Science plus Maths
 * Part 1 & Part 2; Kannada has Science plus Maths Part 1. URLs are the official ncert.nic.in
 * textbook pages supplied for the class-7 syllabus.
 */
fun ncertTextbooks(kannada: Boolean): List<NcertBook> {
    val lang = if (kannada) "kn" else "en"
    val subtitle = HomeCopy.textbookClassSubtitle(lang)
    return if (kannada) {
        listOf(
            NcertBook(HomeCopy.textbookScienceTitle(lang), subtitle, "https://ncert.nic.in/textbook.php?gkncu1=0-12"),
            NcertBook(HomeCopy.textbookMathPart1Title(lang), subtitle, "https://ncert.nic.in/textbook.php?gkngp1=0-8"),
        )
    } else {
        listOf(
            NcertBook(HomeCopy.textbookScienceTitle(lang), subtitle, "https://ncert.nic.in/textbook.php?gecu1=0-12"),
            NcertBook(HomeCopy.textbookMathPart1Title(lang), subtitle, "https://ncert.nic.in/textbook.php?gegp1=0-8"),
            NcertBook(HomeCopy.textbookMathPart2Title(lang), subtitle, "https://ncert.nic.in/textbook.php?gegp2=0-7"),
        )
    }
}

/** Home-screen entry card that opens the textbooks chooser. */
@Composable
fun TextbookEntryCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val lang = getCurrentLanguageCode()
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = HomeCopy.textbooksSectionTitle(lang),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = HomeCopy.textbooksSectionSubtitle(lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Chooser screen: lists the textbooks for the current app language. */
@Composable
fun TextbooksScreen(
    onBack: () -> Unit,
    onOpenBook: (NcertBook) -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            SimulationHeader(
                title = HomeCopy.textbooksSectionTitle(getCurrentLanguageCode()),
                onBackClick = onBack,
                showVoiceToggle = false,
            )
            Column(modifier = Modifier.padding(16.dp)) {
                ncertTextbooks(isKannada()).forEach { book ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenBook(book) },
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(Modifier.size(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = book.title,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = book.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

/** Opens a single NCERT textbook URL in an in-app WebView. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TextbookWebScreen(
    url: String,
    title: String,
    onBack: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            SimulationHeader(
                title = title,
                onBackClick = onBack,
                showVoiceToggle = false,
            )
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = WebViewClient()
                        loadUrl(url)
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
