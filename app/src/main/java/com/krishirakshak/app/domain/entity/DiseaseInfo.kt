package com.krishirakshak.app.domain.entity

/**
 * Domain entity representing disease information.
 */
data class DiseaseInfo(
    val name: String,
    val scientificName: String?,
    val severity: DiseaseSeverity,
    val description: String,
    val symptoms: List<String>,
    val causes: List<String>,
    val preventiveMeasures: List<String>
)

/**
 * Disease severity levels
 */
enum class DiseaseSeverity {
    LOW,
    MODERATE,
    SEVERE,
    CRITICAL
}
