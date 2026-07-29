package com.wim.markdown.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.wim.markdown.MarkdownEditorMode
import com.wim.markdown.model.Block
import com.wim.markdown.model.InlineStyle
import com.wim.markdown.model.RichText
import com.wim.markdown.model.StyleRange
import com.wim.markdown.model.contentOrNull
import com.wim.markdown.model.normalizeSpans
import com.wim.markdown.model.withCell
import com.wim.markdown.model.withContent
import com.wim.markdown.serializer.HtmlParser
import com.wim.markdown.serializer.HtmlSerializer
import com.wim.markdown.serializer.InlineMarkdown
import com.wim.markdown.serializer.MarkdownParser
import com.wim.markdown.serializer.MarkdownSerializer
import com.wim.markdown.serializer.ParsedTaskItem
import com.wim.markdown.serializer.parseTaskItem

/** 工具栏可切换的块类型 */
sealed interface BlockType {
    data object Paragraph : BlockType
    data class Heading(val level: Int) : BlockType
    data class ListItem(val ordered: Boolean) : BlockType
    data object TaskListItem : BlockType
    data object Quote : BlockType
}

private val INLINE_HEADING = Regex("^(#{1,6}) ")
private val INLINE_UNORDERED = Regex("^- ")
private val INLINE_ORDERED = Regex("^\\d+\\. ")
private val INLINE_QUOTE = Regex("^> ")
private val INLINE_DIVIDER = Regex("^-{3,}$")
private const val MAX_INDENT = 3

private fun Block.parsePendingTask(source: String): ParsedTaskItem? =
    (this as? Block.ListItem)
        ?.takeIf { it.checked == null }
        ?.let { parseTaskItem(source) }

/** 编辑器唯一状态入口。所有编辑操作都经过这里，UI 层无业务逻辑。 */
@Stable
class MarkdownEditorState(initialBlocks: List<Block> = listOf(Block.Paragraph())) {

    val blocks: SnapshotStateList<Block> = mutableStateListOf<Block>().apply {
        addAll(initialBlocks.ifEmpty { listOf(Block.Paragraph()) })
    }

    var focusedIndex by mutableIntStateOf(-1)
        private set
    var selection by mutableStateOf(TextRange.Zero)

    /**
     * 下次输入的样式覆盖集：null = 无覆盖（自然延续光标处样式）；
     * 非 null = 权威期望集合（光标处继承样式 XOR 用户点按），光标移动后重置。
     */
    var pendingStyles by mutableStateOf<Set<InlineStyle>?>(null)
        private set

    /** INLINE 模式下焦点块的 markdown 源码；RICH 模式或无焦点时为 null */
    var inlineSource by mutableStateOf<String?>(null)
        private set

    var mode: MarkdownEditorMode = MarkdownEditorMode.RICH_TOOLBAR
        private set

    companion object {
        fun fromMarkdown(markdown: String): MarkdownEditorState =
            MarkdownEditorState(MarkdownParser.parse(markdown))

        fun fromHtml(html: String): MarkdownEditorState =
            MarkdownEditorState(HtmlParser.parse(html))
    }

    fun toMarkdown(): String {
        commitInlineEdit(keepFocus = true)
        return MarkdownSerializer.serialize(blocks.toList())
    }

    fun toHtml(): String {
        commitInlineEdit(keepFocus = true)
        return HtmlSerializer.serialize(blocks.toList())
    }

    fun setMode(mode: MarkdownEditorMode) {
        if (mode == this.mode) return
        commitInlineEdit()
        this.mode = mode
        if (mode == MarkdownEditorMode.INLINE_MARKDOWN && focusedIndex in blocks.indices) {
            reopenInline(plainCursor = selection.min)
        }
    }

    // ---- 焦点管理 ----

    fun focusBlock(index: Int, cursor: Int? = null) {
        if (index !in blocks.indices || blocks[index].contentOrNull() == null) return
        if (index == focusedIndex) return
        commitInlineEdit()
        focusedIndex = index
        pendingStyles = null
        val content = blocks[index].contentOrNull() ?: return
        if (mode == MarkdownEditorMode.INLINE_MARKDOWN) {
            reopenInline(plainCursor = cursor ?: content.text.length)
        } else {
            selection = TextRange((cursor ?: content.text.length).coerceIn(0, content.text.length))
        }
    }

