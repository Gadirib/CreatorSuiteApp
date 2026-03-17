package com.example.creatorsuiteapp.ui.screens.create

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.creatorsuiteapp.R
import com.example.creatorsuiteapp.data.media.MediaSelectionStore
import kotlinx.coroutines.launch

@Composable
fun ImportVideoSheetScreen(
    onClose: () -> Unit,
    onPick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isImporting by remember { mutableStateOf(false) }

    // Keeps URI access valid for the editor flow.
    fun handleUri(uri: android.net.Uri) {
        scope.launch {
            isImporting = true
            try {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: SecurityException) {  }

                MediaSelectionStore.setVideo(uri)
                onPick()
            } finally {
                isImporting = false
            }
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) handleUri(uri) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) handleUri(uri) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(enabled = !isImporting) { onClose() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .border(1.dp, Color(0xFF31354A), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .padding(18.dp)
                .clickable(enabled = false) {}
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(56.dp).height(5.dp)
                    .background(Color(0xFF5B5E73), RoundedCornerShape(99.dp))
            )

            Spacer(Modifier.height(14.dp))

            Text("Import Video", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)

            Spacer(Modifier.height(14.dp))

            ImportSourceRow(
                text = "Videos",
                iconRes = R.drawable.ic_files,
                description = "Camera roll & media library",
                enabled = !isImporting
            ) {
                videoPicker.launch("video/*")
            }

            Spacer(Modifier.height(10.dp))

            ImportSourceRow(
                text = "Files",
                iconRes = R.drawable.ic_files,
                description = "Browse video files",
                enabled = !isImporting
            ) {
                filePicker.launch(arrayOf("video/*"))
            }

            Spacer(Modifier.height(16.dp))
        }

        if (isImporting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFFFF2E63))
                    Spacer(Modifier.height(12.dp))
                    Text("Importing video…", color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun ImportSourceRow(
    text: String,
    iconRes: Int,
    description: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp)
            .background(
                if (enabled) Color(0xFF141722) else Color(0xFF0D1017),
                RoundedCornerShape(16.dp)
            )
            .border(1.dp, Color(0xFF2A2E3F), RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painterResource(iconRes), null, modifier = Modifier.size(30.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(text, color = if (enabled) Color.White else Color(0xFF6F738A), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = Color(0xFF6F738A), fontSize = 12.sp)
        }
    }
}
