package com.ncert7.aitutorandlab.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.anurag.eduai.uikit.screens.EduAvatarStudioScreen
import com.anurag.eduai.uikit.theme.EduAiTheme

/** Debug-only entry point so Avatar Studio can be opened via adb without signing in. */
class DebugAvatarStudioActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EduAiTheme {
                EduAvatarStudioScreen(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
