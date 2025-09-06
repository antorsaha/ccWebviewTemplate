@file:Suppress("LABEL_NAME_CLASH")

package com.saha.composewebviewapptemplate.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import com.saha.composewebviewapptemplate.BuildConfig
import com.saha.composewebviewapptemplate.R
import com.saha.composewebviewapptemplate.dialogs.ErrorDialog
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Data class to define content type
data class WebViewContent(
    val type: ContentType, val data: String, val baseUrl: String? = null
)

enum class ContentType {
    URL,        // Load from URL
    HTML,       // Load HTML string
    ASSET,      // Load from assets folder
    FILE        // Load from local file
}

// Error types
enum class WebViewError {
    NO_INTERNET, CONNECTION_FAILED, TIMEOUT, SSL_ERROR, UNKNOWN
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    content: WebViewContent,
    onPageFinished: ((String?) -> Unit)? = null,
    onPageStarted: ((String?) -> Unit)? = null,
    onError: ((WebViewError, String) -> Unit)? = null
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var showWebView by remember { mutableStateOf(true) }
    var errorType by remember { mutableStateOf(WebViewError.UNKNOWN) }
    var errorMessage by remember { mutableStateOf("") }
    var retryCount by remember { mutableStateOf(0) }
    var isRetrying by remember { mutableStateOf(false) }
    var hasShownError by remember { mutableStateOf(false) }
    val maxRetries = 5 // Increased retries for better resilience

    // For file upload & camera
    var fileChooserCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    // Enhanced internet connection check
    fun isInternetAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    // Only show critical errors to users
    fun showError(error: WebViewError, message: String) {
        hasShownError = true
        errorType = error
        errorMessage = message
        showErrorDialog = true
        showWebView = false
        isLoading = false
        isRetrying = false
        onError?.invoke(error, message)
    }

    // Reset error state for new loads
    fun resetErrorState() {
        hasShownError = false
        retryCount = 0
        isRetrying = false
    }

    // Silent retry mechanism using LaunchedEffect
    LaunchedEffect(retryCount, isRetrying) {
        if (retryCount > 0 && retryCount < maxRetries && !isRetrying && !hasShownError) {
            isRetrying = true
            
            // Exponential backoff with jitter
            val baseDelay = 1000L
            val jitter = (0..500).random().toLong()
            val delayMs = (baseDelay * Math.pow(1.5, (retryCount - 1).toDouble())).toLong() + jitter
            
            delay(delayMs)
            webViewRef?.reload()
            isRetrying = false
        } else if (retryCount >= maxRetries && !hasShownError) {
            // Only show error after all silent retries failed
            showError(WebViewError.CONNECTION_FAILED, 
                context.getString(R.string.unable_to_connect_to_website_please_check_your_internet_connection_and_try_again))
        }
    }

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

    // Check internet connection before loading
    LaunchedEffect(content) {
        if (content.type == ContentType.URL && !isInternetAvailable()) {
            showError(
                WebViewError.NO_INTERNET,
                context.getString(R.string.no_internet_connection_please_check_your_network_settings_and_try_again)
            )
        }
    }

    // Back press handling
    BackHandler {
        if (showErrorDialog) {
            showErrorDialog = false
        } else {
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
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.destroy()
        }
    }

