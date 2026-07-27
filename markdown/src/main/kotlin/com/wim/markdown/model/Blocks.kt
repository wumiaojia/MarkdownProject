package com.wim.markdown.model

/** 行内样式类型 */
enum class InlineStyle { Bold, Italic, Strikethrough, Underline, Code }

/** 行内样式区间，[start, end) 为纯文本坐标 */
data class StyleRange(val start: Int, val end: Int, val style: InlineStyle)

/** 带行内样式的文本 */
data class RichText(val text: String = "", val spans: List<StyleRange> = emptyList())

/** 文档块。文档 = List<Block> */
sealed interface Block {
    data class Paragraph(val content: RichText = RichText()) : Block
    data class Heading(val level: Int, val content: RichText = RichText()) : Block
    data class ListItem(
        val ordered: Boolean,
        val indent: Int = 0,
        val content: RichText = RichText(),
    ) : Block
    data class Quote(val content: RichText = RichText()) : Block
    data object Divider : Block

    /** 表格 */
    data class Table(
        val rows: List<List<RichText>>,
        val hasHeaderRow: Boolean = true,
        val hasHeaderColumn: Boolean = false,
    ) : Block
}

/** 替换表格单个单元格 */
fun Block.Table.withCell(row: Int, col: Int, cell: RichText): Block.Table =
    copy(
        rows = rows.mapIndexed { r, cells ->
            if (r == row) cells.mapIndexed { c, old -> if (c == col) cell else old } else cells
        },
    )

/** 单一正文块的内容；Divider/Table 返回 null */
fun Block.contentOrNull(): RichText? = when (this) {
    is Block.Paragraph -> content
    is Block.Heading -> content
    is Block.ListItem -> content
    is Block.Quote -> content
    Block.Divider -> null
    is Block.Table -> null
}

/** 替换块正文，保持块类型；Divider/Table 原样返回 */
fun Block.withContent(content: RichText): Block = when (this) {
    is Block.Paragraph -> copy(content = content)
    is Block.Heading -> copy(content = content)
    is Block.ListItem -> copy(content = content)
    is Block.Quote -> copy(content = content)
    Block.Divider -> this
    is Block.Table -> this
}

/**
 * 规范化样式区间：钳制越界、丢弃空区间、合并相邻/重叠的同类型区间、稳定排序。
 * 模型中的 spans 应始终保持规范化。
 */
fun normalizeSpans(spans: List<StyleRange>, textLength: Int): List<StyleRange> {
    val cleaned = spans
        .map { StyleRange(it.start.coerceIn(0, textLength), it.end.coerceIn(0, textLength), it.style) }
        .filter { it.start < it.end }
    return cleaned
        .groupBy { it.style }
        .flatMap { (_, ranges) ->
            val sorted = ranges.sortedBy { it.start }
            val merged = mutableListOf<StyleRange>()
            for (r in sorted) {
                val last = merged.lastOrNull()
                if (last != null && r.start <= last.end) {
                    merged[merged.size - 1] = last.copy(end = maxOf(last.end, r.end))
                } else {
                    merged.add(r)
                }
            }
            merged
        }
        .sortedWith(compareBy({ it.start }, { it.style }))
}
