package com.example.creatorsuiteapp.data.media

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object MediaSelectionStore {
    private val _videoUri = MutableStateFlow<Uri?>(null)
    val videoUri: StateFlow<Uri?> = _videoUri

    private val _audioUri = MutableStateFlow<Uri?>(null)
    val audioUri: StateFlow<Uri?> = _audioUri

    fun setVideo(uri: Uri?) {
        _videoUri.value = uri
    }

    fun setVideo(path: String) {
        _videoUri.value = Uri.parse("file://$path")
    }

    fun setAudio(uri: Uri?) {
        _audioUri.value = uri
    }

    fun clearAudio() {
        _audioUri.value = null
    }

    fun clear() {
        _videoUri.value = null
        _audioUri.value = null
    }
}