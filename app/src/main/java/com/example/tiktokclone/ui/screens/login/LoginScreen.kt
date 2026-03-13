package com.example.tiktokclone.ui.screens.login

import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.tiktokclone.R
import com.example.tiktokclone.data.tiktok.TikTokSessionManager
import com.example.tiktokclone.ui.theme.Pink40
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    var showWebView by remember { mutableStateOf(false) }
    var isCheckingSession by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // ── Your original beautiful UI ──
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFEDEDED), RoundedCornerShape(22.dp))
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close",
                    tint = Color(0xFF2C2C2C),
                    modifier = Modifier.size(26.dp)
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                    contentDescription = "Help",
                    tint = Color(0xFF999999),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(42.dp))

            Text(
                text = "Log in to TikTok",
                color = Color(0xFF1F1F1F),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Connect your real TikTok account to unlock full features.",
                color = Color(0xFF8F8F8F),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(20.dp))

            LoginMethodRow(iconRes = R.drawable.ic_phone, text = "Use phone / email / username")
            Spacer(modifier = Modifier.height(10.dp))
            LoginMethodRow(iconRes = R.drawable.ic_facebook, text = "Continue with Facebook")
            Spacer(modifier = Modifier.height(10.dp))
            LoginMethodRow(iconRes = R.drawable.ic_apple, text = "Continue with Apple")
            Spacer(modifier = Modifier.height(10.dp))
            LoginMethodRow(iconRes = R.drawable.ic_google, text = "Continue with Google")
            Spacer(modifier = Modifier.height(10.dp))
            LoginMethodRow(iconRes = R.drawable.ic_x, text = "Continue with X")
            Spacer(modifier = Modifier.height(10.dp))
            LoginMethodRow(iconRes = R.drawable.ic_instagram, text = "Continue with Instagram")

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { showWebView = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2E63))
            ) {
                Text("Connect TikTok", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = buildAnnotatedString {
                    append("Don't have an account? ")
                    withStyle(style = SpanStyle(color = Pink40, fontWeight = FontWeight.SemiBold)) {
                        append("Sign up")
                    }
                },
                color = Color(0xFF8A8A8A),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // ── Real TikTok login WebView in full-screen dialog ──
    if (showWebView) {
        Dialog(
            onDismissRequest = { showWebView = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            settings.allowFileAccess = true
                            settings.javaScriptCanOpenWindowsAutomatically = true
                            settings.setSupportMultipleWindows(true)
                            settings.userAgentString = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                            CookieManager.getInstance().let { cm ->
                                cm.setAcceptCookie(true)
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                                    cm.setAcceptThirdPartyCookies(this, true)
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    Log.d("TikTokLogin", "Page loaded: $url")

                                    isCheckingSession = true
                                    coroutineScope.launch {
                                        var attempts = 0
                                        while (attempts < 90 && showWebView) {  // ~3 dəqiqə max
                                            delay(2000)
                                            attempts++
                                            CookieManager.getInstance().flush() // Cookies sync
                                            val cookies = CookieManager.getInstance().getCookie("https://www.tiktok.com") ?: ""
                                            Log.d("TikTokLoginCookies", cookies.take(200))
                                            if (cookies.contains("sessionid") || cookies.contains("sessionid_ss")) {
                                                val session = try { TikTokSessionManager.extractSessionInfo() } catch (e: Exception) {
                                                    Log.e("TikTokLogin", "Session extraction failed", e)
                                                    null
                                                }
                                                if (session != null) {
                                                    TikTokSessionManager.saveSession(context, session)
                                                    showWebView = false
                                                    isCheckingSession = false
                                                    onLoginSuccess()
                                                    return@launch
                                                }
                                            }
                                        }
                                        isCheckingSession = false
                                        Toast.makeText(context, "Login timeout or failed. Try again.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                                    Log.d("WebViewJS", consoleMessage?.message() ?: "No message")
                                    return true
                                }
                            }

                            loadUrl("https://www.tiktok.com/login/phone-or-email/email")
                        }
                    }
                )

                // Loading overlay during session check
                if (isCheckingSession) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }

                // Close button
                IconButton(
                    onClick = { showWebView = false },
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

// Your LoginMethodRow remains unchanged
@Composable
private fun LoginMethodRow(iconRes: Int, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(Color(0xFFF0F0F0))
            .border(1.dp, Color(0xFFD9D9D9))
            .clickable { /* still fake */ },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(14.dp))
        Image(painterResource(iconRes), null, Modifier.size(24.dp))
        Spacer(Modifier.width(14.dp))
        Text(text, color = Color(0xFF2C2C2C), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
