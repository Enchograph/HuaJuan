package com.huajuan.aispace.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

internal data class OverlayEntry<T>(
    val id: Long,
    val route: T,
    val visibility: MutableTransitionState<Boolean>
)

@Stable
class LayeredNavigatorState<T> internal constructor(
    isLayer0Open: () -> Boolean,
    openLayer0: () -> Unit,
    closeLayer0: () -> Unit,
    navigateMain: (String) -> Unit,
    initialMainRoute: String,
    exitApp: () -> Unit
) {
    private var isLayer0Open: () -> Boolean = isLayer0Open
    private var openLayer0: () -> Unit = openLayer0
    private var closeLayer0: () -> Unit = closeLayer0
    private var navigateMain: (String) -> Unit = navigateMain
    private var exitApp: () -> Unit = exitApp

    private val overlayEntries = mutableStateListOf<OverlayEntry<T>>()
    private var overlayIdSeed = 0L
    private val currentMainRouteState = mutableStateOf(initialMainRoute)

    val hasOverlays: Boolean
        get() = overlayEntries.isNotEmpty()

    val currentMainRoute: String
        get() = currentMainRouteState.value

    fun pushOverlay(route: T) {
        val entry = OverlayEntry(
            id = overlayIdSeed++,
            route = route,
            visibility = MutableTransitionState(false).apply { targetState = true }
        )
        overlayEntries.add(entry)
    }

    fun popOverlay() {
        overlayEntries.lastOrNull()?.let { entry ->
            entry.visibility.targetState = false
        }
    }

    fun openLayer0() {
        openLayer0.invoke()
    }

    fun closeLayer0() {
        closeLayer0.invoke()
    }

    fun navigateMain(route: String) {
        currentMainRouteState.value = route
        navigateMain.invoke(route)
    }

    fun handleBack() {
        when {
            overlayEntries.isNotEmpty() -> popOverlay()
            isLayer0Open() -> exitApp()
            else -> openLayer0()
        }
    }

    internal fun updateHandlers(
        isLayer0Open: () -> Boolean,
        openLayer0: () -> Unit,
        closeLayer0: () -> Unit,
        navigateMain: (String) -> Unit,
        exitApp: () -> Unit
    ) {
        this.isLayer0Open = isLayer0Open
        this.openLayer0 = openLayer0
        this.closeLayer0 = closeLayer0
        this.navigateMain = navigateMain
        this.exitApp = exitApp
    }

    internal fun setCurrentMainRoute(route: String) {
        currentMainRouteState.value = route
    }

    internal fun entries(): List<OverlayEntry<T>> = overlayEntries

    internal fun onEntryIdle(entry: OverlayEntry<T>) {
        if (entry.visibility.isIdle && !entry.visibility.currentState) {
            overlayEntries.remove(entry)
        }
    }
}

@Composable
fun <T> rememberLayeredNavigator(
    isLayer0Open: () -> Boolean,
    openLayer0: () -> Unit,
    closeLayer0: () -> Unit,
    navigateMain: (String) -> Unit,
    initialMainRoute: String,
    exitApp: () -> Unit
): LayeredNavigatorState<T> {
    val state = remember {
        LayeredNavigatorState<T>(
            isLayer0Open,
            openLayer0,
            closeLayer0,
            navigateMain,
            initialMainRoute,
            exitApp
        )
    }
    SideEffect {
        state.updateHandlers(isLayer0Open, openLayer0, closeLayer0, navigateMain, exitApp)
    }
    return state
}

@Composable
fun <T> LayeredOverlayHost(
    state: LayeredNavigatorState<T>,
    enter: EnterTransition,
    exit: ExitTransition,
    content: @Composable (route: T, onBack: () -> Unit) -> Unit
) {
    if (!state.hasOverlays) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        state.entries().forEach { entry ->
            key(entry.id) {
                AnimatedVisibility(
                    visibleState = entry.visibility,
                    enter = enter,
                    exit = exit
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent)
                    ) {
                        content(entry.route, state::popOverlay)
                    }
                }
                LaunchedEffect(entry.visibility.isIdle, entry.visibility.currentState) {
                    state.onEntryIdle(entry)
                }
            }
        }
    }
}
