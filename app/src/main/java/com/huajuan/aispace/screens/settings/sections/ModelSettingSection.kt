package com.huajuan.aispace.screens.settings.sections

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huajuan.aispace.R
import com.huajuan.aispace.data.BuiltInServiceProviders
import com.huajuan.aispace.data.Message
import com.huajuan.aispace.data.ModelCapability
import com.huajuan.aispace.data.ModelDataProvider
import com.huajuan.aispace.data.ProviderType
import com.huajuan.aispace.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ModelSettingSection(
    useCloudModel: Boolean,
    onUseCloudModelChange: (Boolean) -> Unit,
    serviceProvider: String,
    onServiceProviderChange: (String) -> Unit,
    customApiUrl: String,
    onCustomApiUrlChange: (String) -> Unit,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    selectedModel: String,
    onSelectedModelChange: (String) -> Unit,
    repository: Repository,
    scope: CoroutineScope
) {
    val context = LocalContext.current
    var localSelectedModel by remember { mutableStateOf(repository.getLocalSelectedModel()) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.assistant_model_settings),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.padding(end = 16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_use_cloud_model),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (useCloudModel) stringResource(R.string.state_enabled) else stringResource(R.string.settings_use_local_model),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = useCloudModel,
                    onCheckedChange = onUseCloudModelChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }

            if (useCloudModel) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ServiceProviderSelector(
                        serviceProvider = serviceProvider,
                        onServiceProviderChange = onServiceProviderChange,
                        customApiUrl = customApiUrl,
                        onCustomApiUrlChange = onCustomApiUrlChange,
                        repository = repository
                    )

                    var passwordVisibility by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = onApiKeyChange,
                        label = { Text(stringResource(R.string.model_requirement_missing_api_key_title)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        visualTransformation = if (passwordVisibility) {
                            androidx.compose.ui.text.input.VisualTransformation.None
                        } else {
                            androidx.compose.ui.text.input.PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        trailingIcon = {
                            val image = if (passwordVisibility) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passwordVisibility = !passwordVisibility }) {
                                Icon(
                                    imageVector = image,
                                    contentDescription = if (passwordVisibility) stringResource(R.string.settings_hide_password) else stringResource(R.string.settings_show_password)
                                )
                            }
                        }
                    )

                    ModelSelector(
                        serviceProvider = serviceProvider,
                        selectedModel = selectedModel,
                        onModelChange = onSelectedModelChange,
                        repository = repository
                    )
                }

                Button(
                    onClick = {
                        scope.launch {
                            val testMessage = Message(
                                id = "test-message",
                                text = context.getString(R.string.settings_test_connection_prompt),
                                isUser = true,
                                timestamp = java.util.Date()
                            )
                            val response = repository.getAIResponse(listOf(testMessage), "default")
                            Toast.makeText(context, response, Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text(stringResource(R.string.settings_test_connection))
                }
            } else {
                Column {
                    Text(
                        text = stringResource(R.string.settings_local_models),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val localModels = remember { repository.getLocalModelList() }
                    val currentLocalModel = remember { repository.getLocalSelectedModel() }

                    localModels.forEach { model ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            androidx.compose.material3.ListItem(
                                headlineContent = {
                                    Text(
                                        text = model.displayName,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        text = stringResource(R.string.settings_downloaded),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingContent = {
                                    RadioButton(
                                        selected = currentLocalModel == model.displayName,
                                        onClick = {
                                            localSelectedModel = model.displayName
                                            repository.setLocalSelectedModel(model.displayName)
                                        },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = MaterialTheme.colorScheme.primary,
                                            unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, context.getString(R.string.settings_model_hub_coming_soon), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_add_model))
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceProviderSelector(
    serviceProvider: String,
    onServiceProviderChange: (String) -> Unit,
    customApiUrl: String,
    onCustomApiUrlChange: (String) -> Unit,
    repository: Repository
) {
    val modelDataProvider = ModelDataProvider(repository)
    val serviceProviders = modelDataProvider.getAllServiceProviders()
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var providerToDelete by remember { mutableStateOf("") }

    Column {
        Text(
            text = stringResource(R.string.settings_provider_name),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurface
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = MaterialTheme.shapes.medium
                )
        ) {
            items(serviceProviders) { provider ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onServiceProviderChange(provider) }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = provider == serviceProvider,
                            onClick = { onServiceProviderChange(provider) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = provider,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (!ModelDataProvider.predefinedServiceProviders.containsKey(provider)) {
                            IconButton(
                                onClick = {
                                    providerToDelete = provider
                                    showDeleteConfirm = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.settings_delete_provider),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                if (provider != serviceProviders.last()) {
                    HorizontalDivider()
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAddDialog = true }
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.settings_add_provider),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.settings_new_provider_title),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        if (BuiltInServiceProviders.isLegacyCustom(serviceProvider)) {
            OutlinedTextField(
                value = customApiUrl,
                onValueChange = onCustomApiUrlChange,
                label = { Text(stringResource(R.string.settings_api_base_url_compact)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = MaterialTheme.shapes.medium
            )

            Text(
                text = stringResource(R.string.settings_api_base_url_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }

    if (showAddDialog) {
        var providerName by remember { mutableStateOf("") }
        var providerUrl by remember { mutableStateOf("") }
        var providerType by remember { mutableStateOf(ProviderType.OpenAI) }
        val focusManager = LocalFocusManager.current

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(text = stringResource(R.string.settings_add_new_provider)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = providerName,
                        onValueChange = { providerName = it },
                        label = { Text(stringResource(R.string.settings_provider_name)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        shape = MaterialTheme.shapes.medium,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    OutlinedTextField(
                        value = providerUrl,
                        onValueChange = { providerUrl = it },
                        label = { Text(stringResource(R.string.settings_api_base_url_compact)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ProviderTypeDropdown(
                        providerType = providerType,
                        onProviderTypeChange = { providerType = it }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (providerName.isNotBlank() && providerUrl.isNotBlank()) {
                            repository.addCustomServiceProvider(providerName, providerUrl, providerType)
                            showAddDialog = false
                        }
                    },
                    enabled = providerName.isNotBlank() && providerUrl.isNotBlank()
                ) { Text(stringResource(R.string.action_add)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(text = stringResource(R.string.settings_confirm_delete)) },
            text = {
                Text(text = stringResource(R.string.settings_delete_provider_confirm, providerToDelete))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        repository.removeCustomServiceProvider(providerToDelete)
                        if (serviceProvider == providerToDelete) {
                            onServiceProviderChange(BuiltInServiceProviders.SiliconFlow)
                        }
                        showDeleteConfirm = false
                    }
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
fun ModelSelector(
    serviceProvider: String,
    selectedModel: String,
    onModelChange: (String) -> Unit,
    repository: Repository
) {
    val context = LocalContext.current
    val modelDataProvider = ModelDataProvider(repository)
    val models = modelDataProvider.getModelListForProvider(serviceProvider)
    val isPredefinedProvider = ModelDataProvider.predefinedServiceProviders.containsKey(serviceProvider)
    var showAddDialog by remember { mutableStateOf(false) }
    var showManageDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var modelToDelete by remember { mutableStateOf("") }
    var fetchedModels by remember { mutableStateOf<List<com.huajuan.aispace.data.ModelInfo>>(emptyList()) }
    var fetching by remember { mutableStateOf(false) }
    var fetchError by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column {
        Text(
            text = stringResource(R.string.settings_select_model),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurface
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = MaterialTheme.shapes.medium
                )
        ) {
            item {
                models.forEachIndexed { index, model ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onModelChange(model.displayName) }
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = model.displayName == selectedModel,
                                onClick = { onModelChange(model.displayName) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = model.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = model.apiCode,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            val isPredefinedModel = isPredefinedProvider &&
                                ModelDataProvider.predefinedServiceProviders[serviceProvider]?.models?.any { it.apiCode == model.apiCode } == true

                            if (!isPredefinedModel) {
                                IconButton(
                                    onClick = {
                                        modelToDelete = model.displayName
                                        showDeleteConfirm = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.settings_delete_model),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                    if (index != models.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAddDialog = true }
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.settings_add_model),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.settings_new_model_title),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            if (!isPredefinedProvider) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showManageDialog = true
                                fetchError = ""
                                fetching = true
                                scope.launch {
                                    try {
                                        fetchedModels = repository.fetchRemoteModels(serviceProvider)
                                    } catch (e: Exception) {
                                        fetchError = e.message ?: context.getString(R.string.settings_fetch_model_list_failed)
                                    } finally {
                                        fetching = false
                                    }
                                }
                            }
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.settings_manage_models),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.settings_manage_models),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        if (models.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_no_models_for_provider),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
    }

    if (showAddDialog) {
        var modelCode by remember { mutableStateOf("") }
        var modelName by remember { mutableStateOf("") }
        var selectedCapabilities by remember { mutableStateOf(emptySet<ModelCapability>()) }
        val focusManager = LocalFocusManager.current

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(text = stringResource(R.string.settings_add_new_model)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = modelCode,
                        onValueChange = { modelCode = it },
                        label = { Text(stringResource(R.string.settings_model_code)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        shape = MaterialTheme.shapes.medium,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    OutlinedTextField(
                        value = modelName,
                        onValueChange = { modelName = it },
                        label = { Text(stringResource(R.string.settings_model_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
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
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (modelCode.isNotBlank() && modelName.isNotBlank()) {
                            repository.addCustomModelToProvider(
                                providerName = serviceProvider,
                                modelName = modelName,
                                apiCode = modelCode,
                                capabilities = selectedCapabilities
                            )
                            showAddDialog = false
                        }
                    },
                    enabled = modelCode.isNotBlank() && modelName.isNotBlank()
                ) { Text(stringResource(R.string.action_add)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(text = stringResource(R.string.settings_confirm_delete)) },
            text = { Text(text = stringResource(R.string.settings_delete_model_confirm, modelToDelete)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        repository.removeCustomModelFromProvider(serviceProvider, modelToDelete)
                        if (selectedModel == modelToDelete) {
                            val firstModel = models.firstOrNull { it.displayName != modelToDelete }?.displayName ?: ""
                            if (firstModel.isNotEmpty()) {
                                onModelChange(firstModel)
                            }
                        }
                        showDeleteConfirm = false
                    }
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (showManageDialog) {
        AlertDialog(
            onDismissRequest = { showManageDialog = false },
            title = { Text(text = stringResource(R.string.settings_manage_models)) },
            text = {
                Column {
                    if (fetching) {
                        Text(
                            text = stringResource(R.string.settings_fetching_model_list),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (fetchError.isNotBlank()) {
                        Text(
                            text = fetchError,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else if (fetchedModels.isEmpty()) {
                        Text(
                            text = stringResource(R.string.settings_model_list_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                        ) {
                            items(fetchedModels) { model ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = model.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = model.apiCode,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val name = model.displayName.ifBlank { model.apiCode }
                                            repository.addCustomModelToProvider(
                                                providerName = serviceProvider,
                                                modelName = name,
                                                apiCode = model.apiCode,
                                                capabilities = model.capabilities.ifEmpty { setOf(ModelCapability.Chat) }
                                            )
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = stringResource(R.string.settings_add_model),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showManageDialog = false }) { Text(stringResource(R.string.action_done)) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderTypeDropdown(
    providerType: ProviderType,
    onProviderTypeChange: (ProviderType) -> Unit,
    modifier: Modifier = Modifier,
    label: String = ""
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
            label = { Text(label.ifBlank { stringResource(R.string.settings_protocol_type) }) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (type, optionLabel) ->
                DropdownMenuItem(
                    text = { Text(optionLabel) },
                    onClick = {
                        onProviderTypeChange(type)
                        expanded = false
                    }
                )
            }
        }
    }
}
