package com.example.tiktokclone.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tiktokclone.R
import com.example.tiktokclone.data.ServiceLocator
import com.example.tiktokclone.domain.model.CleanJob
import com.example.tiktokclone.domain.model.CleanerItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

class CleanerViewModel : ViewModel() {
    private val repo = ServiceLocator.contentRepository

    private val _items = MutableStateFlow<List<CleanerItem>>(emptyList())
    val items: StateFlow<List<CleanerItem>> = _items

    private val _cleanJob = MutableStateFlow<CleanJob?>(null)
    val cleanJob: StateFlow<CleanJob?> = _cleanJob

    private val _cleanProgress = MutableStateFlow(0f)
    val cleanProgress: StateFlow<Float> = _cleanProgress

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _username = MutableStateFlow<String?>(null)
    val username: StateFlow<String?> = _username

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            runCatching {
                val me = repo.getMe()
                _username.value = me.name.removePrefix("@").trim()
                // Official TikTok API does not expose reposts list. Keep empty.
                _items.value = emptyList()
            }.onFailure { e ->
                _error.value = e.message ?: "Cannot load profile"
            }
            _loading.value = false
        }
    }
    fun refresh() {
        load()   // Re-run the load logic
    }

    fun startClean(selectedIds: List<String>) {
        viewModelScope.launch {
            _error.value = "Repost deletion is not supported by official TikTok API."
            _cleanProgress.value = 0f
        }
    }

    private fun formatLikes(likes: Long): String {
        return when {
            likes >= 1_000_000 -> String.format("%.1fM", likes / 1_000_000f)
            likes >= 1_000 -> String.format("%.1fk", likes / 1_000f)
            else -> likes.toString()
        }
    }

    private fun formatDuration(durationSec: Double): String {
        val total = durationSec.toInt().coerceAtLeast(0)
        val minutes = total / 60
        val seconds = total % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    private fun formatRelativeDays(createdAt: String): String {
        return try {
            val created = OffsetDateTime.parse(createdAt)
            val days = Duration.between(created, OffsetDateTime.now()).toDays().coerceAtLeast(0)
            if (days == 0L) "today" else "$days days ago"
        } catch (_: DateTimeParseException) {
            "recently"
        }
    }

    private fun thumbForId(id: String): Int {
        val samples = listOf(
            R.drawable.sample_video_1_thumb,
            R.drawable.sample_video_2_thumb,
            R.drawable.sample_img_1,
            R.drawable.sample_img_2
        )
        val idx = kotlin.math.abs(id.hashCode()) % samples.size
        return samples[idx]
    }
}
