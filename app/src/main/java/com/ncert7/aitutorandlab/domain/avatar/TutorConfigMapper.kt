package com.ncert7.aitutorandlab.domain.avatar

import com.anurag.eduai.uikit.avatar.TutorConfig
import com.anurag.eduai.uikit.avatar.core.TutorCharacter
import com.ncert7.aitutorandlab.data.local.entities.TutorConfigEntity

object TutorConfigMapper {
    data class RemoteTutorConfig(
        val config: TutorConfig,
        val presetId: String?,
        val updatedAt: Long,
    )

    fun fromFirestoreMap(raw: Map<String, Any>): RemoteTutorConfig? {
        val characterName = raw["character"] as? String ?: return null
        return RemoteTutorConfig(
            config =
                TutorConfig(
                    character =
                        runCatching { TutorCharacter.valueOf(characterName) }
                            .getOrDefault(TutorCharacter.Free),
                    outfit = (raw["outfit"] as? Number)?.toInt() ?: 0,
                    neck = (raw["neck"] as? Number)?.toInt() ?: 0,
                    hair = (raw["hair"] as? Number)?.toInt() ?: 0,
                    hairColor = (raw["hairColor"] as? Number)?.toInt() ?: 0,
                    glasses = (raw["glasses"] as? Number)?.toInt() ?: 0,
                    frameColor = (raw["frameColor"] as? Number)?.toInt() ?: 0,
                    eyeLine = raw["eyeLine"] as? Boolean ?: false,
                    cheeks = raw["cheeks"] as? Boolean ?: true,
                ),
            presetId = raw["presetId"] as? String,
            updatedAt = (raw["updatedAt"] as? Number)?.toLong() ?: 0L,
        )
    }

    fun toEntity(
        studentId: String,
        config: TutorConfig,
        presetId: String? = null,
        isSynced: Boolean = false,
    ): TutorConfigEntity =
        TutorConfigEntity(
            studentId = studentId,
            character = config.character.name,
            outfit = config.outfit,
            neck = config.neck,
            hair = config.hair,
            hairColor = config.hairColor,
            glasses = config.glasses,
            frameColor = config.frameColor,
            eyeLine = config.eyeLine,
            cheeks = config.cheeks,
            presetId = presetId,
            updatedAt = System.currentTimeMillis(),
            isSynced = isSynced,
        )

    fun toUi(entity: TutorConfigEntity): TutorConfig =
        TutorConfig(
            character =
                runCatching { TutorCharacter.valueOf(entity.character) }
                    .getOrDefault(TutorCharacter.Free),
            outfit = entity.outfit,
            neck = entity.neck,
            hair = entity.hair,
            hairColor = entity.hairColor,
            glasses = entity.glasses,
            frameColor = entity.frameColor,
            eyeLine = entity.eyeLine,
            cheeks = entity.cheeks,
        )

    fun toFirestoreMap(config: TutorConfig, presetId: String?, updatedAt: Long = System.currentTimeMillis()): Map<String, Any> =
        buildMap {
            put("character", config.character.name)
            put("outfit", config.outfit)
            put("neck", config.neck)
            put("hair", config.hair)
            put("hairColor", config.hairColor)
            put("glasses", config.glasses)
            put("frameColor", config.frameColor)
            put("eyeLine", config.eyeLine)
            put("cheeks", config.cheeks)
            presetId?.let { put("presetId", it) }
            put("updatedAt", updatedAt)
        }
}
