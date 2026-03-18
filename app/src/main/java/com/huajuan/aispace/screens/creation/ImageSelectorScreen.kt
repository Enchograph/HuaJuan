package com.huajuan.aispace.screens.creation
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.huajuan.aispace.R
import com.huajuan.aispace.data.model.ImageItem
import com.huajuan.aispace.utils.getImageList
import kotlinx.coroutines.delay

/**
 * 图片选择页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageSelectorScreen(
    onImageSelected: (List<String>) -> Unit,  // 选择图片后的回调
    initialSelectedImages: List<String> = emptyList(),
    onBack: () -> Unit  // 返回回调
) {
    val context = LocalContext.current
    var imageList by remember { mutableStateOf<List<ImageItem>>(emptyList()) }
    var selectedImages by remember(initialSelectedImages) { mutableStateOf(initialSelectedImages) }
    var permissionGranted by remember { mutableStateOf(false) }
    var albumExpanded by remember { mutableStateOf(false) }
    var selectedAlbum by remember { mutableStateOf(context.getString(R.string.image_selector_all_photos)) }
    var visible by remember { mutableStateOf(true) }

    val closeWithAnimation: () -> Unit = {
        visible = false
    }

    LaunchedEffect(visible) {
        if (!visible) {
            delay(220)
            onBack()
        }
    }
    
    // 权限请求启动器
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
    }

    // 检查权限
    LaunchedEffect(Unit) {
        permissionGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_IMAGES
        ) == PackageManager.PERMISSION_GRANTED
        
        if (!permissionGranted) {
            requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
        }
    }

    // 获取图片列表
    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            imageList = getImageList(context)
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // 顶部栏
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .clickable { albumExpanded = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(selectedAlbum, fontSize = 18.sp)
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = stringResource(R.string.image_selector_switch_album)
                        )
                    }
                    DropdownMenu(
                        expanded = albumExpanded,
                        onDismissRequest = { albumExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.image_selector_all_photos)) },
                            onClick = {
                                selectedAlbum = context.getString(R.string.image_selector_all_photos)
                                albumExpanded = false
                            }
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = closeWithAnimation) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            onImageSelected(selectedImages)
                            closeWithAnimation()
                        },
                        enabled = selectedImages.isNotEmpty()
                    ) {
                        Text(
                            text = stringResource(R.string.image_selector_done_count, selectedImages.size),
                            color = if (selectedImages.isNotEmpty()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )

            if (!permissionGranted) {
                // 权限请求提示
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.image_selector_permission_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // 图片网格
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    state = rememberLazyGridState(),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(imageList) { image ->
                        val index = selectedImages.indexOf(image.uri)
                        ImageItemCard(
                            image = image,
                            selectedIndex = index,
                            onClick = { uri ->
                                selectedImages = if (selectedImages.contains(uri)) {
                                    selectedImages.filterNot { it == uri }
                                } else {
                                    selectedImages + uri
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 图片项卡片
 */
@Composable
fun ImageItemCard(
    image: ImageItem,
    selectedIndex: Int,
    onClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.small)
            .clickable { onClick(image.uri) }
    ) {
        AsyncImage(
            model = image.uri,
            contentDescription = image.displayName,
            modifier = Modifier
                .fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.TopEnd
        ) {
            val isSelected = selectedIndex >= 0
            Box(
                modifier = Modifier
                    .padding(6.dp)
                    .size(24.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.small
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Text(
                        text = "${selectedIndex + 1}",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

