package com.wim.markdown

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import com.wim.markdown.state.MarkdownEditorState
import com.wim.markdown.ui.BlockEditor
import com.wim.markdown.ui.DefaultMarkdownEditorSpacing
import com.wim.markdown.ui.EditorToolbar
import com.wim.markdown.ui.LocalMarkdownEditorSpacing
import com.wim.markdown.ui.LocalMarkdownTableStyle
import com.wim.markdown.ui.LocalMarkdownTypography
import com.wim.markdown.ui.MarkdownEditorSpacing
import com.wim.markdown.ui.MarkdownTableStyle
import com.wim.markdown.ui.MarkdownTypography

/**
 * 所见即所得 markdown 编辑器。
 *
 * @param state 通过 [MarkdownEditorState] 或 [MarkdownEditorState.fromMarkdown] 创建并持有
 * @param mode 交互形态，运行时可切换，内容不丢失
 * @param toolbarItems 工具栏按钮及其顺序，可自定义排序或裁剪
 * @param headingPicker 标题选择器形态：弹出菜单或平铺按钮
 * @param headingOptions 标题选项及顺序（0 = 正文，1..6 = 标题级别），两种形态均生效
 * @param toolbarActiveColor 工具栏选中/高亮颜色，默认取主题 primary
 */
@Composable
fun MarkdownEditor(
    state: MarkdownEditorState,
    mode: MarkdownEditorMode,
    modifier: Modifier = Modifier,
    showToolbar: Boolean = true,
    toolbarItems: List<ToolbarItem> = ToolbarItem.entries,
    headingPicker: HeadingPickerStyle = HeadingPickerStyle.POPUP,
    headingOptions: List<Int> = DefaultHeadingOptions,
    toolbarActiveColor: Color = Color.Unspecified,
    spacing: MarkdownEditorSpacing = DefaultMarkdownEditorSpacing,
    typography: MarkdownTypography = MarkdownTypography(),
    tableStyle: MarkdownTableStyle = MarkdownTableStyle(),
    readOnly: Boolean = false,
    showTableActions: Boolean = true,
    autoImePadding: Boolean = true,
    showToolbarWhenKeyboardShown: Boolean = true,
    showToolbarWhenKeyboardHidden: Boolean = true,
) {
    LaunchedEffect(mode) { state.setMode(mode) }

    val isKeyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val shouldShowToolbar = !readOnly && showToolbar && (if (isKeyboardVisible) showToolbarWhenKeyboardShown else showToolbarWhenKeyboardHidden)

    Column(
        modifier.then(if (autoImePadding) Modifier.imePadding() else Modifier)
    ) {
        CompositionLocalProvider(
            LocalMarkdownEditorSpacing provides spacing,
            LocalMarkdownTypography provides typography,
            LocalMarkdownTableStyle provides tableStyle,
        ) {
            LazyColumn(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = spacing.horizontalPadding),
            ) {
                itemsIndexed(state.blocks) { index, block ->
                    BlockEditor(
                        index = index,
                        block = block,
                        state = state,
                        mode = mode,
                        previousBlock = state.blocks.getOrNull(index - 1),
                        readOnly = readOnly,
                        showTableActions = showTableActions,
                    )
                }
            }
        }
        if (shouldShowToolbar) {
            HorizontalDivider()
            EditorToolbar(
                state = state,
                items = toolbarItems,
                headingPicker = headingPicker,
                headingOptions = headingOptions,
                activeColor = toolbarActiveColor,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
