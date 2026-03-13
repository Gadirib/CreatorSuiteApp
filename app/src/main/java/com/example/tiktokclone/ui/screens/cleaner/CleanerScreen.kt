package com.example.tiktokclone.ui.screens.cleaner

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.tiktokclone.data.tiktok.TikTokApiClient
import com.example.tiktokclone.data.tiktok.TikTokSessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

// Minimal repost model (expand later with more fields from your document)
data class TikTokRepost(
    val id: String,             // aweme_id for delete
    val desc: String,
    val authorUsername: String,
    val playCount: Int,
    val coverUrl: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanerScreen(
    onBackToContent: () -> Unit,
    onOpenCreate: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val session = TikTokSessionManager.getSession(context)
    if (session == null) {
        LaunchedEffect(Unit) { onBackToContent() }
        return
    }

    val apiClient = remember { TikTokApiClient(context) }

    var reposts by remember { mutableStateOf<List<TikTokRepost>>(emptyList()) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var deletionsToday by remember { mutableIntStateOf(getDeletionsToday(context)) }
    var isDeleting by remember { mutableStateOf(false) }

    // Load reposts with pagination & rate limiting
    LaunchedEffect(session.secUid) {
        isLoading = true
        errorMessage = null
        val allReposts = mutableListOf<TikTokRepost>()
        var cursor = 0
        var consecutiveErrors = 0

        while (true) {
            val json = apiClient.getReposts(session.secUid, cursor) ?: run {
                consecutiveErrors++
                if (consecutiveErrors >= 5) {
                    errorMessage = "Too many network errors. Stopping."
                    break
                }
                delay(3000)
                continue
            }

            consecutiveErrors = 0

            if (json.optInt("statusCode", -1) != 0) break

            val items = json.optJSONArray("itemList") ?: break
            repeat(items.length()) { i ->
                val item = items.getJSONObject(i)
                val video = item.optJSONObject("video") ?: return@repeat

                allReposts += TikTokRepost(
                    id = item.optString("id"),
                    desc = item.optString("desc", "(no description)"),
                    authorUsername = item.optJSONObject("author")?.optString("uniqueId") ?: "?",
                    playCount = video.optJSONObject("stats")?.optInt("playCount") ?: 0,
                    coverUrl = video.optString("cover")
                )
            }

            val hasMore = json.optBoolean("hasMore", false)
            if (!hasMore) break

            cursor = json.optInt("cursor", cursor + 30)
            delay(5000) // document: 5 seconds between pages
        }

        reposts = allReposts
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clean Reposts") },
                navigationIcon = {
                    IconButton(onClick = onBackToContent) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = false,
                    onClick = onOpenCreate,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    label = { Text("Create") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onOpenSettings,
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                isLoading -> {
                    Spacer(Modifier.weight(1f))
                    CircularProgressIndicator()
                    Spacer(Modifier.height(24.dp))
                    Text("Loading your reposts…")
                    Spacer(Modifier.weight(1f))
                }

                errorMessage != null -> {
                    Spacer(Modifier.weight(1f))
                    Text(
                        errorMessage ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.weight(1f))
                }

                reposts.isEmpty() -> {
                    Spacer(Modifier.weight(1f))
                    Text("No reposts found or already cleaned.")
                    Spacer(Modifier.weight(1f))
                }

                else -> {
                    Text(
                        "Deletions today: $deletionsToday / 50",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(reposts) { repost ->
                            val selected = repost.id in selectedIds
                            Card(
                                onClick = {
                                    selectedIds = if (selected) selectedIds - repost.id else selectedIds + repost.id
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = selected,
                                        onCheckedChange = null
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = repost.desc.take(80).let { if (repost.desc.length > 80) "$it..." else it },
                                            maxLines = 2
                                        )
                                        Text(
                                            "@${repost.authorUsername} • ${repost.playCount} plays",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (selectedIds.isNotEmpty() && !isDeleting) {
                        val count = selectedIds.size
                        Button(
                            onClick = {
                                scope.launch {
                                    isDeleting = true
                                    var success = 0
                                    var failed = 0
                                    var consecFails = 0

                                    selectedIds.forEach { id ->
                                        if (deletionsToday >= 50) {
                                            Toast.makeText(context, "Daily limit (50) reached", Toast.LENGTH_LONG).show()
                                            return@forEach
                                        }

                                        var ok = false
                                        repeat(2) {
                                            try {
                                                apiClient.deleteRepost(id)
                                                ok = true
                                                return@repeat
                                            } catch (e: Exception) {
                                                delay(Random.nextLong(1500, 4000))
                                            }
                                        }

                                        if (ok) {
                                            success++
                                            deletionsToday++
                                            saveDeletionsToday(context, deletionsToday)
                                            consecFails = 0
                                        } else {
                                            failed++
                                            consecFails++
                                            if (consecFails >= 5) {
                                                errorMessage = "Too many failures. Stopping."
                                                return@launch
                                            }
                                        }

                                        delay(Random.nextLong(1000, 3000)) // document rate limit
                                    }

                                    Toast.makeText(
                                        context,
                                        "Deleted $success of $count (failed: $failed)",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    selectedIds = emptySet()
                                    isDeleting = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text("Delete $count selected")
                        }
                    }
                }
            }
        }
    }
}

// Daily counter helpers (simple, resets on app restart)
private const val PREFS = "cleaner"
private const val KEY_COUNT = "deletions_today"

private fun getDeletionsToday(ctx: Context) =
    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_COUNT, 0)

private fun saveDeletionsToday(ctx: Context, value: Int) {
    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit()
        .putInt(KEY_COUNT, value)
        .apply()
}