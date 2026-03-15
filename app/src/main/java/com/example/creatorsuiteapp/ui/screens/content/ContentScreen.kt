package com.example.creatorsuiteapp.ui.screens.content

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val vm: ContentViewModel = viewModel()
    val posts by vm.posts.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var selectedItem by remember { mutableStateOf<ContentItem?>(null) }
    var hiddenIds by remember { mutableStateOf(setOf<String>()) }
    var sampleItems by remember { mutableStateOf<List<ContentItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        vm.load()
        val prefs = context.getSharedPreferences("content_prefs", 0)
        hiddenIds = prefs.getStringSet("deleted_ids", emptySet())?.toSet().orEmpty()
    }

    Scaffold(
        containerColor = Color(0xFF010101),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF010101))
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .padding(bottom = 113.dp)
            ) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.creator_logo),
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
                ProBadge()
                Spacer(modifier = Modifier.width(10.dp))

                Image(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = "Settings",
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onOpenSettings() },
                    colorFilter = ColorFilter.tint(Color(0xFFFF2E63))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            BoxDivider()

            Spacer(modifier = Modifier.height(12.dp))

            if (loading) {
                Text("Loading...", color = Color(0xFF8A8FA6), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
            }
            if (error != null) {
                Text("Error: $error", color = Color(0xFFFF2E63), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("SORT BY:", color = Color(0xFFA4A4C1).copy(alpha = 0.6f), fontSize = 12.sp, letterSpacing = 1.2.sp)
                    Text(
                        "NAME",
                        color = Color(0xFFA4A4C1),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.2.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                ContentSearchField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val contentItems = if (posts.isEmpty()) {
                if (sampleItems.isEmpty()) {
                    val samples = listOf(
                        R.drawable.sample_img_1,
                        R.drawable.sample_video_1_thumb,
                        R.drawable.sample_img_2,
                        R.drawable.sample_video_2_thumb
                    )
                    val items = mutableListOf<ContentItem>()
                    samples.forEachIndexed { idx, res ->
                        items.add(
                            ContentItem(
                                id = "sample-$idx",
                                caption = "Video name",
                                status = null,
                                thumbRes = res,
                                isVideo = true,
                                isPlaceholder = false
                            )
                        )
                    }
                    while (items.size < 12) {
                        val idx = items.size
                        items.add(
                            ContentItem(
                                id = "placeholder-$idx",
                                caption = null,
                                status = null,
                                thumbRes = null,
                                isVideo = true,
                                isPlaceholder = true
                            )
                        )
                    }
                    sampleItems = items
                }
                sampleItems
            } else {
                posts
                    .filter { it.id !in hiddenIds }
                    .map { post ->
                        ContentItem(
                            id = post.id,
                            caption = post.caption,
                            status = post.status,
                            thumbRes = sampleThumbForId(post.id),
                            isVideo = true,
                            isPlaceholder = false
                        )
                    }
            }

            val normalizedQuery = searchQuery.trim().lowercase()
            val filteredContentItems = contentItems.filter { item ->
                normalizedQuery.isBlank() ||
                    item.caption.orEmpty().lowercase().contains(normalizedQuery) ||
                    item.id.lowercase().contains(normalizedQuery)
            }

            if (posts.isEmpty() && normalizedQuery.isBlank()) {
                EmptyContentState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else if (filteredContentItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No videos found",
                        color = Color(0xFFA4A4C1),
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    itemsIndexed(
                        items = filteredContentItems,
                        key = { _, item -> item.id }
                    ) { _, item ->
                        ContentGridCell(
                            item = item,
                            onClick = { selectedItem = item }
                        )
                    }
                }
            }
        }

            BottomNav(
                active = "CONTENT",
                onContentClick = {},
                onCenterClick = onOpenCreate,
                onCleanerClick = onOpenCleaner,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    if (selectedItem != null) {
        ContentActionSheet(
            item = selectedItem!!,
            onDismiss = { selectedItem = null },
            onPost = {
                scope.launch { snackbarHostState.showSnackbar("Post to TikTok") }
                selectedItem = null
            },
            onShare = {
                scope.launch { snackbarHostState.showSnackbar("Share") }
                selectedItem = null
            },
            onDelete = {
                val id = selectedItem!!.id
                if (id.startsWith("sample-")) {
                    sampleItems = sampleItems.filterNot { it.id == id }
                } else {
                    hiddenIds = hiddenIds + id
                    val prefs = context.getSharedPreferences("content_prefs", 0)
                    prefs.edit().putStringSet("deleted_ids", hiddenIds).apply()
                    vm.deletePost(id)
                }
                scope.launch { snackbarHostState.showSnackbar("Deleted") }
                selectedItem = null
            }
        )
    }
}

@Composable
private fun ContentSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            colorFilter = ColorFilter.tint(Color(0xFFA4A4C1).copy(alpha = 0.6f))
        )
        Spacer(modifier = Modifier.width(6.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(
                color = Color(0xFFA4A4C1),
                fontSize = 12.sp,
                letterSpacing = 1.2.sp
            ),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                if (query.isBlank()) {
                    Text("SEARCH", color = Color(0xFFA4A4C1), fontSize = 12.sp, letterSpacing = 1.8.sp)
                }
                innerTextField()
            }
        )
    }
}

