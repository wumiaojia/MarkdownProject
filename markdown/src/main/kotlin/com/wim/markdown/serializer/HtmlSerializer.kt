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
                while (i < blocks.size && blocks[i] is Block.ListItem && (blocks[i] as Block.ListItem).ordered == ordered) {
                    sb.append("  <li>").append(serializeInline((blocks[i] as Block.ListItem).content)).append("</li>\n")
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
            val active = spans.filter { it.start <= a && it.end >= b }.map { it.style }.toSet()
            
            // Close tags that are not in active
            while (stack.isNotEmpty() && stack.last() !in active) {
                sb.append(closeTag(stack.removeAt(stack.size - 1)))
            }
            
            // Open tags that are in active but not in stack
            val toOpen = active.filter { it !in stack }.sortedBy { it.ordinal }
            toOpen.forEach {
                sb.append(openTag(it))
                stack.add(it)
            }
            
            sb.append(escapeHtml(rich.text.substring(a, b)))
        }
        
        while (stack.isNotEmpty()) {
            sb.append(closeTag(stack.removeAt(stack.size - 1)))
        }
        
        return sb.toString()
    }

    private fun openTag(style: InlineStyle): String = when (style) {
        InlineStyle.Bold -> "<strong>"
        InlineStyle.Italic -> "<em>"
        InlineStyle.Strikethrough -> "<s>"
        InlineStyle.Underline -> "<u>"
        InlineStyle.Code -> "<code>"
    }

    private fun closeTag(style: InlineStyle): String = when (style) {
        InlineStyle.Bold -> "</strong>"
        InlineStyle.Italic -> "</em>"
        InlineStyle.Strikethrough -> "</s>"
        InlineStyle.Underline -> "</u>"
        InlineStyle.Code -> "</code>"
    }

    private fun escapeHtml(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}
