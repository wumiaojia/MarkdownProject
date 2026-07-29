package com.wim.markdown.serializer

import com.wim.markdown.model.Block
import com.wim.markdown.model.RichText

private val HEADING = Regex("^(#{1,6}) (.*)$")
private val LIST_ITEM = Regex("^( *)(-|\\d+\\.) (.*)$")
private val TASK_ITEM = Regex("^\\[([ xX])](?:\\s+(.*))?$")
private val QUOTE = Regex("^> ?(.*)$")
private val DIVIDER = Regex("^(-{3,}|\\*{3,}|_{3,})\\s*$")
private val SEPARATOR_CELL = Regex("^:?-{3,}:?$")

internal data class ParsedTaskItem(val checked: Boolean, val content: String)

internal fun parseTaskItem(content: String): ParsedTaskItem? {
    val match = TASK_ITEM.matchEntire(content) ?: return null
    return ParsedTaskItem(
        checked = match.groupValues[1].equals("x", ignoreCase = true),
        content = match.groupValues[2],
    )
}

/** markdown 文本 -> 块模型。按行解析，连续竖线行聚合为表格，未识别语法容错为段落。 */
object MarkdownParser {

    fun parse(markdown: String): List<Block> {
        val lines = markdown.lines()
        val blocks = mutableListOf<Block>()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            when {
                line.isBlank() -> i++
                line.trimStart().startsWith("|") -> {
                    val tableLines = mutableListOf<String>()
                    while (i < lines.size && lines[i].trimStart().startsWith("|")) {
                        tableLines.add(lines[i])
                        i++
                    }
                    blocks.add(parseTable(tableLines))
                }
                line.trimStart().lowercase().startsWith("<table") -> {
                    val htmlTable = StringBuilder()
                    while (i < lines.size) {
                        htmlTable.append(lines[i]).append("\n")
                        if (lines[i].lowercase().contains("</table>")) {
                            i++
                            break
                        }
                        i++
                    }
                    blocks.addAll(HtmlParser.parse(htmlTable.toString()))
                }
                else -> {
                    blocks.add(parseLine(line))
                    i++
                }
            }
        }
        return blocks.ifEmpty { listOf(Block.Paragraph(RichText())) }
    }

    fun parseLine(line: String): Block {
        HEADING.matchEntire(line)?.let {
            return Block.Heading(it.groupValues[1].length, inline(it.groupValues[2]))
        }
        DIVIDER.matchEntire(line.trim())?.let { return Block.Divider }
        LIST_ITEM.matchEntire(line)?.let {
            val (indent, marker, content) = it.destructured
            val task = parseTaskItem(content)
            return Block.ListItem(
                ordered = marker != "-",
                indent = indent.length / 2,
                content = inline(task?.content ?: content),
                checked = task?.checked,
            )
        }
        QUOTE.matchEntire(line)?.let { return Block.Quote(inline(it.groupValues[1])) }
        return Block.Paragraph(inline(line))
    }

    /** 分隔行（| --- | --- |）被丢弃；短行按最大列数补空单元格 */
    private fun parseTable(lines: List<String>): Block {
        val rows = lines.mapNotNull { line ->
            val cells = splitCells(line)
            if (cells.all { SEPARATOR_CELL.matches(it.trim()) }) null
            else cells.map { inline(it.trim().replace("\\|", "|")) }
        }
        if (rows.isEmpty()) return Block.Paragraph(inline(lines.joinToString(" ")))
        val cols = rows.maxOf { it.size }
        return Block.Table(rows.map { it + List(cols - it.size) { RichText() } })
    }

    /** 去掉首尾竖线后按未转义的 | 分割 */
    private fun splitCells(line: String): List<String> {
        val t = line.trim().removePrefix("|").removeSuffix("|")
        val cells = mutableListOf<String>()
        val sb = StringBuilder()
        var i = 0
        while (i < t.length) {
            val ch = t[i]
            when {
                ch == '\\' && i + 1 < t.length && t[i + 1] == '|' -> {
                    sb.append("\\|")
                    i += 2
                }
                ch == '|' -> {
                    cells.add(sb.toString())
                    sb.clear()
                    i++
                }
                else -> {
                    sb.append(ch)
                    i++
                }
            }
        }
        cells.add(sb.toString())
        return cells
    }

    private fun inline(s: String): RichText = InlineMarkdown.parse(s).plain
}
