package com.huajuan.aispace.utils

import com.huajuan.aispace.HuaJuanApplication
import com.huajuan.aispace.R
import com.huajuan.aispace.i18n.LocalizedResources
import kotlin.math.min

/**
 * Think标签处理器
 * 用于处理AI模型输出中的think标签，支持顺序分段渲染。
 */
object ThinkTagProcessor {

    sealed class RenderSegment {
        data class TextSegment(val text: String) : RenderSegment()

        data class ThinkSegment(
            val index: Int,
            val label: String,
            val content: String,
            val isClosed: Boolean
        ) : RenderSegment()
    }

    private data class TagDef(val tag: String, val labelRes: Int)

    private data class StartMatch(
        val type: Kind,
        val start: Int,
        val end: Int,
        val tag: String,
        val label: String
    ) {
        enum class Kind { TAG, FENCE }
    }

    private val tagDefs = listOf(
        TagDef("think", R.string.think_label_deep),
        TagDef("analysis", R.string.think_label_analysis),
        TagDef("reasoning", R.string.think_label_reasoning),
        TagDef("thinking", R.string.think_label_thinking),
        TagDef("thought", R.string.think_label_thinking),
        TagDef("scratchpad", R.string.think_label_scratchpad)
    )

    private val fencedDefs = listOf(
        TagDef("think", R.string.think_label_deep),
        TagDef("thinking", R.string.think_label_thinking),
        TagDef("analysis", R.string.think_label_analysis),
        TagDef("reasoning", R.string.think_label_reasoning)
    )

    private val tagLabelMap: Map<String, Int> = tagDefs.associate { it.tag to it.labelRes }
    private val fencedLabelMap: Map<String, Int> = fencedDefs.associate { it.tag to it.labelRes }

