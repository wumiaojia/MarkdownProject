# Markdown

[![JitPack](https://jitpack.io/v/wumiaojia/markdown.svg)](https://jitpack.io/#wumiaojia/markdown)

一个使用 Kotlin 和 Jetpack Compose 实现的 Android Markdown 富文本编辑器。

它提供富文本工具栏与行内 Markdown 两种编辑模式，使用统一的块模型管理内容，并支持在 Markdown、HTML 和编辑器状态之间转换。编辑器的工具栏、排版间距、字号和表格样式都可以按需配置。

## 功能

- 纯 Jetpack Compose UI
- 富文本工具栏与行内 Markdown 两种编辑模式，可在运行时切换
- Markdown 和 HTML 的导入、编辑与导出
- 粗体、斜体、删除线、下划线和行内代码
- H1～H6 标题、段落、引用、分割线
- 有序列表、无序列表和多级缩进
- 可编辑表格，支持增删行列、行表头和列表头
- 工具栏按钮、顺序和标题选项可配置
- 编辑器间距、排版字号和表格视觉样式可配置
- 只读模式及软键盘适配

## 安装

在项目的 `settings.gradle.kts` 中添加 JitPack：

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
            content {
                includeGroup("com.github.wumiaojia")
            }
        }
    }
}
```

添加依赖：

```kotlin
dependencies {
    implementation("com.github.wumiaojia:markdown:0.1.2")
}
```

## 环境要求

- Android `minSdk 29`
- Java 17
- Jetpack Compose

## 快速开始

使用 `MarkdownEditorState` 持有编辑内容，并把状态和编辑模式传给 `MarkdownEditor`：

```kotlin
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.wim.markdown.MarkdownEditor
import com.wim.markdown.MarkdownEditorMode
import com.wim.markdown.state.MarkdownEditorState

@Composable
fun EditorScreen() {
    val editorState = remember {
        MarkdownEditorState.fromMarkdown(
            """
            # 欢迎使用

            这是一个支持 **Markdown** 的 Compose 编辑器。
            """.trimIndent()
        )
    }

    MarkdownEditor(
        state = editorState,
        mode = MarkdownEditorMode.RICH_TOOLBAR,
        modifier = Modifier.fillMaxSize(),
    )
}
```

`MarkdownEditorState` 本身包含 Compose 可观察状态，因此应使用 `remember` 保存实例，避免在重组时重新创建编辑器内容。

## 导入与导出

### Markdown

```kotlin
val state = MarkdownEditorState.fromMarkdown(markdown)
val result = state.toMarkdown()
```

### HTML

```kotlin
val state = MarkdownEditorState.fromHtml(html)
val result = state.toHtml()
```

如果需要用新内容替换整篇文档，请创建新的状态实例：

```kotlin
var editorState by remember {
    mutableStateOf(MarkdownEditorState.fromMarkdown(initialMarkdown))
}

fun replaceContent(markdown: String) {
    editorState = MarkdownEditorState.fromMarkdown(markdown)
}
```

建议在保存、分享或按钮点击等事件中调用 `toMarkdown()` 或 `toHtml()`，不要在每次 Compose 重组时调用。

## 编辑模式

`MarkdownEditorMode` 提供两种交互方式：

| 模式 | 说明 |
| --- | --- |
| `RICH_TOOLBAR` | 隐藏 Markdown 标记，通过工具栏编辑样式，适合所见即所得场景 |
| `INLINE_MARKDOWN` | 当前获得焦点的块显示 Markdown 源码并高亮标记，其他块保持渲染状态 |

模式可以在运行时切换，编辑内容不会丢失：

```kotlin
var mode by remember { mutableStateOf(MarkdownEditorMode.RICH_TOOLBAR) }

MarkdownEditor(
    state = editorState,
    mode = mode,
)

