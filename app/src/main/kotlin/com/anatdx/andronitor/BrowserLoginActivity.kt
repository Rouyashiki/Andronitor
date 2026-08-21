package com.anatdx.andronitor

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback

class BrowserLoginActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private lateinit var progress: ProgressBar
    private lateinit var error: TextView

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser_login)

        webView = findViewById(R.id.login_web_view)
        progress = findViewById(R.id.login_progress)
        error = findViewById(R.id.login_error)

        findViewById<View>(R.id.login_back).setOnClickListener { goBackOrFinish() }
        findViewById<View>(R.id.login_use_session).setOnClickListener { returnSession() }

        val cookieManager = CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        webView.setBackgroundColor(Color.WHITE)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadsImagesAutomatically = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(false)
            useWideViewPort = true
            loadWithOverviewMode = false
            textZoom = 100
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            userAgentString = WebSettings.getDefaultUserAgent(this@BrowserLoginActivity)
                .replace("; wv", "")
                .replace("Version/4.0 ", "")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                isAlgorithmicDarkeningAllowed = false
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                forceDark = WebSettings.FORCE_DARK_OFF
            }
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                cookieManager.flush()
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progress.progress = newProgress
                progress.visibility = if (newProgress < 100) View.VISIBLE else View.INVISIBLE
            }
        }
        webView.loadUrl(LOGIN_URL)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goBackOrFinish()
        })
    }

    private fun returnSession() {
        val cookieManager = CookieManager.getInstance()
        val cookie = cookieManager.getCookie("https://x.com").orEmpty()
        if (!cookie.contains("auth_token=") || !cookie.contains("twid=")) {
            error.setText(R.string.login_first_error)
            error.visibility = View.VISIBLE
            return
        }
        cookieManager.flush()
        setResult(RESULT_OK, Intent().putExtra(EXTRA_COOKIE, cookie))
        finish()
    }

    private fun goBackOrFinish() {
        if (webView.canGoBack()) webView.goBack() else finish()
    }

    override fun onPause() {
        CookieManager.getInstance().flush()
        webView.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onDestroy() {
        webView.stopLoading()
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.destroy()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_COOKIE = "browser_login_cookie"
        private const val LOGIN_URL = "https://x.com/i/flow/login"
    }
}
