package com.saha.composewebviewapptemplate

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.saha.composewebviewapptemplate.screens.WebViewScreen
import com.saha.composewebviewapptemplate.screens.WebViewContent
import com.saha.composewebviewapptemplate.screens.ContentType
import com.saha.composewebviewapptemplate.screens.WebViewError
import com.saha.composewebviewapptemplate.ui.theme.ComposeWebViewAppTemplateTheme
import com.saha.composewebviewapptemplate.utils.AppConstants

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ComposeWebViewAppTemplateTheme {
                WebViewScreen(
                    content = WebViewContent(
                        type = ContentType.URL,
                        data = AppConstants.APP_BASE_URL
                    ),
                    onPageFinished = { url ->
                        Log.d("WebView", "Page finished loading: $url")
                    },
                    onPageStarted = { url ->
                        Log.d("WebView", "Page started loading: $url")
                    },
                    onError = { errorType, message ->
                        Log.e("WebView", "Error: $errorType - $message")
                        // You can add additional error handling here
                        // For example, show a toast, log to analytics, etc.
                    }
                )
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!", modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ComposeWebViewAppTemplateTheme {
        Greeting("Android")
    }
}