// 在按钮或菜单事件中切换
mode = MarkdownEditorMode.INLINE_MARKDOWN
```

## `MarkdownEditor` 配置

| 参数 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `state` | `MarkdownEditorState` | 必填 | 编辑器状态和内容 |
| `mode` | `MarkdownEditorMode` | 必填 | 当前编辑模式 |
| `modifier` | `Modifier` | `Modifier` | 编辑器布局修饰符 |
| `showToolbar` | `Boolean` | `true` | 是否显示内置工具栏 |
| `toolbarItems` | `List<ToolbarItem>` | 全部按钮 | 工具栏按钮及显示顺序 |
| `headingPicker` | `HeadingPickerStyle` | `POPUP` | 标题选择器样式 |
| `headingOptions` | `List<Int>` | `0, 1, 2, 3, 4, 5, 6` | 标题级别及顺序，`0` 表示正文 |
| `toolbarActiveColor` | `Color` | `Color.Unspecified` | 工具栏激活颜色，未指定时使用主题主色 |
| `spacing` | `MarkdownEditorSpacing` | 默认间距 | 块间距和水平留白 |
| `typography` | `MarkdownTypography` | 默认字号 | 标题、正文、列表和表格字号 |
| `tableStyle` | `MarkdownTableStyle` | 默认表格样式 | 表格颜色、尺寸和布局 |
| `readOnly` | `Boolean` | `false` | 只读模式；同时隐藏工具栏 |
| `showTableActions` | `Boolean` | `true` | 表格获得焦点时是否显示操作按钮 |
| `autoImePadding` | `Boolean` | `true` | 是否自动添加软键盘底部间距 |
| `showToolbarWhenKeyboardShown` | `Boolean` | `true` | 键盘显示时是否显示工具栏 |
| `showToolbarWhenKeyboardHidden` | `Boolean` | `true` | 键盘隐藏时是否显示工具栏 |

完整配置示例：

```kotlin
MarkdownEditor(
    state = editorState,
    mode = MarkdownEditorMode.RICH_TOOLBAR,
    modifier = Modifier.fillMaxSize(),
    showToolbar = true,
    toolbarItems = listOf(
        ToolbarItem.Bold,
        ToolbarItem.Italic,
        ToolbarItem.Heading,
        ToolbarItem.BulletList,
        ToolbarItem.NumberedList,
        ToolbarItem.Quote,
        ToolbarItem.Table,
    ),
    headingPicker = HeadingPickerStyle.INLINE,
    headingOptions = listOf(0, 1, 2, 3),
    toolbarActiveColor = MaterialTheme.colorScheme.tertiary,
    readOnly = false,
    showTableActions = true,
    autoImePadding = true,
)
```

## 工具栏

可用的 `ToolbarItem`：

| 配置项 | 功能 |
| --- | --- |
| `Bold` | 粗体 |
| `Italic` | 斜体 |
| `Strikethrough` | 删除线 |
| `Underline` | 下划线 |
| `Code` | 行内代码 |
| `Heading` | 正文与标题切换 |
| `BulletList` | 无序列表 |
| `NumberedList` | 有序列表 |
| `IndentDecrease` | 减少列表缩进 |
| `IndentIncrease` | 增加列表缩进 |
| `Quote` | 引用 |
| `Divider` | 分割线 |
| `Table` | 插入表格 |

`toolbarItems` 的顺序就是最终显示顺序，也可以传入空列表隐藏所有按钮。`HeadingPickerStyle.POPUP` 使用弹出菜单，`HeadingPickerStyle.INLINE` 则把标题选项直接平铺在工具栏中。

## 排版与样式

### 块间距

```kotlin
val spacing = MarkdownEditorSpacing(
    horizontalPadding = 20.dp,
    paragraphSpacing = 8.dp,
    headingTopSpacing = 20.dp,
    listItemSpacing = 4.dp,
)
```

`MarkdownEditorSpacing` 的全部配置：

| 参数 | 默认值 | 说明 |
| --- | --- | --- |
| `horizontalPadding` | `16.dp` | 编辑区域左右留白 |
| `paragraphSpacing` | `6.dp` | 普通段落之间的间距 |
| `headingTopSpacing` | `16.dp` | 标题上方间距 |
| `headingBottomSpacing` | `6.dp` | 标题与后续段落的间距 |
| `headingToHeadingSpacing` | `8.dp` | 连续标题之间的间距 |
| `listItemSpacing` | `2.dp` | 连续列表项之间的间距 |
| `listGroupSpacing` | `8.dp` | 列表组与其他块之间的间距 |
| `quoteSpacing` | `8.dp` | 引用块间距 |
| `dividerSpacing` | `12.dp` | 分割线间距 |
| `tableSpacing` | `12.dp` | 表格间距 |

### 字号

```kotlin
val typography = MarkdownTypography(
    headingSizes = listOf(32.sp, 28.sp, 24.sp, 20.sp, 18.sp, 16.sp),
    paragraphSize = 16.sp,
    quoteSize = 16.sp,
)
```

`MarkdownTypography` 的全部配置：

| 参数 | 默认值 | 说明 |
| --- | --- | --- |
| `headingSizes` | `28, 24, 20, 18, 16, 15.sp` | 按 H1 到 H6 排列的标题字号 |
| `paragraphSize` | `16.sp` | 段落字号 |
| `listItemSize` | `16.sp` | 列表项字号 |
| `tableHeaderSize` | `14.sp` | 表头字号 |
| `tableBodySize` | `14.sp` | 表格正文的字号 |
| `quoteSize` | `16.sp` | 引用字号 |

### 表格

```kotlin
val tableStyle = MarkdownTableStyle(
    headerBackgroundColor = MaterialTheme.colorScheme.surfaceVariant,
    borderColor = MaterialTheme.colorScheme.outlineVariant,
    cornerRadius = 8.dp,
    defaultAlignment = TextAlign.Start,
    minColumnWidth = 80.dp,
    maxColumnWidth = 320.dp,
    layoutMode = TableLayoutMode.ADAPTIVE,
    enableHorizontalScroll = true,
    showHorizontalScrollbar = true,
)
```

`MarkdownTableStyle` 的全部配置：

| 参数 | 默认值 | 说明 |
| --- | --- | --- |
| `backgroundColor` | `Color.Transparent` | 表格背景色 |
| `headerBackgroundColor` | `Color.Transparent` | 表头背景色 |
| `contentColor` | `Color.Unspecified` | 单元格文字颜色 |
| `headerContentColor` | `Color.Unspecified` | 表头文字颜色 |
| `borderColor` | `Color.LightGray` | 边框颜色 |
| `borderWidth` | `1.dp` | 边框宽度 |
| `cornerRadius` | `4.dp` | 圆角 |
| `cellHorizontalPadding` | `8.dp` | 单元格水平内边距 |
| `cellVerticalPadding` | `8.dp` | 单元格垂直内边距 |
| `defaultAlignment` | `TextAlign.Start` | 单元格默认文字对齐 |
| `minColumnWidth` | `64.dp` | 最小列宽；传 `null` 表示不限制 |
| `maxColumnWidth` | `400.dp` | 最大列宽；传 `null` 表示不限制 |
| `enableHorizontalScroll` | `true` | 内容超宽时是否允许横向滚动 |
| `layoutMode` | `ADAPTIVE` | 表格列宽布局模式 |
| `showHorizontalScrollbar` | `true` | 是否显示横向滚动条 |

表格布局模式：

- `TableLayoutMode.ADAPTIVE`：列宽根据内容自适应，并受最小、最大列宽限制。
- `TableLayoutMode.STRETCH`：所有列平均分配可用宽度并撑满整行。

最后把配置传给编辑器：

```kotlin
MarkdownEditor(
    state = editorState,
    mode = mode,
    spacing = spacing,
    typography = typography,
    tableStyle = tableStyle,
)
```

## 状态与编程式操作

常用的 `MarkdownEditorState` API：

| API | 说明 |
| --- | --- |
| `fromMarkdown(markdown)` | 从 Markdown 创建状态 |
| `fromHtml(html)` | 从 HTML 创建状态 |
| `toMarkdown()` | 导出 Markdown |
| `toHtml()` | 导出 HTML |
| `blocks` | 当前可观察的块列表 |
| `focusBlock(index, cursor)` | 聚焦指定文本块 |
| `clearFocus()` | 清除焦点并提交当前行内编辑 |
| `toggleInlineStyle(style)` | 切换粗体、斜体等行内样式 |
| `setBlockType(type)` | 切换段落、标题、列表或引用 |
| `changeIndent(delta)` | 调整当前列表项缩进，范围为 0～3 |
| `insertDivider()` | 在当前块后插入分割线 |
| `insertTable(rows, columns)` | 在当前块后插入表格，默认 2 × 2 |
| `addTableRow()` / `removeTableRow()` | 增删表格末行 |
| `addTableColumn()` / `removeTableColumn()` | 增删表格末列 |
| `toggleTableHeaderRow()` | 切换首行表头 |
| `toggleTableHeaderColumn()` | 切换首列表头 |
| `deleteFocusedTable()` | 删除当前表格 |

例如：

```kotlin
editorState.insertTable(rows = 3, columns = 4)
editorState.insertDivider()

