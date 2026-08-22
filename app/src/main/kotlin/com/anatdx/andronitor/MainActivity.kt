package com.anatdx.andronitor

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.anatdx.andronitor.ui.AndronitorApp

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-edge is applied by AndronitorTheme, which also knows whether
        // the resolved theme is dark and can pick the system bar icon colors.
        setContent { AndronitorApp() }
    }
}
