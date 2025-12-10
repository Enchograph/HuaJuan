package com.chenhongyu.huajuan.workers

import android.content.Context
import com.chenhongyu.huajuan.data.AppDatabase
import com.chenhongyu.huajuan.data.HtmlTemplateEngine
import com.chenhongyu.huajuan.render.UnsplashFetcher
import com.chenhongyu.huajuan.data.HtmlToBitmapRenderer
import com.chenhongyu.huajuan.render.HtmlRenderer
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
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
            // New simple generation: fetch a random portrait from Unsplash
            val bitmap: Bitmap = try {
                withContext(Dispatchers.IO) { UnsplashFetcher.fetchRandomPortraitBitmap(context, entity.width) }
            } catch (e: Exception) {
                // fallback to HTML renderer pipeline if Unsplash fetch fails
                e.printStackTrace()
                val html = if (entity.promptHtml.isBlank()) {
                    val templates = withContext(Dispatchers.IO) { listAssetFiles(context, "ai_templates") }
                    val chosen = if (templates.isNotEmpty()) templates[Random.nextInt(templates.size)] else null
                    chosen?.let { withContext(Dispatchers.IO) { loadAssetAsString(context, "ai_templates/$it") } } ?: "<html><body><pre>${entity.promptJson ?: ""}</pre></body></html>"
                } else HtmlTemplateEngine.apply(entity.promptHtml, entity.promptJson)

                val finalHtml = if (entity.promptHtml.isBlank()) {
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

                // Try HTML renderers; if both fail, create placeholder
                try {
                    HtmlRenderer.renderHtmlToBitmap(context, finalHtml, entity.width)
                } catch (primaryEx: Exception) {
                    primaryEx.printStackTrace()
                    try {
                        val h = if (entity.height > 0) entity.height else (entity.width * 1)
                        withContext(Dispatchers.Main) {
                            HtmlToBitmapRenderer.render(context, finalHtml, entity.width, h)
                        }
                    } catch (secondaryEx: Exception) {
                        secondaryEx.printStackTrace()
                        // final fallback: placeholder bitmap with title text
                        createPlaceholderBitmap(entity.width, entity.height.takeIf { it > 0 } ?: (entity.width * 16 / 9), entity.title ?: "AI 创作")
                    }
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

    private fun createPlaceholderBitmap(width: Int, height: Int, title: String): Bitmap {
        val w = width.coerceAtLeast(200)
        val h = height.coerceAtLeast(320)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.parseColor("#F4F1EA"))
        val paint = Paint().apply {
            color = Color.parseColor("#2C2C2C")
            textSize = (w / 14).toFloat()
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        // simple center text
        val x = (w / 10).toFloat()
        val y = (h / 2).toFloat()
        canvas.drawText(title, x, y, paint)
        return bmp
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
