package com.wim.markdown.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.wim.markdown.model.InlineStyle
import com.wim.markdown.model.RichText
import com.wim.markdown.model.normalizeSpans

internal fun InlineStyle.toSpanStyle(codeBackground: Color): SpanStyle = when (this) {
    InlineStyle.Bold -> SpanStyle(fontWeight = FontWeight.Bold)
    InlineStyle.Italic -> SpanStyle(fontStyle = FontStyle.Italic)
    InlineStyle.Strikethrough -> SpanStyle(textDecoration = TextDecoration.LineThrough)
    InlineStyle.Underline -> SpanStyle(textDecoration = TextDecoration.Underline)
    InlineStyle.Code -> SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground)
}

internal fun RichText.toAnnotatedString(codeBackground: Color): AnnotatedString =
    buildAnnotatedString {
        append(text)
        normalizeSpans(spans, text.length).forEach { r ->
            addStyle(r.style.toSpanStyle(codeBackground), r.start, r.end)
        }
    }
