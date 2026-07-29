package com.wim.markdown.serializer

import com.wim.markdown.model.Block
import com.wim.markdown.model.InlineStyle
import com.wim.markdown.model.RichText
import com.wim.markdown.model.StyleRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaskListTest {

    @Test
    fun parsesUncheckedAndCheckedTaskItems() {
        assertEquals(
            Block.ListItem(
                ordered = false,
                content = RichText("todo"),
                checked = false,
            ),
            MarkdownParser.parseLine("- [ ] todo"),
        )
        assertEquals(
            Block.ListItem(
                ordered = false,
                content = RichText(
                    "done",
                    listOf(StyleRange(0, 4, InlineStyle.Bold)),
                ),
                checked = true,
            ),
            MarkdownParser.parseLine("- [X] **done**"),
        )
    }

    @Test
    fun preservesIndentAndOrderedTaskMarkers() {
        val blocks = listOf(
            Block.ListItem(
                ordered = false,
                indent = 1,
                content = RichText("nested"),
                checked = false,
            ),
            Block.ListItem(
                ordered = true,
                content = RichText("numbered"),
                checked = true,
            ),
        )

        val markdown = MarkdownSerializer.serialize(blocks)

        assertEquals("  - [ ] nested\n1. [x] numbered", markdown)
        assertEquals(blocks, MarkdownParser.parse(markdown))
    }

    @Test
    fun keepsMalformedTaskMarkerAsRegularListContent() {
        val item = MarkdownParser.parseLine("- [x]done") as Block.ListItem

        assertNull(item.checked)
        assertEquals(RichText("[x]done"), item.content)
    }

    @Test
    fun htmlRoundTripPreservesTaskState() {
        val blocks = listOf(
            Block.ListItem(ordered = false, content = RichText("todo"), checked = false),
            Block.ListItem(ordered = false, content = RichText("done"), checked = true),
        )

        val html = HtmlSerializer.serialize(blocks)

        assertEquals(
            """
            <ul>
              <li><input type="checkbox" disabled /> todo</li>
              <li><input type="checkbox" disabled checked /> done</li>
            </ul>
            """.trimIndent(),
            html,
        )
        assertEquals(blocks, HtmlParser.parse(html))
    }
}
