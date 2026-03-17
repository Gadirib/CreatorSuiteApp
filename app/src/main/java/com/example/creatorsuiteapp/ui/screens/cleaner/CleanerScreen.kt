package com.example.creatorsuiteapp.ui.screens.cleaner

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.creatorsuiteapp.data.tiktok.TikTokLoginActivity
import com.example.creatorsuiteapp.data.tiktok.TikTokSessionManager
import com.example.creatorsuiteapp.ui.components.BottomNav
import com.example.tiktokclone.ui.screens.cleaner.LoadState
import com.example.tiktokclone.ui.screens.cleaner.TikTokRepost
import com.example.tiktokclone.ui.screens.cleaner.UnRepostViewModel

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
    val deletePercent by vm.deletePercent.collectAsState()
    val deletionsToday by vm.deletionsToday.collectAsState()
    var lastDeletedCount by remember { mutableIntStateOf(0) }
    var showDeleteDone by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val loginLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val refreshed = TikTokSessionManager.getSession(context)
        if (refreshed != null) onBackToContent()
    }

    val session = TikTokSessionManager.getSession(context)
    if (session == null) {
        CleanerLoggedOutScreen(
            onConnect = {
                val intent = Intent(context, TikTokLoginActivity::class.java)
                loginLauncher.launch(intent)
            },
            onOpenCreate = onOpenCreate,
            onOpenCleaner = {},
            onOpenContent = onBackToContent
        )
        return
    }

    val totalReposts = reposts.size
    val selectedCount = selectedIds.size
    val normalizedQuery = searchQuery.trim().lowercase()
    val filteredReposts = reposts.filter { repost ->
        normalizedQuery.isBlank() ||
            repost.authorUsername.lowercase().contains(normalizedQuery) ||
            repost.authorNickname.lowercase().contains(normalizedQuery) ||
            repost.desc.lowercase().contains(normalizedQuery)
    }

    LaunchedEffect(Unit) {
        if (loadState is LoadState.Idle) {
            vm.initApi(context)
            vm.loadReposts()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF010101))) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .padding(bottom = 113.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(com.example.creatorsuiteapp.R.drawable.creator_logo),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("CREATOR", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Text(
                        "S U I T E",
                        color = Color(0xFFA4A4C1),
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 7.2.sp,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                com.example.creatorsuiteapp.ui.components.ProBadge()
                Spacer(modifier = Modifier.width(10.dp))
                Image(
                    painter = painterResource(com.example.creatorsuiteapp.R.drawable.ic_settings),
                    contentDescription = "Settings",
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onOpenSettings() },
                    colorFilter = ColorFilter.tint(Color(0xFFFF2E63))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF22252C)))

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CleanerStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Reposts",
                    value = totalReposts.toString().padStart(2, '0'),
                    iconRes = com.example.creatorsuiteapp.R.drawable.ic_tiktok_badge
                )
                CleanerStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Selected",
                    value = selectedCount.toString().padStart(2, '0'),
                    iconRes = com.example.creatorsuiteapp.R.drawable.ic_cleaner_done
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            CleanerSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it }
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CleanerActionButton(
                    modifier = Modifier.weight(1f),
                    text = if (selectedCount == totalReposts && totalReposts > 0) "Deselect All" else "Select All",
                    iconRes = com.example.creatorsuiteapp.R.drawable.ic_cleaner_select_all,
                    onClick = { if (selectedCount == totalReposts && totalReposts > 0) vm.clearSelection() else vm.selectAll() }
                )
                CleanerIconButton(
                    iconRes = com.example.creatorsuiteapp.R.drawable.ic_cleaner_refresh,
                    onClick = { vm.loadReposts() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                        } else if (filteredReposts.isEmpty()) {
                            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No creators found", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Text("Try another search", color = Color(0xFF8A8FA6))
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                                items(filteredReposts, key = { it.id }) { repost ->
                                    CleanerRepostRow(
                                        repost = repost,
                                        selected = repost.id in selectedIds,
                                        onClick = { vm.toggleSelect(repost.id) }
                                    )
                                }
                            }
                        }
                    }
                    else -> {}
                }

            }
        }

        if (selectedIds.isNotEmpty() && deleteProgress == null) {
            val canDelete = deletionsToday < vm.dailyLimit
            CleanerSelectionBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 126.dp, start = 24.dp, end = 24.dp),
                selectedCount = selectedIds.size,
                onWipeAll = {
                    if (!canDelete) { Toast.makeText(context, "Daily limit reached", Toast.LENGTH_LONG).show(); return@CleanerSelectionBar }
                    lastDeletedCount = selectedIds.size
                    vm.deleteSelected { deleted, failed ->
                        if (failed == 0) {
                            showDeleteDone = true
                        } else {
                            Toast.makeText(context, "Deleted $deleted, failed $failed", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        }

        if (deleteProgress != null) {
            CleanerDeleteProgressOverlay(
                progressPercent = deletePercent ?: 0,
                selectedCount = lastDeletedCount
            )
        }

        if (showDeleteDone && deleteProgress == null) {
            CleanerDeleteDoneOverlay(
                deletedCount = lastDeletedCount,
                onDismiss = { showDeleteDone = false }
            )
        }

        BottomNav(
            active = "CLEANER",
            onContentClick = onBackToContent,
            onCenterClick = onOpenCreate,
            onCleanerClick = {},
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun CleanerDeleteProgressOverlay(progressPercent: Int, selectedCount: Int) {
    val clampedPercent = progressPercent.coerceIn(0, 100)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedPercent / 100f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "cleaner_delete_progress"
    )
    val displayedPercent = (animatedProgress * 100).toInt().coerceIn(0, 100)
    val postLabel = if (selectedCount == 1) "post" else "posts"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(210.dp)) {
                    drawCircle(
                        color = Color(0xFF3A3A4A),
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = Color(0xFFFF2E63),
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress.coerceIn(0f, 1f),
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "$displayedPercent%",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(Modifier.height(24.dp))
            Text("Cleaning Feed", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Removing $selectedCount $postLabel safely...",
                color = Color(0xFFA4A4C1),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun CleanerDeleteDoneOverlay(deletedCount: Int, onDismiss: () -> Unit) {
    val repostLabel = if (deletedCount == 1) "Repost" else "Reposts"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(210.dp)
                    .clip(RoundedCornerShape(105.dp))
                    .background(Color(0xFF005B2E).copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", color = Color.White, fontSize = 92.sp, fontWeight = FontWeight.Light)
            }
            Spacer(Modifier.height(24.dp))
            Text("Done!", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Successfully removed\n$deletedCount $repostLabel",
                color = Color(0xFFA4A4C1),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(84.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFE2C55))
            ) {
                Text("Got it!", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun CleanerLoggedOutScreen(
    onConnect: () -> Unit,
    onOpenCreate: () -> Unit,
    onOpenCleaner: () -> Unit,
    onOpenContent: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF010101))) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .padding(bottom = 113.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(com.example.creatorsuiteapp.R.drawable.creator_logo),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("CREATOR", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Text(
                        "S U I T E",
                        color = Color(0xFFA4A4C1),
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 7.2.sp,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                com.example.creatorsuiteapp.ui.components.ProBadge()
                Spacer(modifier = Modifier.width(10.dp))

                Image(
                    painter = painterResource(com.example.creatorsuiteapp.R.drawable.ic_settings),
                    contentDescription = "Settings",
                    modifier = Modifier.size(32.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color(0xFFFF2E63))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF22252C)))

            Spacer(modifier = Modifier.height(70.dp))

            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF111216)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(com.example.creatorsuiteapp.R.drawable.ic_cleaner_empty),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "MANAGE YOUR REPOSTS",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Log in to your TikTok\naccount to view and remove\nyour reposts anytime",
                color = Color(0xFFA4A4C1),
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            Button(
                onClick = onConnect,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFE2C55))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(com.example.creatorsuiteapp.R.drawable.ic_user_22),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Connect TikTok",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        BottomNav(
            active = "CLEANER",
            onContentClick = onOpenContent,
            onCenterClick = onOpenCreate,
            onCleanerClick = onOpenCleaner,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun CleanerRepostRow(repost: TikTokRepost, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF14151A))
            .border(1.dp, if (selected) Color(0xFFFF2E63) else Color.Transparent, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = repost.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(18.dp)
                    .background(Color(0xFFFF2E63), RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(com.example.creatorsuiteapp.R.drawable.ic_tiktok_badge),
                    contentDescription = null,
                    modifier = Modifier.size(10.dp)
                )
            }
            Text(
                text = "00:00",
                color = Color(0xFFA4A4C1),
                fontSize = 8.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text("@${repost.authorUsername}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(com.example.creatorsuiteapp.R.drawable.ic_cleaner_days),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    colorFilter = ColorFilter.tint(Color(0xFFA4A4C1))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formatDaysAgo(repost.createTime),
                    color = Color(0xFFA4A4C1),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Image(
                    painter = painterResource(com.example.creatorsuiteapp.R.drawable.ic_heart),
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    colorFilter = ColorFilter.tint(Color(0xFFFF2E63))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatLikes(repost.likeCount),
                    color = Color(0xFFFF2E63),
                    fontSize = 12.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(if (selected) Color(0xFFFF2E63) else Color(0xFF0F0F14))
                .border(1.dp, Color(0xFF3A3A42), RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Text("✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CleanerStatCard(modifier: Modifier = Modifier, title: String, value: String, iconRes: Int) {
    Box(
        modifier = modifier
            .height(96.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF14151A))
            .padding(12.dp)
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 32.sp,
            lineHeight = 35.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp, top = 6.dp)
        )
        Text(
            text = title,
            color = Color(0xFFA4A4C1),
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}

@Composable
private fun CleanerSearchBar(query: String, onQueryChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF14151A))
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Image(
            painter = painterResource(com.example.creatorsuiteapp.R.drawable.ic_cleaner_search),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            colorFilter = ColorFilter.tint(Color(0xFFA4A4C1))
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(
                color = Color(0xFFA4A4C1),
                fontSize = 12.sp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 28.dp),
            decorationBox = { innerTextField ->
                if (query.isBlank()) {
                    Text(
                        text = "Search creators...",
                        color = Color(0xFFA4A4C1),
                        fontSize = 12.sp
                    )
                }
                innerTextField()
            }
        )
    }
}

@Composable
private fun CleanerActionButton(modifier: Modifier = Modifier, text: String, iconRes: Int, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF14151A))
            .clickable { onClick() },
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(Color.White)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CleanerIconButton(iconRes: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF14151A))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            colorFilter = ColorFilter.tint(Color.White)
        )
    }
}

@Composable
private fun CleanerSelectionBar(modifier: Modifier = Modifier, selectedCount: Int, onWipeAll: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFF2E63))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            Text("$selectedCount selected", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("READY TO REMOVE", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp, letterSpacing = 1.sp)
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .height(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
                .clickable { onWipeAll() }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(com.example.creatorsuiteapp.R.drawable.ic_delete),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    colorFilter = ColorFilter.tint(Color(0xFFFF2E63))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("WIPE ALL", color = Color(0xFFFF2E63), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

private fun formatDaysAgo(createTimeSec: Long): String {
    if (createTimeSec <= 0L) return "recently"
    val nowMs = System.currentTimeMillis()
    val createdMs = createTimeSec * 1000
    val days = ((nowMs - createdMs) / 86_400_000L).coerceAtLeast(0L)
    return if (days == 0L) "today" else "$days days ago"
}

private fun formatLikes(likes: Int): String {
    return when {
        likes >= 1_000_000 -> String.format("%.1fm", likes / 1_000_000f)
        likes >= 1_000 -> String.format("%.1fk", likes / 1_000f)
        else -> likes.toString()
    }
}
