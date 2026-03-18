package com.huajuan.aispace.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.huajuan.aispace.R
import com.huajuan.aispace.components.settings.SettingsListItem
import com.huajuan.aispace.components.settings.SettingsSectionCard
import com.huajuan.aispace.data.Repository
import com.huajuan.aispace.screens.settings.sections.DebugSettingSection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun AboutScreen(modifier: Modifier) {
    val context = LocalContext.current
    val versionName = remember {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        info.versionName ?: "0.1"
    }
    val appIcon = remember {
        context.packageManager.getApplicationIcon(context.packageName).toBitmap().asImageBitmap()
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            SettingsSectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        bitmap = appIcon,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(stringResource(R.string.settings_version_name, versionName), fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                SettingsListItem(
                    title = stringResource(R.string.settings_terms_title),
                    subtitle = stringResource(R.string.settings_terms_subtitle),
                    onClick = {}
                )
                SettingsListItem(
                    title = stringResource(R.string.settings_privacy_title),
                    subtitle = stringResource(R.string.settings_privacy_subtitle),
                    onClick = {}
                )
            }
        }
    }
}

@Composable
internal fun DebugSettingsScreen(
    modifier: Modifier,
    repository: Repository,
    debugMode: Boolean,
    tokenAnimationFixedDurationEnabled: Boolean,
    tokenAnimationFixedDurationMs: Int,
    scope: kotlinx.coroutines.CoroutineScope,
    onDebugModeChange: (Boolean) -> Unit,
    onTokenAnimationFixedDurationEnabledChange: (Boolean) -> Unit,
    onTokenAnimationFixedDurationMsChange: (Int) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            DebugSettingSection(
                debugMode = debugMode,
                tokenAnimationFixedDurationEnabled = tokenAnimationFixedDurationEnabled,
                tokenAnimationFixedDurationMs = tokenAnimationFixedDurationMs,
                repository = repository,
                scope = scope,
                onDebugModeChange = onDebugModeChange,
                onTokenAnimationFixedDurationEnabledChange = onTokenAnimationFixedDurationEnabledChange,
                onTokenAnimationFixedDurationMsChange = onTokenAnimationFixedDurationMsChange
            )
        }
    }
}

@Composable
internal fun ModelDownloadScreen(modifier: Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
                SettingsSectionCard {
                Text(stringResource(R.string.settings_local_model_not_built_in))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.settings_local_model_import_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

internal fun formatTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return "0"
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
