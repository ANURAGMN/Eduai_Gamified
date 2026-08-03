package com.ncert7.aitutorandlab.notification

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationTokensTest {
    @Test
    fun fill_replacesAllKnownTokens() {
        val text =
            NotificationTokens(
                name = "Sam",
                bite = "Math",
                days = 3,
                streak = 7,
                gems = 20,
                league = "Gold",
                avatar = "Astronaut",
            ).fill("{name}|{bite}|{days}|{streak}|{gems}|{league}|{avatar}")

        assertEquals("Sam|Math|3|7|20|Gold|Astronaut", text)
    }
}
