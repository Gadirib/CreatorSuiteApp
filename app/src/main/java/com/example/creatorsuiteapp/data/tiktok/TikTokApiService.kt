package com.example.creatorsuiteapp.data.tiktok

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

@SuppressLint("SetJavaScriptEnabled")
object TikTokApiService {

    private const val TAG = "TikTokApi"
    private val mainHandler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    private val pageLoaded = AtomicBoolean(false)
    private val callId = AtomicInteger(0)

    fun init(context: Context) {
        if (webView != null) { Log.d(TAG, "Already init"); return }
        pageLoaded.set(false)
        val wv = WebView(context.applicationContext)
        wv.settings.javaScriptEnabled = true
        wv.settings.domStorageEnabled = true
        wv.settings.userAgentString = TikTokSessionManager.UA
        wv.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(wv, true)
        wv.addJavascriptInterface(JsBridge, "_AppBridge_")
        wv.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                return !url.startsWith("http://") && !url.startsWith("https://")
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                if (url != null && url.startsWith("https://") && url.contains("tiktok.com") && !pageLoaded.get()) {
                    pageLoaded.set(true)
                    Log.d(TAG, "✅ WebView ready at $url")
                }
            }
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                if (request?.isForMainFrame == true) {
                    Log.e(TAG, "Error: ${error?.description}")
                    mainHandler.postDelayed({ view?.loadUrl("https://www.tiktok.com/foryou") }, 3000)
                }
            }
        }
        webView = wv
        wv.loadUrl("https://www.tiktok.com/foryou")
        Log.d(TAG, "WebView created")
    }

    fun destroy() { webView?.destroy(); webView = null; pageLoaded.set(false) }

    private fun getCookieValue(name: String): String =
        CookieManager.getInstance().getCookie("https://www.tiktok.com")
            ?.split(";")?.map { it.trim() }
            ?.firstOrNull { it.startsWith("$name=") }
            ?.substringAfter("$name=")?.trim() ?: ""

    private suspend fun runJs(js: String): String? {
        var waited = 0
        while (!pageLoaded.get() && waited < 25000) { delay(500); waited += 500 }
        if (!pageLoaded.get()) { Log.e(TAG, "Not ready"); return null }
        val id = "c${callId.getAndIncrement()}"
        val finalJs = js.replace("__ID__", id)
        return withTimeoutOrNull(20_000) {
            suspendCancellableCoroutine { cont ->
                JsBridge.register(id) { result -> if (cont.isActive) cont.resume(result) }
                mainHandler.post {
                    val wv = webView ?: run { JsBridge.unregister(id); if (cont.isActive) cont.resume(null); return@post }
                    wv.evaluateJavascript(finalJs, null)
                }
            }
        }.also {
            if (it == null) { Log.e(TAG, "[$id] timed out"); JsBridge.unregister(id) }
            else Log.d(TAG, "[$id] ${it.take(300)}")
        }
    }

    // ── getReposts ────────────────────────────────────────────────────────────
    suspend fun getReposts(secUid: String, cursor: Int = 0): JSONObject? {
        val encodedSecUid = URLEncoder.encode(secUid, "UTF-8")
        val msToken = getCookieValue("msToken")
        val params = buildString {
            append("secUid=$encodedSecUid&count=30&cursor=$cursor")
            append("&aid=1988&app_name=tiktok_web&device_platform=web_mobile&referer=&root_referer=")
            if (msToken.isNotEmpty()) append("&msToken=${URLEncoder.encode(msToken, "UTF-8")}")
        }
        val url = "https://www.tiktok.com/api/repost/item_list/?$params"
        val js = """
            (function() {
                fetch('$url', { credentials: 'include', headers: { 'Accept': 'application/json, text/plain, */*', 'Referer': 'https://www.tiktok.com/', 'sec-fetch-site': 'same-origin', 'sec-fetch-mode': 'cors', 'sec-fetch-dest': 'empty' } })
                .then(function(r){ return r.text(); })
                .then(function(t){ window._AppBridge_.onResult('__ID__', t || '{"empty":true}'); })
                .catch(function(e){ window._AppBridge_.onResult('__ID__', JSON.stringify({error:e.toString()})); });
            })();
        """.trimIndent()
        val result = runJs(js) ?: return null
        return try { JSONObject(result) } catch (e: Exception) { null }
    }

    // ── deleteRepost ──────────────────────────────────────────────────────────
    // ✅ CONFIRMED from DevTools interception:
    // - Endpoint: POST /tiktok/v1/upvote/delete
    // - item_id passed as URL QUERY PARAM (not in body)
    // - Body is EMPTY ""
    // - No X-CSRFToken header needed
    // - All browser fingerprint params in URL
    suspend fun deleteRepost(itemId: String): Boolean {
        val msToken = getCookieValue("msToken")
        val verifyFp = getCookieValue("s_v_web_id")

        // ✅ Exact params from real TikTok web request
        val params = buildString {
            append("aid=1988")
            append("&app_name=tiktok_web")
            append("&device_platform=web_mobile")
            append("&cookie_enabled=1")
            append("&data_collection_enabled=true")
            append("&focus_state=true")
            append("&from_page=video")
            append("&is_fullscreen=false")
            append("&is_page_visible=true")
            // ✅ item_id is in the URL, NOT in the body
            append("&item_id=$itemId")
            append("&os=android")
            append("&user_is_login=true")
            if (verifyFp.isNotEmpty()) append("&verifyFp=${URLEncoder.encode(verifyFp, "UTF-8")}")
            if (msToken.isNotEmpty()) append("&msToken=${URLEncoder.encode(msToken, "UTF-8")}")
        }

        val url = "https://www.tiktok.com/tiktok/v1/upvote/delete?$params"
        Log.d(TAG, "deleteRepost item_id=$itemId")

        // ✅ POST with EMPTY body — confirmed from DevTools
        // No X-CSRFToken header — TikTok doesn't require it for this endpoint
        val js = """
            (function() {
                fetch('$url', {
                    method: 'POST',
                    credentials: 'include',
                    headers: {
                        'sec-fetch-site': 'same-origin',
                        'sec-fetch-mode': 'cors',
                        'sec-fetch-dest': 'empty'
                    },
                    body: ''
                })
                .then(function(r) {
                    console.log('[DEL] HTTP ' + r.status);
                    return r.text();
                })
                .then(function(t) {
                    console.log('[DEL] ' + t.substring(0,200));
                    window._AppBridge_.onResult('__ID__', t || '{}');
                })
                .catch(function(e) {
                    console.log('[DEL] error: ' + e);
                    window._AppBridge_.onResult('__ID__', JSON.stringify({error:e.toString()}));
                });
            })();
        """.trimIndent()

        val result = runJs(js) ?: return false
        return try {
            val json = JSONObject(result)
            val code = json.optInt("status_code", json.optInt("statusCode", -1))
            Log.d(TAG, "deleteRepost code=$code msg=${json.optString("status_msg")}")
            code == 0
        } catch (e: Exception) {
            Log.e(TAG, "Parse error: $result")
            false
        }
    }

    object JsBridge {
        private val cbs = java.util.concurrent.ConcurrentHashMap<String, (String?) -> Unit>()
        fun register(id: String, cb: (String?) -> Unit) { cbs[id] = cb }
        fun unregister(id: String) { cbs.remove(id) }
        @JavascriptInterface
        fun onResult(id: String, json: String) {
            Log.d("AppBridge", "[$id] len=${json.length}: ${json.take(200)}")
            cbs.remove(id)?.invoke(json)
        }
    }
}