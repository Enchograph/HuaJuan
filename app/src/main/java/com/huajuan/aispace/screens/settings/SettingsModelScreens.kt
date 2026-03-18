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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.OpenInNew
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.huajuan.aispace.R
import com.huajuan.aispace.components.SearchField
import com.huajuan.aispace.data.ModelCapability
import com.huajuan.aispace.data.ModelDataProvider
import com.huajuan.aispace.data.ProviderType
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

@Composable
internal fun ModelHubScreen(
    modifier: Modifier,
    repository: Repository,
    onProviderClick: (String) -> Unit,
    onAddProvider: () -> Unit,
    onModelDownload: () -> Unit
) {
    var useCloud by remember { mutableStateOf(true) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = useCloud,
                    onClick = { useCloud = true },
                    shape = MaterialTheme.shapes.small,
                    label = { Text(stringResource(R.string.settings_cloud_models)) }
                )
                SegmentedButton(
                    selected = !useCloud,
                    onClick = { useCloud = false },
                    shape = MaterialTheme.shapes.small,
                    label = { Text(stringResource(R.string.settings_local_models)) }
                )
            }
        Box(modifier = Modifier.weight(1f)) {
            if (useCloud) {
                CloudModelsScreen(
                    modifier = Modifier.fillMaxSize(),
                    repository = repository,
                    onProviderClick = onProviderClick,
                    onAddProvider = onAddProvider
                )
            } else {
                LocalModelsScreen(
                    modifier = Modifier.fillMaxSize(),
                    repository = repository,
                    onDownloadClick = onModelDownload
                )
            }
        }
    }
}

