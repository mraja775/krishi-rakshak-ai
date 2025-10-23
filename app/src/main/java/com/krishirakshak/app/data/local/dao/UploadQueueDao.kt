package com.krishirakshak.app.data.local.dao

import androidx.room.*
import com.krishirakshak.app.data.local.entity.UploadQueueEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for UploadQueue entity.
 * Manages the queue of images pending upload.
 */
@Dao
interface UploadQueueDao {

    /**
     * Insert new item to upload queue
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItem(queueItem: UploadQueueEntity)

    /**
     * Get all pending uploads for a user
     */
    @Query("SELECT * FROM upload_queue WHERE user_id = :userId ORDER BY created_at ASC")
    suspend fun getPendingUploads(userId: String): List<UploadQueueEntity>

    /**
     * Get all pending uploads as Flow
     */
    @Query("SELECT * FROM upload_queue ORDER BY created_at ASC")
    fun getPendingUploadsFlow(): Flow<List<UploadQueueEntity>>

    /**
     * Get oldest pending upload (for sequential processing)
     */
    @Query("SELECT * FROM upload_queue ORDER BY created_at ASC LIMIT 1")
    suspend fun getOldestPendingUpload(): UploadQueueEntity?

    /**
     * Get queue item by ID
     */
    @Query("SELECT * FROM upload_queue WHERE id = :queueId LIMIT 1")
    suspend fun getQueueItemById(queueId: String): UploadQueueEntity?

    /**
     * Update retry count and last retry timestamp
     */
    @Query(
        """UPDATE upload_queue
        SET retry_count = :retryCount,
            last_retry_at = :lastRetryAt,
            error_message = :errorMessage
        WHERE id = :queueId"""
    )
    suspend fun updateRetryInfo(
        queueId: String,
        retryCount: Int,
        lastRetryAt: Long,
        errorMessage: String?
    )

    /**
     * Delete queue item after successful upload
     */
    @Delete
    suspend fun deleteQueueItem(queueItem: UploadQueueEntity)

    /**
     * Delete queue item by ID
     */
    @Query("DELETE FROM upload_queue WHERE id = :queueId")
    suspend fun deleteQueueItemById(queueId: String)

    /**
     * Delete all queue items for a user
     */
    @Query("DELETE FROM upload_queue WHERE user_id = :userId")
    suspend fun deleteAllQueueItemsForUser(userId: String)

    /**
     * Delete queue items older than specified timestamp
     */
    @Query("DELETE FROM upload_queue WHERE created_at < :timestamp")
    suspend fun deleteOldQueueItems(timestamp: Long)

    /**
     * Get total queue count
     */
    @Query("SELECT COUNT(*) FROM upload_queue")
    suspend fun getQueueCount(): Int

    /**
     * Get failed uploads (retry count >= max)
     */
    @Query("SELECT * FROM upload_queue WHERE retry_count >= :maxRetries ORDER BY created_at ASC")
    suspend fun getFailedUploads(maxRetries: Int = 3): List<UploadQueueEntity>
}
