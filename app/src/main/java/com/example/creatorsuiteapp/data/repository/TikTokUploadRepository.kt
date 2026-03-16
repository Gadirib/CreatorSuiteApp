package com.example.creatorsuiteapp.data.repository

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import com.example.creatorsuiteapp.domain.model.MediaUpload
import com.example.creatorsuiteapp.domain.model.PublishResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.zip.CRC32
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.text.SimpleDateFormat
import java.util.*

class TikTokUploadRepository(private val context: Context) {

    private val TAG = "TikTokUpload"
    private val client = OkHttpClient.Builder().build()
    private val UA = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    // ── Cookie helpers ────────────────────────────────────────────────────────
    private fun getCookie(name: String): String =
        CookieManager.getInstance().getCookie("https://www.tiktok.com")
            ?.split(";")?.map { it.trim() }
            ?.firstOrNull { it.startsWith("$name=") }
            ?.substringAfter("$name=")?.trim() ?: ""

    private fun allCookies(): String =
        CookieManager.getInstance().getCookie("https://www.tiktok.com") ?: ""

    private fun baseHeaders(): Headers = Headers.Builder()
        .add("User-Agent", UA)
        .add("Cookie", allCookies())
        .add("Referer", "https://www.tiktok.com/")
        .add("Origin", "https://www.tiktok.com")
        .build()

    // ── Step 1: Get AWS upload credentials ───────────────────────────────────
    data class AwsCredentials(
        val accessKeyId: String,
        val secretAccessKey: String,
        val sessionToken: String,
        val uploadHost: String,
        val region: String = "ap-singapore-1",
        val service: String = "vod"
    )

    suspend fun getUploadAuth(): AwsCredentials = withContext(Dispatchers.IO) {
        val csrf = getCookie("tt_csrf_token")
        val request = Request.Builder()
            .url("https://www.tiktok.com/api/v1/video/upload/auth/")
            .headers(baseHeaders())
            .addHeader("X-CSRFToken", csrf)
            .get()
            .build()

        val body = client.newCall(request).execute().body?.string()
            ?: throw Exception("Empty response from upload/auth")
        Log.d(TAG, "Step1 auth: ${body.take(300)}")

        val json = JSONObject(body)
        val statusCode = json.optInt("statusCode", json.optInt("status_code", -1))
        if (statusCode != 0) throw Exception("Upload auth failed: $body")

        val data = json.optJSONObject("data") ?: json
        AwsCredentials(
            accessKeyId = data.getString("accessKeyId"),
            secretAccessKey = data.getString("secretAccessKey"),
            sessionToken = data.getString("sessionToken"),
            uploadHost = data.getString("uploadHost")
        )
    }

    // ── Step 2: Apply upload slot (AWS Sig V4 signed) ─────────────────────────
    data class UploadSlot(
        val uploadId: String,
        val vid: String
    )

    suspend fun applyUpload(creds: AwsCredentials, fileSize: Long): UploadSlot = withContext(Dispatchers.IO) {
        val url = "https://${creds.uploadHost}/top/v1?Action=ApplyUploadInner" +
                "&SpaceName=tiktok" +
                "&FileType=video" +
                "&FileSize=$fileSize" +
                "&IsInner=1"

        val now = Date()
        val signedRequest = awsSignedRequest(
            method = "POST",
            url = url,
            body = ByteArray(0),
            creds = creds,
            now = now
        )

        val body = client.newCall(signedRequest).execute().body?.string()
            ?: throw Exception("Empty response from ApplyUploadInner")
        Log.d(TAG, "Step2 apply: ${body.take(300)}")

        val json = JSONObject(body)
        val result = json.optJSONObject("Result") ?: json.optJSONObject("result") ?: json
        val uploadId = result.optString("UploadID", result.optString("upload_id", ""))
        val vid = result.optString("Vid", result.optString("vid", ""))

        if (uploadId.isEmpty()) throw Exception("No UploadID in response: $body")
        UploadSlot(uploadId = uploadId, vid = vid)
    }

