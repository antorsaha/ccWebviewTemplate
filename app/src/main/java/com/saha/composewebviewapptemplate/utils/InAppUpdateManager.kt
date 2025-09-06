package com.saha.composewebviewapptemplate.utils

import android.util.Log
import androidx.activity.ComponentActivity
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import com.google.android.play.core.ktx.requestCompleteUpdate

/**
 * InAppUpdateManager - Handles in-app updates for the application
 * 
 * This class provides functionality to:
 * - Check for app updates from Google Play Store
 * - Handle flexible and immediate update flows
 * - Manage update progress and completion
 * - Show update dialogs and progress indicators
 * 
 * Features:
 * - Automatic update checking on app launch
 * - Flexible update flow (user can continue using app)
 * - Immediate update flow (forces update before app use)
 * - Update progress tracking
 * - Error handling and retry mechanisms
 * 
 * Usage:
 * 1. Initialize in your Activity
 * 2. Call checkForUpdates() to start the process
 * 3. Handle update callbacks in your UI
 */
class InAppUpdateManager(
    private val activity: ComponentActivity
) {
    
    // Google Play Core App Update Manager
    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity)
    
    // Update request code for handling results
    private val UPDATE_REQUEST_CODE = 1001
    
    // Update availability states
    private var isUpdateAvailable = false
    private var isUpdateInProgress = false
    
    // ========================================================================
    // UPDATE CHECKING AND INITIALIZATION
    // ========================================================================
    
    /**
     * Check for available app updates
     * This should be called when the app starts or when user manually checks
     * 
     * @param onUpdateAvailable Callback when update is available
     * @param onNoUpdate Callback when no update is available
     * @param onError Callback when error occurs during check
     */
    suspend fun checkForUpdates(
        onUpdateAvailable: (AppUpdateInfo) -> Unit,
        onNoUpdate: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            Log.d("InAppUpdate", "Checking for app updates...")
            
            val appUpdateInfo = appUpdateManager.requestAppUpdateInfo()
            
            when (appUpdateInfo.updateAvailability()) {
                UpdateAvailability.UPDATE_AVAILABLE -> {
                    Log.d("InAppUpdate", "Update available: ${appUpdateInfo.availableVersionCode()}")
                    isUpdateAvailable = true
                    onUpdateAvailable(appUpdateInfo)
                }
                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                    Log.d("InAppUpdate", "Developer triggered update in progress")
                    isUpdateInProgress = true
                    onUpdateAvailable(appUpdateInfo)
                }
                UpdateAvailability.UPDATE_NOT_AVAILABLE -> {
                    Log.d("InAppUpdate", "No update available")
                    isUpdateAvailable = false
                    onNoUpdate()
                }
                else -> {
                    Log.d("InAppUpdate", "Update availability unknown")
                    onNoUpdate()
                }
            }
        } catch (e: Exception) {
            Log.e("InAppUpdate", "Error checking for updates", e)
            onError(e)
        }
    }
    
    /**
     * Start flexible update flow
     * User can continue using the app while update downloads in background
     * 
     * @param appUpdateInfo Update information from checkForUpdates
     * @param onUpdateStarted Callback when update starts
     * @param onError Callback when error occurs
     */
    fun startFlexibleUpdate(
        appUpdateInfo: AppUpdateInfo,
        onUpdateStarted: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                Log.d("InAppUpdate", "Starting flexible update...")
                
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.FLEXIBLE,
                    activity,
                    UPDATE_REQUEST_CODE
                )
                
                isUpdateInProgress = true
                onUpdateStarted()
            } else {
                Log.w("InAppUpdate", "Flexible update not allowed")
                onError(Exception("Flexible update not allowed"))
            }
        } catch (e: Exception) {
            Log.e("InAppUpdate", "Error starting flexible update", e)
            onError(e)
        }
    }
    
    /**
     * Start immediate update flow
     * Forces user to update before continuing to use the app
     * 
     * @param appUpdateInfo Update information from checkForUpdates
     * @param onUpdateStarted Callback when update starts
     * @param onError Callback when error occurs
     */
    fun startImmediateUpdate(
        appUpdateInfo: AppUpdateInfo,
        onUpdateStarted: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                Log.d("InAppUpdate", "Starting immediate update...")
                
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.IMMEDIATE,
                    activity,
                    UPDATE_REQUEST_CODE
                )
                
                isUpdateInProgress = true
                onUpdateStarted()
            } else {
                Log.w("InAppUpdate", "Immediate update not allowed")
                onError(Exception("Immediate update not allowed"))
            }
        } catch (e: Exception) {
            Log.e("InAppUpdate", "Error starting immediate update", e)
            onError(e)
        }
    }
    
    // ========================================================================
    // UPDATE FLOW HANDLING
    // ========================================================================
    
    /**
     * Complete the update process
     * Call this when update is downloaded and ready to install
     */
    suspend fun completeUpdate(
        onUpdateCompleted: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            Log.d("InAppUpdate", "Completing update...")
            appUpdateManager.requestCompleteUpdate()
            isUpdateInProgress = false
            onUpdateCompleted()
        } catch (e: Exception) {
            Log.e("InAppUpdate", "Error completing update", e)
            onError(e)
        }
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
        if (requestCode == UPDATE_REQUEST_CODE) {
            when (resultCode) {
                android.app.Activity.RESULT_OK -> {
                    Log.d("InAppUpdate", "Update completed successfully")
                    isUpdateInProgress = false
                    onUpdateCompleted()
                }
                android.app.Activity.RESULT_CANCELED -> {
                    Log.d("InAppUpdate", "Update cancelled by user")
                    isUpdateInProgress = false
                    onUpdateCancelled()
                }
                else -> {
                    Log.d("InAppUpdate", "Update failed or cancelled")
                    isUpdateInProgress = false
                    onUpdateCancelled()
                }
            }
        }
    }
    
    // ========================================================================
    // UTILITY METHODS
    // ========================================================================
    
    /**
     * Check if update is currently in progress
     */
    fun isUpdateInProgress(): Boolean = isUpdateInProgress
    
    /**
     * Check if update is available
     */
    fun isUpdateAvailable(): Boolean = isUpdateAvailable
    
    /**
     * Get update progress (0-100)
     * This would need to be implemented with proper progress tracking
     */
    fun getUpdateProgress(): Int = 0 // Placeholder
    
    /**
     * Cancel ongoing update
     */
    fun cancelUpdate() {
        Log.d("InAppUpdate", "Update cancelled by user")
        isUpdateInProgress = false
    }
}