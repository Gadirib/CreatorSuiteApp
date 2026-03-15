package com.example.creatorsuiteapp.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.creatorsuiteapp.data.tiktok.TikTokSessionManager   // ← make sure this import exists
import com.example.creatorsuiteapp.ui.screens.action.ActionScreen
import com.example.creatorsuiteapp.ui.screens.cleaner.CleanerScreen
import com.example.creatorsuiteapp.ui.screens.content.ContentScreen
import com.example.creatorsuiteapp.ui.screens.cover.CoverScreen
import com.example.creatorsuiteapp.ui.screens.create.CreateSheetScreen
import com.example.creatorsuiteapp.ui.screens.create.ImportVideoSheetScreen
import com.example.creatorsuiteapp.ui.screens.editor.VideoTrimScreen
import com.example.creatorsuiteapp.ui.screens.login.LoginScreen
import com.example.creatorsuiteapp.ui.screens.rec.RecScreen
import com.example.creatorsuiteapp.ui.screens.saved.SavedScreen
import com.example.creatorsuiteapp.ui.screens.settings.SettingsScreen

@Composable
fun AppRoot() {
    val context = LocalContext.current

    // This is the key part: decide starting screen based on auth status
    var screen by remember {
        mutableStateOf(
            if (TikTokSessionManager.isLoggedIn(context)) {
                AppScreen.Content   // already authorized → go to main screen
            } else {
                AppScreen.Login     // not authorized → force login first
            }
        )
    }

    // Optional: you can keep a short splash/cover if you want,
    // but document wants login to be the entry point when not authorized
    when (screen) {

        AppScreen.Login -> LoginScreen(
            onLoginSuccess = {
                screen = AppScreen.Content   // after login → main screen
            }
        )

        AppScreen.Cover -> CoverScreen(
            onTimeout = {
                // If you still want Cover as splash → check auth again after timeout
                screen = if (TikTokSessionManager.isLoggedIn(context)) {
                    AppScreen.Content
                } else {
                    AppScreen.Login
                }
            }
        )

        AppScreen.Action -> ActionScreen(
            onConnect = { screen = AppScreen.Content }
        )

        AppScreen.Content -> ContentScreen(
            onOpenCreate = { screen = AppScreen.CreateSheet },
            onOpenCleaner = { screen = AppScreen.Cleaner },
            onOpenSettings = { screen = AppScreen.Settings }
        )

        AppScreen.Saved -> SavedScreen(
            onClose = { screen = AppScreen.Content }
        )

        AppScreen.CreateSheet -> CreateSheetScreen(
            onClose = { screen = AppScreen.Content },
            onRecord = { screen = AppScreen.Rec },
            onImport = { screen = AppScreen.ImportSheet }
        )

        AppScreen.ImportSheet -> ImportVideoSheetScreen(
            onClose = { screen = AppScreen.Content },
            onPick = { screen = AppScreen.VideoTrim }
        )

        AppScreen.VideoTrim -> VideoTrimScreen(
            onClose = { screen = AppScreen.Content }
        )

        AppScreen.Rec -> RecScreen(
            onClose = { screen = AppScreen.Content }
        )

        AppScreen.Cleaner -> CleanerScreen(
            onBackToContent = { screen = AppScreen.Content },
            onOpenCreate    = { screen = AppScreen.CreateSheet },
            onOpenSettings  = { screen = AppScreen.Settings }
        )

        AppScreen.Settings -> SettingsScreen(
            onClose = { screen = AppScreen.Content },
            onLoggedOut = {
                // Important: after logout → force login again
                screen = AppScreen.Login
            }
        )
    }
}