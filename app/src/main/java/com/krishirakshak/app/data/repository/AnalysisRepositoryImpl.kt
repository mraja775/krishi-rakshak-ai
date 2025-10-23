package com.krishirakshak.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.krishirakshak.app.data.local.dao.AnalysisDao
import com.krishirakshak.app.data.mapper.toDomain
import com.krishirakshak.app.data.mapper.toEntity
import com.krishirakshak.app.domain.entity.Analysis
import com.krishirakshak.app.domain.entity.AnalysisStatus
import com.krishirakshak.app.domain.entity.DiseaseInfo
import com.krishirakshak.app.domain.entity.DiseaseSeverity
import com.krishirakshak.app.domain.repository.AnalysisRepository
import com.krishirakshak.app.util.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalysisRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val analysisDao: AnalysisDao
) : AnalysisRepository {

    companion object {
        private const val ANALYSES_COLLECTION = "analyses"
    }

    override suspend fun createAnalysis(analysis: Analysis): Result<String> {
        return try {
            val userId = firebaseAuth.currentUser?.uid
                ?: return Result.Error(Exception("User not authenticated"))

            // Save to Firestore
            firestore.collection(ANALYSES_COLLECTION)
                .document(analysis.id)
                .set(mapOf(
                    "id" to analysis.id,
                    "userId" to userId,
                    "imageUrl" to analysis.imageUrl,
                    "plantType" to analysis.plantType,
                    "confidence" to analysis.confidence,
                    "recommendations" to analysis.recommendations,
                    "timestamp" to analysis.timestamp,
                    "status" to analysis.status.name
                ))
                .await()

            // Save to local database
            analysisDao.insertAnalysis(analysis.toEntity())

            Timber.d("Analysis created: ${analysis.id}")
            Result.Success(analysis.id)

        } catch (e: Exception) {
            Timber.e(e, "Failed to create analysis")
            Result.Error(e, "Failed to create analysis: ${e.message}")
        }
    }

    override suspend fun getAnalysisById(analysisId: String): Result<Analysis> {
        return try {
            val entity = analysisDao.getAnalysisById(analysisId)
            if (entity != null) {
                Result.Success(entity.toDomain())
            } else {
                Result.Error(Exception("Analysis not found"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get analysis")
            Result.Error(e, "Failed to get analysis: ${e.message}")
        }
    }

    override fun observeAnalysisById(analysisId: String): Flow<Analysis?> {
        return callbackFlow {
            Timber.d("Starting Firestore listener for analysis: $analysisId")

            val listener = firestore.collection(ANALYSES_COLLECTION)
                .document(analysisId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Timber.e(error, "Error listening to analysis")
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        try {
                            val data = snapshot.data ?: return@addSnapshotListener

                            val analysis = Analysis(
                                id = data["id"] as? String ?: analysisId,
                                userId = data["userId"] as? String ?: "",
                                imageUrl = data["imageUrl"] as? String ?: "",
                                localImagePath = data["localImagePath"] as? String,
                                plantType = data["plantType"] as? String,
                                diseaseInfo = (data["diseaseInfo"] as? Map<*, *>)?.let { diseaseMap ->
                                    DiseaseInfo(
                                        name = diseaseMap["name"] as? String ?: "",
                                        scientificName = diseaseMap["scientificName"] as? String,
                                        severity = DiseaseSeverity.valueOf(
                                            diseaseMap["severity"] as? String ?: "LOW"
                                        ),
                                        description = diseaseMap["description"] as? String ?: "",
                                        symptoms = (diseaseMap["symptoms"] as? List<*>)
                                            ?.mapNotNull { it as? String } ?: emptyList(),
                                        causes = (diseaseMap["causes"] as? List<*>)
                                            ?.mapNotNull { it as? String } ?: emptyList(),
                                        preventiveMeasures = (diseaseMap["preventiveMeasures"] as? List<*>)
                                            ?.mapNotNull { it as? String } ?: emptyList()
                                    )
                                },
                                confidence = (data["confidence"] as? Number)?.toFloat() ?: 0f,
                                recommendations = (data["recommendations"] as? List<*>)
                                    ?.mapNotNull { it as? String } ?: emptyList(),
                                timestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L,
                                status = AnalysisStatus.valueOf(
                                    data["status"] as? String ?: "PENDING"
                                )
                            )

                            Timber.d("Firestore update received: ${analysis.id}, status: ${analysis.status}")
                            trySend(analysis)

                            // Also update local database
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    analysisDao.insertAnalysis(analysis.toEntity())
                                } catch (e: Exception) {
                                    Timber.e(e, "Failed to update local database")
                                }
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Error parsing analysis data")
                        }
                    } else {
                        Timber.d("Analysis document does not exist: $analysisId")
                        trySend(null)
                    }
                }

            awaitClose {
                Timber.d("Removing Firestore listener for: $analysisId")
                listener.remove()
            }
        }
    }

    override suspend fun getAllAnalyses(): Result<List<Analysis>> {
        return try {
            val userId = firebaseAuth.currentUser?.uid
                ?: return Result.Error(Exception("User not authenticated"))

            val entities = analysisDao.getAllAnalyses(userId)
            Result.Success(entities.map { it.toDomain() })
        } catch (e: Exception) {
            Timber.e(e, "Failed to get all analyses")
            Result.Error(e, "Failed to get analyses: ${e.message}")
        }
    }

    override fun observeAllAnalyses(): Flow<List<Analysis>> {
        val userId = firebaseAuth.currentUser?.uid ?: ""
        return analysisDao.getAllAnalysesFlow(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun deleteAnalysis(analysisId: String): Result<Unit> {
        return try {
            // Delete from Firestore
            firestore.collection(ANALYSES_COLLECTION)
                .document(analysisId)
                .delete()
                .await()

            // Delete from local database
            analysisDao.deleteAnalysisById(analysisId)

            Timber.d("Analysis deleted: $analysisId")
            Result.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to delete analysis")
            Result.Error(e, "Failed to delete analysis: ${e.message}")
        }
    }

    override suspend fun syncAnalyses(): Result<Unit> {
        return try {
            val userId = firebaseAuth.currentUser?.uid
                ?: return Result.Error(Exception("User not authenticated"))

            val snapshot = firestore.collection(ANALYSES_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .await()

            // This is a simplified sync - in production you'd want more sophisticated logic
            Timber.d("Synced ${snapshot.documents.size} analyses from Firestore")
            Result.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to sync analyses")
            Result.Error(e, "Sync failed: ${e.message}")
        }
    }
}
