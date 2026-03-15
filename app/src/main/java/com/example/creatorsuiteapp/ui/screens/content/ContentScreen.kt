package com.example.creatorsuiteapp.ui.screens.content

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.creatorsuiteapp.R
import com.example.creatorsuiteapp.ui.components.BottomNav
import com.example.creatorsuiteapp.ui.components.ProBadge
import com.example.creatorsuiteapp.ui.viewmodel.ContentViewModel
import kotlinx.coroutines.launch

@Composable
fun ContentScreen(
    onOpenCreate: () -> Unit,
    onOpenCleaner: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val vm: ContentViewModel = viewModel()
    val posts by vm.posts.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var editPostId by remember { mutableStateOf<String?>(null) }
    var editCaption by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("NAME") }

    LaunchedEffect(Unit) {
        vm.load()
    }

    Scaffold(
        containerColor = Color.Black,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(padding)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.creator_logo),
                contentDescription = null,
                modifier = Modifier.size(34.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text("CREATOR", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                Text(
                    "S U I T E",
                    color = Color(0xFFA5A7C5),
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 4.sp,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            ProBadge()
            Spacer(modifier = Modifier.width(10.dp))

            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Settings",
                tint = Color(0xFFFF2E63),
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onOpenSettings() }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        BoxDivider()

        Spacer(modifier = Modifier.height(16.dp))

        if (loading) {
            Text("Loading...", color = Color(0xFF8A8FA6), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
        }
        if (error != null) {
            Text("Error: $error", color = Color(0xFFFF2E63), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
        }

        val filteredPosts = posts
            .filter { post ->
                searchQuery.isBlank() || post.caption.contains(searchQuery, ignoreCase = true)
            }
            .let { list ->
                when (sortBy) {
                    "LIKES" -> list.sortedByDescending { it.likes }
                    "NEWEST" -> list.sortedByDescending { it.createdAt }
                    else -> list.sortedBy { it.caption.lowercase() }
                }
            }

        if (filteredPosts.isEmpty() && !loading) {
            EmptyContentState(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("SORT BY:", color = Color(0xFF6F738A), fontSize = 12.sp, letterSpacing = 2.sp)
                    listOf("NAME", "LIKES", "NEWEST").forEach { option ->
                        Text(
                            option,
                            color = if (sortBy == option) Color.White else Color(0xFF6F738A),
                            fontWeight = if (sortBy == option) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
                            modifier = Modifier.clickable { sortBy = option }
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        colorFilter = ColorFilter.tint(Color(0xFF6F738A))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SEARCH", color = Color(0xFF6F738A), fontSize = 12.sp, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Image(
                        painter = painterResource(R.drawable.ic_refresh),
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { vm.refresh() },
                        colorFilter = ColorFilter.tint(Color(0xFF6F738A))
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Search caption...", color = Color(0xFF6F738A)) },
                leadingIcon = {
                    Image(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        colorFilter = ColorFilter.tint(Color(0xFF6F738A))
                    )
                },
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2D3348),
                    unfocusedBorderColor = Color(0xFF2D3348),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFFFF2E63)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            val contentItems = filteredPosts.map { post ->
                ContentItem(
                    id = post.id,
                    caption = post.caption,
                    status = post.status,
                    thumbRes = sampleThumbForId(post.id),
                    isVideo = true
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                itemsIndexed(
                    items = contentItems,
                    key = { _, item -> item.id }
                ) { _, item ->
                    ContentGridCell(
                        thumbRes = item.thumbRes,
                        isVideo = item.isVideo,
                        status = item.status,
                        onEdit = {
                            editPostId = item.id
                            editCaption = item.caption.orEmpty()
                        },
                        onDelete = {
                            vm.deletePost(item.id)
                            scope.launch {
                                snackbarHostState.showSnackbar("Deleted")
                            }
                        }
                )
            }
        }
        }

        BottomNav(
            active = "CONTENT",
            onContentClick = {},
            onCenterClick = onOpenCreate,
            onCleanerClick = onOpenCleaner
        )
        }
    }

    if (editPostId != null) {
        AlertDialog(
            onDismissRequest = { editPostId = null },
            title = { Text("Edit Caption") },
            text = {
                OutlinedTextField(
                    value = editCaption,
                    onValueChange = { editCaption = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = editPostId
                        if (id != null) {
                            vm.editPostCaption(id, editCaption.trim())
                            scope.launch { snackbarHostState.showSnackbar("Caption updated") }
                        }
                        editPostId = null
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editPostId = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun BoxDivider() {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFF22252C))
    )
}

@Composable
private fun EmptyContentState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "YOU DON'T HAVE\nANY SAVED VIDEOS YET",
                color = Color(0xFF8A8FA6),
                fontSize = 16.sp,
                letterSpacing = 2.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "START CREATING\nVIRAL CONTENT NOW!",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        Canvas(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
                .size(180.dp)
        ) {
            val path = Path().apply {
                moveTo(size.width * 0.75f, size.height * 0.15f)
                quadraticBezierTo(
                    size.width * 0.10f,
                    size.height * 0.35f,
                    size.width * 0.45f,
                    size.height * 0.86f
                )
            }
            drawPath(
                path = path,
                color = Color(0xFFB12A48),
                style = Stroke(width = 6f, cap = StrokeCap.Round)
            )

            // arrow head
            val tip = Offset(size.width * 0.45f, size.height * 0.86f)
            drawLine(
                color = Color(0xFFB12A48),
                start = tip,
                end = Offset(tip.x - 18f, tip.y - 12f),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFB12A48),
                start = tip,
                end = Offset(tip.x + 6f, tip.y - 20f),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun ContentGridCell(
    thumbRes: Int?,
    isVideo: Boolean,
    status: String?,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.88f)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0A0C14))
    ) {
        if (thumbRes != null) {
            Image(
                painter = painterResource(thumbRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(26.dp)
                    .background(Color(0xFFFF2E63), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_tiktok),
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }

            if (!status.isNullOrBlank()) {
                val isSaved = status.equals("saved", ignoreCase = true)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .background(
                            if (isSaved) Color(0xFF1A7F67) else Color(0xFF2E3142),
                            RoundedCornerShape(999.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isSaved) "SAVED" else "POSTED",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (onDelete != null) {
                if (onEdit != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 4.dp, end = 34.dp)
                            .size(32.dp)
                            .clickable { onEdit() },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_edit),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.85f))
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(32.dp)
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_clean_feed),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.85f))
                    )
                }
            }

            if (isVideo) {
                Text(
                    text = "00:00",
                    color = Color.White,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(22.dp)
                    .background(Color(0xFF2A0E1B), RoundedCornerShape(11.dp))
            )
        }
    }
}

private data class ContentItem(
    val id: String,
    val caption: String? = null,
    val status: String? = null,
    val thumbRes: Int?,
    val isVideo: Boolean
)

private fun sampleThumbForId(id: String): Int {
    val samples = listOf(
        R.drawable.sample_img_1,
        R.drawable.sample_img_2,
        R.drawable.sample_video_1_thumb,
        R.drawable.sample_video_2_thumb
    )
    val idx = kotlin.math.abs(id.hashCode()) % samples.size
    return samples[idx]
}
