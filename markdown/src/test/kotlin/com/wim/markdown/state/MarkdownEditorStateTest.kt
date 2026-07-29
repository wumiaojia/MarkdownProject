package com.wim.markdown.state

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.wim.markdown.model.Block
import com.wim.markdown.model.InlineStyle
import com.wim.markdown.model.RichText
import com.wim.markdown.model.StyleRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MarkdownEditorStateTest {

    @Test
    fun richTextInputShiftsSpansAndConsumesPendingStyles() {
        val state = MarkdownEditorState(listOf(Block.Paragraph(RichText("ab"))))
        state.focusBlock(0, cursor = 2)
        state.toggleInlineStyle(InlineStyle.Bold)

        state.onRichValueChange(TextFieldValue("abc", TextRange(3)))

        assertEquals(
            Block.Paragraph(
                RichText("abc", listOf(StyleRange(2, 3, InlineStyle.Bold))),
            ),
            state.blocks.single(),
        )
        assertEquals(TextRange(3), state.selection)
        assertNull(state.pendingStyles)
    }

    @Test
    fun newlineInputSplitsBlockAndConsumesPendingStyles() {
        val state = MarkdownEditorState(listOf(Block.Quote(RichText("ab"))))
        state.focusBlock(0, cursor = 1)
        state.toggleInlineStyle(InlineStyle.Bold)

        state.onRichValueChange(TextFieldValue("a\nXb", TextRange(3)))

        assertEquals(
            listOf(
                Block.Quote(RichText("a")),
                Block.Quote(
                    RichText("Xb", listOf(StyleRange(0, 1, InlineStyle.Bold))),
                ),
            ),
            state.blocks.toList(),
        )
        assertEquals(1, state.focusedIndex)
        assertEquals(TextRange.Zero, state.selection)
        assertNull(state.pendingStyles)
    }

    @Test
    fun selectionChangeKeepsContentAndClearsPendingStyles() {
        val original = Block.Paragraph(RichText("ab"))
        val state = MarkdownEditorState(listOf(original))
        state.focusBlock(0, cursor = 1)
        state.toggleInlineStyle(InlineStyle.Bold)

        state.onRichValueChange(TextFieldValue("ab", TextRange(2)))

        assertEquals(original, state.blocks.single())
        assertEquals(TextRange(2), state.selection)
        assertNull(state.pendingStyles)
    }

    @Test
    fun setTaskCheckedOnlyUpdatesTaskItems() {
        val regular = Block.ListItem(ordered = false, content = RichText("regular"))
        val state = MarkdownEditorState(
            listOf(
                Block.ListItem(
                    ordered = false,
                    content = RichText("todo"),
                    checked = false,
                ),
                regular,
            ),
        )

        state.setTaskChecked(0, true)
        state.setTaskChecked(1, true)

        assertEquals(
            Block.ListItem(
                ordered = false,
                content = RichText("todo"),
                checked = true,
            ),
            state.blocks[0],
        )
        assertEquals(regular, state.blocks[1])
    }

    @Test
    fun newlineAfterTaskCreatesUncheckedContinuation() {
        val state = MarkdownEditorState(
            listOf(
                Block.ListItem(
                    ordered = false,
                    content = RichText("done"),
                    checked = true,
                ),
            ),
        )
        state.focusBlock(0, cursor = 4)

        state.onRichValueChange(TextFieldValue("done\nnext", TextRange(9)))

        assertEquals(
            listOf(
                Block.ListItem(
                    ordered = false,
                    content = RichText("done"),
                    checked = true,
                ),
                Block.ListItem(
                    ordered = false,
                    content = RichText("next"),
                    checked = false,
                ),
            ),
            state.blocks.toList(),
        )
    }

    @Test
    fun inlineTypingConvertsRegularListToTask() {
        val state = MarkdownEditorState()
        state.setMode(com.wim.markdown.MarkdownEditorMode.INLINE_MARKDOWN)
        state.focusBlock(0)

        state.onInlineValueChange(TextFieldValue("- ", TextRange(2)))
        state.onInlineValueChange(TextFieldValue("[ ]", TextRange(3)))
        assertNull((state.blocks.single() as Block.ListItem).checked)

        state.onInlineValueChange(TextFieldValue("[ ] todo", TextRange(8)))

        assertEquals(
            Block.ListItem(
                ordered = false,
                content = RichText("todo"),
                checked = false,
            ),
            state.blocks.single(),
        )
        assertEquals("todo", state.inlineSource)
        assertEquals(TextRange(4), state.selection)
    }

    @Test
    fun committingMarkerOnlyTaskPreservesTaskState() {
        val state = MarkdownEditorState()
        state.setMode(com.wim.markdown.MarkdownEditorMode.INLINE_MARKDOWN)
        state.focusBlock(0)
        state.onInlineValueChange(TextFieldValue("- ", TextRange(2)))
        state.onInlineValueChange(TextFieldValue("[ ]", TextRange(3)))

        state.clearFocus()

        assertEquals(
            Block.ListItem(
                ordered = false,
                content = RichText(),
                checked = false,
            ),
            state.blocks.single(),
        )
    }

    @Test
    fun exportingMarkerOnlyTaskKeepsInlineEditorConsistent() {
        val state = MarkdownEditorState()
        state.setMode(com.wim.markdown.MarkdownEditorMode.INLINE_MARKDOWN)
        state.focusBlock(0)
        state.onInlineValueChange(TextFieldValue("- ", TextRange(2)))
        state.onInlineValueChange(TextFieldValue("[x]", TextRange(3)))

        val markdown = state.toMarkdown()

        assertEquals("- [x] ", markdown)
        assertEquals("", state.inlineSource)
        assertEquals(TextRange.Zero, state.selection)
    }

    @Test
    fun selectingTaskListAgainKeepsCompletedState() {
        val state = MarkdownEditorState(
            listOf(
                Block.ListItem(
                    ordered = false,
                    content = RichText("done"),
                    checked = true,
                ),
            ),
        )
        state.focusBlock(0)

        state.setBlockType(BlockType.TaskListItem)

        assertEquals(true, (state.blocks.single() as Block.ListItem).checked)
    }
}
