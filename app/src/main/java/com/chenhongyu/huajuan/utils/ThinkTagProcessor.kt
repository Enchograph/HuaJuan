package com.chenhongyu.huajuan.utils

import java.util.regex.Pattern

/**
 * Think标签处理器
 * 用于处理AI模型输出中的think标签，支持显示/隐藏控制
 */
object ThinkTagProcessor {
    
    /**
     * 将think标签内容转换为可控制显示/隐藏的HTML结构
     * 
     * @param content 包含think标签的原始内容
     * @param showThink 是否显示think内容
     * @return 处理后的内容
     */
    fun processThinkTags(content: String, showThink: Boolean = false): String {
        // 使用正则表达式匹配think标签（使用您指定的think标签）
        val pattern = Pattern.compile("<think>(.*?)</think>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
        val matcher = pattern.matcher(content)
        
        // 使用StringBuffer来构建结果
        val result = StringBuffer()
        
        while (matcher.find()) {
            val thinkContent = matcher.group(1)?.trim() ?: ""
            
            // 计算思考时间 - 在实际应用中，这可能需要根据实际的AI响应时间来计算
            // 由于在渲染时无法知道确切的思考时间，我们使用占位符
            val timeElapsed = "xx秒"
            
            // 根据showThink参数决定think内容的显示方式
            val replacement = if (showThink) {
                "<details class=\"think-block\" open>" +
                "<summary>深度思考中，用时$timeElapsed (展开)</summary>" +
                "<div class=\"think-content\">$thinkContent</div>" +
                "</details>"
            } else {
                "<details class=\"think-block\" style=\"display:none;\">" +
                "<summary>深度思考中，用时$timeElapsed (已隐藏)</summary>" +
                "<div class=\"think-content\">$thinkContent</div>" +
                "</details>"
            }
            matcher.appendReplacement(result, replacement)
        }
        matcher.appendTail(result)
        
        return result.toString()
    }
    
    /**
     * 检查内容中是否包含think标签
     */
    fun containsThinkTag(content: String): Boolean {
        val pattern = Pattern.compile("<think>.*?</think>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
        return pattern.matcher(content).find()
    }
    
    /**
     * 提取think标签内的内容
     */
    fun extractThinkContent(content: String): List<String> {
        val pattern = Pattern.compile("<think>(.*?)</think>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
        val matcher = pattern.matcher(content)
        val results = mutableListOf<String>()
        
        while (matcher.find()) {
            results.add(matcher.group(1)?.trim() ?: "")
        }
        
        return results
    }
    
    /**
     * 移除所有think标签及其内容
     */
    fun removeThinkTags(content: String): String {
        val pattern = Pattern.compile("<think>.*?</think>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
        return pattern.matcher(content).replaceAll("")
    }
    
    /**
     * 仅移除think标签，保留内容
     */
    fun stripThinkTags(content: String): String {
        val pattern = Pattern.compile("</?think>", Pattern.CASE_INSENSITIVE)
        return pattern.matcher(content).replaceAll("")
    }
    
    /**
     * 替换think标签中的时间信息
     * 
     * @param content 包含think标签的内容
     * @param timeElapsed 思考用时（秒）
     * @return 替换时间后的内容
     */
    fun replaceThinkTime(content: String, timeElapsed: Int): String {
        // 使用正则表达式替换时间信息
        val pattern = Pattern.compile("<summary>深度思考中，用时[^<]* \\([^)]*\\)</summary>")
        val matcher = pattern.matcher(content)
        val result = StringBuffer()
        
        while (matcher.find()) {
            val replacement = "<summary>深度思考中，用时${timeElapsed}秒 (展开)</summary>"
            matcher.appendReplacement(result, replacement)
        }
        matcher.appendTail(result)
        
        return result.toString()
    }
}