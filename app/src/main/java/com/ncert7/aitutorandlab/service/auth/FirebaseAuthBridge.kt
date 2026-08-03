package com.ncert7.aitutorandlab.service.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.ncert7.aitutorandlab.debug.DebugLogger
import kotlinx.coroutines.tasks.await

/**
 * Signs the user into Firebase Auth so Firestore rules can use [FirebaseAuth.getCurrentUser].
 * The app's canonical [studentId] (Google subject id or @padaams.in email) stays unchanged;
 * [FirebaseRepository.ensureAuthIndex] maps auth uid → studentId for rules.
 */
object FirebaseAuthBridge {

    private const val TAG = "FirebaseAuthBridge"

    suspend fun signInWithGoogleIdToken(idToken: String): FirebaseUser {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = FirebaseAuth.getInstance().signInWithCredential(credential).await()
        val user =
            result.user
                ?: throw IllegalStateException("Firebase Auth returned no user after Google sign-in")
        DebugLogger.debugLog(TAG, "Firebase Auth OK uid=${user.uid} email=${user.email}")
        return user
    }

    suspend fun signInWithEmailPassword(email: String, password: String): FirebaseUser {
        val auth = FirebaseAuth.getInstance()
        val result =
            try {
                auth.signInWithEmailAndPassword(email, password).await()
            } catch (signInError: Exception) {
                DebugLogger.debugLog(TAG, "Email sign-in failed, trying create: ${signInError.message}")
                auth.createUserWithEmailAndPassword(email, password).await()
            }
        val user =
            result.user
                ?: throw IllegalStateException("Firebase Auth returned no user after email sign-in")
        DebugLogger.debugLog(TAG, "Firebase email Auth OK uid=${user.uid}")
        return user
    }

    fun currentUid(): String? = FirebaseAuth.getInstance().currentUser?.uid

    fun signOut() {
        FirebaseAuth.getInstance().signOut()
    }
}
