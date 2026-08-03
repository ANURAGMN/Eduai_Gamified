package com.ncert7.aitutorandlab.domain.examplan

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Serializes exam-plan day replacement and trial-item materialization so trial rows
 * are never inserted with a stale [planDayId] while plan days are being recreated.
 */
@Singleton
class ExamPlanMutationLock @Inject constructor() {
    private val mutex = Mutex()

    suspend fun <T> withPlanMutation(block: suspend () -> T): T = mutex.withLock { block() }
}
