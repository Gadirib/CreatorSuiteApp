package com.example.creatorsuiteapp.ui.screens.content

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.creatorsuiteapp.R
import com.example.creatorsuiteapp.data.repository.SavedVideoRepository
import com.example.creatorsuiteapp.data.tiktok.TikTokSessionManager
import com.example.creatorsuiteapp.domain.model.SavedVideo
import com.example.creatorsuiteapp.ui.components.BottomNav
import com.example.creatorsuiteapp.ui.components.ProBadge
import com.example.creatorsuiteapp.ui.screens.rec.PublishSheet
import com.example.creatorsuiteapp.ui.screens.rec.PublishStatusOverlay
import com.example.creatorsuiteapp.ui.viewmodel.PublishStage
import com.example.creatorsuiteapp.ui.viewmodel.PublishViewModel
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ContentScreen(
    onOpenCreate: () -> Unit,
    onOpenCleaner: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val publishVm: PublishViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val publishState by publishVm.state.collectAsState()

    // ✅ Observe real saved videos from repository
    val savedVideos by SavedVideoRepository.videos.collectAsState()

    var selectedVideo by remember { mutableStateOf<SavedVideo?>(null) }
    var publishTarget by remember { mutableStateOf<SavedVideo?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Load from JSON on first composition
    LaunchedEffect(Unit) {
        SavedVideoRepository.load(context)
    }

    LaunchedEffect(publishState.stage, publishTarget?.id) {
        if (publishState.stage == PublishStage.Success) {
            val target = publishTarget ?: return@LaunchedEffect
            val postId = publishState.postId ?: return@LaunchedEffect
            SavedVideoRepository.markPosted(
                context = context,
                id = target.id,
                tiktokItemId = postId,
                username = TikTokSessionManager.getSession(context)?.username.orEmpty()
            )
        }
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
                Spacer(Modifier.height(8.dp))

                // ── Header ────────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(painterResource(R.drawable.creator_logo), null, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("CREATOR", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        Text("S U I T E", color = Color(0xFFA4A4C1), fontWeight = FontWeight.Medium, letterSpacing = 7.2.sp, fontSize = 12.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    ProBadge()
                    Spacer(Modifier.width(10.dp))
                    Image(
                        painterResource(R.drawable.ic_settings), "Settings",
                        modifier = Modifier.size(32.dp).clickable { onOpenSettings() },
                        colorFilter = ColorFilter.tint(Color(0xFFFF2E63))
                    )
                }

                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF22252C)))
                Spacer(Modifier.height(12.dp))

                // ── Sort + Search ─────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("SORT BY:", color = Color(0xFFA4A4C1).copy(alpha = 0.6f), fontSize = 12.sp, letterSpacing = 1.2.sp)
                        Text("DATE", color = Color(0xFFA4A4C1), fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.2.sp)
                    }
                    Spacer(Modifier.width(16.dp))
                    ContentSearchField(query = searchQuery, onQueryChange = { searchQuery = it }, modifier = Modifier.weight(1f))
                }

                Spacer(Modifier.height(12.dp))

                // ── Grid ──────────────────────────────────────────────────────
                val normalizedQuery = searchQuery.trim().lowercase()
                val filtered = savedVideos
                    .sortedByDescending { it.createdAt }
                    .filter { video ->
                        normalizedQuery.isBlank() ||
                                video.fileName.lowercase().contains(normalizedQuery) ||
                                video.tiktokUsername?.lowercase()?.contains(normalizedQuery) == true
                    }

                if (savedVideos.isEmpty()) {
                    EmptyContentState(modifier = Modifier.fillMaxWidth().weight(1f))
                } else if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxWidth().weight(1f), Alignment.Center) {
                        Text("No videos found", color = Color(0xFFA4A4C1), fontSize = 14.sp)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        items(filtered, key = { it.id }) { video ->
                            VideoGridCell(
                                video = video,
                                thumbFile = SavedVideoRepository.thumbFile(context, video),
                                onClick = { selectedVideo = video }
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

    // ── Action sheet ──────────────────────────────────────────────────────────
    if (selectedVideo != null) {
        val video = selectedVideo!!
        VideoActionSheet(
            video = video,
            thumbFile = SavedVideoRepository.thumbFile(context, video),
            onDismiss = { selectedVideo = null },
            onPost = {
                publishTarget = video
                selectedVideo = null
            },
            onShare = {
                scope.launch { snackbarHostState.showSnackbar("Shared") }
                selectedVideo = null
            },
            onDelete = {
                scope.launch {
                    SavedVideoRepository.delete(context, video.id)
                    snackbarHostState.showSnackbar("Deleted")
                }
                selectedVideo = null
            }
        )
    }

    if (publishTarget != null) {
        PublishSheet(
            onDismiss = { publishTarget = null },
            onPublish = { caption, privacy ->
                val target = publishTarget ?: return@PublishSheet
                val uri = android.net.Uri.fromFile(SavedVideoRepository.videoFile(context, target))
                publishVm.publishFromUri(uri, caption, privacy)
            },
            isBusy = publishState.stage == PublishStage.Uploading || publishState.stage == PublishStage.Publishing
        )
    }

    if (publishState.stage != PublishStage.Idle) {
        PublishStatusOverlay(
            state = publishState,
            onClose = {
                publishVm.reset()
                publishTarget = null
            }
        )
    }
}

// ── Video grid cell ───────────────────────────────────────────────────────────

@Composable
private fun VideoGridCell(
    video: SavedVideo,
    thumbFile: File,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(120f / 140f)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0A0C14))
            .clickable { onClick() }
    ) {
        // ✅ Real thumbnail from {app}/Thumbnails/{uuid}.jpg
        if (thumbFile.exists()) {
            AsyncImage(
                model = thumbFile,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Placeholder while thumbnail generates
            Box(Modifier.fillMaxSize().background(Color(0xFF1A1D29)))
        }

        // TikTok badge — pink if posted, dark if not
        Box(
            modifier = Modifier
                .padding(8.dp).size(24.dp)
                .background(
                    if (video.postedToTikTok) Color(0xFFFF2E63) else Color(0xFF2A0E1B),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painterResource(R.drawable.ic_tiktok_badge), null,
                modifier = Modifier.size(14.dp),
                colorFilter = ColorFilter.tint(Color.White)
            )
        }

        // Duration badge
        val durationText = formatDuration(video.duration)
        Text(
            text = durationText,
            color = Color(0xFFA4A4C1),
            fontSize = 10.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(999.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        )

        Image(
            painterResource(R.drawable.ic_menu_dots), null,
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(30.dp)
        )
    }
}

// ── Action sheet ──────────────────────────────────────────────────────────────

@Composable
private fun VideoActionSheet(
    video: SavedVideo,
    thumbFile: File,
    onDismiss: () -> Unit,
    onPost: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val videoFile = remember(video.id) { SavedVideoRepository.videoFile(context, video) }
    val player = remember(video.id) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
            playWhenReady = false
        }
    }
    var isPlaying by remember(video.id) { mutableStateOf(false) }
    var playerProgress by remember(video.id) { mutableFloatStateOf(0f) }
    var durationMs by remember(video.id) { mutableLongStateOf((video.duration * 1000).toLong()) }
    var showExpandedPlayer by remember(video.id) { mutableStateOf(false) }

    LaunchedEffect(video.id) {
        player.setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(videoFile)))
        player.prepare()
    }

    LaunchedEffect(video.id) {
        while (true) {
            kotlinx.coroutines.delay(100)
            val currentDuration = player.duration.takeIf { it > 0 } ?: durationMs
            val currentPosition = player.currentPosition
            durationMs = currentDuration
            playerProgress = if (currentDuration > 0) {
                (currentPosition.toFloat() / currentDuration).coerceIn(0f, 1f)
            } else {
                0f
            }
            isPlaying = player.isPlaying
        }
    }

    DisposableEffect(video.id) {
        onDispose {
            player.release()
        }
    }

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
                .fillMaxHeight(0.78f)
                .clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))
                .background(Color(0xFF0A0A0A))
                .padding(top = 24.dp)
                .clickable(interactionSource = interaction, indication = null) {},
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = formatDuration(video.duration),
                color = Color(0xFFA4A4C1), fontSize = 16.sp,
                letterSpacing = 1.6.sp, fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(20.dp))

            // ✅ Real thumbnail in action sheet
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF17171D))
                    .border(1.dp, Color(0xFF3A3D52), RoundedCornerShape(24.dp))
                    .padding(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0A0C14))
                        .clickable {
                            if (player.isPlaying) {
                                player.pause()
                                isPlaying = false
                            } else {
                                player.play()
                                isPlaying = true
                            }
                        }
                ) {
                    if (videoFile.exists()) {
                        AndroidView(
                            factory = {
                                PlayerView(it).apply {
                                    useController = false
                                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    this.player = player
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (thumbFile.exists()) {
                        AsyncImage(
                            model = thumbFile,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .size(22.dp)
                            .background(Color(0xFFFF2E63), RoundedCornerShape(11.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painterResource(R.drawable.ic_tiktok_badge),
                            null,
                            modifier = Modifier.size(11.dp),
                            colorFilter = ColorFilter.tint(Color.White)
                        )
                    }

                    Image(
                        painter = painterResource(R.drawable.ic_expand),
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .size(22.dp)
                            .clickable { showExpandedPlayer = true },
                        colorFilter = ColorFilter.tint(Color.White)
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(64.dp)
                            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(32.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isPlaying) "⏸" else "▶",
                            color = Color(0xFFFF2E63),
                            fontSize = 24.sp
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatMillis((playerProgress * durationMs).toLong()),
                        color = Color(0xFFA4A4C1),
                        fontSize = 10.sp
                    )
                    Slider(
                        value = playerProgress,
                        onValueChange = { progress ->
                            playerProgress = progress
                            player.seekTo((progress * durationMs).toLong())
                        },
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFF2E63),
                            activeTrackColor = Color(0xFFFF2E63),
                            inactiveTrackColor = Color(0xFF4A4D5F)
                        )
                    )
                    Text(
                        text = "-${formatMillis((durationMs - (playerProgress * durationMs).toLong()).coerceAtLeast(0L))}",
                        color = Color(0xFFA4A4C1),
                        fontSize = 10.sp
                    )
                }
            }

            if (video.postedToTikTok && video.tiktokUsername != null) {
                Spacer(Modifier.height(8.dp))
                Text("Posted as @${video.tiktokUsername}", color = Color(0xFF6F738A), fontSize = 12.sp)
            }

            Spacer(Modifier.height(24.dp))

            ActionSheetButton("Post to TikTok", Color(0xFF38040E), Color(0xFFFE2C55), R.drawable.ic_post_tiktok, onPost)
            Spacer(Modifier.height(12.dp))
            ActionSheetButton("Share", Color(0x1A27FFEA), Color(0xCC27FFEA), R.drawable.ic_share, onShare)
            Spacer(Modifier.height(12.dp))
            ActionSheetButton("Delete", Color(0x33FFFFFF), Color(0x33A4A4C1), R.drawable.ic_delete, onDelete)
        }
    }

    if (showExpandedPlayer) {
        Dialog(
            onDismissRequest = { showExpandedPlayer = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = {
                        PlayerView(it).apply {
                            useController = true
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            this.player = player
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                Image(
                    painter = painterResource(R.drawable.ic_expand),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(20.dp)
                        .size(28.dp)
                        .clickable { showExpandedPlayer = false },
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatDuration(seconds: Double): String {
    if (seconds <= 0) return "00:00"
    val total = seconds.toLong()
    val m = total / 60
    val s = total % 60
    return "%02d:%02d".format(m, s)
}

private fun formatMillis(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val total = ms / 1000
    val m = total / 60
    val s = total % 60
    return "%02d:%02d".format(m, s)
}

@Composable
private fun ContentSearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.ic_search), null, modifier = Modifier.size(24.dp), colorFilter = ColorFilter.tint(Color(0xFFA4A4C1).copy(alpha = 0.6f)))
        Spacer(Modifier.width(6.dp))
        BasicTextField(
            value = query, onValueChange = onQueryChange, singleLine = true,
            textStyle = TextStyle(color = Color(0xFFA4A4C1), fontSize = 12.sp, letterSpacing = 1.2.sp),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (query.isBlank()) Text("SEARCH", color = Color(0xFFA4A4C1), fontSize = 12.sp, letterSpacing = 1.8.sp)
                inner()
            }
        )
    }
}

@Composable
private fun EmptyContentState(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("YOU DON'T HAVE\nANY SAVED VIDEOS YET", color = Color(0xFFA4A4C1), fontSize = 14.sp, letterSpacing = 2.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Text("START CREATING\nVIRAL CONTENT NOW!", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, textAlign = TextAlign.Center)
        }
        Canvas(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp).size(180.dp)) {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(size.width * 0.65f, size.height * 0.05f)
                quadraticBezierTo(size.width * 0.10f, size.height * 0.35f, size.width * 0.45f, size.height * 0.86f)
            }
            drawPath(path, Color(0xFFB12A48), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            val tip = Offset(size.width * 0.45f, size.height * 0.86f)
            drawLine(Color(0xFFB12A48), tip, Offset(tip.x - 18f, tip.y - 12f), 6f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            drawLine(Color(0xFFB12A48), tip, Offset(tip.x + 6f, tip.y - 20f), 6f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
    }
}

@Composable
private fun ActionSheetButton(text: String, background: Color, border: Color, iconRes: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp).height(54.dp)
            .clip(RoundedCornerShape(14.dp)).background(background)
            .border(1.dp, border, RoundedCornerShape(14.dp)).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Image(painterResource(iconRes), null, modifier = Modifier.align(Alignment.CenterStart).padding(start = 24.dp).size(24.dp), colorFilter = ColorFilter.tint(Color.White))
        Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
