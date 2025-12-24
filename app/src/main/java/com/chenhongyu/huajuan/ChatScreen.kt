@file:Suppress("unused", "DEPRECATION")

package com.chenhongyu.huajuan

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import java.util.Date
import kotlinx.coroutines.delay
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.chenhongyu.huajuan.ui.theme.HuaJuanTheme
import androidx.compose.ui.window.Dialog
import com.chenhongyu.huajuan.data.Message
import com.chenhongyu.huajuan.data.ChatState
import com.chenhongyu.huajuan.data.AppState
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlin.math.max
import kotlin.math.roundToInt
import com.chenhongyu.huajuan.stream.ChatEvent
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import com.chenhongyu.huajuan.utils.ThinkTagProcessor
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.graphics.Color
import coil3.compose.AsyncImage
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.foundation.lazy.LazyRow
import com.chenhongyu.huajuan.utils.getImageList
import com.chenhongyu.huajuan.components.ExpandedInputArea
import com.chenhongyu.huajuan.components.ExpandedInputSquareButton

/**
 * 图片数据类
 */
data class ImageItem(
    val id: String,
    val uri: String,
    val displayName: String,
    val dateAdded: Long
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
    repository: Repository,
    onOpenImageSelector: () -> Unit = {}
) {
    println("DEBUG: ChatScreen recomposed with currentConversationId: ${appState.currentConversationId}")
    val scope = rememberCoroutineScope()
    // Editor state: when non-null, show the AI creation editor for that message
    var editorMessage by remember { mutableStateOf<com.chenhongyu.huajuan.data.Message?>(null) }
    var isExpanded by remember { mutableStateOf(false) }
    var selectedImageUris by remember { mutableStateOf<List<String>>(emptyList()) }
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
    // 控制编辑系统提示词对话框显示
    var showEditPromptDialog by remember { mutableStateOf(false) }

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
        //println("DEBUG: DisposableEffect loaded ${messages.size} messages for conversationId: $conversationId")
        chatState = ChatState(
            messages = messages,
            inputText = ""
        )
        //println("DEBUG: Updated chatState with new messages, total messages: ${chatState.messages.size}")
        
        onDispose {
            // 清理工作（如果需要）
        }
    }
    
    val listState = rememberLazyListState()
    var pendingNewChunks by remember { mutableStateOf(0) }

    // Improved auto-scroll control
    val density = LocalDensity.current
    val BOTTOM_THRESHOLD_DP = 48.dp
    val BOTTOM_THRESHOLD_PX = with(density) { BOTTOM_THRESHOLD_DP.toPx().roundToInt() }
    val USER_SCROLL_GRACE_MS = 1200L
    val LAYOUT_STABILIZE_MS = 350L

    var isAutoScrolling by remember { mutableStateOf(false) }
    var lastUserScrollTime by remember { mutableStateOf(0L) }
    var ignoreAutoScrollUntil by remember { mutableStateOf(0L) }
    var autoScrollJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // Detect user-initiated scrolls and cancel auto-scroll if user intervenes
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { inProgress ->
                if (inProgress) {
                    // If a user scroll starts while an auto-scroll coroutine is running, cancel it
                    if (isAutoScrolling) {
                        autoScrollJob?.cancel()
                        autoScrollJob = null
                        isAutoScrolling = false
                    }
                    lastUserScrollTime = System.currentTimeMillis()
                }
            }
    }

    // 函数：更新消息的think可见性状态
    fun updateMessageThinkVisibility(messageId: String, showThink: Boolean) {
        val updatedMessages = chatState.messages.map { message ->
            if (message.id == messageId) message.copy(showThink = showThink) else message
        }
        chatState = chatState.copy(messages = updatedMessages)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = roleName,
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
                    // 编辑系统提示词按钮
                    IconButton(onClick = { showEditPromptDialog = true }) {
                        Icon(Icons.Outlined.Settings, contentDescription = "会话设置")
                    }
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
                    val imageUris = selectedImageUris
                    if (text.isNotBlank() || imageUris.isNotEmpty()) {
                        // 创建用户消息，使用UUID确保ID唯一性
                        val userMessage = Message(
                            id = java.util.UUID.randomUUID().toString(),
                            text = text,
                            isUser = true,
                            timestamp = Date(),
                            imageUris = imageUris // 添加选中的图片
                        )
                        
                        // 更新聊天状态
                        val updatedMessages = chatState.messages + userMessage
                        chatState = chatState.copy(
                            messages = updatedMessages,
                            inputText = ""
                        )
                        
                        // 清空选中的图片
                        selectedImageUris = emptyList()
                        
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
                                val userMessagesOnly = updatedMessages.filter { it.isUser }
                                // Collect stream and append chunks
                                repository.streamAIResponse(userMessagesOnly, currentConversationId).collect { event ->
                                    val eventTime = System.currentTimeMillis()
                                    println("UI-STREAM-DEBUG: received event=${event.javaClass.simpleName} at=${eventTime} thread=${Thread.currentThread().name}")
                                    when (event) {
                                        is ChatEvent.Chunk -> {
                                            println("UI-STREAM-DEBUG: chunk textPreview='${event.text.take(200)}' at=${System.currentTimeMillis()}")
                                            val updatedMessages2 = chatState.messages.map { message ->
                                                if (message.id == aiMessageId) {
                                                    // 检查新文本块是否包含think标签的开始
                                                    val newText = message.text + event.text
                                                    // 如果AI消息包含think标签，设置默认显示think内容
                                                    val hasThinkBefore = ThinkTagProcessor.containsThinkTag(message.text)
                                                    val hasThinkAfter = ThinkTagProcessor.containsThinkTag(newText)
                                                    
                                                    val newShowThink = if (!hasThinkBefore && hasThinkAfter) {
                                                        // 首次发现think标签时，显示think内容
                                                        true
                                                    } else {
                                                        message.showThink // 保持当前状态
                                                    }
                                                    
                                                    message.copy(text = newText, showThink = newShowThink)
                                                } else message
                                            }
                                            chatState = chatState.copy(messages = updatedMessages2)

                                            // Auto-scroll if user is at (or near) bottom
                                            val layoutInfo = listState.layoutInfo
                                            val total = chatState.messages.size

                                            // Robust pixel-based "at bottom" detection
                                            val visible = layoutInfo.visibleItemsInfo
                                            val isAtBottom = if (visible.isEmpty()) {
                                                true
                                            } else {
                                                val last = visible.lastOrNull()!!
                                                val indexOk = last.index >= total - 1
                                                val pixelOk = (last.offset + last.size) >= (layoutInfo.viewportEndOffset - BOTTOM_THRESHOLD_PX)
                                                indexOk && pixelOk
                                            }

                                            val now = System.currentTimeMillis()
                                            val userScrolledRecently = now - lastUserScrollTime < USER_SCROLL_GRACE_MS
                                            val layoutStabilizing = now < ignoreAutoScrollUntil

                                            if (isAtBottom && !userScrolledRecently && !isAutoScrolling && !layoutStabilizing) {
                                                // Launch a cancelable auto-scroll job
                                                autoScrollJob = scope.launch {
                                                    try {
                                                        isAutoScrolling = true
                                                        listState.animateScrollToItem(max(0, total - 1))
                                                    } catch (e: Exception) {
                                                        // animation cancelled or failed; ignore
                                                        println("UI-STREAM-DEBUG: auto-scroll cancelled or failed: ${e.message}")
                                                    } finally {
                                                        isAutoScrolling = false
                                                        autoScrollJob = null
                                                    }
                                                }
                                                // Briefly ignore further auto-scrolls while layout may be adjusting
                                                ignoreAutoScrollUntil = System.currentTimeMillis() + LAYOUT_STABILIZE_MS
                                            } else {
                                                pendingNewChunks += 1
                                                println("UI-STREAM-DEBUG: not auto-scrolling (isAtBottom=$isAtBottom, userScrolledRecently=$userScrolledRecently, isAutoScrolling=$isAutoScrolling, layoutStabilizing=$layoutStabilizing). pendingNewChunks=$pendingNewChunks")
                                            }
                                        }
                                        is ChatEvent.Error -> {
                                            val updatedMessages2 = chatState.messages.map { message ->
                                                if (message.id == aiMessageId) message.copy(text = "错误：${event.message}") else message
                                            }
                                            chatState = chatState.copy(messages = updatedMessages2)
                                            // persist
                                            dbMutex.lock()
                                            try { repository.saveMessages(currentConversationId, chatState.messages) } finally { dbMutex.unlock() }
                                        }
                                        is ChatEvent.Done -> {
                                            // AI回复完成，此时检查AI消息是否包含think标签，如果是，则自动隐藏think内容
                                            val updatedMessages2 = chatState.messages.map { message ->
                                                if (message.id == aiMessageId && ThinkTagProcessor.containsThinkTag(message.text)) {
                                                    // AI回复完成，自动隐藏think内容
                                                    message.copy(showThink = false)
                                                } else message
                                            }
                                            chatState = chatState.copy(messages = updatedMessages2)
                                            
                                            // save final
                                            dbMutex.lock()
                                            try { repository.saveMessages(currentConversationId, updatedMessages2) } finally { dbMutex.unlock() }
                                            // update conversation summary
                                            dbMutex.lock()
                                            try { repository.updateLastMessage(currentConversationId, chatState.messages.lastOrNull()?.text ?: "")
                                                  appState.conversations = repository.getConversations()
                                            } finally { dbMutex.unlock() }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "获取AI回复失败: ${e.message}", Toast.LENGTH_LONG).show()
                                val updatedMessages2 = chatState.messages.map { message ->
                                    if (message.id == aiMessageId) message.copy(text = "获取回复失败: ${e.message}") else message
                                }
                                chatState = chatState.copy(messages = updatedMessages2)
                                dbMutex.lock()
                                try { repository.saveMessages(currentConversationId, updatedMessages2) } finally { dbMutex.unlock() }
                            }
                        }
                    }
                },
                onOpenImageSelector = { 
                    onOpenImageSelector()
                },
                selectedImageUris = selectedImageUris,
                onSelectedImageUrisChange = { uris -> selectedImageUris = uris }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        // Opt-out of all automatic window insets to avoid double application
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
         ChatContentArea(
             messages = chatState.messages,
             repository = repository,
             systemPrompt = systemPrompt,
             listState = listState,
             conversationId = appState.currentConversationId,
             modifier = Modifier
                 .padding(paddingValues)
                 .fillMaxSize(),
             onOpenEditor = { msg -> editorMessage = msg },
             onUpdateMessageThinkVisibility = { messageId, showThink -> 
                 updateMessageThinkVisibility(messageId, showThink) 
             }
         )
     }

    // Small overlay: when new chunks arrive while user scrolled up, show a small indicator
