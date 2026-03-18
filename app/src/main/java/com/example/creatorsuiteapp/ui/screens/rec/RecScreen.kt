package com.example.creatorsuiteapp.ui.screens.rec

import android.Manifest
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.creatorsuiteapp.R
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.creatorsuiteapp.ui.viewmodel.PublishStage
import com.example.creatorsuiteapp.ui.viewmodel.PublishViewModel
import kotlinx.coroutines.delay
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Camera
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
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
import com.example.creatorsuiteapp.data.media.MediaSelectionStore
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import com.example.creatorsuiteapp.data.ServiceLocator
import com.example.creatorsuiteapp.data.repository.SavedVideoRepository
import java.io.File

private enum class RecTool { PITCH, NOICE, EFFECTS, SOUNDS }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecScreen(onClose: () -> Unit) {
    val publishVm: PublishViewModel = viewModel()
    val publishState by publishVm.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // ── Audio processor ───────────────────────────────────────────────────────
    val audioProcessor = remember { RealtimeAudioProcessor(context) }
    DisposableEffect(Unit) { onDispose { audioProcessor.release() } }

    // ── State ─────────────────────────────────────────────────────────────────
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

    // ── Permissions ───────────────────────────────────────────────────────────
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result -> hasPermissions = result.values.all { it } }

    val audioPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> if (uri != null) MediaSelectionStore.setAudio(uri) }

    LaunchedEffect(Unit) {
        val cam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        val mic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        hasPermissions = cam && mic
        if (!hasPermissions) {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    // ── Camera — skip re-bind while recording ─────────────────────────────────
    LaunchedEffect(hasPermissions, lensFacing) {
        if (!hasPermissions || isRecording) return@LaunchedEffect
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
            preview, capture
        )
        videoCapture = capture
        torchEnabled = false
    }

    LaunchedEffect(camera, torchEnabled) {
        camera?.cameraControl?.enableTorch(torchEnabled)
    }

    // ── Timer ─────────────────────────────────────────────────────────────────
    LaunchedEffect(isRecording) {
        if (isRecording) {
            sec = 0
            while (isRecording) { delay(1000); sec++ }
        }
    }

    // ── Layout ────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        if (hasPermissions) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            val effectOverlay = effectOverlayColor(selectedEffect, effectLevel)
            if (effectOverlay != Color.Transparent) {
                Box(modifier = Modifier.fillMaxSize().background(effectOverlay))
            }
        }

        // Top bar
        Row(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(top = 52.dp),
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
                    color = if (torchEnabled) Color(0xFFFFC94D) else Color.White, fontSize = 24.sp,
                    modifier = Modifier.clickable { if (!isRecording) torchEnabled = !torchEnabled }
                )
                Text("⟳", color = Color.White, fontSize = 28.sp,
                    modifier = Modifier.clickable {
                        if (!isRecording) lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                            CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                    }
                )
            }
        }

        // Tool panels
        Box(
            modifier = Modifier.align(Alignment.TopStart).padding(top = 170.dp).padding(end = 118.dp)
        ) {
            when (selectedTool) {
                RecTool.PITCH -> PanelCard(title = "PITCH") {
                    SliderRow("VOCAL VOLUME", vocalVolume, 0, 10) { vocalVolume = it }
                    Spacer(Modifier.height(10.dp))
                    SliderRow("DELAY", delayValue, 0, 10) { delayValue = it }
                    Spacer(Modifier.height(10.dp))
                    SliderRow("PITCH", pitchValue, -10, 10) { pitchValue = it }
                }
                RecTool.NOICE -> PanelCard(title = "NOISE SUPPRESSION") {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                            .background(Color(0xFF0E1017), RoundedCornerShape(28.dp))
                            .border(1.dp, Color(0xFF2A2D3A), RoundedCornerShape(28.dp))
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight()
                                .background(if (!noiseOn) Color(0xFFFF2E63) else Color.Transparent, RoundedCornerShape(22.dp))
                                .clickable { noiseOn = false },
                            contentAlignment = Alignment.Center
                        ) { Text("Off", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight()
                                .background(if (noiseOn) Color(0xFFFF2E63) else Color.Transparent, RoundedCornerShape(22.dp))
                                .clickable { noiseOn = true },
                            contentAlignment = Alignment.Center
                        ) { Text("On", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                    }
                }
                RecTool.EFFECTS -> PanelCard(title = "EFFECTS") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        effects.forEach { item -> SelectChip(item, item == selectedEffect) { selectedEffect = item } }
                    }
                    Spacer(Modifier.height(12.dp))
                    SliderRow("LEVEL", effectLevel, 0, 10) { effectLevel = it }
                }
                RecTool.SOUNDS -> PanelCard(title = "SOUNDS") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        sounds.forEach { item -> SelectChip(item, item == selectedSound) { selectedSound = item } }
                    }
                    Spacer(Modifier.height(12.dp))
                    SliderRow("VOLUME", soundVolume, 0, 10) { soundVolume = it }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF171923), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF2A2D3A), RoundedCornerShape(12.dp))
                                .clickable { audioPicker.launch("audio/*") }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) { Text("Pick Audio", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        if (selectedAudioUri != null) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF350817), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFFFF2E63), RoundedCornerShape(12.dp))
                                    .clickable { MediaSelectionStore.setAudio(null) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) { Text("Clear", color = Color(0xFFFF2E63), fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        }
                    }
                }
            }
        }

        // Tool buttons
        Column(
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 170.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RecToolButton("PITCH", R.drawable.ic_rec_filter, selectedTool == RecTool.PITCH) { selectedTool = RecTool.PITCH }
            RecToolButton("NOICE", R.drawable.ic_rec_bg, selectedTool == RecTool.NOICE) { selectedTool = RecTool.NOICE }
            RecToolButton("EFFECTS", R.drawable.ic_rec_fx, selectedTool == RecTool.EFFECTS) { selectedTool = RecTool.EFFECTS }
            RecToolButton("SOUNDS", R.drawable.ic_rec_audio, selectedTool == RecTool.SOUNDS) { selectedTool = RecTool.SOUNDS }
        }

        // Bottom controls
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(38.dp)
                    .background(Color(0xFF171923), RoundedCornerShape(19.dp)),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("0.3x", "0.5x", "1x", "2x", "3x").forEach { item ->
                    Box(
                        modifier = Modifier
                            .background(if (speed == item) Color(0xFFFF2E63) else Color.Transparent, RoundedCornerShape(16.dp))
                            .clickable { speed = item }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) { Text(item, color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Record button
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .border(2.dp, Color(0xFFFF2E63), RoundedCornerShape(39.dp))
                    .clickable {
                        val capture = videoCapture ?: return@clickable
                        if (isRecording) {
                            // ── STOP ─────────────────────────────────────────
                            recording?.stop()
                            recording = null
                            scope.launch {
                                delay(2000) // wait for CameraX to finalize MP4
                                isRecording = false
                                val finalUri = audioProcessor.stop()
                                if (finalUri != null) {
                                    latestRecordedUri = finalUri
                                    MediaSelectionStore.setVideo(finalUri)
                                }
                                showSaveSheet = true
                            }
                        } else {
                            // ── START ─────────────────────────────────────────
                            audioProcessor.configure(
                                noiseOn = noiseOn,
                                pitchValue = (pitchValue - 0.5f) * 20f,
                                vocalVolume = vocalVolume,
                                delayValue = delayValue,
                                selectedEffect = selectedEffect,
                                effectLevel = effectLevel,
                                selectedSound = selectedSound,
                                soundVolume = soundVolume
                            )
                            val videoPath = audioProcessor.start()
                            val outputOptions = FileOutputOptions.Builder(File(videoPath)).build()
                            recording = capture.output
                                .prepareRecording(context, outputOptions)
                                .start(ContextCompat.getMainExecutor(context)) { event ->
                                    when (event) {
                                        is VideoRecordEvent.Start -> isRecording = true
                                        is VideoRecordEvent.Finalize -> { /* handled in stop block */ }
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
            Spacer(Modifier.height(10.dp))
        }

        // ── Save sheet ────────────────────────────────────────────────────────
        if (showSaveSheet) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f))
                    .clickable { showSaveSheet = false }
            )
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0E0F14), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Handle bar
                    Box(modifier = Modifier.width(40.dp).height(4.dp)
                        .background(Color(0xFF3A3E53), RoundedCornerShape(2.dp)))
                    Spacer(Modifier.height(20.dp))
                    Text("Save your video?", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(20.dp))
                    // Post to TikTok
                    Box(
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                            .background(Color(0xFF2A0D16), RoundedCornerShape(14.dp))
                            .border(1.dp, Color(0xFFFF2E63), RoundedCornerShape(14.dp))
                            .clickable { showSaveSheet = false; showPublishSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.ic_tiktok),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Post to TikTok", color = Color(0xFFFF2E63), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    // Save locally
                    Box(
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                            .background(Color(0xFF14151A), RoundedCornerShape(14.dp))
                            .border(1.dp, Color(0xFF2A2E3F), RoundedCornerShape(14.dp))
                            .clickable {
                                showSaveSheet = false
                                val savedUri = latestRecordedUri
                                if (savedUri != null) {
                                    scope.launch { SavedVideoRepository.importVideo(context, savedUri) }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Save to Library", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(10.dp))
                    // Discard
                    Box(
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                            .clickable { showSaveSheet = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Discard", color = Color(0xFF6F738A), fontSize = 16.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // ── Publish sheet ─────────────────────────────────────────────────────
        if (showPublishSheet) {
            PublishSheet(
                onDismiss = { showPublishSheet = false },
                onPublish = { caption, privacy ->
                    showPublishSheet = false
                    val uri = latestRecordedUri
                    if (uri != null) {
                        publishVm.publishFromUri(uri, caption, privacy)
                    } else {
                        publishVm.publish(ByteArray(0), "recorded.mp4", caption, privacy)
                    }
                },
                isBusy = publishState.stage == PublishStage.Uploading ||
                        publishState.stage == PublishStage.Publishing
            )
        }

        if (publishState.stage != PublishStage.Idle) {
            PublishStatusOverlay(state = publishState, onClose = { publishVm.reset() })
        }
    }
}

private suspend fun android.content.Context.getCameraProvider(): ProcessCameraProvider =
    suspendCancellableCoroutine { continuation ->
        ProcessCameraProvider.getInstance(this).addListener(
            {
                try { continuation.resume(ProcessCameraProvider.getInstance(this).get()) }
                catch (t: Throwable) { continuation.resumeWithException(t) }
            },
            ContextCompat.getMainExecutor(this)
        )
    }

private fun effectOverlayColor(effect: String, level: Float): Color {
    val alpha = (0.08f + (level * 0.22f)).coerceIn(0f, 0.30f)
    return when (effect) {
        "Small Room"     -> Color(0x224A6FA5).copy(alpha = alpha)
        "Medium Room"    -> Color(0x223B7A6E).copy(alpha = alpha)
        "Cathedral"      -> Color(0x228E6CB3).copy(alpha = alpha)
        "Large Room"     -> Color(0x22617DA8).copy(alpha = alpha)
        "Medium Hall"    -> Color(0x22A66C4C).copy(alpha = alpha)
        "Large Hall"     -> Color(0x225D8FA3).copy(alpha = alpha)
        "Medium Chamber" -> Color(0x22B06F8C).copy(alpha = alpha)
        else             -> Color.Transparent
    }
}
