package com.chenhongyu.huajuan

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenhongyu.huajuan.data.Repository
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.m3.Markdown
import androidx.compose.foundation.isSystemInDarkTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

fun formatTime(date: Date): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(date)
}

fun formatConversationTime(date: Date): String {
    val now = Date()
    val diff = now.time - date.time
    val minutes = diff / 60000
    val hours = diff / 3600000
    val days = diff / 86400000
    
    return when {
        days > 0 -> "${days}天前"
        hours > 0 -> "${hours}小时前"
        minutes > 0 -> "${minutes}分钟前"
        else -> "刚刚"
    }
}

data class Message(
    val id: Long,
    val text: String,
    val isUser: Boolean,
    val timestamp: Date
)

data class UserInfo(
    val username: String = "用户名",
    val signature: String = "个性签名",
    val avatar: String = "U"
)

data class Conversation(
    val id: Long,
    val title: String,
    val lastMessage: String,
    val timestamp: Date
)

data class LocalModel(
    val id: String,
    val name: String,
    val size: String,
    val isDownloaded: Boolean = false,
    val downloadProgress: Int = 0
)

data class AppState(
    val conversations: List<Conversation> = listOf(
        Conversation(
            id = 1,
            title = "历史对话 1",
            lastMessage = "你好，请介绍一下你能做什么？",
            timestamp = Date(System.currentTimeMillis() - 3600000) // 1小时前
        ),
        Conversation(
            id = 2,
            title = "历史对话 2",
            lastMessage = "如何学习Jetpack Compose？",
            timestamp = Date(System.currentTimeMillis() - 86400000) // 1天前
        )
    ),
    var currentConversationId: Long? = 1
)

data class ChatState(
    val messages: List<Message> = listOf(
        Message(
            id = 1,
            text = "你好！我是花卷AI助手，有什么我可以帮你的吗？",
            isUser = false,
            timestamp = Date()
        ),
        Message(
            id = 2,
            text = "你好，请介绍一下你能做什么？",
            isUser = true,
            timestamp = Date(System.currentTimeMillis() - 120000) // 2分钟前
        )
    ),
    val inputText: String = ""
)

