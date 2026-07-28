package com.wim.markdown.serializer

import com.wim.markdown.model.Block
import com.wim.markdown.model.InlineStyle
import com.wim.markdown.model.RichText
import com.wim.markdown.model.StyleRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InlineMarkdownEscapingTest {

    @Test
    fun literalInlineTokensAndBackslashRoundTrip() {
        val rich = RichText("""**bold** _italic_ ~~strike~~ `code` <u>under</u> \ tail""")

        val source = InlineMarkdown.serialize(rich)
        val parsed = InlineMarkdown.parse(source)

        assertEquals(
            """\*\*bold\*\* \_italic\_ \~\~strike\~\~ \`code\` \<u>under\</u> \\ tail""",
            source,
        )
        assertEquals(rich, parsed.plain)
        assertTrue(parsed.markerRanges.isEmpty())
        assertTrue(parsed.sourceSpans.isEmpty())
    }

    @Test
    fun parserConsumesEscapesForEveryInlineToken() {
        val source =
            """\*\*bold\*\* \_italic\_ \~\~strike\~\~ \`code\` \<u>under\</u> \\"""

        val parsed = InlineMarkdown.parse(source)

        assertEquals(
            RichText("""**bold** _italic_ ~~strike~~ `code` <u>under</u> \"""),
            parsed.plain,
        )
        assertTrue(parsed.markerRanges.isEmpty())
        assertTrue(parsed.sourceSpans.isEmpty())
    }

    @Test
    fun evenBackslashesDoNotEscapeFollowingMarker() {
        val parsed = InlineMarkdown.parse("""\\**bold**""")

        assertEquals(
            RichText(
                text = """\bold""",
                spans = listOf(StyleRange(1, 5, InlineStyle.Bold)),
            ),
            parsed.plain,
        )
    }

    @Test
    fun styledContentMayContainItsOwnDelimiter() {
        val cases = listOf(
            InlineStyle.Bold to "a ** b",
            InlineStyle.Italic to "a _ b",
            InlineStyle.Strikethrough to "a ~~ b",
            InlineStyle.Code to "a ` b",
            InlineStyle.Underline to "a <u> b </u>",
        )

        for ((style, text) in cases) {
            val rich = RichText(text, listOf(StyleRange(0, text.length, style)))

            val parsed = InlineMarkdown.parse(InlineMarkdown.serialize(rich))

            assertEquals("Failed to round-trip $style", rich, parsed.plain)
        }
    }

    @Test
    fun escapedDelimiterInsideRealStyleRemainsStyledText() {
        val parsed = InlineMarkdown.parse("""**a \*\* b**""")

        assertEquals(
            RichText(
                text = "a ** b",
                spans = listOf(StyleRange(0, 6, InlineStyle.Bold)),
            ),
            parsed.plain,
        )
    }

    @Test
    fun markdownParserPreservesEscapedInlineMarkersAsText() {
        val block = MarkdownParser.parseLine(
            """literal \*\* \_ \~\~ \` \<u> \</u> \\""",
        ) as Block.Paragraph

        assertEquals(
            RichText("""literal ** _ ~~ ` <u> </u> \"""),
            block.content,
        )
    }
}