    Scaffold { paddingValues ->
        Surface(modifier = Modifier.padding(paddingValues)) {
            // Show loading indicator only when loading and no error
            if (isLoading && !showErrorDialog && showWebView) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    Text(
                        text = if (isRetrying) stringResource(R.string.connecting) else stringResource(R.string.loading),
                        modifier = Modifier.padding(top = 16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            // Only show WebView when there's no error
            if (showWebView) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            webViewRef = this

                            // Enhanced WebView settings for maximum reliability
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false
                                
                                // Enhanced caching and performance
                                cacheMode = WebSettings.LOAD_DEFAULT
                                
                                // Enhanced security and compatibility
                                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                allowFileAccess = true
                                allowContentAccess = true

                                
                                // Media and hardware acceleration
                                mediaPlaybackRequiresUserGesture = false
                                setGeolocationEnabled(true)
                                
                                // Enhanced user agent
                                userAgentString = buildString {
                                    append("Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}; Mobile) ")
                                    append("AppleWebKit/537.36 (KHTML, like Gecko) ")
                                    append("Chrome/120.0.0.0 Mobile Safari/537.36")
                                }
                                
                                // Additional settings for better compatibility
                                layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
                                
                                // Enable safe browsing (API 26+)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    safeBrowsingEnabled = true
                                }
                                
                                // Enhanced timeout settings
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    loadsImagesAutomatically = true
                                }
                            }

                            // Enable cookies with better management
                            CookieManager.getInstance().apply {
                                setAcceptCookie(true)
                                flush()
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    super.onProgressChanged(view, newProgress)
                                    if (newProgress == 100) {
                                        isLoading = false
                                        isRetrying = false
                                        retryCount = 0 // Reset retry count on successful load
                                        hasShownError = false // Reset error state on success
                                    }
                                }

                                // Geolocation support
                                override fun onGeolocationPermissionsShowPrompt(
                                    origin: String?, callback: GeolocationPermissions.Callback?
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
                                    val timeStamp =
                                        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                    val imageFile = File.createTempFile(
                                        "JPEG_${timeStamp}_",
                                        ".jpg",
                                        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                                    )
                                    cameraImageUri = FileProvider.getUriForFile(
                                        context, "${context.packageName}.provider", imageFile
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
                                    Log.d(
                                        "WebView",
                                        context.getString(R.string.console_message_from_line,
                                            consoleMessage?.message() ?: "",
                                            consoleMessage?.lineNumber() ?: 0,
                                            consoleMessage?.sourceId() ?: ""
                                        )
                                    )
                                    return true
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    isRetrying = false
                                    resetErrorState() // Reset error state on successful load
                                    onPageFinished?.invoke(url)
                                }

                                override fun onPageStarted(
                                    view: WebView?,
                                    url: String?,
                                    favicon: Bitmap?
                                ) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                    onPageStarted?.invoke(url)
                                }

                                // Optimized error handling - only show critical errors
                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    super.onReceivedError(view, request, error)
                                    val errorCode = error?.errorCode ?: -1
                                    
                                    // Only handle main frame errors, ignore resource errors
                                    if (request?.isForMainFrame == true) {
                                        when (errorCode) {
                                            ERROR_HOST_LOOKUP -> {
                                                // Silent retry for host lookup errors
                                                Log.w("WebView", context.getString(R.string.host_lookup_failed_attempting_silent_retry))
                                                retryCount++
                                            }
                                            ERROR_CONNECT -> {
                                                // Silent retry for connection errors
                                                Log.w("WebView", context.getString(R.string.connection_failed_attempting_silent_retry))
                                                retryCount++
                                            }
                                            ERROR_TIMEOUT -> {
                                                // Silent retry for timeout errors
                                                Log.w("WebView", context.getString(R.string.connection_timeout_attempting_silent_retry))
                                                retryCount++
                                            }
                                            ERROR_IO -> {
                                                // Silent retry for IO errors
                                                Log.w("WebView", context.getString(R.string.io_error_attempting_silent_retry))
                                                retryCount++
                                            }
                                            ERROR_BAD_URL -> {
                                                // Show error immediately for bad URLs
                                                showError(WebViewError.UNKNOWN, 
                                                    context.getString(R.string.invalid_website_address_please_check_the_url_and_try_again))
                                            }
                                            ERROR_FILE_NOT_FOUND -> {
                                                // Silent retry for 404 errors (might be temporary)
                                                Log.w("WebView", context.getString(R.string.file_not_found_attempting_silent_retry))
                                                retryCount++
                                            }
                                            ERROR_TOO_MANY_REQUESTS -> {
                                                // Silent retry with longer delay for rate limiting
                                                Log.w("WebView", context.getString(R.string.too_many_requests_attempting_silent_retry_with_delay))
                                                retryCount++
                                            }
                                            ERROR_UNSAFE_RESOURCE -> {
                                                // Show error for unsafe resources
                                                showError(WebViewError.SSL_ERROR,
                                                    context.getString(R.string.unsafe_content_blocked_for_security)
                                                )
                                            }
                                            else -> {
                                                // Silent retry for unknown errors
                                                Log.w("WebView", context.getString(R.string.unknown_error_attempting_silent_retry, errorCode))
                                                retryCount++
                                            }
                                        }
                                    } else {
                                        // Log resource errors but don't show to user
                                        Log.d("WebView", context.getString(R.string.resource_error_ignored, errorCode))
                                    }
                                }

