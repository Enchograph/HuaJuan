package com.chenhongyu.huajuan

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
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
import androidx.compose.foundation.combinedClickable
import com.chenhongyu.huajuan.data.Conversation
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.platform.LocalConfiguration
import kotlin.math.roundToInt
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalDensity

/**
 * 侧边栏导航
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SideDrawer(

    onChatPageSelected: (String) -> Unit,
    onSettingPageSelected: () -> Unit,
    onAICreationPageSelected: () -> Unit,
    onAgentPageSelected: () -> Unit,
    conversations: List<Conversation>,
    drawerWidth: Dp,
    darkTheme: Boolean,
    repository: Repository,
    currentConversationId: String?, // 添加当前对话ID参数
) {
    HuaJuanTheme(darkTheme = darkTheme) {
        ModalDrawerSheet(
            modifier = Modifier.width(drawerWidth),
            drawerContainerColor = MaterialTheme.colorScheme.surface,
            drawerTonalElevation = 1.dp,
            drawerShape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 0.dp, bottomEnd = 0.dp)
        ) {
            var showContextMenu by remember { mutableStateOf(false) }
            var selectedConversation by remember { mutableStateOf<Conversation?>(null) }
            var showEditDialog by remember { mutableStateOf(false) }
            var newTitle by remember { mutableStateOf("") }
            val scope = rememberCoroutineScope()
            var menuPosition by remember { mutableStateOf(Offset.Zero) }
            var contextMenuOffset by remember { mutableStateOf(IntOffset.Zero) }
            val conversationPositions = remember { mutableStateMapOf<String, Pair<Offset, androidx.compose.ui.geometry.Size>>() }
            var pinnedConversations by remember { mutableStateOf(setOf<String>()) }
            
            // 读取用户信息用于底部用户栏显示
            val userInfo = remember { repository.getUserInfo() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // 顶部搜索框 (始终置顶)
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    placeholder = { Text("搜索...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .align(Alignment.TopCenter),
                    shape = RoundedCornerShape(50.dp), // 胶囊形状
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                // 底部用户栏 (始终置底)
                ListItem(
                    headlineContent = {
                        Text(
                            text = userInfo.username,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
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
                                text = userInfo.avatar,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    },
                    trailingContent = {
                        Row {
//                            IconButton(
//                                onClick = {
//                                    println("点击了扫码按钮")
//                                },
//                                modifier = Modifier
//                                    .clip(CircleShape)
//                            ) {
//                                Icon(
//                                    imageVector = Icons.Outlined.QrCodeScanner,
//                                    contentDescription = "扫码",
//                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
//                                )
//                            }
//
//                            IconButton(
//                                onClick = {
//                                    println("点击了通知按钮")
//                                },
//                                modifier = Modifier
//                                    .clip(CircleShape)
//                            ) {
//                                Icon(
//                                    imageVector = Icons.Outlined.Notifications,
//                                    contentDescription = "通知",
//                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
//                                    )
//                            }

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
                        .align(Alignment.BottomCenter)
                )

                // 可滚动的内容区域 (第二和第三区块)
                val listState = rememberLazyListState()
                var showBackToTop by remember { mutableStateOf(false) }

                // 监听滚动状态以确定是否显示"回到花卷"按钮
                LaunchedEffect(listState) {
                    snapshotFlow { listState.firstVisibleItemIndex }.collect { index ->
                        // 当第一项(花卷项)完全不可见时显示按钮
                        showBackToTop = index >= 3 // 花卷项是第0项，AI创作是第1项，发现智能体是第2项
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp + 56.dp, bottom = 80.dp) // 为顶部搜索框和底部用户栏留出空间
                ) {
                    // 第二个区块：主要功能
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            // "花卷" - 聊天页面入口
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = "花卷",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                leadingContent = {
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
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        println("选择了花卷聊天")
                                        // 创建新的对话而不是使用"default"
                                        scope.launch {
                                            val newConversation = repository.createNewConversation(
                                                title = "新对话",
                                                roleName = "默认助手",
                                                systemPrompt = "你是一个AI助手"
                                            )
                                            onChatPageSelected(newConversation.id)
                                        }
                                    }
                            )

                            // "AI 创作" - AI创作分享社区入口
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = "AI 创作",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                leadingContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.secondaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        println("选择了AI创作")
                                        onAICreationPageSelected()
                                    }
                            )

                            // "发现智能体" - AI智能体选择页面入口
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = "发现智能体",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                leadingContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ViewModule,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        println("选择了发现智能体")
                                        onAgentPageSelected()
                                    }
                            )
                        }

                        // 分割线
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }

                    // 显示置顶对话
                    if (pinnedConversations.isNotEmpty()) {
                        item {
                            Text(
                                text = "置顶对话",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                            )
                        }
                        
                        items(conversations.filter { conversation -> 
                            conversation.id in pinnedConversations 
                        }, key = { it.id }) { conversation ->
                            Box(
                                modifier = Modifier.then(
                                    if (showContextMenu && selectedConversation?.id == conversation.id) 
                                        Modifier  // 去除模糊效果
                                    else 
                                        Modifier
                                )
                            ) {
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            text = conversation.title,
                                            maxLines = 1,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (conversation.id == currentConversationId) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    },
                                    leadingContent = {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(12.dp)) // 圆形彩色背景图标
                                                .background(
                                                    if (conversation.id == currentConversationId) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.primaryContainer
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.PushPin,
                                                contentDescription = null,
                                                tint = if (conversation.id == currentConversationId) {
                                                    MaterialTheme.colorScheme.onPrimary
                                                } else {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                },
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    },
                                    colors = ListItemDefaults.colors(
                                        containerColor = Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .combinedClickable(
                                            onClick = {
                                                println("DEBUG: Selected pinned conversation in SideDrawer - Title: ${conversation.title}, ID: ${conversation.id}")
                                                onChatPageSelected(conversation.id)
                                            },
                                            onLongClick = {
                                                println("DEBUG: Long clicked pinned conversation - Title: ${conversation.title}, ID: ${conversation.id}")
                                                // 获取当前项的位置信息
                                                conversationPositions[conversation.id]?.let { (position, size) ->
                                                    // 计算菜单位置：在当前项正下方，但稍微向上偏移一点
                                                    val x = position.x.toInt()
                                                    val y = (position.y + size.height - 155).toInt() // 向上偏移大约一项的高度 (简化处理)
                                                    contextMenuOffset = IntOffset(x, y)
                                                }
                                                selectedConversation = conversation
                                                showContextMenu = true
                                                println("DEBUG: showContextMenu set to $showContextMenu")
                                            },
                                            indication = null, // 移除长按波纹效果
                                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                        )
                                        .then(
                                            if (conversation.id == currentConversationId) {
                                                Modifier.background(
                                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .padding(horizontal = 8.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        /*
                                        .border(
                                            width = if (conversation.id == currentConversationId) 2.dp else 0.dp,
                                            color = if (conversation.id == currentConversationId) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                Color.Transparent
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        */
                                        .padding(horizontal = 8.dp)
                                        .onGloballyPositioned { coordinates ->
                                            // 存储当前项的位置信息，包括相对于屏幕的位置
                                            val positionInRoot = coordinates.boundsInRoot().topLeft
                                            val size = coordinates.size.toSize()
                                            conversationPositions[conversation.id] = positionInRoot to size
                                        }
                                )
                            }
                        }
                        
                        // 分割线
                        item {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }

                    // 第三个区块：历史记录
                    item {
                        Text(
                            text = "历史记录",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                        )
                    }

                    items(conversations.filter { conversation -> 
                        conversation.id !in pinnedConversations 
                    }, key = { it.id }) { conversation ->
                        Box(
                            modifier = Modifier.then(
                                if (showContextMenu && selectedConversation?.id == conversation.id) 
                                    Modifier  // 去除模糊效果
                                else 
                                    Modifier
                            )
                        ) {
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = conversation.title,
                                        maxLines = 1,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (conversation.id == currentConversationId) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                },
                                leadingContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp)) // 圆形彩色背景图标
                                            .background(
                                                if (conversation.id == currentConversationId) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.primaryContainer
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ChatBubbleOutline,
                                            contentDescription = null,
                                            tint = if (conversation.id == currentConversationId) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            },
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .combinedClickable(
                                        onClick = {
                                            println("DEBUG: Selected conversation in SideDrawer - Title: ${conversation.title}, ID: ${conversation.id}")
                                            onChatPageSelected(conversation.id)
                                        },
                                        onLongClick = {
                                            println("DEBUG: Long clicked conversation - Title: ${conversation.title}, ID: ${conversation.id}")
                                            // 获取当前项的位置信息
                                            conversationPositions[conversation.id]?.let { (position, size) ->
                                                // 计算菜单位置：在当前项正下方，但稍微向上偏移一点
                                                val x = position.x.toInt()
                                                val y = (position.y + size.height - 155).toInt() // 向上偏移大约一项的高度 (简化处理)
                                                contextMenuOffset = IntOffset(x, y)
                                            }
                                            selectedConversation = conversation
                                            showContextMenu = true
                                            println("DEBUG: showContextMenu set to $showContextMenu")
                                        },
                                        indication = null, // 移除长按波纹效果
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                    )
                                    .then(
                                        if (conversation.id == currentConversationId) {
                                            Modifier.background(
                                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .padding(horizontal = 8.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    /*
                                    .border(
                                        width = if (conversation.id == currentConversationId) 2.dp else 0.dp,
                                        color = if (conversation.id == currentConversationId) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            Color.Transparent
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    */
                                    .padding(horizontal = 8.dp)
                                    .onGloballyPositioned { coordinates ->
                                        // 存储当前项的位置信息，包括相对于屏幕的位置
                                        val positionInRoot = coordinates.boundsInRoot().topLeft
                                        val size = coordinates.size.toSize()
                                        conversationPositions[conversation.id] = positionInRoot to size
                                    }
                            )
                        }
                    }
                }

                // "回到花卷"浮动按钮
                if (showBackToTop) {
                    FloatingActionButton(
                        onClick = {
                            scope.launch {
                                listState.animateScrollToItem(0)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 90.dp), // 在用户栏上方
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "回到花卷")
                    }
                }

                // 对话操作上下文菜单 - 放在Box的最后一层，确保在最上层显示
                val context = LocalContext.current
                println("DEBUG: showContextMenu=$showContextMenu, selectedConversation=${selectedConversation?.title}")
                if (showContextMenu && selectedConversation != null) {
                    val conversation = selectedConversation!!
                    println("DEBUG: Showing context menu for conversation ${conversation.title}")
                    ContextMenu(
                        conversation = conversation,
                        offset = contextMenuOffset,
                        isPinned = conversation.id in pinnedConversations,
                        onDismiss = {
                            println("DEBUG: Dismissing context menu")
                            showContextMenu = false
                            selectedConversation = null
                        },
                        onPin = {
                            // 切换置顶状态
                            if (conversation.id in pinnedConversations) {
                                pinnedConversations = pinnedConversations - conversation.id
                            } else {
                                pinnedConversations = pinnedConversations + conversation.id
                            }
                            println("DEBUG: Toggle pin conversation, pinnedConversations=$pinnedConversations")
                            showContextMenu = false
                            selectedConversation = null
                        },
                        onEdit = {
                            newTitle = conversation.title
                            showEditDialog = true
                            println("DEBUG: Edit conversation, showEditDialog=$showEditDialog")
                            showContextMenu = false
                        },
                        onShare = {
                            // 分享功能 - 使用应用级持久化的用户信息并使用Compose scope
                            println("DEBUG: Share conversation")
                            val ctx = context
                            val conv = conversation
                            // use the remembered scope instead of GlobalScope
                            scope.launch {
                                try {
                                    // Fetch messages on IO
                                    val messages = withContext(Dispatchers.IO) {
                                        repository.getMessages(conv.id)
                                    }
                                    val modelName = repository.getSelectedModel()
                                    val userInfo = repository.getUserInfo()

                                    // Call share helper (suspend) from coroutine
                                    com.chenhongyu.huajuan.share.ShareHelper.shareConversationAsHtml(
                                        ctx,
                                        conv,
                                        messages,
                                        userInfo.username,
                                        userInfo.signature,
                                        modelName
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(ctx, "分享失败: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        onOpenInBrowser = {
                            // Generate HTML file and open in local browser
                            val ctx = context
                            val conv = conversation
                            scope.launch {
                                try {
                                    val messages = withContext(Dispatchers.IO) { repository.getMessages(conv.id) }
                                    val userInfo = repository.getUserInfo()
                                    val file = com.chenhongyu.huajuan.share.ShareHelper.generateConversationHtmlFile(ctx, conv, messages, userInfo.username, userInfo.signature, repository.getSelectedModel())

                                    val authority = "com.chenhongyu.huajuan.fileprovider"
                                    val uri = FileProvider.getUriForFile(ctx, authority, file)

                                    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "text/html")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    ctx.startActivity(viewIntent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(ctx, "打开失败: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        onDelete = {
                            scope.launch {
                                try {
                                    repository.deleteConversation(conversation.id)
                                    // 如果被删除的是置顶对话，则从置顶集合中移除
                                    if (conversation.id in pinnedConversations) {
                                        pinnedConversations = pinnedConversations - conversation.id
                                    }
                                    // 通知MainActivity刷新对话列表
                                    onChatPageSelected("refresh_needed")
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "对话已删除", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "删除失败: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                            showContextMenu = false
                            selectedConversation = null
                        }
                    )
                }

                // 编辑对话标题对话框
                if (showEditDialog && selectedConversation != null) {
                    val conversation = selectedConversation!!
                    EditTitleDialog(
                        conversation = conversation,
                        currentTitle = newTitle,
                        onTitleChange = { newTitle = it },
                        onDismiss = {
                            showEditDialog = false
                            selectedConversation = null
                        },
                        onSave = { title ->
                            scope.launch {
                                try {
                                    repository.updateConversationTitle(conversation.id, title)
                                    // 通知MainActivity刷新对话列表
                                    onChatPageSelected("refresh_needed")
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "对话名称已更新", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "更新失败: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                            showEditDialog = false
                            selectedConversation = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ContextMenu(
    conversation: Conversation,
    offset: IntOffset,
    isPinned: Boolean,
    onDismiss: () -> Unit,
    onPin: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onOpenInBrowser: () -> Unit,
    onDelete: () -> Unit
) {
    // 获取屏幕尺寸
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) {
                // 点击空白处关闭菜单
                onDismiss()
            }
    ) {
        Surface(
            modifier = Modifier
                .width(250.dp)
                .align(Alignment.TopStart)
                .offset {
                    // 确保菜单不会超出屏幕右边界
                    val x = offset.x.coerceAtMost(
                        (screenWidth - 250.dp).roundToPx()
                            .coerceAtLeast(0)
                    )
                    // 确保菜单不会超出屏幕下边界
                    val y = offset.y.coerceAtMost(
                        (screenHeight - 150.dp).roundToPx()
                            .coerceAtLeast(0)
                    )
                    IntOffset(x, y)
                },
            tonalElevation = 8.dp,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .width(250.dp)
                    .padding(8.dp)
            ) {
                ContextMenuItem(
                    icon = if (isPinned) Icons.Default.RemoveFromQueue else Icons.Default.PushPin,
                    text = if (isPinned) "取消置顶" else "置顶",
                    onClick = {
                        onPin()
                    }
                )
                
                ContextMenuItem(
                    icon = Icons.Default.Edit,
                    text = "编辑对话名称",
                    onClick = {
                        onEdit()
                    }
                )
                
                ContextMenuItem(
                    icon = Icons.Default.Share,
                    text = "分享对话",
                    onClick = {
                        onShare()
                    }
                )
                
                ContextMenuItem(
                    icon = Icons.Default.OpenInBrowser,
                    text = "在浏览器中打开",
                    onClick = {
                        onOpenInBrowser()
                    }
                )

                ContextMenuItem(
                    icon = Icons.Default.Delete,
                    text = "删除对话",
                    textColor = MaterialTheme.colorScheme.error,
                    onClick = {
                        onDelete()
                    }
                )
            }
        }
    }
}

@Composable
fun ContextMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    ListItem(
        headlineContent = {
            Text(
                text = text,
                color = textColor,
                style = MaterialTheme.typography.bodyLarge // 使用与历史记录相同的字体大小
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier
                    .size(24.dp) // 使用与历史记录相同的图标大小
                    .offset(x = (-8).dp) // 将图标向左移动1dp
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp)) // 使用与历史记录项相同的圆角
            .clickable(
                indication = null, // 移除点击波纹效果
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { 
                onClick() 
            }
            .padding(horizontal = 8.dp) // 添加与历史记录项相同的内边距
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTitleDialog(
    conversation: Conversation,
    currentTitle: String,
    onTitleChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "编辑对话名称",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = currentTitle,
                    onValueChange = onTitleChange,
                    label = { Text("对话名称") },
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                        }
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (currentTitle.isNotBlank()) {
                        onSave(currentTitle)
                    }
                },
                enabled = currentTitle.isNotBlank()
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

private fun deleteConversation(conversationId: String, repository: Repository, context: Context) {
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
        try {
            repository.deleteConversation(conversationId)
            withContext(Dispatchers.Main) {
                //Toast.makeText(context, "对话已删除", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "删除失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

private fun updateConversationTitle(conversationId: String, title: String, repository: Repository, context: Context) {
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
        try {
            repository.updateConversationTitle(conversationId, title)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "对话名称已更新", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "更新失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
