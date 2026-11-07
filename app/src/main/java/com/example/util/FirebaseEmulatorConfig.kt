package com.example.util

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.BuildConfig
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Utility for configuring Firebase SDK clients to connect to the local
 * Firebase Emulator Suite during development and testing.
 *
 * Emulator Ports:
 * - Firebase Authentication Emulator: 9099
 * - Cloud Firestore Emulator: 8085
 *
 * Network Configuration:
 * - Android Emulator alias for host loopback: 10.0.2.2
 * - Host machine / adb-reversed physical device loopback: 127.0.0.1
 */
object FirebaseEmulatorConfig {

    private const val TAG = "FirebaseEmulatorConfig"

    const val AUTH_EMULATOR_PORT = 9099
    const val FIRESTORE_EMULATOR_PORT = 8085

    const val ANDROID_EMULATOR_HOST = "10.0.2.2"
    const val LOCALHOST_HOST = "127.0.0.1"

    private var emulatorsConfigured = false

    /**
     * Determines whether the app is running inside the standard Android Emulator.
     */
    fun isRunningOnAndroidEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT)
    }

    /**
     * Resolves the appropriate emulator host IP address depending on whether
     * the app is running in an Android Emulator or directly on host/physical device.
     */
    fun getEmulatorHost(): String {
        return if (isRunningOnAndroidEmulator()) {
            ANDROID_EMULATOR_HOST
        } else {
            LOCALHOST_HOST
        }
    }

    /**
     * Configures Firebase Auth and Cloud Firestore to use local emulators.
     * Safe to call multiple times; only executes configuration once.
     */
    fun configureEmulators(
        context: Context,
        host: String = getEmulatorHost(),
        authPort: Int = AUTH_EMULATOR_PORT,
        firestorePort: Int = FIRESTORE_EMULATOR_PORT,
        force: Boolean = false
    ) {
        if (emulatorsConfigured && !force) {
            Log.d(TAG, "Emulators already configured; skipping.")
            return
        }

        try {
            // Configure Firebase Auth Emulator
            val auth: FirebaseAuth = Firebase.auth
            auth.useEmulator(host, authPort)
            Log.i(TAG, "Configured Firebase Auth emulator at $host:$authPort")

            // Configure Cloud Firestore Named Database Emulator
            val databaseId = try {
                context.getString(R.string.firestore_database_id)
            } catch (e: Exception) {
                "ai-studio-android-remixthe-b1d35dd2-d6f5-4ff6-8c75-8d89468bf295"
            }

            val firestore = FirebaseFirestore.getInstance(databaseId)
            firestore.useEmulator(host, firestorePort)
            Log.i(TAG, "Configured Firestore ($databaseId) emulator at $host:$firestorePort")

            emulatorsConfigured = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to Firebase emulators at $host: ${e.message}", e)
        }
    }

    /**
     * Returns true if emulators have been successfully configured in this process.
     */
    fun areEmulatorsConfigured(): Boolean = emulatorsConfigured
}

