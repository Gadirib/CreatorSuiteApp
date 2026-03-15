package com.example.creatorsuiteapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.creatorsuiteapp.data.ServiceLocator
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
    val postId: String? = null
)

class PublishViewModel : ViewModel() {
    private val repo = ServiceLocator.contentRepository

    private val _state = MutableStateFlow(PublishUiState())
    val state: StateFlow<PublishUiState> = _state

    fun publish(fileBytes: ByteArray, fileName: String, caption: String, privacy: String) {
        viewModelScope.launch {
            try {
                _state.value = PublishUiState(stage = PublishStage.Uploading)
                val upload = repo.uploadMedia(fileBytes, fileName)
                _state.value = PublishUiState(stage = PublishStage.Publishing)
                val result = repo.publishPost(caption, upload.mediaUrl, privacy)
                _state.value = PublishUiState(stage = PublishStage.Success, postId = result.postId)
            } catch (t: Throwable) {
                _state.value = PublishUiState(stage = PublishStage.Error, message = t.message ?: "Publish failed")
            }
        }
    }

    fun reset() {
        _state.value = PublishUiState()
    }
}
