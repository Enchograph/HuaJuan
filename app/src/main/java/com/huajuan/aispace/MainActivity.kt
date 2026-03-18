package com.huajuan.aispace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.huajuan.aispace.ui.animations.NavigationTransitions
import com.huajuan.aispace.data.AppFacade
import com.huajuan.aispace.data.AgentProvider
import com.huajuan.aispace.data.Repository
import com.huajuan.aispace.data.AppState
import com.huajuan.aispace.data.ToolIds
import com.huajuan.aispace.screens.assistant.AssistantListScreen
import com.huajuan.aispace.screens.assistant.AssistantSettingsDetailScreen
import com.huajuan.aispace.screens.assistant.AssistantSettingsAdvancedScreen
import com.huajuan.aispace.screens.assistant.AssistantSettingsHomeScreen
import com.huajuan.aispace.screens.assistant.AssistantSettingsSection
import com.huajuan.aispace.screens.chat.ChatScreen
import com.huajuan.aispace.screens.creation.ImageSelectorScreen
import com.huajuan.aispace.screens.drawer.SideDrawer
import com.huajuan.aispace.screens.files.FilesScreen
import com.huajuan.aispace.screens.image.ImageGenerationScreen
import com.huajuan.aispace.screens.knowledge.KnowledgeBaseDetailScreen
import com.huajuan.aispace.screens.knowledge.KnowledgeBaseScreen
import com.huajuan.aispace.screens.knowledge.KnowledgeBaseSearchScreen
import com.huajuan.aispace.screens.knowledge.KnowledgeBaseSettingsScreen
import com.huajuan.aispace.screens.search.ConversationSearchScreen
import com.huajuan.aispace.screens.settings.SettingScreen
import com.huajuan.aispace.screens.terminal.RemoteTerminalScreen
import com.huajuan.aispace.screens.translate.TranslationScreen
import com.huajuan.aispace.ui.theme.HuaJuanTheme
import com.huajuan.aispace.navigation.LayeredOverlayHost
import com.huajuan.aispace.navigation.rememberLayeredNavigator
import com.huajuan.aispace.screens.settings.SettingsSubRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.roundToInt
import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.compose.material3.MaterialTheme

private object MainRoute {
    const val Chat = "chat"
    const val Settings = "settings"
    const val AssistantList = "assistant_list"
    const val KnowledgeBase = "knowledge_base"
    const val ImageGeneration = "image_generation"
    const val Translation = "translation"
    const val Files = "files"
    const val RemoteTerminal = "remote_terminal"
    const val Search = "search"
}

private sealed interface OverlayRoute {
    data object ImageSelector : OverlayRoute
    data class KnowledgeBaseDetail(val knowledgeBase: com.huajuan.aispace.data.KnowledgeBaseDto) : OverlayRoute
    data class KnowledgeBaseSettings(val kbId: String) : OverlayRoute
    data class KnowledgeBaseSearch(val kbId: String) : OverlayRoute
    data object KnowledgeBaseCreate : OverlayRoute
    data class SettingsSub(val route: com.huajuan.aispace.screens.settings.SettingsSubRoute) : OverlayRoute
    data class AssistantSettingsHome(val assistantId: String) : OverlayRoute
    data class AssistantSettingsAdvanced(val assistantId: String) : OverlayRoute
    data class AssistantSettingsDetail(
        val assistantId: String,
        val section: AssistantSettingsSection
    ) : OverlayRoute
}

class MainActivity : ComponentActivity() {
    private lateinit var repository: Repository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 初始化Repository
        repository = AppFacade(this)

        setContent {
            HuaJuanTheme {
                MainApp(repository)
            }
        }
    }
}

