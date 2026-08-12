package com.ncert7.aitutorandlab.utils

import com.ncert7.aitutorandlab.data.local.dao.ConceptDao
import com.ncert7.aitutorandlab.data.local.dao.ChapterDao
import com.ncert7.aitutorandlab.data.local.entities.ExamPlanDayEntity
import com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemEntity
import com.ncert7.aitutorandlab.utils.ExamPlanCopy
import com.ncert7.aitutorandlab.utils.isKannadaLanguage

/** Resolves exam-plan and trial labels from syllabus entities at display time. */
object TrialTitleResolver {

    suspend fun localizedItemTitle(
        entity: PlanTrialItemEntity,
        languageCode: String,
        conceptDao: ConceptDao,
        chapterDao: ChapterDao,
    ): String {
        val concept = conceptDao.getConcept(entity.conceptId) ?: return entity.title
        val conceptName = concept.getLocalizedName(languageCode)
        val chapterName =
            chapterDao.getChapter(entity.chapterId)?.getLocalizedName(languageCode)
                ?: entity.title.substringBefore(" · ").trim()
        return TrialCopy.itemTitle(
            languageCode = languageCode,
            chapterName = chapterName,
            kind = entity.kind,
            conceptName = conceptName,
        )
    }

    suspend fun localizedPlanDayLabel(
        day: ExamPlanDayEntity,
        languageCode: String,
        conceptDao: ConceptDao,
        chapterDao: ChapterDao,
    ): String {
        when (day.dayType) {
            "REVISE", "MOCK", "EXAM" ->
                return ExamPlanCopy.localizedStoredDayLabel(languageCode, day.dayType, day.label)
            "CHAPTER_TRIAL" -> {
                // conceptIds stores the chapter id for chapter trials; day.label is frozen at
                // first create language and must not be shown after a language switch.
                val chapterId = day.conceptIds.trim().substringBefore(',').trim()
                return chapterDao.getChapter(chapterId)?.getLocalizedName(languageCode)
                    ?: day.label
            }
            else -> {
                val conceptIds =
                    day.conceptIds
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                if (conceptIds.isEmpty()) {
                    return if (isKannadaLanguage(languageCode)) {
                        "ನಿಮ್ಮ ಮೊದಲ ಅಧ್ಯಾಯವನ್ನು ಅನ್ವೇಷಿಸಿ"
                    } else {
                        "Explore your first chapter"
                    }
                }
                val concepts = conceptIds.mapNotNull { conceptDao.getConcept(it) }
                if (concepts.isEmpty()) return day.label
                if (concepts.size == 1) {
                    return concepts.first().getLocalizedName(languageCode)
                }
                val first = concepts.first().getLocalizedName(languageCode)
                val suffix =
                    if (isKannadaLanguage(languageCode)) {
                        " +${concepts.size - 1} ಹೆಚ್ಚು"
                    } else {
                        " +${concepts.size - 1} more"
                    }
                return first + suffix
            }
        }
    }
}
