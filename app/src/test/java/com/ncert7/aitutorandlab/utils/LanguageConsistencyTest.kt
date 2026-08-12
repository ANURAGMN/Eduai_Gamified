package com.ncert7.aitutorandlab.utils

import com.ncert7.aitutorandlab.data.local.dao.ChapterDao
import com.ncert7.aitutorandlab.data.local.dao.ConceptDao
import com.ncert7.aitutorandlab.data.local.entities.ChapterEntity
import com.ncert7.aitutorandlab.data.local.entities.ConceptEntity
import com.ncert7.aitutorandlab.data.local.entities.ExamPlanDayEntity
import com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemEntity
import com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemKind
import com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemStatus
import com.ncert7.aitutorandlab.data.local.entities.ProgressEntity
import com.ncert7.aitutorandlab.ui.screens.home.GamifiedHomeMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies language consistency when plan/trial content was materialized in one language
 * but the user switches to another after login.
 */
class LanguageConsistencyTest {

    private val concept =
        sampleConcept(
            conceptId = "concept-1",
            chapterId = "chapter-1",
            englishName = "Photosynthesis",
            kannadaName = "ಪ್ರಕಾಶ ಸಂಶ್ಲೇಷಣೆ",
        )

    private val chapter =
        sampleChapter(
            chapterId = "chapter-1",
            englishName = "Life Processes",
            kannadaName = "ಜೀವ ಪ್ರಕ್ರಿಯೆಗಳು",
        )

    /** Case 1: logged out in Kannada, plan label stored in KN, user switched to English. */
    @Test
    fun case1_knLoginThenSwitchToEnglish_heroUsesEnglishConceptName() {
        val planDay =
            lessonPlanDay(
                label = "ಪ್ರಕಾಶ ಸಂಶ್ಲೇಷಣೆ",
                conceptId = concept.conceptId,
            )

        val (state, _) =
            GamifiedHomeMapper.map(
                greeting = "Good morning",
                userName = "Student",
                streak = 0,
                todayConceptCount = 0,
                todaySimulationCount = 0,
                selectedSubjectName = "Science",
                progressConcepts = listOf(null to concept),
                progressSimulations = emptyList(),
                languageCode = "en",
                todayPlanDay = planDay,
            )

        assertEquals("Photosynthesis", state.heroTitle)
        assertFalse(state.heroTitle.contains("ಪ್ರ"))
    }

    /** Case 2: logged out in English, plan label stored in EN, user switched to Kannada. */
    @Test
    fun case2_enLoginThenSwitchToKannada_heroUsesKannadaConceptName() {
        val planDay =
            lessonPlanDay(
                label = "Photosynthesis",
                conceptId = concept.conceptId,
            )

        val (state, _) =
            GamifiedHomeMapper.map(
                greeting = "Good morning",
                userName = "Student",
                streak = 0,
                todayConceptCount = 0,
                todaySimulationCount = 0,
                selectedSubjectName = "Science",
                progressConcepts = listOf(null to concept),
                progressSimulations = emptyList(),
                languageCode = "kn",
                todayPlanDay = planDay,
            )

        assertEquals("ಪ್ರಕಾಶ ಸಂಶ್ಲೇಷಣೆ", state.heroTitle)
        assertFalse(state.heroTitle.contains("Photosynthesis"))
    }

    @Test
    fun case1_planDayLabelResolverReturnsEnglishDespiteStoredKannadaLabel() = runBlocking {
        val day =
            lessonPlanDay(
                label = "ಪ್ರಕಾಶ ಸಂಶ್ಲೇಷಣೆ",
                conceptId = concept.conceptId,
            )
        val conceptDao = FakeConceptDao(concept)
        val label = TrialTitleResolver.localizedPlanDayLabel(day, "en", conceptDao, FakeChapterDao(chapter))
        assertEquals("Photosynthesis", label)
    }

    @Test
    fun case2_planDayLabelResolverReturnsKannadaDespiteStoredEnglishLabel() = runBlocking {
        val day =
            lessonPlanDay(
                label = "Photosynthesis",
                conceptId = concept.conceptId,
            )
        val conceptDao = FakeConceptDao(concept)
        val label = TrialTitleResolver.localizedPlanDayLabel(day, "kn", conceptDao, FakeChapterDao(chapter))
        assertEquals("ಪ್ರಕಾಶ ಸಂಶ್ಲೇಷಣೆ", label)
    }

    @Test
    fun chapterTrial_localizesFromChapterEntityDespiteStoredKannadaLabel() = runBlocking {
        val day =
            ExamPlanDayEntity(
                id = 2L,
                studentId = "student-1",
                dayIndex = -1,
                calendarEpochDay = 0L,
                dayType = "CHAPTER_TRIAL",
                status = "TODAY",
                label = "ಜೀವ ಪ್ರಕ್ರಿಯೆಗಳು",
                conceptIds = chapter.chapterId,
                estimatedMinutes = 30,
            )
        val label =
            TrialTitleResolver.localizedPlanDayLabel(
                day,
                "en",
                FakeConceptDao(concept),
                FakeChapterDao(chapter),
            )
        assertEquals("Life Processes", label)
    }