    fun clearFocus() {
        commitInlineEdit()
        focusedIndex = -1
        pendingStyles = null
    }

    /** INLINE 模式：把焦点块源码解析写回模型 */
    private fun commitInlineEdit(keepFocus: Boolean = false) {
        val src = inlineSource ?: return
        val current = blocks.getOrNull(focusedIndex)
        if (current?.contentOrNull() != null) {
            val task = current.parsePendingTask(src)
            val contentSource = task?.content ?: src
            val content = InlineMarkdown.parse(contentSource).plain
            blocks[focusedIndex] = if (task == null) {
                current.withContent(content)
            } else {
                (current as Block.ListItem).copy(content = content, checked = task.checked)
            }
            if (keepFocus && task != null) {
                inlineSource = contentSource
                val removedLength = src.length - contentSource.length
                selection = TextRange(
                    (selection.start - removedLength).coerceIn(0, contentSource.length),
                    (selection.end - removedLength).coerceIn(0, contentSource.length),
                )
            }
        }
        if (!keepFocus) inlineSource = null
    }

    /** 以纯文本坐标 plainCursor 为基准重开 INLINE 源码编辑，光标映射到源码坐标 */
    private fun reopenInline(plainCursor: Int) {
        val content = blocks.getOrNull(focusedIndex)?.contentOrNull() ?: return
        val src = InlineMarkdown.serialize(content)
        inlineSource = src
        val clamped = plainCursor.coerceIn(0, content.text.length)
        val srcCursor = InlineMarkdown.serialize(sliceRich(content, 0, clamped)).length
        selection = TextRange(srcCursor.coerceIn(0, src.length))
    }

    // ---- RICH 模式输入 ----

    fun onRichValueChange(value: TextFieldValue) {
        val current = blocks.getOrNull(focusedIndex)?.contentOrNull() ?: return
        val newlineIndex = value.text.indexOf('\n')
        if (newlineIndex >= 0 || value.text != current.text) {
            val updated = RichText(
                value.text,
                SpanShifter.shift(current.text, current.spans, value.text, pendingStyles),
            )
            pendingStyles = null
            if (newlineIndex >= 0) {
                splitFocusedBlock(updated, newlineIndex)
                return
            }
            blocks[focusedIndex] = blocks[focusedIndex].withContent(updated)
        } else if (value.selection != selection) {
            // 仅移动光标：重置样式覆盖
            pendingStyles = null
        }
        selection = value.selection
    }

    // ---- INLINE 模式输入 ----

    fun onInlineValueChange(value: TextFieldValue) {
        if (inlineSource == null) return
        val nl = value.text.indexOf('\n')
        if (nl >= 0) {
            handleInlineEnter(value.text.substring(0, nl), value.text.substring(nl + 1))
            return
        }
        // 段落行首输入块前缀 -> 实时转换块类型
        val current = blocks.getOrNull(focusedIndex)
        if (current is Block.Paragraph && convertByPrefix(value)) return
        if (
            current is Block.ListItem &&
            current.checked == null &&
            convertTaskPrefix(current, value)
        ) {
            return
        }
        inlineSource = value.text
        selection = value.selection
    }

    /** 返回 true 表示发生了前缀转换 */
    private fun convertByPrefix(value: TextFieldValue): Boolean {
        val text = value.text
        val (block, prefixLen) = when {
            INLINE_HEADING.containsMatchIn(text) -> {
                val level = INLINE_HEADING.find(text)!!.groupValues[1].length
                Block.Heading(level, RichText()) to level + 1
            }
            INLINE_UNORDERED.containsMatchIn(text) ->
                Block.ListItem(ordered = false, indent = 0, content = RichText()) to 2
            INLINE_ORDERED.containsMatchIn(text) -> {
                val len = INLINE_ORDERED.find(text)!!.value.length
                Block.ListItem(ordered = true, indent = 0, content = RichText()) to len
            }
            INLINE_QUOTE.containsMatchIn(text) -> Block.Quote(RichText()) to 2
            else -> return false
        }
        val rest = text.substring(prefixLen)
        if (block is Block.ListItem && convertTaskPrefix(block, value, prefixLen)) return true
        blocks[focusedIndex] = block.withContent(InlineMarkdown.parse(rest).plain)
        inlineSource = rest
        selection = TextRange((value.selection.min - prefixLen).coerceIn(0, rest.length))
        return true
    }

