package com.huajuan.aispace.screens.settings.sections

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huajuan.aispace.R
import com.huajuan.aispace.components.SearchField
import com.huajuan.aispace.components.settings.SettingsListItem
import com.huajuan.aispace.data.ModelCapabilityResolver
import com.huajuan.aispace.data.ModelDataProvider
import com.huajuan.aispace.data.ModelInfo
import com.huajuan.aispace.data.ModelUsage
import com.huajuan.aispace.data.Repository
import com.huajuan.aispace.ui.animations.NavigationTransitions

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ModelPickerScreen(
    modifier: Modifier = Modifier,
    repository: Repository,
    modelUsage: ModelUsage = ModelUsage.All,
    onSelectModel: (String) -> Unit
) {
    var selectedProvider by remember { mutableStateOf<String?>(null) }
    var providerQuery by remember { mutableStateOf("") }
    var modelQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    val modelDataProvider = remember(repository) { ModelDataProvider(repository) }
    val localModels = remember(repository) { repository.getLocalModelList() }
    val providers = remember(modelDataProvider) {
        listOf(context.getString(R.string.settings_local_models)) + modelDataProvider.getAllServiceProviders()
    }
    val filteredProviders by remember(providerQuery, providers, modelUsage, localModels) {
        derivedStateOf {
            providers.filter { provider ->
                provider.contains(providerQuery, ignoreCase = true) && providerSupportsUsage(
                    providerName = provider,
                    localProviderLabel = context.getString(R.string.settings_local_models),
                    modelUsage = modelUsage,
                    localModels = localModels,
                    modelDataProvider = modelDataProvider
                )
            }
        }
    }

    val modelInfos by remember(selectedProvider, modelUsage) {
        derivedStateOf {
            when (val provider = selectedProvider) {
                null -> emptyList()
                context.getString(R.string.settings_local_models) -> localModels
                else -> modelDataProvider.getModelListForProvider(provider)
            }.let { models ->
                if (modelUsage == ModelUsage.All || selectedProvider == null) {
                    models
                } else {
                    val apiUrl = selectedProvider?.let { provider ->
                        if (provider == context.getString(R.string.settings_local_models)) null else modelDataProvider.getApiUrlForProvider(provider)
                    }
                    models.filter { model ->
                        ModelCapabilityResolver.matchesUsage(
                            modelInfo = model,
                            providerName = selectedProvider ?: "",
                            apiUrl = apiUrl,
                            usage = modelUsage
                        )
                    }
                }
            }
        }
    }
    val filteredModels by remember(modelQuery, modelInfos) {
        derivedStateOf { modelInfos.filter { it.displayName.contains(modelQuery, ignoreCase = true) } }
    }
    BackHandler(enabled = selectedProvider != null) {
        selectedProvider = null
        modelQuery = ""
    }

    AnimatedContent(
        targetState = selectedProvider,
        modifier = modifier.fillMaxSize(),
        transitionSpec = {
            val goingToModels = targetState != null
            if (goingToModels) {
                NavigationTransitions.overlayEnter() togetherWith ExitTransition.None
            } else {
                EnterTransition.None togetherWith NavigationTransitions.overlayExit()
            }
        },
        label = "ModelPickerContent"
    ) { provider ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            if (provider == null) {
                item {
                    SearchField(
                        value = providerQuery,
                        onValueChange = { providerQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = stringResource(R.string.model_picker_search_provider)
                    )
                }
                item {
                    filteredProviders.forEachIndexed { index, vendor ->
                        val isLocal = vendor == context.getString(R.string.settings_local_models)
                        SettingsListItem(
                            title = vendor,
                            subtitle = if (isLocal) stringResource(R.string.model_picker_local) else stringResource(R.string.model_picker_cloud),
                            leading = {
                                Icon(
                                    imageVector = if (isLocal) Icons.Outlined.PhoneIphone else Icons.Outlined.Cloud,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                selectedProvider = vendor
                                modelQuery = ""
                            }
                        )
                        if (index != filteredProviders.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            } else {
                item {
                    SearchField(
                        value = modelQuery,
                        onValueChange = { modelQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = stringResource(R.string.model_picker_search_model)
                    )
                }
                item {
                    filteredModels.forEachIndexed { index, model ->
                        SettingsListItem(
                            title = model.displayName,
                            subtitle = model.apiCode,
                            leading = { Icon(Icons.Outlined.Tune, contentDescription = null) },
                            onClick = {
                                val ref = if (provider == context.getString(R.string.settings_local_models)) "local|${model.displayName}" else "$provider|${model.displayName}"
                                onSelectModel(ref)
                            }
                        )
                        if (index != filteredModels.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

private fun providerSupportsUsage(
    providerName: String,
    localProviderLabel: String,
    modelUsage: ModelUsage,
    localModels: List<ModelInfo>,
    modelDataProvider: ModelDataProvider
): Boolean {
    if (modelUsage == ModelUsage.All) return true
    val models = if (providerName == localProviderLabel) {
        localModels
    } else {
        modelDataProvider.getModelListForProvider(providerName)
    }
    val apiUrl = if (providerName == localProviderLabel) null else modelDataProvider.getApiUrlForProvider(providerName)
    return models.any { model ->
        ModelCapabilityResolver.matchesUsage(
            modelInfo = model,
            providerName = providerName,
            apiUrl = apiUrl,
            usage = modelUsage
        )
    }
}
