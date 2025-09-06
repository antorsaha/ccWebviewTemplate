package com.saha.composewebviewapptemplate.services

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.saha.composewebviewapptemplate.utils.InAppUpdateManager

/**
 * UpdateCheckerService - Service for managing app updates
 * 
 * This service provides a complete update checking and management system:
 * - Automatic update checking on app launch
 * - User-friendly update dialogs
 * - Progress tracking and status updates
 * - Error handling and retry mechanisms
 * 
 * Features:
 * - Background update checking
 * - Flexible and immediate update flows
 * - Update progress monitoring
 * - User preference handling
 * - Network error handling
 */
class UpdateCheckerService(
    private val activity: ComponentActivity
) {
    
    // In-app update manager instance
    private val updateManager = InAppUpdateManager(activity)
    
    // Update state management
    private var isCheckingForUpdates = false
    private var lastUpdateCheckTime = 0L
    private val UPDATE_CHECK_INTERVAL = 24 * 60 * 60 * 1000L // 24 hours
    
    // ========================================================================
    // PUBLIC METHODS
    // ========================================================================
    
    /**
     * Check for updates if enough time has passed since last check
     * This is the main method to call for update checking
     * 
     * @param forceCheck Force update check regardless of time interval
     * @param onUpdateDialog Callback to show update dialog
     * @param onNoUpdate Callback when no update is available
     */
    suspend fun checkForUpdatesIfNeeded(
        forceCheck: Boolean = false,
        onUpdateDialog: (AppUpdateInfo) -> Unit,
        onNoUpdate: () -> Unit
    ) {
        val currentTime = System.currentTimeMillis()
        
        // Check if enough time has passed or if forced
        if (forceCheck || (currentTime - lastUpdateCheckTime) > UPDATE_CHECK_INTERVAL) {
            checkForUpdates(onUpdateDialog, onNoUpdate)
            lastUpdateCheckTime = currentTime
        } else {
            Log.d("UpdateChecker", "Skipping update check - too soon since last check")
            onNoUpdate()
        }
    }
    
    /**
     * Check for updates immediately
     * 
     * @param onUpdateDialog Callback to show update dialog
     * @param onNoUpdate Callback when no update is available
     */
    suspend fun checkForUpdates(
        onUpdateDialog: (AppUpdateInfo) -> Unit,
        onNoUpdate: () -> Unit
    ) {
        if (isCheckingForUpdates) {
            Log.d("UpdateChecker", "Update check already in progress")
            return
        }
        
        isCheckingForUpdates = true
        
        try {
            updateManager.checkForUpdates(
                onUpdateAvailable = { appUpdateInfo ->
                    Log.d("UpdateChecker", "Update available: ${appUpdateInfo.availableVersionCode()}")
                    onUpdateDialog(appUpdateInfo)
                },
                onNoUpdate = {
                    Log.d("UpdateChecker", "No update available")
                    onNoUpdate()
                },
                onError = { error ->
                    Log.e("UpdateChecker", "Error checking for updates", error)
                    onNoUpdate() // Continue with app flow even if check fails
                }
            )
        } finally {
            isCheckingForUpdates = false
        }
    }
    
    /**
     * Start flexible update (user can continue using app)
     * 
     * @param appUpdateInfo Update information
     * @param onUpdateStarted Callback when update starts
     * @param onError Callback when error occurs
     */
    fun startFlexibleUpdate(
        appUpdateInfo: AppUpdateInfo,
        onUpdateStarted: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        updateManager.startFlexibleUpdate(
            appUpdateInfo = appUpdateInfo,
            onUpdateStarted = {
                Log.d("UpdateChecker", "Flexible update started")
                onUpdateStarted()
            },
            onError = { error ->
                Log.e("UpdateChecker", "Error starting flexible update", error)
                onError(error)
            }
        )
    }
    
    /**
     * Start immediate update (forces update before app use)
     * 
     * @param appUpdateInfo Update information
     * @param onUpdateStarted Callback when update starts
     * @param onError Callback when error occurs
     */
    fun startImmediateUpdate(
        appUpdateInfo: AppUpdateInfo,
        onUpdateStarted: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        updateManager.startImmediateUpdate(
            appUpdateInfo = appUpdateInfo,
            onUpdateStarted = {
                Log.d("UpdateChecker", "Immediate update started")
                onUpdateStarted()
            },
            onError = { error ->
                Log.e("UpdateChecker", "Error starting immediate update", error)
                onError(error)
            }
        )
    }
    
    /**
     * Complete the update process
     * Call this when update is downloaded and ready to install
     * 
     * @param onUpdateCompleted Callback when update is completed
     * @param onError Callback when error occurs
     */
    suspend fun completeUpdate(
        onUpdateCompleted: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        updateManager.completeUpdate(
            onUpdateCompleted = {
                Log.d("UpdateChecker", "Update completed successfully")
                onUpdateCompleted()
            },
            onError = { error ->
                Log.e("UpdateChecker", "Error completing update", error)
                onError(error)
            }
        )
    }
    
    /**
     * Handle activity result for update flow
     * Call this from your Activity's onActivityResult
     * 
     * @param requestCode Request code from activity result
     * @param resultCode Result code from activity result
     * @param onUpdateCompleted Callback when update is completed
     * @param onUpdateCancelled Callback when update is cancelled
     */
    fun handleActivityResult(
        requestCode: Int,
        resultCode: Int,
        onUpdateCompleted: () -> Unit,
        onUpdateCancelled: () -> Unit
    ) {
        updateManager.handleActivityResult(
            requestCode = requestCode,
            resultCode = resultCode,
            onUpdateCompleted = onUpdateCompleted,
            onUpdateCancelled = onUpdateCancelled
        )
    }
    
    // ========================================================================
    // UTILITY METHODS
    // ========================================================================
    
    /**
     * Check if update is currently in progress
     */
    fun isUpdateInProgress(): Boolean = updateManager.isUpdateInProgress()
    
    /**
     * Get update progress percentage
     */
    fun getUpdateProgress(): Int = updateManager.getUpdateProgress()
    
    /**
     * Cancel ongoing update
     */
    fun cancelUpdate() {
        updateManager.cancelUpdate()
    }
}