    /** 普通列表项继续输入 [ ]/[x] 时，转换为任务列表并移除源码中的任务标记。 */
    private fun convertTaskPrefix(
        current: Block.ListItem,
        value: TextFieldValue,
        listPrefixLength: Int = 0,
    ): Boolean {
        val source = value.text.substring(listPrefixLength)
        val task = parseTaskItem(source) ?: return false
        // 输入完 "[ ]" 后继续等待分隔空格，避免下一次输入产生多余的正文空格。
        if (source.length == 3) return false
        val rest = task.content
        val removedLength = value.text.length - rest.length
        blocks[focusedIndex] = current.copy(
            content = InlineMarkdown.parse(rest).plain,
            checked = task.checked,
        )
        inlineSource = rest
        selection = TextRange((value.selection.min - removedLength).coerceIn(0, rest.length))
        return true
    }

    private fun handleInlineEnter(left: String, right: String) {
        val index = focusedIndex
        var current = blocks[index]
        // "---" 回车 -> 分割线
        if (current is Block.Paragraph && INLINE_DIVIDER.matches(left.trim())) {
            blocks[index] = Block.Divider
            blocks.add(index + 1, Block.Paragraph(InlineMarkdown.parse(right).plain))
            focusedIndex = index + 1
            inlineSource = right
            selection = TextRange.Zero
            return
        }
        val task = current.parsePendingTask(left)
        if (task != null) {
            current = (current as Block.ListItem).copy(checked = task.checked)
        }
        val leftRich = InlineMarkdown.parse(task?.content ?: left).plain
        val rightRich = InlineMarkdown.parse(right).plain
        // 空列表项回车 -> 退出列表
        if (current is Block.ListItem && leftRich.text.isEmpty() && rightRich.text.isEmpty()) {
            blocks[index] = Block.Paragraph(RichText())
            inlineSource = ""
            selection = TextRange.Zero
            return
        }
        blocks[index] = current.withContent(leftRich)
        blocks.add(index + 1, continuationBlock(current, rightRich))
        focusedIndex = index + 1
        inlineSource = right
        selection = TextRange.Zero
    }

    // ---- 结构操作（两种模式共用）----

    private fun splitFocusedBlock(merged: RichText, nl: Int) {
        val index = focusedIndex
        val current = blocks[index]
        val left = sliceRich(merged, 0, nl)
        val right = sliceRich(merged, nl + 1, merged.text.length)
        if (current is Block.ListItem && left.text.isEmpty() && right.text.isEmpty()) {
            blocks[index] = Block.Paragraph(RichText())
            selection = TextRange.Zero
            return
        }
        blocks[index] = current.withContent(left)
        blocks.add(index + 1, continuationBlock(current, right))
        focusedIndex = index + 1
        selection = TextRange.Zero
    }

    private fun continuationBlock(current: Block, content: RichText): Block = when (current) {
        is Block.ListItem -> current.copy(
            content = content,
            checked = if (current.checked == null) null else false,
        )
        is Block.Quote -> Block.Quote(content)
        else -> Block.Paragraph(content)
    }

    /** 块首退格。返回 true 表示已处理。 */
    fun onBackspaceAtStart(): Boolean {
        val index = focusedIndex
        if (blocks.getOrNull(index) == null) return false
        val inInline = inlineSource != null
        if (inInline) commitInlineEdit(keepFocus = true)

        val handled = backspaceAtStartRich(index)
        if (inInline) {
            inlineSource = null
            reopenInline(plainCursor = if (handled) selection.min else 0)
        }
        return handled
    }

