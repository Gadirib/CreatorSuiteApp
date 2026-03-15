package com.example.creatorsuiteapp.ui.screens.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.creatorsuiteapp.R
import com.example.creatorsuiteapp.ui.screens.rec.PublishSheet
import com.example.creatorsuiteapp.ui.screens.rec.PublishStatusOverlay
import com.example.creatorsuiteapp.ui.viewmodel.PublishStage
import com.example.creatorsuiteapp.ui.viewmodel.PublishViewModel
import com.example.creatorsuiteapp.data.media.MediaSelectionStore
import kotlin.math.roundToInt
import androidx.compose.runtime.rememberCoroutineScope
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.media3.common.MediaItem
import androidx.media3.common.Effect
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.RawResourceDataSource
import java.io.File
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.TransformationException
import androidx.media3.transformer.TransformationResult
import androidx.media3.transformer.Transformer
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.RgbAdjustment
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.launch
import android.net.Uri

@Composable
fun VideoTrimScreen(onClose: () -> Unit) {
    val accentPink = Color(0xFFFF2E63)
    val publishVm: PublishViewModel = viewModel()
    val publishState by publishVm.state.collectAsState()
    val context = LocalContext.current
    val selectedVideoUri by MediaSelectionStore.videoUri.collectAsState()
    val selectedAudioUri by MediaSelectionStore.audioUri.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedBottomTab by remember { mutableStateOf("Trimming") }
    var selectedAction by remember { mutableStateOf("Split") }

    val durationSec = 10f
    var leftFraction by remember { mutableFloatStateOf(0.22f) }
    var rightFraction by remember { mutableFloatStateOf(0.78f) }
    var splitFraction by remember { mutableFloatStateOf(0.50f) }

    val selectedSec = splitFraction * durationSec
    val canApply = selectedAction.isNotEmpty()

    val ratios = listOf("Original", "1:1", "5:4", "9:16", "16:9")
    var selectedRatio by remember { mutableStateOf("1:1") }

    val adjustTabs = listOf("contrast", "brightness", "saturation", "exposure", "vibrance")
    var selectedAdjust by remember { mutableStateOf("contrast") }
    val adjustValues = remember {
        mutableStateMapOf<String, Float>(
            "contrast" to 0f,
            "brightness" to 0f,
            "saturation" to 0f,
            "exposure" to 75f,
            "vibrance" to 0f
        )
    }

    var textInput by remember { mutableStateOf("YOUR TEXT") }
    val fontStyles = listOf("Style 1", "Style 2", "Style 3", "Style 4", "Style 5", "Style 6")
    var selectedFontStyle by remember { mutableStateOf("Style 3") }
    val textColors = listOf(
        Color.White, Color.Black, Color(0xFFFF2E63), Color(0xFF2EE6A6),
        Color(0xFF2E5BFF), Color(0xFFE62ED5), Color(0xFFFF315F),
        Color(0xFF3BE29F), Color(0xFF3E63E8), Color(0xFFE93DD8)
    )
    var selectedTextColor by remember { mutableStateOf(Color.White) }

    val fontRowState = rememberLazyListState()
    val colorRowState = rememberLazyListState()
    val textScrollState = rememberScrollState()

    var textStartSec by remember { mutableFloatStateOf(0f) }
    var textDurationSec by remember { mutableFloatStateOf(2.5f) }
    val textEndSec = textStartSec + textDurationSec
    val showTextAtCurrentMoment = selectedSec in textStartSec..textEndSec

    var textOffsetX by remember { mutableFloatStateOf(0f) }
    var textOffsetY by remember { mutableFloatStateOf(0f) }

    val stickerPacks = listOf("Pack 1", "Pack 2", "Pack 3", "Pack 4", "Pack 5")
    var selectedPack by remember { mutableStateOf("Pack 3") }

    val stickerItems = listOf(
        R.drawable.sticker_1, R.drawable.sticker_2, R.drawable.sticker_3,
        R.drawable.sticker_4, R.drawable.sticker_5, R.drawable.sticker_6
    )
    var selectedStickerRes by remember { mutableIntStateOf(R.drawable.sticker_1) }

    var stickerStartSec by remember { mutableFloatStateOf(0f) }
    var stickerDurationSec by remember { mutableFloatStateOf(2.5f) }
    val stickerEndSec = stickerStartSec + stickerDurationSec
    val showStickerAtCurrentMoment = selectedSec in stickerStartSec..stickerEndSec

    var stickerOffsetX by remember { mutableFloatStateOf(0f) }
    var stickerOffsetY by remember { mutableFloatStateOf(0f) }

    val packRowState = rememberLazyListState()
    var showPublishSheet by remember { mutableStateOf(false) }
    var isTrimming by remember { mutableStateOf(false) }
    var trimError by remember { mutableStateOf<String?>(null) }

    val audioPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) MediaSelectionStore.setAudio(uri)
    }

    @OptIn(UnstableApi::class)
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    LaunchedEffect(selectedVideoUri, selectedAudioUri) {
        val videoUri = selectedVideoUri
            ?: RawResourceDataSource.buildRawResourceUri(R.raw.sample_video_1)
        val dataSourceFactory = DefaultDataSource.Factory(context)
        val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(videoUri))
        val mediaSource = if (selectedAudioUri != null) {
            val audioSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(selectedAudioUri!!))
            MergingMediaSource(videoSource, audioSource)
        } else {
            videoSource
        }
        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = false
    }

    LaunchedEffect(
        adjustValues["contrast"],
        adjustValues["brightness"],
        adjustValues["saturation"],
        adjustValues["exposure"],
        adjustValues["vibrance"]
    ) {
        exoPlayer.setVideoEffects(buildExportEffects(adjustValues.toMap()))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .padding(bottom = 176.dp)
        ) {
        Spacer(Modifier.height(36.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "←",
                color = Color.White,
                fontSize = 26.sp,
                modifier = Modifier.clickable { onClose() }
            )
            Spacer(Modifier.width(8.dp))
            Image(
                painter = painterResource(R.drawable.ic_video_small),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                colorFilter = ColorFilter.tint(accentPink)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text("VIDEO", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                Text("E D I T O R", color = Color(0xFF9094AA), letterSpacing = 4.sp, fontSize = 12.sp)
            }
            Spacer(Modifier.weight(1f))
            Text("✕", color = Color.White, fontSize = 28.sp, modifier = Modifier.clickable { onClose() })
        }

        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF202433))
        )
        Spacer(Modifier.height(12.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(Color(0xFF12141E), RoundedCornerShape(22.dp))
                .border(1.dp, Color(0xFF2B3043), RoundedCornerShape(22.dp))
                .clip(RoundedCornerShape(22.dp))
        ) {
            val density = LocalDensity.current
            val boxW = with(density) { maxWidth.toPx() }
            val boxH = with(density) { maxHeight.toPx() }

            AndroidView(
                factory = {
                    PlayerView(it).apply {
                        useController = false
                        player = exoPlayer
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .padding(10.dp)
                    .size(24.dp)
                    .background(accentPink, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_tiktok),
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(72.dp)
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(36.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_video_small),
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    colorFilter = ColorFilter.tint(accentPink)
                )
            }

            Image(
                painter = painterResource(R.drawable.ic_expand),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(24.dp),
                colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.9f))
            )

            if (selectedBottomTab == "Text" && showTextAtCurrentMoment) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset { IntOffset(textOffsetX.roundToInt(), textOffsetY.roundToInt()) }
                        .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .pointerInput(textInput) {
                            detectDragGestures { change, dragAmount ->
                                change.consumeAllChanges()
                                textOffsetX = (textOffsetX + dragAmount.x).coerceIn(-boxW * 0.35f, boxW * 0.35f)
                                textOffsetY = (textOffsetY + dragAmount.y).coerceIn(-boxH * 0.35f, boxH * 0.35f)
                            }
                        }
                ) {
                    Text(text = textInput, style = textStyleFor(selectedFontStyle, selectedTextColor))
                }
            }

            if (selectedBottomTab == "Sticker" && showStickerAtCurrentMoment) {
                Image(
                    painter = painterResource(selectedStickerRes),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset { IntOffset(stickerOffsetX.roundToInt(), stickerOffsetY.roundToInt()) }
                        .size(110.dp)
                        .pointerInput(selectedStickerRes) {
                            detectDragGestures { change, dragAmount ->
                                change.consumeAllChanges()
                                stickerOffsetX = (stickerOffsetX + dragAmount.x).coerceIn(-boxW * 0.38f, boxW * 0.38f)
                                stickerOffsetY = (stickerOffsetY + dragAmount.y).coerceIn(-boxH * 0.38f, boxH * 0.38f)
                            }
                        }
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("00:00", color = Color(0xFFA3A8BD), fontSize = 10.sp)
                Spacer(Modifier.width(8.dp))
                Slider(
                    value = splitFraction,
                    onValueChange = { splitFraction = it.coerceIn(leftFraction, rightFraction) },
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        activeTrackColor = accentPink,
                        inactiveTrackColor = Color(0xFF3A3E53),
                        thumbColor = accentPink
                    )
                )
                Spacer(Modifier.width(8.dp))
                Text("-00:00", color = Color(0xFFA3A8BD), fontSize = 10.sp)
            }
        }

        Spacer(Modifier.height(14.dp))

        when (selectedBottomTab) {
            "Trimming" -> {
                Text("SELECTED RANGE", color = Color(0xFF7D8198), fontSize = 11.sp, letterSpacing = 2.sp)
                Text("00:${"%05.2f".format(selectedSec)}", color = accentPink, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(14.dp))

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(92.dp)
                        .background(Color(0xFF11131D), RoundedCornerShape(18.dp))
                        .border(1.dp, Color(0xFF2B3043), RoundedCornerShape(18.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    val density = LocalDensity.current
                    val boxWidthPx = with(density) { maxWidth.toPx() }
                    val handleWidthDp = 26.dp
                    val handleWidthPx = with(density) { handleWidthDp.toPx() }
                    val minGap = 0.14f

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(6) {
                            Image(
                                painter = painterResource(R.drawable.sample_video_1_thumb),
                                contentDescription = null,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    val selectionX = (leftFraction * boxWidthPx).coerceIn(0f, boxWidthPx)
                    val selectionW = ((rightFraction - leftFraction) * boxWidthPx).coerceAtLeast(handleWidthPx)
                    val selectionXDp = with(density) { selectionX.toDp() }
                    val selectionWDp = with(density) { selectionW.toDp() }

                    Box(
                        modifier = Modifier
                            .offset(x = selectionXDp)
                            .width(selectionWDp)
                            .fillMaxHeight()
                            .border(2.dp, accentPink, RoundedCornerShape(18.dp))
                    )

                    val leftHandleX = (selectionX - handleWidthPx / 2f)
                        .coerceIn(-handleWidthPx / 2f, boxWidthPx - handleWidthPx / 2f)
                    val rightHandleX = (selectionX + selectionW - handleWidthPx / 2f)
                        .coerceIn(-handleWidthPx / 2f, boxWidthPx - handleWidthPx / 2f)

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(leftHandleX.roundToInt(), 0) }
                            .width(handleWidthDp)
                            .fillMaxHeight()
                            .background(accentPink, RoundedCornerShape(14.dp))
                            .pointerInput(boxWidthPx, rightFraction) {
                                detectDragGestures { change, dragAmount ->
                                    change.consumeAllChanges()
                                    val df = dragAmount.x / boxWidthPx
                                    leftFraction = (leftFraction + df).coerceIn(0f, rightFraction - minGap)
                                    splitFraction = splitFraction.coerceIn(leftFraction, rightFraction)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) { Text("||", color = Color.White, fontWeight = FontWeight.Bold) }

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(rightHandleX.roundToInt(), 0) }
                            .width(handleWidthDp)
                            .fillMaxHeight()
                            .background(accentPink, RoundedCornerShape(14.dp))
                            .pointerInput(boxWidthPx, leftFraction) {
                                detectDragGestures { change, dragAmount ->
                                    change.consumeAllChanges()
                                    val df = dragAmount.x / boxWidthPx
                                    rightFraction = (rightFraction + df).coerceIn(leftFraction + minGap, 1f)
                                    splitFraction = splitFraction.coerceIn(leftFraction, rightFraction)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) { Text("||", color = Color.White, fontWeight = FontWeight.Bold) }
                }

                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TrimSplitBtn(
                        modifier = Modifier.weight(1f),
                        text = "Trim Segment",
                        iconRes = R.drawable.ic_trim_tool,
                        selected = selectedAction == "Trim",
                        selectedColor = accentPink,
                        onClick = { selectedAction = "Trim" }
                    )
                    TrimSplitBtn(
                        modifier = Modifier.weight(1f),
                        text = "Split Here",
                        iconRes = R.drawable.ic_split,
                        selected = selectedAction == "Split",
                        selectedColor = accentPink,
                        onClick = { selectedAction = "Split" }
                    )
                }

                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .background(Color(0xFF141722), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text("✕  Cancel", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .background(if (canApply) accentPink else Color(0xFF4A1627), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "✓  Apply Changes",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable(enabled = canApply && !isTrimming) {
                                val durationMs = exoPlayer.duration.takeIf { it > 0 } ?: 10_000L
                                val startMs = (leftFraction * durationMs).toLong()
                                val endMs = (rightFraction * durationMs).toLong()
                                val source = selectedVideoUri
                                if (source != null) {
                                    isTrimming = true
                                    trimError = null
                                    scope.launch {
                                        runCatching {
                                            val out = trimVideo(
                                                context = context,
                                                sourceUri = source,
                                                startMs = startMs,
                                                endMs = endMs,
                                                adjustValues = adjustValues.toMap()
                                            )
                                            MediaSelectionStore.setVideo(out)
                                        }.onFailure { e ->
                                            trimError = e.message ?: "Trim failed"
                                        }
                                        isTrimming = false
                                    }
                                }
                            }
                        )
                    }
                }
            }

            "Size" -> {
                Text("ASPECT RATIO", color = Color(0xFF7D8198), fontSize = 11.sp, letterSpacing = 2.sp)
                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AspectRatioCard(Modifier.weight(1f), "Original", selectedRatio == "Original") { selectedRatio = "Original" }
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
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .background(Color(0xFF141722), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text("✕  Cancel", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .background(accentPink, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text("✓  Apply Changes", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                }
            }

            "Adjust" -> {
                Text("ADJUST", color = Color(0xFF7D8198), fontSize = 11.sp, letterSpacing = 2.sp)
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .background(Color(0xFF141722)),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    adjustTabs.forEach { item ->
                        Text(
                            text = item,
                            color = if (selectedAdjust == item) accentPink else Color(0xFF6F738A),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { selectedAdjust = item }
                        )
                    }
                }

                Spacer(Modifier.height(26.dp))

                AdjustRuler(
                    value = adjustValues[selectedAdjust] ?: 0f,
                    onValueChange = { adjustValues[selectedAdjust] = it },
                    accentPink = accentPink
                )

                Spacer(Modifier.height(28.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .background(Color(0xFF141722), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text("✕  Cancel", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .background(accentPink, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text("✓  Apply Changes", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                }
            }

            "Text" -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(textScrollState)
                        .padding(bottom = 16.dp)
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

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .background(Color(0xFF141722), RoundedCornerShape(12.dp))
                                .clickable { textStartSec = selectedSec },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Set At ${"%.2f".format(selectedSec)}s", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .background(Color(0xFF141722), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Range ${"%.2f".format(textStartSec)} - ${"%.2f".format(textEndSec)}", color = Color(0xFF9BA1B8), fontSize = 12.sp)
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Text("Duration", color = Color(0xFF7D8198), fontSize = 11.sp, letterSpacing = 1.sp)
                    Slider(
                        value = textDurationSec,
                        onValueChange = { textDurationSec = it.coerceIn(0.5f, 8f) },
                        valueRange = 0.5f..8f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = accentPink,
                            inactiveTrackColor = Color(0xFF2B3043),
                            thumbColor = accentPink
                        )
                    )

                    Spacer(Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF141722))
                            .clipToBounds()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "FONT",
                            color = Color(0xFF7D8198),
                            fontSize = 10.sp,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        Spacer(Modifier.height(6.dp))

                        Box(Modifier.fillMaxWidth().clipToBounds()) {
                            LazyRow(
                                state = fontRowState,
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(fontStyles) { styleName ->
                                    TextFontChip(
                                        title = styleName,
                                        selected = selectedFontStyle == styleName,
                                        onClick = { selectedFontStyle = styleName },
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Text(
                            text = "COLOR",
                            color = Color(0xFF7D8198),
                            fontSize = 10.sp,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        Spacer(Modifier.height(6.dp))

                        Box(Modifier.fillMaxWidth().clipToBounds()) {
                            LazyRow(
                                state = colorRowState,
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(textColors) { c ->
                                    TextColorDot(
                                        color = c,
                                        selected = selectedTextColor == c,
                                        onClick = { selectedTextColor = c },
                                        size = 28.dp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .background(Color(0xFF141722), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) { Text("✕  Cancel", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .background(accentPink, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) { Text("✓  Apply Changes", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }

            "Sticker" -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 16.dp)
                ) {
                    Text("ADD STICKERS", color = Color(0xFF7D8198), fontSize = 10.sp, letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .background(Color(0xFF141722), RoundedCornerShape(10.dp))
                                .clickable { stickerStartSec = selectedSec },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Set At ${"%.2f".format(selectedSec)}s", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .background(Color(0xFF141722), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Range ${"%.2f".format(stickerStartSec)} - ${"%.2f".format(stickerEndSec)}", color = Color(0xFF9BA1B8), fontSize = 11.sp)
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Text("Duration", color = Color(0xFF7D8198), fontSize = 10.sp, letterSpacing = 1.sp)
                    Slider(
                        value = stickerDurationSec,
                        onValueChange = { stickerDurationSec = it.coerceIn(0.5f, 8f) },
                        valueRange = 0.5f..8f,
                        modifier = Modifier.height(24.dp),
                        colors = SliderDefaults.colors(
                            activeTrackColor = accentPink,
                            inactiveTrackColor = Color(0xFF2B3043),
                            thumbColor = accentPink
                        )
                    )

                    Spacer(Modifier.height(6.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF141722))
                            .clipToBounds()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "STICKER PACKS",
                            color = Color(0xFF7D8198),
                            fontSize = 10.sp,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        Spacer(Modifier.height(6.dp))

                        Box(Modifier.fillMaxWidth().clipToBounds()) {
                            LazyRow(
                                state = packRowState,
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(stickerPacks) { pack ->
                                    Text(
                                        text = pack,
                                        color = if (selectedPack == pack) accentPink else Color(0xFF6F738A),
                                        fontSize = 14.sp,
                                        fontWeight = if (selectedPack == pack) FontWeight.Bold else FontWeight.SemiBold,
                                        modifier = Modifier.clickable { selectedPack = pack }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .padding(horizontal = 12.dp)
                        ) {
                            items(stickerItems.size) { i ->
                                val res = stickerItems[i]
                                Box(
                                    modifier = Modifier
                                        .height(70.dp)
                                        .background(Color(0xFF0F111A), RoundedCornerShape(12.dp))
                                        .border(
                                            1.dp,
                                            if (selectedStickerRes == res) accentPink else Color.Transparent,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedStickerRes = res },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(res),
                                        contentDescription = null,
                                        modifier = Modifier.size(52.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .background(Color(0xFF141722), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) { Text("✕  Cancel", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .background(accentPink, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) { Text("✓  Apply Changes", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }

            "Audio" -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text("AUDIO", color = Color(0xFF7D8198), fontSize = 11.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .background(Color(0xFF141722), RoundedCornerShape(12.dp))
                            .clickable { audioPicker.launch("audio/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Pick Audio Track", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = if (selectedAudioUri != null) "Audio selected" else "No audio selected",
                        color = Color(0xFF8A8FA6),
                        fontSize = 12.sp
                    )

                    Spacer(Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .background(Color(0xFF1A1D29), RoundedCornerShape(12.dp))
                            .clickable { MediaSelectionStore.clearAudio() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Clear Audio", color = Color(0xFFFF2E63), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .background(Color(0xFF350817), RoundedCornerShape(24.dp))
                    .border(1.dp, Color(0xFFFF2E63), RoundedCornerShape(24.dp))
                    .clickable { showPublishSheet = true }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_tiktok),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    colorFilter = ColorFilter.tint(Color(0xFFFF2E63))
                )
                Spacer(Modifier.width(8.dp))
                Text("Post to TikTok", color = Color(0xFFFF2E63), fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(86.dp)
                    .background(Color.Black)
                    .border(1.dp, Color(0xFF1F2230), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomEditorItem("Trimming", R.drawable.ic_trim_tool, selectedBottomTab == "Trimming") { selectedBottomTab = "Trimming" }
                BottomEditorItem("Size", R.drawable.ic_size_tool, selectedBottomTab == "Size") { selectedBottomTab = "Size" }
                BottomEditorItem("Adjust", R.drawable.ic_rec_fx, selectedBottomTab == "Adjust") { selectedBottomTab = "Adjust" }
                BottomEditorItem("Text", R.drawable.ic_text_tool, selectedBottomTab == "Text") { selectedBottomTab = "Text" }
                BottomEditorItem("Sticker", R.drawable.ic_sticker_tool, selectedBottomTab == "Sticker") { selectedBottomTab = "Sticker" }
                BottomEditorItem("Audio", R.drawable.ic_rec_audio, selectedBottomTab == "Audio") { selectedBottomTab = "Audio" }
            }
        }

        if (showPublishSheet) {
            PublishSheet(
                onDismiss = { showPublishSheet = false },
                onPublish = { caption, privacy ->
                    showPublishSheet = false
                    val fakeBytes = ByteArray(1024) { 0 }
                    publishVm.publish(
                        fileBytes = fakeBytes,
                        fileName = "imported.mp4",
                        caption = caption,
                        privacy = privacy
                    )
                },
                isBusy = publishState.stage == PublishStage.Uploading ||
                    publishState.stage == PublishStage.Publishing
            )
        }

        if (publishState.stage != PublishStage.Idle) {
            PublishStatusOverlay(
                state = publishState,
                onClose = { publishVm.reset() }
            )
        }

        if (isTrimming) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Text("Trimming...", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (trimError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { trimError = null },
                contentAlignment = Alignment.Center
            ) {
                Text("Trim failed: $trimError", color = Color(0xFFFF2E63), fontSize = 14.sp)
            }
        }
    }
}

@OptIn(UnstableApi::class)
private suspend fun trimVideo(
    context: android.content.Context,
    sourceUri: Uri,
    startMs: Long,
    endMs: Long,
    adjustValues: Map<String, Float>
): Uri = suspendCancellableCoroutine { cont ->
    val outputFile = File(context.cacheDir, "trimmed_${System.currentTimeMillis()}.mp4")
    val mediaItem = MediaItem.Builder()
        .setUri(sourceUri)
        .setClippingConfiguration(
            MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(startMs)
                .setEndPositionMs(endMs)
                .build()
        )
        .build()
    val effectList = buildExportEffects(adjustValues)
    val edited = EditedMediaItem.Builder(mediaItem)
        .setEffects(Effects(emptyList(), effectList))
        .build()

    val transformer = Transformer.Builder(context).build()
    transformer.addListener(object : Transformer.Listener {
        override fun onTransformationCompleted(
            inputMediaItem: MediaItem,
            transformationResult: TransformationResult
        ) {
            if (!cont.isCompleted) cont.resume(Uri.fromFile(outputFile))
        }

        override fun onTransformationError(
            inputMediaItem: MediaItem,
            exception: TransformationException
        ) {
            if (!cont.isCompleted) cont.resumeWithException(exception)
        }
    })
    transformer.start(edited, outputFile.absolutePath)
}

@OptIn(UnstableApi::class)
private fun buildExportEffects(adjustValues: Map<String, Float>): List<Effect> {
    val effects = mutableListOf<Effect>()

    val contrastValue = (adjustValues["contrast"] ?: 0f) / 100f
    if (contrastValue != 0f) effects += Contrast(contrastValue.coerceIn(-1f, 1f))

    val brightnessValue = (adjustValues["brightness"] ?: 0f) / 100f
    if (brightnessValue != 0f) effects += Brightness(brightnessValue.coerceIn(-1f, 1f))

    val saturationValue = adjustValues["saturation"] ?: 0f
    if (saturationValue != 0f) {
        effects += HslAdjustment.Builder()
            .adjustSaturation(saturationValue.coerceIn(-100f, 100f))
            .build()
    }

    val exposureValue = (adjustValues["exposure"] ?: 0f) / 100f
    if (exposureValue != 0f) effects += Brightness((exposureValue * 0.5f).coerceIn(-1f, 1f))

    val vibranceValue = adjustValues["vibrance"] ?: 0f
    if (vibranceValue != 0f) {
        val scale = (1f + (vibranceValue / 220f)).coerceAtLeast(0f)
        effects += RgbAdjustment.Builder()
            .setRedScale(scale)
            .setGreenScale(scale)
            .setBlueScale(scale)
            .build()
    }

    return effects
}
