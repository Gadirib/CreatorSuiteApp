package com.example.tiktokclone.ui.screens.rec

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.provider.MediaStore
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tiktokclone.R
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tiktokclone.ui.viewmodel.PublishStage
import com.example.tiktokclone.ui.viewmodel.PublishViewModel
import kotlinx.coroutines.delay
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Camera
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.camera.view.PreviewView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.launch
import com.example.tiktokclone.data.media.MediaSelectionStore
import android.net.Uri
import com.example.tiktokclone.data.ServiceLocator

private enum class RecTool { PITCH, NOICE, EFFECTS, SOUNDS }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecScreen(onClose: () -> Unit) {
    val publishVm: PublishViewModel = viewModel()
    val publishState by publishVm.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var isRecording by remember { mutableStateOf(false) }
    var sec by remember { mutableStateOf(0) }
    var showSaveSheet by remember { mutableStateOf(false) }
    var showPublishSheet by remember { mutableStateOf(false) }
    var hasPermissions by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var latestRecordedUri by remember { mutableStateOf<Uri?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var torchEnabled by remember { mutableStateOf(false) }
    val previewView = remember { PreviewView(context) }
    val selectedAudioUri by MediaSelectionStore.audioUri.collectAsState()

    var selectedTool by remember { mutableStateOf(RecTool.PITCH) }
    var speed by remember { mutableStateOf("1x") }

    var vocalVolume by remember { mutableFloatStateOf(0.55f) }
    var delayValue by remember { mutableFloatStateOf(0.55f) }
    var pitchValue by remember { mutableFloatStateOf(0.80f) }

    var noiseOn by remember { mutableStateOf(true) }

    val effects = listOf("None", "Small Room", "Medium Room", "Cathedral", "Large Room", "Medium Hall", "Large Hall", "Medium Chamber")
    var selectedEffect by remember { mutableStateOf("Medium Hall") }
    var effectLevel by remember { mutableFloatStateOf(0.50f) }

    val sounds = listOf("None", "Zombie", "Echo", "Applause", "Pikachu", "Laughter", "Truck", "Train", "Siren", "Vuvuzela", "Moskito", "Deep voice", "Horn")
    var selectedSound by remember { mutableStateOf("Moskito") }
    var soundVolume by remember { mutableFloatStateOf(0.55f) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasPermissions = result.values.all { it }
    }
    val audioPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            MediaSelectionStore.setAudio(uri)
        }
    }

    LaunchedEffect(Unit) {
        val cam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val mic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
        hasPermissions = cam && mic
        if (!hasPermissions) {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    LaunchedEffect(hasPermissions, lensFacing) {
        if (!hasPermissions) return@LaunchedEffect
        val cameraProvider = context.getCameraProvider()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
            .build()
        val capture = VideoCapture.withOutput(recorder)
        cameraProvider.unbindAll()
        camera = cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.Builder().requireLensFacing(lensFacing).build(),
            preview,
            capture
        )
        videoCapture = capture
        torchEnabled = false
    }

    LaunchedEffect(camera, torchEnabled) {
        camera?.cameraControl?.enableTorch(torchEnabled)
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                delay(1000)
                sec++
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        if (hasPermissions) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
            val effectOverlay = effectOverlayColor(selectedEffect, effectLevel)
            if (effectOverlay != Color.Transparent) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(effectOverlay)
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 52.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("✕", color = Color.White, fontSize = 24.sp, modifier = Modifier.clickable { onClose() })

            Box(
                modifier = Modifier
                    .background(Color(0xFFFF2E63), RoundedCornerShape(99.dp))
                    .padding(horizontal = 16.dp, vertical = 7.dp)
            ) {
                Text("• ${"%02d:%02d".format(sec / 60, sec % 60)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (torchEnabled) "⚡" else "◌",
                    color = if (torchEnabled) Color(0xFFFFC94D) else Color.White,
                    fontSize = 24.sp,
                    modifier = Modifier.clickable {
                        if (!isRecording) torchEnabled = !torchEnabled
                    }
                )
                Text(
                    "⟳",
                    color = Color.White,
                    fontSize = 28.sp,
                    modifier = Modifier.clickable {
                        if (!isRecording) {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        }
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 170.dp)
                .padding(end = 118.dp)
        ) {
            when (selectedTool) {
                RecTool.PITCH -> {
                    PanelCard(title = "PITCH") {
                        SliderRow("VOCAL VOLUME", vocalVolume, minValue = 0, maxValue = 10) { vocalVolume = it }
                        Spacer(Modifier.height(10.dp))
                        SliderRow("DELAY", delayValue, minValue = 0, maxValue = 10) { delayValue = it }
                        Spacer(Modifier.height(10.dp))
                        SliderRow("PITCH", pitchValue, minValue = -10, maxValue = 10) { pitchValue = it }
                    }
                }

                RecTool.NOICE -> {
                    PanelCard(title = "NOISE SUPPRESSION") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(Color(0xFF0E1017), RoundedCornerShape(28.dp))
                                .border(1.dp, Color(0xFF2A2D3A), RoundedCornerShape(28.dp))
                                .padding(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(if (!noiseOn) Color(0xFFFF2E63) else Color.Transparent, RoundedCornerShape(22.dp))
                                    .clickable { noiseOn = false },
                                contentAlignment = Alignment.Center
                            ) { Text("Off", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(if (noiseOn) Color(0xFFFF2E63) else Color.Transparent, RoundedCornerShape(22.dp))
                                    .clickable { noiseOn = true },
                                contentAlignment = Alignment.Center
                            ) { Text("On", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                        }
                    }
                }

                RecTool.EFFECTS -> {
                    PanelCard(title = "EFFECTS") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            effects.forEach { item ->
                                SelectChip(text = item, selected = item == selectedEffect, onClick = { selectedEffect = item })
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        SliderRow("LEVEL", effectLevel, minValue = 0, maxValue = 10) { effectLevel = it }
                    }
                }

                RecTool.SOUNDS -> {
                    PanelCard(title = "SOUNDS") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            sounds.forEach { item ->
                                SelectChip(text = item, selected = item == selectedSound, onClick = { selectedSound = item })
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        SliderRow("VOLUME", soundVolume, minValue = 0, maxValue = 10) { soundVolume = it }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF171923), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFF2A2D3A), RoundedCornerShape(12.dp))
                                    .clickable { audioPicker.launch("audio/*") }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text("Pick Audio", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            if (selectedAudioUri != null) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF350817), RoundedCornerShape(12.dp))
                                        .border(1.dp, Color(0xFFFF2E63), RoundedCornerShape(12.dp))
                                        .clickable { MediaSelectionStore.setAudio(null) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text("Clear", color = Color(0xFFFF2E63), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 170.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RecToolButton("PITCH", R.drawable.ic_rec_filter, selectedTool == RecTool.PITCH) { selectedTool = RecTool.PITCH }
            RecToolButton("NOICE", R.drawable.ic_rec_bg, selectedTool == RecTool.NOICE) { selectedTool = RecTool.NOICE }
            RecToolButton("EFFECTS", R.drawable.ic_rec_fx, selectedTool == RecTool.EFFECTS) { selectedTool = RecTool.EFFECTS }
            RecToolButton("SOUNDS", R.drawable.ic_rec_audio, selectedTool == RecTool.SOUNDS) { selectedTool = RecTool.SOUNDS }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(Color(0xFF171923), RoundedCornerShape(19.dp)),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("0.3x", "0.5x", "1x", "2x", "3x").forEach { item ->
                    val selected = speed == item
                    Box(
                        modifier = Modifier
                            .background(if (selected) Color(0xFFFF2E63) else Color.Transparent, RoundedCornerShape(16.dp))
                            .clickable { speed = item }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(item, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(78.dp)
                    .border(2.dp, Color(0xFFFF2E63), RoundedCornerShape(39.dp))
                    .clickable {
                        val capture = videoCapture ?: return@clickable
                        if (isRecording) {
                            recording?.stop()
                            recording = null
                            isRecording = false
                            showSaveSheet = true
                        } else {
                            sec = 0
                            val name = "tikTok_${System.currentTimeMillis()}"
                            val contentValues = ContentValues().apply {
                                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/TikTokClone")
                            }
                            val outputOptions = MediaStoreOutputOptions.Builder(
                                context.contentResolver,
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            ).setContentValues(contentValues).build()
                            recording = capture.output
                                .prepareRecording(context, outputOptions)
                                .apply {
                                    if (ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    ) {
                                        withAudioEnabled()
                                    }
                                }
                                .start(ContextCompat.getMainExecutor(context)) { event ->
                                    when (event) {
                                        is VideoRecordEvent.Start -> {
                                            isRecording = true
                                        }
                                        is VideoRecordEvent.Finalize -> {
                                            isRecording = false
                                            val uri = event.outputResults.outputUri
                                            if (uri != null) {
                                                latestRecordedUri = uri
                                                MediaSelectionStore.setVideo(uri)
                                            }
                                        }
                                    }
                                }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isRecording) 24.dp else 58.dp)
                        .background(
                            if (isRecording) Color(0xFFFF2E63) else Color.White,
                            RoundedCornerShape(if (isRecording) 6.dp else 29.dp)
                        )
                )
            }

            Spacer(Modifier.height(16.dp))

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
        }

        if (showSaveSheet) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { showSaveSheet = false }
            )

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                SaveVideoSheet(
                    onNewVideo = {
                        showSaveSheet = false
                        sec = 0
                        isRecording = false
                    },
                    onDiscard = { showSaveSheet = false },
                    onSave = {
                        showSaveSheet = false
                        val savedUri = latestRecordedUri
                        if (savedUri != null) {
                            scope.launch {
                                runCatching {
                                    ServiceLocator.contentRepository.publishPost(
                                        caption = "Saved clip",
                                        mediaUrl = savedUri.toString(),
                                        privacy = "private"
                                    )
                                }
                            }
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "video/*"
                                        putExtra(Intent.EXTRA_STREAM, savedUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                )
                            }
                        }
                    },
                    onPost = {
                        showSaveSheet = false
                        showPublishSheet = true
                    }
                )
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
                        fileName = "recorded.mp4",
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
    }
}

private suspend fun android.content.Context.getCameraProvider(): ProcessCameraProvider =
    suspendCancellableCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener(
            {
                try {
                    continuation.resume(future.get())
                } catch (t: Throwable) {
                    continuation.resumeWithException(t)
                }
            },
            ContextCompat.getMainExecutor(this)
        )
    }

private fun effectOverlayColor(effect: String, level: Float): Color {
    val alpha = (0.08f + (level * 0.22f)).coerceIn(0f, 0.30f)
    return when (effect) {
        "Small Room" -> Color(0x224A6FA5).copy(alpha = alpha)
        "Medium Room" -> Color(0x223B7A6E).copy(alpha = alpha)
        "Cathedral" -> Color(0x228E6CB3).copy(alpha = alpha)
        "Large Room" -> Color(0x22617DA8).copy(alpha = alpha)
        "Medium Hall" -> Color(0x22A66C4C).copy(alpha = alpha)
        "Large Hall" -> Color(0x225D8FA3).copy(alpha = alpha)
        "Medium Chamber" -> Color(0x22B06F8C).copy(alpha = alpha)
        else -> Color.Transparent
    }
}