    @Test
    fun case1_trialItemTitleUsesEnglishDespiteStoredKannadaTitle() = runBlocking {
        val entity =
            trialItem(
                title = "ಜೀವ ಪ್ರಕ್ರಿಯೆಗಳು · ಅಧ್ಯಯನ · ಪ್ರಕಾಶ ಸಂಶ್ಲೇಷಣೆ",
            )
        val title =
            TrialTitleResolver.localizedItemTitle(
                entity = entity,
                languageCode = "en",
                conceptDao = FakeConceptDao(concept),
                chapterDao = FakeChapterDao(chapter),
            )
        assertEquals("Life Processes · Study · Photosynthesis", title)
    }

    @Test
    fun case2_trialItemTitleUsesKannadaDespiteStoredEnglishTitle() = runBlocking {
        val entity =
            trialItem(
                title = "Life Processes · Study · Photosynthesis",
            )
        val title =
            TrialTitleResolver.localizedItemTitle(
                entity = entity,
                languageCode = "kn",
                conceptDao = FakeConceptDao(concept),
                chapterDao = FakeChapterDao(chapter),
            )
        assertEquals("ಜೀವ ಪ್ರಕ್ರಿಯೆಗಳು · ಅಧ್ಯಯನ · ಪ್ರಕಾಶ ಸಂಶ್ಲೇಷಣೆ", title)
    }

    @Test
    fun multiConceptPlanDay_localizesPlusMoreSuffix() = runBlocking {
        val second =
            sampleConcept(
                conceptId = "concept-2",
                chapterId = "chapter-1",
                englishName = "Respiration",
                kannadaName = "ಶ್ವಸನ",
            )
        val day =
            lessonPlanDay(
                label = "Stored label ignored",
                conceptId = "${concept.conceptId},${second.conceptId}",
            )
        val label =
            TrialTitleResolver.localizedPlanDayLabel(
                day,
                "en",
                FakeConceptDao(concept, second),
                FakeChapterDao(chapter),
            )
        assertEquals("Photosynthesis +1 more", label)
    }

    @Test
    fun bookmarksAndRevisionFollowActiveLanguage() {
        val progress =
            ProgressEntity(
                studentId = "s1",
                itemType = "CONCEPT",
                itemId = concept.conceptId,
                status = "COMPLETED",
                progressPercentage = 100,
                language = "kn",
                appName = "eduai",
            )

        val (stateKn, _) =
            GamifiedHomeMapper.map(
                greeting = "",
                userName = "Student",
                streak = 0,
                todayConceptCount = 0,
                todaySimulationCount = 0,
                selectedSubjectName = "",
                progressConcepts = listOf(progress to concept),
                progressSimulations = emptyList(),
                languageCode = "kn",
            )
        assertTrue(stateKn.bookmarks.any { it.key == "ಪ್ರಕಾಶ ಸಂಶ್ಲೇಷಣೆ" })
        assertTrue(stateKn.revision.any { it.topic == "ಪ್ರಕಾಶ ಸಂಶ್ಲೇಷಣೆ" })

        val (stateEn, _) =
            GamifiedHomeMapper.map(
                greeting = "",
                userName = "Student",
                streak = 0,
                todayConceptCount = 0,
                todaySimulationCount = 0,
                selectedSubjectName = "",
                progressConcepts = listOf(progress to concept),
                progressSimulations = emptyList(),
                languageCode = "en",
            )
        assertTrue(stateEn.bookmarks.any { it.key == "Photosynthesis" })
        assertTrue(stateEn.revision.any { it.topic == "Photosynthesis" })
    }

    private fun lessonPlanDay(label: String, conceptId: String): ExamPlanDayEntity =
        ExamPlanDayEntity(
            id = 1L,
            studentId = "student-1",
            dayIndex = 1,
            calendarEpochDay = 0L,
            dayType = "LESSON",
            status = "TODAY",
            label = label,
            conceptIds = conceptId,
            estimatedMinutes = 30,
        )

    private fun trialItem(title: String): PlanTrialItemEntity =
        PlanTrialItemEntity(
            id = 1L,
            studentId = "student-1",
            planDayId = 1L,
            dayIndex = 1,
            chapterId = chapter.chapterId,
            conceptId = concept.conceptId,
            kind = PlanTrialItemKind.STUDY,
            sourceId = concept.conceptId,
            title = title,
            sequenceIndex = 0,
            status = PlanTrialItemStatus.PENDING,
            requiredCount = 7,
        )

