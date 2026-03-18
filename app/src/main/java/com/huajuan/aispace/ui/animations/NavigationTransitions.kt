package com.huajuan.aispace.ui.animations

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

object NavigationTransitions {
    fun overlayEnter(): EnterTransition =
        slideInHorizontally(initialOffsetX = { it }) + fadeIn()

    fun overlayExit(): ExitTransition =
        slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
}
