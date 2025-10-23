package com.krishirakshak.app.data.local.dao

import androidx.room.*
import com.krishirakshak.app.data.local.entity.AnalysisEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Analysis entity.
 * Provides queries for plant disease analysis history.
 */
@Dao
interface AnalysisDao {

    /**
     * Insert new analysis
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(analysis: AnalysisEntity)

    /**
     * Insert multiple analyses (for bulk sync)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalyses(analyses: List<AnalysisEntity>)

    /**
     * Get analysis by ID
     */
    @Query("SELECT * FROM analyses WHERE id = :analysisId LIMIT 1")
    suspend fun getAnalysisById(analysisId: String): AnalysisEntity?

    /**
     * Get analysis by ID as Flow (for real-time updates)
     */
    @Query("SELECT * FROM analyses WHERE id = :analysisId LIMIT 1")
    fun getAnalysisByIdFlow(analysisId: String): Flow<AnalysisEntity?>

    /**
     * Get all analyses for a user, ordered by timestamp (newest first)
     */
    @Query("SELECT * FROM analyses WHERE user_id = :userId ORDER BY timestamp DESC")
    suspend fun getAllAnalyses(userId: String): List<AnalysisEntity>

    /**
     * Get all analyses for a user as Flow (for real-time updates)
     */
    @Query("SELECT * FROM analyses WHERE user_id = :userId ORDER BY timestamp DESC")
    fun getAllAnalysesFlow(userId: String): Flow<List<AnalysisEntity>>

    /**
     * Get all analyses for a user by date range
     */
    @Query("SELECT * FROM analyses WHERE user_id = :userId ORDER BY timestamp DESC")
    fun getAllAnalysesByUser(userId: String): Flow<List<AnalysisEntity>>

    /**
     * Get recent analyses for a user (limit N)
     */
    @Query("SELECT * FROM analyses WHERE user_id = :userId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentAnalyses(userId: String, limit: Int = 10): List<AnalysisEntity>

    /**
     * Get unsynced analyses (pending cloud sync)
     */
    @Query("SELECT * FROM analyses WHERE user_id = :userId AND synced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedAnalyses(userId: String): List<AnalysisEntity>

    /**
     * Update sync status
     */
    @Query("UPDATE analyses SET synced = :synced WHERE id = :analysisId")
    suspend fun updateSyncStatus(analysisId: String, synced: Boolean)

    /**
     * Update analysis with AI results
     */
    @Query(
        """UPDATE analyses
        SET plant_type = :plantType,
            disease_info_json = :diseaseInfoJson,
            confidence = :confidence,
            recommendations_json = :recommendationsJson,
            status = :status,
            synced = 1
        WHERE id = :analysisId"""
    )
    suspend fun updateAnalysisResults(
        analysisId: String,
        plantType: String?,
        diseaseInfoJson: String?,
        confidence: Double,
        recommendationsJson: String,
        status: String
    )

    /**
     * Delete analysis
     */
    @Delete
    suspend fun deleteAnalysis(analysis: AnalysisEntity)

    /**
     * Delete analysis by ID
     */
    @Query("DELETE FROM analyses WHERE id = :analysisId")
    suspend fun deleteAnalysisById(analysisId: String)

    /**
     * Delete old analyses (keep only last N records)
     */
    @Query(
        """DELETE FROM analyses
        WHERE id IN (
            SELECT id FROM analyses
            WHERE user_id = :userId
            ORDER BY timestamp DESC
            LIMIT -1 OFFSET :keepCount
        )"""
    )
    suspend fun deleteOldAnalyses(userId: String, keepCount: Int = 100)

    /**
     * Get total analysis count for user
     */
    @Query("SELECT COUNT(*) FROM analyses WHERE user_id = :userId")
    suspend fun getAnalysisCount(userId: String): Int

    /**
     * Search analyses by plant type or disease info
     */
    @Query(
        """SELECT * FROM analyses
        WHERE user_id = :userId
        AND (plant_type LIKE '%' || :query || '%' OR disease_info_json LIKE '%' || :query || '%')
        ORDER BY timestamp DESC"""
    )
    fun searchAnalyses(userId: String, query: String): Flow<List<AnalysisEntity>>
}