    private fun sampleConcept(
        conceptId: String,
        chapterId: String,
        englishName: String,
        kannadaName: String,
    ): ConceptEntity =
        ConceptEntity(
            conceptId = conceptId,
            chapterId = chapterId,
            conceptName = englishName,
            conceptNameKannada = kannadaName,
            orderIndex = 1,
            type = "STUDY",
            problemId = "problem-1",
            problemTopicName = "Topic",
            problemTopicNameKn = "ವಿಷಯ",
            simulationId = "",
            simulationIdKannada = "",
        )

    private fun sampleChapter(
        chapterId: String,
        englishName: String,
        kannadaName: String,
    ): ChapterEntity =
        ChapterEntity(
            chapterId = chapterId,
            subjectId = "subject-1",
            chapterName = englishName,
            chapterNameKannada = kannadaName,
            revisionId = "rev-1",
            orderIndex = 1,
        )

    private class FakeConceptDao(vararg concepts: ConceptEntity) : ConceptDao {
        private val byId = concepts.associateBy { it.conceptId }

        override suspend fun getConcept(conceptId: String): ConceptEntity? = byId[conceptId]

        override suspend fun insertConcepts(concepts: List<ConceptEntity>) = unsupported()

        override suspend fun insertConcept(concept: ConceptEntity) = unsupported()

        override suspend fun updateConcept(concept: ConceptEntity) = unsupported()

        override suspend fun getAllConceptsSync(): List<ConceptEntity> = unsupported()

        override fun getConceptsForChapter(chapterId: String): Flow<List<ConceptEntity>> = flowOf(emptyList())

        override suspend fun getConceptsForChapterSync(chapterId: String, type: String): List<ConceptEntity> =
            unsupported()

        override fun getConceptFlow(conceptId: String): Flow<ConceptEntity?> = flowOf(byId[conceptId])

        override suspend fun getConceptByProblemId(problemId: String): ConceptEntity? = null

        override suspend fun getNextConcepts(chapterId: String, currentIndex: Int, limit: Int): List<ConceptEntity> =
            unsupported()

        override suspend fun deleteConceptsForChapter(chapterId: String) = unsupported()

        override fun getConceptsByIds(conceptIds: List<String>): Flow<List<ConceptEntity>> = flowOf(emptyList())

        override suspend fun deleteConcept(conceptId: String) = unsupported()

        override suspend fun deleteAllConcepts() = unsupported()

        override suspend fun getFirstConceptsOfChapter(orderIndex: String, type: String, limit: Int): List<ConceptEntity> =
            unsupported()

        override suspend fun getStudyConceptsForChapter(chapterId: String): List<ConceptEntity> = emptyList()

        override suspend fun getMathProblemConceptsForChapter(chapterId: String): List<ConceptEntity> = emptyList()

        override suspend fun getSimulationConceptsForChapter(chapterId: String, language: String): List<ConceptEntity> =
            emptyList()

        override suspend fun getStudyConceptCount(chapterId: String): Int = 0

        override suspend fun getMathProblemConceptCount(chapterId: String): Int = 0

        override suspend fun getSimulationConceptCount(chapterId: String, language: String): Int = 0

        override suspend fun getConceptCount(): Int = byId.size

        override suspend fun getChapter(chapterId: String): ChapterEntity? = null

        override suspend fun getChapterForConcept(conceptId: String): ChapterEntity? = null

        private fun unsupported(): Nothing = error("Not used in test")
    }

    private class FakeChapterDao(private val chapter: ChapterEntity) : ChapterDao {
        override suspend fun getChapter(chapterId: String): ChapterEntity? =
            if (chapterId == chapter.chapterId) chapter else null

        override suspend fun insertChapters(chapters: List<ChapterEntity>) = unsupported()

        override suspend fun insertChapter(chapter: ChapterEntity) = unsupported()

        override suspend fun updateChapter(chapter: ChapterEntity) = unsupported()

        override suspend fun getAllChaptersSync(): List<ChapterEntity> = unsupported()

        override fun getChaptersForSubject(subjectId: String): Flow<List<ChapterEntity>> = flowOf(emptyList())

        override suspend fun getChaptersForSubjectSync(subjectId: String): List<ChapterEntity> = emptyList()

        override fun getChapterFlow(chapterId: String): Flow<ChapterEntity?> = flowOf(chapter)

        override suspend fun getChapterById(chapterId: String): ChapterEntity? = getChapter(chapterId)

        override suspend fun getChapterForConcept(conceptId: String): ChapterEntity? = chapter

        override suspend fun deleteChaptersForSubject(subjectId: String) = unsupported()

        override suspend fun deleteChapter(chapterId: String) = unsupported()

        override suspend fun getChapterByName(chapterName: String): ChapterEntity? = null

        override suspend fun getConceptsByChapterId(chapterId: String): List<ConceptEntity> = emptyList()

        override suspend fun getConceptById(conceptId: String): ConceptEntity? = null

        private fun unsupported(): Nothing = error("Not used in test")
    }
}
