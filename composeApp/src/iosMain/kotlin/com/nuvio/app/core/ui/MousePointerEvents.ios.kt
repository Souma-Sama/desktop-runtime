package com.nuvio.app.core.ui

import androidx.compose.ui.Modifier

internal actual fun Modifier.mouseBackButton(onBack: () -> Unit): Modifier = this

internal actual fun Modifier.playerMouseNavButtons(): Modifier = this

internal actual fun Modifier.onScrollDismiss(onDismiss: () -> Unit): Modifier = this
