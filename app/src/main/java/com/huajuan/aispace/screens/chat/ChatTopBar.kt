package com.huajuan.aispace.screens.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.huajuan.aispace.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    conversationTitle: String,
    onMenuClick: () -> Unit,
    onCreateConversation: () -> Unit,
    onToggleSearch: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = conversationTitle.ifBlank { stringResource(R.string.conversation_new) },
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Outlined.Menu, contentDescription = stringResource(R.string.cd_open_drawer))
            }
        },
        actions = {
            // 不要删除这段搜索按钮代码，后续还会恢复使用；当前仅按需求临时注释。
             IconButton(onClick = onToggleSearch) {
                 Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.cd_search_messages))
             }
            IconButton(onClick = onCreateConversation) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_new_conversation))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
