package com.wim.markdown

/**
 * 工具栏按钮项。枚举声明顺序即默认显示顺序，
 * 调用方可通过 [MarkdownEditor] 的 toolbarItems 参数自定义顺序或裁剪按钮。
 */
enum class ToolbarItem {
    Bold,
    Italic,
    Strikethrough,
    Underline,
    Code,
    Heading,
    BulletList,
    NumberedList,
    IndentDecrease,
    IndentIncrease,
    Quote,
    Divider,
    Table,
}

/** 标题选择器形态 */
enum class HeadingPickerStyle {
    /** 弹出下拉菜单选择正文/H1-H6 */
    POPUP,

    /** 正文/H1-H6 平铺在工具栏上，当前级别高亮 */
    INLINE,
}

/**
 * 标题选择器默认选项及顺序：0 表示正文，1..6 表示对应级别标题。
 * 调用方可重排或裁剪，平铺与弹出两种形态均生效。
 */
val DefaultHeadingOptions: List<Int> = listOf(0, 1, 2, 3, 4, 5, 6)
