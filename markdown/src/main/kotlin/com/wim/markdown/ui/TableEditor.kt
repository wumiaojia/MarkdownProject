package com.wim.markdown.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wim.markdown.model.Block
import com.wim.markdown.model.RichText
import com.wim.markdown.state.MarkdownEditorState

@Composable
internal fun TableBlockEditor(
    index: Int,
    table: Block.Table,
    state: MarkdownEditorState,
    modifier: Modifier = Modifier,
    showTableActions: Boolean = true,
) {
    val style = LocalMarkdownTableStyle.current
    val borderColor = style.borderColor
    val scrollState = rememberScrollState()

    // 只要开启了滚动，且设置了最小列宽或者本身就是自适应模式，就允许横滑
    val canScroll = style.enableHorizontalScroll && 
            (style.layoutMode == TableLayoutMode.ADAPTIVE || style.minColumnWidth != null)

    Column(modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val containerWidth = maxWidth
            Box(
                Modifier
                    .fillMaxWidth()
                    .then(if (canScroll) Modifier.horizontalScroll(scrollState) else Modifier)
            ) {
                TableGrid(
                    table = table,
                    style = style,
                    state = state,
                    blockIndex = index,
                    containerWidth = containerWidth,
                    modifier = Modifier
                        .clip(RoundedCornerShape(style.cornerRadius))
                        .background(style.backgroundColor)
                        .border(
                            style.borderWidth,
                            borderColor,
                            RoundedCornerShape(style.cornerRadius)
                        )
                )
            }
        }
        
        if (canScroll && style.showHorizontalScrollbar && scrollState.maxValue > 0) {
            TableScrollbar(scrollState)
        }

        if (showTableActions && (state.focusedIndex == index)) {
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                TextButton(onClick = { state.addTableRow() }) { Text("加行") }
                TextButton(onClick = { state.addTableColumn() }) { Text("加列") }
                TextButton(onClick = { state.removeTableRow() }) { Text("删行") }
                TextButton(onClick = { state.removeTableColumn() }) { Text("删列") }
                TextButton(onClick = { state.toggleTableHeaderRow() }) {
                    Text(if (table.hasHeaderRow) "关行表头" else "开行表头")
                }
                TextButton(onClick = { state.toggleTableHeaderColumn() }) {
                    Text(if (table.hasHeaderColumn) "关列表头" else "开列表头")
                }
                TextButton(onClick = { state.deleteFocusedTable() }) { Text("删表") }
            }
        }
    }
}

/**
 * 真正的网格布局，确保列宽一致。
 */
@Composable
private fun TableGrid(
    table: Block.Table,
    style: MarkdownTableStyle,
    state: MarkdownEditorState,
    blockIndex: Int,
    containerWidth: Dp,
    modifier: Modifier = Modifier
) {
    val rowCount = table.rows.size
    val columnCount = if (rowCount > 0) table.rows[0].size else 0
    if (columnCount == 0) return

    Layout(
        modifier = modifier,
        content = {
            table.rows.forEachIndexed { r, row ->
                row.forEachIndexed { c, cell ->
                    val isHeader = (r == 0 && table.hasHeaderRow) || (c == 0 && table.hasHeaderColumn)
                    TableCell(
                        state = state,
                        blockIndex = blockIndex,
                        row = r,
                        col = c,
                        cell = cell,
                        header = isHeader,
                        isLastRow = r == table.rows.lastIndex,
                        isLastColumn = c == row.lastIndex,
                    )
                }
            }
        }
    ) { measurables, constraints ->
        val minColWidthPx = style.minColumnWidth?.roundToPx() ?: 0
        val maxColWidthPx = style.maxColumnWidth?.roundToPx() ?: Int.MAX_VALUE
        val containerWidthPx = containerWidth.roundToPx()

        // 1. 测量每列的理想宽度
        val colMaxWidths = IntArray(columnCount) { 0 }
        measurables.chunked(columnCount).forEach { rowMeasurables ->
            rowMeasurables.forEachIndexed { c, cellMeasurable ->
                // 使用 maxColWidthPx 进行内在宽度测量，触发换行
                val intrinsicWidth = cellMeasurable.maxIntrinsicWidth(constraints.maxHeight)
                colMaxWidths[c] = maxOf(colMaxWidths[c], intrinsicWidth)
            }
        }

        // 2. 计算最终列宽
        val finalColWidths = if (style.layoutMode == TableLayoutMode.STRETCH) {
            val averageWidth = containerWidthPx / columnCount
            IntArray(columnCount) { 
                maxOf(averageWidth, minColWidthPx).coerceAtMost(maxColWidthPx)
            }
        } else {
            IntArray(columnCount) { c ->
                colMaxWidths[c].coerceIn(minColWidthPx, maxColWidthPx)
            }
        }

        // 3. 第一轮：确定每行的高度 (使用 maxIntrinsicHeight 避免重复调用 measure)
        val rowHeights = IntArray(rowCount) { r ->
            var maxHeight = 0
            for (c in 0 until columnCount) {
                val measurable = measurables[r * columnCount + c]
                val h = measurable.maxIntrinsicHeight(finalColWidths[c])
                maxHeight = maxOf(maxHeight, h)
            }
            maxHeight
        }

        // 4. 第二轮测量：强制拉伸所有单元格至行高（关键修复：对齐行高）
        val finalPlaceables = measurables.mapIndexed { index, measurable ->
            val r = index / columnCount
            val c = index % columnCount
            measurable.measure(
                Constraints.fixed(finalColWidths[c], rowHeights[r])
            )
        }

        val totalWidth = finalColWidths.sum()
        val totalHeight = rowHeights.sum()

        layout(totalWidth, totalHeight) {
            var y = 0
            for (r in 0 until rowCount) {
                var x = 0
                for (c in 0 until columnCount) {
                    finalPlaceables[r * columnCount + c].placeRelative(x, y)
                    x += finalColWidths[c]
                }
                y += rowHeights[r]
            }
        }
    }
}

