package com.example.tiktokclone.data.tiktok

import android.content.Context
import android.webkit.CookieManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class TikTokSession(
    val username: String,
    val secUid: String,
    val avatarUrl: String?
)

object TikTokSessionManager {

    private const val PREF_NAME = "tiktok_session"

    const val UA =
        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    fun isLoggedIn(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString("username", null) != null
    }

    fun getSession(context: Context): TikTokSession? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        val username = prefs.getString("username", null) ?: return null
        val secUid = prefs.getString("secUid", null) ?: return null
        val avatar = prefs.getString("avatarUrl", null)

        return TikTokSession(username, secUid, avatar)
    }

    fun saveSession(context: Context, session: TikTokSession) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .putString("username", session.username)
            .putString("secUid", session.secUid)
            .putString("avatarUrl", session.avatarUrl)
            .apply()
    }

    private fun getCookies(): String {
        return CookieManager.getInstance().getCookie("https://www.tiktok.com") ?: ""
    }

    private fun hasSession(): Boolean {
        val cookies = getCookies()
        return cookies.contains("sessionid") ||
                cookies.contains("sessionid_ss") ||
                cookies.contains("sid_tt")
    }

    suspend fun extractSessionInfo(): TikTokSession? = withContext(Dispatchers.IO) {

        if (!hasSession()) {
            Log.d("TikTokSession", "No session cookie found")
            return@withContext null
        }

        val cookies = getCookies()

        Log.d("TikTokSession", "Cookies: $cookies")

        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("https://www.tiktok.com/")
            .header("Cookie", cookies)
            .header("User-Agent", UA)
            .build()

        try {

            val html = client.newCall(request).execute().body?.string() ?: return@withContext null

            val scriptStart = html.indexOf("id=\"__UNIVERSAL_DATA_FOR_REHYDRATION__\"")

            if (scriptStart == -1) {
                Log.d("TikTokSession", "Universal data not found")
                return@withContext null
            }

            val jsonStart = html.indexOf("{", scriptStart)
            val jsonEnd = html.indexOf("</script>", jsonStart)

            val jsonStr = html.substring(jsonStart, jsonEnd).trim()

            val json = JSONObject(jsonStr)

            val userInfo = json
                .getJSONObject("userInfo")
                .getJSONObject("user")

            val username = userInfo.getString("uniqueId")
            val secUid = userInfo.getString("secUid")

            var avatar = userInfo.optString("avatarLarger")

            if (avatar.isNullOrEmpty()) avatar = userInfo.optString("avatarMedium")
            if (avatar.isNullOrEmpty()) avatar = userInfo.optString("avatarThumb")

            Log.d("TikTokSession", "USERNAME: $username")
            Log.d("TikTokSession", "SECUID: $secUid")

            TikTokSession(username, secUid, avatar)

        } catch (e: Exception) {

            Log.e("TikTokSession", "Session extract error", e)
            null

        }
    }
}