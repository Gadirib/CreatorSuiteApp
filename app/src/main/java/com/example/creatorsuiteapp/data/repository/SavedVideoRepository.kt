package com.example.creatorsuiteapp.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.example.creatorsuiteapp.domain.model.SavedVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object SavedVideoRepository {

    private const val TAG = "SavedVideoRepo"
    private const val JSON_FILE = "saved_videos.json"
    private const val VIDEOS_DIR = "SavedVideos"
    private const val THUMBS_DIR = "Thumbnails"

    // Uses the current TikTok account as the storage scope.
    private fun accountId(context: Context): String {
        val prefs = context.getSharedPreferences("tiktok_session", Context.MODE_PRIVATE)
        val secUid = prefs.getString("secUid", null)
        return if (!secUid.isNullOrBlank()) secUid.takeLast(16) else "default"
    }

    private const val THUMB_WIDTH = 300
    private const val THUMB_HEIGHT = 350
    private const val THUMB_QUALITY = 70

    private val _videos = MutableStateFlow<List<SavedVideo>>(emptyList())
    val videos: StateFlow<List<SavedVideo>> = _videos

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }


    private fun videosDir(context: Context): File =
        File(context.filesDir, "${accountId(context)}/$VIDEOS_DIR").also { it.mkdirs() }

    private fun thumbsDir(context: Context): File =
        File(context.filesDir, "${accountId(context)}/$THUMBS_DIR").also { it.mkdirs() }

    private fun jsonFile(context: Context): File =
        File(context.filesDir, "${accountId(context)}/$JSON_FILE").also { it.parentFile?.mkdirs() }

    fun videoFile(context: Context, savedVideo: SavedVideo): File =
        File(videosDir(context), savedVideo.fileName)

    fun thumbFile(context: Context, savedVideo: SavedVideo): File =
        File(thumbsDir(context), savedVideo.thumbnailFileName)


    suspend fun switchAccount(context: Context) = load(context)

    suspend fun load(context: Context) = withContext(Dispatchers.IO) {
        try {
            val file = jsonFile(context)
            if (!file.exists()) { _videos.value = emptyList(); return@withContext }
            val json = JSONArray(file.readText())
            val list = (0 until json.length()).map { parseVideo(json.getJSONObject(it)) }
            _videos.value = list
            Log.d(TAG, "Loaded ${list.size} videos")
        } catch (e: Exception) {
            Log.e(TAG, "Load error: ${e.message}")
            _videos.value = emptyList()
        }
    }


    suspend fun importVideo(context: Context, sourceUri: Uri): SavedVideo? =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val id = UUID.randomUUID().toString()
                val fileName = "$id.mp4"
                val thumbFileName = "$id.jpg"

                val outVideoFile = File(videosDir(context), fileName)
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(outVideoFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: run {
                    Log.e(TAG, "Cannot open source URI: $sourceUri")
                    return@withContext null
                }

                // Builds the saved clip metadata from the picked source.
                val retriever = MediaMetadataRetriever()
                var duration = 0.0
                try {
                    retriever.setDataSource(context, sourceUri)
                    val durationMs = retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_DURATION
                    )?.toLongOrNull() ?: 0L
                    duration = durationMs / 1000.0

                    val timeUs = minOf(1_000_000L, (durationMs * 1000).coerceAtLeast(0L))
                    val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)

                    if (frame != null) {
                        val scaled = Bitmap.createScaledBitmap(frame, THUMB_WIDTH, THUMB_HEIGHT, true)
                        val thumbFile = File(thumbsDir(context), thumbFileName)
                        FileOutputStream(thumbFile).use { out ->
                            scaled.compress(Bitmap.CompressFormat.JPEG, THUMB_QUALITY, out)
                        }
                        if (scaled != frame) frame.recycle()
                        scaled.recycle()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Retriever error: ${e.message}")
                } finally {
                    try { retriever.release() } catch (e: Exception) {}
                }

                val savedVideo = SavedVideo(
                    id = id,
                    fileName = fileName,
                    thumbnailFileName = thumbFileName,
                    duration = duration,
                    createdAt = Date()
                )

                val updatedList = _videos.value + savedVideo
                saveJson(context, updatedList)
                _videos.value = updatedList

                Log.d(TAG, "Imported video: $id, duration=${duration}s")
                savedVideo
            } catch (e: Exception) {
                Log.e(TAG, "importVideo error: ${e.message}")
                null
            }
        }


    suspend fun delete(context: Context, id: String) = withContext(Dispatchers.IO) {
        val video = _videos.value.firstOrNull { it.id == id } ?: return@withContext
        try {
            File(videosDir(context), video.fileName).delete()
            File(thumbsDir(context), video.thumbnailFileName).delete()
        } catch (e: Exception) {
            Log.e(TAG, "Delete file error: ${e.message}")
        }
        val updatedList = _videos.value.filterNot { it.id == id }
        saveJson(context, updatedList)
        _videos.value = updatedList
    }


    suspend fun markPosted(
        context: Context,
        id: String,
        tiktokItemId: String,
        username: String,
        kind: String = "webSessionPrivateAPI"
    ) = withContext(Dispatchers.IO) {
        val updatedList = _videos.value.map { v ->
            if (v.id == id) v.copy(
                postedToTikTok = true,
                tiktokItemId = tiktokItemId,
                tiktokUsername = username,
                tiktokIntegrationKind = kind,
                tiktokPublishedAt = Date()
            ) else v
        }
        saveJson(context, updatedList)
        _videos.value = updatedList
    }


    private fun saveJson(context: Context, list: List<SavedVideo>) {
        val arr = JSONArray()
        list.forEach { arr.put(toJson(it)) }
        jsonFile(context).writeText(arr.toString(2))
    }

    private fun toJson(v: SavedVideo): JSONObject = JSONObject().apply {
        put("id", v.id)
        put("fileName", v.fileName)
        put("thumbnailFileName", v.thumbnailFileName)
        put("duration", v.duration)
        put("createdAt", dateFormat.format(v.createdAt))
        put("postedToTikTok", v.postedToTikTok)
        v.tiktokItemId?.let { put("tiktokItemId", it) }
        v.tiktokIntegrationKind?.let { put("tiktokIntegrationKind", it) }
        v.tiktokUsername?.let { put("tiktokUsername", it) }
        v.tiktokPublishedAt?.let { put("tiktokPublishedAt", dateFormat.format(it)) }
    }

    private fun parseVideo(j: JSONObject): SavedVideo = SavedVideo(
        id = j.getString("id"),
        fileName = j.getString("fileName"),
        thumbnailFileName = j.getString("thumbnailFileName"),
        duration = j.getDouble("duration"),
        createdAt = dateFormat.parse(j.getString("createdAt")) ?: Date(),
        postedToTikTok = j.optBoolean("postedToTikTok", false),
        tiktokItemId = j.optString("tiktokItemId").takeIf { it.isNotEmpty() },
        tiktokIntegrationKind = j.optString("tiktokIntegrationKind").takeIf { it.isNotEmpty() },
        tiktokUsername = j.optString("tiktokUsername").takeIf { it.isNotEmpty() },
        tiktokPublishedAt = j.optString("tiktokPublishedAt").takeIf { it.isNotEmpty() }
            ?.let { runCatching { dateFormat.parse(it) }.getOrNull() }
    )
}
