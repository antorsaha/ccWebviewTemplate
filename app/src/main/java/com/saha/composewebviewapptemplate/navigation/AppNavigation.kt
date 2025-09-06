package com.saha.composewebviewapptemplate.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.saha.composewebviewapptemplate.screens.ContentType
import com.saha.composewebviewapptemplate.screens.OnboardingScreen
import com.saha.composewebviewapptemplate.screens.SplashScreen
import com.saha.composewebviewapptemplate.screens.WebViewContent
import com.saha.composewebviewapptemplate.screens.WebViewScreen
import com.saha.composewebviewapptemplate.services.UpdateCheckerService
import com.saha.composewebviewapptemplate.services.UpdateDialog
import com.saha.composewebviewapptemplate.services.UpdateProgressDialog
import com.saha.composewebviewapptemplate.utils.AppConstants
import com.saha.composewebviewapptemplate.utils.PreferenceManager
import kotlinx.coroutines.delay

// ============================================================================
// NAVIGATION SYSTEM - APP FLOW MANAGEMENT
// ============================================================================
// This file manages the complete app navigation flow:
// 1. Splash Screen (3 seconds) + Update Check
// 2. Onboarding Screen (only on first launch)
// 3. WebView Screen (main app content) + Update Check
// 4. Error Screen (if needed)
// ============================================================================

/**
 * Screen definitions for the app navigation
 * Each screen represents a different state in the app flow
 */
sealed class Screen {
    object Splash : Screen()
    object Onboarding : Screen()
    object WebView : Screen()
    object Error : Screen()
}

/**
 * Navigation state data class
 * Holds the current app state and any relevant data
 */
data class NavigationState(
    val currentScreen: Screen = Screen.Splash,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val hasSeenOnboarding: Boolean = false,
    val showUpdateDialog: Boolean = false,
    val showUpdateProgress: Boolean = false,
    val updateInfo: AppUpdateInfo? = null,
    val updateProgress: Int = 0
)

/**
 * Main navigation composable that manages the entire app flow
 *
 * Features:
 * - Automatic onboarding detection (first launch only)
 * - SharedPreferences integration for state persistence
 * - In-app update checking and management
 * - Smooth transitions between screens
 * - Error handling and recovery
 *
 * Flow:
 * Splash → Onboarding (first time) → WebView
 * Splash → WebView (subsequent launches)
 */
