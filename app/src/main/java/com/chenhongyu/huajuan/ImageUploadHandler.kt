package com.chenhongyu.huajuan

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.chenhongyu.huajuan.data.Repository
import com.chenhongyu.huajuan.data.Message
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Date
import java.util.UUID

/**
 * 图片上传处理器
 */
class ImageUploadHandler(
    private val context: Context,
    private val repository: Repository
) {
    // 选中的图片URI列表
    private val _selectedImages = mutableStateOf<List<String>>(emptyList())
    val selectedImages by _selectedImages

    // 添加选中的图片
    fun addSelectedImage(uri: String) {
        _selectedImages.value = _selectedImages.value + uri
    }

    // 移除选中的图片
    fun removeSelectedImage(uri: String) {
        _selectedImages.value = _selectedImages.value - uri
    }

    // 清空选中的图片
    fun clearSelectedImages() {
        _selectedImages.value = emptyList()
    }

    // 将图片URI转换为文件路径
    fun getRealPathFromUri(uri: Uri): String? {
        return try {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndex(MediaStore.Images.Media.DATA)
                    if (columnIndex >= 0) {
                        return@use it.getString(columnIndex)
                    }
                }
            }
            // 如果无法通过MediaStore获取路径，复制文件到应用私有目录
            copyFileToInternalStorage(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 复制文件到内部存储
    private fun copyFileToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val outputFile = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(outputFile)

            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            outputFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 发送包含图片的消息
    suspend fun sendImageMessage(
        conversationId: String?,
        text: String,
        imageUris: List<String>,
        onMessageAdded: (Message) -> Unit
    ) {
        if (conversationId == null) return

        // 创建用户消息，包含图片信息
        val userMessage = Message(
            id = UUID.randomUUID().toString(),
            text = text,
            isUser = true,
            timestamp = Date(),
            imageUris = imageUris // 假设Message类支持imageUris字段
        )

        // 更新聊天状态
        val currentMessages = repository.getMessages(conversationId)
        val updatedMessages = currentMessages + userMessage
        repository.saveMessages(conversationId, updatedMessages)
        onMessageAdded(userMessage)

        // 更新对话列表中的最后消息
        repository.updateLastMessage(conversationId, text)

        // 调用AI API获取回复
        // 这里需要根据具体的AI API实现来处理图片
        // 伪代码：调用带图片的AI API
        // repository.streamAIResponseWithImages(userMessagesOnly, imageUris, currentConversationId)
    }
}

/**
 * 扩展Message类以支持图片URI
 */
data class MessageWithImages(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Date,
    val imageUris: List<String> = emptyList(), // 添加图片URI列表
    val showThink: Boolean = false
)

/**
 * Composable函数用于管理图片选择状态
 */
@Composable
fun rememberImageUploadHandler(
    context: Context = androidx.compose.ui.platform.LocalContext.current,
    repository: Repository
): ImageUploadHandler {
    return androidx.compose.runtime.remember {
        ImageUploadHandler(context, repository)
    }
}