package com.example.tiktokclone.ui.screens.cleaner

import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun WebRepostCleanerScreen(
    username: String,           // real username without @
    onBack: () -> Unit
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var detectedCount by remember { mutableIntStateOf(0) }
    var autoRemoveEnabled by remember { mutableStateOf(false) }

    // Load saved cookies once when composable enters composition
    LaunchedEffect(Unit) {
        val savedCookies = CookieManager.getInstance().getCookie("https://www.tiktok.com")
        if (savedCookies != null && savedCookies.isNotEmpty()) {
            CookieManager.getInstance().setCookie("https://www.tiktok.com", savedCookies)
            CookieManager.getInstance().flush()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Slow Auto-Remove (15s delay)",
                color = if (autoRemoveEnabled) Color(0xFFFF2E63) else Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Switch(
                checked = autoRemoveEnabled,
                onCheckedChange = { autoRemoveEnabled = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFFFF2E63),
                    checkedTrackColor = Color(0xFFFF2E63).copy(alpha = 0.5f)
                )
            )
        }

        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Build/UP1A.231005.007) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    }

                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun reportRepostCount(count: Int) {
                            detectedCount = count
                        }
                    }, "AndroidBridge")

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean = false

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)

                            view?.evaluateJavascript("""
                        (function() {
                            let items = document.querySelectorAll(
                                '[data-e2e="user-post-item"], ' +
                                'div[data-e2e*="repost"], ' +
                                'article[data-e2e="video-card"], ' +
                                'div.tiktok-1s72l6s-DivVideoCard'
                            );
                            let count = items.length || 0;
                            AndroidBridge.reportRepostCount(count);
                        })();
                    """.trimIndent(), null)
                        }
                    }

                    // Reuse login cookies
                    val savedCookies = CookieManager.getInstance().getCookie("https://www.tiktok.com")
                    if (savedCookies != null && savedCookies.isNotEmpty()) {
                        CookieManager.getInstance().setCookie("https://www.tiktok.com", savedCookies)
                        CookieManager.getInstance().flush()
                    }

                    loadUrl("https://www.tiktok.com/@$username?tab=reposts")
                }
            },
            modifier = Modifier.weight(1f),
            update = { webViewRef = it }
        )
        // Bottom controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Detected visible reposts: $detectedCount (scroll manually to load more)",
                color = Color(0xFF7C8096),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = onBack) {
                    Text("Close")
                }
                Button(onClick = { webViewRef?.reload() }) {
                    Text("Reload")
                }
                Button(onClick = { webViewRef?.evaluateJavascript("window.scrollTo(0, document.body.scrollHeight);", null) }) {
                    Text("Scroll Down")
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { webViewRef?.destroy() }
    }
}