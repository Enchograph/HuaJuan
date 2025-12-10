package com.chenhongyu.huajuan

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
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
import com.chenhongyu.huajuan.data.UserInfo
import com.chenhongyu.huajuan.data.ModelDataProvider
import com.chenhongyu.huajuan.data.ModelInfo
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.chenhongyu.huajuan.data.Message // 导入Message类

/**
 * 设置界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    repository: Repository, 
    darkModeState: MutableState<Boolean>,
    onMenuClick: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    var darkMode by remember { mutableStateOf(darkModeState.value) }
    var useCloudModel by remember { mutableStateOf(repository.getUseCloudModel()) }
    var serviceProvider by remember { mutableStateOf(repository.getServiceProvider()) }
    var customApiUrl by remember { mutableStateOf(repository.getCustomApiUrl()) }
    var apiKey by remember { mutableStateOf(repository.getApiKeyForProvider(serviceProvider)) }
    var selectedModel by remember { 
        mutableStateOf(
            repository.getSelectedModelForProvider(serviceProvider).takeIf { it.isNotEmpty() } 
                ?: "GPT-3.5 Turbo"
        ) 
    }
    var userInfo by remember { mutableStateOf(UserInfo()) }
    var showEditDialog by remember { mutableStateOf(false) }
    var debugMode by remember { mutableStateOf<Boolean>(repository.getDebugMode()) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 添加调试输出
    LaunchedEffect(serviceProvider) {
        println("当前选中的服务提供商: $serviceProvider")
        // 当服务商变更时，更新API密钥和模型选择状态
        apiKey = repository.getApiKeyForProvider(serviceProvider)
        selectedModel = repository.getSelectedModelForProvider(serviceProvider).takeIf { it.isNotEmpty() } 
            ?: "GPT-3.5 Turbo"
    }
    
    LaunchedEffect(selectedModel) {
        println("当前选中的模型: $selectedModel")
    }
    
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
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Outlined.Menu, contentDescription = "打开侧边栏")
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
            
            // 调试设置
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
                            text = "调试设置",
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
                                    text = "调试模式",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (debugMode) "已启用" else "已禁用",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = debugMode,
                                onCheckedChange = { 
                                    debugMode = it
                                    repository.setDebugMode(it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                        
                        if (debugMode) {
                            Spacer(Modifier.height(16.dp))
                            var showClearConfirm by remember { mutableStateOf(false) }

                            // 清空对话数据库按钮
                            Button(
                                onClick = {
                                    scope.launch {
                                        try {
                                            // 获取所有对话
                                            val conversations = repository.getConversations()
                                            // 删除所有对话（会级联删除所有消息）
                                            conversations.forEach { conversation -> 
                                                repository.deleteConversation(conversation.id)
                                            }
                                            Toast.makeText(context, "已清空所有对话", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "清空对话失败: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            ) {
                                Text("清空对话数据库")
                            }

                            // 新增：删除数据库但保留API/密钥
                            Button(
                                onClick = { showClearConfirm = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            ) {
                                Text("删除数据库（保留 API 与密钥）")
                            }

                            if (showClearConfirm) {
                                AlertDialog(
                                    onDismissRequest = { showClearConfirm = false },
                                    title = { Text("确认删除数据库") },
                                    text = { Text("该操作会删除除 API/密钥 之外的所有本地数据，无法恢复。确定继续吗？") },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            showClearConfirm = false
                                            scope.launch {
                                                try {
                                                    repository.clearUserDataExceptApiKeys()
                                                    Toast.makeText(context, "已删除本地数据（保留 API & 密钥）", Toast.LENGTH_SHORT).show()
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "删除失败: ${e.message}", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }) { Text("确认") }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showClearConfirm = false }) { Text("取消") }
                                    }
                                )
                            }

                            // 一键新建一百条对话按钮
                            Button(
                                onClick = {
                                    scope.launch {
                                        try {
                                            for (i in 1..100) {
                                                repository.createNewConversation(
                                                    title = "测试对话 #$i",
                                                    roleName = "默认助手",
                                                    systemPrompt = "你是一个AI助手"
                                                )
                                            }
                                            Toast.makeText(context, "已创建100条测试对话", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "创建对话失败: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("一键新建一百条对话")
                            }
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
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ServiceProviderSelector(
                                    serviceProvider = serviceProvider,
                                    onServiceProviderChange = { 
                                        println("ServiceProviderSelector 回调被调用，新值: $it")
                                        serviceProvider = it
                                        repository.setServiceProvider(it)
                                        
                                        // 更新API密钥和模型选择状态
                                        apiKey = repository.getApiKeyForProvider(it)
                                        selectedModel = repository.getSelectedModelForProvider(it).takeIf { model -> model.isNotEmpty() } 
                                            ?: "GPT-3.5 Turbo"
                                        
                                        // 当服务提供商改变时，重置选中的模型
                                        val modelDataProvider = ModelDataProvider(repository)
                                        val models = modelDataProvider.getModelListForProvider(it)
                                        if (models.isNotEmpty()) {
                                            val firstModel = models.first().displayName
                                            selectedModel = firstModel
                                            repository.setSelectedModelForProvider(it, firstModel)
                                        }
                                    },
                                    customApiUrl = customApiUrl,
                                    onCustomApiUrlChange = { 
                                        customApiUrl = it
                                        repository.setCustomApiUrl(it)
                                    },
                                    repository = repository
                                )
                                
                                // Debug-only quick switch to 硅基流动, set hard-coded key invisibly (not shown in UI)
                                if (debugMode) {
                                    Spacer(Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            val hardCodedKey = "sk_test_silicon_hardcoded_key_123456"
                                            repository.setServiceProvider("硅基流动")
                                            repository.setApiKeyForProvider("硅基流动", hardCodedKey)
                                            serviceProvider = "硅基流动"
                                            apiKey = repository.getApiKeyForProvider("硅基流动")

                                            // Set default model if available
                                            val modelDataProvider = ModelDataProvider(repository)
                                            val models = modelDataProvider.getModelListForProvider("硅基流动")
                                            if (models.isNotEmpty()) {
                                                val firstModel = models.first().displayName
                                                selectedModel = firstModel
                                                repository.setSelectedModelForProvider("硅基流动", firstModel)
                                            }

                                            Toast.makeText(context, "已切换到 硅基流动（密钥已隐式设置）", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    ) {
                                        Text("切换到 硅基流动 (测试)")
                                    }
                                }

                                var passwordVisibility by remember { mutableStateOf(false) }

                                // 如果当前服务提供商是 硅基流动，则不在前端展示密钥输入框（密钥由代码写死）
                                if (serviceProvider != "硅基流动") {
                                    OutlinedTextField(
                                        value = apiKey,
                                        onValueChange = {
                                            apiKey = it
                                            repository.setApiKeyForProvider(serviceProvider, it)
                                        },
                                        label = { Text("API密钥") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 16.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        visualTransformation = if (passwordVisibility) {
                                            androidx.compose.ui.text.input.VisualTransformation.None
                                        } else {
                                            androidx.compose.ui.text.input.PasswordVisualTransformation()
                                        },
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
                                        ),
                                        trailingIcon = {
                                            val image = if (passwordVisibility) {
                                                Icons.Filled.Visibility
                                            } else {
                                                Icons.Filled.VisibilityOff
                                            }

                                            IconButton(onClick = {
                                                passwordVisibility = !passwordVisibility
                                            }) {
                                                Icon(
                                                    imageVector = image,
                                                    contentDescription = if (passwordVisibility) {
                                                        "隐藏密码"
                                                    } else {
                                                        "显示密码"
                                                    }
                                                )
                                            }
                                        }
                                    )
                                } else {
                                    Text(
                                        text = "使用内置的硅基流动密钥（不会在界面展示）",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 16.dp)
                                    )
                                }

                                ModelSelector(
                                    serviceProvider = serviceProvider,
                                    selectedModel = selectedModel,
                                    onModelChange = { 
                                        selectedModel = it
                                        repository.setSelectedModelForProvider(serviceProvider, it)
                                    },
                                    repository = repository // 传递 repository 参数
                                )
                            }
                            
                            // 添加测试连接按钮
                            Button(
                                onClick = {
                                    // 在实际应用中，这里会测试API连接
                                    scope.launch {
                                        val testMessage = Message(
                                            id = "test-message",
                                            text = "输出“测试成功”四个字符，不要输出任何多余的格式和文字。",
                                            isUser = true,
                                            timestamp = java.util.Date()
                                        )
                                        // 使用默认对话ID进行测试
                                        val response = repository.getAIResponse(listOf(testMessage), "default")
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
                    }
                }
            }
        }
    }
}

@Composable
fun EditUserInfoDialog(
    userInfo: UserInfo,
    onUserInfoChange: (UserInfo) -> Unit,
    onDismiss: () -> Unit
) {
    var username by remember { mutableStateOf(userInfo.username) }
    var signature by remember { mutableStateOf(userInfo.signature) }
    var avatar by remember { mutableStateOf(userInfo.avatar) }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "编辑用户信息",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
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
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("取消")
                    }
                    
                    Spacer(Modifier.width(8.dp))
                    
                    Button(
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}

/**
 * 服务提供商选择器（符合Material Design规范的新版本）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceProviderSelector(
    serviceProvider: String,
    onServiceProviderChange: (String) -> Unit,
    customApiUrl: String,
    onCustomApiUrlChange: (String) -> Unit,
    repository: Repository
) {
    // 根据模型列表.md更新的服务提供商列表
    val modelDataProvider = ModelDataProvider(repository)
    val serviceProviders = modelDataProvider.getAllServiceProviders()
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var providerToDelete by remember { mutableStateOf("") }
    
    Column {
        Text(
            text = "服务提供商",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
        
        // 使用固定高度的现代化选择列表
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp) // 设置固定高度以避免嵌套滚动问题
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(12.dp)
                )
                .clip(RoundedCornerShape(12.dp))
        ) {
            items(serviceProviders) { provider ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onServiceProviderChange(provider)
                        }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (provider == serviceProvider),
                            onClick = { onServiceProviderChange(provider) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = provider,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // 显示删除按钮（除了预定义的服务商）
                        if (!ModelDataProvider.predefinedServiceProviders.containsKey(provider)) {
                            IconButton(
                                onClick = {
                                    providerToDelete = provider
                                    showDeleteConfirm = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "删除服务商",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                
                // 添加分割线（除了最后一个元素）
                if (provider != serviceProviders.last()) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 56.dp)
                    )
                }
            }
            
            item {
                // 添加新服务商按钮
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showAddDialog = true
                        }
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加服务商",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = "新建服务商",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
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
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            
            Text(
                text = "提示：请输入完整的API端点URL，例如 https://api.openai.com/v1/chat/completions",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
    
    // 添加服务商对话框
    if (showAddDialog) {
        var providerName by remember { mutableStateOf("") }
        var providerUrl by remember { mutableStateOf("") }
        val focusManager = LocalFocusManager.current
        
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = "添加新服务商",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = providerName,
                        onValueChange = { providerName = it },
                        label = { Text("服务商名称") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    
                    OutlinedTextField(
                        value = providerUrl,
                        onValueChange = { providerUrl = it },
                        label = { Text("API基址") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                            }
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (providerName.isNotBlank() && providerUrl.isNotBlank()) {
                            repository.addCustomServiceProvider(providerName, providerUrl)
                            showAddDialog = false
                            providerName = ""
                            providerUrl = ""
                        }
                    },
                    enabled = providerName.isNotBlank() && providerUrl.isNotBlank(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("添加")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("取消")
                }
            }
        )
    }
    
    // 删除确认对话框
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = "确认删除",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "确定要删除服务商 \"$providerToDelete\" 吗？此操作将同时删除该服务商下的所有自定义模型。",
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        repository.removeCustomServiceProvider(providerToDelete)
                        // 如果删除的是当前选中的服务商，则切换到默认服务商
                        if (serviceProvider == providerToDelete) {
                            onServiceProviderChange("硅基流动")
                        }
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 模型选择器（符合Material Design规范的新版本）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelector(
    serviceProvider: String,
    selectedModel: String,
    onModelChange: (String) -> Unit,
    repository: Repository
) {
    val modelDataProvider = ModelDataProvider(repository)
    val models = modelDataProvider.getModelListForProvider(serviceProvider)
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var modelToDelete by remember { mutableStateOf("") }
    
    Column {
        Text(
            text = "选择模型",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
        
        // 使用固定高度的现代化选择列表
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp) // 设置固定高度以避免嵌套滚动问题
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(12.dp)
                )
                .clip(RoundedCornerShape(12.dp))
        ) {
            items(models) { model ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onModelChange(model.displayName)
                        }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (model.displayName == selectedModel),
                            onClick = { onModelChange(model.displayName) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = model.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // 显示删除按钮（除了预定义的模型）
                        val isPredefinedProvider = ModelDataProvider.predefinedServiceProviders.containsKey(serviceProvider)
                        val isPredefinedModel = isPredefinedProvider && 
                            ModelDataProvider.predefinedServiceProviders[serviceProvider]?.models?.any { it.apiCode == model.apiCode } == true
                        
                        if (!isPredefinedModel) {
                            IconButton(
                                onClick = {
                                    modelToDelete = model.displayName
                                    showDeleteConfirm = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "删除模型",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                
                // 添加分割线（除了最后一个元素）
                if (model != models.last()) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 56.dp)
                    )
                }
            }
            
            item {
                // 添加新模型按钮
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showAddDialog = true
                        }
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加模型",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = "新建模型",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        
        if (models.isEmpty()) {
            Text(
                text = "该服务提供商暂无可用模型",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
    }
    
    // 添加模型对话框
    if (showAddDialog) {
        var modelCode by remember { mutableStateOf("") }
        var modelName by remember { mutableStateOf("") }
        val focusManager = LocalFocusManager.current
        
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = "添加新模型",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = modelCode,
                        onValueChange = { modelCode = it },
                        label = { Text("模型编码") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    
                    OutlinedTextField(
                        value = modelName,
                        onValueChange = { modelName = it },
                        label = { Text("模型名称") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                            }
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (modelCode.isNotBlank() && modelName.isNotBlank()) {
                            repository.addCustomModelToProvider(serviceProvider, modelName, modelCode)
                            showAddDialog = false
                            modelCode = ""
                            modelName = ""
                        }
                    },
                    enabled = modelCode.isNotBlank() && modelName.isNotBlank(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("添加")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("取消")
                }
            }
        )
    }
    
    // 删除确认对话框
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = "确认删除",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "确定要删除模型 \"$modelToDelete\" 吗？",
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        repository.removeCustomModelFromProvider(serviceProvider, modelToDelete)
                        // 如果删除的是当前选中的模型，则切换到第一个模型
                        if (selectedModel == modelToDelete) {
                            val firstModel = models.firstOrNull { it.displayName != modelToDelete }?.displayName ?: ""
                            if (firstModel.isNotEmpty()) {
                                onModelChange(firstModel)
                            }
                        }
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("取消")
                }
            }
        )
    }
}
