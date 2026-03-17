package com.example.creatorsuiteapp.data.repository

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import com.example.creatorsuiteapp.domain.model.PublishResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.CRC32
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class TikTokUploadRepository(private val context: Context) {

    private val TAG = "TikTokUpload"
    private val BASE = "https://www.tiktok.com"
    private val AID = "1988"
    private val UA = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private fun getCookie(name: String): String =
        CookieManager.getInstance().getCookie("https://www.tiktok.com")
            ?.split(";")?.map { it.trim() }
            ?.firstOrNull { it.startsWith("$name=") }
            ?.substringAfter("$name=")?.trim() ?: ""

    private fun allCookies(): String =
        CookieManager.getInstance().getCookie("https://www.tiktok.com") ?: ""

    private fun baseHeaders() = Headers.Builder()
        .add("User-Agent", UA)
        .add("Cookie", allCookies())
        .add("Referer", "https://www.tiktok.com/tiktokstudio/upload?from=webapp")
        .add("Origin", "https://www.tiktok.com")
        .add("Accept", "application/json, text/plain, */*")
        .build()

    data class AwsCredentials(
        val accessKeyId: String,
        val secretAccessKey: String,
        val sessionToken: String,
        val spaceName: String = "tiktok",
        val region: String = "ap-singapore-1",
        val service: String = "vod"
    )

    suspend fun getUploadAuth(): AwsCredentials = withContext(Dispatchers.IO) {
        val csrf = getCookie("tt_csrf_token")
        val body = client.newCall(
            Request.Builder()
                .url("$BASE/api/v1/video/upload/auth/?aid=$AID")
                .headers(baseHeaders())
                .addHeader("X-CSRFToken", csrf)
                .get().build()
        ).execute().body?.string() ?: throw Exception("Empty upload/auth response")

        Log.d(TAG, "Step1: ${body.take(200)}")
        val json = JSONObject(body)
        if (json.optInt("status_code", -1) != 0)
            throw Exception("Upload auth failed: ${json.optString("status_msg")}")

        val token = json.getJSONObject("video_token_v5")
        AwsCredentials(
            accessKeyId = token.getString("access_key_id"),
            secretAccessKey = token.getString("secret_acess_key"),
            sessionToken = token.getString("session_token"),
            spaceName = token.optString("space_name", "tiktok")
        ).also { Log.d(TAG, "Step1 OK keyId=${it.accessKeyId.take(16)}...") }
    }

    data class UploadSlot(
        val vid: String,
        val storeUri: String,
        val uploadHost: String,
        val uploadAuth: String,
        val sessionKey: String
    )

    suspend fun applyUpload(creds: AwsCredentials, fileSize: Long): UploadSlot = withContext(Dispatchers.IO) {
        val now = Date()
        val dateTimeStr = awsDateTime(now)
        val dateStr = dateTimeStr.substring(0, 8)
        val path = "/top/v1"
        val queryParams = sortedMapOf(
            "Action" to "ApplyUploadInner",
            "FileSize" to fileSize.toString(),
            "FileType" to "video",
            "IsInner" to "1",
            "SpaceName" to creds.spaceName,
            "Version" to "2020-11-19"
        )
        val query = queryParams.entries.joinToString("&") { "${it.key}=${it.value}" }
        val authHeader = buildAuthHeader(creds, dateTimeStr, dateStr, path, query)

        val resp = client.newCall(
            Request.Builder()
                .url("$BASE$path?$query")
                .headers(baseHeaders())
                .addHeader("X-Amz-Date", dateTimeStr)
                .addHeader("x-amz-security-token", creds.sessionToken)
                .addHeader("Authorization", authHeader)
                .get().build()
        ).execute()

        val body = resp.body?.string() ?: throw Exception("Empty ApplyUploadInner response")
        Log.d(TAG, "Step2 HTTP ${resp.code}: ${body.take(400)}")

        val json = JSONObject(body)
        val errCode = json.optJSONObject("ResponseMetadata")
            ?.optJSONObject("Error")?.optInt("CodeN", 0) ?: 0
        val errMsg = json.optJSONObject("ResponseMetadata")
            ?.optJSONObject("Error")?.optString("Message", "") ?: ""

        if (errCode == 30411 || errMsg.contains("too many", ignoreCase = true))
            throw Exception("Rate limited — please wait a few minutes and try again.")
        if (errCode != 0) throw Exception("ApplyUpload error $errCode: $errMsg")

        val node = json.getJSONObject("Result")
            .getJSONObject("InnerUploadAddress")
            .getJSONArray("UploadNodes").getJSONObject(0)

        val storeInfo = node.getJSONArray("StoreInfos").getJSONObject(0)
        val storeUri = storeInfo.getString("StoreUri")
        val uploadHost = node.optString("UploadHost", "").takeIf { it.isNotEmpty() }
            ?: "${storeUri.substringBefore("/")}.tiktokcdn.com"
        val sessionKey = node.optString("SessionKey",
            json.getJSONObject("Result").getJSONObject("InnerUploadAddress")
                .optString("SessionKey", ""))

        UploadSlot(
            vid = node.optString("Vid", ""),
            storeUri = storeUri,
            uploadHost = uploadHost,
            uploadAuth = storeInfo.getString("Auth"),
            sessionKey = sessionKey
        ).also { Log.d(TAG, "Step2 OK vid=${it.vid} host=${it.uploadHost}") }
    }

    suspend fun uploadChunks(
        slot: UploadSlot,
        videoBytes: ByteArray,
        onProgress: (Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        val baseUrl = "https://${slot.uploadHost}/${slot.storeUri}"
        Log.d(TAG, "Step3 baseUrl=$baseUrl size=${videoBytes.size}")

        val initResp = client.newCall(
            Request.Builder().url("$baseUrl?uploads")
                .addHeader("Authorization", slot.uploadAuth)
                .addHeader("Content-Type", "video/mp4")
                .post(ByteArray(0).toRequestBody("video/mp4".toMediaType()))
                .build()
        ).execute()
        val initBody = initResp.body?.string() ?: throw Exception("Empty multipart init")
        Log.d(TAG, "Step3 init HTTP ${initResp.code}: ${initBody.take(200)}")

        val uploadId = parseXmlTag(initBody, "UploadId")
            ?: throw Exception("No UploadId in: $initBody")
        Log.d(TAG, "Step3 uploadId=$uploadId")

        val chunkSize = 5 * 1024 * 1024
        val totalChunks = (videoBytes.size + chunkSize - 1) / chunkSize
        val crcList = mutableListOf<Long>()

        for (i in 0 until totalChunks) {
            val chunk = videoBytes.copyOfRange(i * chunkSize, minOf((i + 1) * chunkSize, videoBytes.size))
            val crc = CRC32().also { it.update(chunk) }.value
            crcList.add(crc)

            val chunkResp = client.newCall(
                Request.Builder()
                    .url("$baseUrl?partNumber=${i + 1}&uploadID=$uploadId")
                    .addHeader("Authorization", slot.uploadAuth)
                    .addHeader("Content-Type", "video/mp4")
                    .addHeader("Content-Crc32", "%08x".format(crc))
                    .post(chunk.toRequestBody("video/mp4".toMediaType()))
                    .build()
            ).execute()
            val chunkBody = chunkResp.body?.string() ?: ""
            Log.d(TAG, "Step3 chunk ${i+1}/$totalChunks HTTP ${chunkResp.code}")
            if (chunkResp.code >= 400) throw Exception("Chunk ${i+1} failed ${chunkResp.code}: $chunkBody")

            withContext(Dispatchers.Main) { onProgress(((i + 1) * 100) / totalChunks) }
        }

        val completeBody = crcList.mapIndexed { i, crc -> "${i + 1}:%08x".format(crc) }.joinToString(",")
        val completeResp = client.newCall(
            Request.Builder()
                .url("$baseUrl?uploadID=$uploadId")
                .addHeader("Authorization", slot.uploadAuth)
                .addHeader("Content-Type", "text/plain")
                .post(completeBody.toRequestBody("text/plain".toMediaType()))
                .build()
        ).execute()
        val completeRespBody = completeResp.body?.string() ?: ""
        Log.d(TAG, "Step3 complete HTTP ${completeResp.code}: ${completeRespBody.take(200)}")
        if (completeResp.code >= 400) throw Exception("Multipart complete failed: $completeRespBody")
        Log.d(TAG, "Step3 OK")
    }

    private fun parseXmlTag(xml: String, tag: String): String? = try {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(xml.reader())
        var found = false
        var result: String? = null
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT && result == null) {
            when (event) {
                XmlPullParser.START_TAG -> found = (parser.name == tag)
                XmlPullParser.TEXT -> if (found) { result = parser.text; found = false }
            }
            event = parser.next()
        }
        result ?: Regex("<$tag>(.*?)</$tag>").find(xml)?.groupValues?.get(1)
    } catch (e: Exception) {
        Regex("<$tag>(.*?)</$tag>").find(xml)?.groupValues?.get(1)
    }

    suspend fun commitUpload(creds: AwsCredentials, slot: UploadSlot): String = withContext(Dispatchers.IO) {
        val now = Date()
        val dateTimeStr = awsDateTime(now)
        val dateStr = dateTimeStr.substring(0, 8)
        val path = "/top/v1"
        val queryParams = sortedMapOf(
            "Action" to "CommitUploadInner",
            "SpaceName" to creds.spaceName,
            "Version" to "2020-11-19"
        )
        val query = queryParams.entries.joinToString("&") { "${it.key}=${it.value}" }
        val bodyStr = JSONObject().apply {
            put("SessionKey", slot.sessionKey)
            put("Functions", org.json.JSONArray())
        }.toString()

        val bodyHash = sha256Hex(bodyStr.toByteArray(Charsets.UTF_8))
        val authHeader = buildAuthHeader(creds, dateTimeStr, dateStr, path, query, bodyHash = bodyHash, method = "POST")

        val resp = client.newCall(
            Request.Builder()
                .url("$BASE$path?$query")
                .headers(baseHeaders())
                .addHeader("X-Amz-Date", dateTimeStr)
                .addHeader("x-amz-security-token", creds.sessionToken)
                .addHeader("Authorization", authHeader)
                .addHeader("Content-Type", "application/json")
                .post(bodyStr.toRequestBody("application/json".toMediaType()))
                .build()
        ).execute()

        val body = resp.body?.string() ?: throw Exception("Empty CommitUploadInner response")
        Log.d(TAG, "Step4 HTTP ${resp.code}: ${body.take(300)}")

        val json = JSONObject(body)
        val videoId = json.optJSONObject("Result")?.optString("Vid", "")
            ?.takeIf { it.isNotEmpty() } ?: slot.vid
        Log.d(TAG, "Step4 OK videoId=$videoId")
        videoId
    }

    suspend fun publishPost(videoId: String, caption: String, privacy: String): String = withContext(Dispatchers.IO) {
        val csrf = getCookie("tt_csrf_token")
        val privacyType = when (privacy.lowercase()) { "public" -> 0; "friends" -> 1; else -> 2 }

        try {
            val url = "$BASE/api/v1/item/create/" +
                    "?aid=$AID" +
                    "&video_id=$videoId" +
                    "&text=${java.net.URLEncoder.encode(caption, "UTF-8")}" +
                    "&visibility_type=$privacyType" +
                    "&channel=tiktok_web" +
                    "&device_platform=web" +
                    "&app_name=tiktok_web"

            val resp = client.newCall(
                Request.Builder().url(url)
                    .headers(baseHeaders())
                    .addHeader("X-CSRFToken", csrf)
                    .addHeader("X-Secsdk-Csrf-Request", "1")
                    .addHeader("X-Secsdk-Csrf-Version", "1.2.8")
                    .post(ByteArray(0).toRequestBody("application/json".toMediaType()))
                    .build()
            ).execute()
            val body = resp.body?.string() ?: ""
            Log.d(TAG, "Step5 primary HTTP ${resp.code}: ${body.take(300)}")
            val json = JSONObject(body)
            if (json.optInt("status_code", -1) == 0) {
                return@withContext json.optJSONObject("data")?.optString("item_id", videoId) ?: videoId
            }
            Log.w(TAG, "Primary failed, trying fallback")
        } catch (e: Exception) {
            Log.w(TAG, "Primary exception: ${e.message}")
        }

        val fbBody = JSONObject().apply {
            put("post_common_info", JSONObject().apply {
                put("creation_id", UUID.randomUUID().toString().replace("-", ""))
                put("enter_post_page_from", 1)
                put("post_type", 3)
            })
            put("feature_common_info_list", org.json.JSONArray())
            put("single_post_req_list", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("video_info", JSONObject().apply { put("video_id", videoId) })
                    put("content_info", JSONObject().apply {
                        put("text", caption)
                        put("privacy_level_new", privacyType)
                    })
                    put("is_aigc", false)
                })
            })
        }
        val fbResp = client.newCall(
            Request.Builder()
                .url("$BASE/tiktok/web/project/post/v1/?app_name=tiktok_web&channel=tiktok_web&device_platform=web&aid=$AID")
                .headers(baseHeaders())
                .addHeader("X-CSRFToken", csrf)
                .addHeader("X-Secsdk-Csrf-Request", "1")
                .addHeader("X-Secsdk-Csrf-Version", "1.2.8")
                .addHeader("Content-Type", "application/json")
                .post(fbBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
        ).execute()
        val fbRespBody = fbResp.body?.string() ?: throw Exception("Empty fallback response")
        Log.d(TAG, "Step5 fallback HTTP ${fbResp.code}: ${fbRespBody.take(300)}")

        val fbJson = JSONObject(fbRespBody)
        if (fbJson.optInt("status_code", -1) == 0)
            return@withContext fbJson.optJSONObject("data")?.optString("item_id", videoId) ?: videoId

        throw Exception("Publish failed: $fbRespBody")
    }

    suspend fun uploadAndPublish(
        videoBytes: ByteArray,
        caption: String,
        privacy: String,
        onProgress: (stage: String, percent: Int) -> Unit
    ): PublishResult = withContext(Dispatchers.IO) {
        onProgress("Getting credentials…", 0)
        val creds = getUploadAuth()

        onProgress("Reserving upload slot…", 5)
        val slot = applyUpload(creds, videoBytes.size.toLong())

        onProgress("Uploading video…", 10)
        uploadChunks(slot, videoBytes) { p -> onProgress("Uploading…", 10 + (p * 0.75).toInt()) }

        onProgress("Confirming upload…", 87)
        val videoId = commitUpload(creds, slot)

        onProgress("Publishing…", 93)
        val itemId = publishPost(videoId, caption, privacy)

        onProgress("Done!", 100)
        PublishResult(postId = itemId, status = "success")
    }

    private fun buildAuthHeader(
        creds: AwsCredentials,
        dateTimeStr: String,
        dateStr: String,
        path: String,
        query: String,
        bodyHash: String? = null,
        method: String = "GET"
    ): String {
        val sep = String(charArrayOf(10.toChar()))
        val signedHeaders = "x-amz-date;x-amz-security-token"
        val payloadHash = bodyHash ?: sha256Hex(ByteArray(0))
        val canonicalHeaders = "x-amz-date:$dateTimeStr${sep}x-amz-security-token:${creds.sessionToken}$sep"
        val canonicalRequest = "$method${sep}$path${sep}$query${sep}$canonicalHeaders${sep}$signedHeaders${sep}$payloadHash"
        val credentialScope = "$dateStr/${creds.region}/${creds.service}/aws4_request"
        val stringToSign = "AWS4-HMAC-SHA256${sep}$dateTimeStr${sep}$credentialScope${sep}${sha256Hex(canonicalRequest.toByteArray(Charsets.UTF_8))}"
        val signingKey = hmacSha256(
            hmacSha256(hmacSha256(hmacSha256(
                ("AWS4${creds.secretAccessKey}").toByteArray(), dateStr),
                creds.region), creds.service), "aws4_request"
        )
        val sig = hmacSha256Hex(signingKey, stringToSign)
        return "AWS4-HMAC-SHA256 Credential=${creds.accessKeyId}/$credentialScope, SignedHeaders=$signedHeaders, Signature=$sig"
    }

    private fun awsDateTime(date: Date): String =
        SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }.format(date)

    private fun hmacSha256(key: ByteArray, data: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data.toByteArray(Charsets.UTF_8))
    }

    private fun hmacSha256Hex(key: ByteArray, data: String): String =
        hmacSha256(key, data).joinToString("") { "%02x".format(it) }

    private fun sha256Hex(input: ByteArray): String =
        java.security.MessageDigest.getInstance("SHA-256")
            .digest(input).joinToString("") { "%02x".format(it) }
}