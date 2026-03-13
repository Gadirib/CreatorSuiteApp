package com.example.tiktokclone.data.tiktok

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient

object TikTokLoginManager {

    fun startLogin(activity: Activity) {

        val webView = WebView(activity)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.userAgentString =
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

        webView.webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: WebView?, url: String?) {

                val cookies = CookieManager.getInstance()
                    .getCookie("https://www.tiktok.com")

                if (cookies != null && cookies.contains("sessionid")) {

                    val sessionId = Regex("sessionid=([^;]+)")
                        .find(cookies)?.groupValues?.get(1)

                    activity.getSharedPreferences("tiktok", Context.MODE_PRIVATE)
                        .edit()
                        .putString("sessionid", sessionId)
                        .apply()

                    (webView.parent as? ViewGroup)?.removeView(webView)
                }
            }
        }

        webView.loadUrl("https://www.tiktok.com/login/phone-or-email/email")

        activity.addContentView(
            webView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }
}