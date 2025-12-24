package com.chenhongyu.huajuan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.chenhongyu.huajuan.data.ImageStorage
import java.io.File
import kotlinx.coroutines.launch
import com.chenhongyu.huajuan.utils.getImageList

/**
 * 图片选择页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageSelectorScreen(
    onImageSelected: (List<String>) -> Unit,  // 选择图片后的回调
    onBack: () -> Unit  // 返回回调
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var imageList by remember { mutableStateOf<List<ImageItem>>(emptyList()) }
    var selectedImages by remember { mutableStateOf<Set<String>>(setOf()) }
    var permissionGranted by remember { mutableStateOf(false) }
    
    // 权限请求启动器
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
        if (isGranted) {
            scope.launch {
                imageList = getImageList(context)
            }
        }
    }

    // 检查权限
    LaunchedEffect(Unit) {
        permissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionGranted) {
            scope.launch {
                imageList = getImageList(context)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    // 获取图片列表
    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            scope.launch {
                imageList = getImageList(context)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // 顶部栏
        TopAppBar(
            title = { Text("选择图片", fontSize = 18.sp) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            },
            actions = {
                TextButton(
                    onClick = { 
                        onImageSelected(selectedImages.toList()) 
                        onBack() // 选择完成后返回
                    },
                    enabled = selectedImages.isNotEmpty()
                ) {
                    Text(
                        text = "完成 (${selectedImages.size})",
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
                    text = "请授予存储权限以访问图片",
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
                    ImageItemCard(
                        image = image,
                        isSelected = selectedImages.contains(image.uri),
                        onClick = { uri ->
                            selectedImages = if (selectedImages.contains(uri)) {
                                selectedImages - uri
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

/**
 * 图片项卡片
 */
@Composable
fun ImageItemCard(
    image: ImageItem,
    isSelected: Boolean,
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
                .fillMaxSize()
        )
        
        // 选择状态覆盖层
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    ),
                contentAlignment = Alignment.TopEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.shapes.small
                        )
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "已选择",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}