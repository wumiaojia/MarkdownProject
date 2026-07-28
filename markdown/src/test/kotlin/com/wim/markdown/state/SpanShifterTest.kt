package com.wim.markdown.state

import com.wim.markdown.model.InlineStyle
import com.wim.markdown.model.StyleRange
import org.junit.Assert.assertEquals
import org.junit.Test

class SpanShifterTest {

    @Test
    fun insertionAtSpanStartDoesNotInheritStyle() {
        val spans = SpanShifter.shift(
            oldText = "abc",
            oldSpans = listOf(StyleRange(1, 2, InlineStyle.Bold)),
            newText = "aXbc",
        )

        assertEquals(listOf(StyleRange(2, 3, InlineStyle.Bold)), spans)
    }

    @Test
    fun insertionAtSpanEndContinuesStyle() {
        val spans = SpanShifter.shift(
            oldText = "abc",
            oldSpans = listOf(StyleRange(1, 2, InlineStyle.Bold)),
            newText = "abXc",
        )

        assertEquals(listOf(StyleRange(1, 3, InlineStyle.Bold)), spans)
    }

    @Test
    fun replacementDoesNotPullFollowingSpanOntoInsertedText() {
        val spans = SpanShifter.shift(
            oldText = "abcd",
            oldSpans = listOf(StyleRange(2, 4, InlineStyle.Italic)),
            newText = "aXd",
        )

        assertEquals(listOf(StyleRange(2, 3, InlineStyle.Italic)), spans)
    }
}
