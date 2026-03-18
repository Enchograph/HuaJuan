package com.huajuan.aispace.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.huajuan.aispace.R
import com.huajuan.aispace.components.SearchField
import com.huajuan.aispace.data.ModelDataProvider
import com.huajuan.aispace.data.Repository
import com.huajuan.aispace.data.repository.SettingsRepository
import com.huajuan.aispace.screens.settings.sections.DebugSettingSection
import com.huajuan.aispace.screens.settings.sections.ModelPickerScreen
import com.huajuan.aispace.components.settings.SettingsListItem
import com.huajuan.aispace.components.settings.SettingsOption
import com.huajuan.aispace.components.settings.SettingsOptionDialog
import com.huajuan.aispace.components.settings.SettingsSectionCard
import com.huajuan.aispace.components.settings.SettingsSwitchItem
import com.huajuan.aispace.navigation.DrawerUiResetEffect
import com.huajuan.aispace.screens.assistant.AssistantSettingsScreen
import com.huajuan.aispace.viewmodel.SettingsUiState
import com.huajuan.aispace.viewmodel.SettingsViewModel
import com.huajuan.aispace.i18n.AppLocaleManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun SettingsHomeScreen(
    modifier: Modifier,
    drawerUiResetRequest: Int = 0,
    onNavigate: (SettingsSubRoute) -> Unit
) {
    var query by remember { mutableStateOf("") }
    DrawerUiResetEffect(drawerUiResetRequest) {
        query = ""
    }
    val entries = listOf(
        HomeEntry(
            stringResource(R.string.settings_common_title),
            stringResource(R.string.settings_home_common_subtitle),
            SettingsSubRoute.Common,
            listOf("common", "language", "notification", "theme", "font", "transparent")
        ),
        HomeEntry(
            stringResource(R.string.settings_model_management_title),
            stringResource(R.string.settings_home_model_subtitle),
            SettingsSubRoute.ModelManagement,
            listOf("provider", "model", "download")
        ),
        HomeEntry(
            stringResource(R.string.settings_tools_title),
            stringResource(R.string.settings_home_tools_subtitle),
            SettingsSubRoute.ToolSettings,
            listOf("tool", "translate", "document", "search", "mcp")
        ),
        HomeEntry(
            stringResource(R.string.settings_global_memory_title),
            stringResource(R.string.settings_home_memory_subtitle),
            SettingsSubRoute.GlobalMemory,
            listOf("memory")
        ),
        HomeEntry(
            stringResource(R.string.settings_about_title),
            stringResource(R.string.settings_home_about_subtitle),
            SettingsSubRoute.About,
            listOf("version", "license")
        ),
        HomeEntry(
            stringResource(R.string.settings_more_title),
            stringResource(R.string.settings_home_more_subtitle),
            SettingsSubRoute.Advanced,
            listOf("advanced", "debug", "diagnostic")
        )
    )
    val filteredEntries = remember(query) {
        if (query.isBlank()) {
            entries
        } else {
            entries.filter { entry ->
                entry.title.contains(query, ignoreCase = true) ||
                    entry.subtitle.contains(query, ignoreCase = true) ||
                    entry.keywords.any { it.contains(query, ignoreCase = true) }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            SearchField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.settings_search)
            )
        }
        item {
            SettingsSectionCard {
                filteredEntries.forEachIndexed { index, entry ->
                    val icon = when (entry.route) {
                        SettingsSubRoute.Common -> Icons.Outlined.Settings
                        SettingsSubRoute.ToolSettings -> Icons.Outlined.Extension
                        SettingsSubRoute.Advanced -> Icons.Outlined.Bolt
                        SettingsSubRoute.ModelManagement -> Icons.Outlined.Tune
                        SettingsSubRoute.Translation -> Icons.Outlined.Translate
                        SettingsSubRoute.DocumentProcessing -> Icons.Outlined.Description
                        SettingsSubRoute.WebSearch -> Icons.Outlined.Search
                        SettingsSubRoute.McpTools -> Icons.Outlined.Extension
                        SettingsSubRoute.GlobalMemory -> Icons.Outlined.Memory
                        SettingsSubRoute.About -> Icons.Outlined.Info
                        SettingsSubRoute.Debug -> Icons.Outlined.Widgets
                        else -> Icons.Outlined.Settings
                    }
                    SettingsListItem(
                        title = entry.title,
                        subtitle = entry.subtitle,
                        leading = { Icon(icon, contentDescription = null) },
                        onClick = { onNavigate(entry.route) }
                    )
                    if (index != filteredEntries.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
internal fun AdvancedSettingsHomeScreen(
    modifier: Modifier,
    onNavigate: (SettingsSubRoute) -> Unit
) {
    val entries = listOf(
        HomeEntry(
            stringResource(R.string.settings_debug_title),
            stringResource(R.string.settings_debug_subtitle),
            SettingsSubRoute.Debug,
            listOf("debug", "diagnostic")
        )
    )
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            SettingsSectionCard {
                entries.forEachIndexed { index, entry ->
                    val icon = when (entry.route) {
                        SettingsSubRoute.Debug -> Icons.Outlined.Widgets
                        else -> Icons.Outlined.Settings
                    }
                    SettingsListItem(
                        title = entry.title,
                        subtitle = entry.subtitle,
                        leading = { Icon(icon, contentDescription = null) },
                        onClick = { onNavigate(entry.route) }
                    )
                    if (index != entries.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
internal fun CommonSettingsScreen(
    modifier: Modifier,
    uiState: SettingsUiState,
    onNotificationToggle: (Boolean) -> Unit,
    onLanguageChange: (String) -> Unit,
    onThemeModeChange: (String) -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onTransparencyChange: (String) -> Unit
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    val languageOptions = listOf(
        SettingsOption(AppLocaleManager.SYSTEM, stringResource(R.string.language_system)),
        SettingsOption(AppLocaleManager.ZH_HANS, stringResource(R.string.language_zh_hans)),
        SettingsOption(AppLocaleManager.EN, "English"),
        SettingsOption(AppLocaleManager.JA, stringResource(R.string.language_ja))
    )
    val languageLabel = languageOptions.firstOrNull { it.value == uiState.language }?.label
        ?: stringResource(R.string.language_system)

    if (showLanguageDialog) {
        SettingsOptionDialog(
            title = stringResource(R.string.settings_language_choose),
            options = languageOptions,
            selectedValue = uiState.language,
            onSelect = { onLanguageChange(it) },
            onDismiss = { showLanguageDialog = false }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            SettingsSectionCard(title = stringResource(R.string.settings_basic_title)) {
                SettingsListItem(
                    title = stringResource(R.string.settings_language_title),
                    subtitle = stringResource(R.string.settings_current_language, languageLabel),
                    leading = { Icon(Icons.Outlined.Language, contentDescription = null) },
                    onClick = { showLanguageDialog = true }
                )
                HorizontalDivider()
                SettingsSwitchItem(
                    title = stringResource(R.string.settings_notifications_title),
                    subtitle = if (uiState.notificationsEnabled) {
                        stringResource(R.string.state_enabled)
                    } else {
                        stringResource(R.string.state_disabled)
                    },
                    leading = { Icon(Icons.Outlined.Notifications, contentDescription = null) },
                    checked = uiState.notificationsEnabled,
                    onCheckedChange = onNotificationToggle
                )
            }
        }
        item {
            SettingsSectionCard(title = stringResource(R.string.settings_theme_mode_title)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val options = listOf(
                        "light" to stringResource(R.string.theme_light),
                        "dark" to stringResource(R.string.theme_dark),
                        "system" to stringResource(R.string.theme_system)
                    )
                    options.forEach { (value, label) ->
                        SegmentedButton(
                            selected = uiState.themeMode == value,
                            onClick = { onThemeModeChange(value) },
                            shape = MaterialTheme.shapes.small,
                            label = { Text(label) }
                        )
                    }
                }
            }
        }
        item {
            SettingsSectionCard(title = stringResource(R.string.settings_font_size_title)) {
                Text(
                    text = stringResource(R.string.settings_font_preview),
                    fontSize = (14.sp * uiState.fontScale),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = uiState.fontScale,
                    onValueChange = onFontScaleChange,
                    valueRange = 0.85f..1.2f
                )
            }
        }
        item {
            SettingsSectionCard(title = stringResource(R.string.settings_transparency_title)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val options = listOf(
                        "low" to stringResource(R.string.level_low),
                        "medium" to stringResource(R.string.level_medium),
                        "high" to stringResource(R.string.level_high)
                    )
                    options.forEach { (value, label) ->
                        SegmentedButton(
                            selected = uiState.transparencyLevel == value,
                            onClick = { onTransparencyChange(value) },
                            shape = MaterialTheme.shapes.small,
                            label = { Text(label) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ToolSettingsHomeScreen(
    modifier: Modifier,
    onNavigate: (SettingsSubRoute) -> Unit
) {
    val entries = listOf(
        HomeEntry(
            stringResource(R.string.settings_translation_title),
            stringResource(R.string.settings_tools_translation_subtitle),
            SettingsSubRoute.Translation,
            listOf("translation")
        ),
        HomeEntry(
            stringResource(R.string.settings_document_processing_title),
            stringResource(R.string.settings_tools_document_subtitle),
            SettingsSubRoute.DocumentProcessing,
            listOf("document", "ocr")
        ),
        HomeEntry(
            stringResource(R.string.settings_knowledge_base_title),
            stringResource(R.string.settings_tools_knowledge_subtitle),
            SettingsSubRoute.KnowledgeBase,
            listOf("knowledge", "embedding", "chunk", "retrieval")
        ),
        HomeEntry(
            stringResource(R.string.settings_web_search_title),
            stringResource(R.string.settings_tools_search_subtitle),
            SettingsSubRoute.WebSearch,
            listOf("search", "api", "blacklist", "date")
        ),
        HomeEntry(
            stringResource(R.string.settings_mcp_title),
            stringResource(R.string.settings_tools_mcp_subtitle),
            SettingsSubRoute.McpTools,
            listOf("mcp")
        )
    )
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            SettingsSectionCard {
                entries.forEachIndexed { index, entry ->
                    val icon = when (entry.route) {
                        SettingsSubRoute.Translation -> Icons.Outlined.Translate
                        SettingsSubRoute.DocumentProcessing -> Icons.Outlined.Description
                        SettingsSubRoute.KnowledgeBase -> Icons.Outlined.Storage
                        SettingsSubRoute.WebSearch -> Icons.Outlined.Search
                        SettingsSubRoute.McpTools -> Icons.Outlined.Extension
                        else -> Icons.Outlined.Settings
                    }
                    SettingsListItem(
                        title = entry.title,
                        subtitle = entry.subtitle,
                        leading = { Icon(icon, contentDescription = null) },
                        onClick = { onNavigate(entry.route) }
                    )
                    if (index != entries.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
