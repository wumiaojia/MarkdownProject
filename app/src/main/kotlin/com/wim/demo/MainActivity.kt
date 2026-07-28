package com.wim.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wim.markdown.*
import com.wim.markdown.state.MarkdownEditorState
import com.wim.markdown.ui.MarkdownEditorSpacing
import com.wim.markdown.ui.MarkdownTableStyle
import com.wim.markdown.ui.MarkdownTypography
import com.wim.markdown.ui.TableLayoutMode
import com.wim.demo.ui.theme.MarkDownTheme
import androidx.compose.ui.text.style.TextAlign

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MarkDownTheme {
                MarkdownDemoScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownDemoScreen() {
    val initialMarkdown = """
        # Markdown & HTML 原生实验室

        这是一个 100% **Compose** 实现的富文本编辑器，支持 **Markdown** 与 **HTML** 混合解析。

        ### 1. 响应式表格 (内容自适应 + 智能换行)
        默认开启 **ADAPTIVE** 布局，最大宽度限制为 **400dp**。下方表格演示了长文本自动换行与行高对齐：

        | 姓名 | 职位 | 联系方式 | 详细备注 |
        | :--- | :--- | :--- | :--- |
        | 张三 | 高级 Android 开发工程师 | zhangsan@example.com | 负责移动端架构设计与组件化重构，具有丰富的性能优化经验。负责移动端架构设计与组件化重构，具有丰富的性能优化经验。负责移动端架构设计与组件化重构，具有丰富的性能优化经验。 |
        | 李四 | 产品经理 | lisi@example.com | 关注用户体验。 |

        ---

        ### 2. 均分布满示例 (STRETCH)
        在设置面板切换至“均分布满”，表格将强制填满屏幕宽度：

        | 标题 A | 标题 B | 标题 C |
        | :--- | :--- | :--- |
        | 数据 1 | 数据 2 | 数据 3 |
    """.trimIndent()

    var state by remember { mutableStateOf(MarkdownEditorState.fromMarkdown(initialMarkdown)) }
    var mode by remember { mutableStateOf(MarkdownEditorMode.RICH_TOOLBAR) }
    var readOnly by remember { mutableStateOf(false) }
    var showToolbar by remember { mutableStateOf(true) }
    var showToolbarWhenKeyboardShown by remember { mutableStateOf(true) }
    var showToolbarWhenKeyboardHidden by remember { mutableStateOf(true) }
    var headingStyle by remember { mutableStateOf(HeadingPickerStyle.POPUP) }
    var isHtmlMode by remember { mutableStateOf(false) }
    var showTableActions by remember { mutableStateOf(true) }
    var enabledToolbarItems by remember { mutableStateOf(ToolbarItem.entries.toSet()) }

    // 间距配置
    var paragraphSpacing by remember { mutableFloatStateOf(6f) }
    var listItemSpacing by remember { mutableFloatStateOf(2f) }
    var horizontalPadding by remember { mutableFloatStateOf(16f) }

    // 视觉配置
    var h1Size by remember { mutableFloatStateOf(28f) }
    var tableCornerRadius by remember { mutableFloatStateOf(4f) }
    var tableAlignment by remember { mutableStateOf(TextAlign.Start) }
    var tableTheme by remember { mutableStateOf("Default") }
    var minColumnWidth by remember { mutableFloatStateOf(64f) }
    var maxColumnWidth by remember { mutableFloatStateOf(400f) }
    var cellHorizontalPadding by remember { mutableFloatStateOf(8f) }
    var cellVerticalPadding by remember { mutableFloatStateOf(8f) }
    var enableHorizontalScroll by remember { mutableStateOf(true) }
    var tableLayoutMode by remember { mutableStateOf(TableLayoutMode.ADAPTIVE) }
    var showScrollbar by remember { mutableStateOf(true) }

    var showSettings by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf<String?>(null) }

    // 检测键盘是否显示，用于隐藏 BottomAppBar
    val isKeyboardVisible = WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0

    val spacing = MarkdownEditorSpacing(
        paragraphSpacing = paragraphSpacing.dp,
        listItemSpacing = listItemSpacing.dp,
        horizontalPadding = horizontalPadding.dp
    )

    val typography = MarkdownTypography(
        headingSizes = listOf(
            h1Size.sp,
            (h1Size * 0.85f).sp,
            (h1Size * 0.72f).sp,
            (h1Size * 0.65f).sp,
            (h1Size * 0.58f).sp,
            (h1Size * 0.54f).sp
        )
    )

    val baseTableStyle = MarkdownTableStyle(
        cornerRadius = tableCornerRadius.dp,
        cellHorizontalPadding = cellHorizontalPadding.dp,
        cellVerticalPadding = cellVerticalPadding.dp,
        defaultAlignment = tableAlignment,
        minColumnWidth = if (minColumnWidth > 0) minColumnWidth.dp else null,
        maxColumnWidth = if (maxColumnWidth > 0) maxColumnWidth.dp else null,
        enableHorizontalScroll = enableHorizontalScroll,
        layoutMode = tableLayoutMode,
        showHorizontalScrollbar = showScrollbar,
    )
    val tableStyle = when (tableTheme) {
        "Blue" -> baseTableStyle.copy(
            headerBackgroundColor = Color(0xFFE3F2FD),
            borderColor = Color(0xFF90CAF9),
            headerContentColor = Color(0xFF1976D2),
        )
        "Dark" -> baseTableStyle.copy(
            backgroundColor = Color(0xFF2C2C2C),
            headerBackgroundColor = Color(0xFF3D3D3D),
            borderColor = Color(0xFF555555),
            contentColor = Color.White,
            headerContentColor = Color(0xFFBB86FC),
        )
        else -> baseTableStyle
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Markdown Lab") },
                actions = {
                    IconButton(onClick = {
                        val content = if (isHtmlMode) state.toHtml() else state.toMarkdown()
                        showExportDialog = content
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "导出")
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        },
        bottomBar = {
            // 当键盘弹出时，隐藏 BottomAppBar 以便工具栏完美贴合键盘
            if (showToolbar && !isKeyboardVisible) {
                BottomAppBar {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 模式切换
                        SingleChoiceSegmentedButtonRow {
                            SegmentedButton(
                                selected = mode == MarkdownEditorMode.RICH_TOOLBAR,
                                onClick = { mode = MarkdownEditorMode.RICH_TOOLBAR },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) { Text("富文本") }
                            SegmentedButton(
                                selected = mode == MarkdownEditorMode.INLINE_MARKDOWN,
                                onClick = { mode = MarkdownEditorMode.INLINE_MARKDOWN },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) { Text("源码") }
                        }

                        // 快速动作
                        Row {
                            IconButton(onClick = { state.insertTable() }) {
                                Icon(Icons.Default.GridOn, contentDescription = "插入表格")
                            }
                            IconButton(onClick = { state = MarkdownEditorState() }) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "清空")
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding) // 消费掉 Scaffold 的 Padding，防止内部 imePadding 重复计算
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            MarkdownEditor(
                state = state,
                mode = mode,
                readOnly = readOnly,
                showToolbar = showToolbar,
                showToolbarWhenKeyboardShown = showToolbarWhenKeyboardShown,
                showToolbarWhenKeyboardHidden = showToolbarWhenKeyboardHidden,
                toolbarItems = ToolbarItem.entries.filter { it in enabledToolbarItems },
                headingPicker = headingStyle,
                spacing = spacing,
                typography = typography,
                tableStyle = tableStyle,
                showTableActions = showTableActions,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    if (showSettings) {
        ModalBottomSheet(onDismissRequest = { showSettings = false }) {
            SettingsPanel(
                readOnly = readOnly,
                onReadOnlyChange = { readOnly = it },
                showToolbar = showToolbar,
                onShowToolbarChange = { showToolbar = it },
                headingStyle = headingStyle,
                onHeadingStyleChange = { headingStyle = it },
                isHtmlMode = isHtmlMode,
                onHtmlModeChange = {
                    val currentContent = if (isHtmlMode) state.toHtml() else state.toMarkdown()
                    isHtmlMode = it
                    state = if (it) MarkdownEditorState.fromHtml(currentContent)
                            else MarkdownEditorState.fromMarkdown(currentContent)
                },
                paragraphSpacing = paragraphSpacing,
                onParagraphSpacingChange = { paragraphSpacing = it },
                listItemSpacing = listItemSpacing,
                onListItemSpacingChange = { listItemSpacing = it },
                horizontalPadding = horizontalPadding,
                onHorizontalPaddingChange = { horizontalPadding = it },
                showTableActions = showTableActions,
                onShowTableActionsChange = { showTableActions = it },
                showToolbarWhenKeyboardShown = showToolbarWhenKeyboardShown,
                onShowToolbarWhenKeyboardShownChange = { showToolbarWhenKeyboardShown = it },
                showToolbarWhenKeyboardHidden = showToolbarWhenKeyboardHidden,
                onShowToolbarWhenKeyboardHiddenChange = { showToolbarWhenKeyboardHidden = it },
                enabledToolbarItems = enabledToolbarItems,
                onEnabledToolbarItemsChange = { enabledToolbarItems = it },
                h1Size = h1Size,
                onH1SizeChange = { h1Size = it },
                tableCornerRadius = tableCornerRadius,
                onTableCornerRadiusChange = { tableCornerRadius = it },
                tableAlignment = tableAlignment,
                onTableAlignmentChange = { tableAlignment = it },
                tableTheme = tableTheme,
                onTableThemeChange = { tableTheme = it },
                minColumnWidth = minColumnWidth,
                onMinColumnWidthChange = { minColumnWidth = it },
                maxColumnWidth = maxColumnWidth,
                onMaxColumnWidthChange = { maxColumnWidth = it },
                cellHorizontalPadding = cellHorizontalPadding,
                onCellHorizontalPaddingChange = { cellHorizontalPadding = it },
                cellVerticalPadding = cellVerticalPadding,
                onCellVerticalPaddingChange = { cellVerticalPadding = it },
                enableHorizontalScroll = enableHorizontalScroll,
                onEnableHorizontalScrollChange = { enableHorizontalScroll = it },
                tableLayoutMode = tableLayoutMode,
                onTableLayoutModeChange = { tableLayoutMode = it },
                showScrollbar = showScrollbar,
                onShowScrollbarChange = { showScrollbar = it }
            )
        }
    }

    if (showExportDialog != null) {
        AlertDialog(
            onDismissRequest = { showExportDialog = null },
            title = { Text(if (isHtmlMode) "HTML 导出" else "Markdown 导出") },
            text = {
                Box(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                    Text(showExportDialog!!, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = null }) { Text("关闭") }
            }
        )
    }
}

@Composable
fun SettingsPanel(
    readOnly: Boolean,
    onReadOnlyChange: (Boolean) -> Unit,
    showToolbar: Boolean,
    onShowToolbarChange: (Boolean) -> Unit,
    headingStyle: HeadingPickerStyle,
    onHeadingStyleChange: (HeadingPickerStyle) -> Unit,
    isHtmlMode: Boolean,
    onHtmlModeChange: (Boolean) -> Unit,
    paragraphSpacing: Float,
    onParagraphSpacingChange: (Float) -> Unit,
    listItemSpacing: Float,
    onListItemSpacingChange: (Float) -> Unit,
    horizontalPadding: Float,
    onHorizontalPaddingChange: (Float) -> Unit,
    showTableActions: Boolean,
    onShowTableActionsChange: (Boolean) -> Unit,
    showToolbarWhenKeyboardShown: Boolean,
    onShowToolbarWhenKeyboardShownChange: (Boolean) -> Unit,
    showToolbarWhenKeyboardHidden: Boolean,
    onShowToolbarWhenKeyboardHiddenChange: (Boolean) -> Unit,
    enabledToolbarItems: Set<ToolbarItem>,
    onEnabledToolbarItemsChange: (Set<ToolbarItem>) -> Unit,
    h1Size: Float,
    onH1SizeChange: (Float) -> Unit,
    tableCornerRadius: Float,
    onTableCornerRadiusChange: (Float) -> Unit,
    tableAlignment: TextAlign,
    onTableAlignmentChange: (TextAlign) -> Unit,
    tableTheme: String,
    onTableThemeChange: (String) -> Unit,
    minColumnWidth: Float,
    onMinColumnWidthChange: (Float) -> Unit,
    maxColumnWidth: Float,
    onMaxColumnWidthChange: (Float) -> Unit,
    cellHorizontalPadding: Float,
    onCellHorizontalPaddingChange: (Float) -> Unit,
    cellVerticalPadding: Float,
    onCellVerticalPaddingChange: (Float) -> Unit,
    enableHorizontalScroll: Boolean,
    onEnableHorizontalScrollChange: (Boolean) -> Unit,
    tableLayoutMode: TableLayoutMode,
    onTableLayoutModeChange: (TableLayoutMode) -> Unit,
    showScrollbar: Boolean,
    onShowScrollbarChange: (Boolean) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState())
    ) {
        Text("通用设置", style = MaterialTheme.typography.titleMedium)
        ListItem(
            headlineContent = { Text("只读模式") },
            trailingContent = { Switch(readOnly, onReadOnlyChange) }
        )
        ListItem(
            headlineContent = { Text("显示工具栏") },
            trailingContent = { Switch(showToolbar, onShowToolbarChange) }
        )
        ListItem(
            headlineContent = { Text("数据格式: ${if (isHtmlMode) "HTML" else "Markdown"}") },
            trailingContent = { Switch(isHtmlMode, onHtmlModeChange) }
        )
        ListItem(
            headlineContent = { Text("显示表格辅助按钮") },
            trailingContent = { Switch(showTableActions, onShowTableActionsChange) }
        )
        ListItem(
            headlineContent = { Text("键盘弹出时显示工具栏") },
            trailingContent = { Switch(showToolbarWhenKeyboardShown, onShowToolbarWhenKeyboardShownChange) }
        )
        ListItem(
            headlineContent = { Text("键盘收起时显示工具栏") },
            trailingContent = { Switch(showToolbarWhenKeyboardHidden, onShowToolbarWhenKeyboardHiddenChange) }
        )

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        Text("标题选择器样式", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = headingStyle == HeadingPickerStyle.POPUP,
                onClick = { onHeadingStyleChange(HeadingPickerStyle.POPUP) },
                label = { Text("弹出菜单") }
            )
            FilterChip(
                selected = headingStyle == HeadingPickerStyle.INLINE,
                onClick = { onHeadingStyleChange(HeadingPickerStyle.INLINE) },
                label = { Text("工具栏平铺") }
            )
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        Text("工具栏功能配置", style = MaterialTheme.typography.titleMedium)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolbarItem.entries.forEach { item ->
                FilterChip(
                    selected = item in enabledToolbarItems,
                    onClick = {
                        val newSet = if (item in enabledToolbarItems) {
                            enabledToolbarItems - item
                        } else {
                            enabledToolbarItems + item
                        }
                        onEnabledToolbarItemsChange(newSet)
                    },
                    label = { Text(item.name) }
                )
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        Text("间距与排版 (dp)", style = MaterialTheme.typography.titleMedium)
        Text("段落间距: ${paragraphSpacing.toInt()}", style = MaterialTheme.typography.bodySmall)
        Slider(paragraphSpacing, onParagraphSpacingChange, valueRange = 0f..20f)

        Text("列表项间距: ${listItemSpacing.toInt()}", style = MaterialTheme.typography.bodySmall)
        Slider(listItemSpacing, onListItemSpacingChange, valueRange = 0f..10f)

        Text("页边距: ${horizontalPadding.toInt()}", style = MaterialTheme.typography.bodySmall)
        Slider(horizontalPadding, onHorizontalPaddingChange, valueRange = 0f..32f)

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        Text("视觉高级定制", style = MaterialTheme.typography.titleMedium)
        Text("基础标题字号 (H1): ${h1Size.toInt()}sp", style = MaterialTheme.typography.bodySmall)
        Slider(h1Size, onH1SizeChange, valueRange = 16f..48f)

        Text("表格圆角: ${tableCornerRadius.toInt()}dp", style = MaterialTheme.typography.bodySmall)
        Slider(tableCornerRadius, onTableCornerRadiusChange, valueRange = 0f..16f)

        Text("表格默认对齐", style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = tableAlignment == TextAlign.Start, onClick = { onTableAlignmentChange(TextAlign.Start) }, label = { Text("左") })
            FilterChip(selected = tableAlignment == TextAlign.Center, onClick = { onTableAlignmentChange(TextAlign.Center) }, label = { Text("中") })
            FilterChip(selected = tableAlignment == TextAlign.End, onClick = { onTableAlignmentChange(TextAlign.End) }, label = { Text("右") })
        }

        Text("表格主题预设", style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = tableTheme == "Default", onClick = { onTableThemeChange("Default") }, label = { Text("默认") })
            FilterChip(selected = tableTheme == "Blue", onClick = { onTableThemeChange("Blue") }, label = { Text("简约蓝") })
            FilterChip(selected = tableTheme == "Dark", onClick = { onTableThemeChange("Dark") }, label = { Text("深色") })
        }

        Text("表格最小列宽: ${minColumnWidth.toInt()}dp", style = MaterialTheme.typography.bodySmall)
        Slider(minColumnWidth, onMinColumnWidthChange, valueRange = 0f..200f)

        Text("表格最大列宽: ${if (maxColumnWidth > 0) maxColumnWidth.toInt().toString() + "dp" else "无限制"}", style = MaterialTheme.typography.bodySmall)
        Slider(maxColumnWidth, onMaxColumnWidthChange, valueRange = 0f..500f)

        Text("单元格水平间距: ${cellHorizontalPadding.toInt()}dp", style = MaterialTheme.typography.bodySmall)
        Slider(cellHorizontalPadding, onCellHorizontalPaddingChange, valueRange = 0f..32f)

        Text("单元格垂直间距: ${cellVerticalPadding.toInt()}dp", style = MaterialTheme.typography.bodySmall)
        Slider(cellVerticalPadding, onCellVerticalPaddingChange, valueRange = 0f..32f)

        ListItem(
            headlineContent = { Text("允许表格横向滚动") },
            trailingContent = { Switch(enableHorizontalScroll, onEnableHorizontalScrollChange) }
        )

        ListItem(
            headlineContent = { Text("显示横向滚动条") },
            trailingContent = { Switch(showScrollbar, onShowScrollbarChange) }
        )

        Text("表格布局模式", style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = tableLayoutMode == TableLayoutMode.STRETCH, onClick = { onTableLayoutModeChange(TableLayoutMode.STRETCH) }, label = { Text("均分布满") })
            FilterChip(selected = tableLayoutMode == TableLayoutMode.ADAPTIVE, onClick = { onTableLayoutModeChange(TableLayoutMode.ADAPTIVE) }, label = { Text("内容自适应") })
        }

        Spacer(Modifier.height(32.dp))
    }
}
