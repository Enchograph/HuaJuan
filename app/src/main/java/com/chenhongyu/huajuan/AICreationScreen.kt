package com.chenhongyu.huajuan

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chenhongyu.huajuan.data.AppDatabase
import com.chenhongyu.huajuan.data.AICreationEntity
import com.chenhongyu.huajuan.data.ImageStorage
import com.chenhongyu.huajuan.data.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.core.content.FileProvider
import androidx.compose.ui.window.Dialog

/**
 * AI创作页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AICreationScreen(
    onMenuClick: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = remember { db.aiCreationDao() }
    val repository = remember { Repository(context) }
    val scope = rememberCoroutineScope()

    // collect creations from Room
    val creationsState = produceState<List<AICreationEntity>>(initialValue = emptyList(), key1 = dao) {
        dao.getAllCreations().collect { list ->
            value = list
        }
    }

    var selected by remember { mutableStateOf<AICreationEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 创作") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Outlined.Menu, contentDescription = "打开侧边栏")
                    }
                },
                actions = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            val items = creationsState.value
            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无已收藏的AI消息，长按消息可收藏为AI创作")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    items(items) { item ->
                        AICreationCard(item = item, onClick = { selected = item })
                    }
                }
            }

            if (selected != null) {
                val item = selected!!
                AlertDialog(
                    onDismissRequest = { selected = null },
                    title = { Text(text = item.aiRoleName ?: "AI 创作") },
                    text = {
                        Column {
                            val bmp = item.imageFileName?.let { ImageStorage.loadBitmap(it) }
                            val localBmp = bmp
                            if (localBmp != null) {
                                Image(bitmap = localBmp.asImageBitmap(), contentDescription = null, modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp))
                            } else {
                                Text("尚未生成图片，正在排队或生成中...")
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "作者: ${item.username ?: "匿名"}")
                            Text(text = "更新时间: ${formatTime(java.util.Date(item.updatedAt))}")
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            // Regenerate: enqueue generation again
                            scope.launch {
                                repository.enqueueAICreationGeneration(item.id)
                                Toast.makeText(context, "已开始重新生成", Toast.LENGTH_SHORT).show()
                                selected = null
                            }
                        }) { Text("重新生成") }
                    },
                    dismissButton = {
                        Row {
                            TextButton(onClick = {
                                // Share
                                try {
                                    val f = File(item.imageFileName ?: "")
                                    if (f.exists()) {
                                        val authority = "com.chenhongyu.huajuan.fileprovider"
                                        val uri: Uri = FileProvider.getUriForFile(context, authority, f)
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "image/*"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        val chooser = Intent.createChooser(shareIntent, "分享图片")
                                        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(chooser)
                                    } else {
                                        Toast.makeText(context, "图片不存在，无法分享", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (_: Exception) {
                                    Toast.makeText(context, "分享失败", Toast.LENGTH_SHORT).show()
                                }
                            }) { Text("分享") }

                            Spacer(modifier = Modifier.width(8.dp))

                            TextButton(onClick = {
                                // Delete
                                scope.launch {
                                    try {
                                        repository.deleteAICreation(item.id)
                                        Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        selected = null
                                    }
                                }
                            }) { Text("删除") }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AICreationCard(item: AICreationEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(6.dp)
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            var bmp by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
            LaunchedEffect(item.imageFileName) {
                if (!item.imageFileName.isNullOrEmpty()) {
                    bmp = withContext(Dispatchers.IO) {
                        try {
                            BitmapFactory.decodeFile(item.imageFileName)
                        } catch (_: Exception) {
                            null
                        }
                    }
                } else {
                    bmp = null
                }
            }

            val localBmpCard = bmp
            if (localBmpCard != null) {
                Image(bitmap = localBmpCard.asImageBitmap(), contentDescription = item.aiRoleName ?: "AI 创建", modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp))
            } else {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    Text(item.aiRoleName ?: "默认", fontWeight = FontWeight.Bold)
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(text = item.username ?: "匿名", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = formatTimeAgo(item.createdAt), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AICreationEditor(
    message: com.chenhongyu.huajuan.data.Message,
    repository: Repository,
    conversationId: String,
    onDismiss: () -> Unit,
    onPublished: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("来自 ${repository.getUserInfo().username} 的创作") }
    var content by remember { mutableStateOf(message.text) }
    var selectedTemplate by remember { mutableStateOf("classic") }
    val templates = listOf("classic", "poem", "card")

    Dialog(onDismissRequest = onDismiss) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("发布到 AI 创作") },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "关闭")
                            }
                        }
                    )
                }
            ) { padding ->
                Column(modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize(), verticalArrangement = Arrangement.Top) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("帖子标题") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("帖子内容") },
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "选择模板", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        templates.forEach { t ->
                            FilterChip(
                                selected = (t == selectedTemplate),
                                onClick = { selectedTemplate = t },
                                label = { Text(t) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = onDismiss) { Text("取消") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            val promptHtml = when (selectedTemplate) {
                                "poem" -> "<div style=\"font-family: serif; padding:24px; text-align:center;\"><h2>${title}</h2><p>${content.replace("\n", "<br/>")}</p><footer style=\"margin-top:16px;opacity:0.7;\">— ${repository.getUserInfo().username}</footer></div>"
                                "card" -> "<div style=\"font-family: sans-serif; padding:20px; border:1px solid #eee; border-radius:12px;\"><h3>${title}</h3><div>${content.replace("\n", "<br/>")}</div></div>"
                                else -> "<div style=\"font-family: system-ui; padding:16px;\"><h3>${title}</h3><div>${content.replace("\n", "<br/>")}</div></div>"
                            }
                            scope.launch {
                                try {
                                    val id = repository.createAICreationFromMessage(
                                        title = title,
                                        username = repository.getUserInfo().username,
                                        userSignature = repository.getUserInfo().signature,
                                        aiRoleName = repository.getConversationRoleName(conversationId),
                                        aiModelName = repository.getSelectedModel(),
                                        promptHtml = promptHtml,
                                        promptJson = "{\"sourceConversationId\": \"$conversationId\", \"messageId\": \"${message.id}\"}"
                                    )
                                    repository.enqueueAICreationGeneration(id)
                                    onPublished(id)
                                    onDismiss()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "发布失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) { Text("发布") }
                    }
                }
            }
        }
    }
}

fun formatTimeAgo(epochMillis: Long): String {
    val diff = System.currentTimeMillis() - epochMillis
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return when {
        days > 0 -> "${days}天前"
        hours > 0 -> "${hours}小时前"
        minutes > 0 -> "${minutes}分钟前"
        else -> "刚刚"
    }
}