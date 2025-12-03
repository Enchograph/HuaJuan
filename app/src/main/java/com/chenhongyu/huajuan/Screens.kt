package com.chenhongyu.huajuan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Check
import kotlinx.coroutines.delay
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val currentConversationId: Long? = 1
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
 * 侧边栏导航
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SideDrawer(
    onChatPageSelected: () -> Unit = {},
    onSettingPageSelected: () -> Unit = {},
    conversations: List<Conversation> = emptyList(),
    drawerWidth: androidx.compose.ui.unit.Dp = 300.dp
) {
    val scope = rememberCoroutineScope()
    
    ModalDrawerSheet(
        modifier = Modifier.width(drawerWidth),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        // 搜索框
        TextField(
            value = "",
            onValueChange = {},
            placeholder = { Text("搜索...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(Color.LightGray.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(50.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            )
        )

        // 新建对话按钮
        Button(
            onClick = {
                println("新建对话按钮被点击")
                // 在实际应用中，这里会创建一个新的对话
                onChatPageSelected()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("新建对话")
        }

        // 历史对话列表
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(conversations) { conversation ->
                ListItem(
                    headlineContent = { Text(conversation.title) },
                    supportingContent = { 
                        Text("${conversation.lastMessage} · ${formatConversationTime(conversation.timestamp)}") 
                    },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Blue.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(conversation.title.take(1), color = Color.White)
                        }
                    },
                    modifier = Modifier.clickable { 
                        println("选择了对话: ${conversation.title}")
                        onChatPageSelected() 
                    }
                )
            }
        }

        // 底部用户栏
        Divider()
        ListItem(
            headlineContent = { Text("用户名") },
            supportingContent = { Text("个性签名") },
            leadingContent = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    Text("U", color = Color.White)
                }
            },
            trailingContent = {
                IconButton(onClick = {
                    println("点击了设置按钮")
                    onSettingPageSelected()
                }) {
                    Icon(Icons.Default.Settings, contentDescription = "设置")
                }
            },
            modifier = Modifier.clickable { 
                println("点击了底部用户栏")
                onSettingPageSelected() 
            }
        )
    }
}

