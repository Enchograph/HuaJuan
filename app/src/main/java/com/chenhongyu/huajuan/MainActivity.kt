package com.chenhongyu.huajuan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 初始化Repository
        repository = Repository(this)
        
        setContent {
            MainApp(repository)
        }
    }
}

@Composable
fun MainApp(repository: Repository) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    
    // 获取屏幕宽度
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val drawerWidth = screenWidth * 0.75f
    val drawerWidthPx = with(density) { drawerWidth.toPx() }
    
    // 当前页面状态：0-聊天页，1-设置页
    val currentPage = remember { mutableStateOf(0) }
    
    // 从Repository获取深色模式设置
    val darkMode = remember { mutableStateOf(repository.getDarkMode()) }
    
    val appState = remember { AppState() }
    
    // 抽屉状态和位置控制
    val drawerOffset = remember { Animatable(-drawerWidthPx) }
    val maxDrawerOffset = 0f
    val minDrawerOffset = -drawerWidthPx
    
    // 计算主页面的偏移量（与抽屉联动）
    val mainPageOffset = drawerOffset.value + drawerWidthPx
    
    // 计算遮罩透明度
    val scrimAlpha = (drawerOffset.value - minDrawerOffset) / (maxDrawerOffset - minDrawerOffset) * 0.3f
    val scrimColor = Color.Black.copy(alpha = scrimAlpha)
    
    LaunchedEffect(Unit) {
        drawerOffset.snapTo(minDrawerOffset)
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
        // 主页面内容
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(mainPageOffset.roundToInt(), 0) }
                .background(scrimColor)
        ) {
            HuaJuanTheme(darkTheme = darkMode.value) {
                when (currentPage.value) {
                    0 -> ChatScreen(
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
                    1 -> SettingScreen(repository, darkMode, onBack = { currentPage.value = 0 })
                    else -> ChatScreen(
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
                    currentPage.value = 0
                    scope.launch { 
                        drawerOffset.animateTo(minDrawerOffset, spring(stiffness = Spring.StiffnessMediumLow))
                    }
                },
                onSettingPageSelected = { 
                    currentPage.value = 1
                    scope.launch { 
                        drawerOffset.animateTo(minDrawerOffset, spring(stiffness = Spring.StiffnessMediumLow))
                    }
                },
                conversations = appState.conversations,
                drawerWidth = drawerWidth
            )
        }
    }
}