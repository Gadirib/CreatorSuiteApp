package com.example.tiktokclone.ui.screens.login

import android.util.Log
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.tiktokclone.data.tiktok.TikTokSessionManager
import com.example.tiktokclone.ui.theme.Pink40
import kotlinx.coroutines.delay

private const val UA = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    var showWebView by remember { mutableStateOf(false) }
    var pollingActive by remember { mutableStateOf(false) }
    var loginDetected by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black).padding(16.dp)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFEDEDED), RoundedCornerShape(22.dp))
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Close, null, tint = Color(0xFF2C2C2C), modifier = Modifier.size(26.dp))
                Spacer(Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Outlined.HelpOutline, null, tint = Color(0xFF999999), modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(42.dp))
            Text(
                "Log in to TikTok", color = Color(0xFF1F1F1F), fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Connect your real TikTok account to unlock full features.",
                color = Color(0xFF8F8F8F), fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { loginDetected = false; showWebView = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2E63))
            ) {
                Text("Connect TikTok", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                buildAnnotatedString {
                    append("Don't have an account? ")
                    withStyle(SpanStyle(color = Pink40, fontWeight = FontWeight.SemiBold)) { append("Sign up") }
                },
                color = Color(0xFF8A8A8A), fontSize = 14.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showWebView) {
        LaunchedEffect(Unit) {
            pollingActive = true
            var attempts = 0
            while (attempts < 90) {
                delay(2000)
                attempts++
                CookieManager.getInstance().flush()
                val sessionId = findSessionId()
                Log.d("TikTokPoll", "Attempt $attempts | sessionId=${sessionId != null} | loginDetected=$loginDetected")

                if (sessionId != null || loginDetected) {
                    Log.d("TikTokPoll", "🔑 Login signal! Waiting for cookies...")
                    delay(2000)
                    CookieManager.getInstance().flush()
                    val session = TikTokSessionManager.extractSessionInfo()
                    if (session != null) {
                        TikTokSessionManager.saveSession(context, session)
                        Log.d("TikTokPoll", "✅ Logged in as @${session.username}")
                        pollingActive = false
                        showWebView = false
                        onLoginSuccess()
                        return@LaunchedEffect
                    }
                    Log.d("TikTokPoll", "Session null, retrying...")
                }
            }
            pollingActive = false
            Toast.makeText(context, "Login timeout. Please try again.", Toast.LENGTH_LONG).show()
        }

        Dialog(
            onDismissRequest = { showWebView = false; pollingActive = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.White)) {

                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->

                        // ✅ FrameLayout holds BOTH WebViews
                        // mainWebView is always visible
                        // popupWebView sits on top and becomes visible when OAuth popup opens
                        val container = FrameLayout(ctx)

                        val matchParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        // ── POPUP WebView (for Google/Facebook OAuth window.open) ──────────
                        val popupWebView = WebView(ctx).apply {
                            layoutParams = matchParams
                            visibility = android.view.View.GONE // hidden until needed

                            settings.userAgentString = UA
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.setSupportMultipleWindows(true)
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    if (url == null) return
                                    Log.d("TikTokPopup", "Popup page: $url")
                                    CookieManager.getInstance().flush()

                                    // OAuth done: Google/Facebook redirected back to tiktok.com
                                    if (url.contains("tiktok.com") &&
                                        !url.contains("accounts.google") &&
                                        !url.contains("facebook.com") &&
                                        !url.contains("appleid.apple")
                                    ) {
                                        Log.d("TikTokPopup", "✅ OAuth returned to TikTok: $url")
                                        loginDetected = true
                                        // Hide popup, show main WebView again
                                        visibility = android.view.View.GONE
                                    }
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onCloseWindow(window: WebView?) {
                                    Log.d("TikTokPopup", "Popup closed by JS")
                                    CookieManager.getInstance().flush()
                                    loginDetected = true
                                    visibility = android.view.View.GONE
                                }
                            }
                        }

                        // ── MAIN WebView (TikTok login page) ─────────────────────────────
                        val mainWebView = WebView(ctx).apply {
                            layoutParams = matchParams

                            settings.userAgentString = UA
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            settings.loadsImagesAutomatically = true
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            settings.mediaPlaybackRequiresUserGesture = false
                            // ✅ Required for window.open() to fire onCreateWindow
                            settings.setSupportMultipleWindows(true)
                            settings.javaScriptCanOpenWindowsAutomatically = true

                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    if (url == null) return
                                    Log.d("TikTokMain", "Main page: $url")
                                    CookieManager.getInstance().flush()

                                    val onTikTok = url.contains("tiktok.com")
                                    val onLoginPage = url.contains("/login") ||
                                            url.contains("/signup") ||
                                            url.contains("phone-or-email") ||
                                            url.contains("/register")

                                    if (onTikTok && !onLoginPage) {
                                        Log.d("TikTokMain", "✅ Post-login page: $url")
                                        loginDetected = true
                                    }
                                }
                            }

                            // ✅ This is the key handler — fires when TikTok calls window.open()
                            // for Google/Facebook/Apple login
                            webChromeClient = object : WebChromeClient() {
                                override fun onCreateWindow(
                                    view: WebView?,
                                    isDialog: Boolean,
                                    isUserGesture: Boolean,
                                    resultMsg: android.os.Message?
                                ): Boolean {
                                    Log.d("TikTokPopup", "window.open() → showing popup WebView")

                                    // Show the popup WebView on top of the main one
                                    popupWebView.visibility = android.view.View.VISIBLE

                                    // Connect popup WebView to the JS message transport
                                    val transport = resultMsg?.obj as? WebView.WebViewTransport
                                    transport?.webView = popupWebView
                                    resultMsg?.sendToTarget()
                                    return true
                                }
                            }

                            loadUrl("https://www.tiktok.com/login/phone-or-email/email")
                        }

                        // Add both to container — main below, popup on top
                        container.addView(mainWebView)
                        container.addView(popupWebView)
                        container
                    }
                )

                if (pollingActive) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                        color = Color(0xFFFF2E63),
                        trackColor = Color.Transparent
                    )
                }

                IconButton(
                    onClick = { showWebView = false; pollingActive = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                ) {
                    Icon(Icons.Outlined.Close, null, tint = Color.White)
                }
            }
        }
    }
}

private fun findSessionId(): String? {
    val cm = CookieManager.getInstance()
    for (domain in listOf("https://www.tiktok.com", "https://tiktok.com", "https://m.tiktok.com")) {
        val cookies = cm.getCookie(domain) ?: continue
        val match = cookies.split(";")
            .map { it.trim() }
            .firstOrNull { it.startsWith("sessionid=") }
            ?.substringAfter("sessionid=")?.trim()
        if (!match.isNullOrEmpty()) {
            Log.d("TikTokPoll", "sessionid on $domain")
            return match
        }
    }
    return null
}