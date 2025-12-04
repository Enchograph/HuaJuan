package com.chenhongyu.huajuan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.OnBackPressedCallback
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.chenhongyu.huajuan.data.Repository
import com.chenhongyu.huajuan.ui.theme.HuaJuanTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private lateinit var repository: Repository
    private var onBackPressedCallback: OnBackPressedCallback? = null
    private var closeDrawerCallback: (() -> Unit)? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 初始化Repository
        repository = Repository(this)
        
        setContent {
            MainApp(repository) { closeCallback ->
                // 保存关闭侧边栏的回调函数
                closeDrawerCallback = closeCallback
            }
        }
        
        // 处理返回键事件
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 调用关闭侧边栏的回调函数
                closeDrawerCallback?.invoke()
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback!!)
    }
}

@Composable
fun MainApp(
    repository: Repository,
    onCloseDrawerCallback: (()->Unit) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    
    // 获取屏幕宽度
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val drawerWidth = screenWidth * 0.75f
    val drawerWidthPx = with(density) { drawerWidth.toPx() }
    val screenWidthPx = with(density) { screenWidth.toPx() }
    
    // 当前页面状态：0-聊天页，1-设置页，2-AI创作页，3-智能体页
    val currentPage = remember { mutableStateOf(0) }
    
    // 从Repository获取深色模式设置
    val darkMode = remember { mutableStateOf(repository.getDarkMode()) }
    
    val appState = remember { AppState() }
    
    // 抽屉状态和位置控制
    val drawerOffset = remember { Animatable(-drawerWidthPx) }
    val maxDrawerOffset = 0f
    val minDrawerOffset = -drawerWidthPx
    
    LaunchedEffect(Unit) {
        drawerOffset.snapTo(minDrawerOffset)
    }
    
    // 提供关闭侧边栏的回调函数
    onCloseDrawerCallback {
        scope.launch {
            drawerOffset.animateTo(minDrawerOffset, spring(stiffness = Spring.StiffnessMediumLow))
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .draggable(
                state = rememberDraggableState { delta ->
                    scope.launch {
                        drawerOffset.snapTo((drawerOffset.value + delta).coerceIn(minDrawerOffset, maxDrawerOffset))
                    }
                },
                orientation = Orientation.Horizontal,
                onDragStopped = { velocity ->
                    // 根据速度和位置决定是打开还是关闭抽屉
                    scope.launch {
                        if (velocity > 0 || drawerOffset.value > minDrawerOffset / 2) {
                            // 打开抽屉
                            drawerOffset.animateTo(maxDrawerOffset, spring(stiffness = Spring.StiffnessMediumLow))
                        } else {
                            // 关闭抽屉
                            drawerOffset.animateTo(minDrawerOffset, spring(stiffness = Spring.StiffnessMediumLow))
                        }
                    }
                }
            )
    ) {
        // 主页面容器（包含所有主页面）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset((drawerOffset.value + drawerWidthPx).roundToInt(), 0) }
        ) {
            // 主页面内容
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // 页面内容容器 - 使用偏移来切换页面
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset { 
                            IntOffset(
                                if (currentPage.value == 0) 0 else 
                                if (currentPage.value == 1) -screenWidthPx.roundToInt() else
                                if (currentPage.value == 2) -(screenWidthPx * 2).roundToInt() else
                                -(screenWidthPx * 3).roundToInt(), 
                                0
                            ) 
                        }
                ) {
                    // 聊天页面
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset { IntOffset(0, 0) }
                    ) {
                        HuaJuanTheme(darkTheme = darkMode.value) {
                            ChatScreen(
                                onMenuClick = { 
                                    scope.launch {
                                        if (drawerOffset.value == minDrawerOffset) {
                                            drawerOffset.animateTo(maxDrawerOffset, spring(stiffness = Spring.StiffnessMediumLow))
                                        } else {
                                            drawerOffset.animateTo(minDrawerOffset, spring(stiffness = Spring.StiffnessMediumLow))
                                        }
                                    }
                                }, 
                                appState, 
                                darkMode.value, 
                                repository
                            )
                        }
                    }
                    
                    // 设置页面
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset { IntOffset(screenWidthPx.roundToInt(), 0) }
                    ) {
                        HuaJuanTheme(darkTheme = darkMode.value) {
                            SettingScreen(
                                repository, 
                                darkMode,
                                onMenuClick = {
                                    scope.launch {
                                        if (drawerOffset.value == minDrawerOffset) {
                                            drawerOffset.animateTo(maxDrawerOffset, spring(stiffness = Spring.StiffnessMediumLow))
                                        } else {
                                            drawerOffset.animateTo(minDrawerOffset, spring(stiffness = Spring.StiffnessMediumLow))
                                        }
                                    }
                                },
                                onBack = { 
                                    // 切换回聊天页面（无动画）
                                    currentPage.value = 0
                                    // 随后触发动画隐藏侧边栏
                                    scope.launch {
                                        drawerOffset.animateTo(minDrawerOffset, spring(stiffness = Spring.StiffnessMediumLow))
                                    }
                                }
                            )
                        }
                    }
                    
                    // AI 创作页面
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset { IntOffset((screenWidthPx * 2).roundToInt(), 0) }
                    ) {
                        HuaJuanTheme(darkTheme = darkMode.value) {
                            AICreationScreen(
                                onMenuClick = {
                                    scope.launch {
                                        if (drawerOffset.value == minDrawerOffset) {
                                            drawerOffset.animateTo(maxDrawerOffset, spring(stiffness = Spring.StiffnessMediumLow))
                                        } else {
                                            drawerOffset.animateTo(minDrawerOffset, spring(stiffness = Spring.StiffnessMediumLow))
                                        }
                                    }
                                },
                                onBack = { 
                                    // 切换回聊天页面（无动画）
                                    currentPage.value = 0
                                    // 随后触发动画隐藏侧边栏
                                    scope.launch {
                                        drawerOffset.animateTo(minDrawerOffset, spring(stiffness = Spring.StiffnessMediumLow))
                                    }
                                }
                            )
                        }
                    }
                    
                    // 智能体页面
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset { IntOffset((screenWidthPx * 3).roundToInt(), 0) }
                    ) {
                        HuaJuanTheme(darkTheme = darkMode.value) {
                            AgentScreen(
                                onMenuClick = {
                                    scope.launch {
                                        if (drawerOffset.value == minDrawerOffset) {
                                            drawerOffset.animateTo(maxDrawerOffset, spring(stiffness = Spring.StiffnessMediumLow))
                                        } else {
                                            drawerOffset.animateTo(minDrawerOffset, spring(stiffness = Spring.StiffnessMediumLow))
                                        }
                                    }
                                },
                                onBack = { 
                                    // 切换回聊天页面（无动画）
                                    currentPage.value = 0
                                    // 随后触发动画隐藏侧边栏
                                    scope.launch {
                                        drawerOffset.animateTo(minDrawerOffset, spring(stiffness = Spring.StiffnessMediumLow))
                                    }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(drawerOffset.value.roundToInt(), 0) }
        ) {
            SideDrawer(
                onChatPageSelected = { conversationId -> 
                    appState.currentConversationId = conversationId
                    // 切换到聊天页面（无动画）
                    currentPage.value = 0
                    // 随后触发动画隐藏侧边栏
                    scope.launch { 
                        drawerOffset.animateTo(minDrawerOffset, spring(stiffness = Spring.StiffnessMediumLow))
                    }
                },
                onSettingPageSelected = { 
                    // 切换到设置页面（无动画）
                    currentPage.value = 1
                    // 随后触发动画隐藏侧边栏
                    scope.launch { 
                        drawerOffset.animateTo(minDrawerOffset, spring(stiffness = Spring.StiffnessMediumLow))
                    }
                },
                onAICreationPageSelected = {
                    // 切换到AI创作页面（无动画）
                    currentPage.value = 2
                    // 随后触发动画隐藏侧边栏
                    scope.launch {
                        drawerOffset.animateTo(minDrawerOffset, spring(stiffness = Spring.StiffnessMediumLow))
                    }
                },
                onAgentPageSelected = {
                    // 切换到智能体页面（无动画）
                    currentPage.value = 3
                    // 随后触发动画隐藏侧边栏
                    scope.launch {
                        drawerOffset.animateTo(minDrawerOffset, spring(stiffness = Spring.StiffnessMediumLow))
                    }
                },
                conversations = appState.conversations,
                drawerWidth = drawerWidth,
                darkTheme = darkMode.value
            )
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
                        scope.launch {
                            drawerOffset.animateTo(minDrawerOffset, spring(stiffness = Spring.StiffnessMediumLow))
                        }
                    }
            )
        }
    }
}
