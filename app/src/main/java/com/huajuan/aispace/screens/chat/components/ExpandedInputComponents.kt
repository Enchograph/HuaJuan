package com.huajuan.aispace.screens.chat.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import com.huajuan.aispace.R
import com.huajuan.aispace.data.model.ImageItem
import com.huajuan.aispace.utils.getImageList
import kotlin.math.floor

/**
 * 扩展输入区域的主要组件
 */
@Composable
fun ExpandedInputArea(
    onOpenImageSelector: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenFilePicker: () -> Unit,
    onQuickPickImage: (String) -> Unit,
    onOpenKnowledgeBasePicker: () -> Unit,
    currentKnowledgeBaseLabel: String,
    currentSearchProviderLabel: String,
    onOpenSearchSettings: () -> Unit,
    onOpenMcpSettings: () -> Unit,
    onOpenQuickPhrasePicker: () -> Unit
) {
    var scrollOffset by remember { mutableStateOf(0f) }
    var showMoreActions by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var imageList by remember { mutableStateOf<List<ImageItem>>(emptyList()) }
    var permissionGranted by remember { mutableStateOf(false) }

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
            imageList = getImageList(context, maxItems = 60)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        val density = LocalDensity.current
        val rowSpacing = 4.dp
        val sidePadding = 4.dp
        val baseColSpacing = 4.dp
        val innerWidthPx = with(density) { (maxWidth - sidePadding * 2).toPx() }
        val baseColSpacingPx = with(density) { baseColSpacing.toPx() }
        val tilePx = floor((innerWidthPx - baseColSpacingPx * 3) / 4f).coerceAtLeast(1f)
        val colSpacingPx = ((innerWidthPx - tilePx * 4) / 3f).coerceAtLeast(0f)
        val tileSize = with(density) { tilePx.toDp() }
        val colSpacing = with(density) { colSpacingPx.toDp() }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 第一行：四个功能按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = sidePadding),
                horizontalArrangement = Arrangement.spacedBy(colSpacing)
            ) {
                ExpandedInputSquareButton(
                    icon = Icons.Outlined.CameraAlt,
                    label = stringResource(R.string.chat_camera),
                    onClick = onOpenCamera,
                    buttonSize = tileSize
                )
                ExpandedInputSquareButton(
                    icon = Icons.Outlined.PhotoLibrary,
                    label = stringResource(R.string.chat_album),
                    onClick = onOpenImageSelector,
                    buttonSize = tileSize
                )
                ExpandedInputSquareButton(
                    icon = Icons.AutoMirrored.Outlined.InsertDriveFile,
                    label = stringResource(R.string.chat_file),
                    onClick = onOpenFilePicker,
                    buttonSize = tileSize
                )
                ExpandedInputSquareButton(
                    icon = Icons.Outlined.MoreHoriz,
                    label = stringResource(R.string.chat_more_actions),
                    onClick = { showMoreActions = !showMoreActions },
                    buttonSize = tileSize
                )
            }

            if (showMoreActions) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = sidePadding),
                    horizontalArrangement = Arrangement.spacedBy(colSpacing)
                ) {
                    ExpandedInputSquareButton(
                        icon = Icons.Outlined.Storage,
                        label = currentKnowledgeBaseLabel,
                        onClick = onOpenKnowledgeBasePicker,
                        buttonSize = tileSize
                    )
                    ExpandedInputSquareButton(
                        icon = Icons.Outlined.Search,
                        label = currentSearchProviderLabel,
                        onClick = onOpenSearchSettings,
                        buttonSize = tileSize
                    )
                    ExpandedInputSquareButton(
                        icon = Icons.Outlined.Extension,
                        label = "MCP",
                        onClick = onOpenMcpSettings,
                        buttonSize = tileSize
                    )
                    ExpandedInputSquareButton(
                        icon = Icons.Outlined.Bolt,
                        label = stringResource(R.string.chat_quick_phrase),
                        onClick = onOpenQuickPhrasePicker,
                        buttonSize = tileSize
                    )
                }
            }

            // 图片网格：2行*4列，显示一行半高度
            val gridRows = 2
            val gridFullHeight = tileSize * gridRows + rowSpacing * (gridRows - 1)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(tileSize * 1.5f + rowSpacing)
                    .padding(horizontal = sidePadding)
                    .clipToBounds()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                if (scrollOffset < -80) {
                                    onOpenImageSelector()
                                }
                                scrollOffset = 0f
                            }
                        ) { change, dragAmount ->
                            scrollOffset += dragAmount.y
                            change.consume()
                        }
                    }
            ) {
                val totalImages = 8
                val displayImages = imageList.take(totalImages)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(gridFullHeight),
                    verticalArrangement = Arrangement.spacedBy(rowSpacing)
                ) {
                    repeat(2) { rowIndex ->
                        Row(
                            modifier = Modifier
                                .height(tileSize)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(colSpacing)
                        ) {
                            val start = rowIndex * 4
                            val rowImages = displayImages.drop(start).take(4)
                            repeat(4) { index ->
                                val image = rowImages.getOrNull(index)
                                if (image == null) {
                                    Box(
                                        modifier = Modifier
                                            .size(tileSize)
                                            .clip(MaterialTheme.shapes.small)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    )
                                } else {
                                    val interactionSource = remember(image.uri) { MutableInteractionSource() }
                                    val pressed by interactionSource.collectIsPressedAsState()
                                    val scale by animateFloatAsState(
                                        targetValue = if (pressed) 0.97f else 1f,
                                        animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
                                        label = "quick_pick_scale"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(tileSize)
                                            .clip(MaterialTheme.shapes.small)
                                            .graphicsLayer {
                                                scaleX = scale
                                                scaleY = scale
                                            }
                                            .clickable(
                                                interactionSource = interactionSource,
                                                indication = null
                                            ) { onQuickPickImage(image.uri) }
                                    ) {
                                        AsyncImage(
                                            model = image.uri,
                                            contentDescription = image.displayName,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }
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
    buttonSize: androidx.compose.ui.unit.Dp = 0.dp // 添加按钮尺寸参数
) {
    val size = if (buttonSize > 0.dp) buttonSize else 64.dp // 如果指定了尺寸则使用，否则使用默认值
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "square_btn_scale"
    )
    Box(
        modifier = Modifier
            .size(size) // 使用计算出的尺寸
            .clip(MaterialTheme.shapes.small) // 圆角
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
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
