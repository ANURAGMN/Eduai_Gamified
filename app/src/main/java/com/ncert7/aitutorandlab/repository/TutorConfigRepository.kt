package com.ncert7.aitutorandlab.repository

import android.content.Context
import com.anurag.eduai.uikit.avatar.AllAvatarPresets
import com.anurag.eduai.uikit.avatar.TutorConfig
import com.anurag.eduai.uikit.avatar.TutorConfigStore
import com.ncert7.aitutorandlab.data.local.dao.TutorConfigDao
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.domain.avatar.TutorConfigMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TutorConfigRepository @Inject constructor(
    private val tutorConfigDao: TutorConfigDao,
    private val firebaseRepository: FirebaseRepository,
) {
    private val configMutex = Mutex()

    private val defaultConfig: TutorConfig =
        AllAvatarPresets.firstOrNull { it.id == "scholar" }?.config
            ?: TutorConfig(character = com.anurag.eduai.uikit.avatar.core.TutorCharacter.Free)

    suspend fun ensureLoaded(context: Context, studentId: String) {
        if (!isValidStudentId(studentId)) return
        configMutex.withLock {
            withContext(Dispatchers.IO) {
                val local = tutorConfigDao.get(studentId)
                val remote = runCatching { firebaseRepository.fetchTutorConfig(studentId) }.getOrNull()

                val localUpdated = local?.updatedAt ?: 0L
                val remoteUpdated = remote?.updatedAt ?: 0L

                when {
                    remote != null && remoteUpdated >= localUpdated -> {
                        persistLocally(
                            context = context,
                            studentId = studentId,
                            config = remote.config,
                            presetId = remote.presetId,
                            updatedAt = remoteUpdated.takeIf { it > 0 } ?: System.currentTimeMillis(),
                            isSynced = true,
                        )
                    }
                    local != null -> {
                        applyToStore(context, TutorConfigMapper.toUi(local))
                    }
                    else -> seedDefaultLocked(context, studentId)
                }
            }
        }
    }

    suspend fun save(
        context: Context,
        studentId: String,
        config: TutorConfig,
        presetId: String? = null,
        syncRemote: Boolean = true,
    ) {
        if (!isValidStudentId(studentId)) {
            withContext(Dispatchers.Main.immediate) {
                TutorConfigStore.save(context, config)
            }
            return
        }
        configMutex.withLock {
            saveLocked(context, studentId, config, presetId, syncRemote)
        }
    }

    suspend fun applyPreset(context: Context, studentId: String, presetId: String) {
        val preset = AllAvatarPresets.find { it.id == presetId } ?: return
        save(context, studentId, preset.config, presetId = preset.id)
    }

    /** Push any tutor looks saved offline to Firestore (called from WeeklySyncWorker). */
    suspend fun syncPendingToRemote() {
        withContext(Dispatchers.IO) {
            tutorConfigDao.getUnsynced().forEach { entity ->
                if (!isValidStudentId(entity.studentId)) return@forEach
                val config = TutorConfigMapper.toUi(entity)
                val synced =
                    runCatching {
                        firebaseRepository.syncTutorConfig(
                            entity.studentId,
                            config,
                            entity.presetId,
                            entity.updatedAt,
                        )
                    }.getOrDefault(false)
                if (synced) {
                    tutorConfigDao.upsert(entity.copy(isSynced = true))
                }
            }
        }
    }

    private suspend fun seedDefaultLocked(context: Context, studentId: String) {
        TutorConfigStore.load(context)
        val fromLegacyPrefs =
            if (context.getSharedPreferences("eduai_tutor_config", Context.MODE_PRIVATE)
                    .contains("character")
            ) {
                TutorConfigStore.state.value
            } else {
                null
            }
        val config = fromLegacyPrefs ?: defaultConfig
        saveLocked(
            context = context,
            studentId = studentId,
            config = config,
            presetId = "scholar",
            syncRemote = fromLegacyPrefs == null,
        )
    }

    private suspend fun saveLocked(
        context: Context,
        studentId: String,
        config: TutorConfig,
        presetId: String?,
        syncRemote: Boolean,
    ) {
        withContext(Dispatchers.IO) {
            val updatedAt = System.currentTimeMillis()
            var synced = !syncRemote
            if (syncRemote) {
                synced =
                    runCatching {
                        firebaseRepository.syncTutorConfig(studentId, config, presetId, updatedAt)
                    }.getOrDefault(false)
                if (!synced) {
                    DebugLogger.errorLog("TutorConfigRepository", "Firestore tutor sync failed for $studentId")
                }
            }
            persistLocally(
                context = context,
                studentId = studentId,
                config = config,
                presetId = presetId,
                updatedAt = updatedAt,
                isSynced = synced,
            )
        }
    }

    private suspend fun persistLocally(
        context: Context,
        studentId: String,
        config: TutorConfig,
        presetId: String?,
        updatedAt: Long,
        isSynced: Boolean,
    ) {
        withContext(Dispatchers.Main.immediate) {
            TutorConfigStore.save(context, config)
        }
        tutorConfigDao.upsert(
            TutorConfigMapper.toEntity(studentId, config, presetId, isSynced = isSynced)
                .copy(updatedAt = updatedAt),
        )
    }

    private suspend fun applyToStore(context: Context, config: TutorConfig) {
        withContext(Dispatchers.Main.immediate) {
            TutorConfigStore.save(context, config)
        }
    }

    private fun isValidStudentId(studentId: String): Boolean =
        studentId.isNotBlank() && studentId != "null"
}
