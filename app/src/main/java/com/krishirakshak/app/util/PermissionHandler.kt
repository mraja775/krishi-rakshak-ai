package com.krishirakshak.app.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.core.content.ContextCompat

/**
 * Utility object for handling runtime permissions.
 * Simplifies permission checking and requesting for camera and storage.
 */
object PermissionHandler {

    /**
     * Camera permission
     */
    const val CAMERA_PERMISSION = Manifest.permission.CAMERA

    /**
     * Storage/Media permissions (varies by Android version)
     */
    val STORAGE_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    /**
     * Check if camera permission is granted
     */
    fun isCameraPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            CAMERA_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if storage/media permissions are granted
     */
    fun isStoragePermissionGranted(context: Context): Boolean {
        return STORAGE_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Request camera permission
     */
    fun requestCameraPermission(
        launcher: ManagedActivityResultLauncher<String, Boolean>
    ) {
        launcher.launch(CAMERA_PERMISSION)
    }

    /**
     * Request storage permission
     */
    fun requestStoragePermission(
        launcher: ManagedActivityResultLauncher<String, Boolean>
    ) {
        launcher.launch(STORAGE_PERMISSIONS.first())
    }

    /**
     * Open app settings for manual permission grant
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Get permission rationale message
     */
    fun getCameraRationale(): String {
        return "Camera permission is required to take photos of plants for disease detection."
    }

    fun getStorageRationale(): String {
        return "Storage permission is required to select photos from your gallery."
    }
}
