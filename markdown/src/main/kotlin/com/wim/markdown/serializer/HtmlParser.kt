package com.wim.markdown.serializer

import com.wim.markdown.model.Block
import com.wim.markdown.model.InlineStyle
import com.wim.markdown.model.RichText
import com.wim.markdown.model.StyleRange
import com.wim.markdown.model.normalizeSpans

/** HTML 文本 -> 块模型 */
object HtmlParser {

    fun parse(html: String): List<Block> {
        if (html.isBlank()) return listOf(Block.Paragraph())
        
        val blocks = mutableListOf<Block>()
        // Updated regex to include ul and ol
        val blockRegex = Regex("<(p|h[1-6]|blockquote|ul|ol|hr|table)[^>]*>(.*?)</\\1>|<hr\\s*/>", RegexOption.DOT_MATCHES_ALL)
        
        var lastEnd = 0
        blockRegex.findAll(html).forEach { match ->
            val between = html.substring(lastEnd, match.range.first).trim()
            if (between.isNotEmpty()) {
                blocks.add(Block.Paragraph(parseInline(between)))
            }
            
            val tag = (match.groupValues[1]).lowercase()
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
                    val liRegex = Regex("<li[^>]*>(.*?)</li>", RegexOption.DOT_MATCHES_ALL)
                    liRegex.findAll(content).forEach { liMatch ->
                        blocks.add(Block.ListItem(ordered = ordered, indent = 0, content = parseInline(liMatch.groupValues[1])))
                    }
                }
                tag == "hr" || match.value.startsWith("<hr") -> blocks.add(Block.Divider)
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
        
        val trRegex = Regex("<tr[^>]*>(.*?)</tr>", RegexOption.DOT_MATCHES_ALL)
        val tdRegex = Regex("<(td|th)[^>]*>(.*?)</\\1>", RegexOption.DOT_MATCHES_ALL)
        
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
            if (row.size < maxCols) row + List(maxCols - row.size) { RichText() } else row
        }
        
        return Block.Table(
            rows = normalizedRows,
            hasHeaderRow = rowIsHeader.firstOrNull() ?: true,
            hasHeaderColumn = firstColIsHeader.all { it } && firstColIsHeader.isNotEmpty()
        )
    }

    fun parseInline(html: String): RichText {
        val cleanHtml = html.trim()
        if (cleanHtml.isEmpty()) return RichText()
        
        val tags = listOf(
            TagInfo("<strong>", "</strong>", InlineStyle.Bold),
            TagInfo("<b>", "</b>", InlineStyle.Bold),
            TagInfo("<em>", "</em>", InlineStyle.Italic),
            TagInfo("<i>", "</i>", InlineStyle.Italic),
            TagInfo("<s>", "</s>", InlineStyle.Strikethrough),
            TagInfo("<strike>", "</strike>", InlineStyle.Strikethrough),
            TagInfo("<del>", "</del>", InlineStyle.Strikethrough),
            TagInfo("<u>", "</u>", InlineStyle.Underline),
            TagInfo("<code>", "</code>", InlineStyle.Code)
        )
        
        val toks = mutableListOf<HtmlTok>()
        var i = 0
        while (i < cleanHtml.length) {
            var found = false
            for (tag in tags) {
                if (cleanHtml.startsWith(tag.open, i)) {
                    toks.add(HtmlTok(i, tag.open.length, tag.style, isClose = false))
                    i += tag.open.length
                    found = true
                    break
                } else if (cleanHtml.startsWith(tag.close, i)) {
                    toks.add(HtmlTok(i, tag.close.length, tag.style, isClose = true))
                    i += tag.close.length
                    found = true
                    break
                }
            }
            if (!found) i++
        }
        
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
        i = 0
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

    private data class TagInfo(val open: String, val close: String, val style: InlineStyle)
    private data class HtmlTok(val pos: Int, val len: Int, val style: InlineStyle, val isClose: Boolean)
}
