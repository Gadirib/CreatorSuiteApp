package com.example.tiktokclone.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.tiktokclone.ui.screens.action.ActionScreen
import com.example.tiktokclone.ui.screens.cleaner.CleanerScreen
import com.example.tiktokclone.ui.screens.content.ContentScreen
import com.example.tiktokclone.ui.screens.cover.CoverScreen
import com.example.tiktokclone.ui.screens.create.CreateSheetScreen
import com.example.tiktokclone.ui.screens.create.ImportVideoSheetScreen
import com.example.tiktokclone.ui.screens.editor.VideoTrimScreen
import com.example.tiktokclone.ui.screens.login.LoginScreen
import com.example.tiktokclone.ui.screens.rec.RecScreen
import com.example.tiktokclone.ui.screens.saved.SavedScreen
import com.example.tiktokclone.ui.screens.settings.SettingsScreen


@Composable
fun AppRoot() {
    // Visual-only demo app: no backend and no auth.
    var screen by remember { mutableStateOf(AppScreen.Cover) }

    when (screen) {
        AppScreen.Cover -> CoverScreen(
            onTimeout = { screen = AppScreen.Action }
        )

        AppScreen.Action -> ActionScreen(
            onConnect = { screen = AppScreen.Content }
        )

        AppScreen.Login -> LoginScreen(
            onLoginSuccess = { screen = AppScreen.Content }
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
            onOpenCreate = { screen = AppScreen.CreateSheet },
            onOpenSettings = { screen = AppScreen.Settings }
        )

        AppScreen.Settings -> SettingsScreen(
            onClose = { screen = AppScreen.Content },
            onLoggedOut = {
                screen = AppScreen.Login
            }
        )
    }
}
