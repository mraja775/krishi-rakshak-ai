package com.krishirakshak.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for storing plant disease analysis results.
 * Cached locally for offline access and synced with Firestore.
 */
@Entity(
    tableName = "analyses",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["user_id"]), Index(value = ["timestamp"])]
)
data class AnalysisEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String, // Unique analysis ID

    @ColumnInfo(name = "user_id")
    val userId: String, // Foreign key to users table

    @ColumnInfo(name = "image_url")
    val imageUrl: String, // Cloud Storage URL

    @ColumnInfo(name = "local_image_path")
    val localImagePath: String? = null, // Local file path

    @ColumnInfo(name = "plant_type")
    val plantType: String? = null, // e.g., "Tomato"

    @ColumnInfo(name = "disease_info_json")
    val diseaseInfoJson: String? = null, // JSON string of DiseaseInfo

    @ColumnInfo(name = "confidence")
    val confidence: Double, // 0.0 - 1.0

    @ColumnInfo(name = "recommendations_json")
    val recommendationsJson: String, // JSON array of recommendations

    @ColumnInfo(name = "timestamp")
    val timestamp: Long, // Unix timestamp in milliseconds

    @ColumnInfo(name = "status")
    val status: String, // PENDING, PROCESSING, COMPLETED, FAILED

    @ColumnInfo(name = "synced")
    val synced: Boolean = false // true = synced with Firestore, false = pending
)
