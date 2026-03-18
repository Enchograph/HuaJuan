package com.huajuan.aispace.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.huajuan.aispace.R
import com.huajuan.aispace.components.SearchField
import com.huajuan.aispace.data.MessageSearchResult
import com.huajuan.aispace.data.Repository
import com.huajuan.aispace.data.ToolIds
import com.huajuan.aispace.ui.theme.AppDimens
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun ConversationSearchScreen(
    repository: Repository,
    assistantId: String,
    onBack: () -> Unit,
    onResultClick: (assistantId: String, conversationId: String, messageId: String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<MessageSearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        val current = query.trim()
        if (current.isBlank()) {
            results = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(250)
        if (current == query.trim()) {
            val tokens = current
                .replace(Regex("[^\\p{L}\\p{Nd}]+"), " ")
                .trim()
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() }
            if (tokens.isEmpty()) {
                results = emptyList()
                isSearching = false
                return@LaunchedEffect
            }
            val ftsQuery = tokens.joinToString(" AND ") { "$it*" }
            results = repository.searchAllMessagesAsync(ftsQuery, 100)
                .filterNot { ToolIds.toolConversationIds.contains(it.assistantId) }
            isSearching = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.drawer_search)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.cd_back))
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
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            SearchField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.screenPadding, vertical = AppDimens.spacingS),
                placeholder = stringResource(R.string.drawer_search),
                leadingIcon = Icons.Outlined.Search,
                singleLine = true
            )

            AnimatedVisibility(
                visible = query.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = if (isSearching) stringResource(R.string.chat_searching) else stringResource(R.string.chat_search_result_count, results.size),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = AppDimens.spacingL, bottom = AppDimens.spacingS)
                )
            }

            if (query.isNotBlank() && !isSearching && results.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AppDimens.spacingXxl),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.chat_search_no_result),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = AppDimens.screenPadding,
                        vertical = AppDimens.spacingS
                    ),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.listSpacing)
                ) {
                    items(results, key = { it.messageId }) { item ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onResultClick(item.assistantId, item.conversationId, item.messageId) }
                        ) {
                            Column(modifier = Modifier.padding(AppDimens.cardSpacing)) {
                                Text(
                                    text = item.conversationTitle.ifBlank { stringResource(R.string.conversation_new) },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = item.messageText.trim().ifBlank { stringResource(R.string.unknown) },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = AppDimens.spacingXs)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
