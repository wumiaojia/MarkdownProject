package com.wim.markdown.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class TableEditorTest {

    @Test
    fun `column width bounds preserve an ordered range`() {
        assertEquals(64..400, normalizeColumnWidthBounds(64, 400))
    }

    @Test
    fun `column width bounds reorder inverted values`() {
        assertEquals(100..200, normalizeColumnWidthBounds(200, 100))
    }

    @Test
    fun `column width bounds clamp negative values`() {
        assertEquals(0..64, normalizeColumnWidthBounds(-20, 64))
        assertEquals(0..0, normalizeColumnWidthBounds(-20, -10))
    }
}