    private fun backspaceAtStartRich(index: Int): Boolean {
        when (val current = blocks[index]) {
            is Block.Heading -> {
                blocks[index] = Block.Paragraph(current.content)
                return true
            }
            is Block.Quote -> {
                blocks[index] = Block.Paragraph(current.content)
                return true
            }
            is Block.ListItem -> {
                blocks[index] =
                    if (current.indent > 0) current.copy(indent = current.indent - 1)
                    else Block.Paragraph(current.content)
                return true
            }
            else -> Unit
        }
        if (index == 0) return false
        if (blocks[index - 1] is Block.Divider) {
            blocks.removeAt(index - 1)
            focusedIndex = index - 1
            return true
        }
        val prevContent = blocks[index - 1].contentOrNull() ?: return false
        val curContent = blocks[index].contentOrNull() ?: return false
        val joinAt = prevContent.text.length
        val mergedSpans = prevContent.spans +
            curContent.spans.map { StyleRange(it.start + joinAt, it.end + joinAt, it.style) }
        blocks[index - 1] = blocks[index - 1].withContent(
            RichText(
                prevContent.text + curContent.text,
                normalizeSpans(mergedSpans, joinAt + curContent.text.length),
            ),
        )
        blocks.removeAt(index)
        focusedIndex = index - 1
        selection = TextRange(joinAt)
        return true
    }

    // ---- 工具栏操作 ----

    fun toggleInlineStyle(style: InlineStyle) {
        val src = inlineSource
        if (src != null) {
            if (selection.collapsed) return
            val s = selection.min
            val e = selection.max
            val open = InlineMarkdown.openToken(style)
            val close = InlineMarkdown.closeToken(style)
            inlineSource = src.substring(0, s) + open + src.substring(s, e) + close + src.substring(e)
            selection = TextRange(s + open.length, e + open.length)
            return
        }
        val content = blocks.getOrNull(focusedIndex)?.contentOrNull() ?: return
        if (selection.collapsed) {
            // 基准 = 已有覆盖集，否则为光标处继承的样式；点按即 XOR
            val base = pendingStyles ?: inheritedStylesAt(content, selection.start)
            pendingStyles = if (style in base) base - style else base + style
            return
        }
        val s = selection.min
        val e = selection.max
        val covered = content.spans.any { it.style == style && it.start <= s && it.end >= e }
        val newSpans = if (covered) {
            content.spans.flatMap { r ->
                when {
                    r.style != style || r.end <= s || r.start >= e -> listOf(r)
                    else -> listOfNotNull(
                        if (r.start < s) r.copy(end = s) else null,
                        if (r.end > e) r.copy(start = e) else null,
                    )
                }
            }
        } else {
            content.spans + StyleRange(s, e, style)
        }
        blocks[focusedIndex] = blocks[focusedIndex]
            .withContent(content.copy(spans = normalizeSpans(newSpans, content.text.length)))
    }

    fun setBlockType(type: BlockType) {
        val index = focusedIndex
        val current = blocks.getOrNull(index) ?: return
        val content = inlineSource?.let { InlineMarkdown.parse(it).plain }
            ?: current.contentOrNull() ?: return
        val currentListItem = current as? Block.ListItem
        blocks[index] = when (type) {
            BlockType.Paragraph -> Block.Paragraph(content)
            is BlockType.Heading -> Block.Heading(type.level, content)
            is BlockType.ListItem -> Block.ListItem(
                ordered = type.ordered,
                indent = currentListItem?.indent ?: 0,
                content = content,
            )
            BlockType.TaskListItem -> Block.ListItem(
                ordered = false,
                indent = currentListItem?.indent ?: 0,
                content = content,
                checked = currentListItem?.checked ?: false,
            )
            BlockType.Quote -> Block.Quote(content)
        }
    }

    fun changeIndent(delta: Int) {
        val current = blocks.getOrNull(focusedIndex) as? Block.ListItem ?: return
        blocks[focusedIndex] = current.copy(indent = (current.indent + delta).coerceIn(0, MAX_INDENT))
    }

    /** 更新指定任务列表项的完成状态；普通列表项不会被修改。 */
    fun setTaskChecked(index: Int, checked: Boolean) {
        val current = blocks.getOrNull(index) as? Block.ListItem ?: return
        if (current.checked == null || current.checked == checked) return
        blocks[index] = current.copy(checked = checked)
    }

    fun insertDivider() {
        commitInlineEdit()
        val index = if (focusedIndex in blocks.indices) focusedIndex else blocks.lastIndex
        blocks.add(index + 1, Block.Divider)
        blocks.add(index + 2, Block.Paragraph(RichText()))
        focusedIndex = index + 2
        selection = TextRange.Zero
        if (mode == MarkdownEditorMode.INLINE_MARKDOWN) inlineSource = ""
    }