@Composable
fun AppNavigation() {
    // Get Android context for SharedPreferences and Update Service
    val context = LocalContext.current

    // Initialize preference manager and update service
    val preferenceManager = remember { PreferenceManager(context) }
    val updateService = remember {
        UpdateCheckerService(context as ComponentActivity)
    }

    // Navigation state management
    var navigationState by remember {
        mutableStateOf(NavigationState())
    }

    // ========================================================================
    // UPDATE CHECKING - AUTOMATIC UPDATE DETECTION
    // ========================================================================
    LaunchedEffect(Unit) {
        // Check for updates when app starts
        updateService.checkForUpdatesIfNeeded(
            forceCheck = false,
            onUpdateDialog = { appUpdateInfo ->
                navigationState = navigationState.copy(
                    showUpdateDialog = true, updateInfo = appUpdateInfo
                )
            },
            onNoUpdate = {
                // No update available, continue with normal flow
            })
    }

    // ========================================================================
    // NAVIGATION LOGIC - DETERMINE NEXT SCREEN
    // ========================================================================
    LaunchedEffect(navigationState.currentScreen) {
        when (navigationState.currentScreen) {
            Screen.Splash -> {
                // Show splash for 3 seconds
                delay(3000)

                // Check if user has completed onboarding
                val hasCompletedOnboarding = preferenceManager.isOnboardingCompleted()

                // Navigate based on onboarding status
                navigationState = navigationState.copy(
                    currentScreen = /*if (hasCompletedOnboarding) Screen.WebView else*/ Screen.Onboarding,
                    isLoading = false,
                    hasSeenOnboarding = hasCompletedOnboarding
                )
            }

            Screen.Onboarding -> {
                // Onboarding screen - wait for user interaction
                // No automatic navigation
            }

            Screen.WebView -> {
                // WebView is loaded - main app content
                // Mark first launch as completed
                if (preferenceManager.isFirstLaunch()) {
                    preferenceManager.setFirstLaunchCompleted()
                }

                // Check for updates again when WebView loads (less frequent)
                updateService.checkForUpdatesIfNeeded(
                    forceCheck = false,
                    onUpdateDialog = { appUpdateInfo ->
                        navigationState = navigationState.copy(
                            showUpdateDialog = true, updateInfo = appUpdateInfo
                        )
                    },
                    onNoUpdate = {
                        // No update available
                    })
            }

            Screen.Error -> {
                // Error screen - wait for user action
                // No automatic navigation
            }
        }
    }

    // ========================================================================
    // UPDATE DIALOG HANDLING
    // ========================================================================
    UpdateDialog(
        isVisible = navigationState.showUpdateDialog,
        appUpdateInfo = navigationState.updateInfo,
        onUpdateNow = {
            // User chose to update now
            navigationState.updateInfo?.let { updateInfo ->
                updateService.startFlexibleUpdate(appUpdateInfo = updateInfo, onUpdateStarted = {
                    navigationState = navigationState.copy(
                        showUpdateDialog = false, showUpdateProgress = true, updateProgress = 0
                    )
                }, onError = { error ->
                    // Handle update start error
                    navigationState = navigationState.copy(
                        showUpdateDialog = false, showUpdateProgress = false
                    )
                })
            }
        },
        onUpdateLater = {
            // User chose to update later
            navigationState = navigationState.copy(
                showUpdateDialog = false
            )
        },
        onDismiss = {
            // User dismissed the dialog
            navigationState = navigationState.copy(
                showUpdateDialog = false
            )
        })

    // ========================================================================
    // UPDATE PROGRESS DIALOG
    // ========================================================================
    UpdateProgressDialog(
        isVisible = navigationState.showUpdateProgress,
        progress = navigationState.updateProgress,
        onCancel = {
            // User cancelled the update
            updateService.cancelUpdate()
            navigationState = navigationState.copy(
                showUpdateProgress = false
            )
        })

    // ========================================================================
    // SCREEN RENDERING - DISPLAY APPROPRIATE SCREEN
    // ========================================================================
    when (navigationState.currentScreen) {
        Screen.Splash -> {
            SplashScreen(
                onSplashFinished = {
                    // This callback is handled by LaunchedEffect above
                    // but kept for potential future use
                })
        }

        Screen.Onboarding -> {
            OnboardingScreen(
                onFinish = {
                    // User completed onboarding
                    preferenceManager.setOnboardingCompleted()
                    navigationState = navigationState.copy(
                        currentScreen = Screen.WebView, hasSeenOnboarding = true
                    )
                })
        }

        Screen.WebView -> {
            WebViewScreen(
                content = WebViewContent(
                type = ContentType.URL, data = AppConstants.APP_BASE_URL
            ), onPageFinished = { url ->
                // Handle page finished - can be used for analytics
            }, onPageStarted = { url ->
                // Handle page started - can be used for analytics
            }, onError = { errorType, message ->
                // Handle WebView errors
                // Currently commented out to use internal error handling
                /*navigationState = navigationState.copy(
                    currentScreen = Screen.Error,
                    errorMessage = message
                )*/
            })
        }

        Screen.Error -> {
            ErrorScreen(
                message = navigationState.errorMessage ?: "An error occurred", onRetry = {
                    navigationState = navigationState.copy(
                        currentScreen = Screen.WebView, errorMessage = null
                    )
                })
        }
    }
}

/**
 * Error screen composable
 * Displays error information and provides retry functionality
 *
 * @param message Error message to display
 * @param onRetry Callback when user clicks retry button
 */
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

// ============================================================================
// DEBUGGING AND TESTING UTILITIES
// ============================================================================
/*
 * For testing purposes, you can reset onboarding status:
 * 
 * 1. In your app, add a debug button that calls:
 *    preferenceManager.resetOnboarding()
 * 
 * 2. Or clear all preferences:
 *    preferenceManager.clearAllPreferences()
 * 
 * 3. Or check onboarding status:
 *    val isCompleted = preferenceManager.isOnboardingCompleted()
 * 
 * 4. To force update check:
 *    updateService.checkForUpdates(forceCheck = true, ...)
 */
