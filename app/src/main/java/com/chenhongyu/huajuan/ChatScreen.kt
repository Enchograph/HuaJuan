package com.chenhongyu.huajuan

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenhongyu.huajuan.data.Repository
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import androidx.compose.foundation.isSystemInDarkTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import com.chenhongyu.huajuan.ui.theme.HuaJuanTheme
import androidx.compose.ui.window.Dialog
import com.chenhongyu.huajuan.data.Message
import com.chenhongyu.huajuan.data.ChatState
import com.chenhongyu.huajuan.data.AppState
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlin.math.roundToInt

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
    println("DEBUG: ChatScreen recomposed with currentConversationId: ${appState.currentConversationId}")
    val scope = rememberCoroutineScope()
    var isExpanded by remember { mutableStateOf(false) }
    // 移除对appState.currentConversationId的依赖，避免重组时的过渡动画
    var chatState by remember { 
        println("DEBUG: Initializing chatState")
        mutableStateOf(
            ChatState(
                messages = emptyList(),
                inputText = ""
            )
        ) 
    }
    val context = LocalContext.current
    
    // 获取当前对话的角色名称
    var roleName by remember { mutableStateOf("默认助手") }
    // 获取当前对话的系统提示词
    var systemPrompt by remember { mutableStateOf("你是一个AI助手") }

    // 数据库操作互斥锁，防止并发访问
    val dbMutex = remember { kotlinx.coroutines.sync.Mutex() }
    
    // 使用DisposableEffect替代LaunchedEffect，避免过渡动画
    DisposableEffect(appState.currentConversationId) {
        println("DEBUG: DisposableEffect triggered for conversationId: ${appState.currentConversationId}")
        val conversationId = appState.currentConversationId ?: "default"
        val messages = repository.getMessages(conversationId)
        // 获取当前对话的角色名称
        roleName = repository.getConversationRoleName(conversationId)
        // 获取当前对话的系统提示词
        systemPrompt = repository.getConversationSystemPrompt(conversationId)
        println("DEBUG: DisposableEffect loaded ${messages.size} messages for conversationId: $conversationId")
        chatState = ChatState(
            messages = messages,
            inputText = ""
        )
        println("DEBUG: Updated chatState with new messages, total messages: ${chatState.messages.size}")
        
        onDispose {
            // 清理工作（如果需要）
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "花卷 - $roleName",
                            fontWeight = FontWeight.Bold
                        )
                        if (repository.getDebugMode()) {
                            Text(
                                text = "当前对话ID: ${appState.currentConversationId ?: "default"}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = repository.getSelectedModel(),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                        // 创建新对话
                        scope.launch {
                            val newConversation = repository.createNewConversation(
                                title = "新对话",
                                roleName = "默认助手",
                                systemPrompt = "你是一个AI助手"
                            )
                            
                            // 先保存空消息列表到新对话
                            dbMutex.lock()
                            try {
                                repository.saveMessages(newConversation.id, emptyList())
                            } finally {
                                dbMutex.unlock()
                            }
                            
                            // 然后更新当前对话ID，触发LaunchedEffect重新加载消息
                            appState.currentConversationId = newConversation.id
                            // 注意：这里我们需要通过函数来更新appState.conversations而不是直接赋值
                            appState.conversations = repository.getConversations()
                            
                            // 清空聊天记录
                            chatState = ChatState()
                        }
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
                        // 创建用户消息，使用UUID确保ID唯一性
                        val userMessage = Message(
                            id = java.util.UUID.randomUUID().toString(),
                            text = text,
                            isUser = true,
                            timestamp = Date()
                        )
                        
                        // 更新聊天状态
                        val updatedMessages = chatState.messages + userMessage
                        chatState = chatState.copy(
                            messages = updatedMessages,
                            inputText = ""
                        )
                        
                        // 保存用户消息
                        scope.launch {
                            val conversationId = appState.currentConversationId ?: "default"
                            dbMutex.lock()
                            try {
                                repository.saveMessages(conversationId, updatedMessages)
                            } finally {
                                dbMutex.unlock()
                            }
                            
                            // 更新对话列表中的最后消息
                            dbMutex.lock()
                            try {
                                repository.updateLastMessage(conversationId, text)
                                // 注意：这里我们需要通过函数来更新appState.conversations而不是直接赋值
                                appState.conversations = repository.getConversations()
                            } finally {
                                dbMutex.unlock()
                            }
                        }
                        
                        // 调用AI API获取回复
                        scope.launch {
                            // 使用UUID确保AI消息ID唯一性
                            val aiMessageId = java.util.UUID.randomUUID().toString()

                            // 创建一个初始的AI消息
                            val initialAiMessage = Message(
                                id = aiMessageId,
                                text = "",
                                isUser = false,
                                timestamp = Date()
                            )

                            // 添加初始消息到状态
                            val messagesWithAi = updatedMessages + initialAiMessage
                            chatState = chatState.copy(
                                messages = messagesWithAi
                            )

                            // 保存带AI初始消息的状态
                            val currentConversationId = appState.currentConversationId ?: "default"
                            scope.launch {
                                dbMutex.lock()
                                try {
                                    repository.saveMessages(currentConversationId, messagesWithAi)
                                } finally {
                                    dbMutex.unlock()
                                }
                            }

                            // 获取AI响应
                            try {
                                // 只传递用户消息给AI，排除初始的空AI消息
                                val userMessagesOnly = updatedMessages.filter { it.isUser }
                                // 使用新的方法，传入对话ID以获取系统提示词
                                val aiResponse = repository.getAIResponse(userMessagesOnly, currentConversationId)
                                
                                // 更新消息内容
                                val updatedMessages = chatState.messages.map { message ->
                                    if (message.id == aiMessageId) {
                                        message.copy(text = aiResponse)
                                    } else {
                                        message
                                    }
                                }
                                chatState = chatState.copy(messages = updatedMessages)
                                
                                // 保存所有消息
                                dbMutex.lock()
                                try {
                                    repository.saveMessages(currentConversationId, chatState.messages)
                                } finally {
                                    dbMutex.unlock()
                                }

                                // 更新对话列表中的最后消息为AI回复
                                if (aiResponse.isNotEmpty()) {
                                    dbMutex.lock()
                                    try {
                                        repository.updateLastMessage(currentConversationId, aiResponse)
                                        // 注意：这里我们需要通过函数来更新appState.conversations而不是直接赋值
                                        appState.conversations = repository.getConversations()
                                    } finally {
                                        dbMutex.unlock()
                                    }
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "获取AI回复失败: ${e.message}", Toast.LENGTH_LONG).show()
                                // 更新消息内容为错误信息
                                val updatedMessages = chatState.messages.map { message ->
                                    if (message.id == aiMessageId) {
                                        message.copy(text = "获取回复失败: ${e.message}")
                                    } else {
                                        message
                                    }
                                }
                                chatState = chatState.copy(messages = updatedMessages)

                                // 保存错误消息
                                dbMutex.lock()
                                try {
                                    repository.saveMessages(currentConversationId, updatedMessages)
                                } finally {
                                    dbMutex.unlock()
                                }
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
            repository = repository,
            systemPrompt = systemPrompt,
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        )
    }
}

/**
 * 聊天内容区域
 */
@Composable
fun ChatContentArea(
    messages: List<Message>,
    repository: Repository,
    systemPrompt: String,
    modifier: Modifier = Modifier
) {
    println("DEBUG: ChatContentArea rendering with ${messages.size} messages")
    val scope = rememberCoroutineScope()
    val isDarkTheme = isSystemInDarkTheme()
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp),
        userScrollEnabled = true
    ) {
        // 显示当前消息数量的调试信息
        if (repository.getDebugMode()) {
            item {
                Text(
                    text = "消息数量: ${messages.size}",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        // 显示系统提示词横幅（如果有）
        if (systemPrompt.isNotBlank()) {
            item {
                SystemPromptBanner(systemPrompt = systemPrompt)
            }
        }

        items(messages, key = { message -> message.id }) { message ->
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
                                content = message.text,
                                colors = markdownColor(
                                    text = MaterialTheme.colorScheme.onSurface,
                                    codeBackground = MaterialTheme.colorScheme.secondaryContainer,
                                    codeText = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                typography = markdownTypography(
                                    MaterialTheme.typography.bodyLarge,
                                    MaterialTheme.typography.bodyMedium,
                                    MaterialTheme.typography.headlineSmall,
                                    MaterialTheme.typography.headlineSmall,
                                    MaterialTheme.typography.titleLarge,
                                    MaterialTheme.typography.titleMedium,
                                    MaterialTheme.typography.titleSmall,
                                    MaterialTheme.typography.bodyLarge
                                )
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
                        val context = LocalContext.current
                        val clipboardManager = LocalClipboardManager.current
                        
                        IconButton(
                            onClick = { 
                                isLiked = !isLiked
                                if (isLiked) {
                                    Toast.makeText(context, "已点赞该回复", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "已取消点赞", Toast.LENGTH_SHORT).show()
                                }
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
                                if (isDisliked) {
                                    Toast.makeText(context, "已点踩该回复", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "已取消点踩", Toast.LENGTH_SHORT).show()
                                }
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
                                clipboardManager.setText(AnnotatedString(message.text))
                                Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
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
                                if (isFavorited) {
                                    Toast.makeText(context, "已收藏该回复", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "已取消收藏", Toast.LENGTH_SHORT).show()
                                }
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
                                content = message.text,
                                colors = markdownColor(
                                    text = MaterialTheme.colorScheme.onPrimary,
                                    codeBackground = MaterialTheme.colorScheme.primaryContainer,
                                    codeText = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                typography = markdownTypography(
                                    MaterialTheme.typography.bodyLarge,
                                    MaterialTheme.typography.bodyMedium,
                                    MaterialTheme.typography.headlineSmall,
                                    MaterialTheme.typography.headlineSmall,
                                    MaterialTheme.typography.titleLarge,
                                    MaterialTheme.typography.titleMedium,
                                    MaterialTheme.typography.titleSmall,
                                    MaterialTheme.typography.bodyLarge
                                )
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

@Composable
fun SystemPromptBanner(systemPrompt: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "系统提示",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = systemPrompt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
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
            .imePadding() // 自动适应输入法高度
            .navigationBarsPadding() // 自动适应导航栏高度
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
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
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
            .navigationBarsPadding() // 适配导航栏
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
                .padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurface
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