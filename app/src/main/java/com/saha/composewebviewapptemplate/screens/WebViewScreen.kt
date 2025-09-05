package com.saha.composewebviewapptemplate.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.saha.composewebviewapptemplate.BuildConfig

// Data class to define content type
data class WebViewContent(
    val type: ContentType,
    val data: String,
    val baseUrl: String? = null
)

enum class ContentType {
    URL,        // Load from URL
    HTML,       // Load HTML string
    ASSET,      // Load from assets folder
    FILE        // Load from local file
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    content: WebViewContent,
    onPageFinished: ((String?) -> Unit)? = null,
    onPageStarted: ((String?) -> Unit)? = null
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // For file upload & camera
    var fileChooserCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    // Launcher for activity result (file/camera)
    val fileChooserLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val callback = fileChooserCallback
            var results: Array<Uri>? = null

            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                results = when {
                    data?.data != null -> arrayOf(data.data!!) // File picked
                    cameraImageUri != null -> arrayOf(cameraImageUri!!) // Camera photo
                    else -> null
                }
            }
            callback?.onReceiveValue(results)
            fileChooserCallback = null
            cameraImageUri = null
        }

    // Permissions (camera & storage)
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            // You can check granted/denied here if needed
        }

    // Ask for permissions on launch
    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.MODIFY_AUDIO_SETTINGS
            )
        )
    }

    // Back press handling
    BackHandler {
        webViewRef?.let { webView ->
            if (webView.canGoBack()) {
                webView.goBack()
            } else {
                (context as? Activity)?.finish()
            }
        } ?: run {
            (context as? Activity)?.finish()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.destroy()
        }
    }

    Scaffold { paddingValues ->
        Surface(modifier = Modifier.padding(paddingValues)) {
            if (isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                }
            }

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        webViewRef = this

                        // Enhanced WebView settings
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            cacheMode = WebSettings.LOAD_DEFAULT
                            
                            // Enhanced security and compatibility
                            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            allowFileAccess = true
                            allowContentAccess = true
                            
                            // Media and hardware acceleration
                            mediaPlaybackRequiresUserGesture = false
                            setGeolocationEnabled(true)
                            
                            // User agent (optional - can be customized)
                            userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36"
                            
                            // Additional settings for better compatibility
                            layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
                            
                            // Modern caching approach
                            setCacheMode(WebSettings.LOAD_DEFAULT)
                            
                            // Enable safe browsing (API 26+)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                safeBrowsingEnabled = true
                            }
                        }

                        // Enable cookies
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                if (newProgress == 100) isLoading = false
                            }

                            // Geolocation support
                            override fun onGeolocationPermissionsShowPrompt(
                                origin: String?,
                                callback: GeolocationPermissions.Callback?
                            ) {
                                callback?.invoke(origin, true, false)
                            }

                            // Permission requests (for WebRTC, etc.)
                            override fun onPermissionRequest(request: PermissionRequest?) {
                                request?.grant(request.resources)
                            }

                            // File chooser
                            override fun onShowFileChooser(
                                webView: WebView?,
                                filePathCallback: ValueCallback<Array<Uri>>?,
                                fileChooserParams: FileChooserParams?
                            ): Boolean {
                                fileChooserCallback?.onReceiveValue(null)
                                fileChooserCallback = filePathCallback

                                // Camera intent
                                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                val imageFile = File.createTempFile(
                                    "JPEG_${timeStamp}_",
                                    ".jpg",
                                    context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                                )
                                cameraImageUri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    imageFile
                                )
                                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                                    putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
                                }

                                // File chooser intent
                                val contentIntent = fileChooserParams?.createIntent()

                                // Combine
                                val chooserIntent = Intent(Intent.ACTION_CHOOSER).apply {
                                    putExtra(Intent.EXTRA_INTENT, contentIntent)
                                    putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
                                }

                                try {
                                    fileChooserLauncher.launch(chooserIntent)
                                } catch (e: ActivityNotFoundException) {
                                    fileChooserCallback = null
                                    return false
                                }
                                return true
                            }

                            // Console messages for debugging
                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                Log.d("WebView", "${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}")
                                return true
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                onPageFinished?.invoke(url)
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                                onPageStarted?.invoke(url)
                            }

                            // Enhanced SSL error handling (more secure)
                            @SuppressLint("WebViewClientOnReceivedSslError")
                            override fun onReceivedSslError(
                                view: WebView?,
                                handler: SslErrorHandler?,
                                error: android.net.http.SslError?
                            ) {
                                // Only proceed for development - remove in production
                                if (BuildConfig.DEBUG) {
                                    handler?.proceed()
                                } else {
                                    handler?.cancel()
                                }
                            }

                            // Handle resource loading
                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                return super.shouldInterceptRequest(view, request)
                            }
                        }

                        // Load content based on type
                        when (content.type) {
                            ContentType.URL -> {
                                loadUrl(content.data)
                            }
                            ContentType.HTML -> {
                                loadDataWithBaseURL(
                                    content.baseUrl,
                                    content.data,
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                            }
                            ContentType.ASSET -> {
                                loadUrl("file:///android_asset/${content.data}")
                            }
                            ContentType.FILE -> {
                                loadUrl("file://${content.data}")
                            }
                        }
                    }
                },
                update = { webView -> webViewRef = webView }
            )
        }
    }
}

// Convenience functions for easy usage
@Composable
fun WebViewScreen(url: String) {
    WebViewScreen(
        content = WebViewContent(
            type = ContentType.URL,
            data = url
        )
    )
}

@Composable
fun WebViewScreen(htmlContent: String, baseUrl: String? = null) {
    WebViewScreen(
        content = WebViewContent(
            type = ContentType.HTML,
            data = htmlContent,
            baseUrl = baseUrl
        )
    )
}