package com.example.creatorsuiteapp.ui.screens.rec

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.creatorsuiteapp.ui.viewmodel.PublishStage
import com.example.creatorsuiteapp.ui.viewmodel.PublishUiState

@Composable
fun PublishSheet(
    onDismiss: () -> Unit,
    onPublish: (caption: String, privacy: String) -> Unit,
    isBusy: Boolean
) {
    var caption by remember { mutableStateOf("") }
    val privacyOptions = listOf("Public", "Friends", "Private")
    var selectedPrivacy by remember { mutableStateOf("Public") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(enabled = !isBusy) { onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black, RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .border(1.dp, Color(0xFF2E3243), RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .padding(horizontal = 18.dp, vertical = 14.dp)
                .clickable(enabled = false) {}
        ) {
            Box(
                modifier = Modifier.align(Alignment.CenterHorizontally)
                    .width(56.dp).height(5.dp)
                    .background(Color(0xFF646982), RoundedCornerShape(99.dp))
            )

            Spacer(Modifier.height(14.dp))

            Text("Post to TikTok", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)

            Spacer(Modifier.height(12.dp))

            Text("Caption", color = Color(0xFF8A8FA6), fontSize = 12.sp)
            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                placeholder = { Text("Add a caption…", color = Color(0xFF4A4E68)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2D3348),
                    unfocusedBorderColor = Color(0xFF2D3348),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFFFF2E63)
                )
            )

            Spacer(Modifier.height(12.dp))

            Text("Privacy", color = Color(0xFF8A8FA6), fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                privacyOptions.forEach { option ->
                    val selected = selectedPrivacy == option
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .background(
                                if (selected) Color(0xFFFF2E63) else Color(0xFF141722),
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.dp,
                                if (selected) Color(0xFFFF2E63) else Color(0xFF2D3348),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable(enabled = !isBusy) { selectedPrivacy = option }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(option, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier.weight(1f).height(50.dp)
                        .background(Color(0xFF141722), RoundedCornerShape(12.dp))
                        .clickable(enabled = !isBusy) { onDismiss() },
                    contentAlignment = Alignment.Center
                ) { Text("Cancel", color = Color.White, fontWeight = FontWeight.SemiBold) }

                Box(
                    modifier = Modifier.weight(1f).height(50.dp)
                        .background(Color(0xFFFF2E63), RoundedCornerShape(12.dp))
                        .clickable(enabled = !isBusy) { onPublish(caption, selectedPrivacy) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Text("Publish", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun PublishStatusOverlay(
    state: PublishUiState,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .background(Color(0xFF0F111A), RoundedCornerShape(20.dp))
                .border(1.dp, Color(0xFF2D3348), RoundedCornerShape(20.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (state.stage) {
                PublishStage.Uploading, PublishStage.Publishing -> {
                    CircularProgressIndicator(
                        color = Color(0xFFFF2E63),
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = if (state.stage == PublishStage.Publishing) "Publishing to TikTok…" else "Uploading video…",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = state.message ?: "",
                        color = Color(0xFF8A8FA6),
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    if (state.progress > 0) {
                        LinearProgressIndicator(
                            progress = { state.progress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFFF2E63),
                            trackColor = Color(0xFF2A2E3E)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("${state.progress}%", color = Color(0xFFFF2E63), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                PublishStage.Success -> {
                    Text("✅", fontSize = 40.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Posted to TikTok!", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(6.dp))
                    if (state.postId != null) {
                        Text("Post ID: ${state.postId}", color = Color(0xFF8A8FA6), fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(20.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                            .background(Color(0xFFFF2E63), RoundedCornerShape(14.dp))
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center
                    ) { Text("Done", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                }
                PublishStage.Error -> {
                    Text("❌", fontSize = 40.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Upload Failed", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = state.message ?: "Unknown error",
                        color = Color(0xFFFF2E63),
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(20.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                            .background(Color(0xFF141722), RoundedCornerShape(14.dp))
                            .border(1.dp, Color(0xFF2D3348), RoundedCornerShape(14.dp))
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center
                    ) { Text("Close", color = Color.White, fontWeight = FontWeight.SemiBold) }
                }
                else -> {}
            }
        }
    }
}