    // ---- 表格操作 ----

    /** 在焦点块后插入表格（首行为表头）并聚焦表格 */
    fun insertTable(rows: Int = 2, columns: Int = 2) {
        commitInlineEdit()
        val index = if (focusedIndex in blocks.indices) focusedIndex else blocks.lastIndex
        blocks.add(index + 1, Block.Table(List(rows) { List(columns) { RichText() } }))
        blocks.add(index + 2, Block.Paragraph(RichText()))
        focusedIndex = index + 1
        selection = TextRange.Zero
        pendingStyles = null
    }

    /** 表格块经由单元格获得焦点（表格无单一正文，不走 focusBlock） */
    fun focusTable(index: Int) {
        if (blocks.getOrNull(index) !is Block.Table) return
        if (index == focusedIndex) return
        commitInlineEdit()
        focusedIndex = index
        pendingStyles = null
    }

    fun onTableCellChange(blockIndex: Int, row: Int, col: Int, newText: String) {
        val table = blocks.getOrNull(blockIndex) as? Block.Table ?: return
        val cell = table.rows.getOrNull(row)?.getOrNull(col) ?: return
        if (cell.text == newText) return
        val spans = SpanShifter.shift(cell.text, cell.spans, newText)
        blocks[blockIndex] = table.withCell(row, col, RichText(newText, spans))
    }

    fun addTableRow() {
        val table = focusedTable() ?: return
        val columns = table.rows.first().size
        blocks[focusedIndex] = table.copy(rows = table.rows + listOf(List(columns) { RichText() }))
    }

    fun addTableColumn() {
        val table = focusedTable() ?: return
        blocks[focusedIndex] = table.copy(rows = table.rows.map { it + RichText() })
    }

    fun removeTableRow() {
        val table = focusedTable() ?: return
        if (table.rows.size <= 1) return
        blocks[focusedIndex] = table.copy(rows = table.rows.dropLast(1))
    }

    fun removeTableColumn() {
        val table = focusedTable() ?: return
        if (table.rows.first().size <= 1) return
        blocks[focusedIndex] = table.copy(rows = table.rows.map { it.dropLast(1) })
    }

    fun deleteFocusedTable() {
        if (blocks.getOrNull(focusedIndex) !is Block.Table) return
        blocks.removeAt(focusedIndex)
        if (blocks.isEmpty()) blocks.add(Block.Paragraph(RichText()))
        focusedIndex = -1
    }

    fun toggleTableHeaderRow() {
        val table = focusedTable() ?: return
        blocks[focusedIndex] = table.copy(hasHeaderRow = !table.hasHeaderRow)
    }

    fun toggleTableHeaderColumn() {
        val table = focusedTable() ?: return
        blocks[focusedIndex] = table.copy(hasHeaderColumn = !table.hasHeaderColumn)
    }

    private fun focusedTable(): Block.Table? = blocks.getOrNull(focusedIndex) as? Block.Table

    /** 工具栏高亮：光标/选区处当前生效的行内样式 */
    fun activeStyles(): Set<InlineStyle> {
        if (inlineSource != null) return emptySet()
        val content = blocks.getOrNull(focusedIndex)?.contentOrNull() ?: return emptySet()
        if (selection.collapsed) {
            return pendingStyles ?: inheritedStylesAt(content, selection.start)
        }
        return InlineStyle.entries.filter { st ->
            content.spans.any { it.style == st && it.start <= selection.min && it.end >= selection.max }
        }.toSet()
    }

    /** 光标处继承的样式：紧贴区间尾部视为在区间内（与输入延续规则一致） */
    private fun inheritedStylesAt(content: RichText, pos: Int): Set<InlineStyle> =
        content.spans.filter { it.start < pos && pos <= it.end }.map { it.style }.toSet()
}

/** 截取 [from, to) 的富文本片段，spans 同步裁剪并平移到新坐标 */
internal fun sliceRich(rich: RichText, from: Int, to: Int): RichText {
    val text = rich.text.substring(from, to)
    val spans = rich.spans.mapNotNull { r ->
        val s = maxOf(r.start, from)
        val e = minOf(r.end, to)
        if (s < e) StyleRange(s - from, e - from, r.style) else null
    }
    return RichText(text, spans)
}
