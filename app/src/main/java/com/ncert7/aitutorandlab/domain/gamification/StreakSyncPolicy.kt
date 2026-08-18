package com.ncert7.aitutorandlab.domain.gamification

import com.ncert7.aitutorandlab.data.firebase.model.Streak
import com.ncert7.aitutorandlab.data.local.entities.StreakEntity
import kotlin.math.max

/**
 * Login restore + Firestore upload guards.
 *
 * Reinstall / empty Room used to seed streak=1 and upload it, clobbering a live cloud streak.
 * These merges keep a live remote count and only allow a drop to 1 after a real calendar gap.
 */
object StreakSyncPolicy {

    data class CloudWrite(val streakCount: Int, val lastStreakDate: Long)

    /**
     * Combine unsynced local with remote on login. A first-day seed (count ≤ 1) never beats a
     * higher streak that is still live (last activity today or yesterday).
     */
    fun mergeForLogin(
        local: StreakEntity,
        remote: Streak,
        nowMs: Long = System.currentTimeMillis(),
    ): StreakEntity {
        val remoteEntity = StreakEntity(
            userId = remote.userId.ifBlank { local.userId },
            streakCount = remote.streakCount,
            lastStreakDate = remote.lastStreakDate,
            createdAt = minOf(local.createdAt, remote.createdAt),
            updatedAt = remote.updatedAt,
            appName = remote.appName.ifBlank { local.appName },
            isSynced = true,
        )

        if (local.lastStreakDate == remote.lastStreakDate) {
            val bestCount = max(local.streakCount, remote.streakCount)
            val needsUpload = bestCount > remote.streakCount
            return local.copy(
                streakCount = bestCount,
                createdAt = minOf(local.createdAt, remote.createdAt),
                updatedAt = maxOf(local.updatedAt, remote.updatedAt),
                isSynced = !needsUpload,
            )
        }

        val remoteLive =
            StreakDayLogic.isSameDay(remote.lastStreakDate, nowMs) ||
                StreakDayLogic.isConsecutiveDay(remote.lastStreakDate, nowMs)

        // Empty-Room seed after reinstall — restore the live cloud streak.
        if (local.streakCount <= 1 && remote.streakCount > local.streakCount && remoteLive) {
            return remoteEntity
        }

        // Device truly ahead (offline continuation): later day and count did not drop.
        if (
            !local.isSynced &&
            local.lastStreakDate > remote.lastStreakDate &&
            local.streakCount >= remote.streakCount
        ) {
            return local.copy(isSynced = false)
        }

        return if (local.lastStreakDate > remote.lastStreakDate) {
            local.copy(isSynced = false)
        } else {
            remoteEntity
        }
    }

    /**
     * Last-write guard before Firestore set. Same day never decreases; a seed of 1 cannot
     * replace a live remote; a next-day seed continues the remote instead of resetting.
     */
    fun mergeCloudWrite(
        incomingCount: Int,
        incomingLastDate: Long,
        remoteCount: Int,
        remoteLastDate: Long,
        nowMs: Long = System.currentTimeMillis(),
    ): CloudWrite {
        if (remoteCount <= 0 || remoteLastDate <= 0L) {
            return CloudWrite(incomingCount, incomingLastDate)
        }

        if (StreakDayLogic.isSameDay(remoteLastDate, incomingLastDate)) {
            return CloudWrite(max(incomingCount, remoteCount), incomingLastDate)
        }

        val remoteLive =
            StreakDayLogic.isSameDay(remoteLastDate, nowMs) ||
                StreakDayLogic.isConsecutiveDay(remoteLastDate, nowMs)

        if (incomingCount <= 1 && remoteCount > incomingCount && remoteLive) {
            if (StreakDayLogic.isConsecutiveDay(remoteLastDate, incomingLastDate)) {
                return CloudWrite(remoteCount + 1, incomingLastDate)
            }
            return CloudWrite(remoteCount, remoteLastDate)
        }

        if (StreakDayLogic.isConsecutiveDay(remoteLastDate, incomingLastDate)) {
            return CloudWrite(max(incomingCount, remoteCount + 1), incomingLastDate)
        }

        return CloudWrite(incomingCount, incomingLastDate)
    }
}
