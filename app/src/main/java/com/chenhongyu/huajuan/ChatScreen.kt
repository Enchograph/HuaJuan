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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
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