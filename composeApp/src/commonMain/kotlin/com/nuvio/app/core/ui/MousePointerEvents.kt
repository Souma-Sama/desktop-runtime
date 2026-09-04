package com.nuvio.app.core.ui

import androidx.compose.ui.Modifier

internal expect fun Modifier.mouseBackButton(onBack: () -> Unit): Modifier

internal expect fun Modifier.playerMouseNavButtons(): Modifier

internal expect fun Modifier.onScrollDismiss(onDismiss: () -> Unit): Modifier
