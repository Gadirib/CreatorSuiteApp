@file:OptIn(androidx.media3.common.util.UnstableApi::class)
package com.example.creatorsuiteapp.ui.screens.editor

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbAdjustment
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.creatorsuiteapp.R
import com.example.creatorsuiteapp.data.media.MediaSelectionStore
import com.example.creatorsuiteapp.data.repository.SavedVideoRepository
import com.example.creatorsuiteapp.ui.screens.rec.PublishSheet
import com.example.creatorsuiteapp.ui.screens.rec.PublishStatusOverlay
import com.example.creatorsuiteapp.ui.theme.AppStickers
import com.example.creatorsuiteapp.ui.viewmodel.PublishStage
import com.example.creatorsuiteapp.ui.viewmodel.PublishViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt
import androidx.compose.ui.geometry.Offset as GOffset
import java.io.File

@OptIn(UnstableApi::class)
@Composable
fun VideoTrimScreen(onClose: () -> Unit, onSaved: () -> Unit = onClose) {
    val accentPink = Color(0xFFFF2E63)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val publishVm: PublishViewModel = viewModel()
    val publishState by publishVm.state.collectAsState()

    val selectedVideoUri by MediaSelectionStore.videoUri.collectAsState()

    val exoPlayer = remember { ExoPlayer.Builder(context).build() }
    var isPlaying by remember { mutableStateOf(false) }
    var playerProgress by remember { mutableFloatStateOf(0f) }
    var durationMs by remember { mutableLongStateOf(10_000L) }

    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }

    LaunchedEffect(selectedVideoUri) {
        val videoUri = selectedVideoUri ?: RawResourceDataSource.buildRawResourceUri(R.raw.sample_video_1)
        val factory = DefaultDataSource.Factory(context)
        val videoSource = ProgressiveMediaSource.Factory(factory).createMediaSource(MediaItem.fromUri(videoUri))
        exoPlayer.setMediaSource(videoSource)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = false
        repeat(20) {
            delay(250)
            val dur = exoPlayer.duration
            if (dur > 0) {
                durationMs = dur
                return@LaunchedEffect
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(100)
            val dur = exoPlayer.duration.takeIf { it > 0 } ?: durationMs
            val pos = exoPlayer.currentPosition
            playerProgress = (pos.toFloat() / dur).coerceIn(0f, 1f)
            isPlaying = exoPlayer.isPlaying
            if (dur != durationMs && dur > 0) durationMs = dur
        }
    }

    var selectedTab by remember { mutableStateOf("Trimming") }

    var leftFraction by remember { mutableFloatStateOf(0f) }
    var rightFraction by remember { mutableFloatStateOf(1f) }
    var splitFraction by remember { mutableFloatStateOf(0f) }
    var selectedAction by remember { mutableStateOf("") }
    var isTrimming by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var trimError by remember { mutableStateOf<String?>(null) }

    var thumbFrames by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    LaunchedEffect(selectedVideoUri) {
        val uri = selectedVideoUri ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                try {
                    val fd = context.contentResolver.openFileDescriptor(uri, "r")
                    if (fd != null) { retriever.setDataSource(fd.fileDescriptor); fd.close() }
                    else retriever.setDataSource(context, uri)
                } catch (e2: Exception) { retriever.setDataSource(context, uri) }
                val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 10_000L
                val frames = (0 until 6).mapNotNull { i ->
                    val timeUs = (i * dur * 1000L / 6).coerceAtLeast(0L)
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let {
                        Bitmap.createScaledBitmap(it, 80, 80, true)
                    }
                }
                thumbFrames = frames
            } catch (e: Exception) {
            } finally {
                try { retriever.release() } catch (e: Exception) {}
            }
        }
    }

    val adjustTabs = listOf("contrast", "brightness", "saturation", "exposure", "vibrance")
    var selectedAdjust by remember { mutableStateOf("contrast") }
    val adjustValues = remember {
        mutableStateMapOf("contrast" to 0f, "brightness" to 0f, "saturation" to 0f, "exposure" to 0f, "vibrance" to 0f)
    }

    val ratios = listOf("Original", "1:1", "5:4", "9:16", "16:9")
    var selectedRatio by remember { mutableStateOf("Original") }

    LaunchedEffect(
        selectedRatio,
        adjustValues["contrast"], adjustValues["brightness"],
        adjustValues["saturation"], adjustValues["exposure"], adjustValues["vibrance"]
    ) {
        delay(300)
        val sizeEffects: List<androidx.media3.common.Effect> = when (selectedRatio) {
            "1:1"  -> listOf(Presentation.createForAspectRatio(1f, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP))
            "5:4"  -> listOf(Presentation.createForAspectRatio(5f/4f, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP))
            "9:16" -> listOf(Presentation.createForAspectRatio(9f/16f, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP))
            "16:9" -> listOf(Presentation.createForAspectRatio(16f/9f, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP))
            else   -> emptyList()
        }
        val allEffects = sizeEffects + buildEffects(adjustValues.toMap())
        val currentPos = exoPlayer.currentPosition
        val wasPlaying = exoPlayer.isPlaying
        exoPlayer.pause()
        exoPlayer.setVideoEffects(allEffects)
        val uri = selectedVideoUri ?: RawResourceDataSource.buildRawResourceUri(R.raw.sample_video_1)
        val factory = DefaultDataSource.Factory(context)
        val source = ProgressiveMediaSource.Factory(factory).createMediaSource(MediaItem.fromUri(uri))
        exoPlayer.setMediaSource(source)
        exoPlayer.prepare()
        exoPlayer.seekTo(currentPos)
        exoPlayer.playWhenReady = wasPlaying
    }

    var textInput by remember { mutableStateOf("YOUR TEXT") }
    val fontStyles = listOf("Style 1", "Style 2", "Style 3", "Style 4", "Style 5", "Style 6")
    var selectedFontStyle by remember { mutableStateOf("Style 3") }
    val textColors = remember {
        listOf(
            Color.White, Color.Black,
            Color(0xFFFF0000), Color(0xFF00FF00), Color(0xFF0000FF),
            Color(0xFFFF00FF), Color(0xFFFFFF00), Color(0xFF00FFFF),
            Color(0xFFFF8800), Color(0xFF8800FF), Color(0xFF00FF88),
            Color(0xFFFF0088), Color(0xFF88FF00), Color(0xFF0088FF),
            Color(0xFFFF2E63), Color(0xFF2EE6A6), Color(0xFF2E5BFF),
            Color(0xFFE62ED5), Color(0xFFFF8C00), Color(0xFF00CED1),
            Color(0xFFADFF2F), Color(0xFFDC143C), Color(0xFF7B68EE),
            Color(0xFFFF69B4), Color(0xFF32CD32), Color(0xFFFF4500),
            Color(0xFF9400D3)
        )
    }
    var selectedTextColor by remember { mutableStateOf(Color.White) }
    var textOffsetX by remember { mutableFloatStateOf(0f) }
    var textOffsetY by remember { mutableFloatStateOf(0f) }
    var textScale by remember { mutableFloatStateOf(1f) }

    val stickerPacks = AppStickers.packNames
    var selectedPack by remember { mutableStateOf("Pack 1") }
    val stickerItems by remember(selectedPack) {
        derivedStateOf { AppStickers.forPack(selectedPack) }
    }
    var selectedStickerRes by remember { mutableIntStateOf(AppStickers.Pack1.first()) }
    var stickerOffsetX by remember { mutableFloatStateOf(0f) }
    var stickerOffsetY by remember { mutableFloatStateOf(0f) }
    var stickerScale by remember { mutableFloatStateOf(1f) }
    var stickerApplied by remember { mutableStateOf(false) }

    var showPublishSheet by remember { mutableStateOf(false) }
    var previewWidthPx by remember { mutableFloatStateOf(0f) }
    var previewHeightPx by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 12.dp).padding(bottom = 100.dp)
        ) {
            Spacer(Modifier.height(36.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("←", color = Color.White, fontSize = 26.sp, modifier = Modifier.clickable { onClose() })
                Spacer(Modifier.width(8.dp))
                Image(painterResource(R.drawable.ic_video_small), null, modifier = Modifier.size(20.dp), colorFilter = ColorFilter.tint(accentPink))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("VIDEO", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                    Text("E D I T O R", color = Color(0xFF9094AA), letterSpacing = 4.sp, fontSize = 12.sp)
                }
                Spacer(Modifier.weight(1f))
                Text("✕", color = Color.White, fontSize = 28.sp, modifier = Modifier.clickable { onClose() })
            }

            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF202433)))
            Spacer(Modifier.height(12.dp))

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth().height(240.dp)
                    .background(Color(0xFF12141E), RoundedCornerShape(22.dp))
                    .border(1.dp, Color(0xFF2B3043), RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp))
            ) {
                val boxW = with(LocalDensity.current) { maxWidth.toPx() }
                val boxH = with(LocalDensity.current) { maxHeight.toPx() }
                SideEffect { previewWidthPx = boxW; previewHeightPx = boxH }

                val targetAspect = when (selectedRatio) {
                    "1:1"  -> 1f / 1f
                    "5:4"  -> 5f / 4f
                    "9:16" -> 9f / 16f
                    "16:9" -> 16f / 9f
                    else   -> null
                }
                val previewModifier = if (targetAspect != null) {
                    val containerAspect = boxW / boxH
                    if (targetAspect < containerAspect) {
                        val newWDp = with(LocalDensity.current) { (boxH * targetAspect).toDp() }
                        Modifier.width(newWDp).fillMaxHeight().align(Alignment.Center)
                    } else {
                        val newHDp = with(LocalDensity.current) { (boxW / targetAspect).toDp() }
                        Modifier.fillMaxWidth().height(newHDp).align(Alignment.Center)
                    }
                } else Modifier.fillMaxSize()

                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            player = exoPlayer
                        }
                    },
                    modifier = previewModifier
                )

                val bVal = (adjustValues["brightness"] ?: 0f)
                val cVal = (adjustValues["contrast"] ?: 0f)
                val sVal = (adjustValues["saturation"] ?: 0f)
                val eVal = (adjustValues["exposure"] ?: 0f)
                if (bVal != 0f || cVal != 0f || sVal != 0f || eVal != 0f) {
                    val combinedBrightness = (bVal + eVal * 0.5f) / 100f
                    val overlayColor = when {
                        combinedBrightness > 0f -> Color.White.copy(alpha = (combinedBrightness * 0.4f).coerceIn(0f, 0.5f))
                        combinedBrightness < 0f -> Color.Black.copy(alpha = (-combinedBrightness * 0.5f).coerceIn(0f, 0.6f))
                        else -> Color.Transparent
                    }
                    val contrastAlpha = (kotlin.math.abs(cVal) / 100f * 0.3f).coerceIn(0f, 0.3f)
                    val satAlpha = (sVal / 100f * 0.15f).coerceIn(0f, 0.15f)

                    if (overlayColor != Color.Transparent)
                        Box(modifier = Modifier.matchParentSize().background(overlayColor))
                    if (contrastAlpha > 0f)
                        Box(modifier = Modifier.matchParentSize().background(
                            if (cVal > 0f) Color.Black.copy(alpha = contrastAlpha)
                            else Color.White.copy(alpha = contrastAlpha)))
                    if (satAlpha > 0f)
                        Box(modifier = Modifier.matchParentSize().background(
                            if (sVal > 0f) Color(0xFFFF6B9D).copy(alpha = satAlpha)
                            else Color(0xFF888888).copy(alpha = satAlpha)))
                }

                Box(
                    modifier = Modifier.padding(10.dp).size(24.dp).background(accentPink, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(painterResource(R.drawable.ic_tiktok), null, modifier = Modifier.size(12.dp), colorFilter = ColorFilter.tint(Color.White))
                }

                Image(
                    painterResource(R.drawable.ic_expand), null,
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(24.dp),
                    colorFilter = ColorFilter.tint(Color.White.copy(0.9f))
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.Center).size(64.dp)
                        .background(Color.Black.copy(0.6f), CircleShape)
                        .clickable {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (isPlaying) "⏸" else "▶", color = Color.White, fontSize = 22.sp)
                }

                if (textInput.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset { androidx.compose.ui.unit.IntOffset(textOffsetX.roundToInt(), textOffsetY.roundToInt()) }
                            .background(Color.Black.copy(0.35f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .pointerInput(boxW, boxH) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    textOffsetX = (textOffsetX + pan.x).coerceIn(-boxW * 0.35f, boxW * 0.35f)
                                    textOffsetY = (textOffsetY + pan.y).coerceIn(-boxH * 0.35f, boxH * 0.35f)
                                    textScale = (textScale * zoom).coerceIn(0.6f, 2.4f)
                                }
                            }
                    ) {
                        Text(textInput, style = textStyleFor(selectedFontStyle, selectedTextColor), fontSize = (18f * textScale).sp)
                    }
                }

                if (stickerOffsetX != 0f || stickerOffsetY != 0f || selectedTab == "Sticker") {
                    Image(
                        painterResource(selectedStickerRes), null,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset { androidx.compose.ui.unit.IntOffset(stickerOffsetX.roundToInt(), stickerOffsetY.roundToInt()) }
                            .size((100f * stickerScale).dp)
                            .pointerInput(boxW, boxH) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    stickerOffsetX = (stickerOffsetX + pan.x).coerceIn(-boxW * 0.38f, boxW * 0.38f)
                                    stickerOffsetY = (stickerOffsetY + pan.y).coerceIn(-boxH * 0.38f, boxH * 0.38f)
                                    stickerScale = (stickerScale * zoom).coerceIn(0.4f, 3f)
                                }
                            }
                    )
                }

                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val posMs = (playerProgress * durationMs).toLong()
                    Text(formatMs(posMs), color = Color(0xFFA3A8BD), fontSize = 10.sp)
                    Spacer(Modifier.width(6.dp))
                    Slider(
                        value = playerProgress,
                        onValueChange = { p ->
                            playerProgress = p
                            exoPlayer.seekTo((p * durationMs).toLong())
                            splitFraction = p
                        },
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(activeTrackColor = accentPink, inactiveTrackColor = Color(0xFF3A3E53), thumbColor = accentPink)
                    )
                    Spacer(Modifier.width(6.dp))
                    val remaining = durationMs - (playerProgress * durationMs).toLong()
                    Text("-${formatMs(remaining)}", color = Color(0xFFA3A8BD), fontSize = 10.sp)
                }
            }

            Spacer(Modifier.height(14.dp))

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (selectedTab) {

                    "Trimming" -> Column(Modifier.fillMaxWidth()) {
                        Text("SELECTED RANGE", color = Color(0xFF7D8198), fontSize = 11.sp, letterSpacing = 2.sp)
                        val selectedSec = splitFraction * durationMs / 1000.0
                        val mm = (selectedSec / 60).toInt()
                        val ss = selectedSec % 60
                        Text(
                            "%02d:%05.2f".format(mm, ss),
                            color = accentPink, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold
                        )

                        Spacer(Modifier.height(12.dp))

                        BoxWithConstraints(
                            modifier = Modifier.fillMaxWidth().height(88.dp)
                                .background(Color(0xFF11131D), RoundedCornerShape(18.dp))
                                .border(1.dp, Color(0xFF2B3043), RoundedCornerShape(18.dp))
                                .clip(RoundedCornerShape(18.dp))
                        ) {
                            val density = LocalDensity.current
                            val boxW = with(density) { maxWidth.toPx() }
                            val handleW = 28.dp
                            val handleWPx = with(density) { handleW.toPx() }
                            val minGap = 0.1f

                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                if (thumbFrames.isNotEmpty()) {
                                    thumbFrames.forEach { bmp ->
                                        Image(
                                            bitmap = bmp.asImageBitmap(), null,
                                            modifier = Modifier.weight(1f).fillMaxHeight(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                } else {
                                    repeat(6) {
                                        Box(
                                            Modifier.weight(1f).fillMaxHeight().background(Color(0xFF1D2130))
                                        )
                                    }
                                }
                            }

                            val leftX = with(density) { (leftFraction * boxW).toDp() }
                            val rightX = with(density) { (rightFraction * boxW).toDp() }
                            Box(Modifier.width(leftX).fillMaxHeight().background(Color.Black.copy(0.6f)))
                            Box(Modifier.width(maxWidth - rightX).fillMaxHeight().align(Alignment.TopEnd).background(Color.Black.copy(0.6f)))

                            val selW = with(density) { ((rightFraction - leftFraction) * boxW).toDp() }
                            Box(
                                modifier = Modifier.offset(x = leftX).width(selW).fillMaxHeight()
                                    .border(2.dp, accentPink, RoundedCornerShape(14.dp))
                            )

                            val needleX = with(density) { (splitFraction * boxW).toDp() }
                            Box(
                                modifier = Modifier.offset(x = needleX - 1.dp).width(2.dp).fillMaxHeight()
                                    .background(Color.White)
                            )
                            Box(
                                modifier = Modifier.offset(x = needleX - 6.dp, y = (-4).dp)
                                    .size(12.dp).background(Color.White, RoundedCornerShape(6.dp))
                            )

                            val leftHandleX = with(density) { ((leftFraction * boxW) - handleWPx / 2f).toDp() }
                            Box(
                                modifier = Modifier
                                    .offset(x = leftHandleX).width(handleW).fillMaxHeight()
                                    .background(accentPink, RoundedCornerShape(10.dp))
                                    .pointerInput(boxW) {
                                        detectDragGestures { c, d ->
                                            c.consume()
                                            leftFraction = (leftFraction + d.x / boxW).coerceIn(0f, rightFraction - minGap)
                                            splitFraction = splitFraction.coerceIn(leftFraction, rightFraction)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) { Text("||", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp) }

                            val rightHandleX = with(density) { ((rightFraction * boxW) - handleWPx / 2f).toDp() }
                            Box(
                                modifier = Modifier
                                    .offset(x = rightHandleX).width(handleW).fillMaxHeight()
                                    .background(accentPink, RoundedCornerShape(10.dp))
                                    .pointerInput(boxW) {
                                        detectDragGestures { c, d ->
                                            c.consume()
                                            rightFraction = (rightFraction + d.x / boxW).coerceIn(leftFraction + minGap, 1f)
                                            splitFraction = splitFraction.coerceIn(leftFraction, rightFraction)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) { Text("||", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ActionBtn(Modifier.weight(1f), "✂  Trim Segment", selectedAction == "Trim", accentPink) { selectedAction = "Trim" }
                            ActionBtn(Modifier.weight(1f), "⊞  Split Here", selectedAction == "Split", accentPink) { selectedAction = "Split" }
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CancelApplyRow(
                                canApply = selectedAction.isNotEmpty() && !isTrimming,
                                accentPink = accentPink,
                                onCancel = { selectedAction = "" },
                                onApply = {
                                    val uri = selectedVideoUri ?: return@CancelApplyRow
                                    isTrimming = true; trimError = null
                                    scope.launch {
                                        runCatching {
                                            val out = withTimeoutOrNull(120_000L) {
                                                if (selectedAction == "Trim") {
                                                    val startMs = (leftFraction * durationMs).toLong()
                                                    val endMs = (rightFraction * durationMs).toLong()
                                                    trimVideo(context, uri, startMs, endMs)
                                                } else {
                                                    val splitMs = (splitFraction * durationMs).toLong()
                                                    trimVideo(context, uri, 0L, splitMs)
                                                }
                                            }
                                            if (out != null) {
                                                isTrimming = false
                                                isSaving = true
                                                SavedVideoRepository.importVideo(context, out)
                                                isSaving = false
                                                onSaved()
                                            } else {
                                                trimError = "Trim timed out"
                                                isTrimming = false
                                            }
                                        }.onFailure { e ->
                                            android.util.Log.e("VideoTrim", "Trim failed", e)
                                            trimError = e.message ?: "Trim failed"
                                            isTrimming = false
                                        }
                                    }
                                }
                            )
                        }
                    }

                    "Size" -> Column(Modifier.fillMaxWidth()) {
                        Text("ASPECT RATIO", color = Color(0xFF7D8198), fontSize = 11.sp, letterSpacing = 2.sp)
                        Spacer(Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AspectRatioCard(Modifier.weight(1f), "Original", selectedRatio == "Original", videoUri = selectedVideoUri) { selectedRatio = "Original" }
                            AspectRatioCard(Modifier.weight(1f), "1:1", selectedRatio == "1:1") { selectedRatio = "1:1" }
                            AspectRatioCard(Modifier.weight(1f), "5:4", selectedRatio == "5:4") { selectedRatio = "5:4" }
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AspectRatioCard(Modifier.weight(1f), "9:16", selectedRatio == "9:16") { selectedRatio = "9:16" }
                            AspectRatioCard(Modifier.weight(2f), "16:9", selectedRatio == "16:9") { selectedRatio = "16:9" }
                        }

                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CancelApplyRow(canApply = true, accentPink = accentPink,
                                onCancel = { selectedRatio = "Original" },
                                onApply = { selectedTab = "Trimming" }
                            )
                        }
                    }

                    "Adjust" -> Column(Modifier.fillMaxWidth()) {
                        Text("ADJUST", color = Color(0xFF7D8198), fontSize = 11.sp, letterSpacing = 2.sp)
                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            adjustTabs.forEach { tab ->
                                Text(
                                    tab,
                                    color = if (selectedAdjust == tab) accentPink else Color(0xFF6F738A),
                                    fontWeight = if (selectedAdjust == tab) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    modifier = Modifier.clickable { selectedAdjust = tab }
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        AdjustRuler(
                            value = adjustValues[selectedAdjust] ?: 0f,
                            onValueChange = { adjustValues[selectedAdjust] = it },
                            accentPink = accentPink
                        )

                        Spacer(Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CancelApplyRow(canApply = true, accentPink = accentPink,
                                onCancel = { adjustValues[selectedAdjust] = 0f },
                                onApply = { selectedTab = "Trimming" }
                            )
                        }
                    }

                    "Text" -> Column(
                        Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    ) {
                        Text("ADD TEXT", color = Color(0xFF7D8198), fontSize = 11.sp, letterSpacing = 2.sp)
                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2D3348),
                                unfocusedBorderColor = Color(0xFF2D3348),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = accentPink
                            )
                        )

                        Spacer(Modifier.height(14.dp))

                        Text("FONT", color = Color(0xFF7D8198), fontSize = 10.sp, letterSpacing = 1.5.sp)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(fontStyles) { style ->
                                Text(
                                    style,
                                    color = if (selectedFontStyle == style) accentPink else Color(0xFF8A8FA6),
                                    fontWeight = if (selectedFontStyle == style) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp,
                                    modifier = Modifier
                                        .background(
                                            if (selectedFontStyle == style) Color(0xFF2A0D16) else Color(0xFF141722),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (selectedFontStyle == style) accentPink else Color(0xFF2A2E3F),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedFontStyle = style }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Text("COLOR", color = Color(0xFF7D8198), fontSize = 10.sp, letterSpacing = 1.5.sp)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(textColors) { c ->
                                Box(
                                    modifier = Modifier
                                        .size(if (selectedTextColor == c) 34.dp else 28.dp)
                                        .background(c, CircleShape)
                                        .border(
                                            if (selectedTextColor == c) 2.dp else 0.dp,
                                            Color.White, CircleShape
                                        )
                                        .clickable { selectedTextColor = c }
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Text("SIZE", color = Color(0xFF7D8198), fontSize = 10.sp, letterSpacing = 1.5.sp)
                        Slider(
                            value = textScale,
                            onValueChange = { textScale = it.coerceIn(0.6f, 2.4f) },
                            valueRange = 0.6f..2.4f,
                            colors = SliderDefaults.colors(
                                activeTrackColor = accentPink,
                                inactiveTrackColor = Color(0xFF2B3043),
                                thumbColor = accentPink
                            )
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CancelApplyRow(canApply = textInput.isNotBlank(), accentPink = accentPink,
                                onCancel = { textInput = ""; textOffsetX = 0f; textOffsetY = 0f; textScale = 1f },
                                onApply = { selectedTab = "Trimming" }
                            )
                        }
                    }

                    "Sticker" -> Column(
                        Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    ) {
                        Text("ADD STICKERS", color = Color(0xFF7D8198), fontSize = 10.sp, letterSpacing = 1.5.sp)
                        Spacer(Modifier.height(8.dp))

                        Text("STICKER PACKS", color = Color(0xFF7D8198), fontSize = 10.sp, letterSpacing = 1.5.sp)
                        Spacer(Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(stickerPacks) { pack ->
                                Text(
                                    pack,
                                    color = if (selectedPack == pack) accentPink else Color(0xFF6F738A),
                                    fontWeight = if (selectedPack == pack) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.clickable { selectedPack = pack }
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().height(200.dp)
                        ) {
                            items(stickerItems.size) { i ->
                                val res = stickerItems[i]
                                Box(
                                    modifier = Modifier
                                        .height(72.dp)
                                        .background(Color(0xFF0F111A), RoundedCornerShape(12.dp))
                                        .border(
                                            1.dp,
                                            if (selectedStickerRes == res && i == stickerItems.indexOf(res)) accentPink else Color.Transparent,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedStickerRes = res; stickerOffsetX = 0f; stickerOffsetY = 0f },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(painterResource(res), null, modifier = Modifier.size(52.dp))
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Text("SIZE", color = Color(0xFF7D8198), fontSize = 10.sp, letterSpacing = 1.5.sp)
                        Slider(
                            value = stickerScale,
                            onValueChange = { stickerScale = it.coerceIn(0.4f, 3f) },
                            valueRange = 0.4f..3f,
                            colors = SliderDefaults.colors(
                                activeTrackColor = accentPink,
                                inactiveTrackColor = Color(0xFF2B3043),
                                thumbColor = accentPink
                            )
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CancelApplyRow(canApply = true, accentPink = accentPink,
                                onCancel = { stickerOffsetX = 0f; stickerOffsetY = 0f; stickerScale = 1f },
                                onApply = { stickerApplied = true; selectedTab = "Trimming" }
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f).height(46.dp)
                        .background(Color(0xFF141722), RoundedCornerShape(23.dp))
                        .border(1.dp, Color(0xFF2A2E3F), RoundedCornerShape(23.dp))
                        .clickable {
                            val uri = selectedVideoUri ?: return@clickable
                            isSaving = true; trimError = null
                            scope.launch {
                                runCatching {
                                    val allEffects = buildSizeEffects(selectedRatio) +
                                            buildEffects(adjustValues.toMap()) +
                                            (if (textInput.isNotBlank()) buildTextEffects(textInput, selectedFontStyle,
                                                selectedTextColor, textOffsetX, textOffsetY, textScale,
                                                previewWidthPx, previewHeightPx) else emptyList()) +
                                            (if (stickerApplied)
                                                buildStickerEffects(context, selectedStickerRes, stickerOffsetX,
                                                    stickerOffsetY, stickerScale, previewWidthPx, previewHeightPx,
                                                    context.resources.displayMetrics.density)
                                            else emptyList())
                                    val out = withTimeoutOrNull(120_000L) {
                                        if (allEffects.isEmpty()) {
                                            SavedVideoRepository.importVideo(context, uri)
                                            onSaved()
                                            return@withTimeoutOrNull null
                                        }
                                        exportEditedVideo(context, uri, allEffects)
                                    }
                                    if (out != null) { SavedVideoRepository.importVideo(context, out); onSaved() }
                                }.onFailure { e -> trimError = e.message ?: "Save failed" }
                                isSaving = false
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("⬇  Save Video", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Box(
                    modifier = Modifier
                        .weight(1f).height(46.dp)
                        .background(Color(0xFF350817), RoundedCornerShape(23.dp))
                        .border(1.dp, accentPink, RoundedCornerShape(23.dp))
                        .clickable { showPublishSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.ic_tiktok), null,
                            modifier = Modifier.size(16.dp), colorFilter = ColorFilter.tint(accentPink))
                        Spacer(Modifier.width(6.dp))
                        Text("Post to TikTok", color = accentPink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().height(80.dp)
                    .background(Color.Black)
                    .border(1.dp, Color(0xFF1F2230), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EditorTabItem("Trimming", R.drawable.ic_trim_tool, selectedTab == "Trimming", accentPink) { selectedTab = "Trimming" }
                EditorTabItemDot("Size", R.drawable.ic_size_tool, selectedTab == "Size", accentPink, selectedRatio != "Original") { selectedTab = "Size" }
                EditorTabItemDot("Adjust", R.drawable.ic_rec_fx, selectedTab == "Adjust", accentPink,
                    adjustValues.values.any { it != 0f }) { selectedTab = "Adjust" }
                EditorTabItemDot("Text", R.drawable.ic_text_tool, selectedTab == "Text", accentPink, textInput.isNotBlank()) { selectedTab = "Text" }
                EditorTabItemDot("Sticker", R.drawable.ic_sticker_tool, selectedTab == "Sticker", accentPink,
                    stickerOffsetX != 0f || stickerOffsetY != 0f || stickerScale != 1f) { selectedTab = "Sticker" }
            }
        }

        if (isTrimming || isSaving) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(Color(0xFF12141E), RoundedCornerShape(20.dp))
                        .padding(32.dp)
                ) {
                    CircularProgressIndicator(
                        color = accentPink,
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (isSaving) "Saving video…" else "Trimming video…",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (isSaving) "Generating thumbnail" else "Please wait",
                        color = Color(0xFF8A8FA6),
                        fontSize = 13.sp
                    )
                }
            }
        }

        if (trimError != null) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(0.7f)).clickable { trimError = null },
                Alignment.Center
            ) { Text("Error: $trimError", color = accentPink) }
        }

        if (showPublishSheet) {
            PublishSheet(
                onDismiss = { showPublishSheet = false },
                onPublish = { caption, privacy ->
                    showPublishSheet = false
                    val uri = selectedVideoUri
                    if (uri != null) {
                        publishVm.publishFromUri(uri, caption, privacy)
                    } else {
                        publishVm.publish(ByteArray(0), "video.mp4", caption, privacy)
                    }
                },
                isBusy = publishState.stage == PublishStage.Uploading || publishState.stage == PublishStage.Publishing
            )
        }

        if (publishState.stage != PublishStage.Idle) {
            PublishStatusOverlay(state = publishState, onClose = { publishVm.reset() })
        }
    }
}


@Composable
private fun EditorTabItem(label: String, iconRes: Int, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0xFF2A0D16) else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painterResource(iconRes), null,
            modifier = Modifier.size(24.dp),
            colorFilter = ColorFilter.tint(if (selected) accent else Color(0xFF6F738A))
        )
        Spacer(Modifier.height(4.dp))
        Text(label, color = if (selected) accent else Color(0xFF6F738A), fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun EditorTabItemDot(label: String, iconRes: Int, selected: Boolean, accent: Color, hasEdit: Boolean, onClick: () -> Unit) {
    Box {
        Column(
            modifier = Modifier
                .width(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) Color(0xFF2A0D16) else Color.Transparent)
                .clickable { onClick() }
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painterResource(iconRes), null,
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(if (selected) accent else Color(0xFF6F738A))
            )
            Spacer(Modifier.height(4.dp))
            Text(label, color = if (selected) accent else Color(0xFF6F738A), fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        }
        if (hasEdit) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 6.dp)
                    .size(8.dp)
                    .background(Color(0xFF2EE6A6), CircleShape)
            )
        }
    }
}

@Composable
private fun ActionBtn(modifier: Modifier, text: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Box(
        modifier = modifier.height(50.dp)
            .background(if (selected) accent else Color(0xFF141722), RoundedCornerShape(14.dp))
            .border(1.dp, if (selected) accent else Color(0xFF2A2E3F), RoundedCornerShape(14.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
}

@Composable
private fun RowScope.CancelApplyRow(canApply: Boolean, accentPink: Color, onCancel: () -> Unit, onApply: () -> Unit) {
    Box(
        modifier = Modifier.weight(1f).height(54.dp)
            .background(Color(0xFF141722), RoundedCornerShape(14.dp))
            .clickable { onCancel() },
        contentAlignment = Alignment.Center
    ) { Text("✕  Cancel", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }

    Box(
        modifier = Modifier.weight(1f).height(54.dp)
            .background(if (canApply) accentPink else Color(0xFF4A1627), RoundedCornerShape(14.dp))
            .clickable(enabled = canApply) { onApply() },
        contentAlignment = Alignment.Center
    ) { Text("✓  Apply Changes", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun AspectRatioCard(
    modifier: Modifier,
    label: String,
    selected: Boolean,
    videoUri: Uri? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier.height(90.dp)
            .background(if (selected) Color(0xFF2A0D16) else Color(0xFF141722), RoundedCornerShape(14.dp))
            .border(2.dp, if (selected) Color(0xFFFF2E63) else Color(0xFF2A2E3F), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .clip(RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (label == "Original" && videoUri != null) {
            AsyncImage(model = videoUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val (w, h) = when (label) {
                "1:1" -> 36f to 36f; "5:4" -> 40f to 32f; "9:16" -> 22f to 38f; "16:9" -> 44f to 26f
                else -> 36f to 26f
            }
            Box(
                Modifier.size(w.dp, h.dp)
                    .border(2.dp, if (selected) Color(0xFFFF2E63) else Color.White.copy(0.7f), RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.height(6.dp))
            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun AdjustRuler(value: Float, onValueChange: (Float) -> Unit, accentPink: Color) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text("-100", color = Color(0xFF6F738A), fontSize = 11.sp)
            Box(
                modifier = Modifier.size(36.dp).background(Color(0xFF1A1D29), CircleShape)
                    .border(1.dp, accentPink, CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("%.0f".format(value), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            Text("100", color = Color(0xFF6F738A), fontSize = 11.sp)
        }

        Spacer(Modifier.height(8.dp))

        androidx.compose.foundation.Canvas(
            modifier = Modifier.fillMaxWidth().height(40.dp)
        ) {
            val w = size.width
            val centerY = size.height / 2f
            val normalizedPos = (value + 100f) / 200f

            for (i in -100..100 step 5) {
                val x = ((i + 100f) / 200f) * w
                val isBig = i % 25 == 0
                val tickH = if (isBig) 20f else 10f
                drawLine(
                    color = if (i == 0) Color.White else Color(0xFF3A3E53),
                    start = GOffset(x, centerY - tickH / 2f),
                    end = GOffset(x, centerY + tickH / 2f),
                    strokeWidth = if (isBig) 2f else 1f
                )
            }

            val posX = normalizedPos * w
            drawLine(Color(0xFFFF2E63), GOffset(posX, 0f), GOffset(posX, size.height), 3f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }

        Slider(
            value = value,
            onValueChange = { onValueChange(it.coerceIn(-100f, 100f)) },
            valueRange = -100f..100f,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                activeTrackColor = accentPink.copy(alpha = 0.5f),
                inactiveTrackColor = Color(0xFF2B3043),
                thumbColor = accentPink
            )
        )
    }
}

private fun textStyleFor(style: String, color: Color): TextStyle = when (style) {
    "Style 1" -> TextStyle(color = color, fontWeight = FontWeight.Normal, fontSize = 18.sp)
    "Style 2" -> TextStyle(color = color, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    "Style 3" -> TextStyle(color = color, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
    "Style 4" -> TextStyle(color = color, fontWeight = FontWeight.Light, fontStyle = FontStyle.Italic, fontSize = 18.sp)
    "Style 5" -> TextStyle(color = color, fontWeight = FontWeight.Black, fontSize = 18.sp)
    "Style 6" -> TextStyle(color = color, fontWeight = FontWeight.Medium, fontStyle = FontStyle.Italic, fontSize = 18.sp)
    else -> TextStyle(color = color, fontSize = 18.sp)
}

private fun formatMs(ms: Long): String {
    val s = ms / 1000
    return "%02d:%02d".format(s / 60, s % 60)
}


private suspend fun trimVideo(
    context: android.content.Context,
    sourceUri: Uri,
    startMs: Long,
    endMs: Long
): Uri = withContext(Dispatchers.IO) {
    val outputFile = File(context.cacheDir, "trimmed_${System.currentTimeMillis()}.mp4")
    val extractor = android.media.MediaExtractor()
    var muxer: android.media.MediaMuxer? = null

    try {
        val fd = context.contentResolver.openFileDescriptor(sourceUri, "r")
        if (fd != null) {
            extractor.setDataSource(fd.fileDescriptor)
            fd.close()
        } else {
            extractor.setDataSource(context, sourceUri, null)
        }

        val trackCount = extractor.trackCount
        val muxerTrackMap = mutableMapOf<Int, Int>()

        muxer = android.media.MediaMuxer(outputFile.absolutePath, android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        for (i in 0 until trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(android.media.MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                val muxerTrack = muxer.addTrack(format)
                muxerTrackMap[i] = muxerTrack
            }
        }

        muxer.start()

        val startUs = startMs * 1000L
        val endUs = endMs * 1000L
        val bufferSize = 1024 * 1024
        val buffer = java.nio.ByteBuffer.allocate(bufferSize)
        val bufferInfo = android.media.MediaCodec.BufferInfo()

        for ((extractorTrack, muxerTrack) in muxerTrackMap) {
            extractor.selectTrack(extractorTrack)
            extractor.seekTo(startUs, android.media.MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break

                val sampleTime = extractor.sampleTime
                if (sampleTime > endUs) break
                if (sampleTime < startUs) { extractor.advance(); continue }

                bufferInfo.presentationTimeUs = sampleTime - startUs
                bufferInfo.flags = extractor.sampleFlags
                muxer.writeSampleData(muxerTrack, buffer, bufferInfo)
                extractor.advance()
            }
            extractor.unselectTrack(extractorTrack)
        }

        muxer.stop()
        android.util.Log.d("VideoTrim", "Trim done: ${outputFile.absolutePath}")
        Uri.fromFile(outputFile)

    } finally {
        try { muxer?.release() } catch (e: Exception) {}
        extractor.release()
    }
}




@OptIn(UnstableApi::class)
private fun buildSizeEffects(ratio: String): List<androidx.media3.common.Effect> = when (ratio) {
    "1:1"  -> listOf(Presentation.createForAspectRatio(1f,      Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP))
    "5:4"  -> listOf(Presentation.createForAspectRatio(5f/4f,   Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP))
    "9:16" -> listOf(Presentation.createForAspectRatio(9f/16f,  Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP))
    "16:9" -> listOf(Presentation.createForAspectRatio(16f/9f,  Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP))
    else   -> emptyList()
}

@OptIn(UnstableApi::class)
private fun buildTextEffects(
    text: String, style: String, color: Color,
    offsetX: Float, offsetY: Float, scale: Float,
    previewW: Float, previewH: Float
): List<androidx.media3.common.Effect> {
    if (text.isBlank()) return emptyList()
    val spannable = android.text.SpannableString(text).apply {
        setSpan(android.text.style.ForegroundColorSpan(color.toArgb()), 0, length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val tf = when (style) {
            "Style 2", "Style 3", "Style 5" -> android.graphics.Typeface.BOLD
            "Style 4", "Style 6" -> android.graphics.Typeface.ITALIC
            else -> android.graphics.Typeface.NORMAL
        }
        setSpan(android.text.style.StyleSpan(tf), 0, length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        setSpan(android.text.style.AbsoluteSizeSpan((56f * scale).toInt()), 0, length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    val anchorX = if (previewW > 0f) (offsetX / (previewW / 2f)).coerceIn(-1f, 1f) else 0f
    val anchorY = if (previewH > 0f) -(offsetY / (previewH / 2f)).coerceIn(-1f, 1f) else 0f
    val settings = androidx.media3.effect.OverlaySettings.Builder()
        .setBackgroundFrameAnchor(anchorX, anchorY)
        .setOverlayFrameAnchor(0f, 0f)
        .setScale(scale, scale)
        .build()
    return listOf(androidx.media3.effect.OverlayEffect(
        listOf(androidx.media3.effect.TextOverlay.createStaticTextOverlay(spannable, settings))
    ))
}

@OptIn(UnstableApi::class)
private fun buildStickerEffects(
    context: android.content.Context,
    stickerRes: Int,
    offsetX: Float, offsetY: Float, scale: Float,
    previewW: Float, previewH: Float,
    density: Float = context.resources.displayMetrics.density
): List<androidx.media3.common.Effect> {
    val bitmap = android.graphics.BitmapFactory.decodeResource(context.resources, stickerRes)
        ?: return emptyList()
    val anchorX = if (previewW > 0f) (offsetX / (previewW / 2f)).coerceIn(-1f, 1f) else 0f
    val anchorY = if (previewH > 0f) -(offsetY / (previewH / 2f)).coerceIn(-1f, 1f) else 0f
    val stickerPx = 100f * density * scale
    val relativeScale = if (previewW > 0f) (stickerPx / previewW) else (0.15f * scale)
    val settings = androidx.media3.effect.OverlaySettings.Builder()
        .setBackgroundFrameAnchor(anchorX, anchorY)
        .setOverlayFrameAnchor(0f, 0f)
        .setScale(relativeScale, relativeScale)
        .build()
    return listOf(androidx.media3.effect.OverlayEffect(
        listOf(androidx.media3.effect.BitmapOverlay.createStaticBitmapOverlay(bitmap, settings))
    ))
}

private suspend fun exportEditedVideo(
    context: android.content.Context,
    sourceUri: android.net.Uri,
    videoEffects: List<androidx.media3.common.Effect>
): android.net.Uri = kotlin.coroutines.suspendCoroutine { continuation ->
    val outputFile = java.io.File(context.cacheDir, "edited_${System.currentTimeMillis()}.mp4")
    val editedItem = androidx.media3.transformer.EditedMediaItem.Builder(
        androidx.media3.common.MediaItem.fromUri(sourceUri)
    ).setEffects(androidx.media3.transformer.Effects(emptyList(), videoEffects)).build()

    val transformer = androidx.media3.transformer.Transformer.Builder(context)
        .addListener(object : androidx.media3.transformer.Transformer.Listener {
            override fun onCompleted(composition: androidx.media3.transformer.Composition,
                                     result: androidx.media3.transformer.ExportResult) {
                continuation.resumeWith(Result.success(android.net.Uri.fromFile(outputFile)))
            }
            override fun onError(composition: androidx.media3.transformer.Composition,
                                 result: androidx.media3.transformer.ExportResult,
                                 ex: androidx.media3.transformer.ExportException) {
                continuation.resumeWith(Result.failure(ex))
            }
        }).build()
    transformer.start(editedItem, outputFile.absolutePath)
}

@OptIn(UnstableApi::class)
private fun buildEffects(adjustValues: Map<String, Float>): List<androidx.media3.common.Effect> {
    val effects = mutableListOf<androidx.media3.common.Effect>()
    val contrast = (adjustValues["contrast"] ?: 0f) / 100f
    if (contrast != 0f) effects += Contrast(contrast.coerceIn(-1f, 1f))
    val brightness = (adjustValues["brightness"] ?: 0f) / 100f
    if (brightness != 0f) effects += Brightness(brightness.coerceIn(-1f, 1f))
    val saturation = adjustValues["saturation"] ?: 0f
    if (saturation != 0f) effects += HslAdjustment.Builder()
        .adjustSaturation(saturation.coerceIn(-100f, 100f)).build()
    val exposure = (adjustValues["exposure"] ?: 0f) / 200f
    if (exposure != 0f) effects += Brightness(exposure.coerceIn(-1f, 1f))
    val vibrance = adjustValues["vibrance"] ?: 0f
    if (vibrance != 0f) {
        val boost = 1f + vibrance / 150f
        effects += RgbAdjustment.Builder()
            .setRedScale(boost.coerceAtLeast(0f))
            .setGreenScale(boost.coerceAtLeast(0f))
            .setBlueScale(boost.coerceAtLeast(0f))
            .build()
    }
    return effects
}