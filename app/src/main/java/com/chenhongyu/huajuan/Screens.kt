package com.chenhongyu.huajuan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

/**
 * 侧边栏导航
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SideDrawer(
    onChatPageSelected: () -> Unit = {},
    onSettingPageSelected: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    
    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
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
            onClick = onChatPageSelected,
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
            items(10) { index ->
                ListItem(
                    headlineContent = { Text("历史对话 $index") },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Blue.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text((index + 1).toString(), color = Color.White)
                        }
                    },
                    modifier = Modifier.clickable { onChatPageSelected() }
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
                IconButton(onClick = onSettingPageSelected) {
                    Icon(Icons.Default.Settings, contentDescription = "设置")
                }
            },
            modifier = Modifier.clickable { onSettingPageSelected() }
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
                    IconButton(onClick = { /* 新建对话 */ }) {
                        Icon(Icons.Default.Create, contentDescription = "新建对话")
                    }
                }
            )
        },
        bottomBar = {
            BottomInputArea(
                isExpanded = isExpanded,
                onExpandChange = { isExpanded = it }
            )
        }
    ) { paddingValues ->
        ChatContentArea(
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
fun ChatContentArea(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // AI回复气泡
            Column {
                Text(
                    text = "你好！我是花卷AI助手，有什么我可以帮你的吗？",
                    color = Color.Black
                )
                
                // 交互按钮
                Row(
                    modifier = Modifier
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { /* 点赞 */ },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.ThumbUp,
                            contentDescription = "点赞",
                            tint = Color.Blue
                        )
                    }
                    
                    IconButton(
                        onClick = { /* 点踩 */ },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.ThumbDown,
                            contentDescription = "点踩",
                            tint = Color.Blue
                        )
                    }
                    
                    IconButton(
                        onClick = { /* 复制 */ },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "复制",
                            tint = Color.Blue
                        )
                    }
                    
                    IconButton(
                        onClick = { /* 收藏 */ },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = "收藏",
                            tint = Color.Blue
                        )
                    }
                }
                
                Text(
                    text = "今天 10:30",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier
                        .padding(top = 8.dp)
                )
            }
        }
        
        item {
            // 用户发送气泡
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "你好，请介绍一下你能做什么？",
                    color = Color.White,
                    modifier = Modifier
                        .background(Color.Blue, RoundedCornerShape(16.dp))
                        .padding(12.dp)
                )
            }
            
            Text(
                text = "今天 10:28",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
    }
}

/**
 * 底部输入区域
 */
@Composable
fun BottomInputArea(
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit
) {
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
            IconButton(onClick = { /* 相机 */ }) {
                Icon(Icons.Default.Camera, contentDescription = "相机")
            }
            
            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("输入消息...") },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(24.dp)
            )
            
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
                    onClick = { /* 相机 */ },
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
                    onClick = { /* 相册 */ },
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
                    onClick = { /* 文件 */ },
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
                    onClick = { /* 打电话 */ },
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
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = { /* 返回 */ }) {
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
                                Text("U", color = Color.White)
                            }
                            
                            Spacer(Modifier.width(16.dp))
                            
                            Column {
                                Text(
                                    text = "用户名",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = "个性签名",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        
                        Button(
                            onClick = { /* 编辑用户信息 */ },
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
    Column {
        Text(
            text = "本地模型",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        
        // 已下载的模型列表
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
                        onClick = { /* 选择模型 */ }
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
                        onClick = { /* 选择模型 */ }
                    )
                }
            )
        }
        
        // 添加新模型按钮
        OutlinedButton(
            onClick = { /* 下载新模型 */ },
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