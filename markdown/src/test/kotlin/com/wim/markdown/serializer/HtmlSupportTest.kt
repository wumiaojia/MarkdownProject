package com.wim.markdown.serializer

import com.wim.markdown.state.MarkdownEditorState
import org.junit.Assert.assertEquals
import org.junit.Test

class HtmlSupportTest {

    @Test
    fun testHtmlRoundTrip() {
        val initialHtml = "<h1>Title</h1>\n<p>Hello <strong>World</strong></p>"
        val state = MarkdownEditorState.fromHtml(initialHtml)
        
        assertEquals(2, state.blocks.size)
        
        val exportedHtml = state.toHtml()
        assertEquals("<h1>Title</h1>\n<p>Hello <strong>World</strong></p>", exportedHtml)
    }

    @Test
    fun testListHtml() {
        val initialHtml = "<ul>\n  <li>Item 1</li>\n  <li>Item 2</li>\n</ul>"
        val state = MarkdownEditorState.fromHtml(initialHtml)
        
        assertEquals(2, state.blocks.size)
        
        val exportedHtml = state.toHtml()
        assertEquals("<ul>\n  <li>Item 1</li>\n  <li>Item 2</li>\n</ul>", exportedHtml)
    }

    @Test
    fun testMarkdownToHtml() {
        val markdown = "# Title\n\nHello **World**"
        val state = MarkdownEditorState.fromMarkdown(markdown)
        
        val html = state.toHtml()
        assertEquals("<h1>Title</h1>\n<p>Hello <strong>World</strong></p>", html)
    }

    @Test
    fun testHtmlToMarkdown() {
        val html = "<h1>Title</h1><p>Hello <strong>World</strong></p>"
        val state = MarkdownEditorState.fromHtml(html)
        
        val markdown = state.toMarkdown()
        assertEquals("# Title\n\nHello **World**", markdown)
    }

    @Test
    fun testTableHtml() {
        val html = "<table>\n  <tr>\n    <th>H1</th>\n    <th>H2</th>\n  </tr>\n  <tr>\n    <td>C1</td>\n    <td>C2</td>\n  </tr>\n</table>"
        val state = MarkdownEditorState.fromHtml(html)
        
        val exportedHtml = state.toHtml()
        assertEquals(html, exportedHtml)
    }
}
