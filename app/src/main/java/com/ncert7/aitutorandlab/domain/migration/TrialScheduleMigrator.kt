package com.ncert7.aitutorandlab.domain.migration

/** Rebuilds exam-trial queues for all plan days (used by app-data migrations). */
fun interface TrialScheduleMigrator {
    suspend fun materializeAllPlanDays(studentId: String, languageCode: String)
}
