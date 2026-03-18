package com.huajuan.aispace.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

@Composable
fun DrawerUiResetEffect(
    resetRequest: Int,
    onReset: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(resetRequest) {
        if (resetRequest <= 0) return@LaunchedEffect
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        onReset()
    }
}
