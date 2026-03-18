package com.huajuan.aispace.screens.chat.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.ExperimentalFoundationApi
import com.huajuan.aispace.render.storage.ImageSaver
import com.huajuan.aispace.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Toast
import com.huajuan.aispace.R
import com.huajuan.aispace.data.Repository

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullScreenImageViewer(
    imageUris: List<String>,
    initialIndex: Int = 0,
    repository: Repository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var displayUris by remember(imageUris) { mutableStateOf<List<String?>>(List(imageUris.size) { null }) }
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, (imageUris.size - 1).coerceAtLeast(0)),
        pageCount = { imageUris.size }
    )

    LaunchedEffect(imageUris) {
        if (imageUris.isEmpty()) return@LaunchedEffect
        val resolved = imageUris.map { uri ->
            if (
                uri.startsWith("content://") ||
                uri.startsWith("file://") ||
                uri.startsWith("/") ||
                uri.startsWith("http://") ||
                uri.startsWith("https://") ||
                uri.startsWith("data:")
            ) {
                uri
            } else {
                repository.getImageUri(uri)
            }
        }
        displayUris = resolved
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { onDismiss() }
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val uri = displayUris.getOrNull(page)
                coil3.compose.AsyncImage(
                    model = uri,
                    contentDescription = stringResource(R.string.image_generation_title),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.action_close),
                    tint = Color.White
                )
            }

            TextButton(
                onClick = {
                    val targetUri = displayUris.getOrNull(pagerState.currentPage)
                    if (targetUri == null) {
                        Toast.makeText(context, context.getString(R.string.image_loading), Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }
                    scope.launch {
                        val bmp = withContext(Dispatchers.IO) {
                            when {
                                targetUri.startsWith("content://") -> {
                                    context.contentResolver.openInputStream(android.net.Uri.parse(targetUri))?.use { stream ->
                                        BitmapFactory.decodeStream(stream)
                                    }
                                }
                                targetUri.startsWith("http") -> {
                                    val path = ImageUtils.downloadImage(context, targetUri, "img")
                                    if (path != null) BitmapFactory.decodeFile(path) else null
                                }
                                targetUri.startsWith("data:") -> {
                                    try {
                                        val base64 = targetUri.substringAfter("base64,", "")
                                        val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    } catch (_: Exception) {
                                        null
                                    }
                                }
                                else -> BitmapFactory.decodeFile(targetUri)
                            }
                        }
                        if (bmp != null) {
                            val saved = ImageSaver.saveBitmapToMediaStore(context, bmp, "huajuan_${System.currentTimeMillis()}.png")
                            if (saved != null) {
                                Toast.makeText(context, context.getString(R.string.image_saved_to_album), Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, context.getString(R.string.file_save_failed), Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, context.getString(R.string.file_save_failed), Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = stringResource(R.string.action_save),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(R.string.action_save), color = Color.White)
            }
        }
    }
}
