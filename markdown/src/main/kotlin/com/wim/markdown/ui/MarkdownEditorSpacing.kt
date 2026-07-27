package com.wim.markdown.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wim.markdown.model.Block

/**
 * Markdown 编辑器块间距配置。
 * 使用 [DefaultMarkdownEditorSpacing] 作为推荐的默认值。
 */
@Immutable
data class MarkdownEditorSpacing(
    val horizontalPadding: Dp = 16.dp,
    val paragraphSpacing: Dp = 6.dp,
    val headingTopSpacing: Dp = 16.dp,
    val headingBottomSpacing: Dp = 6.dp,
    val headingToHeadingSpacing: Dp = 8.dp,
    val listItemSpacing: Dp = 2.dp,
    val listGroupSpacing: Dp = 8.dp,
    val quoteSpacing: Dp = 8.dp,
    val dividerSpacing: Dp = 12.dp,
    val tableSpacing: Dp = 12.dp,
)

/**
 * 默认推荐的排版间距配置，适配移动端阅读与编辑体验。
 */
val DefaultMarkdownEditorSpacing = MarkdownEditorSpacing()

val LocalMarkdownEditorSpacing = staticCompositionLocalOf { MarkdownEditorSpacing() }

/**
 * 根据当前块和上一个块的上下文，计算当前块应该应用的顶部间距。
 */
internal fun MarkdownEditorSpacing.calculateTopSpacing(current: Block, previous: Block?): Dp {
    if (previous == null) return 0.dp // 第一个块由列表 ContentPadding 处理，不重复加 Padding

    return when (current) {
        is Block.ListItem -> {
            if (previous is Block.ListItem) listItemSpacing else listGroupSpacing
        }
        is Block.Heading -> {
            if (previous is Block.Heading) headingToHeadingSpacing else headingTopSpacing
        }
        is Block.Divider -> dividerSpacing
        is Block.Quote -> quoteSpacing
        is Block.Table -> tableSpacing
        is Block.Paragraph -> {
            // 如果上一个块是标题，段落顶部间距取标题的 bottomSpacing
            if (previous is Block.Heading) headingBottomSpacing else paragraphSpacing
        }
    }
}
