package com.ncert7.aitutorandlab.ui.screens.quests

import com.ncert7.aitutorandlab.domain.gamification.QuestClaimResult
import com.ncert7.aitutorandlab.domain.gamification.QuestClaimType

fun questClaimDialogCopy(type: QuestClaimType): Pair<String, String> =
    when (type) {
        QuestClaimType.SIMS ->
            "Claim sims quest" to "You finished 3 simulations today. Watch a short video to collect your gems."
        QuestClaimType.STUDY ->
            "Claim study quest" to "You finished today's plan task. Watch a short video to collect your gems."
        QuestClaimType.BONUS ->
            "Claim bonus quest" to "Both daily quests are done. Watch a short video for the bonus gems."
    }

fun questClaimResultMessage(result: QuestClaimResult): String? =
    when (result) {
        QuestClaimResult.SUCCESS -> null
        QuestClaimResult.NOT_ELIGIBLE -> "This quest can't be claimed right now."
        QuestClaimResult.NOT_READY -> "Ad is still loading. Try again in a moment."
        QuestClaimResult.AD_SKIPPED -> "Watch the full video to earn gems."
        QuestClaimResult.DAILY_CAP_REACHED -> "You've reached today's quest ad limit."
        QuestClaimResult.GRANT_FAILED -> "Could not grant gems. Try again later."
    }
