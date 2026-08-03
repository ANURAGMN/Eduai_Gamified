package com.ncert7.aitutorandlab.ui.screens.friends

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anurag.eduai.uikit.theme.EduAiTheme
import com.ncert7.aitutorandlab.service.analytics.ScreenName
import com.ncert7.aitutorandlab.service.analytics.TrackScreenEvent
import com.ncert7.aitutorandlab.ui.screens.friends.viewmodel.FriendsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(onNavigateBack: () -> Unit) {
    TrackScreenEvent(screenName = ScreenName.FRIENDS)

    val viewModel: FriendsViewModel = hiltViewModel()
    val myCode by viewModel.myFriendCode.collectAsState()
    val friendCount by viewModel.friendCount.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var friendCodeInput by remember { mutableStateOf("") }

    LaunchedEffect(statusMessage) {
        statusMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearStatusMessage()
        }
    }

    EduAiTheme {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Friends · $friendCount", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(),
                )
            },
        ) { padding ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(EduAiTheme.colors.surface1)
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Your friend code",
                    style = MaterialTheme.typography.titleMedium,
                    color = EduAiTheme.colors.text,
                )
                Text(
                    text = myCode.ifBlank { "Loading…" },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = EduAiTheme.colors.accent,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { copyFriendCode(context, myCode) },
                        enabled = myCode.isNotBlank(),
                    ) {
                        Text("Copy code")
                    }
                    OutlinedButton(
                        onClick = { shareFriendCode(context, myCode) },
                        enabled = myCode.isNotBlank(),
                    ) {
                        Text("Share")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Add a friend",
                    style = MaterialTheme.typography.titleMedium,
                    color = EduAiTheme.colors.text,
                )
                Text(
                    text = "Enter their 8-character code. Link instantly — earn 50 gems each when they finish their first lesson.",
                    style = MaterialTheme.typography.bodySmall,
                    color = EduAiTheme.colors.textSecondary,
                )
                OutlinedTextField(
                    value = friendCodeInput,
                    onValueChange = { friendCodeInput = it.uppercase().take(8) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Friend code") },
                    singleLine = true,
                )
                Button(
                    onClick = {
                        viewModel.addFriend(friendCodeInput)
                        friendCodeInput = ""
                    },
                    enabled = friendCodeInput.length >= 6,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Add friend")
                }
            }
        }
    }
}

private fun copyFriendCode(context: Context, code: String) {
    if (code.isBlank()) return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Friend code", code))
}

private fun shareFriendCode(context: Context, code: String) {
    if (code.isBlank()) return
    val intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "Add me on EduAI! My friend code is $code",
            )
        }
    context.startActivity(Intent.createChooser(intent, "Share friend code"))
}
