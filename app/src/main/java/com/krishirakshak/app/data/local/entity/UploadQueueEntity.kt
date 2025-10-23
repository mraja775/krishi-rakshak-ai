package com.krishirakshak.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for tracking images pending upload when offline.
 * WorkManager will process this queue when connectivity is restored.
 */
@Entity(
    tableName = "upload_queue",
    indices = [Index(value = ["user_id"]), Index(value = ["created_at"])]
)
data class UploadQueueEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String, // Unique queue item ID

    @ColumnInfo(name = "image_local_path")
    val imageLocalPath: String, // Path to local image file

    @ColumnInfo(name = "user_id")
    val userId: String, // User who uploaded

    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0, // Number of retry attempts

    @ColumnInfo(name = "created_at")
    val createdAt: Long, // Unix timestamp in milliseconds

    @ColumnInfo(name = "last_retry_at")
    val lastRetryAt: Long? = null, // Last retry timestamp

    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null // Last error message if any
)
