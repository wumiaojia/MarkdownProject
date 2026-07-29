package com.wim.markdown.serializer

import com.wim.markdown.model.Block
import com.wim.markdown.model.InlineStyle
import com.wim.markdown.model.RichText
import com.wim.markdown.model.normalizeSpans

/** 块模型 -> HTML 文本 */
object HtmlSerializer {

    fun serialize(blocks: List<Block>): String {
        val sb = StringBuilder()
        var i = 0
        while (i < blocks.size) {
            val block = blocks[i]
            if (block is Block.ListItem) {
                val ordered = block.ordered
                val tag = if (ordered) "ol" else "ul"
                sb.append("<$tag>\n")
                while (i < blocks.size) {
                    val item = blocks[i] as? Block.ListItem ?: break
                    if (item.ordered != ordered) break
                    sb.append("  <li>")
                    item.checked?.let { checked ->
                        val checkedAttribute = if (checked) " checked" else ""
                        sb.append("<input type=\"checkbox\" disabled$checkedAttribute /> ")
                    }
                    sb.append(serializeInline(item.content)).append("</li>\n")
                    i++
                }
                sb.append("</$tag>\n")
            } else {
                sb.append(serializeBlock(block)).append("\n")
                i++
            }
        }
        return sb.toString().trim()
    }

    private fun serializeBlock(block: Block): String = when (block) {
        is Block.Paragraph -> "<p>${serializeInline(block.content)}</p>"
        is Block.Heading -> "<h${block.level}>${serializeInline(block.content)}</h${block.level}>"
        is Block.Quote -> "<blockquote>${serializeInline(block.content)}</blockquote>"
        Block.Divider -> "<hr />"
        is Block.ListItem -> "" // Handled in serialize loop
        is Block.Table -> serializeTable(block)
    }

    private fun serializeTable(table: Block.Table): String {
        val sb = StringBuilder("<table>\n")
        table.rows.forEachIndexed { r, row ->
            sb.append("  <tr>\n")
            row.forEachIndexed { c, cell ->
                val isHeader = (r == 0 && table.hasHeaderRow) || (c == 0 && table.hasHeaderColumn)
                val tag = if (isHeader) "th" else "td"
                sb.append("    <$tag>${serializeInline(cell)}</$tag>\n")
            }
            sb.append("  </tr>\n")
        }
        sb.append("</table>")
        return sb.toString()
    }

    fun serializeInline(rich: RichText): String {
        val n = rich.text.length
        val spans = normalizeSpans(rich.spans, n)
        if (spans.isEmpty()) return escapeHtml(rich.text)

        val points = (spans.flatMap { listOf(it.start, it.end) } + listOf(0, n))
            .distinct().sorted()
        val sb = StringBuilder()
        val stack = mutableListOf<InlineStyle>()
        
        for (i in 0 until points.size - 1) {
            val a = points[i]
            val b = points[i + 1]
            if (a == b) continue
            val targetStack = spans
                .filter { it.start <= a && it.end >= b }
                .map { it.style }
                .distinct()
                .sortedBy { it.ordinal }

            var commonPrefixLength = 0
            while (
                commonPrefixLength < stack.size &&
                commonPrefixLength < targetStack.size &&
                stack[commonPrefixLength] == targetStack[commonPrefixLength]
            ) {
                commonPrefixLength++
            }

            while (stack.size > commonPrefixLength) {
                sb.append("</").append(tagName(stack.removeAt(stack.size - 1))).append(">")
            }

            for (styleIndex in commonPrefixLength until targetStack.size) {
                val style = targetStack[styleIndex]
                sb.append("<").append(tagName(style)).append(">")
                stack.add(style)
            }

            sb.append(escapeHtml(rich.text.substring(a, b)))
        }
        
        while (stack.isNotEmpty()) {
            sb.append("</").append(tagName(stack.removeAt(stack.size - 1))).append(">")
        }
        
        return sb.toString()
    }

    private fun tagName(style: InlineStyle): String = when (style) {
        InlineStyle.Bold -> "strong"
        InlineStyle.Italic -> "em"
        InlineStyle.Strikethrough -> "s"
        InlineStyle.Underline -> "u"
        InlineStyle.Code -> "code"
    }

    private fun escapeHtml(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}
