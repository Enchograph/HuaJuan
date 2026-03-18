package com.huajuan.aispace.screens.image

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.huajuan.aispace.R
import com.huajuan.aispace.components.image.SmartAsyncImage
import com.huajuan.aispace.components.model.ModelRequirementDialog
import com.huajuan.aispace.components.model.ModelRequirementIssue
import com.huajuan.aispace.components.model.resolveModelRequirementIssue
import com.huajuan.aispace.components.settings.SettingsListItem
import com.huajuan.aispace.data.ImageGenerationCapabilityStatus
import com.huajuan.aispace.data.ImageGenerationConversationState
import com.huajuan.aispace.data.ImageGenerationRequest
import com.huajuan.aispace.data.ImageGenerationSupport
import com.huajuan.aispace.data.ModelSelectionValidator
import com.huajuan.aispace.data.ModelUsage
import com.huajuan.aispace.data.Repository
import com.huajuan.aispace.navigation.DrawerUiResetEffect
import com.huajuan.aispace.screens.settings.sections.ModelPickerScreen
import com.huajuan.aispace.ui.theme.AppDimens
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageGenerationScreen(
    onMenuClick: () -> Unit,
    drawerUiResetRequest: Int = 0,
    repository: Repository,
    assistantId: String,
    currentConversationId: String?,
    onCurrentConversationChange: (String?) -> Unit,
    onOpenProviderModelSettings: (String) -> Unit = {}
) {
    var prompt by remember { mutableStateOf("") }
    var negativePrompt by remember { mutableStateOf("") }
    var widthInput by remember { mutableStateOf("1024") }
    var heightInput by remember { mutableStateOf("1024") }
    var generationCount by remember { mutableStateOf(1) }
    var steps by remember { mutableStateOf(28f) }
    var guidance by remember { mutableStateOf(7f) }
    var seed by remember { mutableStateOf("") }
    var showModelPicker by remember { mutableStateOf(false) }
    var modelLabel by remember { mutableStateOf(resolveModelLabel(repository, assistantId)) }
    var latestImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var isGenerating by remember { mutableStateOf(false) }
    var isProbing by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }
    var modelRequirementIssue by remember { mutableStateOf<ModelRequirementIssue?>(null) }
    var support by remember { mutableStateOf(repository.getImageGenerationSupport(assistantId)) }
    val scope = rememberCoroutineScope()
    val context = repository.getContext()
    val widthValue = widthInput.toIntOrNull()
    val heightValue = heightInput.toIntOrNull()
    val hasValidSize = widthValue != null && heightValue != null && widthValue > 0 && heightValue > 0

    DrawerUiResetEffect(drawerUiResetRequest) {
        showModelPicker = false
        showAdvanced = false
        modelRequirementIssue = null
    }

    fun applyRequest(request: ImageGenerationRequest) {
        prompt = request.prompt
        negativePrompt = request.negativePrompt
        widthInput = request.width.toString()
        heightInput = request.height.toString()
        generationCount = request.count
        steps = request.steps.toFloat()
        guidance = request.guidanceScale
        seed = request.seed?.toString().orEmpty()
    }

    fun normalizeCountWithSupport() {
        generationCount = generationCount.coerceIn(1, support.maxCount.coerceAtLeast(1))
    }

    LaunchedEffect(assistantId) {
        modelLabel = resolveModelLabel(repository, assistantId)
        support = repository.getImageGenerationSupport(assistantId)
        normalizeCountWithSupport()
    }

    LaunchedEffect(currentConversationId, assistantId) {
        support = repository.getImageGenerationSupport(assistantId)
        if (currentConversationId.isNullOrBlank()) {
            applyRequest(ImageGenerationRequest(prompt = "", count = 1))
            latestImages = emptyList()
            normalizeCountWithSupport()
            return@LaunchedEffect
        }
        val messages = repository.getMessagesAsync(currentConversationId, assistantId)
        val state = repository.getImageGenerationConversationState(currentConversationId)
        val lastUserMessage = messages.lastOrNull { it.isUser }
        val lastAiMessage = messages.lastOrNull { !it.isUser }
        if (state != null) {
            applyRequest(state.request)
        } else {
            prompt = lastUserMessage?.text.orEmpty()
            negativePrompt = ""
            widthInput = "1024"
            heightInput = "1024"
            generationCount = 1
            steps = 28f
            guidance = 7f
            seed = ""
        }
        latestImages = lastAiMessage?.imageUris.orEmpty()
        normalizeCountWithSupport()
    }

    if (showModelPicker) {
        ModalBottomSheet(onDismissRequest = { showModelPicker = false }) {
            ModelPickerScreen(
                repository = repository,
                modelUsage = ModelUsage.ImageGeneration,
                onSelectModel = { ref ->
                    applyModelSelection(repository, assistantId, ref)
                    modelLabel = resolveModelLabel(repository, assistantId)
                    support = repository.getImageGenerationSupport(assistantId)
                    normalizeCountWithSupport()
                    showModelPicker = false
                }
            )
        }
    }

    ModelRequirementDialog(
        issue = modelRequirementIssue,
        onDismiss = { modelRequirementIssue = null },
        onOpenModelPicker = { showModelPicker = true },
        onOpenProviderSettings = {
            val provider = repository.getAssistantModelConfig(assistantId).serviceProvider
            onOpenProviderModelSettings(provider)
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.image_generation_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Outlined.Menu, contentDescription = stringResource(R.string.cd_open_drawer))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (isGenerating || isProbing) return@IconButton
                            scope.launch {
                                val conversationId = createImageConversation(
                                    repository = repository,
                                    assistantId = assistantId,
                                    title = repository.getContext().getString(R.string.image_generation_new)
                                )
                                applyRequest(ImageGenerationRequest(prompt = "", count = 1))
                                latestImages = emptyList()
                                repository.updateImageGenerationConversationState(conversationId, null)
                                onCurrentConversationChange(conversationId)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.image_generation_new))
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = AppDimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spacingL),
            contentPadding = PaddingValues(bottom = AppDimens.screenPaddingBottom)
        ) {
            item {
                val displayImages = latestImages.take(generationCount.coerceAtLeast(1))
                val slots = generationCount.coerceAtLeast(1)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.cardSpacing),
                    contentPadding = PaddingValues(end = AppDimens.spacingS),
                    modifier = Modifier.height(210.dp)
                ) {
                    items((0 until slots).toList()) { index ->
                        Card(shape = MaterialTheme.shapes.large, modifier = Modifier.width(190.dp)) {
                            if (index < displayImages.size) {
                                SmartAsyncImage(
                                    imageIdOrUri = displayImages[index],
                                    repository = repository,
                                    contentDescription = stringResource(R.string.image_generation_generate),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(190.dp)
                                        .clip(MaterialTheme.shapes.large)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(190.dp)
                                        .clip(MaterialTheme.shapes.large)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = if (isGenerating) stringResource(R.string.image_generation_working) else stringResource(R.string.image_generation_waiting),
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(AppDimens.spacingM),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(AppDimens.cardSpacing)) {
                    SettingsListItem(
                        title = stringResource(R.string.image_generation_model),
                        subtitle = modelLabel,
                        leading = { Icon(Icons.Outlined.Tune, contentDescription = null) },
                        onClick = { showModelPicker = true }
                    )

                    CapabilityCard(
                        support = support,
                        onProbe = {
                            if (isProbing) return@CapabilityCard
                            scope.launch {
                                isProbing = true
                                support = repository.probeImageGenerationSupport(assistantId)
                                normalizeCountWithSupport()
                                Toast.makeText(context, support.message.ifBlank {
                                    when (support.status) {
                                        ImageGenerationCapabilityStatus.Supported -> context.getString(R.string.image_generation_capability_supported)
                                        ImageGenerationCapabilityStatus.Unsupported -> context.getString(R.string.image_generation_capability_unsupported)
                                        ImageGenerationCapabilityStatus.Unknown -> context.getString(R.string.image_generation_capability_unknown)
                                    }
                                }, Toast.LENGTH_SHORT).show()
                                isProbing = false
                            }
                        },
                        probing = isProbing
                    )

                    Text(text = stringResource(R.string.image_generation_size), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spacingS)) {
                        OutlinedTextField(
                            value = widthInput,
                            onValueChange = { widthInput = it.filter(Char::isDigit) },
                            label = { Text(stringResource(R.string.image_generation_width)) },
                            placeholder = { Text("1024") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = heightInput,
                            onValueChange = { heightInput = it.filter(Char::isDigit) },
                            label = { Text(stringResource(R.string.image_generation_height)) },
                            placeholder = { Text("1024") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.image_generation_count))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { generationCount = (generationCount - 1).coerceAtLeast(1) }) {
                                Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.image_generation_decrease))
                            }
                            Text(generationCount.toString())
                            IconButton(onClick = {
                                generationCount = (generationCount + 1).coerceAtMost(support.maxCount.coerceAtLeast(1))
                            }) {
                                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.image_generation_increase))
                            }
                        }
                    }

                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        label = { Text(stringResource(R.string.image_generation_prompt)) },
                        placeholder = { Text(stringResource(R.string.image_generation_prompt_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    OutlinedTextField(
                        value = negativePrompt,
                        onValueChange = { negativePrompt = it },
                        enabled = support.supportsNegativePrompt,
                        label = { Text(stringResource(R.string.image_generation_negative_prompt)) },
                        placeholder = { Text(stringResource(R.string.image_generation_negative_prompt_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAdvanced = !showAdvanced },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Tune, contentDescription = null)
                        Spacer(modifier = Modifier.width(AppDimens.spacingS))
                        Text(
                            text = if (showAdvanced) stringResource(R.string.image_generation_advanced_collapse) else stringResource(R.string.image_generation_advanced),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.rotate(if (showAdvanced) 90f else 0f)
                        )
                    }

                    if (showAdvanced) {
                        if (!support.supportsSteps && !support.supportsGuidanceScale && !support.supportsSeed) {
                            Text(
                                text = stringResource(R.string.image_generation_unsupported_param_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column {
                            Text(stringResource(R.string.image_generation_steps, steps.toInt()))
                            Slider(
                                value = steps,
                                enabled = support.supportsSteps,
                                onValueChange = { steps = it },
                                valueRange = 10f..60f
                            )
                        }
                        Column {
                            Text(stringResource(R.string.image_generation_guidance, "%.1f".format(guidance)))
                            Slider(
                                value = guidance,
                                enabled = support.supportsGuidanceScale,
                                onValueChange = { guidance = it },
                                valueRange = 1f..15f
                            )
                        }
                        OutlinedTextField(
                            value = seed,
                            onValueChange = { seed = it.filter(Char::isDigit) },
                            enabled = support.supportsSeed,
                            label = { Text(stringResource(R.string.image_generation_seed)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Button(
                        onClick = {
                            if (prompt.isBlank() || isGenerating || !hasValidSize || !support.canGenerate) return@Button
                            val issue = resolveModelRequirementIssue(repository, assistantId)
                            if (issue != null) {
                                modelRequirementIssue = issue
                                return@Button
                            }
                            scope.launch {
                                isGenerating = true
                                val conversationId = createImageConversation(
                                    repository = repository,
                                    assistantId = assistantId,
                                    title = prompt.take(12).ifBlank { repository.getContext().getString(R.string.image_generation_record) }
                                )
                                onCurrentConversationChange(conversationId)
                                val request = ImageGenerationRequest(
                                    prompt = prompt,
                                    negativePrompt = negativePrompt,
                                    width = widthValue ?: 1024,
                                    height = heightValue ?: 1024,
                                    count = generationCount,
                                    steps = steps.toInt(),
                                    guidanceScale = guidance,
                                    seed = seed.toLongOrNull()
                                )
                                val userMessage = com.huajuan.aispace.data.Message(
                                    id = UUID.randomUUID().toString(),
                                    text = prompt,
                                    isUser = true,
                                    timestamp = Date()
                                )
                                val existing = repository.getMessagesAsync(conversationId, assistantId)
                                val updatedMessages = existing + userMessage
                                repository.saveMessages(conversationId, assistantId, updatedMessages)
                                repository.updateLastMessage(conversationId, prompt)
                                val result = repository.generateImage(assistantId, conversationId, request)
                                latestImages = result.imageIds
                                applyRequest(result.appliedRequest)
                                support = repository.getImageGenerationSupport(assistantId)
                                val aiMessage = com.huajuan.aispace.data.Message(
                                    id = UUID.randomUUID().toString(),
                                    text = result.displayText,
                                    isUser = false,
                                    timestamp = Date(),
                                    imageUris = result.imageIds
                                )
                                repository.saveMessages(conversationId, assistantId, updatedMessages + aiMessage)
                                repository.updateLastMessage(
                                    conversationId,
                                    result.displayText.ifBlank {
                                        repository.getContext().getString(R.string.image_generation_generate)
                                    }
                                )
                                if (result.imageIds.isEmpty() && result.displayText.isNotBlank()) {
                                    Toast.makeText(context, result.displayText, Toast.LENGTH_SHORT).show()
                                }
                                isGenerating = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isGenerating && prompt.isNotBlank() && hasValidSize && support.canGenerate
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null)
                        Spacer(modifier = Modifier.width(AppDimens.spacingS))
                        Text(if (isGenerating) stringResource(R.string.image_generation_generating) else stringResource(R.string.image_generation_generate))
                    }
                }
            }
        }
    }
}

@Composable
private fun CapabilityCard(
    support: ImageGenerationSupport,
    onProbe: () -> Unit,
    probing: Boolean
) {
    Card(shape = MaterialTheme.shapes.large) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.spacingM),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spacingS)
        ) {
            val title = when (support.status) {
                ImageGenerationCapabilityStatus.Supported -> stringResource(R.string.image_generation_capability_supported)
                ImageGenerationCapabilityStatus.Unsupported -> stringResource(R.string.image_generation_capability_unsupported)
                ImageGenerationCapabilityStatus.Unknown -> stringResource(R.string.image_generation_capability_unknown)
            }
            Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            if (support.status == ImageGenerationCapabilityStatus.Unknown) {
                Text(
                    text = stringResource(R.string.image_generation_capability_probe_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (support.message.isNotBlank()) {
                Text(
                    text = support.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (support.canProbe) {
                Button(onClick = onProbe, enabled = !probing) {
                    Text(
                        if (probing) stringResource(R.string.image_generation_generating)
                        else stringResource(R.string.image_generation_detect_capability)
                    )
                }
            }
        }
    }
}

private fun resolveModelLabel(repository: Repository, assistantId: String): String {
    if (ModelSelectionValidator.isModelMissing(repository, assistantId)) {
        return repository.getContext().getString(R.string.chat_model_unselected)
    }
    val config = repository.getAssistantModelConfig(assistantId)
    return if (config.useCloudModel) {
        "${config.serviceProvider} · ${config.selectedModelName}"
    } else {
        repository.getContext().getString(R.string.settings_local_model_format, config.localSelectedModelName)
    }
}

private fun applyModelSelection(repository: Repository, assistantId: String, ref: String) {
    val parts = ref.split("|")
    if (parts.size != 2) return
    val provider = parts[0]
    val model = parts[1]
    if (provider == "local") {
        repository.setAssistantUseCloudModel(assistantId, false)
        repository.setAssistantLocalSelectedModel(assistantId, model)
    } else {
        repository.setAssistantUseCloudModel(assistantId, true)
        repository.setAssistantServiceProvider(assistantId, provider)
        repository.setAssistantSelectedModel(assistantId, model)
    }
}

private suspend fun createImageConversation(
    repository: Repository,
    assistantId: String,
    title: String
): String {
    val agent = repository.getAssistantById(assistantId)
    val conversation = repository.createNewConversation(
        assistantId = assistantId,
        title = title,
        roleName = agent?.name ?: repository.getContext().getString(R.string.image_generation_title),
        systemPrompt = agent?.systemPrompt ?: repository.getContext().getString(R.string.image_generation_system_prompt_fallback)
    )
    repository.saveMessages(conversation.id, assistantId, emptyList())
    repository.updateImageGenerationConversationState(conversation.id, null)
    return conversation.id
}