/**
 * UpdateDialog - Composable for showing update information to users
 * 
 * Features:
 * - Modern Material Design 3 styling
 * - Update size and version information
 * - Flexible and immediate update options
 * - Dismiss and later options
 * - Customizable appearance
 * 
 * @param isVisible Whether the dialog should be shown
 * @param appUpdateInfo Information about the available update
 * @param onUpdateNow Callback when user chooses to update now
 * @param onUpdateLater Callback when user chooses to update later
 * @param onDismiss Callback when user dismisses the dialog
 */
@Composable
fun UpdateDialog(
    isVisible: Boolean,
    appUpdateInfo: AppUpdateInfo?,
    onUpdateNow: () -> Unit,
    onUpdateLater: () -> Unit,
    onDismiss: () -> Unit
) {
    if (isVisible && appUpdateInfo != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "Update Available",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    text = "A new version of the app is available.\n\n" +
                            "Version: ${appUpdateInfo.availableVersionCode()}\n" +
                            "This update includes bug fixes and improvements.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = onUpdateNow
                ) {
                    Text("Update Now")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onUpdateLater
                ) {
                    Text("Later")
                }
            }
        )
    }
}

/**
 * UpdateProgressDialog - Composable for showing update progress
 * 
 * @param isVisible Whether the progress dialog should be shown
 * @param progress Update progress percentage (0-100)
 * @param onCancel Callback when user cancels the update
 */
@Composable
fun UpdateProgressDialog(
    isVisible: Boolean,
    progress: Int,
    onCancel: () -> Unit
) {
    if (isVisible) {
        AlertDialog(
            onDismissRequest = { /* Prevent dismiss during update */ },
            title = {
                Text(
                    text = "Updating App",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    text = "Please wait while the app updates...\nProgress: $progress%",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = onCancel
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}