package com.wim.markdown.serializer

import com.wim.markdown.model.Block
import com.wim.markdown.model.InlineStyle
import com.wim.markdown.model.RichText
import com.wim.markdown.model.StyleRange
import com.wim.markdown.model.normalizeSpans

/** HTML 文本 -> 块模型 */
object HtmlParser {

    private val inputTag = Regex("^\\s*<input\\b[^>]*>\\s*", RegexOption.IGNORE_CASE)
    private val checkboxType = Regex(
        "\\btype\\s*=\\s*(?:\"checkbox\"|'checkbox'|checkbox)(?=\\s|/?>)",
        RegexOption.IGNORE_CASE,
    )
    private val checkedAttribute = Regex(
        "\\bchecked(?:\\s*=\\s*(?:\"checked\"|'checked'|checked))?(?=\\s|/?>)",
        RegexOption.IGNORE_CASE,
    )

    fun parse(html: String): List<Block> {
        if (html.isBlank()) return listOf(Block.Paragraph())

        val blocks = mutableListOf<Block>()
        val blockRegex = Regex(
            "<(p|h[1-6]|blockquote|ul|ol|table)\\b[^>]*>(.*?)</\\1\\s*>|<hr\\b[^>]*\\/?>",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
        )

        var lastEnd = 0
        blockRegex.findAll(html).forEach { match ->
            val between = html.substring(lastEnd, match.range.first).trim()
            if (between.isNotEmpty()) {
                blocks.add(Block.Paragraph(parseInline(between)))
            }
            
            val tag = match.groupValues[1].lowercase().ifEmpty { "hr" }
            val content = match.groupValues[2]
            
            when {
                tag == "p" -> blocks.add(Block.Paragraph(parseInline(content)))
                tag.startsWith("h") -> {
                    val level = tag.substring(1).toIntOrNull() ?: 1
                    blocks.add(Block.Heading(level, parseInline(content)))
                }
                tag == "blockquote" -> blocks.add(Block.Quote(parseInline(content)))
                tag == "ul" || tag == "ol" -> {
                    val ordered = tag == "ol"
                    val liRegex = Regex(
                        "<li\\b[^>]*>(.*?)</li\\s*>",
                        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
                    )
                    liRegex.findAll(content).forEach { liMatch ->
                        val itemHtml = liMatch.groupValues[1]
                        val input = inputTag.find(itemHtml)
                            ?.takeIf { checkboxType.containsMatchIn(it.value) }
                        blocks.add(
                            Block.ListItem(
                                ordered = ordered,
                                indent = 0,
                                content = parseInline(
                                    if (input == null) itemHtml else itemHtml.removeRange(input.range),
                                ),
                                checked = input?.let {
                                    checkedAttribute.containsMatchIn(it.value)
                                },
                            ),
                        )
                    }
                }
                tag == "hr" -> blocks.add(Block.Divider)
                tag == "table" -> blocks.add(parseTable(content))
            }
            lastEnd = match.range.last + 1
        }
        
        val trailing = html.substring(lastEnd).trim()
        if (trailing.isNotEmpty()) {
            blocks.add(Block.Paragraph(parseInline(trailing)))
        }
        
        return blocks.ifEmpty { listOf(Block.Paragraph()) }
    }

    private fun parseTable(tableContent: String): Block {
        val rows = mutableListOf<List<RichText>>()
        val rowIsHeader = mutableListOf<Boolean>()
        val firstColIsHeader = mutableListOf<Boolean>()
        
        val options = setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        val trRegex = Regex("<tr\\b[^>]*>(.*?)</tr\\s*>", options)
        val tdRegex = Regex("<(td|th)\\b[^>]*>(.*?)</\\1\\s*>", options)
        
        trRegex.findAll(tableContent).forEach { trMatch ->
            val rowContent = trMatch.groupValues[1]
            val cellMatches = tdRegex.findAll(rowContent).toList()
            val cells = cellMatches.map { parseInline(it.groupValues[2]) }
            
            if (cells.isNotEmpty()) {
                rows.add(cells)
                rowIsHeader.add(cellMatches.all { it.groupValues[1].lowercase() == "th" })
                firstColIsHeader.add(cellMatches.first().groupValues[1].lowercase() == "th")
            }
        }
        
        if (rows.isEmpty()) return Block.Paragraph(RichText("Empty Table"))
        val maxCols = rows.maxOf { it.size }
        val normalizedRows = rows.map { row ->
            row + List(maxCols - row.size) { RichText() }
        }
        
        return Block.Table(
            rows = normalizedRows,
            hasHeaderRow = rowIsHeader.first(),
            hasHeaderColumn = firstColIsHeader.all { it },
        )
    }

    fun parseInline(html: String): RichText {
        val cleanHtml = html.trim()
        if (cleanHtml.isEmpty()) return RichText()
        
        val tagRegex = Regex(
            "<\\s*(/?)\\s*(strong|b|em|i|s|strike|del|u|code)(?=\\s|/?>)[^>]*>",
            RegexOption.IGNORE_CASE,
        )
        val toks = tagRegex.findAll(cleanHtml).map { match ->
            val style = when (match.groupValues[2].lowercase()) {
                "strong", "b" -> InlineStyle.Bold
                "em", "i" -> InlineStyle.Italic
                "s", "strike", "del" -> InlineStyle.Strikethrough
                "u" -> InlineStyle.Underline
                "code" -> InlineStyle.Code
                else -> error("Unsupported inline HTML tag")
            }
            HtmlTok(
                pos = match.range.first,
                len = match.value.length,
                style = style,
                isClose = match.groupValues[1].isNotEmpty(),
            )
        }.toList()

        val paired = mutableListOf<Pair<HtmlTok, HtmlTok>>()
        val openStacks = mutableMapOf<InlineStyle, MutableList<HtmlTok>>()

        for (t in toks) {
            if (t.isClose) {
                val stack = openStacks[t.style]
                if (stack != null && stack.isNotEmpty()) {
                    paired.add(stack.removeAt(stack.size - 1) to t)
                }
            } else {
                openStacks.getOrPut(t.style) { mutableListOf() }.add(t)
            }
        }

        val markerToks = paired.flatMap { listOf(it.first, it.second) }.sortedBy { it.pos }
        val sb = StringBuilder()
        val plainPosOf = mutableMapOf<Int, Int>()
        var m = 0
        var i = 0
        while (i < cleanHtml.length) {
            if (m < markerToks.size && markerToks[m].pos == i) {
                plainPosOf[i] = sb.length
                i += markerToks[m].len
                m++
            } else {
                if (cleanHtml.startsWith("&lt;", i)) { sb.append("<"); i += 4 }
                else if (cleanHtml.startsWith("&gt;", i)) { sb.append(">"); i += 4 }
                else if (cleanHtml.startsWith("&amp;", i)) { sb.append("&"); i += 5 }
                else if (cleanHtml.startsWith("&quot;", i)) { sb.append("\""); i += 6 }
                else if (cleanHtml.startsWith("&#39;", i)) { sb.append("'"); i += 5 }
                else {
                    sb.append(cleanHtml[i])
                    i++
                }
            }
        }
        
        val plainSpans = paired.map { (o, c) ->
            StyleRange(plainPosOf.getValue(o.pos), plainPosOf.getValue(c.pos), o.style)
        }
        
        return RichText(sb.toString(), normalizeSpans(plainSpans, sb.length))
    }

    private data class HtmlTok(val pos: Int, val len: Int, val style: InlineStyle, val isClose: Boolean)
}
