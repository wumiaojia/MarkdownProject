package com.wim.markdown.serializer

import com.wim.markdown.model.InlineStyle
import com.wim.markdown.model.RichText
import com.wim.markdown.model.StyleRange
import com.wim.markdown.model.normalizeSpans

private const val UNDERLINE_OPEN = "<u>"
private const val UNDERLINE_CLOSE = "</u>"
private val ESCAPABLE_CHARS = setOf('\\', '*', '_', '~', '`', '<')

/** 行内样式与 markdown 标记的双向转换 */
object InlineMarkdown {

    /**
     * 解析结果。
     * @param plain 纯文本 + 纯文本坐标的样式区间（写回模型用）
     * @param markerRanges 源码坐标中标记符号占据的区间（VisualTransformation 置灰用）
     * @param sourceSpans 源码坐标中样式作用的区间，不含标记（VisualTransformation 着色用）
     */
    data class ParseResult(
        val plain: RichText,
        val markerRanges: List<IntRange>,
        val sourceSpans: List<StyleRange>,
    )

    fun openToken(style: InlineStyle): String = when (style) {
        InlineStyle.Bold -> "**"
        InlineStyle.Italic -> "_"
        InlineStyle.Strikethrough -> "~~"
        InlineStyle.Underline -> UNDERLINE_OPEN
        InlineStyle.Code -> "`"
    }

    fun closeToken(style: InlineStyle): String =
        if (style == InlineStyle.Underline) UNDERLINE_CLOSE else openToken(style)

    /** 块正文 -> 行内 markdown 源码。栈式开闭标记保证嵌套合法。 */
    fun serialize(rich: RichText): String {
        val n = rich.text.length
        val spans = normalizeSpans(rich.spans, n)
        if (spans.isEmpty()) return escapeText(rich.text)

        val points = (spans.flatMap { listOf(it.start, it.end) } + listOf(0, n))
            .distinct().sorted()
        val sb = StringBuilder()
        val stack = ArrayDeque<InlineStyle>()
        for (i in 0 until points.size - 1) {
            val a = points[i]
            val b = points[i + 1]
            if (a == b) continue
            val active = spans.filter { it.start <= a && it.end >= b }.map { it.style }.toSet()
            if (stack.any { it !in active }) {
                val reopen = mutableListOf<InlineStyle>()
                while (stack.any { it !in active }) {
                    val top = stack.removeLast()
                    sb.append(closeToken(top))
                    if (top in active) reopen.add(top)
                }
                for (s in reopen.reversed()) {
                    sb.append(openToken(s))
                    stack.addLast(s)
                }
            }
            for (s in (active - stack.toSet()).sortedBy { it.ordinal }) {
                sb.append(openToken(s))
                stack.addLast(s)
            }
            sb.append(escapeText(rich.text.substring(a, b)))
        }
        while (stack.isNotEmpty()) sb.append(closeToken(stack.removeLast()))
        return sb.toString()
    }

    /** 行内 markdown 源码 -> 纯文本 + 样式区间。未配对标记按字面文本处理。 */
    fun parse(source: String): ParseResult {
        // 1. 扫描所有候选 token（长 token 优先）
        val candidates = listOf(
            UNDERLINE_CLOSE to (InlineStyle.Underline to true),
            UNDERLINE_OPEN to (InlineStyle.Underline to false),
            "**" to (InlineStyle.Bold to false),
            "~~" to (InlineStyle.Strikethrough to false),
            "_" to (InlineStyle.Italic to false),
            "`" to (InlineStyle.Code to false),
        )
        val toks = mutableListOf<Tok>()
        var i = 0
        while (i < source.length) {
            val hit = if (isEscaped(source, i)) {
                null
            } else {
                candidates.firstOrNull { source.startsWith(it.first, i) }
            }
            if (hit != null) {
                toks.add(Tok(i, hit.first.length, hit.second.first, hit.second.second))
                i += hit.first.length
            } else {
                i++
            }
        }

        // 2. 配对。code 优先且互斥：code 区间内其他 token 作废
        val paired = mutableListOf<Pair<Tok, Tok>>()
        val open = mutableMapOf<InlineStyle, Tok>()
        var codeOpen: Tok? = null
        for (t in toks) {
            if (codeOpen != null) {
                if (t.style == InlineStyle.Code) {
                    paired.add(codeOpen to t)
                    codeOpen = null
                }
                continue
            }
            when {
                t.style == InlineStyle.Code -> codeOpen = t
                t.style == InlineStyle.Underline ->
                    if (t.isClose) {
                        open.remove(InlineStyle.Underline)?.let { paired.add(it to t) }
                    } else {
                        open[InlineStyle.Underline] = t
                    }
                else -> {
                    val o = open.remove(t.style)
                    if (o != null) paired.add(o to t) else open[t.style] = t
                }
            }
        }

        // 3. 重建纯文本，仅跳过已配对的标记 token
        val markerToks = paired.flatMap { listOf(it.first, it.second) }.sortedBy { it.pos }
        val sb = StringBuilder()
        val plainPosOf = mutableMapOf<Int, Int>()
        var m = 0
        i = 0
        while (i < source.length) {
            if (m < markerToks.size && markerToks[m].pos == i) {
                plainPosOf[i] = sb.length
                i += markerToks[m].len
                m++
            } else if (
                source[i] == '\\' &&
                i + 1 < source.length &&
                source[i + 1] in ESCAPABLE_CHARS
            ) {
                sb.append(source[i + 1])
                i += 2
            } else {
                sb.append(source[i])
                i++
            }
        }

        val plainSpans = paired.map { (o, c) ->
            StyleRange(plainPosOf.getValue(o.pos), plainPosOf.getValue(c.pos), o.style)
        }
        val sourceSpans = paired.map { (o, c) -> StyleRange(o.pos + o.len, c.pos, o.style) }
        val markerRanges = markerToks.map { it.pos until (it.pos + it.len) }
        return ParseResult(
            plain = RichText(sb.toString(), normalizeSpans(plainSpans, sb.length)),
            markerRanges = markerRanges,
            sourceSpans = sourceSpans,
        )
    }

    private fun escapeText(text: String): String = buildString(text.length) {
        for (ch in text) {
            if (ch in ESCAPABLE_CHARS) append('\\')
            append(ch)
        }
    }

    private fun isEscaped(source: String, pos: Int): Boolean {
        var slashCount = 0
        var i = pos - 1
        while (i >= 0 && source[i] == '\\') {
            slashCount++
            i--
        }
        return slashCount % 2 == 1
    }

    private data class Tok(val pos: Int, val len: Int, val style: InlineStyle, val isClose: Boolean)
}
