package com.huajuan.aispace.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import com.huajuan.aispace.R

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: ImageVector = Icons.Outlined.Search,
    trailingIcon: (@Composable (() -> Unit))? = null,
    singleLine: Boolean = true,
    allowClear: Boolean = true,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val resolvedTrailingIcon = when {
        trailingIcon != null -> trailingIcon
        allowClear && value.isNotBlank() -> {
            {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.search_clear))
                }
            }
        }
        else -> null
    }
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { onClick() }
    } else {
        Modifier
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.then(clickModifier),
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = { Icon(leadingIcon, contentDescription = null) },
        trailingIcon = resolvedTrailingIcon,
        singleLine = singleLine,
        enabled = enabled,
        readOnly = readOnly
    )
}