@Composable
private fun TableCell(
    state: MarkdownEditorState,
    blockIndex: Int,
    row: Int,
    col: Int,
    cell: RichText,
    header: Boolean,
    isLastRow: Boolean,
    isLastColumn: Boolean,
    modifier: Modifier = Modifier,
) {
    val style = LocalMarkdownTableStyle.current
    val typography = LocalMarkdownTypography.current
    val codeBg = MaterialTheme.colorScheme.surfaceVariant
    var value by remember { mutableStateOf(TextFieldValue(cell.toAnnotatedString(codeBg))) }
    // 行列增删等外部变更时同步本地值（自身编辑时文本一致，不会触发）
    LaunchedEffect(cell.text) {
        if (value.text != cell.text) {
            value = TextFieldValue(cell.toAnnotatedString(codeBg), TextRange(cell.text.length))
        }
    }
    val textColor = if (header) style.headerContentColor else style.contentColor
    val cellBg = if (header) style.headerBackgroundColor else Color.Transparent
    val base = MaterialTheme.typography.bodyMedium.copy(
        color = if (textColor != Color.Unspecified) textColor else MaterialTheme.colorScheme.onSurface,
        fontSize = if (header) typography.tableHeaderSize else typography.tableBodySize,
        textAlign = style.defaultAlignment
    )
    BasicTextField(
        value = value,
        onValueChange = { v ->
            val text = v.text.replace("\n", "")
            state.onTableCellChange(blockIndex, row, col, text)
            val updated = (state.blocks.getOrNull(blockIndex) as? Block.Table)
                ?.rows?.getOrNull(row)?.getOrNull(col)
            val selection = TextRange(
                v.selection.min.coerceIn(0, text.length),
                v.selection.max.coerceIn(0, text.length),
            )
            value = TextFieldValue(
                updated?.toAnnotatedString(codeBg) ?: androidx.compose.ui.text.AnnotatedString(text),
                selection,
            )
        },
        textStyle = if (header) base.copy(fontWeight = FontWeight.Bold) else base,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier
            .fillMaxHeight()
            .onFocusChanged { if (it.isFocused) state.focusTable(blockIndex) }
            .background(cellBg)
            .drawBehind {
                val strokeWidth = style.borderWidth.toPx()
                val color = style.borderColor
                // 绘制右边框
                if (!isLastColumn) {
                    drawLine(
                        color = color,
                        start = Offset(size.width, 0f),
                        end = Offset(size.width, size.height),
                        strokeWidth = strokeWidth
                    )
                }
                // 绘制底边框
                if (!isLastRow) {
                    drawLine(
                        color = color,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = strokeWidth
                    )
                }
            }
            .padding(horizontal = style.cellHorizontalPadding, vertical = style.cellVerticalPadding),
    )
}

@Composable
private fun TableScrollbar(scrollState: androidx.compose.foundation.ScrollState) {
    val alpha by animateFloatAsState(
        targetValue = if (scrollState.isScrollInProgress) 1f else 0.4f,
        label = "scrollbarAlpha"
    )
    
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .height(3.dp)
            .background(Color.LightGray.copy(alpha = 0.2f * alpha), RoundedCornerShape(1.5.dp))
    ) {
        val viewportWidthPx = constraints.maxWidth.toFloat()
        val maxValuePx = scrollState.maxValue.toFloat()
        val contentWidthPx = maxValuePx + viewportWidthPx
        
        if (contentWidthPx > 0) {
            val thumbWidthRatio = (viewportWidthPx / contentWidthPx).coerceIn(0.1f, 1f)
            val thumbOffsetRatio = scrollState.value.toFloat() / contentWidthPx
            
            val thumbWidth = maxWidth * thumbWidthRatio
            val thumbOffset = maxWidth * thumbOffsetRatio
            
            Box(
                Modifier
                    .width(thumbWidth) // Using exact width instead of widthIn
                    .fillMaxHeight()
                    .offset(x = thumbOffset)
                    .background(Color.Gray.copy(alpha = 0.5f * alpha), RoundedCornerShape(1.5.dp))
            )
        }
    }
}