//    Box(modifier = Modifier.fillMaxSize()) {
//        if (pendingNewChunks > 0) {
//            FloatingActionButton(
//                onClick = {
//                    // scroll to bottom
//                    scope.launch {
//                        listState.animateScrollToItem(max(0, chatState.messages.size - 1))
//                        pendingNewChunks = 0
//                    }
//                },
//                modifier = Modifier
//                    .align(Alignment.BottomEnd)
//                    .padding(16.dp)
//            ) {
//                Text("新消息")
//            }
//        }
//    }

    // 编辑系统提示词对话框
    if (showEditPromptDialog) {
        val conversationId = appState.currentConversationId ?: "default"
        var editRole by remember { mutableStateOf(roleName) }
        var editPrompt by remember { mutableStateOf(systemPrompt) }

        AlertDialog(
            onDismissRequest = { showEditPromptDialog = false },
            title = { Text("编辑会话角色与系统提示") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editRole,
                        onValueChange = { editRole = it },
                        label = { Text("角色名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editPrompt,
                        onValueChange = { editPrompt = it },
                        label = { Text("系统提示词") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        maxLines = 6
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // 保存到数据库
                    val convId = conversationId
                    scope.launch {
                        try {
                            repository.updateConversationRole(convId, editRole, editPrompt)
                            // 更新界面状态
                            roleName = editRole
                            systemPrompt = editPrompt
                            // 更新对话列表摘要
                            appState.conversations = repository.getConversations()
                        } catch (e: Exception) {
                            // ignore for now, UI could show a toast
                        } finally {
                            showEditPromptDialog = false
                        }
                    }
                }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPromptDialog = false }) { Text("取消") }
            }
        )
    }

    // AI 创作编辑器覆盖页（当用户点击收藏并要发布时）
    if (editorMessage != null) {
        val msg = editorMessage!!
        AICreationEditor(
            message = msg,
            repository = repository,
            conversationId = appState.currentConversationId ?: "default",
            onDismiss = { editorMessage = null },
            onPublished = { id ->
                editorMessage = null
                scope.launch {
                    Toast.makeText(context, "已发布到 AI 创作", Toast.LENGTH_SHORT).show()
                }
            },
            conversationMessages = chatState.messages,
            conversationAt = chatState.messages.firstOrNull()?.timestamp?.time
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
    listState: LazyListState,
    conversationId: String?,
    modifier: Modifier = Modifier,
    onOpenEditor: ((Message) -> Unit)? = null,
    onUpdateMessageThinkVisibility: (String, Boolean) -> Unit = { _, _ -> }
) {
    println("DEBUG: ChatContentArea rendering with ${messages.size} messages")
    val scope = rememberCoroutineScope()
    val isDarkTheme = isSystemInDarkTheme()
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp),
        userScrollEnabled = true,
        state = listState
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
                                content = ThinkTagProcessor.processThinkTags(message.text, message.showThink),
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
                            
                            // 显示AI回复中的图片
                            if (message.imageUris.isNotEmpty()) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(message.imageUris) { uri ->
                                        AsyncImage(
                                            model = uri,
                                            contentDescription = "AI image response",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(120.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                    }
                                }
                            }
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
                        
                        // 检查是否包含think标签，如果有则显示think切换按钮
                        val hasThink = remember(message.text) { ThinkTagProcessor.containsThinkTag(message.text) }
                        var currentShowThink by remember(message.id) { mutableStateOf(message.showThink) }
                        
                        // Think可见性切换按钮
                        if (hasThink) {
                            IconButton(
                                onClick = { 
                                    currentShowThink = !currentShowThink
                                    // 更新消息的think可见性状态
                                    onUpdateMessageThinkVisibility(message.id, currentShowThink)
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
                                    imageVector = if (currentShowThink) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (currentShowThink) "隐藏思考" else "显示思考",
                                    tint = if (currentShowThink) MaterialTheme.colorScheme.primary 
                                          else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
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
                                    // request parent to open editor dialog
                                    onOpenEditor?.invoke(message)
                                } else {
                                    Toast.makeText(context, "已取消收藏", Toast.LENGTH_SHORT).show()
                                }
                                println("${if (isFavorited) "触发创建编辑" else "取消收藏"}消息")
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

                // Render global editor overlay if set (we use a remember in outer scope to manage showing the editor)
                // We'll declare separate state outside the items loop to avoid re-creating; check below for implementation.
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
                                content = ThinkTagProcessor.processThinkTags(message.text, message.showThink),
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
                            
                            // 显示用户发送的图片
                            if (message.imageUris.isNotEmpty()) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(message.imageUris) { uri ->
                                        AsyncImage(
                                            model = uri,
                                            contentDescription = "User image",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(120.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                    }
                                }
                            }
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
            // removed warning/info icon as requested and replaced with a cleaner layout
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AI设定",
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
    onSendMessage: (String) -> Unit = {},
    onOpenImageSelector: () -> Unit = {},
    selectedImageUris: List<String> = emptyList(),
    onSelectedImageUrisChange: (List<String>) -> Unit = {}
) {
    var text by remember { mutableStateOf(inputText) }
    val focusManager = LocalFocusManager.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            // Apply nav bar padding when IME is hidden, and IME padding when shown
            .navigationBarsPadding()
            .imePadding()
    ) {
        // 显示选中的图片缩略图
        if (selectedImageUris.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectedImageUris) { uri ->
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Selected image",
                            modifier = Modifier.fillMaxSize()
                        )
                        // 删除按钮
                        IconButton(
                            onClick = {
                                val updatedUris = selectedImageUris - uri
                                onSelectedImageUrisChange(updatedUris)
                            },
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.TopEnd)
                                .background(Color.Red, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Remove image",
                                modifier = Modifier.size(12.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
        
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
                        if (text.isNotBlank() || selectedImageUris.isNotEmpty()) {
                            onSendMessage(text)
                            text = ""
                            onSelectedImageUrisChange(emptyList())
                            focusManager.clearFocus()
                        }
                    }
                ),
                singleLine = true
            )
            
            IconButton(
                enabled = text.isNotBlank() || selectedImageUris.isNotEmpty(),
                onClick = { 
                    if (text.isNotBlank() || selectedImageUris.isNotEmpty()) {
                        onSendMessage(text)
                        text = ""
                        onSelectedImageUrisChange(emptyList())
                        focusManager.clearFocus()
                    }
                }
            ) {
                Icon(
                    imageVector = if (text.isNotBlank() || selectedImageUris.isNotEmpty()) Icons.AutoMirrored.Filled.Send else Icons.AutoMirrored.Outlined.Send,
                    contentDescription = "发送",
                    tint = if (text.isNotBlank() || selectedImageUris.isNotEmpty()) MaterialTheme.colorScheme.primary 
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
            ExpandedInputArea(onOpenImageSelector = onOpenImageSelector)
        }
    }
}
