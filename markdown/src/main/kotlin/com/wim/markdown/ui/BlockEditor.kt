package com.wim.markdown.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wim.markdown.MarkdownEditorMode
import com.wim.markdown.model.Block
import com.wim.markdown.model.contentOrNull
import com.wim.markdown.serializer.orderedNumber
import com.wim.markdown.state.MarkdownEditorState

private val TaskContentTopPadding = 12.dp

@Composable
internal fun BlockEditor(
    index: Int,
    block: Block,
    state: MarkdownEditorState,
    mode: MarkdownEditorMode,
    modifier: Modifier = Modifier,
    previousBlock: Block?,
    readOnly: Boolean,
    showTableActions: Boolean,
) {
    val spacing = LocalMarkdownEditorSpacing.current
    val topPadding = spacing.calculateTopSpacing(block, previousBlock)

    if (block is Block.Divider) {
        HorizontalDivider(modifier.padding(top = topPadding, bottom = spacing.dividerSpacing))
        return
    }
    if (block is Block.Table) {
        TableBlockEditor(
            index = index,
            table = block,
            state = state,
            modifier = modifier.padding(top = topPadding),
            readOnly = readOnly,
            showTableActions = showTableActions,
        )
        return
    }

    val textStyle = blockTextStyle(block)
    val contentTopPadding =
        if (block is Block.ListItem && block.checked != null) TaskContentTopPadding else 0.dp
    Row(
        modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(top = topPadding),
    ) {
        BlockPrefix(index, block, state, readOnly)
        Box(
            Modifier
                .weight(1f)
                .padding(top = contentTopPadding),
        ) {
            if (!readOnly && index == state.focusedIndex) {
                FocusedField(block, state, mode, textStyle)
            } else {
                UnfocusedText(index, block, state, textStyle, readOnly)
            }
        }
    }
}

@Composable
private fun blockTextStyle(block: Block): TextStyle {
    val typography = LocalMarkdownTypography.current
    val base = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
    return when (block) {
        is Block.Heading -> base.copy(
            fontSize = typography.headingSizes.getOrElse((block.level - 1)) { 15.sp },
            fontWeight = FontWeight.Bold,
        )
        is Block.ListItem -> base.copy(
            fontSize = typography.listItemSize,
            textDecoration = if (block.checked == true) {
                TextDecoration.LineThrough
            } else {
                TextDecoration.None
            },
        )
        is Block.Quote -> base.copy(fontSize = typography.quoteSize)
        else -> base.copy(fontSize = typography.paragraphSize)
    }
}

@Composable
private fun BlockPrefix(
    index: Int,
    block: Block,
    state: MarkdownEditorState,
    readOnly: Boolean,
) {
    when (block) {
        is Block.ListItem -> {
            val indent = (16 * block.indent).dp
            val label = when {
                block.ordered -> "${orderedNumber(state.blocks, index)}."
                block.checked == null -> "•"
                else -> null
            }
            label?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(
                        start = indent,
                        top = if (block.checked == null) 0.dp else TaskContentTopPadding,
                        end = if (block.checked == null) 8.dp else 4.dp,
                    ),
                )
            }
            block.checked?.let { checked ->
                Checkbox(
                    checked = checked,
                    onCheckedChange = { state.setTaskChecked(index, it) },
                    enabled = !readOnly,
                    modifier = Modifier
                        .padding(
                            start = if (label == null) indent else 0.dp,
                            end = 8.dp,
                        ),
                )
            }
        }
        is Block.Quote -> Box(
            Modifier
                .padding(end = 8.dp)
                .width(3.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary),
        )
        else -> Unit
    }
}

@Composable
private fun FocusedField(
    block: Block,
    state: MarkdownEditorState,
    mode: MarkdownEditorMode,
    textStyle: TextStyle,
) {
    val inline = mode == MarkdownEditorMode.INLINE_MARKDOWN
    val codeBg = MaterialTheme.colorScheme.surfaceVariant
    val markerColor = MaterialTheme.colorScheme.outline
    val focusRequester = remember { FocusRequester() }

    val value = if (inline) {
        TextFieldValue(state.inlineSource.orEmpty(), state.selection)
    } else {
        val content = block.contentOrNull() ?: return
        TextFieldValue(content.toAnnotatedString(codeBg), state.selection)
    }
    val transformation = if (inline) {
        remember(markerColor, codeBg) { InlineMarkdownVisualTransformation(markerColor, codeBg) }
    } else {
        VisualTransformation.None
    }

    BasicTextField(
        value = value,
        onValueChange = { if (inline) state.onInlineValueChange(it) else state.onRichValueChange(it) },
        textStyle = textStyle,
        visualTransformation = transformation,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (
                    event.type == KeyEventType.KeyDown &&
                    event.key == Key.Backspace &&
                    state.selection.collapsed &&
                    state.selection.start == 0
                ) {
                    state.onBackspaceAtStart()
                } else {
                    false
                }
            },
    )
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
private fun UnfocusedText(
    index: Int,
    block: Block,
    state: MarkdownEditorState,
    textStyle: TextStyle,
    readOnly: Boolean,
) {
    val content = block.contentOrNull() ?: return
    val codeBg = MaterialTheme.colorScheme.surfaceVariant
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text = content.toAnnotatedString(codeBg),
        style = textStyle,
        onTextLayout = { layout = it },
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(index, readOnly) {
                if (!readOnly) {
                    detectTapGestures { offset ->
                        val cursor = layout?.getOffsetForPosition(offset) ?: content.text.length
                        state.focusBlock(index, cursor)
                    }
                }
            },
    )
}