@Composable
internal fun CloudModelsScreen(
    modifier: Modifier,
    repository: Repository,
    onProviderClick: (String) -> Unit,
    onAddProvider: () -> Unit
) {
    var filter by remember { mutableStateOf("official") }
    var starredProviders by remember { mutableStateOf(repository.getStarredServiceProviders()) }
    val modelDataProvider = remember(repository) { ModelDataProvider(repository) }
    val providers = modelDataProvider.getAllServiceProviders()
    val customProviders = repository.getCustomServiceProviders()
    val filtered = providers.filter { provider ->
        when (filter) {
            "starred" -> provider in starredProviders
            "official" -> ModelDataProvider.predefinedServiceProviders.containsKey(provider)
            "custom" -> customProviders.contains(provider)
            else -> true
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf(
                    "starred" to stringResource(R.string.assistant_section_starred),
                    "official" to stringResource(R.string.settings_builtin_providers),
                    "custom" to stringResource(R.string.assistant_section_user)
                )
                options.forEach { (value, label) ->
                    SegmentedButton(
                        selected = filter == value,
                        onClick = { filter = value },
                        shape = MaterialTheme.shapes.small,
                        label = { Text(label, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }
        }
        item {
            if (filtered.isEmpty()) {
                val emptyText = when (filter) {
                    "starred" -> stringResource(R.string.assistant_empty_starred)
                    "custom" -> stringResource(R.string.assistant_empty_user)
                    else -> stringResource(R.string.settings_empty_builtin_providers)
                }
                Text(
                    text = emptyText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                filtered.forEachIndexed { index, provider ->
                    val isStarred = provider in starredProviders
                    SettingsListItem(
                        title = provider,
                        subtitle = if (ModelDataProvider.predefinedServiceProviders.containsKey(provider)) stringResource(R.string.settings_builtin_label) else stringResource(R.string.settings_user_added_label),
                        leading = { Icon(Icons.Outlined.Cloud, contentDescription = null) },
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        val updated = if (isStarred) {
                                            starredProviders - provider
                                        } else {
                                            starredProviders + provider
                                        }
                                        starredProviders = updated
                                        repository.setStarredServiceProviders(updated)
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isStarred) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                                        contentDescription = if (isStarred) stringResource(R.string.assistant_unstar) else stringResource(R.string.assistant_star),
                                        tint = if (isStarred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Outlined.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = { onProviderClick(provider) }
                    )
                    if (index != filtered.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
        if (filter == "custom") {
            item {
                Button(
                    onClick = onAddProvider,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(stringResource(R.string.settings_new_provider_title))
                }
            }
        }
    }
}

@Composable
internal fun VendorModelsScreen(
    modifier: Modifier,
    repository: Repository,
    provider: String,
    onAddModel: () -> Unit
) {
    val modelDataProvider = remember(repository) { ModelDataProvider(repository) }
    var models by remember(provider) { mutableStateOf(modelDataProvider.getModelListForProvider(provider)) }
    var apiUrl by remember(provider) { mutableStateOf(modelDataProvider.getApiUrlForProvider(provider)) }
    var apiKey by remember(provider) { mutableStateOf(repository.getApiKeyForProvider(provider)) }
    var providerEnabled by remember(provider) { mutableStateOf(repository.getServiceProviderEnabled(provider)) }
    val isPredefinedProvider = ModelDataProvider.predefinedServiceProviders.containsKey(provider)
    val consoleUrl = remember(provider) {
        ModelDataProvider.predefinedServiceProviders[provider]?.consoleUrl
    }
    val uriHandler = LocalUriHandler.current
    var showCapabilityDialog by remember { mutableStateOf(false) }
    var targetModel by remember { mutableStateOf<com.huajuan.aispace.data.ModelInfo?>(null) }
    var selectedCapabilities by remember { mutableStateOf(emptySet<ModelCapability>()) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            SettingsSwitchItem(
                title = stringResource(R.string.settings_enable_provider),
                subtitle = if (providerEnabled) stringResource(R.string.state_enabled) else stringResource(R.string.settings_disabled_provider),
                leading = { Icon(Icons.Outlined.Cloud, contentDescription = null) },
                checked = providerEnabled,
                onCheckedChange = {
                    providerEnabled = it
                    repository.setServiceProviderEnabled(provider, it)
                }
            )
        }
        item {
            OutlinedTextField(
                value = apiUrl,
                onValueChange = {
                    apiUrl = it
                    if (!isPredefinedProvider) {
                        repository.addCustomServiceProvider(
                            provider,
                            it,
                            repository.getCustomProviderType(provider)
                        )
                    }
                },
                label = { Text(stringResource(R.string.settings_api_base_url)) },
                modifier = Modifier.fillMaxWidth(),
                readOnly = isPredefinedProvider,
                enabled = !isPredefinedProvider
            )
        }
        item {
            PasswordTextField(
                value = apiKey,
                onValueChange = {
                    apiKey = it
                    repository.setApiKeyForProvider(provider, it)
                },
                label = stringResource(R.string.model_requirement_missing_api_key_title),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (!consoleUrl.isNullOrBlank()) {
            item {
                SettingsListItem(
                    title = stringResource(R.string.settings_provider_console),
                    subtitle = stringResource(R.string.settings_provider_console_subtitle, provider),
                    leading = { Icon(Icons.Outlined.OpenInNew, contentDescription = null) },
                    onClick = { uriHandler.openUri(consoleUrl) }
                )
            }
        }
        item {
            models.forEachIndexed { index, model ->
                SettingsListItem(
                    title = model.displayName,
                    subtitle = model.apiCode,
                    trailing = {
                        val isPredefinedModel = isPredefinedProvider &&
                            ModelDataProvider.predefinedServiceProviders[provider]?.models
                                ?.any { it.apiCode == model.apiCode } == true
                        val caps = model.capabilities.ifEmpty { setOf(ModelCapability.Chat) }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (caps.contains(ModelCapability.Chat)) {
                                Icon(
                                    imageVector = Icons.Outlined.Chat,
                                    contentDescription = stringResource(R.string.settings_model_capability_chat),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                            if (caps.contains(ModelCapability.ImageGeneration)) {
                                Icon(
                                    imageVector = Icons.Outlined.Image,
                                    contentDescription = stringResource(R.string.settings_model_capability_image),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                            if (caps.contains(ModelCapability.Embedding)) {
                                Icon(
                                    imageVector = Icons.Outlined.DataObject,
                                    contentDescription = stringResource(R.string.settings_model_capability_embedding),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                            if (!isPredefinedModel) {
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(18.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant)
                                )
                                IconButton(
                                    onClick = {
                                        targetModel = model
                                        selectedCapabilities = caps
                                        showCapabilityDialog = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Tune,
                                        contentDescription = stringResource(R.string.settings_edit_model_capabilities),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                )
                if (index != models.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
        item {
            Button(
                onClick = onAddModel,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(stringResource(R.string.settings_add_model))
            }
        }
    }

    if (showCapabilityDialog) {
        val model = targetModel
        AlertDialog(
            onDismissRequest = { showCapabilityDialog = false },
            title = { Text(text = stringResource(R.string.settings_model_capability_settings)) },
            text = {
                Column {
                    Text(
                        text = model?.displayName ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    listOf(
                        ModelCapability.Chat to stringResource(R.string.settings_model_capability_chat),
                        ModelCapability.ImageGeneration to stringResource(R.string.settings_model_capability_image),
                        ModelCapability.Embedding to stringResource(R.string.settings_model_capability_embedding)
                    ).forEach { (capability, label) ->
                        val checked = selectedCapabilities.contains(capability)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val updated = if (checked) {
                                        (selectedCapabilities - capability).ifEmpty { setOf(ModelCapability.Chat) }
                                    } else {
                                        selectedCapabilities + capability
                                    }
                                    selectedCapabilities = updated
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    val updated = if (isChecked) {
                                        selectedCapabilities + capability
                                    } else {
                                        (selectedCapabilities - capability).ifEmpty { setOf(ModelCapability.Chat) }
                                    }
                                    selectedCapabilities = updated
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        model?.let {
                            repository.addCustomModelToProvider(
                                providerName = provider,
                                modelName = it.displayName,
                                apiCode = it.apiCode,
                                capabilities = selectedCapabilities
                            )
                            models = modelDataProvider.getModelListForProvider(provider)
                        }
                        showCapabilityDialog = false
                    }
                ) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showCapabilityDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
internal fun NewModelFormScreen(
    modifier: Modifier,
    provider: String,
    repository: Repository,
    onSave: (String, String, String, Float, Float, Set<ModelCapability>) -> Unit
) {
    var modelName by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("") }
    var temperature by remember { mutableStateOf(0.7f) }
    var topP by remember { mutableStateOf(0.9f) }
    var selectedCapabilities by remember { mutableStateOf(setOf(ModelCapability.Chat)) }
    val modelDataProvider = remember(repository) { ModelDataProvider(repository) }
    val providerUrl = remember(provider) { modelDataProvider.getApiUrlForProvider(provider) }
    val isPredefined = ModelDataProvider.predefinedServiceProviders.containsKey(provider)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            SettingsSectionCard(title = stringResource(R.string.settings_basic_info)) {
                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text(stringResource(R.string.settings_model_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = if (baseUrl.isBlank()) providerUrl else baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text(stringResource(R.string.settings_api_base_url)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isPredefined
                )
                Spacer(modifier = Modifier.height(12.dp))
                PasswordTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = "API Key",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.settings_model_capabilities),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                listOf(
                    ModelCapability.Chat to stringResource(R.string.settings_model_capability_chat),
                    ModelCapability.ImageGeneration to stringResource(R.string.settings_model_capability_image),
                    ModelCapability.Embedding to stringResource(R.string.settings_model_capability_embedding)
                ).forEach { (capability, label) ->
                    val checked = selectedCapabilities.contains(capability)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val updated = if (checked) {
                                    selectedCapabilities - capability
                                } else {
                                    selectedCapabilities + capability
                                }
                                selectedCapabilities = updated
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { isChecked ->
                                val updated = if (isChecked) {
                                    selectedCapabilities + capability
                                } else {
                                    selectedCapabilities - capability
                                }
                                selectedCapabilities = updated
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = label, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        item {
            SettingsSectionCard(title = stringResource(R.string.settings_parameter_config)) {
                Text(stringResource(R.string.settings_temperature_value, String.format(Locale.getDefault(), "%.2f", temperature)))
                Slider(
                    value = temperature,
                    onValueChange = { temperature = it },
                    valueRange = 0.0f..1.2f
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.settings_top_p_value, String.format(Locale.getDefault(), "%.2f", topP)))
                Slider(
                    value = topP,
                    onValueChange = { topP = it },
                    valueRange = 0.1f..1.0f
                )
            }
        }
        item {
            Button(
                onClick = {
                    onSave(
                        modelName,
                        apiKey,
                        if (isPredefined) providerUrl else baseUrl,
                        temperature,
                        topP,
                        selectedCapabilities
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = modelName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}

@Composable
private fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        visualTransformation = if (passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        trailingIcon = {
            val icon = if (passwordVisible) {
                Icons.Filled.Visibility
            } else {
                Icons.Filled.VisibilityOff
            }
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    imageVector = icon,
                    contentDescription = if (passwordVisible) stringResource(R.string.settings_hide_key) else stringResource(R.string.settings_show_key)
                )
            }
        }
    )
}

@Composable
internal fun NewProviderFormScreen(
    modifier: Modifier,
    onSave: (String, String, ProviderType) -> Unit
) {
    var providerName by remember { mutableStateOf("") }
    var apiUrl by remember { mutableStateOf("") }
    var providerType by remember { mutableStateOf(ProviderType.OpenAI) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            OutlinedTextField(
                value = providerName,
                onValueChange = { providerName = it },
                label = { Text(stringResource(R.string.settings_provider_name)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            OutlinedTextField(
                value = apiUrl,
                onValueChange = { apiUrl = it },
                label = { Text(stringResource(R.string.settings_api_base_url)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            ProviderTypeDropdownField(
                providerType = providerType,
                onProviderTypeChange = { providerType = it },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Button(
                onClick = { onSave(providerName, apiUrl, providerType) },
                modifier = Modifier.fillMaxWidth(),
                enabled = providerName.isNotBlank() && apiUrl.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderTypeDropdownField(
    providerType: ProviderType,
    onProviderTypeChange: (ProviderType) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        ProviderType.OpenAI to "OpenAI",
        ProviderType.Anthropic to "Anthropic",
        ProviderType.Gemini to "Gemini"
    )
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == providerType }?.second ?: providerType.name

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.settings_protocol_type)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (type, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onProviderTypeChange(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
internal fun LocalModelsScreen(
    modifier: Modifier,
    repository: Repository,
    onDownloadClick: () -> Unit
) {
    val localModels = remember { repository.getLocalModelList() }
    val selectedModel = remember { mutableStateOf(repository.getLocalSelectedModel()) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        item {
            if (localModels.isEmpty()) {
                SettingsSectionCard {
                    Text(stringResource(R.string.settings_local_model_not_built_in))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.settings_local_model_wait_import_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                localModels.forEachIndexed { index, model ->
                    SettingsListItem(
                        title = model.displayName,
                        subtitle = stringResource(R.string.settings_imported_model, model.apiCode),
                        leading = {
                            RadioButton(
                                selected = selectedModel.value == model.displayName,
                                onClick = {
                                    selectedModel.value = model.displayName
                                    repository.setLocalSelectedModel(model.displayName)
                                }
                            )
                        }
                    )
                    if (index != localModels.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
        item {
            SettingsListItem(
                title = stringResource(R.string.settings_model_download_title),
                subtitle = stringResource(R.string.settings_model_download_subtitle),
                leading = { Icon(Icons.Outlined.Download, contentDescription = null) },
                onClick = onDownloadClick
            )
        }
    }
}
