package com.ncert7.aitutorandlab.ui.screens.plan

import com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemEntity
import com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemKind

/** Parsed display parts of a materialized trial title: `Chapter · Kind · Detail`. */
data class TrialItemTitleParts(
    val chapter: String,
    val kind: String,
    val detail: String,
)

object TrialItemTitleParser {
    /** Middle dot, en dash, em dash, hyphen — titles may vary by locale/device encoding. */
    private val SEPARATOR = Regex("""\s+[·–—-]\s+""")

    fun parse(title: String): TrialItemTitleParts {
        val parts = title.split(SEPARATOR).map { it.trim() }.filter { it.isNotBlank() }
        return when {
            parts.size >= 3 ->
                TrialItemTitleParts(
                    chapter = parts[0],
                    kind = parts[1],
                    detail = parts.drop(2).joinToString(" · "),
                )
            parts.size == 2 -> TrialItemTitleParts(chapter = parts[0], kind = parts[1], detail = "")
            parts.size == 1 -> TrialItemTitleParts(chapter = parts[0], kind = "", detail = "")
            else -> TrialItemTitleParts(chapter = "", kind = "", detail = title)
        }
    }

    /** Hero headline — chapter for sims, concept for study/revision. */
    fun heroTitle(
        item: PlanTrialItemEntity?,
        todayPlanLabel: String,
        fallbackConceptName: String,
        selectedSubjectName: String,
    ): String {
        if (item == null) {
            return todayPlanLabel.ifBlank {
                fallbackConceptName.ifBlank { selectedSubjectName.ifBlank { "Today's plan" } }
            }
        }
        val parts = parse(item.title)
        return when (item.kind) {
            PlanTrialItemKind.SIM_URL,
            PlanTrialItemKind.SIM_AGENT,
            -> parts.chapter.ifBlank { todayPlanLabel }.ifBlank { fallbackConceptName }
            PlanTrialItemKind.STUDY,
            PlanTrialItemKind.REVISION,
            -> parts.detail.ifBlank { parts.chapter }.ifBlank { todayPlanLabel }
            else -> parts.detail.ifBlank { parts.chapter }.ifBlank { todayPlanLabel }
        }
    }

    /** Hero subtitle line — kind + activity, not the full chapter prefix again. */
    fun heroNextLine(title: String, kannada: Boolean): String {
        val parts = parse(title)
        val line =
            when {
                parts.kind.isNotBlank() && parts.detail.isNotBlank() -> "${parts.kind} · ${parts.detail}"
                parts.detail.isNotBlank() -> parts.detail
                parts.kind.isNotBlank() -> parts.kind
                else -> title
            }
        return if (kannada) "ಮುಂದಿನ: $line" else "Next: $line"
    }
}
