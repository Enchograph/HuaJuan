package com.chenhongyu.huajuan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
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
                }
            )
        }
    ) {
        when (currentPage.value) {
            0 -> ChatScreen(drawerState)
            1 -> SettingScreen()
        }
    }
}