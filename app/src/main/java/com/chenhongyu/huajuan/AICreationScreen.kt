package com.chenhongyu.huajuan

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

/**
 * AI创作页面 - 前端骨架实现
 * - 顶部 AppBar 已保留
 * - 增加按 AI 角色的横向筛选（Chips）
 * - 使用两列瀑布流（实现为左右两列 LazyColumn）展示卡片（目前用占位色块模拟图片）
 * - 卡片上覆盖用户名/角色/时间等元信息
 *
 * 后续工作：将占位色块替换为真实图片加载（Coil/Glide -> 从文件加载），
 * 并用 Room/ViewModel 提供真实数据。
 */

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AICreationScreen(
    onMenuClick: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current

    // sample roles and sample items
    val sampleRoles = remember { listOf("默认", "角色A", "角色B", "角色C") }

    // create some placeholder items with varying heights to simulate waterfall
    val now = System.currentTimeMillis()
    val sampleItems = remember {
        (1..20).map { i ->
            AiCreationPreview(
                id = i.toString(),
                username = "用户$i",
                userSignature = "签名 $i",
                aiRoleName = sampleRoles[i % sampleRoles.size],
                aiModelName = "模型 ${(i % 3) + 1}",
                createdAt = now - i * 60_000L,
                // random heights between 160..320 dp
                placeholderHeightDp = (160 + (i * 13 % 160))
            )
        }
    }

    var selectedRole by remember { mutableStateOf("全部") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 创作") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Outlined.Menu, contentDescription = "打开侧边栏")
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
            // Role filter row
            RoleFilterRow(
                roles = listOf("全部") + sampleRoles,
                selected = selectedRole,
                onSelect = { selectedRole = it }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Grid of cards implemented as two LazyColumn side-by-side
            val itemsToShow = remember(selectedRole) {
                if (selectedRole == "全部") sampleItems
                else sampleItems.filter { it.aiRoleName == selectedRole }
            }

            if (itemsToShow.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无已收藏的AI创作", color = MaterialTheme.colorScheme.onSurface)
                }
            } else {
                // split into two lists for left/right columns by index parity to simulate waterfall
                val left = itemsToShow.filterIndexed { index, _ -> index % 2 == 0 }
                val right = itemsToShow.filterIndexed { index, _ -> index % 2 == 1 }

                Row(modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(left, key = { it.id }) { item ->
                            ImageCard(item = item) {
                                Toast.makeText(context, "打开 ${item.username} 的创作", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(right, key = { it.id }) { item ->
                            ImageCard(item = item) {
                                Toast.makeText(context, "打开 ${item.username} 的创作", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleFilterRow(roles: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        roles.forEach { role ->
            val isSelected = role == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(role) },
                label = { Text(role, fontSize = 13.sp) },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun ImageCard(item: AiCreationPreview, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Placeholder for image - gradient color block with variable height
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(item.placeholderHeightDp.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(colorFromId(item.id.hashCode()), colorFromId(item.id.hashCode() + 31))
                        )
                    )
            )

            // Overlay metadata at bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
                    .padding(8.dp)
            ) {
                Text(text = item.username, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = item.aiRoleName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = formatTimeAgo(item.createdAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun colorFromId(seed: Int): Color {
    // deterministic pseudo-random pastel color from int
    val r = ((seed shr 16) and 0xFF).coerceIn(60, 220)
    val g = ((seed shr 8) and 0xFF).coerceIn(60, 220)
    val b = (seed and 0xFF).coerceIn(60, 220)
    return Color(r, g, b)
}

private fun formatTimeAgo(epochMillis: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - epochMillis
    val minutes = diff / 60_000
    return when {
        minutes < 1 -> "刚刚"
        minutes < 60 -> "${minutes}分钟前"
        minutes < 60 * 24 -> "${minutes / 60}小时前"
        else -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(epochMillis))
    }
}

private data class AiCreationPreview(
    val id: String,
    val username: String,
    val userSignature: String = "",
    val aiRoleName: String = "默认",
    val aiModelName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val placeholderHeightDp: Int = 200
)
