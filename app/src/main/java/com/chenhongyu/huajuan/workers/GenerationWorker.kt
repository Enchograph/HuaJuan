package com.chenhongyu.huajuan.workers

import android.content.Context
import com.chenhongyu.huajuan.data.AppDatabase
import com.chenhongyu.huajuan.data.HtmlTemplateEngine
import com.chenhongyu.huajuan.data.HtmlToBitmapRenderer
import com.chenhongyu.huajuan.render.HtmlRenderer
import com.chenhongyu.huajuan.data.ImageStorage
import java.io.BufferedReader
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Simple generation manager: suspend function that runs the generation pipeline for an AI creation id.
 * This can later be invoked from a WorkManager worker or from UI code.
 */
object GenerationManager {
    suspend fun generateForId(context: Context, id: String): Boolean {
        val db = AppDatabase.getDatabase(context)
        val dao = db.aiCreationDao()

        // Load entity on IO thread
        val entity = withContext(Dispatchers.IO) { dao.getCreationById(id) } ?: return false

        val now = System.currentTimeMillis()
        val updating = entity.copy(status = "GENERATING", updatedAt = now)
        // persist status change on IO
        withContext(Dispatchers.IO) { dao.updateCreation(updating) }

        return try {
            // If promptHtml is empty, pick a random template from assets (asset IO on Dispatchers.IO)
            val html = if (entity.promptHtml.isBlank()) {
                val templates = withContext(Dispatchers.IO) { listAssetFiles(context, "ai_templates") }
                val chosen = if (templates.isNotEmpty()) templates[Random.nextInt(templates.size)] else null
                chosen?.let { withContext(Dispatchers.IO) { loadAssetAsString(context, "ai_templates/$it") } } ?: "<html><body><pre>${entity.promptJson ?: ""}</pre></body></html>"
            } else HtmlTemplateEngine.apply(entity.promptHtml, entity.promptJson)

            val finalHtml = if (entity.promptHtml.isBlank()) {
                // apply template replacements
                val tpl = html
                val payload = "{" +
                        "\"title\": \"${escapeForJson(entity.username ?: "AI 创作")}\"," +
                        "\"username\": \"${escapeForJson(entity.username ?: "用户")}\"," +
                        "\"content\": ${jsonStringOrLiteral(entity.promptJson)}," +
                        "\"aiRoleName\": \"${escapeForJson(entity.aiRoleName ?: "助手")}\"," +
                        "\"aiModelName\": \"${escapeForJson(entity.aiModelName ?: "模型")}\"" +
                        "}"
                HtmlTemplateEngine.apply(tpl, payload)
            } else html

            // First attempt: JS-enabled renderer (more feature-complete). If it fails (renderer crash/timeouts), fall back.
            val bitmap = try {
                HtmlRenderer.renderHtmlToBitmap(context, finalHtml, entity.width)
            } catch (primaryEx: Exception) {
                // log and fallback to non-JS renderer
                primaryEx.printStackTrace()
                // estimate height: use entity.height if set, else cap
                val h = if (entity.height > 0) entity.height else (entity.width * 1) // conservative fallback
                withContext(Dispatchers.Main) {
                    // HtmlToBitmapRenderer requires main thread for WebView ops internally
                    HtmlToBitmapRenderer.render(context, finalHtml, entity.width, h)
                }
            }

            // Save bitmap and thumbnail on IO thread
            val imagePath = withContext(Dispatchers.IO) { ImageStorage.saveBitmap(context, bitmap) }
            val thumbPath = withContext(Dispatchers.IO) { ImageStorage.saveThumbnail(context, bitmap, imagePath) }

            val done = updating.copy(status = "DONE", imageFileName = imagePath, updatedAt = System.currentTimeMillis(), extraJson = "{\"thumb\": \"$thumbPath\"}")
            withContext(Dispatchers.IO) { dao.updateCreation(done) }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            // persist failure on IO
            withContext(Dispatchers.IO) {
                val failed = entity.copy(status = "FAILED", updatedAt = System.currentTimeMillis(), extraJson = "{\"error\": \"${e.message}\"}")
                dao.updateCreation(failed)
            }
            false
        }
    }

    private fun listAssetFiles(context: Context, path: String): List<String> {
        return try {
            context.assets.list(path)?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun loadAssetAsString(context: Context, assetPath: String): String {
        return context.assets.open(assetPath).bufferedReader().use(BufferedReader::readText)
    }

    private fun escapeForJson(s: String): String {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
    }

    private fun jsonStringOrLiteral(json: String?): String {
        if (json == null) return "\"\""
        // if json starts with { or [ treat as literal, else quote it
        val t = json.trimStart()
        return if (t.startsWith("{") || t.startsWith("[")) json else "\"${escapeForJson(json)}\""
    }
}
