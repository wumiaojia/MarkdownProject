package com.wim.markdown.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Markdown 编辑器排版字号配置。
 */
@Immutable
data class MarkdownTypography(
    val headingSizes: List<TextUnit> = listOf(28.sp, 24.sp, 20.sp, 18.sp, 16.sp, 15.sp),
    val paragraphSize: TextUnit = 16.sp,
    val listItemSize: TextUnit = 16.sp,
    val tableHeaderSize: TextUnit = 14.sp,
    val tableBodySize: TextUnit = 14.sp,
    val quoteSize: TextUnit = 16.sp,
)

/**
 * 表格布局模式。
 */
enum class TableLayoutMode {
    /** 撑满全宽，所有列平均分配空间（Modifier.weight(1f)） */
    STRETCH,

    /** 根据内容自适应宽度，内容多则宽，内容少则窄 */
    ADAPTIVE
}

/**
 * Markdown 编辑器表格视觉样式配置。
 */
@Immutable
data class MarkdownTableStyle(
    val backgroundColor: Color = Color.Transparent,
    val headerBackgroundColor: Color = Color.Transparent,
    val contentColor: Color = Color.Unspecified,
    val headerContentColor: Color = Color.Unspecified,
    val borderColor: Color = Color.LightGray,
    val borderWidth: Dp = 1.dp,
    val cornerRadius: Dp = 4.dp,
    val cellHorizontalPadding: Dp = 8.dp,
    val cellVerticalPadding: Dp = 8.dp,
    val defaultAlignment: TextAlign = TextAlign.Start,
    val minColumnWidth: Dp? = 64.dp,
    val maxColumnWidth: Dp? = 400.dp,
    val enableHorizontalScroll: Boolean = true,
    val layoutMode: TableLayoutMode = TableLayoutMode.ADAPTIVE,
    val showHorizontalScrollbar: Boolean = true,
)

val LocalMarkdownTypography = staticCompositionLocalOf { MarkdownTypography() }
val LocalMarkdownTableStyle = staticCompositionLocalOf { MarkdownTableStyle() }