/**
 * 聊天界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onMenuClick: () -> Unit, 
    appState: AppState, 
    isDarkTheme: Boolean, 
    repository: Repository
) {
    val scope = rememberCoroutineScope()
    var isExpanded by remember { mutableStateOf(false) }
    var chatState by remember { mutableStateOf(ChatState()) }
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "花卷",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = repository.getSelectedModel(),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Outlined.Menu, contentDescription = "打开侧边栏")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        /* 新建对话 */
                        println("新建对话按钮被点击")
                        // 清空聊天记录
                        chatState = ChatState()
                    }) {
                        Icon(Icons.Outlined.Create, contentDescription = "新建对话")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            BottomInputArea(
                isExpanded = isExpanded,
                onExpandChange = { isExpanded = it },
                inputText = chatState.inputText,
                onInputTextChanged = { newText -> 
                    chatState = chatState.copy(inputText = newText)
                },
                onSendMessage = { text ->
                    if (text.isNotBlank()) {
                        // 创建用户消息
                        val userMessage = Message(
                            id = System.currentTimeMillis(),
                            text = text,
                            isUser = true,
                            timestamp = Date()
                        )
                        
                        // 更新聊天状态
                        chatState = chatState.copy(
                            messages = chatState.messages + userMessage,
                            inputText = ""
                        )
                        
                        // 调用AI API获取回复（流式）
                        scope.launch {
                            var accumulatedResponse = ""
                            val aiMessageId = System.currentTimeMillis() + 1
                            
                            // 创建一个初始的AI消息
                            val initialAiMessage = Message(
                                id = aiMessageId,
                                text = "",
                                isUser = false,
                                timestamp = Date()
                            )
                            
                            // 添加初始消息到状态
                            chatState = chatState.copy(
                                messages = chatState.messages + initialAiMessage
                            )
                            
                            // 流式接收响应
                            repository.streamAIResponse(text).collect { chunk ->
                                accumulatedResponse += chunk
                                // 更新消息内容
                                val updatedMessages = chatState.messages.map { message ->
                                    if (message.id == aiMessageId) {
                                        message.copy(text = accumulatedResponse)
                                    } else {
                                        message
                                    }
                                }
                                chatState = chatState.copy(messages = updatedMessages)
                            }
                        }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues -> 
        ChatContentArea(
            messages = chatState.messages,
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        )
    }
}

// 模拟AI API调用
suspend fun callAI_API(prompt: String, appState: AppState): String {
    // 模拟网络延迟
    delay(1000)
    
    // 在实际应用中，这里会调用真实的AI API
    // 根据用户设置的服务提供商和API密钥进行调用
    return "这是来自AI的真实回复: $prompt"
}

/**
 * 聊天内容区域
 */
@Composable
fun ChatContentArea(
    messages: List<Message>,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val isDarkTheme = isSystemInDarkTheme()
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(messages) { message ->
            if (!message.isUser) {
                // AI回复气泡
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 60.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(16.dp)
                        ) {
                            Markdown(
                                content = message.text
                            )
                        }
                    }
                    
                    // 交互按钮
                    Row(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .padding(end = 60.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        var isLiked by remember { mutableStateOf(false) }
                        var isDisliked by remember { mutableStateOf(false) }
                        var isCopied by remember { mutableStateOf(false) }
                        var isFavorited by remember { mutableStateOf(false) }
                        
                        IconButton(
                            onClick = { 
                                isLiked = !isLiked
                                println("${if (isLiked) "已点赞" else "取消点赞"}消息")
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant 
                                    else MaterialTheme.colorScheme.surface,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                contentDescription = "点赞",
                                tint = if (isLiked) MaterialTheme.colorScheme.primary 
                                      else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        IconButton(
                            onClick = { 
                                isDisliked = !isDisliked
                                println("${if (isDisliked) "已点踩" else "取消点踩"}消息")
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant 
                                    else MaterialTheme.colorScheme.surface,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = if (isDisliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                                contentDescription = "点踩",
                                tint = if (isDisliked) MaterialTheme.colorScheme.primary 
                                      else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        IconButton(
                            onClick = { 
                                isCopied = true
                                println("已复制消息")
                                // 重置复制状态
                                scope.launch {
                                    delay(2000) // 2秒后重置
                                    isCopied = false
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant 
                                    else MaterialTheme.colorScheme.surface,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = if (isCopied) Icons.Filled.Check else Icons.Outlined.ContentCopy,
                                contentDescription = if (isCopied) "已复制" else "复制",
                                tint = if (isCopied) MaterialTheme.colorScheme.primary 
                                      else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        IconButton(
                            onClick = { 
                                isFavorited = !isFavorited
                                println("${if (isFavorited) "已收藏" else "取消收藏"}消息")
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant 
                                    else MaterialTheme.colorScheme.surface,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Outlined.Favorite,
                                contentDescription = "收藏",
                                tint = if (isFavorited) MaterialTheme.colorScheme.primary 
                                      else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Text(
                        text = formatTime(message.timestamp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .padding(end = 60.dp)
                    )
                }
            } else {
                // 用户发送气泡
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 60.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(16.dp)
                        ) {
                            Markdown(
                                content = message.text
                            )
                        }
                    }
                    
                    Text(
                        text = formatTime(message.timestamp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .padding(start = 60.dp)
                    )
                }
            }
        }
    }
}

/**
 * 底部输入区域
 */
@Composable
fun BottomInputArea(
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    inputText: String = "",
    onInputTextChanged: (String) -> Unit = {},
    onSendMessage: (String) -> Unit = {}
) {
    var text by remember { mutableStateOf(inputText) }
    val focusManager = LocalFocusManager.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // 输入框行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { 
                    /* 相机功能 */
                    println("相机按钮被点击")
                }
            ) {
                Icon(
                    Icons.Outlined.CameraAlt, 
                    contentDescription = "相机",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            OutlinedTextField(
                value = text,
                onValueChange = { 
                    text = it
                    onInputTextChanged(it)
                },
                placeholder = { 
                    Text(
                        text = "输入消息...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ) 
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                ),
                textStyle = MaterialTheme.typography.bodyLarge,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (text.isNotBlank()) {
                            onSendMessage(text)
                            text = ""
                            focusManager.clearFocus()
                        }
                    }
                ),
                singleLine = true
            )
            
            IconButton(
                enabled = text.isNotBlank(),
                onClick = { 
                    onSendMessage(text)
                    text = ""
                    focusManager.clearFocus()
                }
            ) {
                Icon(
                    imageVector = if (text.isNotBlank()) Icons.AutoMirrored.Filled.Send else Icons.AutoMirrored.Outlined.Send,
                    contentDescription = "发送",
                    tint = if (text.isNotBlank()) MaterialTheme.colorScheme.primary 
                          else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(
                onClick = { onExpandChange(!isExpanded) }
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.Close else Icons.Filled.Add,
                    contentDescription = if (isExpanded) "关闭" else "添加",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // 扩展区
        if (isExpanded) {
            ExpandedInputArea()
        }
    }
}

/**
 * 扩展输入区域
 */
@Composable
fun ExpandedInputArea() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        // 第一行功能按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = { 
                        /* 相机功能 */
                        println("扩展区域相机按钮被点击")
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                ) {
                    Icon(
                        Icons.Outlined.CameraAlt, 
                        contentDescription = "相机",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Text(
                    text = "相机", 
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = { 
                        /* 相册功能 */
                        println("相册按钮被点击")
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                ) {
                    Icon(
                        Icons.Outlined.PhotoLibrary, 
                        contentDescription = "相册",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Text(
                    text = "相册", 
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = { 
                        /* 文件功能 */
                        println("文件按钮被点击")
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.InsertDriveFile, 
                        contentDescription = "文件",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Text(
                    text = "文件", 
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = { 
                        /* 打电话功能 */
                        println("打电话按钮被点击")
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                ) {
                    Icon(
                        Icons.Outlined.Call, 
                        contentDescription = "打电话",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Text(
                    text = "通话", 
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // 图片网格标题
        Text(
            text = "最近图片",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
        )
        
        // 四列2.5行的图片网格
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            repeat(3) { rowIndex ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(4) { columnIndex ->
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { 
                                    println("选择了图片: 行$rowIndex, 列$columnIndex")
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.Center)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 设置界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(repository: Repository, darkModeState: MutableState<Boolean>, onBack: () -> Unit = {}) {
    var darkMode by remember { mutableStateOf(darkModeState.value) }
    var useCloudModel by remember { mutableStateOf(repository.getUseCloudModel()) }
    var serviceProvider by remember { mutableStateOf(repository.getServiceProvider()) }
    var customApiUrl by remember { mutableStateOf(repository.getCustomApiUrl()) }
    var apiKey by remember { mutableStateOf(repository.getApiKey()) }
    var selectedModel by remember { mutableStateOf(repository.getSelectedModel()) }
    var userInfo by remember { mutableStateOf(UserInfo()) }
    var showEditDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    if (showEditDialog) {
        EditUserInfoDialog(
            userInfo = userInfo,
            onUserInfoChange = { userInfo = it },
            onDismiss = { showEditDialog = false }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
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
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            // 用户信息部分
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userInfo.avatar, 
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.headlineSmall
                                )
                            }
                            
                            Spacer(Modifier.width(16.dp))
                            
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = userInfo.username,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = userInfo.signature,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Button(
                                onClick = { 
                                    showEditDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier
                            ) {
                                Text("编辑")
                            }
                        }
                    }
                }
            }
            
            // 主题设置
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "外观",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.padding(end = 16.dp)
                            ) {
                                Text(
                                    text = "深色模式",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (darkMode) "已启用" else "已禁用",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = darkMode,
                                onCheckedChange = { 
                                    darkMode = it
                                    darkModeState.value = it
                                    repository.setDarkMode(it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    }
                }
            }
            
            // 模型设置
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "AI模型设置",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.padding(end = 16.dp)
                            ) {
                                Text(
                                    text = "使用云端模型",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (useCloudModel) "已启用" else "使用本地模型",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = useCloudModel,
                                onCheckedChange = { 
                                    useCloudModel = it
                                    repository.setUseCloudModel(it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                        
                        if (useCloudModel) {
                            // 云端模型设置
                            ServiceProviderSelector(
                                serviceProvider = serviceProvider,
                                onServiceProviderChange = { 
                                    serviceProvider = it
                                    repository.setServiceProvider(it)
                                },
                                customApiUrl = customApiUrl,
                                onCustomApiUrlChange = { 
                                    customApiUrl = it
                                    repository.setCustomApiUrl(it)
                                }
                            )
                            
                            OutlinedTextField(
                                value = apiKey,
                                onValueChange = { 
                                    apiKey = it
                                    repository.setApiKey(it)
                                },
                                label = { Text("API密钥") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                            
                            ModelSelector(
                                selectedModel = selectedModel,
                                onModelChange = { 
                                    selectedModel = it
                                    repository.setSelectedModel(it)
                                }
                            )
                            
                            // 添加测试连接按钮
                            Button(
                                onClick = {
                                    // 在实际应用中，这里会测试API连接
                                    scope.launch {
                                        val response = repository.getAIResponse("Hello, test connection!")
                                        Toast.makeText(context, response, Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Text("测试连接")
                            }
                        } else {
                            // 本地模型设置
                            LocalModelSection()
                        }
                    }
                }
            }
        }
    }
}

/**
 * 编辑用户信息对话框
 */
@Composable
fun EditUserInfoDialog(
    userInfo: UserInfo,
    onUserInfoChange: (UserInfo) -> Unit,
    onDismiss: () -> Unit
) {
    var username by remember { mutableStateOf(userInfo.username) }
    var signature by remember { mutableStateOf(userInfo.signature) }
    var avatar by remember { mutableStateOf(userInfo.avatar) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "编辑用户信息",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            ) 
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("用户名") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                
                OutlinedTextField(
                    value = signature,
                    onValueChange = { signature = it },
                    label = { Text("个性签名") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                
                OutlinedTextField(
                    value = avatar,
                    onValueChange = { avatar = it },
                    label = { Text("头像字符") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onUserInfoChange(
                        UserInfo(
                            username = username,
                            signature = signature,
                            avatar = avatar
                        )
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("取消")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

/**
 * 服务提供商选择器
 */
@Composable
fun ServiceProviderSelector(
    serviceProvider: String,
    onServiceProviderChange: (String) -> Unit,
    customApiUrl: String,
    onCustomApiUrlChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val serviceProviders = listOf("OpenAI", "Azure", "Anthropic", "自定义")
    
    Column {
        Text(
            text = "服务提供商",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(serviceProvider)
                Icon(
                    Icons.Outlined.ArrowDropDown, 
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
            ) {
                serviceProviders.forEach { provider ->
                    DropdownMenuItem(
                        text = { 
                            Text(
                                text = provider,
                                color = MaterialTheme.colorScheme.onSurface
                            ) 
                        },
                        onClick = {
                            onServiceProviderChange(provider)
                            expanded = false
                        }
                    )
                }
            }
        }
        
        if (serviceProvider == "自定义") {
            OutlinedTextField(
                value = customApiUrl,
                onValueChange = onCustomApiUrlChange,
                label = { Text("API网址") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    }
}

/**
 * 模型选择器
 */
@Composable
fun ModelSelector(
    selectedModel: String,
    onModelChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val models = listOf("GPT-4", "GPT-3.5 Turbo", "Claude 2", "Claude Instant")
    
    Column {
        Text(
            text = "选择模型",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(selectedModel)
                Icon(
                    Icons.Outlined.ArrowDropDown, 
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                models.forEach { model ->
                    DropdownMenuItem(
                        text = { 
                            Text(
                                text = model,
                                color = MaterialTheme.colorScheme.onSurface
                            ) 
                        },
                        onClick = {
                            onModelChange(model)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LocalModelSection() {
    Column {
        Text(
            text = "本地模型",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            ListItem(
                headlineContent = { 
                    Text(
                        text = "Qwen 7B",
                        color = MaterialTheme.colorScheme.onSurface
                    ) 
                },
                supportingContent = { 
                    Text(
                        text = "已下载",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ) 
                },
                trailingContent = {
                    RadioButton(
                        selected = true,
                        onClick = { 
                            /* 选择模型 */
                            println("选择了Qwen 7B模型")
                        },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary,
                            unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            )
        }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            ListItem(
                headlineContent = { 
                    Text(
                        text = "Llama 2 7B",
                        color = MaterialTheme.colorScheme.onSurface
                    ) 
                },
                supportingContent = { 
                    Text(
                        text = "已下载",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ) 
                },
                trailingContent = {
                    RadioButton(
                        selected = false,
                        onClick = { 
                            /* 选择模型 */
                            println("选择了Llama 2 7B模型")
                        },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary,
                            unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            )
        }
        
        // 添加新模型按钮
        OutlinedButton(
            onClick = { 
                /* 下载新模型 */
                println("添加模型按钮被点击")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(
                Icons.Outlined.Add, 
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(8.dp))
            Text("添加模型")
        }
    }
}

/**
 * 侧边栏导航
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SideDrawer(
    onChatPageSelected: (Long) -> Unit = {},
    onSettingPageSelected: () -> Unit = {},
    conversations: List<Conversation> = emptyList(),
    drawerWidth: androidx.compose.ui.unit.Dp = 300.dp
) {
    ModalDrawerSheet(
        modifier = Modifier.width(drawerWidth),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerTonalElevation = 1.dp
    ) {
        // 顶部应用标题和图标
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "花卷AI",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        // 搜索框 - 修改为浅灰色背景的胶囊形状，内有搜索图标和"搜索..."占位文字
        OutlinedTextField(
            value = "",
            onValueChange = {},
            placeholder = { Text("搜索...") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(50.dp), // 胶囊形状
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )

        // 新建对话按钮
        Button(
            onClick = {
                println("新建对话按钮被点击")
                // 在实际应用中，这里会创建一个新的对话
                onChatPageSelected(-1) // -1表示新建对话
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(2.dp)
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("新建对话")
        }

        // 历史对话标题
        Text(
            text = "历史对话",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
        )

        // 历史对话列表
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(conversations) { conversation ->
                ListItem(
                    headlineContent = { 
                        Text(
                            text = conversation.title,
                            maxLines = 1,
                            style = MaterialTheme.typography.bodyLarge
                        ) 
                    },
                    supportingContent = { 
                        Text(
                            text = "${conversation.lastMessage} · ${formatConversationTime(conversation.timestamp)}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ) 
                    },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape) // 圆形彩色背景图标
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = conversation.title.take(1),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { 
                            println("选择了对话: ${conversation.title}")
                            onChatPageSelected(conversation.id) 
                        }
                        .padding(horizontal = 8.dp)
                )
            }
        }

        // 底部用户栏
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        ListItem(
            headlineContent = { 
                Text(
                    text = "用户名",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                ) 
            },
            supportingContent = { 
                Text(
                    text = "个性签名",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ) 
            },
            leadingContent = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape) // 圆形头像
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "U", 
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            trailingContent = {
                IconButton(
                    onClick = {
                        println("点击了设置按钮")
                        onSettingPageSelected()
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "设置",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            ),
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { 
                    println("点击了底部用户栏")
                    onSettingPageSelected() 
                }
                .padding(horizontal = 8.dp)
        )
    }
}