@Composable
fun MainApp(repository: Repository) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val context = LocalContext.current
    val view = LocalView.current

    // 获取屏幕宽度
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val drawerWidth = screenWidth * 0.75f
    val drawerWidthPx = with(density) { drawerWidth.toPx() }
    val navController = rememberNavController()

    // 待发送图片列表（与图片选择器同步）
    var selectedImageUris by remember { mutableStateOf<List<String>>(emptyList()) }
    var pendingJumpMessageId by remember { mutableStateOf<String?>(null) }
    var deletedChatConversationId by remember { mutableStateOf<String?>(null) }
    var deletedChatConversationVersion by remember { mutableStateOf(0) }
    var chatInputOverlayVisible by remember { mutableStateOf(false) }
    var chatCollapseRequest by remember { mutableStateOf(0) }
    var knowledgeBaseRefreshToken by remember { mutableStateOf(0) }
    var drawerUiResetRequest by remember { mutableStateOf(0) }

    // 从Repository获取深色模式设置
    val darkMode = remember { mutableStateOf(repository.getDarkMode()) }
    val systemDark = isSystemInDarkTheme()

    LaunchedEffect(systemDark) {
        if (repository.getThemeMode() == "system" && darkMode.value != systemDark) {
            darkMode.value = systemDark
            repository.setDarkMode(systemDark)
        }
    }

    SideEffect {
        val window = (context as? Activity)?.window ?: return@SideEffect
        window.statusBarColor = Color.Transparent.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkMode.value
    }

    // 初始化AppState，从Repository加载对话历史
    val initialAssistantId = remember { repository.getCurrentAssistantId() }
    var appState by remember { mutableStateOf(AppState(currentAssistantId = initialAssistantId)) }

    fun conversationAssistantIdForRoute(route: String?, state: AppState): String = when (route) {
        MainRoute.ImageGeneration -> ToolIds.ImageGeneration
        MainRoute.Translation -> ToolIds.Translation
        else -> state.currentAssistantId ?: repository.getCurrentAssistantId()
    }

    fun selectedConversationIdForRoute(route: String?, state: AppState): String? = when (route) {
        MainRoute.ImageGeneration -> state.currentImageGenerationConversationId
        MainRoute.Translation -> state.currentTranslationConversationId
        else -> state.currentConversationId
    }

    // 抽屉状态和位置控制
    val drawerOffset = remember { Animatable(-drawerWidthPx) }
    val maxDrawerOffset = 0f
    val minDrawerOffset = -drawerWidthPx
    var blockDrawerDragForCollapse by remember { mutableStateOf(false) }
    var drawerResetDispatchedForCurrentOpen by remember { mutableStateOf(false) }

    fun dispatchDrawerUiResetIfNeeded() {
        if (!drawerResetDispatchedForCurrentOpen) {
            drawerUiResetRequest += 1
            drawerResetDispatchedForCurrentOpen = true
        }
    }

    val closeDrawer: () -> Unit = {
        scope.launch {
            drawerOffset.animateTo(minDrawerOffset, spring(stiffness = Spring.StiffnessMediumLow))
        }
        Unit
    }
    val openDrawer: () -> Unit = {
        scope.launch {
            dispatchDrawerUiResetIfNeeded()
            drawerOffset.animateTo(maxDrawerOffset, spring(stiffness = Spring.StiffnessMediumLow))
        }
        Unit
    }
    val toggleDrawer: () -> Unit = {
        scope.launch {
            if (drawerOffset.value == minDrawerOffset) {
                dispatchDrawerUiResetIfNeeded()
                drawerOffset.animateTo(maxDrawerOffset, spring(stiffness = Spring.StiffnessMediumLow))
            } else {
                drawerOffset.animateTo(minDrawerOffset, spring(stiffness = Spring.StiffnessMediumLow))
            }
        }
        Unit
    }

    LaunchedEffect(Unit) {
        drawerOffset.snapTo(minDrawerOffset)
    }

    val activity = context as? Activity

    // Layered navigation rules:
    // Layer 0: Conversation list (drawer open). Back exits the app.
    // Layer 1: Chat / Settings / Assistant List / Tools. Back opens drawer (Layer 0).
    // Layer 2+: Overlays opened from Layer 1+ (e.g. model selector). Back closes top overlay.
    val layeredNavigator = rememberLayeredNavigator<OverlayRoute>(
        isLayer0Open = { drawerOffset.value > minDrawerOffset + 1f },
        openLayer0 = openDrawer,
        closeLayer0 = {
            scope.launch {
                drawerOffset.animateTo(minDrawerOffset, spring(stiffness = Spring.StiffnessMediumLow))
            }
        },
        navigateMain = { route ->
            navController.navigate(route) {
                launchSingleTop = true
                restoreState = true
                popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                }
            }
            closeDrawer()
        },
        initialMainRoute = MainRoute.Chat,
        exitApp = { activity?.finish() }
    )

    LaunchedEffect(navController) {
        snapshotFlow { navController.currentBackStackEntry?.destination?.route }
            .collectLatest { route ->
                if (!route.isNullOrBlank()) {
                    layeredNavigator.setCurrentMainRoute(route)
                }
            }
    }

    val currentMainRoute = layeredNavigator.currentMainRoute

    // 监听当前页面对应的选中会话变化并重新加载对话列表
    LaunchedEffect(
        appState.currentConversationId,
        appState.currentTranslationConversationId,
        appState.currentImageGenerationConversationId,
        currentMainRoute
    ) {
        appState = appState.copy(
            conversations = repository.getConversationsAsync(
                conversationAssistantIdForRoute(currentMainRoute, appState)
            )
        )
    }

    LaunchedEffect(currentMainRoute) {
        snapshotFlow { drawerOffset.value > minDrawerOffset + 1f }
            .distinctUntilChanged()
            .collectLatest { isOpen ->
                if (!isOpen) {
                    drawerResetDispatchedForCurrentOpen = false
                }
                if (isOpen) {
                    appState = appState.copy(
                        conversations = repository.getConversationsAsync(
                            conversationAssistantIdForRoute(currentMainRoute, appState)
                        )
                    )
                }
            }
    }

    LaunchedEffect(appState.currentAssistantId, currentMainRoute) {
        repository.repairAssistantBindings()
        val assistantId = conversationAssistantIdForRoute(currentMainRoute, appState)
        if (
            currentMainRoute != MainRoute.Translation &&
            currentMainRoute != MainRoute.ImageGeneration &&
            assistantId != repository.getCurrentAssistantId()
        ) {
            repository.setCurrentAssistantId(assistantId)
        }
        var conversations = repository.getConversationsAsync(assistantId)
        val shouldAutoCreate = currentMainRoute == MainRoute.Chat
        if (conversations.isEmpty() && shouldAutoCreate) {
            val agent = repository.getAssistantById(assistantId)
            val newConversation = repository.createNewConversation(
                assistantId = assistantId,
                title = repository.getCurrentDefaultConversationTitle(),
                roleName = agent?.name ?: repository.getDefaultAssistantName(),
                systemPrompt = agent?.systemPrompt ?: repository.getDefaultAssistantPrompt()
            )
            repository.saveMessages(newConversation.id, assistantId, emptyList())
            conversations = repository.getConversationsAsync(assistantId)
        }
        val validCurrentId = selectedConversationIdForRoute(currentMainRoute, appState)?.takeIf { id ->
            conversations.any { it.id == id }
        }
        appState = when (currentMainRoute) {
            MainRoute.ImageGeneration -> appState.copy(
                conversations = conversations,
                currentImageGenerationConversationId = validCurrentId ?: conversations.firstOrNull()?.id
            )
            MainRoute.Translation -> appState.copy(
                conversations = conversations,
                currentTranslationConversationId = validCurrentId ?: conversations.firstOrNull()?.id
            )
            else -> appState.copy(
                conversations = conversations,
                currentConversationId = validCurrentId ?: conversations.firstOrNull()?.id,
                currentAssistantId = assistantId
            )
        }
    }


    val settleDrawer: (Float) -> Unit = { velocity ->
        scope.launch {
            if (velocity > 0 || drawerOffset.value > minDrawerOffset / 2) {
                // 打开抽屉
                dispatchDrawerUiResetIfNeeded()
                drawerOffset.animateTo(maxDrawerOffset, spring(stiffness = Spring.StiffnessMediumLow))
            } else {
                // 关闭抽屉
                drawerOffset.animateTo(minDrawerOffset, spring(stiffness = Spring.StiffnessMediumLow))
            }
        }
    }

    val drawerDragState = rememberDraggableState { delta ->
        if (blockDrawerDragForCollapse) return@rememberDraggableState
        scope.launch {
            drawerOffset.snapTo((drawerOffset.value + delta).coerceIn(minDrawerOffset, maxDrawerOffset))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .draggable(
                state = drawerDragState,
                orientation = Orientation.Horizontal,
                enabled = !layeredNavigator.hasOverlays,
                onDragStarted = {
                    val shouldCollapse =
                        layeredNavigator.currentMainRoute == MainRoute.Chat &&
                                chatInputOverlayVisible &&
                                drawerOffset.value <= minDrawerOffset + 1f
                    if (shouldCollapse) {
                        blockDrawerDragForCollapse = true
                        chatCollapseRequest += 1
                    }
                },
                onDragStopped = { velocity ->
                    if (blockDrawerDragForCollapse) {
                        blockDrawerDragForCollapse = false
                        return@draggable
                    }
                    // 根据速度和位置决定是打开还是关闭抽屉
                    settleDrawer(velocity)
                }
            )
    ) {
        // 主页面容器（包含所有主页面）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset((drawerOffset.value + drawerWidthPx).roundToInt(), 0) }
        ) {
            NavHost(
                navController = navController,
                startDestination = MainRoute.Chat,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(MainRoute.Chat) {
                    HuaJuanTheme(darkTheme = darkMode.value) {
                        ChatScreen(
                            onMenuClick = { toggleDrawer() },
                            appState = appState,
                            onAppStateChange = { newState -> appState = newState },
                            refreshConversations = { assistantId ->
                                appState = appState.copy(
                                    conversations = repository.getConversationsAsync(assistantId)
                                )
                            },
                            repository = repository,
                            drawerUiResetRequest = drawerUiResetRequest,
                            collapseInputRequest = chatCollapseRequest,
                            onInputOverlayVisibleChange = { visible -> chatInputOverlayVisible = visible },
                            onOpenAssistantSettings = {
                                val assistantId = appState.currentAssistantId ?: repository.getCurrentAssistantId()
                                layeredNavigator.pushOverlay(OverlayRoute.AssistantSettingsHome(assistantId))
                            },
                            onOpenProviderModelSettings = { provider ->
                                layeredNavigator.pushOverlay(OverlayRoute.SettingsSub(SettingsSubRoute.ModelManagement))
                                layeredNavigator.pushOverlay(OverlayRoute.SettingsSub(SettingsSubRoute.VendorModels(provider)))
                            },
                            deletedConversationId = deletedChatConversationId,
                            deletedConversationVersion = deletedChatConversationVersion,
                            targetMessageId = pendingJumpMessageId,
                            onTargetMessageConsumed = { pendingJumpMessageId = null },
                            onOpenImageSelector = {
                                layeredNavigator.pushOverlay(OverlayRoute.ImageSelector)
                            },
                            selectedImageUris = selectedImageUris,
                            onSelectedImageUrisChange = { selectedImageUris = it }
                        )
                    }
                }
                composable(MainRoute.Settings) {
                    HuaJuanTheme(darkTheme = darkMode.value) {
                        SettingScreen(
                            onMenuClick = { toggleDrawer() },
                            drawerUiResetRequest = drawerUiResetRequest,
                            onOpenSubPage = { route ->
                                layeredNavigator.pushOverlay(OverlayRoute.SettingsSub(route))
                            }
                        )
                    }
                }
                composable(MainRoute.AssistantList) {
                    HuaJuanTheme(darkTheme = darkMode.value) {
                        AssistantListScreen(
                            onMenuClick = { toggleDrawer() },
                            drawerUiResetRequest = drawerUiResetRequest,
                            repository = repository,
                            currentAssistantId = appState.currentAssistantId ?: repository.getCurrentAssistantId(),
                            onAssistantSelected = { assistantId ->
                                appState = appState.copy(
                                    currentAssistantId = assistantId,
                                    currentConversationId = null
                                )
                                layeredNavigator.navigateMain(MainRoute.Chat)
                            },
                            onOpenAssistantSettings = { assistantId ->
                                layeredNavigator.pushOverlay(OverlayRoute.AssistantSettingsHome(assistantId))
                            }
                        )
                    }
                }
                composable(MainRoute.KnowledgeBase) {
                    HuaJuanTheme(darkTheme = darkMode.value) {
                        KnowledgeBaseScreen(
                            onMenuClick = { toggleDrawer() },
                            drawerUiResetRequest = drawerUiResetRequest,
                            repository = repository,
                            onOpenKnowledgeBaseDetail = { knowledgeBase ->
                                layeredNavigator.pushOverlay(OverlayRoute.KnowledgeBaseDetail(knowledgeBase))
                            },
                            onOpenKnowledgeBaseCreate = {
                                layeredNavigator.pushOverlay(OverlayRoute.KnowledgeBaseCreate)
                            },
                            refreshToken = knowledgeBaseRefreshToken
                        )
                    }
                }
                composable(MainRoute.ImageGeneration) {
                    HuaJuanTheme(darkTheme = darkMode.value) {
                        ImageGenerationScreen(
                            onMenuClick = { toggleDrawer() },
                            drawerUiResetRequest = drawerUiResetRequest,
                            repository = repository,
                            assistantId = ToolIds.ImageGeneration,
                            currentConversationId = appState.currentImageGenerationConversationId,
                            onCurrentConversationChange = { conversationId ->
                                appState = appState.copy(currentImageGenerationConversationId = conversationId)
                            },
                            onOpenProviderModelSettings = { provider ->
                                layeredNavigator.pushOverlay(OverlayRoute.SettingsSub(SettingsSubRoute.ModelManagement))
                                layeredNavigator.pushOverlay(OverlayRoute.SettingsSub(SettingsSubRoute.VendorModels(provider)))
                            }
                        )
                    }
                }
                composable(MainRoute.Translation) {
                    HuaJuanTheme(darkTheme = darkMode.value) {
                        TranslationScreen(
                            onMenuClick = { toggleDrawer() },
                            drawerUiResetRequest = drawerUiResetRequest,
                            repository = repository,
                            assistantId = ToolIds.Translation,
                            currentConversationId = appState.currentTranslationConversationId,
                            onCurrentConversationChange = { conversationId ->
                                appState = appState.copy(currentTranslationConversationId = conversationId)
                            },
                            onOpenTranslationSettings = {
                                layeredNavigator.pushOverlay(
                                    OverlayRoute.SettingsSub(SettingsSubRoute.Translation)
                                )
                            },
                            onOpenProviderModelSettings = { provider ->
                                layeredNavigator.pushOverlay(OverlayRoute.SettingsSub(SettingsSubRoute.ModelManagement))
                                layeredNavigator.pushOverlay(OverlayRoute.SettingsSub(SettingsSubRoute.VendorModels(provider)))
                            }
                        )
                    }
                }
                composable(MainRoute.Files) {
                    HuaJuanTheme(darkTheme = darkMode.value) {
                        FilesScreen(
                            onMenuClick = { toggleDrawer() },
                            drawerUiResetRequest = drawerUiResetRequest,
                            repository = repository
                        )
                    }
                }
                composable(MainRoute.RemoteTerminal) {
                    HuaJuanTheme(darkTheme = darkMode.value) {
                        RemoteTerminalScreen(
                            onMenuClick = { toggleDrawer() },
                            drawerUiResetRequest = drawerUiResetRequest
                        )
                    }
                }
                composable(MainRoute.Search) {
                    HuaJuanTheme(darkTheme = darkMode.value) {
                        ConversationSearchScreen(
                            repository = repository,
                            assistantId = appState.currentAssistantId ?: repository.getCurrentAssistantId(),
                            onBack = { layeredNavigator.navigateMain(MainRoute.Chat) },
                            onResultClick = { assistantId, conversationId, messageId ->
                                appState = appState.copy(currentAssistantId = assistantId)
                                appState = appState.copy(currentConversationId = conversationId)
                                pendingJumpMessageId = messageId
                                layeredNavigator.navigateMain(MainRoute.Chat)
                            }
                        )
                    }
                }
            }

            // 遮罩层 - 直接应用于主页面容器之上
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color.Black.copy(
                            alpha = (drawerOffset.value - minDrawerOffset) / (maxDrawerOffset - minDrawerOffset) * 0.3f
                        )
                    )
            )
        }
    }

    // 侧边栏
    // val drawerShadowWidth = 12.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(drawerOffset.value.roundToInt(), 0) }
    ) {
        val drawerAssistantId = conversationAssistantIdForRoute(currentMainRoute, appState)
        val highlightConversationId = selectedConversationIdForRoute(currentMainRoute, appState)
        HuaJuanTheme(darkTheme = darkMode.value) {
            SideDrawer(
                onChatSelected = {
                    layeredNavigator.navigateMain(MainRoute.Chat)
                },
                onConversationSelected = { conversationId ->
                    // 如果是特殊标识符"refresh_needed"，则只刷新对话列表而不切换页面
                    if (conversationId == "refresh_needed") {
                        scope.launch {
                            appState = appState.copy(
                                conversations = repository.getConversationsAsync(drawerAssistantId)
                            )
                        }
                        return@SideDrawer
                    }
                    if (conversationId.startsWith("deleted:")) {
                        val deletedId = conversationId.removePrefix("deleted:")
                        deletedChatConversationId = deletedId
                        deletedChatConversationVersion += 1
                        scope.launch {
                            val conversations = repository.getConversationsAsync(drawerAssistantId)
                            when (currentMainRoute) {
                                MainRoute.ImageGeneration -> {
                                    val nextId = appState.currentImageGenerationConversationId
                                        ?.takeUnless { it == deletedId }
                                        ?.takeIf { candidate -> conversations.any { it.id == candidate } }
                                        ?: conversations.firstOrNull()?.id
                                    appState = appState.copy(
                                        currentImageGenerationConversationId = nextId,
                                        conversations = conversations
                                    )
                                }
                                MainRoute.Translation -> {
                                    val nextId = appState.currentTranslationConversationId
                                        ?.takeUnless { it == deletedId }
                                        ?.takeIf { candidate -> conversations.any { it.id == candidate } }
                                        ?: conversations.firstOrNull()?.id
                                    appState = appState.copy(
                                        currentTranslationConversationId = nextId,
                                        conversations = conversations
                                    )
                                }
                                else -> {
                                    val nextId = appState.currentConversationId
                                        ?.takeUnless { it == deletedId }
                                        ?.takeIf { candidate -> conversations.any { it.id == candidate } }
                                        ?: conversations.firstOrNull()?.id
                                    appState = appState.copy(
                                        currentConversationId = nextId,
                                        conversations = conversations
                                    )
                                    layeredNavigator.navigateMain(MainRoute.Chat)
                                }
                            }
                        }
                        return@SideDrawer
                    }

                    val resolvedConversationId = if (conversationId == "default") null else conversationId
                    when (currentMainRoute) {
                        MainRoute.ImageGeneration -> {
                            appState = appState.copy(currentImageGenerationConversationId = resolvedConversationId)
                            scope.launch {
                                appState = appState.copy(
                                    conversations = repository.getConversationsAsync(ToolIds.ImageGeneration)
                                )
                            }
                            layeredNavigator.navigateMain(MainRoute.ImageGeneration)
                        }
                        MainRoute.Translation -> {
                            appState = appState.copy(currentTranslationConversationId = resolvedConversationId)
                            scope.launch {
                                appState = appState.copy(
                                    conversations = repository.getConversationsAsync(ToolIds.Translation)
                                )
                            }
                            layeredNavigator.navigateMain(MainRoute.Translation)
                        }
                        else -> {
                            appState = appState.copy(currentConversationId = resolvedConversationId)
                            scope.launch {
                                appState = appState.copy(
                                    conversations = repository.getConversationsAsync(
                                        appState.currentAssistantId ?: repository.getCurrentAssistantId()
                                    )
                                )
                            }
                            layeredNavigator.navigateMain(MainRoute.Chat)
                        }
                    }
                },
                onSettingPageSelected = {
                    layeredNavigator.navigateMain(MainRoute.Settings)
                },
                onAssistantListPageSelected = {
                    layeredNavigator.navigateMain(MainRoute.AssistantList)
                },
                onKnowledgeBaseSelected = {
                    layeredNavigator.navigateMain(MainRoute.KnowledgeBase)
                },
                onImageGenerationSelected = {
                    layeredNavigator.navigateMain(MainRoute.ImageGeneration)
                },
                onTranslationSelected = {
                    layeredNavigator.navigateMain(MainRoute.Translation)
                },
                onFilesSelected = {
                    layeredNavigator.navigateMain(MainRoute.Files)
                },
                onRemoteTerminalSelected = {
                    layeredNavigator.navigateMain(MainRoute.RemoteTerminal)
                },
                onOpenSearch = {
                    layeredNavigator.navigateMain(MainRoute.Search)
                },
                onDrawerDragState = drawerDragState,
                onDrawerDragStopped = { velocity -> settleDrawer(velocity) },
                conversations = appState.conversations,
                drawerWidth = drawerWidth,
                darkTheme = darkMode.value,
                repository = repository,
                currentConversationId = highlightConversationId, // 只允许一行高亮
                currentAssistantName = repository.getAssistantName(drawerAssistantId),
                currentAssistantId = drawerAssistantId,
                isChatSelected = currentMainRoute == MainRoute.Chat,
                isAssistantListSelected = currentMainRoute == MainRoute.AssistantList,
                isKnowledgeBaseSelected = currentMainRoute == MainRoute.KnowledgeBase,
                isImageGenerationSelected = currentMainRoute == MainRoute.ImageGeneration,
                isTranslationSelected = currentMainRoute == MainRoute.Translation,
                isFilesSelected = currentMainRoute == MainRoute.Files,
                isRemoteTerminalSelected = currentMainRoute == MainRoute.RemoteTerminal,
                isSearchSelected = currentMainRoute == MainRoute.Search,
                isSettingsSelected = currentMainRoute == MainRoute.Settings
            )
        }
    }

    // val drawerOpenProgress = ((drawerOffset.value - minDrawerOffset) / (maxDrawerOffset - minDrawerOffset))
    //     .coerceIn(0f, 1f)
    // val shadowAlpha by animateFloatAsState(
    //     targetValue = drawerOpenProgress * 0.08f,
    //     animationSpec = tween(180),
    //     label = "drawerShadowAlpha"
    // )
    // Box(
    //     modifier = Modifier
    //         .fillMaxHeight()
    //         .width(drawerShadowWidth)
    //         .offset {
    //             IntOffset(
    //                 (drawerOffset.value + drawerWidthPx).roundToInt(),
    //                 0
    //             )
    //         }
    //         .background(
    //             brush = Brush.horizontalGradient(
    //                 colors = listOf(
    //                     Color.Black.copy(alpha = shadowAlpha),
    //                     Color.Transparent
    //                 )
    //             )
    //         )
    // )

    LayeredOverlayHost(
        state = layeredNavigator,
        enter = NavigationTransitions.overlayEnter(),
        exit = NavigationTransitions.overlayExit()
    ) { route, onBack ->
        when (route) {
            OverlayRoute.ImageSelector -> {
                HuaJuanTheme(darkTheme = darkMode.value) {
                    ImageSelectorScreen(
                        onImageSelected = { selectedUris ->
                            selectedImageUris = selectedUris
                            onBack()
                        },
                        initialSelectedImages = selectedImageUris,
                        onBack = onBack
                    )
                }
            }
            is OverlayRoute.KnowledgeBaseDetail -> {
                HuaJuanTheme(darkTheme = darkMode.value) {
                    KnowledgeBaseDetailScreen(
                        knowledgeBase = route.knowledgeBase,
                        repository = repository,
                        onBack = onBack,
                        onOpenSettings = {
                            layeredNavigator.pushOverlay(
                                OverlayRoute.KnowledgeBaseSettings(route.knowledgeBase.id)
                            )
                        },
                        onOpenSearch = {
                            layeredNavigator.pushOverlay(
                                OverlayRoute.KnowledgeBaseSearch(route.knowledgeBase.id)
                            )
                        },
                        onOpenProviderModelSettings = { provider ->
                            layeredNavigator.pushOverlay(OverlayRoute.SettingsSub(SettingsSubRoute.ModelManagement))
                            layeredNavigator.pushOverlay(OverlayRoute.SettingsSub(SettingsSubRoute.VendorModels(provider)))
                        },
                        refreshToken = knowledgeBaseRefreshToken
                    )
                }
            }
            is OverlayRoute.KnowledgeBaseSettings -> {
                HuaJuanTheme(darkTheme = darkMode.value) {
                    KnowledgeBaseSettingsScreen(
                        repository = repository,
                        kbId = route.kbId,
                        onBack = onBack,
                        onOpenProviderModelSettings = { provider ->
                            layeredNavigator.pushOverlay(OverlayRoute.SettingsSub(SettingsSubRoute.ModelManagement))
                            layeredNavigator.pushOverlay(OverlayRoute.SettingsSub(SettingsSubRoute.VendorModels(provider)))
                        },
                        onUpdated = { knowledgeBaseRefreshToken += 1 },
                        onDeleted = {
                            knowledgeBaseRefreshToken += 1
                            layeredNavigator.popOverlay()
                            layeredNavigator.popOverlay()
                        }
                    )
                }
            }
            is OverlayRoute.KnowledgeBaseSearch -> {
                HuaJuanTheme(darkTheme = darkMode.value) {
                    KnowledgeBaseSearchScreen(
                        repository = repository,
                        kbId = route.kbId,
                        onBack = onBack
                    )
                }
            }
            OverlayRoute.KnowledgeBaseCreate -> {
                HuaJuanTheme(darkTheme = darkMode.value) {
                    com.huajuan.aispace.screens.knowledge.CreateKnowledgeBaseScreen(
                        repository = repository,
                        onBack = onBack,
                        onOpenProviderModelSettings = { provider ->
                            layeredNavigator.pushOverlay(OverlayRoute.SettingsSub(SettingsSubRoute.ModelManagement))
                            layeredNavigator.pushOverlay(OverlayRoute.SettingsSub(SettingsSubRoute.VendorModels(provider)))
                        },
                        onCreated = {
                            knowledgeBaseRefreshToken += 1
                            onBack()
                        }
                    )
                }
            }
            is OverlayRoute.SettingsSub -> {
                HuaJuanTheme(darkTheme = darkMode.value) {
                    com.huajuan.aispace.screens.settings.SettingsSubScreen(
                        repository = repository,
                        darkModeState = darkMode,
                        route = route.route,
                        onBack = onBack,
                        onNavigate = { next -> layeredNavigator.pushOverlay(OverlayRoute.SettingsSub(next)) },
                        onSwitchAssistant = { assistantId ->
                            appState = appState.copy(
                                currentAssistantId = assistantId,
                                currentConversationId = null
                            )
                        }
                    )
                }
            }
            is OverlayRoute.AssistantSettingsHome -> {
                HuaJuanTheme(darkTheme = darkMode.value) {
                    AssistantSettingsHomeScreen(
                        repository = repository,
                        assistantId = route.assistantId,
                        onBack = onBack,
                        onOpenAdvanced = {
                            layeredNavigator.pushOverlay(
                                OverlayRoute.AssistantSettingsAdvanced(route.assistantId)
                            )
                        },
                        onSwitchAssistant = { assistantId ->
                            scope.launch {
                                val agent = repository.getAssistantById(assistantId)
                                val newConversation = repository.createNewConversation(
                                    assistantId = assistantId,
                                    title = repository.getCurrentDefaultConversationTitle(),
                                    roleName = agent?.name ?: repository.getDefaultAssistantName(),
                                    systemPrompt = agent?.systemPrompt ?: repository.getDefaultAssistantPrompt()
                                )
                                repository.saveMessages(newConversation.id, assistantId, emptyList())
                                appState = appState.copy(
                                    currentAssistantId = assistantId,
                                    currentConversationId = newConversation.id,
                                    conversations = repository.getConversationsAsync(assistantId)
                                )
                            }
                            layeredNavigator.navigateMain(MainRoute.Chat)
                            layeredNavigator.popOverlay()
                        },
                        onDeleteAssistant = { assistantId ->
                            val fallbackAssistantId = repository.deleteCustomAssistant(assistantId)
                            if (fallbackAssistantId != null) {
                                appState = appState.copy(
                                    currentAssistantId = fallbackAssistantId,
                                    currentConversationId = null,
                                    conversations = emptyList()
                                )
                            }
                            layeredNavigator.popOverlay()
                        }
                    )
                }
            }
            is OverlayRoute.AssistantSettingsAdvanced -> {
                HuaJuanTheme(darkTheme = darkMode.value) {
                    AssistantSettingsAdvancedScreen(
                        repository = repository,
                        assistantId = route.assistantId,
                        onBack = onBack,
                        onOpenSection = { section ->
                            layeredNavigator.pushOverlay(
                                OverlayRoute.AssistantSettingsDetail(route.assistantId, section)
                            )
                        }
                    )
                }
            }
            is OverlayRoute.AssistantSettingsDetail -> {
                HuaJuanTheme(darkTheme = darkMode.value) {
                    AssistantSettingsDetailScreen(
                        repository = repository,
                        assistantId = route.assistantId,
                        section = route.section,
                        onBack = onBack,
                        onOpenKnowledgeBase = {},
                        onOpenMcpSettings = {
                            layeredNavigator.pushOverlay(
                                OverlayRoute.SettingsSub(
                                    com.huajuan.aispace.screens.settings.SettingsSubRoute.McpTools
                                )
                            )
                        },
                        onOpenGlobalMemory = {
                            layeredNavigator.pushOverlay(
                                OverlayRoute.SettingsSub(
                                    com.huajuan.aispace.screens.settings.SettingsSubRoute.GlobalMemory
                                )
                            )
                        }
                    )
                }
            }
        }
    }

    // 当侧边栏展开时，点击主页面露出的部分视为返回操作
    if (drawerOffset.value > minDrawerOffset) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = drawerWidth)
                .clickable(
                    indication = null,
                    interactionSource = null
                ) {
                    closeDrawer()
                }
        )
    }

    BackHandler(enabled = true) {
        layeredNavigator.handleBack()
    }
}
