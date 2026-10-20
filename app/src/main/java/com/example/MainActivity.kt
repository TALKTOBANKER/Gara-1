package com.example

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.webkit.WebViewAssetLoader
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MyApplicationTheme
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {

    private var gameWebView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Google Mobile Ads SDK
        MobileAds.initialize(this) {
            AdManager.preloadInterstitial(this)
        }

        setContent {
            MyApplicationTheme {
                GarageGameScreen(
                    activity = this,
                    onWebViewCreated = { webView ->
                        gameWebView = webView
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        gameWebView?.onResume()
    }

    override fun onPause() {
        gameWebView?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        gameWebView?.destroy()
        gameWebView = null
        super.onDestroy()
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GarageGameScreen(
    activity: MainActivity,
    onWebViewCreated: (WebView) -> Unit
) {
    val context = LocalContext.current
    var webViewRef: WebView? = remember { null }

    BackHandler {
        // When hardware back button is pressed, close any modal/drawer in game if open
        webViewRef?.evaluateJavascript(
            "if(typeof window.closeAllPanels === 'function') { window.closeAllPanels(); }",
            null
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07111B))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Banner Ad placed at the top in its dedicated area:
        // Guaranteed NOT to cover game controls, bottom repair HUD or tabs.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(Color(0xFF0B1724))
                .testTag("admob_banner_container"),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.testTag("admob_banner_view"),
                factory = { ctx ->
                    AdView(ctx).apply {
                        setAdSize(AdSize.BANNER)
                        adUnitId = AdManager.BANNER_AD_UNIT_ID
                        loadAd(AdRequest.Builder().build())
                    }
                }
            )
        }

        // Fullscreen 3D WebGL Game Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("game_webview_container")
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("game_webview"),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        setBackgroundColor(0xFF07111B.toInt())
                        isClickable = true
                        isFocusable = true
                        isFocusableInTouchMode = true
                        requestFocus()

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            mediaPlaybackRequiresUserGesture = false
                            allowFileAccess = true
                            allowContentAccess = true
                            @Suppress("DEPRECATION")
                            allowFileAccessFromFileURLs = true
                            @Suppress("DEPRECATION")
                            allowUniversalAccessFromFileURLs = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                        }

                        val jsBridge = WebAppInterface(
                            activity = activity,
                            onCarDeliveredCallback = { doneCount ->
                                AdManager.showInterstitialIfAppropriate(activity, doneCount)
                            },
                            onStormTriggeredCallback = {
                                AdManager.preloadInterstitial(activity)
                            }
                        )
                        addJavascriptInterface(jsBridge, "AndroidBridge")

                        val assetLoader = WebViewAssetLoader.Builder()
                            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(ctx))
                            .build()

                        webViewClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                val uri = request?.url ?: return null
                                val urlStr = uri.toString()

                                // Intercept Three.js build module request to serve locally with CORS headers
                                if (urlStr.contains("three@0.160.0/build/three.module.js") || urlStr.endsWith("/three.module.js")) {
                                    return try {
                                        val stream = ctx.assets.open("game/js/three.module.js")
                                        val headers = mapOf(
                                            "Access-Control-Allow-Origin" to "*",
                                            "Access-Control-Allow-Methods" to "GET, OPTIONS",
                                            "Access-Control-Allow-Headers" to "*"
                                        )
                                        WebResourceResponse("application/javascript", "UTF-8", 200, "OK", headers, stream)
                                    } catch (e: Exception) {
                                        null
                                    }
                                }

                                // Intercept OrbitControls example module request to serve locally with CORS headers
                                if (urlStr.contains("OrbitControls.js")) {
                                    return try {
                                        val stream = ctx.assets.open("game/js/OrbitControls.js")
                                        val headers = mapOf(
                                            "Access-Control-Allow-Origin" to "*",
                                            "Access-Control-Allow-Methods" to "GET, OPTIONS",
                                            "Access-Control-Allow-Headers" to "*"
                                        )
                                        WebResourceResponse("application/javascript", "UTF-8", 200, "OK", headers, stream)
                                    } catch (e: Exception) {
                                        null
                                    }
                                }

                                // Serve local web game assets securely over HTTPS (bypassing file:// CORS restrictions)
                                val assetResponse = assetLoader.shouldInterceptRequest(uri)
                                if (assetResponse != null) {
                                    return assetResponse
                                }

                                return super.shouldInterceptRequest(view, request)
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                android.util.Log.d(
                                    "GameWebView",
                                    "${consoleMessage?.message()} -- line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}"
                                )
                                return super.onConsoleMessage(consoleMessage)
                            }
                        }

                        // Load game under official appassets.androidplatform.net HTTPS origin
                        loadUrl("https://appassets.androidplatform.net/assets/game/index.html")

                        webViewRef = this
                        onWebViewCreated(this)
                    }
                }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.destroy()
        }
    }
}
