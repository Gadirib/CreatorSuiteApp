package com.example.creatorsuiteapp.data.tiktok

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

    fun clearSession(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun getCookies(): String {
        return CookieManager.getInstance().getCookie("https://www.tiktok.com") ?: ""
    }

    fun hasSession(): Boolean {
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
        Log.d("TikTokSession", "Has cookies, extracting session...")

        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        // ✅ Use /passport/web/account/info/ first — faster and more reliable than parsing HTML
        try {
            val accountRequest = Request.Builder()
                .url("https://www.tiktok.com/passport/web/account/info/")
                .header("Cookie", cookies)
                .header("User-Agent", UA)
                .header("Referer", "https://www.tiktok.com/")
                .build()

            val accountResponse = client.newCall(accountRequest).execute()
            val accountBody = accountResponse.body?.string() ?: ""
            Log.d("TikTokSession", "AccountInfo response: $accountBody")

            val accountJson = JSONObject(accountBody)
            val data = accountJson.optJSONObject("data")

            if (data != null) {
                val username = data.optString("username").takeIf { it.isNotEmpty() }

                if (username != null) {
                    // ✅ Now get secUid from profile HTML — correct JSON path from document
                    val secUid = fetchSecUid(client, cookies, username)

                    val avatar = data.optString("avatar_url").takeIf { it.isNotEmpty() }
                        ?: data.optString("avatar_larger").takeIf { it.isNotEmpty() }

                    Log.d("TikTokSession", "✅ username=$username secUid=$secUid")
                    return@withContext TikTokSession(username, secUid ?: "", avatar)
                }
            }
        } catch (e: Exception) {
            Log.e("TikTokSession", "AccountInfo failed, falling back to HTML parse", e)
        }

        // ✅ Fallback: parse HTML from tiktok.com homepage
        try {
            val htmlRequest = Request.Builder()
                .url("https://www.tiktok.com/")
                .header("Cookie", cookies)
                .header("User-Agent", UA)
                .build()

            val html = client.newCall(htmlRequest).execute().body?.string()
                ?: return@withContext null

            val scriptStart = html.indexOf("id=\"__UNIVERSAL_DATA_FOR_REHYDRATION__\"")

            if (scriptStart == -1) {
                Log.d("TikTokSession", "Universal data tag not found in HTML")
                return@withContext null
            }

            val jsonStart = html.indexOf("{", scriptStart)
            val jsonEnd = html.indexOf("</script>", jsonStart)

            if (jsonStart == -1 || jsonEnd == -1) return@withContext null

            val jsonStr = html.substring(jsonStart, jsonEnd).trim()
            val json = JSONObject(jsonStr)

            // ✅ FIXED: Correct JSON path — __DEFAULT_SCOPE__ → webapp.user-detail → userInfo → user
            // Previous code tried json.getJSONObject("userInfo") directly — WRONG, always threw exception
            val userInfo = json
                .getJSONObject("__DEFAULT_SCOPE__")
                .getJSONObject("webapp.user-detail")
                .getJSONObject("userInfo")
                .getJSONObject("user")

            val username = userInfo.getString("uniqueId")
            val secUid = userInfo.getString("secUid")
            var avatar = userInfo.optString("avatarLarger").takeIf { it.isNotEmpty() }
                ?: userInfo.optString("avatarMedium").takeIf { it.isNotEmpty() }
                ?: userInfo.optString("avatarThumb").takeIf { it.isNotEmpty() }

            Log.d("TikTokSession", "✅ (HTML fallback) username=$username")
            TikTokSession(username, secUid, avatar)

        } catch (e: Exception) {
            Log.e("TikTokSession", "HTML fallback also failed", e)
            null
        }
    }

    // ✅ Fetch secUid by parsing profile page — exact path from document §3 step 6
    private suspend fun fetchSecUid(
        client: OkHttpClient,
        cookies: String,
        username: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://www.tiktok.com/@$username")
                .header("Cookie", cookies)
                .header("User-Agent", UA)
                .build()

            val html = client.newCall(request).execute().body?.string() ?: return@withContext null

            val scriptStart = html.indexOf("id=\"__UNIVERSAL_DATA_FOR_REHYDRATION__\"")
            if (scriptStart == -1) return@withContext null

            val jsonStart = html.indexOf("{", scriptStart)
            val jsonEnd = html.indexOf("</script>", jsonStart)
            if (jsonStart == -1 || jsonEnd == -1) return@withContext null

            val json = JSONObject(html.substring(jsonStart, jsonEnd).trim())

            // ✅ Path from document: __DEFAULT_SCOPE__ → webapp.user-detail → userInfo → user → secUid
            json.getJSONObject("__DEFAULT_SCOPE__")
                .getJSONObject("webapp.user-detail")
                .getJSONObject("userInfo")
                .getJSONObject("user")
                .getString("secUid")

        } catch (e: Exception) {
            Log.e("TikTokSession", "fetchSecUid failed", e)
            null
        }
    }
}
