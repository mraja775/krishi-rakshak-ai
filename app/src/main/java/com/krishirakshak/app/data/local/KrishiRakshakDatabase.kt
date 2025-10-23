package com.krishirakshak.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.krishirakshak.app.data.local.dao.AnalysisDao
import com.krishirakshak.app.data.local.dao.UploadQueueDao
import com.krishirakshak.app.data.local.dao.UserDao
import com.krishirakshak.app.data.local.entity.AnalysisEntity
import com.krishirakshak.app.data.local.entity.UploadQueueEntity
import com.krishirakshak.app.data.local.entity.UserEntity

/**
 * Room database for Krishi Rakshak AI.
 * Contains three tables: users, analyses, and upload_queue.
 *
 * Version 1: Initial schema
 * Version 2: Updated AnalysisEntity schema for Phase 5 (AI Analysis)
 *   - Added: imageUrl, diseaseInfoJson, recommendationsJson, status
 *   - Renamed: imageCloudUrl -> imageUrl, imageLocalPath -> localImagePath
 *   - Removed: diseaseName, remedy
 */
@Database(
    entities = [
        UserEntity::class,
        AnalysisEntity::class,
        UploadQueueEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class KrishiRakshakDatabase : RoomDatabase() {

    /**
     * Get UserDao instance
     */
    abstract fun userDao(): UserDao

    /**
     * Get AnalysisDao instance
     */
    abstract fun analysisDao(): AnalysisDao

    /**
     * Get UploadQueueDao instance
     */
    abstract fun uploadQueueDao(): UploadQueueDao

    companion object {
        const val DATABASE_NAME = "krishi_rakshak_db"
    }
}
