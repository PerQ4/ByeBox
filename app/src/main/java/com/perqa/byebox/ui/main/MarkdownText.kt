package com.perqa.byebox.ui.main

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Document
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.HtmlBlock
import org.commonmark.node.HtmlInline
import org.commonmark.node.Image
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.Text as MdText
import org.commonmark.node.ThematicBreak
import org.commonmark.parser.Parser

/**
 * Лёгкий markdown-рендер для текста подписок (description, announce).
 *
 * Парсит markdown библиотекой commonmark (без тяжёлого Compose Multiplatform
 * рендерера) и собирает [AnnotatedString] со стилями: жирный, курсив,
 * зачёркнутый, моноширинный код, ссылки, заголовки, списки и цитаты.
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val annotated = remember(text, style.fontSize, linkColor) {
        markdownToAnnotated(text, style, linkColor)
    }
    Text(
        text = annotated,
        modifier = modifier,
        style = style,
        color = style.color
    )
}

/**
 * Преобразует markdown-строку в [AnnotatedString]. Если текст не является
 * валидным markdown, возвращается исходный текст без изменений.
 */
fun markdownToAnnotated(
    text: String,
    style: TextStyle = TextStyle.Default,
    linkColor: Color = Color.Unspecified
): AnnotatedString {
    if (text.isBlank()) return AnnotatedString(text)
    val parser = Parser.builder()
        .extensions(listOf(StrikethroughExtension.create()))
        .build()
    val document = try {
        parser.parse(text)
    } catch (t: Throwable) {
        return AnnotatedString(text)
    }

    val builder = AnnotatedString.Builder()
    var firstBlock = true
    getChildren(document).forEach { block ->
        if (!firstBlock) {
            builder.append("\n\n")
        }
        firstBlock = false
        renderBlock(block, builder, style, linkColor, indentLevel = 0)
    }
    return builder.toAnnotatedString()
}

/** Дочерние узлы AST commonmark (Node — связный список через firstChild/next). */
private fun getChildren(node: Node): List<Node> {
    val result = mutableListOf<Node>()
    var child = node.firstChild
    while (child != null) {
        result.add(child)
        child = child.next
    }
    return result
}

private fun renderBlock(
    node: Node,
    builder: AnnotatedString.Builder,
    style: TextStyle,
    linkColor: Color,
    indentLevel: Int
) {
    when (node) {
        is Paragraph -> renderChildren(node, builder, style, linkColor)
        is Heading -> {
            val size: TextUnit = when (node.level) {
                1 -> (style.fontSize.value * 1.3f).sp
                2 -> (style.fontSize.value * 1.15f).sp
                3 -> style.fontSize
                else -> style.fontSize
            }
            builder.pushStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = size,
                    color = style.color
                )
            )
            renderChildren(node, builder, style, linkColor)
            builder.pop()
        }
        is BulletList -> {
            var firstItem = true
            for (child in getChildren(node)) {
                if (child is ListItem) {
                    if (!firstItem) builder.append("\n")
                    firstItem = false
                    builder.append("  ".repeat(indentLevel))
                    builder.append("\u2022 ") // •
                    renderChildren(child, builder, style, linkColor, indentLevel + 1)
                }
            }
        }
        is OrderedList -> {
            var number = node.startNumber
            var firstItem = true
            for (child in getChildren(node)) {
                if (child is ListItem) {
                    if (!firstItem) builder.append("\n")
                    firstItem = false
                    builder.append("  ".repeat(indentLevel))
                    builder.append("$number. ")
                    number++
                    renderChildren(child, builder, style, linkColor, indentLevel + 1)
                }
            }
        }
        is BlockQuote -> {
            builder.pushStyle(
                SpanStyle(
                    fontStyle = FontStyle.Italic,
                    color = style.color.copy(alpha = 0.8f)
                )
            )
            renderChildren(node, builder, style, linkColor)
            builder.pop()
        }
        is FencedCodeBlock -> {
            val codeStyle = SpanStyle(
                fontFamily = FontFamily.Monospace,
                background = style.color.copy(alpha = 0.07f),
                color = style.color
            )
            builder.pushStyle(codeStyle)
            builder.append(node.literal.orEmpty().trimEnd())
            builder.pop()
        }
        is IndentedCodeBlock -> {
            val codeStyle = SpanStyle(
                fontFamily = FontFamily.Monospace,
                background = style.color.copy(alpha = 0.07f),
                color = style.color
            )
            builder.pushStyle(codeStyle)
            builder.append(node.literal.orEmpty().trimEnd())
            builder.pop()
        }
        is ThematicBreak -> {
            builder.append("\u2500\u2500\u2500")
        }
        is HtmlBlock, is HtmlInline -> {
            // Игнорируем HTML — не рисуем небезопасное содержимое.
        }
        else -> {
            renderChildren(node, builder, style, linkColor)
        }
    }
}

private fun renderChildren(
    node: Node,
    builder: AnnotatedString.Builder,
    style: TextStyle,
    linkColor: Color,
    indentLevel: Int = 0
) {
    for (child in getChildren(node)) {
        renderInline(child, builder, style, linkColor, indentLevel)
    }
}

private fun renderInline(
    node: Node,
    builder: AnnotatedString.Builder,
    style: TextStyle,
    linkColor: Color,
    indentLevel: Int
) {
    when (node) {
        is MdText -> builder.append(node.literal)
        is Emphasis -> {
            builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
            renderChildren(node, builder, style, linkColor, indentLevel)
            builder.pop()
        }
        is org.commonmark.node.StrongEmphasis -> {
            builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            renderChildren(node, builder, style, linkColor, indentLevel)
            builder.pop()
        }
        is Strikethrough -> {
            builder.pushStyle(
                SpanStyle(textDecoration = TextDecoration.LineThrough)
            )
            renderChildren(node, builder, style, linkColor, indentLevel)
            builder.pop()
        }
        is Code -> {
            builder.pushStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = style.color.copy(alpha = 0.12f),
                    color = style.color
                )
            )
            builder.append(node.literal)
            builder.pop()
        }
        is Link -> {
            builder.pushStyle(
                SpanStyle(
                    color = linkColor,
                    textDecoration = TextDecoration.Underline
                )
            )
            renderChildren(node, builder, style, linkColor, indentLevel)
            builder.pop()
            if (!node.destination.isNullOrBlank()) {
                builder.append(" (${node.destination})")
            }
        }
        is Image -> {
            // Картинки в описаниях подставок не рисуем; показываем alt-текст из children узла.
            renderChildren(node, builder, style, linkColor, indentLevel)
        }
        is SoftLineBreak -> builder.append(" ")
        is HardLineBreak -> builder.append("\n")
        else -> renderChildren(node, builder, style, linkColor, indentLevel)
    }
}