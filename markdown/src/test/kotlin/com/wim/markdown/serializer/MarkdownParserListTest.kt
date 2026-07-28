package com.wim.markdown.serializer

import com.wim.markdown.model.Block
import com.wim.markdown.model.RichText
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownParserListTest {

    @Test
    fun parsesUnorderedListItem() {
        assertEquals(
            Block.ListItem(ordered = false, content = RichText("item")),
            MarkdownParser.parseLine("- item"),
        )
    }

    @Test
    fun parsesOrderedListItem() {
        assertEquals(
            Block.ListItem(ordered = true, content = RichText("item")),
            MarkdownParser.parseLine("1. item"),
        )
    }

    @Test
    fun parsesMultiDigitOrderedListMarker() {
        assertEquals(
            Block.ListItem(ordered = true, content = RichText("item")),
            MarkdownParser.parseLine("123. item"),
        )
    }

    @Test
    fun convertsEveryTwoLeadingSpacesToOneIndentLevel() {
        assertEquals(
            Block.ListItem(ordered = false, indent = 2, content = RichText("nested")),
            MarkdownParser.parseLine("    - nested"),
        )
    }

    @Test
    fun keepsListLikeTextWithoutMarkerSpaceAsParagraph() {
        listOf("-item", "1.item").forEach { line ->
            assertEquals(
                Block.Paragraph(RichText(line)),
                MarkdownParser.parseLine(line),
            )
        }
    }
}