@Composable
private fun BoxDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFF22252C))
    )
}

@Composable
private fun EmptyContentState(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "YOU DON'T HAVE\nANY SAVED VIDEOS YET",
                color = Color(0xFFA4A4C1),
                fontSize = 14.sp,
                letterSpacing = 2.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "START CREATING\nVIRAL CONTENT NOW!",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        // Curved arrow pointing to center button in bottom nav
        Canvas(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
                .size(180.dp)
        ) {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(size.width * 0.65f, size.height * 0.05f)
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
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
            val tip = Offset(size.width * 0.45f, size.height * 0.86f)
            drawLine(
                color = Color(0xFFB12A48),
                start = tip,
                end = Offset(tip.x - 18f, tip.y - 12f),
                strokeWidth = 6f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFB12A48),
                start = tip,
                end = Offset(tip.x + 6f, tip.y - 20f),
                strokeWidth = 6f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

@Composable
private fun ContentGridCell(
    item: ContentItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(120f / 140f)
            .clip(RoundedCornerShape(14.dp))
            .background(if (item.isPlaceholder) Color(0x1AA4A4C1) else Color(0xFF0A0C14))
            .clickable(enabled = !item.isPlaceholder) { onClick() }
    ) {
        if (item.thumbRes != null) {
            Image(
                painter = painterResource(item.thumbRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(24.dp)
                    .background(Color(0xFFFF2E63), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_tiktok_badge),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            }

            Image(
                painter = painterResource(R.drawable.ic_menu_dots),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(30.dp)
            )

            Text(
                text = "00:00",
                color = Color(0xFFA4A4C1),
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(24.dp)
                    .background(Color(0xFF2A0E1B), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_tiktok),
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.7f))
                )
            }
        }
    }
}

@Composable
private fun ContentActionSheet(
    item: ContentItem,
    onDismiss: () -> Unit,
    onPost: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .clickable { onDismiss() }
    ) {
        val interaction = remember { MutableInteractionSource() }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(550.dp)
                .clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))
                .background(Color(0xFF0A0A0A))
                .padding(top = 24.dp)
                .clickable(
                    interactionSource = interaction,
                    indication = null
                ) { },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "VIDEO NAME",
                color = Color(0xFFA4A4C1),
                fontSize = 16.sp,
                letterSpacing = 1.6.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .size(width = 120.dp, height = 140.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0A0C14))
            ) {
                if (item.thumbRes != null) {
                    Image(
                        painter = painterResource(item.thumbRes),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .size(24.dp)
                        .background(Color(0xFFFF2E63), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_tiktok),
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        colorFilter = ColorFilter.tint(Color.White)
                    )
                }
                Text(
                    text = "00:00",
                    color = Color(0xFFA4A4C1),
                    fontSize = 10.sp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            ActionSheetButton(
                text = "Post to TikTok",
                background = Color(0xFF38040E),
                border = Color(0xFFFE2C55),
                iconRes = R.drawable.ic_post_tiktok,
                onClick = onPost
            )
            Spacer(modifier = Modifier.height(12.dp))
            ActionSheetButton(
                text = "Share",
                background = Color(0x1A27FFEA),
                border = Color(0xCC27FFEA),
                iconRes = R.drawable.ic_share,
                onClick = onShare
            )
            Spacer(modifier = Modifier.height(12.dp))
            ActionSheetButton(
                text = "Delete",
                background = Color(0x33FFFFFF),
                border = Color(0x33A4A4C1),
                iconRes = R.drawable.ic_delete,
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun ActionSheetButton(
    text: String,
    background: Color,
    border: Color,
    iconRes: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .height(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp)
                .size(24.dp),
            colorFilter = ColorFilter.tint(Color.White)
        )
        Text(
            text = text,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private data class ContentItem(
    val id: String,
    val caption: String? = null,
    val status: String? = null,
    val thumbRes: Int?,
    val isVideo: Boolean,
    val isPlaceholder: Boolean
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
