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
}
