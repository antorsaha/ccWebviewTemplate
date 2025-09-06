package com.saha.composewebviewapptemplate.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * PreferenceManager - Utility class for managing app preferences
 *
 * This class handles storing and retrieving user preferences including:
 * - Onboarding completion status
 * - User settings
 * - App state persistence
 *
 * Usage: Use this class to store data that should persist across app sessions
 */
class PreferenceManager(context: Context) {

    // SharedPreferences instance
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        // Preference file name
        private const val PREFS_NAME = "app_preferences"

        // Key constants for different preferences
        const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        const val KEY_FIRST_LAUNCH = "first_launch"
        const val KEY_APP_VERSION = "app_version"
    }

    /**
     * Check if user has completed onboarding
     * @return true if onboarding is completed, false otherwise
     */
    fun isOnboardingCompleted(): Boolean {
        return sharedPreferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    /**
     * Mark onboarding as completed
     * This should be called when user finishes the onboarding process
     */
    fun setOnboardingCompleted() {
        sharedPreferences.edit {
            putBoolean(KEY_ONBOARDING_COMPLETED, true)
        }
    }

    /**
     * Check if this is the first app launch
     * @return true if this is the first launch, false otherwise
     */
    fun isFirstLaunch(): Boolean {
        return sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    /**
     * Mark that the app has been launched at least once
     */
    fun setFirstLaunchCompleted() {
        sharedPreferences.edit {
            putBoolean(KEY_FIRST_LAUNCH, false)
        }
    }

    /**
     * Get stored app version
     * @return the stored app version string
     */
    fun getAppVersion(): String? {
        return sharedPreferences.getString(KEY_APP_VERSION, null)
    }

    /**
     * Store current app version
     * @param version the current app version string
     */
    fun setAppVersion(version: String) {
        sharedPreferences.edit {
            putString(KEY_APP_VERSION, version)
        }
    }

    /**
     * Reset onboarding status (useful for testing or if user wants to see onboarding again)
     */
    fun resetOnboarding() {
        sharedPreferences.edit {
            putBoolean(KEY_ONBOARDING_COMPLETED, false)
        }
    }

    /**
     * Clear all preferences (useful for logout or reset functionality)
     */
    fun clearAllPreferences() {
        sharedPreferences.edit { clear() }
    }

    /**
     * Check if app version has changed (useful for showing changelog or re-onboarding)
     * @param currentVersion the current app version
     * @return true if version has changed, false otherwise
     */
    fun hasVersionChanged(currentVersion: String): Boolean {
        val storedVersion = getAppVersion()
        return storedVersion != currentVersion
    }
}