    private val tagOpenPattern = Regex(
        pattern = "(?is)(<|&lt;)\\s*(think|analysis|reasoning|thinking|thought|scratchpad)\\b[^>]*?(>|&gt;)",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val fencedOpenPattern = Regex(
        pattern = "```\\s*(think|thinking|analysis|reasoning)\\s*(?:\\r?\\n)?",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    fun splitToSegments(content: String): List<RenderSegment> {
        if (content.isEmpty()) return emptyList()

        val segments = mutableListOf<RenderSegment>()
        var cursor = 0
        var thinkIndex = 0

        while (cursor < content.length) {
            val next = findNextStart(content, cursor)
            if (next == null) {
                appendTextSegment(segments, content.substring(cursor))
                break
            }

            if (next.start > cursor) {
                appendTextSegment(segments, content.substring(cursor, next.start))
            }

            when (next.type) {
                StartMatch.Kind.TAG -> {
                    val closeMatch = findTagClose(content, next.tag, next.end)
                    if (closeMatch != null) {
                        val thinkText = content.substring(next.end, closeMatch.range.first).trim()
                        segments.add(
                            RenderSegment.ThinkSegment(
                                index = thinkIndex++,
                                label = next.label,
                                content = thinkText,
                                isClosed = true
                            )
                        )
                        cursor = closeMatch.range.last + 1
                    } else {
                        val thinkText = content.substring(next.end).trim()
                        segments.add(
                            RenderSegment.ThinkSegment(
                                index = thinkIndex++,
                                label = next.label,
                                content = thinkText,
                                isClosed = false
                            )
                        )
                        cursor = content.length
                    }
                }

                StartMatch.Kind.FENCE -> {
                    val fenceCloseIndex = content.indexOf("```", startIndex = next.end)
                    if (fenceCloseIndex >= 0) {
                        val thinkText = content.substring(next.end, fenceCloseIndex).trim()
                        segments.add(
                            RenderSegment.ThinkSegment(
                                index = thinkIndex++,
                                label = next.label,
                                content = thinkText,
                                isClosed = true
                            )
                        )
                        cursor = min(content.length, fenceCloseIndex + 3)
                    } else {
                        val thinkText = content.substring(next.end).trim()
                        segments.add(
                            RenderSegment.ThinkSegment(
                                index = thinkIndex++,
                                label = next.label,
                                content = thinkText,
                                isClosed = false
                            )
                        )
                        cursor = content.length
                    }
                }
            }
        }

        return segments
    }

    /**
     * 兼容旧调用：显示则保留think内容，隐藏则移除think内容。
     */
    fun processThinkTags(content: String, showThink: Boolean = false): String {
        return if (showThink) stripThinkTags(content) else removeThinkTags(content)
    }

    fun containsThinkTag(content: String): Boolean {
        return splitToSegments(content).any { it is RenderSegment.ThinkSegment }
    }

    fun extractThinkContent(content: String): List<String> {
        return splitToSegments(content)
            .filterIsInstance<RenderSegment.ThinkSegment>()
            .map { it.content }
    }

    fun removeThinkTags(content: String): String {
        return buildString {
            splitToSegments(content).forEach { segment ->
                if (segment is RenderSegment.TextSegment) {
                    append(segment.text)
                }
            }
        }
    }

    fun stripThinkTags(content: String): String {
        return buildString {
            splitToSegments(content).forEach { segment ->
                when (segment) {
                    is RenderSegment.TextSegment -> append(segment.text)
                    is RenderSegment.ThinkSegment -> append(segment.content)
                }
            }
        }
    }

    fun firstTextSegment(content: String): String {
        val segment = splitToSegments(content)
            .firstOrNull { it is RenderSegment.TextSegment && it.text.isNotBlank() }
        return if (segment is RenderSegment.TextSegment) segment.text else ""
    }

    fun replaceThinkTime(content: String, timeElapsed: Int): String {
        val localizedPlaceholder = localized(R.string.think_time_placeholder)
        val localizedValue = localized(R.string.think_time_seconds, timeElapsed)
        return content
            .replace("\u0078\u0078\u79d2", "${timeElapsed}\u79d2")
            .replace("xxs", "${timeElapsed}s")
            .replace(localizedPlaceholder, localizedValue)
    }

    private fun findNextStart(content: String, from: Int): StartMatch? {
        val tagMatch = tagOpenPattern.find(content, from)
        val fenceMatch = fencedOpenPattern.find(content, from)

        val tagStart = tagMatch?.range?.first ?: Int.MAX_VALUE
        val fenceStart = fenceMatch?.range?.first ?: Int.MAX_VALUE

        if (tagStart == Int.MAX_VALUE && fenceStart == Int.MAX_VALUE) return null

        return if (tagStart <= fenceStart) {
            val tag = tagMatch!!.groups[2]!!.value.lowercase()
            StartMatch(
                type = StartMatch.Kind.TAG,
                start = tagMatch.range.first,
                end = tagMatch.range.last + 1,
                tag = tag,
                label = localized(tagLabelMap[tag] ?: R.string.think_label_thinking)
            )
        } else {
            val tag = fenceMatch!!.groups[1]!!.value.lowercase()
            StartMatch(
                type = StartMatch.Kind.FENCE,
                start = fenceMatch.range.first,
                end = fenceMatch.range.last + 1,
                tag = tag,
                label = localized(fencedLabelMap[tag] ?: R.string.think_label_thinking)
            )
        }
    }

    private fun appendTextSegment(segments: MutableList<RenderSegment>, text: String) {
        if (text.isEmpty()) return
        val last = segments.lastOrNull()
        if (last is RenderSegment.TextSegment) {
            segments[segments.lastIndex] = RenderSegment.TextSegment(last.text + text)
        } else {
            segments.add(RenderSegment.TextSegment(text))
        }
    }

    private fun findTagClose(source: String, tag: String, startIndex: Int): MatchResult? {
        val closePattern = Regex(
            pattern = "(?is)(<|&lt;)\\s*/\\s*${Regex.escape(tag)}\\s*(>|&gt;)",
            options = setOf(RegexOption.IGNORE_CASE)
        )
        return closePattern.find(source, startIndex)
    }

    private fun localized(resId: Int, vararg args: Any): String =
        LocalizedResources.getString(HuaJuanApplication.instance, resId, null, *args)
}
