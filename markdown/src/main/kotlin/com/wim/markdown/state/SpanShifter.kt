package com.wim.markdown.state

import com.wim.markdown.model.InlineStyle
import com.wim.markdown.model.StyleRange
import com.wim.markdown.model.normalizeSpans

/**
 * 编辑发生后平移/裁剪/合并样式区间。
 * 通过新旧文本的公共前后缀定位唯一编辑区间，按任意 diff 处理（兼容 IME 批量替换）。
 */
object SpanShifter {

    /**
     * @param overrideStyles 插入文本的权威样式覆盖集：null 表示无覆盖（自然延续既有样式）；
     * 非 null 时集合内样式作用于插入区间，集合外样式从插入区间扣除（抑制边缘延续、拆分区间）。
     */
    fun shift(
        oldText: String,
        oldSpans: List<StyleRange>,
        newText: String,
        overrideStyles: Set<InlineStyle>? = null,
    ): List<StyleRange> {
        if (oldText == newText) return oldSpans

        var prefix = 0
        val maxPrefix = minOf(oldText.length, newText.length)
        while (prefix < maxPrefix && oldText[prefix] == newText[prefix]) prefix++

        var suffix = 0
        val maxSuffix = maxPrefix - prefix
        while (
            suffix < maxSuffix &&
            oldText[oldText.length - 1 - suffix] == newText[newText.length - 1 - suffix]
        ) suffix++

        val oldEnd = oldText.length - suffix
        val insLen = newText.length - suffix - prefix
        val delta = newText.length - oldText.length

        val shifted = oldSpans.mapNotNull { span ->
            val s = span.start
            val e = span.end
            when {
                e < prefix -> span
                s > oldEnd -> span.copy(start = s + delta, end = e + delta)
                s <= prefix && e >= oldEnd -> span.copy(end = e + delta)
                s <= prefix -> span.copy(end = prefix)
                e >= oldEnd -> span.copy(start = prefix + insLen, end = e + delta)
                else -> null
            }
        }
        if (overrideStyles == null || insLen == 0) {
            return normalizeSpans(shifted, newText.length)
        }
        val insStart = prefix
        val insEnd = prefix + insLen
        val adjusted = shifted.flatMap { r ->
            when {
                r.style in overrideStyles || r.end <= insStart || r.start >= insEnd -> listOf(r)
                else -> listOfNotNull(
                    if (r.start < insStart) r.copy(end = insStart) else null,
                    if (r.end > insEnd) r.copy(start = insEnd) else null,
                )
            }
        }
        val applied = overrideStyles.map { StyleRange(insStart, insEnd, it) }
        return normalizeSpans(adjusted + applied, newText.length)
    }
}
