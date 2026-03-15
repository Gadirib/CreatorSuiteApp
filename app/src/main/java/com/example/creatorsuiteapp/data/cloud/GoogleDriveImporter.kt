package com.example.creatorsuiteapp.data.cloud

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
// Scope imported fully qualified below to avoid conflict with androidx.annotation.RestrictTo.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class DriveVideoFile(
    val id: String,
    val name: String,
    val mimeType: String,
    val size: Long,
    val thumbnailUrl: String?
)

sealed class DriveState {
    object Idle : DriveState()
    object Authenticating : DriveState()
    object LoadingFiles : DriveState()
    data class FileList(val files: List<DriveVideoFile>) : DriveState()
    data class Downloading(val fileName: String, val progress: Int) : DriveState()
    data class Done(val localPath: String) : DriveState()
    data class Error(val message: String) : DriveState()
}

object GoogleDriveImporter {

    private const val TAG = "GoogleDrive"

    // ✅ From document
    private const val CLIENT_ID = "460919801678-8sk9os4m9p2mmfa1ihakr2aiusoaitj8.apps.googleusercontent.com"
    private const val DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.readonly"

    const val SIGN_IN_REQUEST_CODE = 9001

    private val httpClient = OkHttpClient.Builder().build()

    // ── Build Google Sign-In intent ───────────────────────────────────────────
    fun getSignInIntent(context: Context): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(com.google.android.gms.common.api.Scope(DRIVE_SCOPE))

            .build()

        val client = GoogleSignIn.getClient(context, gso)
        // Always show account picker — sign out first
        client.signOut()
        return client.signInIntent
    }

    // ── Extract account from sign-in result ───────────────────────────────────
    fun getAccountFromIntent(data: Intent?): GoogleSignInAccount? {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            task.result
        } catch (e: Exception) {
            Log.e(TAG, "Sign-in failed: ${e.message}")
            null
        }
    }

    // ── Exchange serverAuthCode for access token ──────────────────────────────
    suspend fun getAccessToken(serverAuthCode: String): String? = withContext(Dispatchers.IO) {
        val body = FormBody.Builder()
            .add("code", serverAuthCode)
            .add("client_id", CLIENT_ID)
            .add("grant_type", "authorization_code")
            .add("redirect_uri", "")
            .build()

        return@withContext try {
            val request = Request.Builder()
                .url("https://oauth2.googleapis.com/token")
                .post(body)
                .build()
            val response = httpClient.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "{}")
            Log.d(TAG, "Token response: ${json.toString().take(200)}")
            json.optString("access_token").takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            Log.e(TAG, "Token exchange error: ${e.message}")
            null
        }
    }

    // ── Use existing signed-in account token directly ─────────────────────────
    // GoogleSignInAccount gives us an id_token, but for Drive API we need OAuth2 token
    // The simplest approach: use GoogleAuthUtil to get access token on background thread
    suspend fun getAccessTokenFromAccount(context: Context, account: GoogleSignInAccount): String? =
        withContext(Dispatchers.IO) {
            return@withContext try {
                com.google.android.gms.auth.GoogleAuthUtil.getToken(
                    context,
                    account.account!!,
                    "oauth2:$DRIVE_SCOPE"
                )
            } catch (e: Exception) {
                Log.e(TAG, "GoogleAuthUtil error: ${e.message}")
                null
            }
        }

    // ── List video files from Drive ───────────────────────────────────────────
    // ✅ From document: mimeType video/mp4 or video/mov
    suspend fun listVideos(accessToken: String): List<DriveVideoFile> = withContext(Dispatchers.IO) {
        val query = "(mimeType='video/mp4' or mimeType='video/quicktime') and trashed=false"
        val url = "https://www.googleapis.com/drive/v3/files" +
                "?q=${java.net.URLEncoder.encode(query, "UTF-8")}" +
                "&fields=files(id,name,mimeType,size,thumbnailLink)" +
                "&pageSize=50" +
                "&orderBy=modifiedTime+desc"

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .build()

        return@withContext try {
            val body = httpClient.newCall(request).execute().body?.string() ?: return@withContext emptyList()
            Log.d(TAG, "Files response: ${body.take(300)}")
            val json = JSONObject(body)
            val files = json.optJSONArray("files") ?: return@withContext emptyList()
            (0 until files.length()).map { i ->
                val f = files.getJSONObject(i)
                DriveVideoFile(
                    id = f.optString("id"),
                    name = f.optString("name"),
                    mimeType = f.optString("mimeType"),
                    size = f.optLong("size", 0),
                    thumbnailUrl = f.optString("thumbnailLink").takeIf { it.isNotEmpty() }
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "listVideos error: ${e.message}")
            emptyList()
        }
    }

    // ── Download video to temp dir ────────────────────────────────────────────
    // ✅ From document: скачиваем во временную директорию
    suspend fun downloadFile(
        context: Context,
        accessToken: String,
        file: DriveVideoFile,
        onProgress: (Int) -> Unit
    ): String? = withContext(Dispatchers.IO) {
        val url = "https://www.googleapis.com/drive/v3/files/${file.id}?alt=media"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .build()

        return@withContext try {
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Download failed HTTP ${response.code}")
                return@withContext null
            }
            val contentLength = response.body?.contentLength() ?: -1L
            val ext = if (file.mimeType.contains("quicktime")) "mov" else "mp4"
            // ✅ Save to temp directory as specified in document
            val outFile = File(context.cacheDir, "gdrive_${UUID.randomUUID()}.$ext")

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