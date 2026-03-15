package com.example.creatorsuiteapp.ui.screens.cleaner

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.creatorsuiteapp.data.tiktok.TikTokApiService
import com.example.creatorsuiteapp.data.tiktok.TikTokSessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

data class TikTokRepost(
    val id: String,          // video ID — this is what /api/repost/cancel/ needs
    val desc: String,
    val authorUsername: String,
    val authorNickname: String,
    val playCount: Int,
    val likeCount: Int,
    val coverUrl: String?,
    val duration: Int,
    val createTime: Long
)

sealed class LoadState {
    object Idle : LoadState()
    object Loading : LoadState()
    data class Error(val message: String) : LoadState()
    object Success : LoadState()
}

class UnRepostViewModel(app: Application) : AndroidViewModel(app) {

    private val context: Context get() = getApplication()

    private val _reposts = MutableStateFlow<List<TikTokRepost>>(emptyList())
    val reposts = _reposts.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds = _selectedIds.asStateFlow()

    private val _loadState = MutableStateFlow<LoadState>(LoadState.Idle)
    val loadState = _loadState.asStateFlow()

    private val _deleteProgress = MutableStateFlow<String?>(null)
    val deleteProgress = _deleteProgress.asStateFlow()

    private val _deletePercent = MutableStateFlow<Int?>(null)
    val deletePercent = _deletePercent.asStateFlow()

    private val _deletionsToday = MutableStateFlow(getDeletionsToday())
    val deletionsToday = _deletionsToday.asStateFlow()

    val dailyLimit = 50

    fun initApi(context: Context) = TikTokApiService.init(context)

    fun loadReposts() {
        val session = TikTokSessionManager.getSession(context) ?: run {
            _loadState.value = LoadState.Error("Not logged in"); return
        }

        viewModelScope.launch {
            _loadState.value = LoadState.Loading
            val all = mutableListOf<TikTokRepost>()
            var cursor = 0
            var errors = 0

            while (true) {
                val json = TikTokApiService.getReposts(session.secUid, cursor)
                if (json == null) {
                    if (++errors >= 5) { _loadState.value = LoadState.Error("Failed. Tap Retry."); return@launch }
                    delay(3000); continue
                }
                errors = 0

                val status = json.optInt("statusCode", json.optInt("status_code", -1))
                if (status != 0) break

                val items = json.optJSONArray("itemList") ?: break

                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    val author = item.optJSONObject("author")
                    val video = item.optJSONObject("video")
                    val stats = item.optJSONObject("stats")

                    // ✅ repostList[0] contains YOUR user info (who reposted),
                    // NOT a repost record ID. The correct delete ID is the video "id" field.
                    val videoId = item.optString("id")

                    all += TikTokRepost(
                        id = videoId,
                        desc = item.optString("desc", "(no description)"),
                        authorUsername = author?.optString("uniqueId") ?: "?",
                        authorNickname = author?.optString("nickname") ?: "?",
                        playCount = stats?.optInt("playCount") ?: 0,
                        likeCount = stats?.optInt("diggCount") ?: 0,
                        coverUrl = video?.optString("cover"),
                        duration = video?.optInt("duration") ?: 0,
                        createTime = item.optLong("createTime")
                    )
                }

                if (!json.optBoolean("hasMore", false)) break
                cursor = json.optInt("cursor", cursor + 30)
                delay(5000)
            }

            _reposts.value = all
            _loadState.value = LoadState.Success
            Log.d("UnRepostVM", "Loaded ${all.size} reposts")
        }
    }

    fun toggleSelect(id: String) {
        _selectedIds.value = _selectedIds.value.toMutableSet().apply {
            if (contains(id)) remove(id) else add(id)
        }
    }

    fun selectAll() { _selectedIds.value = _reposts.value.map { it.id }.toSet() }
    fun clearSelection() { _selectedIds.value = emptySet() }

    fun deleteSelected(onDone: (Int, Int) -> Unit) {
        val toDelete = _reposts.value.filter { it.id in _selectedIds.value }
        if (toDelete.isEmpty()) return

        viewModelScope.launch {
            var success = 0; var failed = 0; var streak = 0

            toDelete.forEachIndexed { i, repost ->
                if (_deletionsToday.value >= dailyLimit) {
                    _deleteProgress.value = "Daily limit reached"
                    _deletePercent.value = null
                    delay(1000); _deleteProgress.value = null
                    onDone(success, failed + (toDelete.size - i)); return@launch
                }

                _deleteProgress.value = "Deleting ${i + 1} of ${toDelete.size}…"
                _deletePercent.value = ((i.toFloat() / toDelete.size.toFloat()) * 100f).toInt()
                Log.d("UnRepostVM", "Deleting videoId=${repost.id}")

                // ✅ Pass the video ID directly — confirmed working endpoint:
                // POST /api/repost/cancel/ with body aweme_id={videoId}
                // returns status_code=0 ("url doesn't match" is just internal routing noise)
                var deleted = false
                repeat(2) {
                    if (!deleted) {
                        deleted = TikTokApiService.deleteRepost(repost.id)
                        if (!deleted) delay(Random.nextLong(1500, 4000))
                    }
                }

                if (deleted) {
                    success++; streak = 0
                    _deletionsToday.value++
                    saveDeletionsToday(_deletionsToday.value)
                    _reposts.value = _reposts.value.filter { it.id != repost.id }
                } else {
                    failed++
                    if (++streak >= 5) {
                        _deleteProgress.value = "Too many failures"
                        _deletePercent.value = null
                        delay(1500); _deleteProgress.value = null
                        onDone(success, failed); return@launch
                    }
                }

                _deletePercent.value = (((i + 1).toFloat() / toDelete.size.toFloat()) * 100f).toInt()
                delay(Random.nextLong(1000, 3000))
            }

            _selectedIds.value = emptySet()
            _deletePercent.value = null
            _deleteProgress.value = null
            onDone(success, failed)
        }
    }

    private fun getDeletionsToday(): Int {
        val p = context.getSharedPreferences("cleaner_prefs", Context.MODE_PRIVATE)
        return if (p.getString("date", "") == today()) p.getInt("count", 0) else 0
    }

    private fun saveDeletionsToday(n: Int) {
        context.getSharedPreferences("cleaner_prefs", Context.MODE_PRIVATE)
            .edit().putString("date", today()).putInt("count", n).apply()
    }

    private fun today() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}
