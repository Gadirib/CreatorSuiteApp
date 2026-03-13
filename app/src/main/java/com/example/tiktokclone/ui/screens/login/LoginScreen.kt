package com.example.tiktokclone.ui.screens.login

import android.content.Context
import android.util.Log
import android.webkit.*
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.tiktokclone.R
import com.example.tiktokclone.data.tiktok.TikTokSessionManager
import com.example.tiktokclone.ui.theme.Pink40
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {

    val context = LocalContext.current
    var showWebView by remember { mutableStateOf(false) }
    var isCheckingSession by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

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
                    contentDescription = null,
                    tint = Color(0xFF2C2C2C),
                    modifier = Modifier.size(26.dp)
                )

                Spacer(Modifier.weight(1f))

                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                    contentDescription = null,
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

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { showWebView = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2E63))
            ) {
                Text(
                    "Connect TikTok",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = buildAnnotatedString {
                    append("Don't have an account? ")
                    withStyle(
                        style = SpanStyle(
                            color = Pink40,
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
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

    if (showWebView) {

        Dialog(
            onDismissRequest = { showWebView = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {

                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->

                        WebView(ctx).apply {

                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true

                            settings.loadsImagesAutomatically = true
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true

                            settings.mixedContentMode =
                                WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            settings.userAgentString =
                                "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.allowContentAccess = true
                            settings.allowFileAccessFromFileURLs = true
                            settings.allowUniversalAccessFromFileURLs = true


                            val cookieManager = CookieManager.getInstance()

                            cookieManager.setAcceptCookie(true)

                            if (android.os.Build.VERSION.SDK_INT >= 21) {
                                cookieManager.setAcceptThirdPartyCookies(this, true)
                            }

                            webChromeClient = WebChromeClient()

                            webViewClient = object : WebViewClient() {

                                override fun onPageFinished(view: WebView?, url: String?) {

                                    Log.d("TikTokLogin", "Page: $url")

                                    if (!isCheckingSession) {

                                        isCheckingSession = true

                                        coroutineScope.launch {

                                            var attempts = 0

                                            while (attempts < 60 && showWebView) {

                                                delay(2000)

                                                attempts++

                                                CookieManager.getInstance().flush()

                                                if (checkTikTokSession(context)) {

                                                    showWebView = false

                                                    isCheckingSession = false

                                                    onLoginSuccess()

                                                    return@launch
                                                }
                                            }

                                            isCheckingSession = false

                                            Toast.makeText(
                                                context,
                                                "Login failed. Try again.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }

                                }
                            }

                            loadUrl(
                                "https://www.tiktok.com/login?lang=en"
                            )
                        }

                    }
                )

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

                IconButton(
                    onClick = { showWebView = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            RoundedCornerShape(50)
                        )
                ) {
                    Icon(Icons.Outlined.Close, null, tint = Color.White)
                }
            }
        }
    }
}


suspend fun checkTikTokSession(
    context: Context
): Boolean {

    val cookies =
        CookieManager.getInstance().getCookie("https://www.tiktok.com") ?: ""

    Log.d("TikTokCookies", cookies)

    if (cookies.contains("sessionid")) {

        val session = TikTokSessionManager.extractSessionInfo()

        if (session != null) {
            TikTokSessionManager.saveSession(context, session)
            return true
        }
    }

    return false
}