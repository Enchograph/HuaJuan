package com.chenhongyu.huajuan

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenhongyu.huajuan.data.Agent
import com.chenhongyu.huajuan.data.AgentProvider
import com.chenhongyu.huajuan.data.Repository
import kotlinx.coroutines.launch

/**
 * 发现智能体页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentScreen(
    onMenuClick: () -> Unit,
    onBack: () -> Unit,
    repository: Repository,
    onConversationCreated: (String) -> Unit = {} // callback to notify parent of created conversation id
) {
    val agentProvider = AgentProvider()
    val agents = agentProvider.getAgents()
    val categories = agentProvider.getCategories()
    var selectedCategory by remember { mutableStateOf("推荐") }
    val scope = rememberCoroutineScope()
    
    // 按类别分组智能体
    val groupedAgents = agents.groupBy { it.category }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "发现智能体",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "返回")
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
            // 分类标签
            CategoryTabs(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )
            
            // 智能体列表
            AgentList(
                agents = groupedAgents[selectedCategory] ?: emptyList(),
                onAgentSelected = { agent ->
                    scope.launch {
                        // 创建新对话并设置角色名称和系统提示词
                        val newConversation = repository.createNewConversation(
                            title = agent.name, 
                            roleName = agent.name, 
                            systemPrompt = agent.systemPrompt
                        )
                        // notify parent immediately so it can update AppState and UI
                        onConversationCreated(newConversation.id)
                        // 切换回聊天页面
                        onBack()
                    }
                },
                repository = repository
            )
        }
    }
}

@Composable
fun CategoryTabs(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = categories.indexOf(selectedCategory),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        edgePadding = 16.dp,
        divider = {}, // 移除默认分割线
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        categories.forEach { category ->
            Tab(
                selected = category == selectedCategory,
                onClick = { onCategorySelected(category) },
                text = {
                    Text(
                        text = category,
                        fontWeight = if (category == selectedCategory) FontWeight.Bold else FontWeight.Normal
                    )
                },
                modifier = Modifier
                    .height(40.dp)
                    .padding(horizontal = 8.dp),
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AgentList(
    agents: List<Agent>,
    onAgentSelected: (Agent) -> Unit,
    repository: Repository
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(agents) { agent ->
            AgentCard(
                agent = agent,
                onClick = {
                    // 创建新对话并设置角色名称和系统提示词
                    onAgentSelected(agent)
                }
            )
        }
    }
}

@Composable
fun AgentCard(
    agent: Agent,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with initials
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = agent.name.take(1),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 文本内容
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = agent.name,  // 显示角色名称
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = agent.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = agent.systemPrompt,  // 显示系统提示词预览
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            
            // 新建对话按钮 — 更直观的交互：用Add图标并加一个Chevron指示导航
            IconButton(
                onClick = onClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "新建对话",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}