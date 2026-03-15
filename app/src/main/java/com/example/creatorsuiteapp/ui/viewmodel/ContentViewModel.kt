package com.example.creatorsuiteapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.creatorsuiteapp.domain.model.Creator
import com.example.creatorsuiteapp.domain.model.Post
import com.example.creatorsuiteapp.data.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class ContentViewModel : ViewModel() {
    private val repo = ServiceLocator.contentRepository

    private val _me = MutableStateFlow<Creator?>(null)
    val me: StateFlow<Creator?> = _me

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            runCatching {
                delay(600)
                _me.value = repo.getMe()
                _posts.value = repo.getMyPosts()
            }.onFailure { e ->
                _error.value = e.message ?: "Unknown error"
            }.also {
                _loading.value = false
            }
        }
    }

    fun refresh() = load()

    fun deletePost(id: String) {
        viewModelScope.launch {
            runCatching { repo.deletePost(id) }
                .onSuccess {
                    _posts.value = _posts.value.filterNot { it.id == id }
                }
                .onFailure { e ->
                    _error.value = e.message ?: "Failed to delete post"
                }
        }
    }

    fun editPostCaption(id: String, caption: String) {
        viewModelScope.launch {
            runCatching { repo.updatePostCaption(id, caption) }
                .onSuccess { updated ->
                    _posts.value = _posts.value.map { post ->
                        if (post.id == id) updated else post
                    }
                }
                .onFailure { e ->
                    _error.value = e.message ?: "Failed to update caption"
                }
        }
    }
}
