package com.wim.markdown.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.wim.markdown.serializer.InlineMarkdown

/**
 * INLINE 模式焦点块的实时着色：标记符号置灰、标记内容按样式渲染。
 * 不改变文本内容，OffsetMapping 恒等，光标无需映射。
 */
internal class InlineMarkdownVisualTransformation(
    private val markerColor: Color,
    private val codeBackground: Color,
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val parsed = InlineMarkdown.parse(text.text)
        val styled = buildAnnotatedString {
            append(text.text)
            parsed.sourceSpans.forEach { r ->
                addStyle(r.style.toSpanStyle(codeBackground), r.start, r.end)
            }
            parsed.markerRanges.forEach { range ->
                addStyle(SpanStyle(color = markerColor), range.first, range.last + 1)
            }
        }
        return TransformedText(styled, OffsetMapping.Identity)
    }
}
