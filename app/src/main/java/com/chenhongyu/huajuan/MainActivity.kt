package com.chenhongyu.huajuan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.chenhongyu.huajuan.data.Repository
import com.chenhongyu.huajuan.ui.theme.HuaJuanTheme
import kotlinx.coroutines.launch

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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // 获取屏幕宽度
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val drawerWidth = screenWidth * 0.75f
    
    // 当前页面状态：0-聊天页，1-设置页
    val currentPage = remember { mutableStateOf(0) }
    
    // 从Repository获取深色模式设置
    val darkMode = remember { mutableStateOf(repository.getDarkMode()) }
    
    val appState = remember { AppState() }
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SideDrawer(
                onChatPageSelected = { conversationId -> 
                    appState.currentConversationId = conversationId
                    currentPage.value = 0
                    scope.launch { drawerState.close() }
                },
                onSettingPageSelected = { 
                    currentPage.value = 1
                    scope.launch { drawerState.close() }
                },
                conversations = appState.conversations
            )
        }
    ) {
        // 根据抽屉状态确定背景暗度
        val scrimColor = if (drawerState.isOpen) {
            Color.Black.copy(alpha = 0.3f)
        } else {
            Color.Transparent
        }
        
        HuaJuanTheme(darkTheme = darkMode.value) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrimColor)
            ) {
                when (currentPage.value) {
                    0 -> ChatScreen(drawerState, appState, darkMode.value, repository)
                    1 -> SettingScreen(repository, darkMode, onBack = { currentPage.value = 0 })
                    else -> ChatScreen(drawerState, appState, darkMode.value, repository)
                }
            }
        }
    }
}