val markdown = editorState.toMarkdown()
val html = editorState.toHtml()
```

通常应优先通过 `MarkdownEditor` 和内置工具栏操作状态。直接修改 `blocks` 适合需要自定义编辑流程的高级场景。

## 支持的语法

### Markdown

| 类型 | 支持的语法 |
| --- | --- |
| 标题 | `#` 到 `######` |
| 粗体 | `**text**` |
| 斜体 | `_text_` |
| 删除线 | `~~text~~` |
| 下划线 | `<u>text</u>` |
| 行内代码 | `` `code` `` |
| 无序列表 | `- item` |
| 有序列表 | `1. item` |
| 列表缩进 | 每级两个空格 |
| 引用 | `> quote` |
| 分割线 | `---`、`***` 或 `___` |
| 表格 | 使用竖线分隔的 Markdown 表格 |

### HTML

块级元素：

- `<p>`
- `<h1>`～`<h6>`
- `<blockquote>`
- `<ul>`、`<ol>` 和 `<li>`
- `<hr>`
- `<table>`、`<tr>`、`<th>` 和 `<td>`

行内元素：

- `<strong>` / `<b>`
- `<em>` / `<i>`
- `<s>` / `<strike>` / `<del>`
- `<u>`
- `<code>`

HTML 支持面向编辑器自身的导入与导出格式，不是完整的浏览器 HTML/CSS 解析器。未知标签、复杂嵌套结构和样式属性不会被完整保留。

## 当前限制

- 暂不支持链接、图片、任务列表和围栏代码块。
- 不以完整 CommonMark 或 GitHub Flavored Markdown 兼容为目标。
- HTML 仅支持上面列出的子集。
- 有序列表导出时会根据连续列表项重新计算序号。
- 直接替换整篇内容时需要创建新的 `MarkdownEditorState`。

## 示例项目

仓库中的 `app` 模块展示了：

- Markdown 与 HTML 切换
- 两种编辑模式
- 工具栏按钮裁剪
- 只读模式
- 自定义间距和字号
- 表格主题、列宽、对齐方式和布局模式
- Markdown / HTML 导出

运行示例：

```bash
./gradlew :app:installDebug
```

## 本地验证

```bash
./gradlew :markdown:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug
```
