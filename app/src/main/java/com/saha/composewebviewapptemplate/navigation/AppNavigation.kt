package com.saha.composewebviewapptemplate.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.saha.composewebviewapptemplate.screens.ContentType
import com.saha.composewebviewapptemplate.screens.SplashScreen
import com.saha.composewebviewapptemplate.screens.WebViewContent
import com.saha.composewebviewapptemplate.screens.WebViewScreen
import com.saha.composewebviewapptemplate.utils.AppConstants
import kotlinx.coroutines.delay

sealed class Screen {
    object Splash : Screen()
    object WebView : Screen()
    object Error : Screen()
}

data class NavigationState(
    val currentScreen: Screen = Screen.Splash,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@Composable
fun AppNavigation() {
    var navigationState by remember {
        mutableStateOf(NavigationState())
    }

    // Handle navigation logic
    LaunchedEffect(navigationState.currentScreen) {
        when (navigationState.currentScreen) {
            Screen.Splash -> {
                delay(3000) // 3 seconds splash
                navigationState = navigationState.copy(
                    currentScreen = Screen.WebView, isLoading = false
                )
            }

            Screen.WebView -> {
                // WebView is loaded, no additional action needed
            }

            Screen.Error -> {
                // Error screen, no automatic navigation
            }
        }
    }

    when (navigationState.currentScreen) {
        Screen.Splash -> {
            SplashScreen(
                onSplashFinished = {
                    navigationState = navigationState.copy(
                        currentScreen = Screen.WebView, isLoading = false
                    )
                })
        }

        Screen.WebView -> {
            WebViewScreen(
                content = WebViewContent(
                type = ContentType.URL, data = AppConstants.APP_BASE_URL
            ), onPageFinished = { url ->
                // Handle page finished
            }, onPageStarted = { url ->
                // Handle page started
            }, onError = { errorType, message ->
                /*navigationState = navigationState.copy(
                    currentScreen = Screen.Error, errorMessage = message
                )*/
            })
        }

        Screen.Error -> {
            // You can create a custom error screen here
            ErrorScreen(
                message = navigationState.errorMessage ?: "An error occurred", onRetry = {
                    navigationState = navigationState.copy(
                        currentScreen = Screen.WebView, errorMessage = null
                    )
                })
        }
    }
}

@Composable
fun ErrorScreen(
    message: String, onRetry: () -> Unit
) {
    // Simple error screen - you can customize this
    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            androidx.compose.material3.Text(
                text = "Error",
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
            )
            androidx.compose.material3.Text(
                text = message, modifier = androidx.compose.ui.Modifier.padding(16.dp)
            )
            androidx.compose.material3.Button(
                onClick = onRetry
            ) {
                androidx.compose.material3.Text("Retry")
            }
        }
    }
}
