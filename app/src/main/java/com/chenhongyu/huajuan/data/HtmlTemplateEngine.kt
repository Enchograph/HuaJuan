package com.chenhongyu.huajuan.data

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject

object HtmlTemplateEngine {
    private val gson = Gson()

    /**
     * Very small template engine: replaces {{key}} with corresponding value from jsonPayload.
     * jsonPayload is a JSON string representing an object. If missing, empty string is used.
     */
    fun apply(template: String, jsonPayload: String?): String {
        if (jsonPayload.isNullOrBlank()) return template
        return try {
            val tree: JsonElement = gson.fromJson(jsonPayload, JsonElement::class.java)
            val obj = if (tree.isJsonObject) tree.asJsonObject else JsonObject()
            var out = template
            // replace simple {{key}} tokens
            val regex = "\\{\\{([a-zA-Z0-9_.]+)\\}\\}".toRegex()
            out = regex.replace(out) { match ->
                val key = match.groupValues[1]
                resolvePath(obj, key) ?: ""
            }
            out
        } catch (e: Exception) {
            template
        }
    }

    private fun resolvePath(obj: JsonObject, path: String): String? {
        val parts = path.split('.')
        var cur: JsonElement? = obj
        for (p in parts) {
            if (cur == null || !cur.isJsonObject) return null
            val jo = cur.asJsonObject
            if (!jo.has(p)) return null
            cur = jo.get(p)
        }
        return if (cur != null && cur.isJsonPrimitive) cur.asString else cur?.toString()
    }
}

