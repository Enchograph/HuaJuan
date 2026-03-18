
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
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.huajuan.aispace.screens.assistant.AssistantSettingsScreen
import com.huajuan.aispace.viewmodel.SettingsUiState
import com.huajuan.aispace.viewmodel.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSubScreen(
    repository: Repository,
    darkModeState: MutableState<Boolean>,
    route: SettingsSubRoute,
    onBack: () -> Unit,
    onNavigate: (SettingsSubRoute) -> Unit,
    onSwitchAssistant: (String) -> Unit
) {
    val vm: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(SettingsRepository(repository))
    )
    val uiState by vm.uiState.collectAsState()
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(route.resolveTitle(context)) },
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
        when (route) {
            SettingsSubRoute.Common -> CommonSettingsScreen(
                modifier = Modifier.padding(paddingValues),
                uiState = uiState,
                onNotificationToggle = vm::setNotificationsEnabled,
                onLanguageChange = vm::setLanguage,
                onThemeModeChange = { mode ->
                    val resolvedDark = when (mode) {
                        "dark" -> true
                        "light" -> false
                        else -> systemDark
                    }
                    darkModeState.value = resolvedDark
                    vm.setThemeMode(mode, resolvedDark)
                },
                onFontScaleChange = vm::setFontScale,
                onTransparencyChange = vm::setTransparencyLevel
            )
            SettingsSubRoute.ToolSettings -> ToolSettingsHomeScreen(
                modifier = Modifier.padding(paddingValues),
                onNavigate = onNavigate
            )
            SettingsSubRoute.Advanced -> AdvancedSettingsHomeScreen(
                modifier = Modifier.padding(paddingValues),
                onNavigate = onNavigate
            )
            SettingsSubRoute.ModelManagement -> ModelHubScreen(
                modifier = Modifier.padding(paddingValues),
                repository = repository,
                onProviderClick = { provider ->
                    vm.setServiceProvider(provider)
                    onNavigate(SettingsSubRoute.VendorModels(provider))
                },
                onAddProvider = { onNavigate(SettingsSubRoute.NewProviderForm) },
                onModelDownload = { onNavigate(SettingsSubRoute.ModelDownload) }
            )
            is SettingsSubRoute.VendorModels -> VendorModelsScreen(
                modifier = Modifier.padding(paddingValues),
                repository = repository,
                provider = route.provider,
                onAddModel = { onNavigate(SettingsSubRoute.NewModelForm(route.provider)) }
            )
            is SettingsSubRoute.NewModelForm -> NewModelFormScreen(
                modifier = Modifier.padding(paddingValues),
                provider = route.provider,
                repository = repository,
                onSave = { name, apiKey, baseUrl, temperature, topP, capabilities ->
                    if (name.isNotBlank()) {
                        vm.addCustomModelToProvider(route.provider, name, name, capabilities)
                        if (apiKey.isNotBlank()) {
                            vm.setApiKeyForProvider(route.provider, apiKey)
                        }
                        if (baseUrl.isNotBlank()) {
                            vm.addCustomServiceProvider(
                                route.provider,
                                baseUrl,
                                vm.getCustomProviderType(route.provider)
                            )
                        }
                        vm.setModelTemperature(route.provider, name, temperature)
                        vm.setModelTopP(route.provider, name, topP)
                        onBack()
                    }
                }
            )
            SettingsSubRoute.NewProviderForm -> NewProviderFormScreen(
                modifier = Modifier.padding(paddingValues),
                onSave = { name, apiUrl, providerType ->
                    if (name.isNotBlank() && apiUrl.isNotBlank()) {
                        vm.addCustomServiceProvider(name, apiUrl, providerType)
                        onBack()
                    }
                }
            )
            SettingsSubRoute.Translation -> TranslationSettingsScreen(
                modifier = Modifier.padding(paddingValues),
                repository = repository
            )
            SettingsSubRoute.DocumentProcessing -> DocumentProcessingScreen(
                modifier = Modifier.padding(paddingValues),
                uiState = uiState,
                onAutoOcrChange = vm::setDocumentAutoOcrEnabled,
                onPreferEpubChange = vm::setDocumentPreferEpub,
                onRenderQualityChange = vm::setDocumentRenderQuality,
                onRemoveBackgroundChange = vm::setDocumentRemoveBackground
            )
            SettingsSubRoute.KnowledgeBase -> KnowledgeBaseSettingsScreen(
                modifier = Modifier.padding(paddingValues),
                onNavigate = onNavigate
            )
            SettingsSubRoute.KnowledgeBaseBasic -> KnowledgeBaseBasicSettingsScreen(
                modifier = Modifier.padding(paddingValues),
                uiState = uiState,
                onUpdateSettings = vm::updateKnowledgeSettings
            )
            SettingsSubRoute.KnowledgeBaseIndexing -> KnowledgeBaseIndexingSettingsScreen(
                modifier = Modifier.padding(paddingValues),
                uiState = uiState,
                onUpdateSettings = vm::updateKnowledgeSettings
            )
            SettingsSubRoute.KnowledgeBaseRetrieval -> KnowledgeBaseRetrievalSettingsScreen(
                modifier = Modifier.padding(paddingValues),
                uiState = uiState,
                onUpdateSettings = vm::updateKnowledgeSettings
            )
            SettingsSubRoute.KnowledgeBaseAdvanced -> KnowledgeBaseAdvancedSettingsScreen(
                modifier = Modifier.padding(paddingValues),
                uiState = uiState,
                onUpdateSettings = vm::updateKnowledgeSettings
            )
            SettingsSubRoute.KnowledgeBaseTaskCenter -> KnowledgeBaseTaskCenterScreen(
                modifier = Modifier.padding(paddingValues),
                repository = repository
            )
            SettingsSubRoute.WebSearch -> WebSearchScreen(
                modifier = Modifier.padding(paddingValues),
                uiState = uiState,
                onSearchEngineChange = vm::setSearchEngine,
                onSearchResultCountChange = vm::setSearchResultCount,
                onSearchIncludeDateChange = vm::setSearchIncludeDate,
                onSearchCompressionChange = vm::setSearchCompression,
                onSearchBlacklistChange = vm::setSearchBlacklist,
                onSearchProviderApiKeyChange = vm::setSearchProviderApiKey
            )
            SettingsSubRoute.McpTools -> McpToolsScreen(
                modifier = Modifier.padding(paddingValues),
                uiState = uiState,
                onToolsEnabledChange = vm::setMcpToolsEnabled
            )
            SettingsSubRoute.GlobalMemory -> GlobalMemoryScreen(
                modifier = Modifier.padding(paddingValues),
                uiState = uiState,
                onEnabledChange = vm::setGlobalMemoryEnabled,
                onRetentionDaysChange = vm::setGlobalMemoryRetentionDays,
                onModeChange = vm::setGlobalMemoryMode,
                onClearMemory = {
                    vm.clearGlobalMemory()
                    Toast.makeText(context, context.getString(R.string.memory_cleared), Toast.LENGTH_SHORT).show()
                }
            )
            SettingsSubRoute.AssistantSettings -> AssistantSettingsScreen(
                repository = repository,
                assistantId = repository.getCurrentAssistantId(),
                modifier = Modifier.padding(paddingValues),
                onSwitchToAssistant = {
                    onSwitchAssistant(repository.getCurrentAssistantId())
                },
                onOpenMcpSettings = { onNavigate(SettingsSubRoute.McpTools) },
                onOpenGlobalMemory = { onNavigate(SettingsSubRoute.GlobalMemory) }
            )
            SettingsSubRoute.About -> AboutScreen(
                modifier = Modifier.padding(paddingValues)
            )
            SettingsSubRoute.Debug -> DebugSettingsScreen(
                modifier = Modifier.padding(paddingValues),
                repository = repository,
                debugMode = uiState.debugMode,
                tokenAnimationFixedDurationEnabled = uiState.tokenAnimationFixedDurationEnabled,
                tokenAnimationFixedDurationMs = uiState.tokenAnimationFixedDurationMs,
                scope = scope,
                onDebugModeChange = vm::setDebugMode,
                onTokenAnimationFixedDurationEnabledChange = vm::setTokenAnimationFixedDurationEnabled,
                onTokenAnimationFixedDurationMsChange = vm::setTokenAnimationFixedDurationMs
            )
            SettingsSubRoute.ModelDownload -> ModelDownloadScreen(
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

internal data class HomeEntry(
    val title: String,
    val subtitle: String,
    val route: SettingsSubRoute,
    val keywords: List<String> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    onMenuClick: () -> Unit = {},
    drawerUiResetRequest: Int = 0,
    onOpenSubPage: (SettingsSubRoute) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
        SettingsHomeScreen(
            modifier = Modifier.padding(paddingValues),
            drawerUiResetRequest = drawerUiResetRequest,
            onNavigate = onOpenSubPage
        )
    }
}
