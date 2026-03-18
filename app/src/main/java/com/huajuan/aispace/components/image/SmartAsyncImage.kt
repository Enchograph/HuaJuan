package com.huajuan.aispace.components.image

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.huajuan.aispace.data.Repository

@Composable
fun SmartAsyncImage(
    imageIdOrUri: String,
    repository: Repository,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    maxHeightDp: Dp = 520.dp,
    fixedSizeDp: Dp? = null
) {
    var localUri by remember(imageIdOrUri) { mutableStateOf<String?>(null) }

    LaunchedEffect(imageIdOrUri) {
        localUri = if (imageIdOrUri.startsWith("content://") || imageIdOrUri.startsWith("file://") || imageIdOrUri.startsWith("/")) {
            imageIdOrUri
        } else {
            repository.getImageUri(imageIdOrUri)
        }
    }

    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current)
            .data(localUri)
            .crossfade(true)
            .build()
    )
    val state by painter.state.collectAsState()
    val aspectRatio: Float? = (state as? AsyncImagePainter.State.Success)
        ?.result
        ?.image
        ?.let { image ->
            val w = image.width
            val h = image.height
            if (w > 0 && h > 0) w.toFloat() / h.toFloat() else null
        }
    val outerModifier = if (fixedSizeDp != null) {
        if (aspectRatio != null) {
            if (aspectRatio <= 1f) {
                Modifier.height(fixedSizeDp).aspectRatio(aspectRatio)
            } else {
                Modifier.width(fixedSizeDp).aspectRatio(aspectRatio)
            }
        } else {
            Modifier.size(fixedSizeDp)
        }
    } else {
        Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeightDp)
    }

    Box(modifier = outerModifier.then(modifier)) {
        if (localUri != null) {
            Image(
                painter = painter,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        }
        if (localUri == null || state is AsyncImagePainter.State.Loading || state is AsyncImagePainter.State.Empty) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
