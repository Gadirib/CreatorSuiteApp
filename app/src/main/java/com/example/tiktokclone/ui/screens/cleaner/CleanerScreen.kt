package com.example.tiktokclone.ui.screens.cleaner

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.tiktokclone.data.tiktok.TikTokSessionManager
import com.example.tiktokclone.ui.components.BottomNav

@Composable
fun CleanerScreen(
    onBackToContent: () -> Unit,
    onOpenCreate: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val vm: UnRepostViewModel = viewModel()

    val reposts by vm.reposts.collectAsState()
    val selectedIds by vm.selectedIds.collectAsState()
    val loadState by vm.loadState.collectAsState()
    val deleteProgress by vm.deleteProgress.collectAsState()
    val deletionsToday by vm.deletionsToday.collectAsState()

    val session = TikTokSessionManager.getSession(context)
    if (session == null) {
        LaunchedEffect(Unit) { onBackToContent() }
        return
    }

    LaunchedEffect(Unit) {
        if (loadState is LoadState.Idle) {
            // ✅ init() on Main thread — WebView creation requires Main thread
            // loadReposts() starts immediately; TikTokApiService will wait
            // internally until the WebView fires onPageFinished before calling fetch()
            vm.initApi(context)
            vm.loadReposts()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
            Spacer(Modifier.height(52.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackToContent) {
                    Icon(Icons.Outlined.ArrowBack, null, tint = Color.White)
                }
                Text("CLEAN REPOSTS", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, letterSpacing = 2.sp)
                Spacer(Modifier.weight(1f))
                if (reposts.isNotEmpty() && loadState is LoadState.Success) {
                    TextButton(onClick = { if (selectedIds.size == reposts.size) vm.clearSelection() else vm.selectAll() }) {
                        Text(if (selectedIds.size == reposts.size) "Deselect all" else "Select all", color = Color(0xFFFF2E63), fontSize = 13.sp)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Deletions today: $deletionsToday / ${vm.dailyLimit}", color = Color(0xFF8A8FA6), fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                if (reposts.isNotEmpty()) Text("${reposts.size} reposts", color = Color(0xFF8A8FA6), fontSize = 12.sp)
            }

            Box(modifier = Modifier.weight(1f)) {
                when (val s = loadState) {
                    is LoadState.Loading -> Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFFFF2E63))
                        Spacer(Modifier.height(16.dp))
                        Text("Loading reposts…", color = Color(0xFF8A8FA6))
                    }
                    is LoadState.Error -> Column(modifier = Modifier.align(Alignment.Center).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.message, color = Color(0xFFFF2E63), textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { vm.initApi(context); vm.loadReposts() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2E63))
                        ) { Text("Retry") }
                    }
                    is LoadState.Success -> {
                        if (reposts.isEmpty()) {
                            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No reposts found", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Text("Your feed is clean!", color = Color(0xFF8A8FA6))
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(reposts, key = { it.id }) { repost ->
                                    RepostGridCell(repost = repost, selected = repost.id in selectedIds, onClick = { vm.toggleSelect(repost.id) })
                                }
                            }
                        }
                    }
                    else -> {}
                }

                if (deleteProgress != null) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFFFF2E63))
                            Spacer(Modifier.height(16.dp))
                            Text(deleteProgress ?: "", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (selectedIds.isNotEmpty() && deleteProgress == null) {
                val canDelete = deletionsToday < vm.dailyLimit
                Button(
                    onClick = {
                        if (!canDelete) { Toast.makeText(context, "Daily limit reached", Toast.LENGTH_LONG).show(); return@Button }
                        vm.deleteSelected { deleted, failed ->
                            Toast.makeText(context, if (failed == 0) "Deleted $deleted ✓" else "Deleted $deleted, failed $failed", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (canDelete) Color(0xFFFF2E63) else Color(0xFF444444))
                ) {
                    Text("Delete ${selectedIds.size} selected", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            BottomNav(active = "CLEANER", onContentClick = onBackToContent, onCenterClick = onOpenCreate, onCleanerClick = {})
        }
    }
}

@Composable
private fun RepostGridCell(repost: TikTokRepost, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(0.75f)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1A1A2E))
            .clickable(onClick = onClick)
            .then(if (selected) Modifier.border(2.dp, Color(0xFFFF2E63), RoundedCornerShape(10.dp)) else Modifier)
    ) {
        if (!repost.coverUrl.isNullOrEmpty()) {
            AsyncImage(model = repost.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.45f).align(Alignment.BottomCenter)
            .background(brush = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.75f)))))
        Text("@${repost.authorUsername}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
            modifier = Modifier.align(Alignment.BottomStart).padding(6.dp))
        if (selected) {
            Box(modifier = Modifier.padding(6.dp).size(22.dp).background(Color(0xFFFF2E63), RoundedCornerShape(99.dp)).align(Alignment.TopEnd), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}