/**
 * 聊天界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(drawerState: DrawerState) {
    val scope = rememberCoroutineScope()
    var isExpanded by remember { mutableStateOf(false) }
    var chatState by remember { mutableStateOf(ChatState()) }
    
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
                            text = "当前AI模型",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "打开侧边栏")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        /* 新建对话 */
                        println("新建对话按钮被点击")
                        // 清空聊天记录
                        chatState = ChatState()
                    }) {
                        Icon(Icons.Default.Create, contentDescription = "新建对话")
                    }
                }
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
                        
                        // 模拟AI回复（实际应该调用AI API）
                        // 这里使用协程延迟模拟网络请求
                        scope.launch {
                            kotlinx.coroutines.delay(1000) // 模拟1秒延迟
                            val aiMessage = Message(
                                id = System.currentTimeMillis() + 1,
                                text = "这是AI的回复：$text",
                                isUser = false,
                                timestamp = Date()
                            )
                            chatState = chatState.copy(
                                messages = chatState.messages + userMessage + aiMessage
                            )
                        }
                    }
                }
            )
        }
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
    LazyColumn(
        modifier = modifier
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(messages) { message ->
            if (!message.isUser) {
                // AI回复气泡
                Column {
                    Text(
                        text = message.text,
                        color = Color.Black
                    )
                    
                    // 交互按钮
                    Row(
                        modifier = Modifier
                            .padding(top = 8.dp),
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
                                .background(Color.White, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                contentDescription = "点赞",
                                tint = if (isLiked) Color.Blue else Color.Gray
                            )
                        }
                        
                        IconButton(
                            onClick = { 
                                isDisliked = !isDisliked
                                println("${if (isDisliked) "已点踩" else "取消点踩"}消息")
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isDisliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                                contentDescription = "点踩",
                                tint = if (isDisliked) Color.Blue else Color.Gray
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
                                .background(Color.White, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isCopied) Icons.Filled.Check else Icons.Outlined.ContentCopy,
                                contentDescription = if (isCopied) "已复制" else "复制",
                                tint = if (isCopied) Color.Green else Color.Blue
                            )
                        }
                        
                        IconButton(
                            onClick = { 
                                isFavorited = !isFavorited
                                println("${if (isFavorited) "已收藏" else "取消收藏"}消息")
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Outlined.Favorite,
                                contentDescription = "收藏",
                                tint = if (isFavorited) Color.Red else Color.Blue
                            )
                        }
                    }
                    
                    Text(
                        text = formatTime(message.timestamp),
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier
                            .padding(top = 8.dp)
                    )
                }
            } else {
                // 用户发送气泡
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = message.text,
                        color = Color.White,
                        modifier = Modifier
                            .background(Color.Blue, RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    )
                    
                    Text(
                        text = formatTime(message.timestamp),
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier
                            .padding(top = 4.dp)
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
            IconButton(onClick = { 
                /* 相机功能 */
                println("相机按钮被点击")
            }) {
                Icon(Icons.Default.Camera, contentDescription = "相机")
            }
            
            OutlinedTextField(
                value = text,
                onValueChange = { 
                    text = it
                    onInputTextChanged(it)
                },
                placeholder = { Text("输入消息...") },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(24.dp),
                singleLine = true
            )
            
            IconButton(onClick = { 
                onSendMessage(text)
                text = "" // 清空输入框
            }) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "发送"
                )
            }
            
            IconButton(onClick = { onExpandChange(!isExpanded) }) {
                Icon(
                    if (isExpanded) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = if (isExpanded) "关闭" else "添加"
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
                        .background(Color.LightGray, CircleShape)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "相机")
                }
                Text("相机", fontSize = 12.sp)
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
                        .background(Color.LightGray, CircleShape)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "相册")
                }
                Text("相册", fontSize = 12.sp)
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
                        .background(Color.LightGray, CircleShape)
                ) {
                    Icon(Icons.Default.InsertDriveFile, contentDescription = "文件")
                }
                Text("文件", fontSize = 12.sp)
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
                        .background(Color.LightGray, CircleShape)
                ) {
                    Icon(Icons.Default.Call, contentDescription = "打电话")
                }
                Text("通话", fontSize = 12.sp)
            }
        }
        
        // 图片网格
        Text(
            text = "最近图片",
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
                                .background(Color.LightGray, RoundedCornerShape(8.dp))
                                .clickable { 
                                    println("选择了图片: 行$rowIndex, 列$columnIndex")
                                }
                        )
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
fun SettingScreen() {
    var darkMode by remember { mutableStateOf(false) }
    var useCloudModel by remember { mutableStateOf(true) }
    var serviceProvider by remember { mutableStateOf("OpenAI") }
    var customApiUrl by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf("GPT-4") }
    var userInfo by remember { mutableStateOf(UserInfo()) }
    var showEditDialog by remember { mutableStateOf(false) }
    
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
                    IconButton(onClick = { 
                        /* 返回功能 */
                        println("返回按钮被点击")
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 用户信息部分
            item {
                Card {
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
                                    .clip(CircleShape)
                                    .background(Color.Gray),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(userInfo.avatar, color = Color.White)
                            }
                            
                            Spacer(Modifier.width(16.dp))
                            
                            Column {
                                Text(
                                    text = userInfo.username,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = userInfo.signature,
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        
                        Button(
                            onClick = { 
                                showEditDialog = true
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("编辑")
                        }
                    }
                }
            }
            
            // 主题设置
            item {
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "外观",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        
                        Spacer(Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("深色模式")
                            Switch(
                                checked = darkMode,
                                onCheckedChange = { darkMode = it }
                            )
                        }
                    }
                }
            }
            
            // 模型设置
            item {
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "AI模型设置",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        
                        Spacer(Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("使用云端模型")
                            Switch(
                                checked = useCloudModel,
                                onCheckedChange = { useCloudModel = it }
                            )
                        }
                        
                        if (useCloudModel) {
                            // 云端模型设置
                            ServiceProviderSelector(
                                serviceProvider = serviceProvider,
                                onServiceProviderChange = { serviceProvider = it },
                                customApiUrl = customApiUrl,
                                onCustomApiUrlChange = { customApiUrl = it }
                            )
                            
                            OutlinedTextField(
                                value = apiKey,
                                onValueChange = { apiKey = it },
                                label = { Text("API密钥") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            ModelSelector(
                                selectedModel = selectedModel,
                                onModelChange = { selectedModel = it }
                            )
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
        title = { Text("编辑用户信息") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("用户名") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = signature,
                    onValueChange = { signature = it },
                    label = { Text("个性签名") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = avatar,
                    onValueChange = { avatar = it },
                    label = { Text("头像字符") },
                    modifier = Modifier.fillMaxWidth()
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
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
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
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(serviceProvider)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                serviceProviders.forEach { provider ->
                    DropdownMenuItem(
                        text = { Text(provider) },
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
                    .padding(top = 8.dp)
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
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selectedModel)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                models.forEach { model ->
                    DropdownMenuItem(
                        text = { Text(model) },
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
    /* // 模拟本地模型列表
    val localModels = remember {
        mutableStateListOf(
            LocalModel("1", "Qwen 7B", "3.5GB", true),
            LocalModel("2", "Llama 2 7B", "12GB", true),
            LocalModel("3", "Mistral 7B", "4.1GB", false)
        )
    } */
    
    var showDownloadDialog by remember { mutableStateOf(false) }
    
    /* if (showDownloadDialog) {
        DownloadModelDialog(
            onDismiss = { showDownloadDialog = false },
            onDownload = { model ->
                // 模拟下载过程
                /* val newModel = model.copy(isDownloaded = true)
                localModels.add(newModel) */
                showDownloadDialog = false
            }
        )
    } */
    
    Column {
        Text(
            text = "本地模型",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        
        // 已下载的模型列表
        /* localModels.filter { it.isDownloaded }.forEach { model ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                ListItem(
                    headlineContent = { Text(model.name) },
                    supportingContent = { Text(model.size) },
                    trailingContent = {
                        RadioButton(
                            selected = true,
                            onClick = { 
                                /* 选择模型 */
                                println("选择了模型: ${model.name}")
                            }
                        )
                    }
                )
            }
        } */
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            ListItem(
                headlineContent = { Text("Qwen 7B") },
                supportingContent = { Text("已下载") },
                trailingContent = {
                    RadioButton(
                        selected = true,
                        onClick = { 
                            /* 选择模型 */
                            println("选择了Qwen 7B模型")
                        }
                    )
                }
            )
        }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            ListItem(
                headlineContent = { Text("Llama 2 7B") },
                supportingContent = { Text("已下载") },
                trailingContent = {
                    RadioButton(
                        selected = false,
                        onClick = { 
                            /* 选择模型 */
                            println("选择了Llama 2 7B模型")
                        }
                    )
                }
            )
        }
        
        // 可下载的模型列表
        /* localModels.filter { !it.isDownloaded }.forEach { model ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                ListItem(
                    headlineContent = { Text(model.name) },
                    supportingContent = { Text(model.size) },
                    trailingContent = {
                        Button(
                            onClick = { 
                                // 开始下载模型
                                println("开始下载模型: ${model.name}")
                            }
                        ) {
                            Text("下载")
                        }
                    }
                )
            }
        } */
        
        // 添加新模型按钮
        OutlinedButton(
            onClick = { 
                /* 下载新模型 */
                println("添加模型按钮被点击")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("添加模型")
        }
    }
}