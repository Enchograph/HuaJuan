package com.huajuan.aispace.screens.chat.upload
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.huajuan.aispace.R
import com.huajuan.aispace.data.Message
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*

private const val TAG = "ImageUploadFeature"

/**
 * 图片上传和AI交互功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageUploadFeature(
    onImagesSelected: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedImages by remember { mutableStateOf<List<String>>(emptyList()) }
    
    // 使用ActivityResultLauncher来选择多张图片
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        val imagePaths = uris.mapNotNull { uri ->
            getRealPathFromUri(context, uri)
        }
        selectedImages = imagePaths
        onImagesSelected(imagePaths)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 选择图片按钮
        Button(
            onClick = {
                imagePickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.chat_album))
        }

        // 显示选中的图片
        if (selectedImages.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectedImages) { imagePath ->
                    AsyncImage(
                        model = imagePath,
                        contentDescription = stringResource(R.string.chat_selected_image),
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    }
}

/**
 * 从URI获取真实路径
 */
fun getRealPathFromUri(context: Context, uri: Uri): String? {
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
        copyFileToInternalStorage(context, uri)
    } catch (e: Exception) {
        Log.e(TAG, "getRealPathFromUri failed", e)
        null
    }
}

/**
 * 复制文件到内部存储
 */
private fun copyFileToInternalStorage(context: Context, uri: Uri): String? {
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
        Log.e(TAG, "copyFileToInternalStorage failed", e)
        null
    }
}

/**
 * 扩展函数：在Message中添加图片
 */
fun Message.withImages(imageUris: List<String>): Message {
    return this.copy(imageUris = imageUris)
}

/**
 * 创建带图片的消息
 */
fun createImageMessage(text: String, imageUris: List<String>, isUser: Boolean = true): Message {
    return Message(
        text = text,
        isUser = isUser,
        timestamp = Date(),
        imageUris = imageUris
    )
}
