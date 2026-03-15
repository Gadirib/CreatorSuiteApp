package com.example.creatorsuiteapp.data.tiktok

import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import android.util.Log

object TikTokApi {

    private val client = OkHttpClient()

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    fun getAccountInfo(cookie: String) {
        val request = Request.Builder()
            .url("https://www.tiktok.com/passport/web/account/info/")
            .addHeader("Cookie", cookie)
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("Referer", "https://www.tiktok.com/")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("AccountInfo", "request failed", e)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: return
                Log.d("AccountInfoRaw", body)

                try {
                    val json = JSONObject(body)
                    val data = json.getJSONObject("data")
                    val username = data.getString("username")
                    Log.d("TikTokUsername", username)
                    getSecUid(username)
                } catch (e: Exception) {
                    Log.e("AccountInfoParse", "error", e)
                }
            }
        })
    }

    fun getSecUid(username: String) {
        val request = Request.Builder()
            .url("https://www.tiktok.com/@$username")
            .addHeader("User-Agent", USER_AGENT)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("SecUid", "request failed", e)
            }

            override fun onResponse(call: Call, response: Response) {
                val html = response.body?.string() ?: return

                try {
                    val start = html.indexOf("id=\"__UNIVERSAL_DATA_FOR_REHYDRATION__\"")
                    if (start == -1) {
                        Log.e("SecUidParse", "UNIVERSAL_DATA tag not found")
                        return
                    }

                    val jsonStart = html.indexOf("{", start)
                    val jsonEnd = html.indexOf("</script>", jsonStart)
                    val jsonString = html.substring(jsonStart, jsonEnd)

                    val json = JSONObject(jsonString)

                    // ✅ FIXED path: __DEFAULT_SCOPE__ → webapp.user-detail → userInfo → user
                    val userInfo = json
                        .getJSONObject("__DEFAULT_SCOPE__")
                        .getJSONObject("webapp.user-detail")
                        .getJSONObject("userInfo")
                        .getJSONObject("user")

                    val secUid = userInfo.getString("secUid")
                    Log.d("TikTokSecUid", secUid)

                } catch (e: Exception) {
                    Log.e("SecUidParse", "parse error", e)
                }
            }
        })
    }
}
