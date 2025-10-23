package com.krishirakshak.app.util

import android.content.Context
import android.net.Uri
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

/**
 * Extension functions for common operations throughout the app
 */

/**
 * Show a toast message
 */
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

/**
 * Format timestamp to readable date string
 */
fun Long.toDateString(pattern: String = "dd MMM yyyy, hh:mm a"): String {
    val date = Date(this)
    val formatter = SimpleDateFormat(pattern, Locale.getDefault())
    return formatter.format(date)
}

/**
 * Format timestamp to relative time (e.g., "2 hours ago")
 */
fun Long.toRelativeTime(): String {
    val now = System.currentTimeMillis()
    val diff = now - this

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000} minutes ago"
        diff < 86400_000 -> "${diff / 3600_000} hours ago"
        diff < 604800_000 -> "${diff / 86400_000} days ago"
        else -> this.toDateString("dd MMM yyyy")
    }
}

/**
 * Generate unique ID with timestamp
 */
fun generateUniqueId(prefix: String = ""): String {
    val timestamp = System.currentTimeMillis()
    val random = UUID.randomUUID().toString().substring(0, 8)
    return if (prefix.isNotEmpty()) {
        "${prefix}_${timestamp}_$random"
    } else {
        "${timestamp}_$random"
    }
}

/**
 * Safe string formatting for confidence percentage
 */
fun Double.toPercentage(): String = "${(this * 100).toInt()}%"

/**
 * Validate phone number (Indian format)
 */
fun String.isValidIndianPhoneNumber(): Boolean {
    val pattern = Regex("^[6-9]\\d{9}$")
    return pattern.matches(this)
}

/**
 * Format phone number for display
 */
fun String.formatPhoneNumber(): String {
    return if (this.length == 10) {
        "${this.substring(0, 5)} ${this.substring(5)}"
    } else {
        this
    }
}

/**
 * Get file name from Uri
 */
fun Uri.getFileName(context: Context): String? {
    var fileName: String? = null
    context.contentResolver.query(this, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0) {
                fileName = cursor.getString(nameIndex)
            }
        }
    }
    return fileName
}

/**
 * Convert bytes to human-readable size
 */
fun Long.toReadableFileSize(): String {
    val kb = this / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0

    return when {
        gb >= 1 -> String.format("%.2f GB", gb)
        mb >= 1 -> String.format("%.2f MB", mb)
        kb >= 1 -> String.format("%.2f KB", kb)
        else -> "$this bytes"
    }
}
