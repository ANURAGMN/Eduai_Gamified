package com.ncert7.aitutorandlab.domain.reels

import org.junit.Assert.assertEquals
import org.junit.Test

class ViewCountFormatterTest {

    @Test fun `under a thousand is verbatim`() {
        assertEquals("0", ViewCountFormatter.format(0))
        assertEquals("540", ViewCountFormatter.format(540))
        assertEquals("999", ViewCountFormatter.format(999))
    }

    @Test fun `thousands use K with trimmed decimal`() {
        assertEquals("1K", ViewCountFormatter.format(1_000))
        assertEquals("1.2K", ViewCountFormatter.format(1_200))
        assertEquals("1.5K", ViewCountFormatter.format(1_500))
        assertEquals("12.3K", ViewCountFormatter.format(12_345))
    }

    @Test fun `boundary round up promotes to next unit`() {
        assertEquals("1M", ViewCountFormatter.format(999_999))
        assertEquals("1M", ViewCountFormatter.format(1_000_000))
    }

    @Test fun `millions and billions`() {
        assertEquals("3.4M", ViewCountFormatter.format(3_400_000))
        assertEquals("1.5B", ViewCountFormatter.format(1_500_000_000))
    }

    @Test fun `negative clamps to zero`() {
        assertEquals("0", ViewCountFormatter.format(-5))
    }
}
