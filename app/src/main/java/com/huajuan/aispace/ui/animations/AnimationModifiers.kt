package com.huajuan.aispace.ui.animations

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.animateItem(
    enterOffsetY: Dp = 12.dp,
    enterScale: Float = 0.985f,
    durationMillis: Int = 240
): Modifier = composed {
    val targetOffset by rememberUpdatedState(enterOffsetY)
    val targetScale by rememberUpdatedState(enterScale)
    var appeared by remember { mutableStateOf(false) }
    val transition = updateTransition(targetState = appeared, label = "item_appear")
    val alpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = durationMillis, easing = FastOutSlowInEasing) },
        label = "item_alpha"
    ) { if (it) 1f else 0f }
    val offsetY by transition.animateDp(
        transitionSpec = { tween(durationMillis = durationMillis + 60, easing = FastOutSlowInEasing) },
        label = "item_offset"
    ) { if (it) 0.dp else targetOffset }
    val scale by transition.animateFloat(
        transitionSpec = { tween(durationMillis = durationMillis, easing = FastOutSlowInEasing) },
        label = "item_scale"
    ) { if (it) 1f else targetScale }

    LaunchedEffect(Unit) {
        appeared = true
    }

    this.graphicsLayer {
        this.alpha = alpha
        translationY = offsetY.toPx()
        scaleX = scale
        scaleY = scale
    }
}
