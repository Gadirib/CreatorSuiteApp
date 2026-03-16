package com.example.creatorsuiteapp.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.creatorsuiteapp.data.repository.TikTokUploadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class PublishStage {
    Idle,
    Uploading,
    Publishing,
    Success,
    Error
}

data class PublishUiState(
    val stage: PublishStage = PublishStage.Idle,
    val message: String? = null,
    val postId: String? = null,
    val progress: Int = 0
)

class PublishViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = TikTokUploadRepository(app.applicationContext)

    private val _state = MutableStateFlow(PublishUiState())
    val state: StateFlow<PublishUiState> = _state

    // ── Publish from URI (preferred — no need to load entire file into memory) ─
    fun publishFromUri(
        videoUri: Uri,
        caption: String,
        privacy: String
    ) {
        viewModelScope.launch {
            try {
                _state.value = PublishUiState(stage = PublishStage.Uploading, message = "Reading video…", progress = 0)

                // Read video bytes from URI
                val bytes = getApplication<Application>().contentResolver
                    .openInputStream(videoUri)?.use { it.readBytes() }
                    ?: throw Exception("Cannot read video file")

                publish(fileBytes = bytes, fileName = "video.mp4", caption = caption, privacy = privacy)

            } catch (t: Throwable) {
                _state.value = PublishUiState(
                    stage = PublishStage.Error,
                    message = t.message ?: "Failed to read video"
                )
            }
        }
    }

    // ── Publish from raw bytes ────────────────────────────────────────────────
    fun publish(fileBytes: ByteArray, fileName: String = "video.mp4", caption: String, privacy: String) {
        viewModelScope.launch {
            try {
                repo.uploadAndPublish(
                    videoBytes = fileBytes,
                    caption = caption,
                    privacy = privacy,
                    onProgress = { stage, percent ->
                        val publishStage = if (percent >= 90) PublishStage.Publishing else PublishStage.Uploading
                        _state.value = PublishUiState(
                            stage = publishStage,
                            message = stage,
                            progress = percent
                        )
                    }
                ).let { result ->
                    _state.value = PublishUiState(
                        stage = PublishStage.Success,
                        postId = result.postId,
                        progress = 100
                    )
                }
            } catch (t: Throwable) {
                _state.value = PublishUiState(
                    stage = PublishStage.Error,
                    message = t.message ?: "Publish failed"
                )
            }
        }
    }

    fun reset() {
        _state.value = PublishUiState()
    }
} 