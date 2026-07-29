package com.wim.markdown.serializer

import com.wim.markdown.model.Block
import com.wim.markdown.model.RichText

/** 计算有序列表项显示序号：向上数同缩进的连续有序项 */
fun orderedNumber(blocks: List<Block>, index: Int): Int {
    val item = blocks.getOrNull(index) as? Block.ListItem ?: return 0
    var n = 1
    var i = index - 1
    while (i >= 0) {
        val b = blocks[i] as? Block.ListItem ?: break
        if (b.indent < item.indent) break
        if (b.indent == item.indent) {
            if (b.ordered) n++ else break
        }
        i--
    }
    return n
}

/** 块模型 -> markdown 文本 */
object MarkdownSerializer {

    fun serialize(blocks: List<Block>): String {
        val sb = StringBuilder()
        blocks.forEachIndexed { i, block ->
            if (i > 0) {
                val bothList = blocks[i - 1] is Block.ListItem && block is Block.ListItem
                sb.append(if (bothList) "\n" else "\n\n")
            }
            sb.append(
                when (block) {
                    is Block.Paragraph -> InlineMarkdown.serialize(block.content)
                    is Block.Heading ->
                        "#".repeat(block.level) + " " + InlineMarkdown.serialize(block.content)
                    is Block.Quote -> "> " + InlineMarkdown.serialize(block.content)
                    Block.Divider -> "---"
                    is Block.ListItem -> {
                        val prefix =
                            if (block.ordered) "${orderedNumber(blocks, i)}. " else "- "
                        val taskMarker = block.checked?.let { if (it) "[x] " else "[ ] " }.orEmpty()
                        "  ".repeat(block.indent) + prefix + taskMarker +
                            InlineMarkdown.serialize(block.content)
                    }
                    is Block.Table -> serializeTable(block)
                },
            )
        }
        return sb.toString()
    }

    private fun serializeTable(table: Block.Table): String {
        fun rowLine(row: List<RichText>): String =
            "| " + row.joinToString(" | ") {
                InlineMarkdown.serialize(it).replace("|", "\\|")
            } + " |"

        if (table.rows.isEmpty()) return ""
        val header = table.rows.first()
        val sb = StringBuilder(rowLine(header))
        // Standard MD table requires a separator line. 
        // We only output it if hasHeaderRow is true, otherwise it's just plain rows.
        if (table.hasHeaderRow) {
            sb.append("\n").append("| " + header.joinToString(" | ") { "---" } + " |")
        }
        table.rows.drop(1).forEach { sb.append("\n").append(rowLine(it)) }
        return sb.toString()
    }
}
