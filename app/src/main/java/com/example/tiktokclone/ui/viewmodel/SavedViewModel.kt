package com.example.tiktokclone.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tiktokclone.data.ServiceLocator
import com.example.tiktokclone.domain.model.Post
import com.example.tiktokclone.domain.model.SavedImageItem
import com.example.tiktokclone.domain.model.SavedVideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class SavedViewModel : ViewModel() {
    private val repo = ServiceLocator.contentRepository

    private val _videos = MutableStateFlow<List<SavedVideoItem>>(emptyList())
    val videos: StateFlow<List<SavedVideoItem>> = _videos

    private val _images = MutableStateFlow<List<SavedImageItem>>(emptyList())
    val images: StateFlow<List<SavedImageItem>> = _images

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            runCatching {
                delay(400)
                val saved = repo.getSavedItems()
                val sampleThumbs = listOf(
                    com.example.tiktokclone.R.drawable.sample_img_1,
                    com.example.tiktokclone.R.drawable.sample_img_2,
                    com.example.tiktokclone.R.drawable.sample_video_1_thumb,
                    com.example.tiktokclone.R.drawable.sample_video_2_thumb
                )
                val videos = saved.filter { it.type == "video" }.mapIndexed { i, _ ->
                    SavedVideoItem(
                        videoRes = com.example.tiktokclone.R.raw.sample_video_1,
                        thumbRes = sampleThumbs[i % sampleThumbs.size]
                    )
                }
                val images = saved.filter { it.type == "image" }.mapIndexed { i, _ ->
                    SavedImageItem(
                        imageRes = sampleThumbs[i % sampleThumbs.size]
                    )
                }
                _videos.value = videos
                _images.value = images
                _posts.value = repo.getMyPosts()
            }.onFailure { e ->
                _error.value = e.message ?: "Unknown error"
            }
            _loading.value = false
        }
    }
}
