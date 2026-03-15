package com.example.creatorsuiteapp.ui.screens.login

import android.util.Log
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.creatorsuiteapp.R
import com.example.creatorsuiteapp.data.tiktok.TikTokSessionManager
import com.example.creatorsuiteapp.ui.icons.AppIcons
import kotlinx.coroutines.delay

private const val UA = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

// TikTok's login page with all options: phone/email, Facebook, Google, Apple etc.
// This is what the user sees when they tap "Log in" in the real TikTok app
private const val TIKTOK_LOGIN_URL = "https://www.tiktok.com/login"

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    var showWebView   by remember { mutableStateOf(false) }
    var pollingActive by remember { mutableStateOf(false) }
    var loginDetected by remember { mutableStateOf(false) }

    // ── DARK ONBOARDING SCREEN ────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF070707))) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(96.dp))

            Image(painterResource(R.drawable.creator_logo), null, modifier = Modifier.size(172.dp))

            Spacer(Modifier.height(20.dp))

            Text("CREATOR", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Text("S U I T E", color = Color(0xFFFE2C55), fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 12.sp)

            Spacer(Modifier.height(12.dp))

            Text(
                "Record, Edit & Clean\nyour content",
                color = Color.White, textAlign = TextAlign.Center,
                fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(42.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OnboardingFeatureTile(Modifier.weight(1f), "Record Video", R.drawable.ic_record_video,
                    Brush.linearGradient(listOf(Color(0xCC114341), Color(0xCC0C2625)), androidx.compose.ui.geometry.Offset.Zero, androidx.compose.ui.geometry.Offset(154f, 48f)))
                OnboardingFeatureTile(Modifier.weight(1f), "Edit Content", R.drawable.ic_edit_content,
                    Brush.linearGradient(listOf(Color(0xCC261646), Color(0xCC140C26)), androidx.compose.ui.geometry.Offset.Zero, androidx.compose.ui.geometry.Offset(154f, 48f)))
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OnboardingFeatureTile(Modifier.weight(1f), "Add Effects", R.drawable.ic_add_effects,
                    Brush.linearGradient(listOf(Color(0xCC431111), Color(0xCC250000)), androidx.compose.ui.geometry.Offset.Zero, androidx.compose.ui.geometry.Offset(154f, 48f)))
                OnboardingFeatureTile(Modifier.weight(1f), "Clean Feed", R.drawable.ic_clean_feed,
                    Brush.linearGradient(listOf(Color(0xCC462E16), Color(0xCC7F2723)), androidx.compose.ui.geometry.Offset.Zero, androidx.compose.ui.geometry.Offset(154f, 48f)))
            }

            Spacer(Modifier.weight(1f))

            // ✅ Tap → opens real TikTok login page (all login options inside)
            Button(
                onClick = { loginDetected = false; showWebView = true },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFE2C55))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(AppIcons.IcUser22), null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Connect TikTok", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(Modifier.height(14.dp))

            TextButton(onClick = {}) {
                Text("SKIP FOR NOW", color = Color.White, fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 2.4.sp,
                    modifier = Modifier.padding(bottom = 4.dp))
            }
            Box(Modifier.height(1.dp).width(120.dp).background(Color.White))
            Spacer(Modifier.height(24.dp))
        }
    }

    // ── WEBVIEW: Real TikTok login page ───────────────────────────────────────
    if (showWebView) {

        // Polling coroutine — detects sessionid cookie after user logs in
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
                }
            }
            pollingActive = false
            Toast.makeText(context, "Login timeout. Please try again.", Toast.LENGTH_LONG).show()
        }

        Dialog(
            onDismissRequest = { showWebView = false; pollingActive = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(Modifier.fillMaxSize().background(Color.White)) {

                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val container = FrameLayout(ctx)
                        val matchParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        // Popup WebView handles OAuth windows (Google, Facebook etc)
                        val popupWebView = WebView(ctx).apply {
                            layoutParams = matchParams
                            visibility = android.view.View.GONE
                            settings.userAgentString = UA
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.setSupportMultipleWindows(true)
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    if (url == null) return
                                    CookieManager.getInstance().flush()
                                    Log.d("TikTokPopup", "Popup: $url")
                                    // OAuth done when redirected back to tiktok.com
                                    if (url.contains("tiktok.com") &&
                                        !url.contains("accounts.google") &&
                                        !url.contains("facebook.com") &&
                                        !url.contains("appleid.apple") &&
                                        !url.contains("twitter.com") &&
                                        !url.contains("instagram.com")) {
                                        loginDetected = true
                                        visibility = android.view.View.GONE
                                    }
                                }
                            }
                            webChromeClient = object : WebChromeClient() {
                                override fun onCloseWindow(window: WebView?) {
                                    CookieManager.getInstance().flush()
                                    loginDetected = true
                                    visibility = android.view.View.GONE
                                }
                            }
                        }

                        // Main WebView — loads the real TikTok login page
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
                            settings.setSupportMultipleWindows(true)
                            settings.javaScriptCanOpenWindowsAutomatically = true
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    if (url == null) return
                                    CookieManager.getInstance().flush()
                                    Log.d("TikTokMain", "Page: $url")
                                    val onTikTok = url.contains("tiktok.com")
                                    val onAuthPage = url.contains("/login") ||
                                            url.contains("/signup") ||
                                            url.contains("phone-or-email") ||
                                            url.contains("/register") ||
                                            url.contains("accounts.google") ||
                                            url.contains("facebook.com") ||
                                            url.contains("appleid.apple") ||
                                            url.contains("twitter.com") ||
                                            url.contains("instagram.com")
                                    if (onTikTok && !onAuthPage) {
                                        Log.d("TikTokMain", "✅ Post-login: $url")
                                        loginDetected = true
                                    }
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?): Boolean {
                                    Log.d("TikTokPopup", "window.open() — OAuth")
                                    popupWebView.visibility = android.view.View.VISIBLE
                                    val transport = resultMsg?.obj as? WebView.WebViewTransport
                                    transport?.webView = popupWebView
                                    resultMsg?.sendToTarget()
                                    return true
                                }
                            }

                            // ✅ Opens the real TikTok login page
                            // User sees: phone/email, Facebook, Google, Apple, Twitter, Instagram
                            loadUrl(TIKTOK_LOGIN_URL)
                        }

                        container.addView(mainWebView)
                        container.addView(popupWebView)
                        container
                    }
                )

                if (pollingActive) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                        color = Color(0xFFFF2E63), trackColor = Color.Transparent
                    )
                }

                IconButton(
                    onClick = { showWebView = false; pollingActive = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd).padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                ) {
                    Icon(Icons.Outlined.Close, null, tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun OnboardingFeatureTile(modifier: Modifier, title: String, iconRes: Int, gradient: Brush) {
    Box(modifier = modifier.height(48.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(13.dp)).background(gradient).border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(13.dp))) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Image(painterResource(iconRes), null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(Color.White))
            Spacer(Modifier.width(12.dp))
            Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

private fun findSessionId(): String? {
    val cm = CookieManager.getInstance()
    for (domain in listOf("https://www.tiktok.com", "https://tiktok.com", "https://m.tiktok.com")) {
        val cookies = cm.getCookie(domain) ?: continue
        val match = cookies.split(";").map { it.trim() }
            .firstOrNull { it.startsWith("sessionid=") }
            ?.substringAfter("sessionid=")?.trim()
        if (!match.isNullOrEmpty()) return match
    }
    return null
}