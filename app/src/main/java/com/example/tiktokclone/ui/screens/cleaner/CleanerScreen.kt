package com.example.tiktokclone.ui.screens.cleaner

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tiktokclone.R
import com.example.tiktokclone.domain.model.CleanerItem
import com.example.tiktokclone.ui.components.BottomNav
import com.example.tiktokclone.ui.components.ProBadge
import com.example.tiktokclone.ui.viewmodel.CleanerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val BULK_OPEN_CONFIRM_THRESHOLD = 5

@Composable
fun CleanerScreen(
    onBackToContent: () -> Unit,
    onOpenCreate: () -> Unit,
    onOpenSettings: () -> Unit
    
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vm: CleanerViewModel = viewModel()
    val items by vm.items.collectAsState()
    val cleanProgress by vm.cleanProgress.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val username by vm.username.collectAsState<String?>()   // ← add <String?>

    var selectedSet by remember { mutableStateOf(setOf<String>()) }
    var searchQuery by remember { mutableStateOf("") }
    val filteredItems = items.filter {
        searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true)
    }
    val selectedCount = selectedSet.size
    val allSelected = filteredItems.isNotEmpty() && filteredItems.all { it.postId in selectedSet }

    var showCleaning by remember { mutableStateOf(false) }
    var showDone by remember { mutableStateOf(false) }
    var removedCount by remember { mutableStateOf(0) }
    var isOpeningSelected by remember { mutableStateOf(false) }
    var showBulkOpenConfirm by remember { mutableStateOf(false) }
    var pendingBulkUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var pendingBulkPostIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var showWebCleaner by remember { mutableStateOf(false) }


    

    LaunchedEffect(items) {
        val availableIds = items.map { it.postId }.toSet()
        selectedSet = selectedSet.intersect(availableIds)
    }

    fun runOpenAndClean(urls: List<String>, postIds: List<String>) {
        removedCount = postIds.size
        if (urls.isNotEmpty() && !isOpeningSelected) {
            isOpeningSelected = true
            scope.launch {
                urls.forEachIndexed { index, url ->
                    openInTikTok(context, url)
                    if (index < urls.lastIndex) delay(900)
                }
                isOpeningSelected = false
            }
        }
        // Official API does not support deleting reposts; keep UI informational.
        showCleaning = false
        showDone = false
        vm.startClean(postIds)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.creator_logo),
                contentDescription = null,
                modifier = Modifier.size(34.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text("CREATOR", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                Text("S U I T E", color = Color(0xFFA5A7C5), fontWeight = FontWeight.SemiBold, letterSpacing = 4.sp, fontSize = 12.sp)
            }
            Spacer(Modifier.weight(1f))
            ProBadge()
            Spacer(Modifier.width(10.dp))
            Image(
                painter = painterResource(R.drawable.ic_settings),
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onOpenSettings() },
                colorFilter = ColorFilter.tint(Color(0xFFFF2E63))
            )
        }

        // ... your existing Column / Scaffold / list / checkboxes / start clean button ...

        Spacer(modifier = Modifier.height(24.dp))

        // Manual reposts access (official API doesn't expose repost list)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    val user = (username ?: "yourusername").removePrefix("@")
                    openInTikTok(context, "https://www.tiktok.com/@$user?tab=reposts")
                },
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFFF2E63),
                    containerColor = Color.Transparent
                ),
                border = BorderStroke(2.dp, Color(0xFFFF2E63))
            ) {
                Text("Open Reposts in TikTok", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { showWebCleaner = true },
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFFF2E63),
                    containerColor = Color.Transparent
                ),
                border = BorderStroke(2.dp, Color(0xFFFF2E63))
            ) {
                Text("Open Web Cleaner", fontWeight = FontWeight.Bold)
            }
        }

        if (showWebCleaner) {
            WebRepostCleanerScreen(
                username = username ?: "yourusername",  // ← from ViewModel / getMe()
                onBack = { showWebCleaner = false }
            )
        }

        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF22252C)))
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CleanerStatCard(
                modifier = Modifier.weight(1f),
                iconRes = R.drawable.ic_tiktok,
                value = items.size.toString().padStart(2, '0'),
                label = "Total Reposts",
                iconTint = Color(0xFFFF2E63)
            )
            CleanerStatCard(
                modifier = Modifier.weight(1f),
                iconVector = Icons.Outlined.Check,
                value = selectedCount.toString().padStart(2, '0'),
                label = "Selected",
                iconTint = Color(0xFF2EE6A6)
            )
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Search creators...", color = Color(0xFF6C718A)) },
            leadingIcon = {
                Image(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    colorFilter = ColorFilter.tint(Color(0xFF6C718A))
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2D3348),
                unfocusedBorderColor = Color(0xFF2D3348),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFFFF2E63)
            )
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .background(Color(0xFF141722), RoundedCornerShape(14.dp))
                    .clickable {
                        selectedSet = if (allSelected) {
                            selectedSet - filteredItems.map { it.postId }.toSet()
                        } else {
                            selectedSet + filteredItems.map { it.postId }.toSet()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.ic_select_all),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        colorFilter = ColorFilter.tint(Color.White)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (allSelected) "Unselect All" else "Select All", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFF141722), RoundedCornerShape(14.dp))
                    .clickable {
                        selectedSet = emptySet()
                        vm.refresh()
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_refresh),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            val topMessage = when {
                loading -> "Loading..."
                error != null -> "Error: $error"
                else -> null
            }
            if (topMessage != null) {
                Text(
                    text = topMessage,
                    color = if (error != null) Color(0xFFFF2E63) else Color(0xFF8A8FA6),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 6.dp)
                )
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (selectedCount > 0) 200.dp else 90.dp)
            ) {
                items(filteredItems) { item ->
                    CleanerRow(
                        item = item,
                        selected = selectedSet.contains(item.postId),
                        onOpenTikTok = {
                            item.tiktokUrl?.let { openInTikTok(context, it) }
                        },
                        onToggle = {
                            selectedSet = if (selectedSet.contains(item.postId)) {
                                selectedSet - item.postId
                            } else {
                                selectedSet + item.postId
                            }
                        }
                    )
                }
            }

            if (selectedCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .padding(bottom = 120.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .background(Color(0xFFFF2E63), RoundedCornerShape(18.dp))
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("$selectedCount selected", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                            Text("READY TO REMOVE", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, letterSpacing = 2.sp)
                        }

                        Box(
                            modifier = Modifier
                                .height(42.dp)
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .clickable {
                                    val selectedItems = items.filter { it.postId in selectedSet }
                                    val urls = selectedItems.mapNotNull { it.tiktokUrl }.distinct()
                                    val ids = selectedItems.map { it.postId }
                                    if (urls.size > BULK_OPEN_CONFIRM_THRESHOLD) {
                                        pendingBulkUrls = urls
                                        pendingBulkPostIds = ids
                                        showBulkOpenConfirm = true
                                    } else {
                                        runOpenAndClean(urls, ids)
                                    }
                                }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(R.drawable.ic_clean_feed),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    colorFilter = ColorFilter.tint(Color(0xFFFF2E63))
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (isOpeningSelected) "OPENING..." else "OPEN SELECTED",
                                    color = Color(0xFFFF2E63),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            BottomNav(
                active = "CLEANER",
                onContentClick = onBackToContent,
                onCenterClick = onOpenCreate,
                onCleanerClick = {},
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            )
        }
    }

    if (showCleaning) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f)),
            contentAlignment = Alignment.Center
        ) {
            CleaningOverlay(
                progress = cleanProgress,
                onFinish = {
                    showCleaning = false
                    showDone = true
                }
            )
        }
    }

    if (showDone) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable { showDone = false },
            contentAlignment = Alignment.Center
        ) {
            DoneOverlay(
                removedCount = removedCount,
                onClose = { showDone = false }
            )
        }
    }

    if (showBulkOpenConfirm) {
        AlertDialog(
            onDismissRequest = { showBulkOpenConfirm = false },
            title = { Text("Open Multiple TikTok Links?") },
            text = { Text("You selected ${pendingBulkUrls.size} items. The app will open them one-by-one in TikTok.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBulkOpenConfirm = false
                        runOpenAndClean(pendingBulkUrls, pendingBulkPostIds)
                        pendingBulkUrls = emptyList()
                        pendingBulkPostIds = emptyList()
                    }
                ) { Text("Continue") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showBulkOpenConfirm = false
                        pendingBulkUrls = emptyList()
                        pendingBulkPostIds = emptyList()
                    }
                ) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CleaningOverlay(progress: Float, onFinish: () -> Unit) {
    val accentPink = Color(0xFFFF2E63)
    val track = Color(0xFF2A2D3A)

    LaunchedEffect(progress) {
        if (progress >= 1f) onFinish()
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(220.dp)
        ) {
            CircularProgressIndicator(
                progress = { progress },
                strokeWidth = 10.dp,
                color = accentPink,
                trackColor = track,
                modifier = Modifier.fillMaxSize()
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(Modifier.height(16.dp))
        Text("Cleaning Feed", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(6.dp))
        Text("Removing reposts safely...", color = Color(0xFF8A8FA6), fontSize = 14.sp)
    }
}

@Composable
private fun DoneOverlay(removedCount: Int, onClose: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(Color(0xFF0E4E2A), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("✓", color = Color.White, fontSize = 96.sp, fontWeight = FontWeight.ExtraBold)
        }

        Spacer(Modifier.height(18.dp))
        Text("Done!", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "Successfully removed\n${removedCount.toString().padStart(2, '0')} Reposts",
            color = Color(0xFF8A8FA6),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color(0xFFFF2E63), RoundedCornerShape(14.dp))
                .clickable { onClose() },
            contentAlignment = Alignment.Center
        ) {
            Text("Got it!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
private fun CleanerStatCard(
    modifier: Modifier,
    iconRes: Int? = null,
    iconVector: ImageVector? = null,
    value: String,
    label: String,
    iconTint: Color
) {
    Column(
        modifier = modifier
            .height(104.dp)
            .background(Color(0xFF141722), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        when {
            iconVector != null -> {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            iconRes != null -> {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    colorFilter = ColorFilter.tint(iconTint)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = value,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 30.sp
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = label,
            color = Color(0xFF8A8FA6),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CleanerRow(
    item: CleanerItem,
    selected: Boolean,
    onOpenTikTok: () -> Unit,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .background(Color(0xFF141722), RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (selected) Color(0xFFFF2E63) else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .clickable { onToggle() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(66.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0F111A))
        ) {
            Image(
                painter = painterResource(item.thumbRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(18.dp)
                    .background(Color(0xFFFF2E63), RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_tiktok),
                    contentDescription = null,
                    modifier = Modifier.size(10.dp),
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }

            Text(
                item.duration,
                color = Color.White,
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_clock),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    colorFilter = ColorFilter.tint(Color(0xFF7C8096))
                )
                Spacer(Modifier.width(6.dp))
                Text(item.days, color = Color(0xFF7C8096), fontSize = 12.sp)
                Spacer(Modifier.width(10.dp))
                Image(
                    painter = painterResource(R.drawable.ic_heart),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    colorFilter = ColorFilter.tint(Color(0xFFFF2E63))
                )
                Spacer(Modifier.width(6.dp))
                Text(item.likes, color = Color(0xFFFF2E63), fontSize = 12.sp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Open",
                    color = Color(0xFF2EE6A6),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onOpenTikTok() }
                )
            }
        }

        Box(
            modifier = Modifier
                .size(28.dp)
                .background(if (selected) Color(0xFFFF2E63) else Color.Transparent, CircleShape)
                .border(2.dp, Color(0xFF4A4E63), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun openInTikTok(context: android.content.Context, url: String) {
    val webUri = Uri.parse(url)
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, webUri).apply {
                setPackage("com.zhiliaoapp.musically")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }.onFailure {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, webUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}

private fun profileUrlFromItem(item: CleanerItem): String? {
    val url = item.tiktokUrl ?: return null
    val marker = "/video/"
    val index = url.indexOf(marker)
    return if (index > 0) url.substring(0, index) else url
}
