package com.example.tiktokclone.data.tiktok
import android.content.Context
import android.webkit.CookieManager
import kotlinx.coroutines.delay
import okhttp3.*
import org.json.JSONObject
import kotlin.random.Random

class TikTokApiClient(private val context: Context) {
    private val client = OkHttpClient()
    private val ua = TikTokSessionManager.UA // тот же UA

    private fun getCookies() = CookieManager.getInstance().getCookie("https://www.tiktok.com") ?: ""
    private fun getCsrfToken(): String {
        val cookies = getCookies()
        return cookies.split(";")
            .find { it.trim().startsWith("tt_csrf_token=") }
            ?.substringAfter("=") ?: ""
    }

    // Получить список репостов (с пагинацией)
    suspend fun getReposts(secUid: String, cursor: Int = 0): JSONObject? {
        val url = "https://www.tiktok.com/api/repost/item_list/?secUid=$secUid&count=30&cursor=$cursor"
        val request = Request.Builder()
            .url(url)
            .header("Cookie", getCookies())
            .header("User-Agent", ua)
            .build()

        return try {
            val response = client.newCall(request).execute()
            JSONObject(response.body?.string() ?: "{}")
        } catch (e: Exception) {
            null
        }
    }

    // Удалить репост (с рандомной задержкой 1-3 сек как в документе)
    suspend fun deleteRepost(awemeId: String) {
        val url = "https://www.tiktok.com/tiktok/v1/upvote/delete"
        val body = FormBody.Builder().add("aweme_id", awemeId).build()

        val request = Request.Builder()
            .url(url)
            .post(body)
            .header("Cookie", getCookies())
            .header("User-Agent", ua)
            .header("X-CSRFToken", getCsrfToken())
            .build()

        client.newCall(request).execute()
        // Rate limit из документа
        delay(Random.nextLong(1000, 3000))
    }
}