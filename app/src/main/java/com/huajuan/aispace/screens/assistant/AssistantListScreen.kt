package com.huajuan.aispace.screens.assistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.huajuan.aispace.R
import com.huajuan.aispace.components.AssistantEmojiIcon
import com.huajuan.aispace.components.SearchField
import com.huajuan.aispace.data.Agent
import com.huajuan.aispace.data.AgentProvider
import com.huajuan.aispace.data.AssistantCategory
import com.huajuan.aispace.data.Repository
import com.huajuan.aispace.components.settings.SettingsItemDivider
import com.huajuan.aispace.components.settings.SettingsListItem
import com.huajuan.aispace.components.settings.SettingsSectionCard
import com.huajuan.aispace.navigation.DrawerUiResetEffect
import com.huajuan.aispace.ui.theme.AppDimens
import java.util.UUID

/**
 * 助手列表页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantListScreen(
    onMenuClick: () -> Unit,
    drawerUiResetRequest: Int = 0,
    repository: Repository,
    currentAssistantId: String,
    onAssistantSelected: (String) -> Unit = {},
    onOpenAssistantSettings: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val agentProvider = remember(context) { AgentProvider(context) }
    val assistantCatalogVersion by repository.getAssistantCatalogVersionFlow().collectAsState()
    val agents = agentProvider.getAgents()
    val categories = agentProvider.getCategories()
    var selectedSection by remember { mutableStateOf(AssistantSection.BuiltIn) }
    var selectedCategory by remember { mutableStateOf(AssistantCategory.All.id) }
    var searchQuery by remember { mutableStateOf("") }
    DrawerUiResetEffect(drawerUiResetRequest) {
        searchQuery = ""
    }
    val starredIds = remember(assistantCatalogVersion) { repository.getStarredAssistantIds() }
    val builtInAgents = remember(assistantCatalogVersion, agents) {
        agents.map { agent -> repository.getAssistantById(agent.id) ?: agent }
    }
    val userAgents = remember(assistantCatalogVersion) { repository.getCustomAssistants() }

    val displayedAgents = remember(selectedCategory, builtInAgents) {
        if (selectedCategory == AssistantCategory.All.id) {
            builtInAgents
        } else {
            builtInAgents.filter { it.category == selectedCategory }
        }
    }
    val combinedAgents = remember(builtInAgents, userAgents) { builtInAgents + userAgents }
    val filteredStarred = remember(starredIds, combinedAgents, searchQuery) {
        combinedAgents.filter { agent ->
            agent.id in starredIds && matchesAssistantQuery(agent, searchQuery)
        }
    }
    val filteredUserAgents = remember(userAgents, searchQuery) {
        userAgents.filter { agent -> matchesAssistantQuery(agent, searchQuery) }
    }
    val filteredBuiltInAgents = remember(displayedAgents, searchQuery) {
        displayedAgents.filter { agent -> matchesAssistantQuery(agent, searchQuery) }
    }
    val createAssistantAndOpenSettings = {
        val newAgent = Agent(
            id = "custom_${UUID.randomUUID()}",
            name = context.getString(R.string.assistant_new_name),
            description = context.getString(R.string.assistant_new_description),
            systemPrompt = repository.getDefaultAssistantPrompt(),
            category = AssistantCategory.User.id,
            iconResId = 0,
            emoji = "✨"
        )
        repository.setCustomAssistants(userAgents + newAgent)
        onOpenAssistantSettings(newAgent.id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.assistant_list_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Outlined.Menu, contentDescription = stringResource(R.string.cd_open_drawer))
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
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AssistantSectionTabs(
                selectedSection = selectedSection,
                onSectionSelected = { selectedSection = it }
            )

            SearchField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = stringResource(R.string.assistant_search),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.screenPadding, vertical = AppDimens.spacingS)
            )

            when (selectedSection) {
                AssistantSection.Starred -> {
                    if (filteredStarred.isEmpty()) {
                        EmptyStateWithAction(
                            message = stringResource(R.string.assistant_empty_starred),
                            actionLabel = stringResource(R.string.assistant_add),
                            onAction = createAssistantAndOpenSettings
                        )
                    } else {
                        AssistantList(
                            agents = filteredStarred,
                            currentAssistantId = currentAssistantId,
                            starredIds = starredIds,
                            onStarToggle = { agentId ->
                                repository.setStarredAssistantIds(toggleStar(starredIds, agentId))
                            },
                            onOpenSettings = { agent ->
                                onOpenAssistantSettings(agent.id)
                            },
                            showAddButton = true,
                            onAddAssistant = createAssistantAndOpenSettings,
                            onAssistantSelected = { agent ->
                                onAssistantSelected(agent.id)
                            }
                        )
                    }
                }
                AssistantSection.UserAdded -> {
                    if (filteredUserAgents.isEmpty()) {
                        EmptyStateWithAction(
                            message = stringResource(R.string.assistant_empty_user),
                            actionLabel = stringResource(R.string.assistant_add),
                            onAction = createAssistantAndOpenSettings
                        )
                    } else {
                        AssistantList(
                            agents = filteredUserAgents,
                            currentAssistantId = currentAssistantId,
                            starredIds = starredIds,
                            onStarToggle = { agentId ->
                                repository.setStarredAssistantIds(toggleStar(starredIds, agentId))
                            },
                            onOpenSettings = { agent ->
                                onOpenAssistantSettings(agent.id)
                            },
                            showAddButton = true,
                            onAddAssistant = createAssistantAndOpenSettings,
                            onAssistantSelected = { agent ->
                                onAssistantSelected(agent.id)
                            }
                        )
                    }
                }
                AssistantSection.BuiltIn -> {
                    CategoryTabs(
                        categories = categories,
                        selectedCategory = selectedCategory,
                        categoryLabel = agentProvider::getCategoryLabel,
                        onCategorySelected = { selectedCategory = it }
                    )
                    if (filteredBuiltInAgents.isEmpty()) {
                        EmptyState(message = stringResource(R.string.assistant_empty_search))
                    } else {
                        AssistantList(
                            agents = filteredBuiltInAgents,
                            currentAssistantId = currentAssistantId,
                            starredIds = starredIds,
                            onStarToggle = { agentId ->
                                repository.setStarredAssistantIds(toggleStar(starredIds, agentId))
                            },
                            onOpenSettings = { agent ->
                                onOpenAssistantSettings(agent.id)
                            },
                            showAddButton = false,
                            onAssistantSelected = { agent ->
                                onAssistantSelected(agent.id)
                            }
                        )
                    }
                }
            }
        }
    }

}

private enum class AssistantSection {
    Starred,
    UserAdded,
    BuiltIn
}

@Composable
private fun AssistantSectionTabs(
    selectedSection: AssistantSection,
    onSectionSelected: (AssistantSection) -> Unit
) {
    val sections = AssistantSection.values().toList()
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.screenPadding, vertical = AppDimens.spacingS)
    ) {
        sections.forEach { section ->
            SegmentedButton(
                selected = selectedSection == section,
                onClick = { onSectionSelected(section) },
                shape = MaterialTheme.shapes.small,
                label = {
                    Text(
                        when (section) {
                            AssistantSection.Starred -> stringResource(R.string.assistant_section_starred)
                            AssistantSection.UserAdded -> stringResource(R.string.assistant_section_user)
                            AssistantSection.BuiltIn -> stringResource(R.string.assistant_section_builtin)
                        }
                    )
                }
            )
        }
    }
}

@Composable
fun CategoryTabs(
    categories: List<String>,
    selectedCategory: String,
    categoryLabel: (String) -> String,
    onCategorySelected: (String) -> Unit
) {
    val safeIndex = categories.indexOf(selectedCategory).let { if (it < 0) 0 else it }
    PrimaryScrollableTabRow(
        selectedTabIndex = safeIndex,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppDimens.spacingS),
        edgePadding = AppDimens.screenPadding,
        divider = {},
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        categories.forEach { category ->
            Tab(
                selected = category == selectedCategory,
                onClick = { onCategorySelected(category) },
                text = {
                    Text(
                        text = categoryLabel(category),
                        fontWeight = if (category == selectedCategory) FontWeight.Bold else FontWeight.Normal
                    )
                },
                modifier = Modifier
                    .height(40.dp)
                    .padding(horizontal = AppDimens.spacingS),
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AssistantList(
    agents: List<Agent>,
    currentAssistantId: String,
    starredIds: Set<String> = emptySet(),
    onStarToggle: (String) -> Unit = {},
    onOpenSettings: (Agent) -> Unit = {},
    showAddButton: Boolean = false,
    onAddAssistant: () -> Unit = {},
    onAssistantSelected: (Agent) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(AppDimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(AppDimens.listSpacing)
    ) {
        item {
            SettingsSectionCard {
                agents.forEachIndexed { index, agent ->
                    AssistantListItem(
                        agent = agent,
                        isSelected = agent.id == currentAssistantId,
                        isStarred = agent.id in starredIds,
                        onStarToggle = { onStarToggle(agent.id) },
                        onOpenSettings = { onOpenSettings(agent) },
                        onClick = { onAssistantSelected(agent) }
                    )
                    if (index != agents.lastIndex) {
                        SettingsItemDivider()
                    }
                }
            }
        }
        if (showAddButton) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AppDimens.spacingM),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = onAddAssistant,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(stringResource(R.string.assistant_add))
                    }
                }
            }
        }
    }
}

@Composable
fun AssistantListItem(
    agent: Agent,
    isSelected: Boolean,
    isStarred: Boolean,
    onStarToggle: () -> Unit,
    onOpenSettings: () -> Unit,
    onClick: () -> Unit
) {
    SettingsListItem(
        title = agent.name,
        subtitle = agent.description,
        leading = {
            AssistantEmojiIcon(
                emoji = agent.emoji,
                selected = isSelected
            )
        },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onStarToggle) {
                    Icon(
                        imageVector = if (isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (isStarred) {
                            stringResource(R.string.assistant_unstar)
                        } else {
                            stringResource(R.string.assistant_star)
                        },
                        tint = if (isStarred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.assistant_settings),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        onClick = onClick,
        subtitleMaxLines = 1,
        itemPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)
    )
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppDimens.spacingXxl),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyStateWithAction(
    message: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppDimens.spacingXxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(AppDimens.spacingM))
        Button(onClick = onAction) {
            Text(actionLabel)
        }
    }
}

private fun matchesAssistantQuery(agent: Agent, query: String): Boolean {
    if (query.isBlank()) return true
    return agent.name.contains(query, ignoreCase = true) ||
            agent.description.contains(query, ignoreCase = true) ||
            agent.category.contains(query, ignoreCase = true)
}

private fun toggleStar(current: Set<String>, id: String): Set<String> =
    if (current.contains(id)) current - id else current + id
