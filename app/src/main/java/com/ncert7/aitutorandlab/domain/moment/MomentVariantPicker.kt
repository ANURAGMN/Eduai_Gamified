package com.ncert7.aitutorandlab.domain.moment

import com.anurag.eduai.uikit.avatar.AllAvatarPresets
import com.anurag.eduai.uikit.avatar.AvatarPreset
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Picks a fresh moment screen each time: one of the five variants for the moment, plus a random
 * avatar look — never repeating the previous variant or avatar back-to-back (tracked in-memory for
 * the app session, which is enough to avoid an obvious repeat).
 */
@Singleton
class MomentVariantPicker
    @Inject
    constructor() {
        private val random: Random = Random.Default
        private val lastVariantByMoment = mutableMapOf<MomentType, String>()
        private var lastAvatarId: String? = null

        fun pick(
            moment: MomentType,
            tokens: MomentTokens = MomentTokens(),
            avatarPool: List<AvatarPreset> = AllAvatarPresets,
            languageCode: String = "en",
        ): MomentUiModel {
            val variant =
                pickAvoidingRepeat(
                    items = MomentVariants.forMoment(moment, languageCode),
                    lastId = lastVariantByMoment[moment],
                    idOf = { it.id },
                )
            lastVariantByMoment[moment] = variant.id

            val pool = avatarPool.ifEmpty { AllAvatarPresets }
            val avatar =
                pickAvoidingRepeat(
                    items = pool,
                    lastId = lastAvatarId,
                    idOf = { it.id },
                )
            lastAvatarId = avatar.id

            return MomentUiModel(
                moment = moment,
                variantId = variant.id,
                celebratory = variant.celebratory,
                emotion = variant.emotion,
                avatarConfig = avatar.config,
                headline = tokens.fill(variant.headline),
                body = tokens.fill(variant.body),
                primaryCta = variant.primaryCta,
                secondaryCta = variant.secondaryCta,
                gems = tokens.gems,
                xp = tokens.xp,
            )
        }

        private fun <T> pickAvoidingRepeat(
            items: List<T>,
            lastId: String?,
            idOf: (T) -> String,
        ): T {
            if (items.isEmpty()) error("MomentVariantPicker: empty pool")
            if (items.size == 1) return items.first()
            val candidates = items.filter { idOf(it) != lastId }.ifEmpty { items }
            return candidates[random.nextInt(candidates.size)]
        }
    }
