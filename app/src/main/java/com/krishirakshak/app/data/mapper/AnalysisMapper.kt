package com.krishirakshak.app.data.mapper

import com.krishirakshak.app.data.local.entity.AnalysisEntity
import com.krishirakshak.app.domain.entity.Analysis
import com.krishirakshak.app.domain.entity.AnalysisStatus
import com.krishirakshak.app.domain.entity.DiseaseInfo
import com.krishirakshak.app.domain.entity.DiseaseSeverity
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Convert AnalysisEntity to Analysis domain entity
 */
fun AnalysisEntity.toDomain(): Analysis {
    return Analysis(
        id = id,
        userId = userId,
        imageUrl = imageUrl,
        localImagePath = localImagePath,
        plantType = plantType,
        diseaseInfo = diseaseInfoJson?.let { json ->
            try {
                Json.decodeFromString<DiseaseInfoData>(json).toDomain()
            } catch (e: Exception) {
                null
            }
        },
        confidence = confidence.toFloat(),
        recommendations = try {
            Json.decodeFromString<List<String>>(recommendationsJson)
        } catch (e: Exception) {
            emptyList()
        },
        timestamp = timestamp,
        status = AnalysisStatus.valueOf(status)
    )
}

/**
 * Convert Analysis domain entity to AnalysisEntity
 */
fun Analysis.toEntity(): AnalysisEntity {
    return AnalysisEntity(
        id = id,
        userId = userId,
        imageUrl = imageUrl,
        localImagePath = localImagePath,
        plantType = plantType,
        diseaseInfoJson = diseaseInfo?.let { info ->
            Json.encodeToString(DiseaseInfoData.fromDomain(info))
        },
        confidence = confidence.toDouble(),
        recommendationsJson = Json.encodeToString(recommendations),
        timestamp = timestamp,
        status = status.name
    )
}

/**
 * Serializable data class for DiseaseInfo
 */
@kotlinx.serialization.Serializable
private data class DiseaseInfoData(
    val name: String,
    val scientificName: String?,
    val severity: String,
    val description: String,
    val symptoms: List<String>,
    val causes: List<String>,
    val preventiveMeasures: List<String>
) {
    fun toDomain() = DiseaseInfo(
        name = name,
        scientificName = scientificName,
        severity = DiseaseSeverity.valueOf(severity),
        description = description,
        symptoms = symptoms,
        causes = causes,
        preventiveMeasures = preventiveMeasures
    )

    companion object {
        fun fromDomain(disease: DiseaseInfo) = DiseaseInfoData(
            name = disease.name,
            scientificName = disease.scientificName,
            severity = disease.severity.name,
            description = disease.description,
            symptoms = disease.symptoms,
            causes = disease.causes,
            preventiveMeasures = disease.preventiveMeasures
        )
    }
}
