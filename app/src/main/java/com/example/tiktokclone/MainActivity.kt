package com.example.tiktokclone

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.tiktokclone.data.tiktok.TikTokLoginActivity
import com.example.tiktokclone.navigation.AppRoot
import com.example.tiktokclone.ui.theme.TikTokCloneTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = Intent(this, TikTokLoginActivity::class.java)
        startActivity(intent)

        enableEdgeToEdge()

        setContent {
            TikTokCloneTheme {
                AppRoot()
            }
        }
    }
}
