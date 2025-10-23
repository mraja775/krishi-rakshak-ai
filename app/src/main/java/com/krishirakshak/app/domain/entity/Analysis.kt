package com.krishirakshak.app.domain.entity

/**
 * Domain entity representing a plant disease analysis.
 */
data class Analysis(
    val id: String,
    val userId: String,
    val imageUrl: String,
    val localImagePath: String?,
    val plantType: String?,
    val diseaseInfo: DiseaseInfo?,
    val confidence: Float,
    val recommendations: List<String>,
    val timestamp: Long,
    val status: AnalysisStatus
)

/**
 * Status of the analysis
 */
enum class AnalysisStatus {
    PENDING,     // Uploaded, waiting for AI
    PROCESSING,  // AI analysis in progress
    COMPLETED,   // Analysis done
    FAILED       // Analysis failed
}