    // ── Step 3: Upload video in 5MB chunks with CRC32 ─────────────────────────
    suspend fun uploadChunks(
        creds: AwsCredentials,
        slot: UploadSlot,
        videoBytes: ByteArray,
        onProgress: (Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        val chunkSize = 5 * 1024 * 1024 // 5MB
        val totalChunks = (videoBytes.size + chunkSize - 1) / chunkSize

        for (i in 0 until totalChunks) {
            val start = i * chunkSize
            val end = minOf(start + chunkSize, videoBytes.size)
            val chunk = videoBytes.copyOfRange(start, end)

            // CRC32 checksum per document spec
            val crc32 = CRC32()
            crc32.update(chunk)
            val checksum = crc32.value

            val url = "https://${creds.uploadHost}/top/v1?Action=UploadPart" +
                    "&SpaceName=tiktok" +
                    "&UploadID=${slot.uploadId}" +
                    "&PartNumber=${i + 1}" +
                    "&CRC32=$checksum"

            val signedRequest = awsSignedRequest(
                method = "PUT",
                url = url,
                body = chunk,
                creds = creds,
                now = Date(),
                contentType = "application/octet-stream"
            )

            val response = client.newCall(signedRequest).execute()
            Log.d(TAG, "Step3 chunk ${i+1}/$totalChunks: HTTP ${response.code}")
            if (!response.isSuccessful) throw Exception("Chunk upload failed: HTTP ${response.code}")

            val progress = ((i + 1) * 100) / totalChunks
            withContext(Dispatchers.Main) { onProgress(progress) }
        }
    }

    // ── Step 4: Commit upload ─────────────────────────────────────────────────
    suspend fun commitUpload(creds: AwsCredentials, slot: UploadSlot): String = withContext(Dispatchers.IO) {
        val url = "https://${creds.uploadHost}/top/v1?Action=CommitUploadInner" +
                "&SpaceName=tiktok" +
                "&UploadID=${slot.uploadId}"

        val signedRequest = awsSignedRequest(
            method = "POST",
            url = url,
            body = ByteArray(0),
            creds = creds,
            now = Date()
        )

        val body = client.newCall(signedRequest).execute().body?.string()
            ?: throw Exception("Empty response from CommitUploadInner")
        Log.d(TAG, "Step4 commit: ${body.take(300)}")

        val json = JSONObject(body)
        val result = json.optJSONObject("Result") ?: json.optJSONObject("result") ?: json
        val videoId = result.optString("Vid", result.optString("vid", slot.vid))
        if (videoId.isEmpty()) throw Exception("No Vid in commit response: $body")
        videoId
    }

    // ── Step 5: Publish post ──────────────────────────────────────────────────
    suspend fun publishPost(
        videoId: String,
        caption: String,
        privacy: String
    ): String = withContext(Dispatchers.IO) {
        val csrf = getCookie("tt_csrf_token")
        val privacyLevel = when (privacy.lowercase()) {
            "public" -> 0
            "friends" -> 1
            "private" -> 2
            else -> 0
        }

        // Try primary endpoint first, fallback if needed
        val endpoints = listOf(
            "https://www.tiktok.com/api/v1/item/create/",
            "https://www.tiktok.com/api/v1/web/project/create/",
            "https://www.tiktok.com/tiktok/web/project/post/v1/"
        )

        val bodyJson = JSONObject().apply {
            put("video_id", videoId)
            put("text", caption)
            put("privacy_level", privacyLevel)
            put("disable_duet", false)
            put("disable_comment", false)
            put("disable_stitch", false)
            put("video_cover_timestamp_ms", 1000)
        }

        for (endpoint in endpoints) {
            try {
                val requestBody = bodyJson.toString()
                    .toRequestBody("application/json; charset=utf-8".toMediaType())

                val request = Request.Builder()
                    .url(endpoint)
                    .headers(baseHeaders())
                    .addHeader("X-CSRFToken", csrf)
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build()

                val body = client.newCall(request).execute().body?.string() ?: continue
                Log.d(TAG, "Step5 publish [$endpoint]: ${body.take(300)}")

                val json = JSONObject(body)
                val code = json.optInt("statusCode", json.optInt("status_code", -1))
                if (code == 0) {
                    val itemId = json.optJSONObject("data")?.optString("item_id")
                        ?: json.optString("item_id", videoId)
                    return@withContext itemId
                }
            } catch (e: Exception) {
                Log.w(TAG, "Endpoint $endpoint failed: ${e.message}")
            }
        }

        throw Exception("All publish endpoints failed")
    }

    // ── Full pipeline ─────────────────────────────────────────────────────────
    suspend fun uploadAndPublish(
        videoBytes: ByteArray,
        caption: String,
        privacy: String,
        onProgress: (stage: String, percent: Int) -> Unit
    ): PublishResult = withContext(Dispatchers.IO) {
        onProgress("Getting upload credentials…", 0)
        val creds = getUploadAuth()

        onProgress("Reserving upload slot…", 5)
        val slot = applyUpload(creds, videoBytes.size.toLong())

        onProgress("Uploading video…", 10)
        uploadChunks(creds, slot, videoBytes) { chunkPercent ->
            onProgress("Uploading video…", 10 + (chunkPercent * 0.7).toInt())
        }

        onProgress("Confirming upload…", 85)
        val videoId = commitUpload(creds, slot)

        onProgress("Publishing…", 90)
        val itemId = publishPost(videoId, caption, privacy)

        onProgress("Done", 100)
        PublishResult(postId = itemId, status = "success")
    }

    // ── AWS Signature V4 ──────────────────────────────────────────────────────
    // Manual implementation per document spec using javax.crypto.Mac (HmacSHA256)
    private fun awsSignedRequest(
        method: String,
        url: String,
        body: ByteArray,
        creds: AwsCredentials,
        now: Date,
        contentType: String = "application/x-www-form-urlencoded"
    ): Request {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val datetimeFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val dateStr = dateFormat.format(now)
        val datetimeStr = datetimeFormat.format(now)

        val parsedUrl = java.net.URL(url)
        val host = parsedUrl.host
        val path = if (parsedUrl.path.isEmpty()) "/" else parsedUrl.path
        val query = parsedUrl.query ?: ""

        val bodyHash = sha256Hex(body)

        // Canonical headers (must be sorted)
        val canonicalHeaders = buildString {
            append("content-type:$contentType\n")
            append("host:$host\n")
            append("x-amz-content-sha256:$bodyHash\n")
            append("x-amz-date:$datetimeStr\n")
            append("x-amz-security-token:${creds.sessionToken}\n")
        }
        val signedHeaders = "content-type;host;x-amz-content-sha256;x-amz-date;x-amz-security-token"

        // Canonical request
        val canonicalRequest = buildString {
            append("$method\n")
            append("$path\n")
            append("$query\n")
            append("$canonicalHeaders\n")
            append("$signedHeaders\n")
            append(bodyHash)
        }

        // String to sign
        val credentialScope = "$dateStr/${creds.region}/${creds.service}/aws4_request"
        val stringToSign = buildString {
            append("AWS4-HMAC-SHA256\n")
            append("$datetimeStr\n")
            append("$credentialScope\n")
            append(sha256Hex(canonicalRequest.toByteArray()))
        }

        // Signing key: HMAC chain
        val signingKey = hmacSha256(
            hmacSha256(
                hmacSha256(
                    hmacSha256(
                        "AWS4${creds.secretAccessKey}".toByteArray(),
                        dateStr
                    ),
                    creds.region
                ),
                creds.service
            ),
            "aws4_request"
        )

        val signature = hmacSha256Hex(signingKey, stringToSign)

        val authHeader = "AWS4-HMAC-SHA256 " +
                "Credential=${creds.accessKeyId}/$credentialScope, " +
                "SignedHeaders=$signedHeaders, " +
                "Signature=$signature"

        val requestBody = if (method == "GET") null
        else body.toRequestBody(contentType.toMediaType())

        return Request.Builder()
            .url(url)
            .method(method, requestBody)
            .header("Content-Type", contentType)
            .header("Host", host)
            .header("X-Amz-Content-Sha256", bodyHash)
            .header("X-Amz-Date", datetimeStr)
            .header("X-Amz-Security-Token", creds.sessionToken)
            .header("Authorization", authHeader)
            .build()
    }

    // ── Crypto helpers ────────────────────────────────────────────────────────
    private fun hmacSha256(key: ByteArray, data: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data.toByteArray(Charsets.UTF_8))
    }

    private fun hmacSha256Hex(key: ByteArray, data: String): String =
        hmacSha256(key, data).joinToString("") { "%02x".format(it) }

    private fun sha256Hex(input: ByteArray): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return digest.digest(input).joinToString("") { "%02x".format(it) }
    }

    private fun sha256Hex(input: String): String = sha256Hex(input.toByteArray(Charsets.UTF_8))
}