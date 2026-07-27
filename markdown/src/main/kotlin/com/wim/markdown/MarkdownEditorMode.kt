package com.wim.markdown

/** 编辑器交互形态 */
enum class MarkdownEditorMode {
    /** 富文本工具栏模式：不显示语法符号，通过工具栏操作样式 */
    RICH_TOOLBAR,

    /** 行内编辑模式：焦点块显示 markdown 源码并实时着色，非焦点块完全渲染 */
    INLINE_MARKDOWN,
}
