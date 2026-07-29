package com.wim.markdown.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import com.wim.markdown.HeadingPickerStyle
import com.wim.markdown.ToolbarItem
import com.wim.markdown.model.Block
import com.wim.markdown.model.InlineStyle
import com.wim.markdown.state.BlockType
import com.wim.markdown.state.MarkdownEditorState

@Composable
internal fun EditorToolbar(
    state: MarkdownEditorState,
    items: List<ToolbarItem>,
    headingPicker: HeadingPickerStyle,
    headingOptions: List<Int>,
    activeColor: Color,
    modifier: Modifier = Modifier,
) {
    val active = state.activeStyles()
    val resolvedActive = activeColor.takeOrElse { MaterialTheme.colorScheme.primary }
    val focusedBlock = state.blocks.getOrNull(state.focusedIndex)
    val focusedListItem = focusedBlock as? Block.ListItem
    Row(modifier.horizontalScroll(rememberScrollState())) {
        items.forEach { item ->
            when (item) {
                ToolbarItem.Bold -> StyleButton(
                    Icons.Default.FormatBold,
                    "加粗",
                    InlineStyle.Bold in active,
                    resolvedActive,
                ) { state.toggleInlineStyle(InlineStyle.Bold) }

                ToolbarItem.Italic -> StyleButton(
                    Icons.Default.FormatItalic,
                    "斜体",
                    InlineStyle.Italic in active,
                    resolvedActive,
                ) { state.toggleInlineStyle(InlineStyle.Italic) }

                ToolbarItem.Strikethrough -> StyleButton(
                    Icons.Default.FormatStrikethrough,
                    "删除线",
                    InlineStyle.Strikethrough in active,
                    resolvedActive,
                ) { state.toggleInlineStyle(InlineStyle.Strikethrough) }

                ToolbarItem.Underline -> StyleButton(
                    Icons.Default.FormatUnderlined,
                    "下划线",
                    InlineStyle.Underline in active,
                    resolvedActive,
                ) { state.toggleInlineStyle(InlineStyle.Underline) }

                ToolbarItem.Code -> StyleButton(
                    Icons.Default.Code,
                    "代码",
                    InlineStyle.Code in active,
                    resolvedActive,
                ) { state.toggleInlineStyle(InlineStyle.Code) }

                ToolbarItem.Heading -> when (headingPicker) {
                    HeadingPickerStyle.POPUP ->
                        HeadingPopupButton(state, headingOptions, resolvedActive)
                    HeadingPickerStyle.INLINE ->
                        InlineHeadingButtons(state, headingOptions, resolvedActive)
                }

                ToolbarItem.BulletList -> StyleButton(
                    Icons.AutoMirrored.Filled.FormatListBulleted,
                    "无序列表",
                    focusedListItem?.let { !it.ordered && it.checked == null } == true,
                    resolvedActive,
                ) { state.setBlockType(BlockType.ListItem(ordered = false)) }

                ToolbarItem.NumberedList -> StyleButton(
                    Icons.Default.FormatListNumbered,
                    "有序列表",
                    focusedListItem?.let { it.ordered && it.checked == null } == true,
                    resolvedActive,
                ) { state.setBlockType(BlockType.ListItem(ordered = true)) }

                ToolbarItem.TaskList -> StyleButton(
                    Icons.Default.CheckBox,
                    "任务列表",
                    focusedListItem?.checked != null,
                    resolvedActive,
                ) { state.setBlockType(BlockType.TaskListItem) }

                ToolbarItem.IndentDecrease -> StyleButton(
                    Icons.AutoMirrored.Filled.FormatIndentDecrease,
                    "减少缩进",
                    false,
                    resolvedActive,
                ) { state.changeIndent(-1) }

                ToolbarItem.IndentIncrease -> StyleButton(
                    Icons.AutoMirrored.Filled.FormatIndentIncrease,
                    "增加缩进",
                    false,
                    resolvedActive,
                ) { state.changeIndent(1) }

                ToolbarItem.Quote -> StyleButton(
                    Icons.Default.FormatQuote,
                    "引用",
                    focusedBlock is Block.Quote,
                    resolvedActive,
                ) { state.setBlockType(BlockType.Quote) }

                ToolbarItem.Divider -> StyleButton(
                    Icons.Default.HorizontalRule,
                    "分割线",
                    false,
                    resolvedActive,
                ) { state.insertDivider() }

                ToolbarItem.Table -> StyleButton(
                    Icons.Default.TableChart,
                    "表格",
                    focusedBlock is Block.Table,
                    resolvedActive,
                ) { state.insertTable() }
            }
        }
    }
}

/** 当前焦点块对应的标题选项：0 = 正文，1..6 = 标题级别，null = 其它块类型 */
private fun currentHeadingOption(state: MarkdownEditorState): Int? =
    when (val block = state.blocks.getOrNull(state.focusedIndex)) {
        is Block.Heading -> block.level
        is Block.Paragraph -> 0
        else -> null
    }

private fun headingBlockType(option: Int): BlockType =
    if (option == 0) BlockType.Paragraph else BlockType.Heading(option)

private fun headingLabel(option: Int): String =
    if (option == 0) "正文"
    else "H$option"

@Composable
private fun StyleButton(
    icon: ImageVector,
    description: String,
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = if (active) activeColor
            else MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Icon(icon, contentDescription = description)
    }
}

@Composable
private fun HeadingPopupButton(
    state: MarkdownEditorState,
    options: List<Int>,
    activeColor: Color,
) {
    var expanded by remember { mutableStateOf(false) }
    val current = currentHeadingOption(state)
    Box {
        StyleButton(
            Icons.Default.Title,
            "标题",
            active = current != null && current != 0,
            activeColor = activeColor,
        ) { expanded = true }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = headingLabel(option),
                            color = if (option == current) activeColor else Color.Unspecified,
                        )
                    },
                    onClick = {
                        state.setBlockType(headingBlockType(option))
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun InlineHeadingButtons(
    state: MarkdownEditorState,
    options: List<Int>,
    activeColor: Color,
) {
    val current = currentHeadingOption(state)
    options.forEach { option ->
        HeadingChip(
            text = headingLabel(option),
            selected = option == current,
            activeColor = activeColor,
        ) { state.setBlockType(headingBlockType(option)) }
    }
}

@Composable
private fun HeadingChip(
    text: String,
    selected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick) {
        Text(
            text = text,
            color = if (selected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
