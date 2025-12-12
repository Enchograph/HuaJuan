package com.chenhongyu.huajuan.render

import android.content.Context
import android.graphics.Bitmap
import com.chenhongyu.huajuan.storage.ImageSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object HtmlRenderAndSave {
    /**
     * Render the provided template (HTML string) with provided data and save the resulting image
     * into app files dir. Returns the saved absolute file path.
     */
    suspend fun renderTemplateToFile(
        context: Context,
        templateHtml: String,
        data: Map<String, String>,
        outputFileName: String = "ai_render_${System.currentTimeMillis()}.png",
        targetWidthPx: Int
    ): String {
        // Fill template
        val finalHtml = HtmlTemplateFiller.fillTemplate(templateHtml, data)

        // Render to bitmap on main/UI thread
        val bitmap: Bitmap = HtmlRenderer.renderHtmlToBitmap(context, finalHtml, targetWidthPx)

        // Save bitmap to filesDir (IO)
        return withContext(Dispatchers.IO) {
            ImageSaver.saveBitmapToFilesDir(context, bitmap, outputFileName)
        }
    }
}

