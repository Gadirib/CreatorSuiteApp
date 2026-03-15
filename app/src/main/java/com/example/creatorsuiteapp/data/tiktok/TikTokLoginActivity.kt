package com.example.creatorsuiteapp.data.tiktok

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.creatorsuiteapp.ui.screens.login.LoginScreen
import com.example.creatorsuiteapp.ui.theme.TikTokCloneTheme

class TikTokLoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TikTokCloneTheme {
                LoginScreen(
                    onLoginSuccess = { finish() }
                )
            }
        }
    }
}
