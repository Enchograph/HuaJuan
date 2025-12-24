package com.chenhongyu.huajuan.render

import org.json.JSONObject
import com.chenhongyu.huajuan.utils.ThinkTagProcessor

object HtmlTemplateFiller {
    /**
     * Produce final HTML by appending a script that fills DOM elements defined in the template.
     * The template is expected to contain elements with ids: date-render, user-name, user-sig,
     * ai-name, ai-model, ai-prompt, user-msg, ai-msg. The function will append a small script
     * that sets these elements using the provided data map.
     */
    fun fillTemplate(templateHtml: String, data: Map<String, String>): String {
        // Process think tags in the content
        val processedData = data.mapValues { entry ->
            when (entry.key) {
                "userContent", "aiContent" -> ThinkTagProcessor.processThinkTags(entry.value)
                else -> entry.value
            }
        }
        val json = JSONObject(processedData as Map<*, *>).toString()

        val script = buildString {
            append("<script>\n")
            append("(function(){\n")
            append("  try {\n")
            append("    var data = ")
            append(json)
            append(";\n")
            // set text content
            append("    if(document.getElementById('date-render')) document.getElementById('date-render').innerText = data.time || '';\n")
            append("    if(document.getElementById('user-name')) document.getElementById('user-name').innerText = data.userName || '';\n")
            append("    if(document.getElementById('user-sig')) document.getElementById('user-sig').innerText = data.userSig || '';\n")
            append("    if(document.getElementById('ai-name')) document.getElementById('ai-name').innerText = data.aiName || '';\n")
            append("    if(document.getElementById('ai-model')) document.getElementById('ai-model').innerText = 'Model: ' + (data.aiModel || '');\n")
            append("    if(document.getElementById('ai-prompt')) document.getElementById('ai-prompt').innerText = data.aiPrompt || '';\n")
            // markdown rendering using marked (template includes marked)
            append("    if(window.marked) {\n")
            append("      if(document.getElementById('user-msg')) document.getElementById('user-msg').innerHTML = marked.parse(data.userContent || '');\n")
            append("      if(document.getElementById('ai-msg')) document.getElementById('ai-msg').innerHTML = marked.parse(data.aiContent || '');\n")
            append("    } else {\n")
            append("      if(document.getElementById('user-msg')) document.getElementById('user-msg').innerText = data.userContent || '';\n")
            append("      if(document.getElementById('ai-msg')) document.getElementById('ai-msg').innerText = data.aiContent || '';\n")
            append("    }\n")
            append("  } catch (e) { console.error('Template filler error', e); }\n")
            append("})();\n")
            append("</script>\n")
        }

        // insert script before closing </body> if present, otherwise append
        val idx = templateHtml.lastIndexOf("</body>")
        return if (idx >= 0) {
            templateHtml.substring(0, idx) + script + templateHtml.substring(idx)
        } else {
            templateHtml + script
        }
    }
}

