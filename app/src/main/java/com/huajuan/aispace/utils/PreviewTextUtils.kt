package com.huajuan.aispace.utils

/**
 * Preview text helpers (e.g. conversation list subtitle).
 */
object PreviewTextUtils {
    fun stripMarkdown(text: String): String {
        if (text.isBlank()) return ""
        var result = text
        // Fenced code blocks
        result = result.replace(Regex("(?s)```.*?```"), " ")
        // Inline code
        result = result.replace(Regex("`([^`]*)`"), "$1")
        // Images: ![alt](url) -> alt
        result = result.replace(Regex("!\\[([^\\]]*)\\]\\([^)]*\\)"), "$1")
        // Links: [text](url) -> text
        result = result.replace(Regex("\\[([^\\]]+)\\]\\([^)]*\\)"), "$1")
        // Headings
        result = result.replace(Regex("(?m)^\\s{0,3}#{1,6}\\s*"), "")
        // Blockquotes
        result = result.replace(Regex("(?m)^\\s{0,3}>\\s?"), "")
        // List markers
        result = result.replace(Regex("(?m)^\\s*([-*+]|\\d+\\.)\\s+"), "")
        // Horizontal rules
        result = result.replace(Regex("(?m)^\\s*([-*_])\\1\\1+\\s*$"), " ")
        // Emphasis
        result = result.replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
        result = result.replace(Regex("__([^_]+)__"), "$1")
        result = result.replace(Regex("\\*([^*]+)\\*"), "$1")
        result = result.replace(Regex("_([^_]+)_"), "$1")

        return result.replace(Regex("\\s+"), " ").trim()
    }
}
