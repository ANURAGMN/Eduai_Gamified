package com.ncert7.aitutorandlab.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationDeepLinkMapperTest {
    @Test
    fun fromRoute_trialIncludesDayIndex() {
        val link =
            NotificationDeepLinkMapper.fromRoute(
                route = "trial",
                params = mapOf("dayIndex" to "2"),
            )
        assertTrue(link is NotificationDeepLink.Screen)
        assertEquals("plan_trial/2", link.route)
    }

    @Test
    fun fromRoute_planIsTab() {
        val link = NotificationDeepLinkMapper.fromRoute("plan")
        assertTrue(link is NotificationDeepLink.Tab)
        assertEquals("plan", link.route)
    }

    @Test
    fun parseParams_splitsCommaPairs() {
        assertEquals(
            mapOf("dayIndex" to "1", "claim" to "true"),
            NotificationDeepLinkMapper.parseParams("dayIndex=1,claim=true"),
        )
    }

    @Test
    fun fromRoute_progressAndAvatarStudioAreScreens() {
        val progress = NotificationDeepLinkMapper.fromRoute("progress")
        assertTrue(progress is NotificationDeepLink.Screen)
        assertEquals("progress", progress.route)

        val avatar = NotificationDeepLinkMapper.fromRoute("avatar_studio")
        assertTrue(avatar is NotificationDeepLink.Screen)
        assertEquals("avatar_studio", avatar.route)
    }

    @Test
    fun fromRoute_homeAndQuestsAreTabs() {
        val home = NotificationDeepLinkMapper.fromRoute("home")
        assertTrue(home is NotificationDeepLink.Tab)
        assertEquals("home", home.route)

        val quests = NotificationDeepLinkMapper.fromRoute("quest")
        assertTrue(quests is NotificationDeepLink.Tab)
        assertEquals("quests", quests.route)
    }

    @Test
    fun fromRoute_chapterOpensConceptStudyScreen() {
        val link =
            NotificationDeepLinkMapper.fromRoute(
                route = "chapter",
                params = mapOf("chapterId" to "photosynthesis"),
            )
        assertTrue(link is NotificationDeepLink.Screen)
        assertEquals("concepts/photosynthesis/study", link.route)
    }

    @Test
    fun deepLinkStore_roundTripsPendingLink() {
        NotificationDeepLinkStore.setFromIntent(route = "plan", paramsRaw = null)
        val link = NotificationDeepLinkStore.consume()
        assertTrue(link is NotificationDeepLink.Tab)
        assertEquals("plan", link?.route)

        assertNull(NotificationDeepLinkStore.consume())
    }
}
