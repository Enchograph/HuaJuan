package com.chenhongyu.huajuan

import android.R.attr.fontWeight
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
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import com.chenhongyu.huajuan.data.AppDatabase
import com.chenhongyu.huajuan.data.AICreationEntity
import com.chenhongyu.huajuan.data.ImageStorage
import com.chenhongyu.huajuan.data.Repository
import com.chenhongyu.huajuan.render.HtmlTemplateFiller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.core.content.FileProvider
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.chenhongyu.huajuan.utils.ThinkTagProcessor

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
                title = {
                    Text(
                        text = "AI 创作",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Outlined.Menu, contentDescription = "打开侧边栏")
                    }
                },
                // removed the right-side back action per design request
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
                        .padding(4.dp), // reduce outer padding for denser modern look
                    contentPadding = PaddingValues(2.dp) // smaller gaps between items
                ) {
                    items(items) { item ->
                        AICreationCard(item = item, onClick = { selected = item })
                    }
                }
            }

            // Replace AlertDialog with full-screen detail when selected
            if (selected != null) {
                val item = selected!!
                AICreationDetail(item = item, repository = repository, onClose = { selected = null })
            }
        }
    }
}

@Composable
fun AICreationCard(item: AICreationEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(4.dp) // smaller card padding
            .fillMaxWidth()
            .clickable { onClick() },
        // remove elevation to eliminate shadows
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(8.dp) // smaller rounded corners
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

            // Image area (高度为原来的两倍)，不再在图片上叠加信息
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)) {
                if (localBmpCard != null) {
                    Image(
                        bitmap = localBmpCard.asImageBitmap(),
                        contentDescription = item.aiRoleName ?: "AI 创建",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                        Text(item.aiRoleName ?: "默认", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            // 信息栏：单行，左侧帖子标题，右侧生成日期
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title ?: (item.aiRoleName ?: "无标题"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = formatTime(java.util.Date(item.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
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
    onPublished: (String) -> Unit,
    conversationMessages: List<com.chenhongyu.huajuan.data.Message>? = null,
    conversationAt: Long? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val userInfo = repository.getUserInfo()
    var title by remember { mutableStateOf("来自 ${userInfo.username} 的创作") }
    var commentary by remember { mutableStateOf("") }
    var content by remember { mutableStateOf(message.text) }
    var selectedTemplate by remember { mutableStateOf("classic") }
    val templates = listOf("classic", "poem", "card", "dialog")
    var includeConversation by remember { mutableStateOf(true) }
    var publishedAt by remember { mutableStateOf(conversationAt ?: System.currentTimeMillis()) }

    // track the current created entity id so we can publish later
    var currentCreationId by remember { mutableStateOf<String?>(null) }

    var previewBmp by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isGenerating by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        // Limit dialog height to 90% of screen to avoid overly tall layout
        val configuration = LocalConfiguration.current
        val screenHeight = configuration.screenHeightDp.dp
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = screenHeight * 0.9f),
            color = MaterialTheme.colorScheme.background
        ) {
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
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(12.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Top
                ) {
                    // Section: 基本信息
                    Text(text = "基本信息", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("帖子标题") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = commentary,
                        onValueChange = { commentary = it },
                        label = { Text("帖子正文") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        maxLines = 8,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Section: 选项
                    Text(text = "选项", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = includeConversation,
                            onCheckedChange = { includeConversation = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "包含对话内容")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "选择模板", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    // Use horizontal scrolling to avoid crowding when template names are long
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(templates.size) { idx ->
                            val t = templates[idx]
                            FilterChip(
                                selected = (t == selectedTemplate),
                                onClick = { selectedTemplate = t },
                                label = { Text(t) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Section: 预览 - shrink min/max height to be more compact
                    Text(text = "封面预览", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 160.dp, max = 260.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (previewBmp != null) {
                            Image(
                                bitmap = previewBmp!!.asImageBitmap(),
                                contentDescription = "封面预览",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val hint = if (isGenerating) "正在生成封面..." else "尚未生成封面（封面将作为帖子封面）"
                                Text(hint, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "提示：点击下方“生成预览”按钮生成封面图。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action bar
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = onDismiss, enabled = !isGenerating) { Text("取消") }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        isGenerating = true
                                        val convText = if (includeConversation && conversationMessages != null) {
                                            conversationMessages.joinToString(separator = "\n") { m ->
                                                val who = if (m.isUser) repository.getUserInfo().username else repository.getConversationRoleName(conversationId)
                                                "[${who}] ${ThinkTagProcessor.removeThinkTags(m.text)}"
                                            }
                                        } else null

                                        val promptHtml = when (selectedTemplate) {
                                            "poem" -> "<div style=\"font-family: serif; padding:24px; text-align:center;\"><h2>${title}</h2><p>${commentary.replace("\n", "<br/>")}</p><footer style=\"margin-top:16px;opacity:0.7;\">— ${userInfo.username}</footer></div>"
                                            "card" -> "<div style=\"font-family: sans-serif; padding:20px; border:1px solid #eee; border-radius:12px;\"><h3>${title}</h3><div>${commentary.replace("\n", "<br/>")}</div></div>"
                                            "dialog" -> {
                                                // load template from assets and fill with data
                                                try {
                                                    val tpl = context.assets.open("ai_templates/dialog.html").bufferedReader().use { it.readText() }
                                                    val dataMap = mapOf(
                                                        "time" to java.text.SimpleDateFormat("MMM dd, yyyy · HH:mm", java.util.Locale.getDefault()).format(java.util.Date(publishedAt)),
                                                        "userName" to userInfo.username,
                                                        "userSig" to userInfo.signature,
                                                        "aiName" to repository.getConversationRoleName(conversationId),
                                                        "aiModel" to repository.getSelectedModel(),
                                                        "aiPrompt" to commentary,
                                                        "userContent" to (convText ?: content),
                                                        "aiContent" to ""
                                                    )
                                                    HtmlTemplateFiller.fillTemplate(tpl, dataMap)
                                                } catch (_: Exception) {
                                                    // fallback to simple card HTML
                                                    "<div style=\"font-family: system-ui; padding:16px;\"><h3>${title}</h3><div>${commentary.replace("\n", "<br/>")}</div></div>"
                                                }
                                            }
                                            else -> "<div style=\"font-family: system-ui; padding:16px;\"><h3>${title}</h3><div>${commentary.replace("\n", "<br/>")}</div></div>"
                                        }

                                        val id = repository.createAICreationFromMessage(
                                            title = title,
                                            username = repository.getUserInfo().username,
                                            userSignature = repository.getUserInfo().signature,
                                            aiRoleName = repository.getConversationRoleName(conversationId),
                                            aiModelName = repository.getSelectedModel(),
                                            promptHtml = promptHtml,
                                            promptJson = "{\"sourceConversationId\": \"$conversationId\", \"messageId\": \"${message.id}\"}",
                                            conversationText = convText ?: content,
                                            conversationAt = conversationAt,
                                            publishedAt = publishedAt,
                                            commentary = commentary
                                        )

                                        // remember id for publishing later
                                        currentCreationId = id

                                        // generate synchronously to get image immediately for preview
                                        val ok = repository.generateAICreationNow(id)
                                        if (ok) {
                                            // load entity image path
                                            val dao = AppDatabase.getDatabase(context).aiCreationDao()
                                            val ent = withContext(Dispatchers.IO) { dao.getCreationById(id) }
                                            val imgPath = ent?.imageFileName
                                            previewBmp = if (!imgPath.isNullOrEmpty()) {
                                                withContext(Dispatchers.IO) {
                                                    try {
                                                        BitmapFactory.decodeFile(imgPath)
                                                    } catch (_: Exception) {
                                                        null
                                                    }
                                                }
                                            } else null
                                        } else {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "已生成失败，请稍后重试", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Toast.makeText(context, "生成错误: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isGenerating = false
                                    }
                                }
                            },
                            enabled = !isGenerating
                        ) { Text("生成预览") }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        isGenerating = true
                                        // ensure we have an entity id
                                        val id = currentCreationId ?: run {
                                            val convText = if (includeConversation && conversationMessages != null) {
                                                conversationMessages.joinToString(separator = "\n") { m ->
                                                    val who = if (m.isUser) repository.getUserInfo().username else repository.getConversationRoleName(conversationId)
                                                    "[${who}] ${ThinkTagProcessor.removeThinkTags(m.text)}"
                                                }
                                            } else null

                                            val promptHtml = when (selectedTemplate) {
                                                "poem" -> "<div style=\"font-family: serif; padding:24px; text-align:center;\"><h2>${title}</h2><p>${commentary.replace("\n", "<br/>")}</p><footer style=\"margin-top:16px;opacity:0.7;\">— ${userInfo.username}</footer></div>"
                                                "card" -> "<div style=\"font-family: sans-serif; padding:20px; border:1px solid #eee; border-radius:12px;\"><h3>${title}</h3><div>${commentary.replace("\n", "<br/>")}</div></div>"
                                                "dialog" -> {
                                                    // load template from assets and fill with data
                                                    try {
                                                        val tpl = context.assets.open("ai_templates/dialog.html").bufferedReader().use { it.readText() }
                                                        val dataMap = mapOf(
                                                            "time" to java.text.SimpleDateFormat("MMM dd, yyyy · HH:mm", java.util.Locale.getDefault()).format(java.util.Date(publishedAt)),
                                                            "userName" to userInfo.username,
                                                            "userSig" to userInfo.signature,
                                                            "aiName" to repository.getConversationRoleName(conversationId),
                                                            "aiModel" to repository.getSelectedModel(),
                                                            "aiPrompt" to commentary,
                                                            "userContent" to (convText ?: content),
                                                            "aiContent" to ""
                                                        )
                                                        HtmlTemplateFiller.fillTemplate(tpl, dataMap)
                                                    } catch (_: Exception) {
                                                        // fallback to simple card HTML
                                                        "<div style=\"font-family: system-ui; padding:16px;\"><h3>${title}</h3><div>${commentary.replace("\n", "<br/>")}</div></div>"
                                                    }
                                                }
                                                else -> "<div style=\"font-family: system-ui; padding:16px;\"><h3>${title}</h3><div>${commentary.replace("\n", "<br/>")}</div></div>"
                                            }

                                            repository.createAICreationFromMessage(
                                                title = title,
                                                username = repository.getUserInfo().username,
                                                userSignature = repository.getUserInfo().signature,
                                                aiRoleName = repository.getConversationRoleName(conversationId),
                                                aiModelName = repository.getSelectedModel(),
                                                promptHtml = promptHtml,
                                                promptJson = "{\"sourceConversationId\": \"$conversationId\", \"messageId\": \"${message.id}\"}",
                                                conversationText = convText ?: content,
                                                conversationAt = conversationAt,
                                                publishedAt = publishedAt,
                                                commentary = commentary
                                            )
                                        }

                                        // Generate if image missing
                                        val dao = AppDatabase.getDatabase(context).aiCreationDao()
                                        val ent = withContext(Dispatchers.IO) { dao.getCreationById(id) }
                                        if (ent == null) {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "创建失败", Toast.LENGTH_SHORT).show()
                                            }
                                            return@launch
                                        }

                                        if (ent.imageFileName.isNullOrEmpty()) {
                                            val genOk = repository.generateAICreationNow(id)
                                            if (!genOk) {
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, "生成封面失败，已加入队列", Toast.LENGTH_SHORT).show()
                                                }
                                                // enqueue for background generation and still publish the post
                                                repository.enqueueAICreationGeneration(id)
                                            }
                                        }

                                        // mark published (suspend)
                                        withContext(Dispatchers.IO) {
                                            repository.publishAICreation(id)
                                        }

                                        // callback on main thread
                                        withContext(Dispatchers.Main) {
                                            onPublished(id)
                                            Toast.makeText(context, "已发布到 AI 创作", Toast.LENGTH_SHORT).show()
                                        }

                                        // dismiss editor
                                        onDismiss()

                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "发布失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    } finally {
                                        isGenerating = false
                                    }
                                }
                            },
                            enabled = !isGenerating
                        ) {
                            Text("发布")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AICreationDetail(
    item: AICreationEntity,
    repository: Repository,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Dialog(onDismissRequest = onClose) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(0.dp)) {
                TopAppBar(
                    title = { Text(item.aiRoleName ?: "AI 创作") },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "关闭")
                        }
                    }
                )

                val scroll = rememberScrollState()
                Column(modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(12.dp)) {

                    // Cover image (高度为原来的两倍)，不再在图片上叠加信息
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(720.dp)) {
                        val bmp = item.imageFileName?.let { ImageStorage.loadBitmap(it) }
                        if (bmp != null) {
                            Image(bitmap = bmp.asImageBitmap(), contentDescription = "题图", modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                        } else {
                            Box(modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                Text("尚未生成题图")
                            }
                        }
                    }

                    // 标题（图片下方独立显示）
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = item.title ?: "无标题", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

                    // 单行信息栏：左用户名，右生成日期
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = item.username ?: "匿名", style = MaterialTheme.typography.bodyMedium)
                        Text(text = item.createdAt.let { formatTime(java.util.Date(it)) }, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 对话信息：时间、发布
                    Text(text = "对话时间: ${item.conversationAt?.let { formatTime(java.util.Date(it)) } ?: "-"}")
                    Text(text = "发布时间: ${item.publishedAt?.let { formatTime(java.util.Date(it)) } ?: formatTime(java.util.Date(item.createdAt))}")

                    Spacer(modifier = Modifier.height(8.dp))

                    if (!item.commentary.isNullOrEmpty()) {
                        Text(text = "帖子正文", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = item.commentary, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    val convText = item.conversationText
                    if (!convText.isNullOrEmpty()) {
                        Text(text = "对话内容", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = ThinkTagProcessor.removeThinkTags(convText), style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Actions: regenerate, share, delete
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = {
                            scope.launch {
                                repository.enqueueAICreationGeneration(item.id)
                                Toast.makeText(context, "已开始重新生成", Toast.LENGTH_SHORT).show()
                                onClose()
                            }
                        }) { Text("重新生成") }

                        Spacer(modifier = Modifier.width(8.dp))

                        TextButton(onClick = {
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
                            scope.launch {
                                try {
                                    repository.deleteAICreation(item.id)
                                    Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                                } catch (_: Exception) {
                                    Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show()
                                } finally {
                                    onClose()
                                }
                            }
                        }) { Text("删除") }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
