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
import com.chenhongyu.huajuan.ui.theme.HuaJuanTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HuaJuanTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // 获取屏幕宽度
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val drawerWidth = screenWidth * 0.75f
    
    // 应用状态管理
    // val appState = remember { mutableStateOf(AppState()) }
    
    // 当前页面状态：0-聊天页，1-设置页
    val currentPage = remember { mutableStateOf(0) }
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SideDrawer(
                onChatPageSelected = { 
                    currentPage.value = 0
                    scope.launch { drawerState.close() }
                },
                onSettingPageSelected = { 
                    currentPage.value = 1
                    scope.launch { drawerState.close() }
                },
                conversations = AppState().conversations
            )
        }
    ) {
        // 根据抽屉状态确定背景暗度
        val scrimColor = if (drawerState.isOpen) {
            Color.Black.copy(alpha = 0.3f)
        } else {
            Color.Transparent
        }
        
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrimColor)
        ) {
            when (currentPage.value) {
                0 -> ChatScreen(drawerState)
                1 -> SettingScreen()
                else -> ChatScreen(drawerState)
            }
        }
    }
}