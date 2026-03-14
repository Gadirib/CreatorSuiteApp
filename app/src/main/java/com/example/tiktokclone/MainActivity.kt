package com.example.tiktokclone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.tiktokclone.navigation.AppRoot
import com.example.tiktokclone.ui.theme.TikTokCloneTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TikTokCloneTheme {
                // AppRoot handles navigation — it checks login state internally
                // and shows LoginScreen or main app depending on session
                AppRoot()
            }
        }
    }
}
