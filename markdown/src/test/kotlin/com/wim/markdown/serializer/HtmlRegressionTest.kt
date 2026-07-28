package com.wim.markdown.serializer

import com.wim.markdown.model.Block
import com.wim.markdown.model.InlineStyle
import com.wim.markdown.model.RichText
import com.wim.markdown.model.StyleRange
import org.junit.Assert.assertEquals
import org.junit.Test

class HtmlRegressionTest {

    @Test
    fun serializeInline_usesCanonicalTagForEveryStyle() {
        val tags = mapOf(
            InlineStyle.Bold to "strong",
            InlineStyle.Italic to "em",
            InlineStyle.Strikethrough to "s",
            InlineStyle.Underline to "u",
            InlineStyle.Code to "code",
        )

        for ((style, tag) in tags) {
            val richText = RichText(
                text = "x",
                spans = listOf(StyleRange(0, 1, style)),
            )

            val html = HtmlSerializer.serializeInline(richText)

            assertEquals("<$tag>x</$tag>", html)
            assertEquals(richText, HtmlParser.parseInline(html))
        }
    }

    @Test
    fun serializeInline_reopensCrossingStylesToPreserveTheirRanges() {
        val richText = RichText(
            text = "abc",
            spans = listOf(
                StyleRange(0, 2, InlineStyle.Bold),
                StyleRange(1, 3, InlineStyle.Italic),
            ),
        )

        val html = HtmlSerializer.serializeInline(richText)

        assertEquals("<strong>a<em>b</em></strong><em>c</em>", html)
        assertEquals(richText, HtmlParser.parseInline(html))
    }

    @Test
    fun parseInline_acceptsMixedCaseTagsAndOpeningTagAttributes() {
        val richText = HtmlParser.parseInline(
            """<STRONG class="lead" data-id="7">Bold <Em style="color: red">both</eM></sTrOnG>""",
        )

        assertEquals("Bold both", richText.text)
        assertEquals(
            listOf(
                StyleRange(0, 9, InlineStyle.Bold),
                StyleRange(5, 9, InlineStyle.Italic),
            ),
            richText.spans,
        )
    }

    @Test
    fun parseInline_acceptsEverySupportedTagAlias() {
        val aliases = mapOf(
            InlineStyle.Bold to listOf("strong", "b"),
            InlineStyle.Italic to listOf("em", "i"),
            InlineStyle.Strikethrough to listOf("s", "strike", "del"),
            InlineStyle.Underline to listOf("u"),
            InlineStyle.Code to listOf("code"),
        )

        for ((style, tags) in aliases) {
            for (tag in tags) {
                assertEquals(
                    RichText("x", listOf(StyleRange(0, 1, style))),
                    HtmlParser.parseInline("<$tag>x</$tag>"),
                )
            }
        }
    }

    @Test
    fun parse_acceptsMixedCaseBlockAndTableTagsWithAttributes() {
        val blocks = HtmlParser.parse(
            """
            <P CLASS="lead">Hello <B title="greeting">World</b></p>
            <TABLE class="data">
              <TR><TH scope="col">Name</th></tr>
              <Tr><Td>Ada</tD></tR>
            </table>
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                Block.Paragraph(
                    RichText(
                        text = "Hello World",
                        spans = listOf(StyleRange(6, 11, InlineStyle.Bold)),
                    ),
                ),
                Block.Table(
                    rows = listOf(
                        listOf(RichText("Name")),
                        listOf(RichText("Ada")),
                    ),
                    hasHeaderRow = true,
                    hasHeaderColumn = false,
                ),
            ),
            blocks,
        )
    }

    @Test
    fun parse_normalizesRaggedTableAndDetectsHeaderColumn() {
        val blocks = HtmlParser.parse(
            """
            <table>
              <tr><th>A</th><td>B</td></tr>
              <tr><th>C</th></tr>
            </table>
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                Block.Table(
                    rows = listOf(
                        listOf(RichText("A"), RichText("B")),
                        listOf(RichText("C"), RichText()),
                    ),
                    hasHeaderRow = false,
                    hasHeaderColumn = true,
                ),
            ),
            blocks,
        )
    }
}