                                // Enhanced SSL error handling
                                @SuppressLint("WebViewClientOnReceivedSslError")
                                override fun onReceivedSslError(
                                    view: WebView?,
                                    handler: SslErrorHandler?,
                                    error: SslError?
                                ) {
                                    if (BuildConfig.DEBUG) {
                                        handler?.proceed()
                                    } else {
                                        // Only show SSL errors for critical issues
                                        val sslError = error?.primaryError ?: -1
                                        when (sslError) {
                                            SslError.SSL_UNTRUSTED,
                                            SslError.SSL_EXPIRED,
                                            SslError.SSL_IDMISMATCH -> {
                                                showError(WebViewError.SSL_ERROR, 
                                                    context.getString(R.string.security_certificate_error_connection_not_secure))
                                                handler?.cancel()
                                            }
                                            else -> {
                                                // For other SSL errors, proceed silently
                                                handler?.proceed()
                                            }
                                        }
                                    }
                                }

                                // Enhanced resource loading with error recovery
                                override fun shouldInterceptRequest(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): WebResourceResponse? {
                                    return try {
                                        super.shouldInterceptRequest(view, request)
                                    } catch (e: Exception) {
                                        Log.w("WebView", context.getString(R.string.error_intercepting_request, e.message ?: ""))
                                        null
                                    }
                                }
                            }

                            // Load content based on type with enhanced error handling
                            try {
                                when (content.type) {
                                    ContentType.URL -> {
                                        if (isInternetAvailable()) {
                                            loadUrl(content.data)
                                        } else {
                                            showError(WebViewError.NO_INTERNET, 
                                                context.getString(R.string.no_internet_connection_please_check_your_network_settings_and_try_again))
                                        }
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
                            } catch (e: Exception) {
                                Log.e("WebView", context.getString(R.string.error_loading_content, e.message ?: ""))
                                retryCount++
                            }
                        }
                    },
                    update = { webView -> webViewRef = webView }
                )
            }

            // Error Dialog - only shown for critical errors
            if (showErrorDialog) {
                ErrorDialog(
                    title = when (errorType) {
                        WebViewError.NO_INTERNET -> stringResource(R.string.no_internet_connection)
                        WebViewError.CONNECTION_FAILED -> stringResource(R.string.connection_failed)
                        WebViewError.TIMEOUT -> stringResource(R.string.connection_timeout)
                        WebViewError.SSL_ERROR -> stringResource(R.string.security_error)
                        WebViewError.UNKNOWN -> stringResource(R.string.loading_error)
                    },
                    message = errorMessage,
                    isCancellable = true,
                    onDismissRequest = {
                        showErrorDialog = false
                        showWebView = true
                        resetErrorState()
                    },
                    actionButtonText = stringResource(R.string.retry),
                    onActionButtonClick = {
                        showErrorDialog = false
                        showWebView = true
                        resetErrorState()
                        webViewRef?.reload()
                    },
                )
            }
        }
    }
}

// Convenience functions for easy usage
@Composable
fun WebViewScreen(url: String) {
    WebViewScreen(
        content = WebViewContent(
            type = ContentType.URL, data = url
        )
    )
}

@Composable
fun WebViewScreen(htmlContent: String, baseUrl: String? = null) {
    WebViewScreen(
        content = WebViewContent(
            type = ContentType.HTML, data = htmlContent, baseUrl = baseUrl
        )
    )
}