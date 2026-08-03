package com.ncert7.aitutorandlab.repository

import com.anurag.eduai.uikit.avatar.TutorConfig
import com.ncert7.aitutorandlab.domain.avatar.TutorConfigMapper
import com.google.firebase.auth.FirebaseAuth
import com.ncert7.aitutorandlab.config.AppConfig
import com.ncert7.aitutorandlab.data.firebase.model.Streak
import com.ncert7.aitutorandlab.data.firebase.model.User
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.service.sync.FirestoreSyncUtils
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class FirebaseRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val authIndexCollection = firestore.collection("auth_index")
    private val usersCollection = firestore.collection("users")
    private val streakCollection = firestore.collection("streak")
    private val gardenCollection = firestore.collection("garden")
    private val friendCodesCollection = firestore.collection("friend_codes")
    private val friendsCollection = firestore.collection("friends")

    suspend fun ensureAuthIndex(studentId: String): Boolean {
        val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid ?: return false
        if (studentId.isBlank()) return false
        return try {
            authIndexCollection.document(firebaseUid).set(
                mapOf(
                    "studentId" to studentId,
                    "appName" to AppConfig.APP_NAME,
                    "updatedAt" to System.currentTimeMillis(),
                ),
            ).await()
            DebugLogger.debugLog("FirebaseRepository", "auth_index updated uid=$firebaseUid studentId=$studentId")
            true
        } catch (e: Exception) {
            DebugLogger.errorLog("FirebaseRepository", "auth_index write failed: ${e.message}")
            false
        }
    }

    /**
     * Check if a user exists in Firestore by email and appName
     * If user exists with email but no appName field, update it with the current app
     * @return UserCheckResult indicating Found, NotFound, or Error
     */
    suspend fun checkUserExists(userId: String): UserCheckResult {
        return try {
            // Validate user ID (email) is not empty
            if (userId.isBlank()) {
                DebugLogger.errorLog("FirebaseRepository", "Cannot check user: User ID is empty")
                return UserCheckResult.Error(IllegalArgumentException("User ID cannot be empty"))
            }

            // Query by email and appName to handle multi-app scenario
            val query = usersCollection
                .whereEqualTo("email", userId)
                .whereEqualTo("appName", AppConfig.APP_NAME)
                .get()
                .await()

            if (query.documents.isEmpty()) {
                // Check if user exists with this email but without appName field
                // This handles the migration case where old users don't have appName
                val emailQuery = usersCollection
                    .whereEqualTo("email", userId)
                    .get()
                    .await()

                if (emailQuery.documents.isNotEmpty()) {
                    val existingDoc = emailQuery.documents.first()
                    val user = existingDoc.toObject(User::class.java)

                    if (user != null && user.appName.isBlank()) {
                        // User exists but has no appName - add it
                        DebugLogger.debugLog("FirebaseRepository", "Found user without appName for email: $userId, updating with current app")
                        existingDoc.reference.update("appName", AppConfig.APP_NAME).await()

                        // Return the user with updated appName
                        val updatedUser = user.copy(appName = AppConfig.APP_NAME)
                        UserCheckResult.Found(updatedUser)
                    } else {
                        // User exists but belongs to a different app
                        DebugLogger.debugLog("FirebaseRepository", "User found with email but different app: $userId")
                        UserCheckResult.NotFound
                    }
                } else {
                    DebugLogger.debugLog("FirebaseRepository", "User not found: $userId")
                    UserCheckResult.NotFound
                }
            } else {
                val snapshot = query.documents.first()
                val user = snapshot.toObject(User::class.java)
                if (user != null) {
                    DebugLogger.debugLog("FirebaseRepository", "User found for app: $userId - ${AppConfig.APP_NAME}")
                    UserCheckResult.Found(user)
                } else {
                    DebugLogger.errorLog("FirebaseRepository", "Failed to parse user data for: $userId")
                    UserCheckResult.Error(Exception("Failed to parse user data"))
                }
            }
        } catch (e: FirebaseNetworkException) {
            DebugLogger.errorLog("FirebaseRepository", "Network error checking user: ${e.message}")
            UserCheckResult.Error(NetworkException("Network error. Please check your connection and try again.", e))
        } catch (e: FirebaseFirestoreException) {
            DebugLogger.errorLog("FirebaseRepository", "Firestore error checking user: ${e.message}")
            when {
                isQuotaExceeded(e) -> UserCheckResult.Error(
                    Exception("Service busy, try again shortly.")
                )
                isNetworkError(e) -> UserCheckResult.Error(
                    NetworkException("Network error. Please check your connection and try again.", e)
                )
                else -> UserCheckResult.Error(e)
            }
        } catch (e: SocketTimeoutException) {
            DebugLogger.errorLog("FirebaseRepository", "Connection timeout checking user: ${e.message}")
            UserCheckResult.Error(NetworkException("Connection timeout. Please try again.", e))
        } catch (e: UnknownHostException) {
            DebugLogger.errorLog("FirebaseRepository", "No internet connection: ${e.message}")
            UserCheckResult.Error(NetworkException("No internet connection. Please check your network.", e))
        } catch (e: IOException) {
            DebugLogger.errorLog("FirebaseRepository", "I/O error checking user: ${e.message}")
            UserCheckResult.Error(NetworkException("Network error occurred. Please try again.", e))
        } catch (e: Exception) {
            DebugLogger.errorLog("FirebaseRepository", "Unexpected error checking user: ${e.message}")
            UserCheckResult.Error(e)
        }
    }

    suspend fun createNewUser(user: User): Boolean {
        return try {
            // Validate user ID is not empty
            if (user.id.isBlank()) {
                DebugLogger.errorLog("FirebaseRepository", "Cannot create user: User ID is empty")
                throw IllegalArgumentException("User ID cannot be empty")
            }

            val appName = AppConfig.APP_NAME
            DebugLogger.debugLog("FirebaseRepository", "Creating/updating user: email=${user.email}, app=$appName")

            val data = mapOf(
                "id" to user.id,
                "email" to user.email,
                "displayName" to user.displayName,
                "profilePictureUri" to user.profilePictureUri,
                "schoolName" to user.schoolName,
                "phoneNumber" to user.phoneNumber,
                "studentClass" to user.studentClass,
                "language" to user.language,
                "createdAt" to user.createdAt,
                "updatedAt" to user.lastLogin,
                "appName" to appName
            )

            // Check if user already exists by email and appName
            val existingQuery = usersCollection
                .whereEqualTo("email", user.email)
                .whereEqualTo("appName", appName)
                .get()
                .await()

            if (existingQuery.documents.isNotEmpty()) {
                // User exists - update instead of create
                val docId = existingQuery.documents.first().id
                usersCollection.document(docId).set(data).await()
                DebugLogger.debugLog("FirebaseRepository", "User updated successfully: ${user.email} for app: $appName")
            } else {
                // New user - use userId (Google ID) as document ID
                usersCollection.document(user.id).set(data).await()
                DebugLogger.debugLog("FirebaseRepository", "User created successfully: ${user.id} for app: $appName")
            }
            true
        } catch (e: FirebaseNetworkException) {
            DebugLogger.errorLog("FirebaseRepository", "Network error creating user: ${e.message}")
            throw NetworkException("Network error. Please check your connection and try again.", e)
        } catch (e: FirebaseFirestoreException) {
            DebugLogger.errorLog("FirebaseRepository", "Firestore error creating user: ${e.message}")
            if (isNetworkError(e)) {
                throw NetworkException("Network error. Please check your connection and try again.", e)
            } else {
                throw e
            }
        } catch (e: SocketTimeoutException) {
            DebugLogger.errorLog("FirebaseRepository", "Connection timeout creating user: ${e.message}")
            throw NetworkException("Connection timeout. Please try again.", e)
        } catch (e: UnknownHostException) {
            DebugLogger.errorLog("FirebaseRepository", "No internet connection: ${e.message}")
            throw NetworkException("No internet connection. Please check your network.", e)
        } catch (e: IOException) {
            DebugLogger.errorLog("FirebaseRepository", "I/O error creating user: ${e.message}")
            throw NetworkException("Network error occurred. Please try again.", e)
        } catch (e: Exception) {
            DebugLogger.errorLog("FirebaseRepository", "Error creating user: ${e.message}")
            throw e
        }
    }

    suspend fun fetchTutorConfig(userId: String): TutorConfigMapper.RemoteTutorConfig? {
        if (userId.isBlank()) return null
        return try {
            val snapshot = usersCollection.document(userId).get().await()
            if (!snapshot.exists()) return null
            @Suppress("UNCHECKED_CAST")
            val raw = snapshot.get("tutorConfig") as? Map<String, Any> ?: return null
            TutorConfigMapper.fromFirestoreMap(raw)
        } catch (e: Exception) {
            DebugLogger.errorLog("FirebaseRepository", "Tutor config fetch failed: ${e.message}")
            null
        }
    }

    suspend fun syncTutorConfig(
        userId: String,
        config: TutorConfig,
        presetId: String?,
        updatedAt: Long = System.currentTimeMillis(),
    ): Boolean {
        if (userId.isBlank()) return false
        return try {
            usersCollection.document(userId)
                .set(
                    mapOf("tutorConfig" to TutorConfigMapper.toFirestoreMap(config, presetId, updatedAt)),
                    com.google.firebase.firestore.SetOptions.merge(),
                )
                .await()
            DebugLogger.debugLog("FirebaseRepository", "Tutor config synced for $userId")
            true
        } catch (e: Exception) {
            DebugLogger.errorLog("FirebaseRepository", "Tutor config sync failed: ${e.message}")
            false
        }
    }

    suspend fun updateUserProfile(
        userId: String,
        name: String,
        phone: String,
        school: String,
        studentClass: Int,
        updatedAt: Long
    ): Boolean {
        return try {
            usersCollection.document(userId)
                .update(
                    mapOf(
                        "displayName" to name,
                        "phoneNumber" to phone,
                        "schoolName" to school,
                        "studentClass" to studentClass,
                        "updatedAt" to updatedAt
                    )
                )
                .await()
            DebugLogger.debugLog("FirebaseRepository", "User profile updated: $userId")
            true
        } catch (e: FirebaseNetworkException) {
            DebugLogger.errorLog("FirebaseRepository", "Network error updating profile: ${e.message}")
            throw NetworkException("Network error. Please check your connection and try again.", e)
        } catch (e: FirebaseFirestoreException) {
            DebugLogger.errorLog("FirebaseRepository", "Firestore error updating profile: ${e.message}")
            if (isNetworkError(e)) {
                throw NetworkException("Network error. Please check your connection and try again.", e)
            } else {
                throw e
            }
        } catch (e: SocketTimeoutException) {
            DebugLogger.errorLog("FirebaseRepository", "Connection timeout updating profile: ${e.message}")
            throw NetworkException("Connection timeout. Please try again.", e)
        } catch (e: UnknownHostException) {
            DebugLogger.errorLog("FirebaseRepository", "No internet connection: ${e.message}")
            throw NetworkException("No internet connection. Please check your network.", e)
        } catch (e: IOException) {
            DebugLogger.errorLog("FirebaseRepository", "I/O error updating profile: ${e.message}")
            throw NetworkException("Network error occurred. Please try again.", e)
        } catch (e: Exception) {
            DebugLogger.errorLog("FirebaseRepository", "Error updating profile: ${e.message}")
            throw e
        }
    }

    /**
     * Check if a Firestore exception is network-related
     */
    private fun isNetworkError(exception: FirebaseFirestoreException): Boolean {
        return when (exception.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> true
            else -> {
                val message = exception.message?.lowercase() ?: ""
                message.contains("network") ||
                        message.contains("timeout") ||
                        message.contains("connection") ||
                        message.contains("unavailable") ||
                        message.contains("quota")
            }
        }
    }

    private fun isQuotaExceeded(exception: FirebaseFirestoreException): Boolean {
        return exception.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ||
            exception.message?.contains("quota", ignoreCase = true) == true
    }


    suspend fun getStreak(userId: String): Streak? {
        return try {
            val studentAppDocId = "${AppConfig.APP_NAME}_$userId"
            val snapshot = streakCollection.document(studentAppDocId)
                .collection("data")
                .document("current")
                .get()
                .await()
            
            if (snapshot.exists()) {
                val streak = snapshot.toObject(Streak::class.java)
                DebugLogger.debugLog("FirebaseRepository", "Streak retrieved from $studentAppDocId: count=${streak?.streakCount}")
                streak
            } else {
                DebugLogger.debugLog("FirebaseRepository", "No streak found for user: $studentAppDocId")
                null
            }
        } catch (e: Exception) {
            DebugLogger.errorLog("FirebaseRepository", "Error getting streak: ${e.message}")
            null
        }
    }

    suspend fun updateStreak(userId: String, streakCount: Int, lastStreakDate: Long): Boolean {
        return try {
            if (userId.isBlank()) {
                DebugLogger.errorLog("FirebaseRepository", "Cannot update streak: User ID is empty")
                return false
            }

            val studentAppDocId = "${AppConfig.APP_NAME}_$userId"
            val streak = Streak(
                userId = userId,
                streakCount = streakCount,
                lastStreakDate = lastStreakDate,
                updatedAt = System.currentTimeMillis(),
                appName = AppConfig.APP_NAME
            )

            streakCollection.document(studentAppDocId)
                .collection("data")
                .document("current")
                .set(streak)
                .await()
            
            DebugLogger.debugLog("FirebaseRepository", "Streak updated for $studentAppDocId: count=$streakCount")
            true
        } catch (e: FirebaseNetworkException) {
            DebugLogger.errorLog("FirebaseRepository", "Network error updating streak: ${e.message}")
            false
        } catch (e: FirebaseFirestoreException) {
            DebugLogger.errorLog("FirebaseRepository", "Firestore error updating streak: ${e.message}")
            false
        } catch (e: Exception) {
            DebugLogger.errorLog("FirebaseRepository", "Error updating streak: ${e.message}")
            false
        }
    }

    // ---- Garden / space reward state (mirrors the streak path: garden/{appName_studentId}/...) ----

    suspend fun saveGardenState(
        userId: String,
        theme: String,
        route: String,
        steps: Int,
        preferredSlot: Int,
    ): Boolean {
        return try {
            if (userId.isBlank()) return false
            val docId = "${AppConfig.APP_NAME}_$userId"
            val data =
                mapOf(
                    "studentId" to userId,
                    "theme" to theme,
                    "route" to route,
                    "steps" to steps,
                    "preferredSlot" to preferredSlot,
                    "appName" to AppConfig.APP_NAME,
                    "updatedAt" to System.currentTimeMillis(),
                )
            gardenCollection.document(docId).collection("state").document("current").set(data).await()
            true
        } catch (e: Exception) {
            DebugLogger.errorLog("FirebaseRepository", "Error saving garden state: ${e.message}")
            false
        }
    }

    suspend fun getGardenState(userId: String): Map<String, Any?>? {
        return try {
            if (userId.isBlank()) return null
            val docId = "${AppConfig.APP_NAME}_$userId"
            val snap =
                gardenCollection.document(docId).collection("state").document("current").get().await()
            if (snap.exists()) snap.data else null
        } catch (e: Exception) {
            DebugLogger.errorLog("FirebaseRepository", "Error getting garden state: ${e.message}")
            null
        }
    }

    /** Upserts each planted item under garden/{doc}/items/{itemId}. Idempotent (item id is stable). */
    suspend fun saveGardenItems(userId: String, items: List<Map<String, Any?>>): Boolean {
        return try {
            if (userId.isBlank() || items.isEmpty()) return true
            val docId = "${AppConfig.APP_NAME}_$userId"
            val col = gardenCollection.document(docId).collection("items")
            val batch = firestore.batch()
            items.forEach { item ->
                val id = item["id"] as? String ?: return@forEach
                batch.set(col.document(id), item)
            }
            batch.commit().await()
            true
        } catch (e: Exception) {
            DebugLogger.errorLog("FirebaseRepository", "Error saving garden items: ${e.message}")
            false
        }
    }

    suspend fun getGardenItems(userId: String): List<Map<String, Any?>> {
        return try {
            if (userId.isBlank()) return emptyList()
            val docId = "${AppConfig.APP_NAME}_$userId"
            val snap = gardenCollection.document(docId).collection("items").get().await()
            snap.documents.mapNotNull { it.data }
        } catch (e: Exception) {
            DebugLogger.errorLog("FirebaseRepository", "Error getting garden items: ${e.message}")
            emptyList()
        }
    }

    data class RemoteFriendCode(
        val studentId: String,
        val displayName: String,
    )

    data class RemoteFriendConnection(
        val studentId: String,
        val friendStudentId: String,
        val displayName: String,
        val status: String,
        val updatedAt: Long,
    )

    data class RemoteFriendFeedItem(
        val ownerStudentId: String,
        val fromStudentId: String,
        val fromDisplayName: String,
        val eventType: String,
        val message: String,
        val eventKey: String,
        val cheers: Int,
        val createdAt: Long,
    )

    private fun friendDoc(studentId: String) =
        friendsCollection.document(FirestoreSyncUtils.studentAppDocId(studentId))

    suspend fun registerFriendCode(code: String, studentId: String, displayName: String): Boolean {
        if (code.isBlank() || studentId.isBlank()) return false
        return try {
            friendCodesCollection.document(code.uppercase()).set(
                mapOf(
                    "studentId" to studentId,
                    "appName" to AppConfig.APP_NAME,
                    "updatedAt" to System.currentTimeMillis(),
                ),
            ).await()
            true
        } catch (e: Exception) {
            DebugLogger.errorLog("FirebaseRepository", "Failed to register friend code: ${e.message}")
            false
        }
    }

    suspend fun lookupFriendCode(code: String): RemoteFriendCode? {
        if (code.isBlank()) return null
        return try {
            val snapshot = friendCodesCollection.document(code.uppercase()).get().await()
            if (!snapshot.exists()) return null
            val appName = snapshot.getString("appName").orEmpty()
            if (appName != AppConfig.APP_NAME) return null
            RemoteFriendCode(
                studentId = snapshot.getString("studentId").orEmpty(),
                displayName = "",
            ).takeIf { it.studentId.isNotBlank() }
        } catch (e: Exception) {
            DebugLogger.errorLog("FirebaseRepository", "Friend code lookup failed: ${e.message}")
            null
        }
    }

    suspend fun fetchUserDisplayName(userId: String): String? {
        if (userId.isBlank()) return null
        return try {
            val snapshot = usersCollection.document(userId).get().await()
            if (!snapshot.exists()) return null
            snapshot.getString("displayName")?.trim()?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            DebugLogger.errorLog("FirebaseRepository", "Failed to fetch user display name: ${e.message}")
            null
        }
    }

    suspend fun fetchFriendCodeDisplayName(studentId: String): String? {
        if (studentId.isBlank()) return null
        return try {
            val snapshot =
                friendCodesCollection
                    .whereEqualTo("studentId", studentId)
                    .whereEqualTo("appName", AppConfig.APP_NAME)
                    .limit(1)
                    .get()
                    .await()
            snapshot.documents.firstOrNull()?.getString("displayName")?.trim()?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            DebugLogger.errorLog("FirebaseRepository", "Failed to fetch friend-code display name: ${e.message}")
            null
        }
    }

    suspend fun upsertFriendConnection(
        ownerStudentId: String,
        friendStudentId: String,
        displayName: String,
    ): Boolean {
        if (ownerStudentId.isBlank() || friendStudentId.isBlank()) return false
        return try {
            friendDoc(ownerStudentId)
                .collection("connections")
                .document(friendStudentId)
                .set(
                    mapOf(
                        "studentId" to ownerStudentId,
                        "friendStudentId" to friendStudentId,
                        "displayName" to displayName,
                        "status" to "ACCEPTED",
                        "appName" to AppConfig.APP_NAME,
                        "updatedAt" to System.currentTimeMillis(),
                    ),
                ).await()
            true
        } catch (e: Exception) {
            DebugLogger.errorLog("FirebaseRepository", "Failed to upsert friend connection: ${e.message}")
            false
        }
    }

    suspend fun publishFriendFeedItem(
        ownerStudentId: String,
        fromStudentId: String,
        fromDisplayName: String,
        eventType: String,
        message: String,
        eventKey: String,
        createdAt: Long = System.currentTimeMillis(),
    ): Boolean {
        if (ownerStudentId.isBlank() || eventKey.isBlank()) return false
        return try {
            friendDoc(ownerStudentId)
                .collection("feed")
                .document(eventKey)
                .set(
                    mapOf(
                        "ownerStudentId" to ownerStudentId,
                        "fromStudentId" to fromStudentId,
                        "fromDisplayName" to fromDisplayName,
                        "eventType" to eventType,
                        "message" to message,
                        "eventKey" to eventKey,
                        "cheers" to 0,
                        "createdAt" to createdAt,
                        "appName" to AppConfig.APP_NAME,
                    ),
                ).await()
            true
        } catch (e: Exception) {
            DebugLogger.errorLog("FirebaseRepository", "Failed to publish friend feed item: ${e.message}")
            false
        }
    }

    suspend fun fetchFriendConnections(ownerStudentId: String): List<RemoteFriendConnection> {
        if (ownerStudentId.isBlank()) return emptyList()
        return try {
            val snapshot =
                friendDoc(ownerStudentId)
                    .collection("connections")
                    .get()
                    .await()
            snapshot.documents.mapNotNull { doc ->
                val studentId = doc.getString("studentId").orEmpty()
                val friendStudentId = doc.getString("friendStudentId").orEmpty()
                if (studentId.isBlank() || friendStudentId.isBlank()) return@mapNotNull null
                RemoteFriendConnection(
                    studentId = studentId,
                    friendStudentId = friendStudentId,
                    displayName = doc.getString("displayName").orEmpty(),
                    status = doc.getString("status") ?: "ACCEPTED",
                    updatedAt = doc.getLong("updatedAt") ?: 0L,
                )
            }
        } catch (e: Exception) {
            DebugLogger.errorLog("FirebaseRepository", "Failed to fetch friend connections: ${e.message}")
            emptyList()
        }
    }

    suspend fun fetchFriendFeed(ownerStudentId: String, limit: Int = 20): List<RemoteFriendFeedItem> {
        if (ownerStudentId.isBlank()) return emptyList()
        return try {
            val snapshot =
                friendDoc(ownerStudentId)
                    .collection("feed")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(limit.toLong())
                    .get()
                    .await()
            snapshot.documents.mapNotNull { doc ->
                val ownerId = doc.getString("ownerStudentId").orEmpty()
                val eventKey = doc.getString("eventKey").orEmpty()
                if (ownerId.isBlank() || eventKey.isBlank()) return@mapNotNull null
                RemoteFriendFeedItem(
                    ownerStudentId = ownerId,
                    fromStudentId = doc.getString("fromStudentId").orEmpty(),
                    fromDisplayName = doc.getString("fromDisplayName").orEmpty(),
                    eventType = doc.getString("eventType").orEmpty(),
                    message = doc.getString("message").orEmpty(),
                    eventKey = eventKey,
                    cheers = doc.getLong("cheers")?.toInt() ?: 0,
                    createdAt = doc.getLong("createdAt") ?: 0L,
                )
            }
        } catch (e: Exception) {
            DebugLogger.errorLog("FirebaseRepository", "Failed to fetch friend feed: ${e.message}")
            emptyList()
        }
    }

}

/**
 * Result type for user existence check
 */
sealed class UserCheckResult {
    data class Found(val user: User) : UserCheckResult()
    object NotFound : UserCheckResult()
    data class Error(val exception: Throwable) : UserCheckResult()
}

/**
 * Custom exception for network-related errors
 */
class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)
