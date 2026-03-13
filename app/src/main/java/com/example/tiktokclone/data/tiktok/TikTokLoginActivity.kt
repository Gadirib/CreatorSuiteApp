package com.example.tiktokclone.data.tiktok

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.tiktokclone.ui.screens.login.LoginScreen
import com.example.tiktokclone.ui.theme.TikTokCloneTheme

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
