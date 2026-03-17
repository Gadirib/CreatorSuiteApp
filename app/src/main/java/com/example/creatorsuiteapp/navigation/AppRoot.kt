package com.example.creatorsuiteapp.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.creatorsuiteapp.data.tiktok.TikTokSessionManager
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

    // Keeps app navigation in local screen state.
    var screen by remember {
        mutableStateOf(
            if (TikTokSessionManager.isLoggedIn(context)) {
                AppScreen.Content
            } else {
                AppScreen.Cover
            }
        )
    }

    when (screen) {

        AppScreen.Login -> LoginScreen(
            onLoginSuccess = {
                screen = AppScreen.Content
            }
        )

        AppScreen.Cover -> CoverScreen(
            onFinished = {
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
            onClose = { screen = AppScreen.Content },
            onSaved = { screen = AppScreen.Content }
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
                screen = AppScreen.Login
            }
        )
    }
}
