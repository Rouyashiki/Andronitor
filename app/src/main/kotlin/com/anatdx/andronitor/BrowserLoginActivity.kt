@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.anatdx.andronitor

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.anatdx.andronitor.data.SettingsStore
import com.anatdx.andronitor.ui.component.WavyLinearProgress
import com.anatdx.andronitor.ui.theme.AndronitorTheme

class BrowserLoginActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private var loadProgress by mutableIntStateOf(0)
    private var errorMessage by mutableStateOf<String?>(null)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        val cookieManager = CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        // X's login flow is a light-themed web page, so the WebView keeps a white
        // backdrop regardless of the app theme.
        webView.setBackgroundColor(AndroidColor.WHITE)
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
                loadProgress = newProgress
            }
        }
        webView.loadUrl(LOGIN_URL)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goBackOrFinish()
        })

        val settings = SettingsStore(this).load()
        setContent {
            AndronitorTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.dynamicColor,
            ) {
                LoginContent(
                    webView = webView,
                    progress = loadProgress,
                    error = errorMessage,
                    onErrorShown = { errorMessage = null },
                    onBack = ::goBackOrFinish,
                    onUseSession = ::returnSession,
                )
            }
        }
    }

    private fun returnSession() {
        val cookieManager = CookieManager.getInstance()
        val cookie = cookieManager.getCookie("https://x.com").orEmpty()
        if (!cookie.contains("auth_token=") || !cookie.contains("twid=")) {
            errorMessage = getString(R.string.login_first_error)
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

@Composable
private fun LoginContent(
    webView: WebView,
    progress: Int,
    error: String?,
    onErrorShown: () -> Unit,
    onBack: () -> Unit,
    onUseSession: () -> Unit,
) {
    val snackbarHost = remember { SnackbarHostState() }
    if (error != null) {
        LaunchedEffect(error) {
            snackbarHost.showSnackbar(error)
            onErrorShown()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.browser_login)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            stringResource(R.string.browser_back),
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onUseSession) {
                        Text(stringResource(R.string.use_session))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // The WebView is owned by the activity so its lifecycle callbacks and
            // cookie flushing keep working exactly as before.
            AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
            AnimatedVisibility(visible = progress in 1..99) {
                WavyLinearProgress(progress = { progress / 100f })
            }
        }
    }
}
