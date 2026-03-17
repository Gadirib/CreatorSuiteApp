package com.example.creatorsuiteapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.creatorsuiteapp.navigation.AppRoot
import com.example.creatorsuiteapp.ui.theme.TikTokCloneTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TikTokCloneTheme {
                AppRoot()
            }
        }
    }
}
