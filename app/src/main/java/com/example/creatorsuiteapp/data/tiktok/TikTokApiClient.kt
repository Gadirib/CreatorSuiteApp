package com.example.creatorsuiteapp.data.tiktok

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class TikTokApiClient(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        // ✅ Don't follow redirects — if TikTok returns 302 we want to see it, not follow blindly
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    private val ua = TikTokSessionManager.UA

    // Must be called on Main thread
    private fun readCookies(): String =
        CookieManager.getInstance().getCookie("https://www.tiktok.com") ?: ""

    private fun extractCsrfToken(cookies: String): String =
        cookies.split(";").map { it.trim() }
            .firstOrNull { it.startsWith("tt_csrf_token=") }
            ?.substringAfter("tt_csrf_token=")?.trim() ?: ""

    private fun extractMsToken(cookies: String): String =
        cookies.split(";").map { it.trim() }
            .firstOrNull { it.startsWith("msToken=") }
            ?.substringAfter("msToken=")?.trim() ?: ""

    // ── Get reposts list ──────────────────────────────────────────────────────
    suspend fun getReposts(secUid: String, cursor: Int = 0): JSONObject? {
        val cookies = readCookies() // Main thread
        val msToken = extractMsToken(cookies)

        Log.d("TikTokApi", "cookies length=${cookies.length}, has sessionid=${cookies.contains("sessionid")}, has msToken=${msToken.isNotEmpty()}")

        if (!cookies.contains("sessionid")) {
            Log.e("TikTokApi", "No sessionid in cookies — user not logged in via WebView")
            return null
        }

        // ✅ msToken must be appended to URL as query param (TikTok API requirement)
        val url = "https://www.tiktok.com/api/repost/item_list/" +
                "?secUid=$secUid&count=30&cursor=$cursor" +
                (if (msToken.isNotEmpty()) "&msToken=$msToken" else "")

        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .get()
                .header("Cookie", cookies)
                .header("User-Agent", ua)
                .header("Referer", "https://www.tiktok.com/")
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "en-US,en;q=0.9")
                // ✅ These headers are required — TikTok checks them
                .header("sec-fetch-site", "same-origin")
                .header("sec-fetch-mode", "cors")
                .header("sec-fetch-dest", "empty")
                .build()

            try {
                val response = client.newCall(request).execute()
                val code = response.code
                val body = response.body?.string() ?: ""

                Log.d("TikTokApi", "getReposts HTTP $code, body length=${body.length}")
                Log.d("TikTokApi", "getReposts body preview: ${body.take(300)}")

                if (code == 302 || code == 301) {
                    val location = response.header("Location") ?: "unknown"
                    Log.e("TikTokApi", "Redirect to: $location — cookies may be invalid or missing")
                    return@withContext null
                }

                if (body.isEmpty()) {
                    Log.e("TikTokApi", "Empty body with HTTP $code")
                    return@withContext null
                }

                if (!body.startsWith("{")) {
                    Log.e("TikTokApi", "Response is not JSON: ${body.take(200)}")
                    return@withContext null
                }

                JSONObject(body)
            } catch (e: Exception) {
                Log.e("TikTokApi", "getReposts exception: ${e.message}")
                null
            }
        }
    }

    // ── Delete a repost ───────────────────────────────────────────────────────
    suspend fun deleteRepost(awemeId: String): Boolean {
        val cookies = readCookies() // Main thread
        val csrfToken = extractCsrfToken(cookies)

        if (!cookies.contains("sessionid")) {
            Log.e("TikTokApi", "deleteRepost: no sessionid")
            return false
        }

        return withContext(Dispatchers.IO) {
            val formBody = "aweme_id=$awemeId"
                .toRequestBody("application/x-www-form-urlencoded".toMediaType())

            val request = Request.Builder()
                .url("https://www.tiktok.com/tiktok/v1/upvote/delete")
                .post(formBody)
                .header("Cookie", cookies)
                .header("User-Agent", ua)
                .header("X-CSRFToken", csrfToken)
                .header("Referer", "https://www.tiktok.com/")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("sec-fetch-site", "same-origin")
                .header("sec-fetch-mode", "cors")
                .build()

            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: return@withContext false
                Log.d("TikTokApi", "deleteRepost $awemeId → HTTP ${response.code}: $body")
                JSONObject(body).optInt("statusCode", -1) == 0
            } catch (e: Exception) {
                Log.e("TikTokApi", "deleteRepost exception: ${e.message}")
                false
            }
        }
    }
}