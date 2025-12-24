package com.chenhongyu.huajuan.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import com.chenhongyu.huajuan.ImageItem
import com.chenhongyu.huajuan.utils.getImageList
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * 扩展输入区域的主要组件
 */
@Composable
fun ExpandedInputArea(onOpenImageSelector: () -> Unit) {
    var scrollOffset by remember { mutableStateOf(0f) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var imageList by remember { mutableStateOf<List<ImageItem>>(emptyList()) }
    var permissionGranted by remember { mutableStateOf(false) }
    
    // 获取屏幕宽度
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val spacing = 4.dp
    
    // 计算按钮宽度（考虑间距）
    val buttonSpacingTotal = spacing * 5 // 左右padding + 按钮间间距
    val buttonWidth = (screenWidth - buttonSpacingTotal) / 4 // 4个按钮
    
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
        
        if (!permissionGranted) {
            // 如果没有权限，请求权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        } else {
            // 如果已有权限，加载图片
            scope.launch {
                imageList = getImageList(context)
            }
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
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        // 如果向上滑动距离超过阈值，则打开图片选择页面
                        if (scrollOffset > 100) {
                            onOpenImageSelector()
                        }
                    }
                ) { change, dragAmount ->
                    scrollOffset += dragAmount.y
                    change.consume()
                }
            }
    ) {
        // 第一行功能按钮 - 四个圆角正方形按钮，几乎紧邻
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 相机按钮
            ExpandedInputSquareButton(
                icon = Icons.Outlined.CameraAlt,
                label = "相机",
                onClick = { println("相机按钮被点击") },
                buttonSize = buttonWidth
            )

            // 相册按钮
            ExpandedInputSquareButton(
                icon = Icons.Outlined.PhotoLibrary,
                label = "相册",
                onClick = { onOpenImageSelector() },
                buttonSize = buttonWidth
            )

            // 文件按钮
            ExpandedInputSquareButton(
                icon = Icons.AutoMirrored.Outlined.InsertDriveFile,
                label = "文件",
                onClick = { println("文件按钮被点击") },
                buttonSize = buttonWidth
            )

            // 电话按钮
            ExpandedInputSquareButton(
                icon = Icons.Outlined.Call,
                label = "电话",
                onClick = { println("电话按钮被点击") },
                buttonSize = buttonWidth
            )
        }

        // 图片网格 - 四列网格，所有元素都是等大的圆角正方形，几乎紧邻
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxWidth()
                .height(buttonWidth * 1.5f) // 图片网格高度基于按钮宽度
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 合并图片列表和占位符
            val totalItems = 8
            val displayItems = imageList.take(10)
            val placeholderCount = max(0, totalItems - displayItems.size)
            val allItems = displayItems + List(placeholderCount) { index ->
                ImageItem(
                    id = "placeholder_$index",
                    uri = "",
                    displayName = "",
                    dateAdded = 0L
                )
            }
            
            items(allItems) { image ->
                if (image.id.startsWith("placeholder_")) {
                    // 占位符项 - 等大的圆角正方形
                    Box(
                        modifier = Modifier
                            .size(buttonWidth) // 使用与按钮相同的尺寸，确保是正方形
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                } else {
                    // 实际图片项 - 等大的圆角正方形，图片等比放大填充
                    Box(
                        modifier = Modifier
                            .size(buttonWidth) // 使用与按钮相同的尺寸，确保是正方形
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { println("选择了图片: ${image.displayName}") }
                    ) {
                        AsyncImage(
                            model = image.uri,
                            contentDescription = image.displayName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop // 确保图片填充整个正方形，保持比例并裁剪多余部分
                        )
                    }
                }
            }
        }
    }
}

/**
 * 扩展输入区域的正方形按钮组件
 * 所有元素都被限定在等大的圆角正方形里面，圆角正方形之间几乎紧邻
 */
@Composable
fun ExpandedInputSquareButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: androidx.compose.ui.unit.Dp = 0.dp // 添加按钮尺寸参数
) {
    val size = if (buttonSize > 0.dp) buttonSize else 64.dp // 如果指定了尺寸则使用，否则使用默认值
    Box(
        modifier = Modifier
            .size(size) // 使用计算出的尺寸
            .clip(RoundedCornerShape(8.dp)) // 圆角
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = label,
                fontSize = 8.sp, // 减小字体大小以适应圆角矩形内
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}