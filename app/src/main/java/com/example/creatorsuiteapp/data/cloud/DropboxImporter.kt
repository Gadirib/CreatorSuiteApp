package com.example.creatorsuiteapp.data.cloud

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class DropboxVideoFile(
    val id: String,
    val name: String,
    val path: String,
    val size: Long
)

sealed class DropboxState {
    object Idle : DropboxState()
    object Authenticating : DropboxState()
    object LoadingFiles : DropboxState()
    data class FileList(val files: List<DropboxVideoFile>) : DropboxState()
    data class Downloading(val fileName: String, val progress: Int) : DropboxState()
    data class Error(val message: String) : DropboxState()
}

object DropboxImporter {

    private const val TAG = "Dropbox"

    // From document
    private const val APP_KEY = "6x6ok852aaqn53i"
    private const val REDIRECT_URI = "db-6x6ok852aaqn53i://2/token"

    private val httpClient = OkHttpClient.Builder().build()

    // ── Build OAuth URL ───────────────────────────────────────────────────────
    // Dropbox uses standard OAuth2 — token returned in URL fragment after user approves
    fun buildAuthUrl(): String {
        return "https://www.dropbox.com/oauth2/authorize" +
                "?client_id=$APP_KEY" +
                "&redirect_uri=${java.net.URLEncoder.encode(REDIRECT_URI, "UTF-8")}" +
                "&response_type=token" +     // implicit flow — token in URL fragment
                "&token_access_type=online"
    }

    // ── Detect redirect URL ───────────────────────────────────────────────────
    fun isRedirectUrl(url: String): Boolean = url.startsWith(REDIRECT_URI)

    // ── Extract token from redirect URL fragment ──────────────────────────────
    // Dropbox redirects to: https://localhost/dropbox-auth#access_token=xxx&...
    fun extractToken(url: String): String? {
        return try {
            val fragment = url.substringAfter("#", "")
            fragment.split("&")
                .firstOrNull { it.startsWith("access_token=") }
                ?.substringAfter("access_token=")
                ?.trim()
                .takeIf { !it.isNullOrEmpty() }
        } catch (e: Exception) {
            Log.e(TAG, "extractToken error: ${e.message}")
            null
        }
    }

    // ── List video files recursively ──────────────────────────────────────────
    // From document: рекурсивный поиск видеофайлов
    suspend fun listVideos(accessToken: String): List<DropboxVideoFile> = withContext(Dispatchers.IO) {
        val result = mutableListOf<DropboxVideoFile>()
        var cursor: String? = null
        var hasMore = true

        while (hasMore) {
            val (files, nextCursor, more) = if (cursor == null) {
                fetchFiles(accessToken, null)
            } else {
                fetchFilesContinue(accessToken, cursor)
            }
            result.addAll(files)
            cursor = nextCursor
            hasMore = more
        }

        Log.d(TAG, "Total videos found: ${result.size}")
        result
    }

    private data class FetchResult(
        val files: List<DropboxVideoFile>,
        val cursor: String?,
        val hasMore: Boolean
    )

    private fun fetchFiles(accessToken: String, cursor: String?): FetchResult {
        val json = JSONObject().apply {
            put("path", "")
            put("recursive", true)
            put("include_media_info", false)
            put("include_deleted", false)
            put("include_has_explicit_shared_members", false)
            put("limit", 2000)
        }

        val request = Request.Builder()
            .url("https://api.dropboxapi.com/2/files/list_folder")
            .header("Authorization", "Bearer $accessToken")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return parseListResponse(httpClient.newCall(request).execute())
    }

    private fun fetchFilesContinue(accessToken: String, cursor: String): FetchResult {
        val json = JSONObject().apply { put("cursor", cursor) }

        val request = Request.Builder()
            .url("https://api.dropboxapi.com/2/files/list_folder/continue")
            .header("Authorization", "Bearer $accessToken")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return parseListResponse(httpClient.newCall(request).execute())
    }

    private fun parseListResponse(response: Response): FetchResult {
        return try {
            val body = response.body?.string() ?: return FetchResult(emptyList(), null, false)
            Log.d(TAG, "List response: ${body.take(300)}")
            val json = JSONObject(body)
            val entries = json.optJSONArray("entries") ?: return FetchResult(emptyList(), null, false)

            val videoExtensions = setOf("mp4", "mov", "avi", "mkv", "m4v", "wmv")
            val files = (0 until entries.length())
                .map { entries.getJSONObject(it) }
                .filter { entry ->
                    val tag = entry.optString(".tag")
                    val name = entry.optString("name").lowercase()
                    tag == "file" && videoExtensions.any { name.endsWith(".$it") }
                }
                .map { entry ->
                    DropboxVideoFile(
                        id = entry.optString("id"),
                        name = entry.optString("name"),
                        path = entry.optString("path_lower"),
                        size = entry.optLong("size", 0)
                    )
                }

            FetchResult(
                files = files,
                cursor = json.optString("cursor").takeIf { it.isNotEmpty() },
                hasMore = json.optBoolean("has_more", false)
            )
        } catch (e: Exception) {
            Log.e(TAG, "parseListResponse error: ${e.message}")
            FetchResult(emptyList(), null, false)
        }
    }

    // ── Download file to temp dir ─────────────────────────────────────────────
    // From document: скачивание с прогрессом
    suspend fun downloadFile(
        context: Context,
        accessToken: String,
        file: DropboxVideoFile,
        onProgress: (Int) -> Unit
    ): String? = withContext(Dispatchers.IO) {
        // Dropbox download: POST to content endpoint with path in Dropbox-API-Arg header
        val apiArg = JSONObject().apply { put("path", file.path) }.toString()

        val request = Request.Builder()
            .url("https://content.dropboxapi.com/2/files/download")
            .header("Authorization", "Bearer $accessToken")
            .header("Dropbox-API-Arg", apiArg)
            .post("".toRequestBody(null))
            .build()

        return@withContext try {
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Download failed HTTP ${response.code}: ${response.body?.string()?.take(200)}")
                return@withContext null
            }

            val contentLength = response.body?.contentLength() ?: -1L
            val ext = file.name.substringAfterLast(".", "mp4").lowercase()
            val outFile = File(context.cacheDir, "dropbox_${UUID.randomUUID()}.$ext")

            response.body?.byteStream()?.use { input ->
                outFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var downloaded = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (contentLength > 0) {
                            val progress = ((downloaded * 100) / contentLength).toInt()
                            withContext(Dispatchers.Main) { onProgress(progress) }
                        }
                    }
                }
            }

            Log.d(TAG, "Downloaded: ${outFile.absolutePath}")
            outFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "downloadFile error: ${e.message}")
            null
        }
    }
}