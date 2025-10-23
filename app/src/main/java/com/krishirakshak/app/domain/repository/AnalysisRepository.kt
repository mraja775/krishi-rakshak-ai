package com.krishirakshak.app.domain.repository

import com.krishirakshak.app.domain.entity.Analysis
import com.krishirakshak.app.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for analysis operations.
 */
interface AnalysisRepository {

    /**
     * Create new analysis
     */
    suspend fun createAnalysis(analysis: Analysis): Result<String>

    /**
     * Get analysis by ID
     */
    suspend fun getAnalysisById(analysisId: String): Result<Analysis>

    /**
     * Observe analysis by ID (reactive)
     */
    fun observeAnalysisById(analysisId: String): Flow<Analysis?>

    /**
     * Get all analyses for current user
     */
    suspend fun getAllAnalyses(): Result<List<Analysis>>

    /**
     * Observe all analyses (reactive)
     */
    fun observeAllAnalyses(): Flow<List<Analysis>>

    /**
     * Delete analysis
     */
    suspend fun deleteAnalysis(analysisId: String): Result<Unit>

    /**
     * Sync analyses from Firestore
     */
    suspend fun syncAnalyses(): Result<Unit>
}
