package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.DayKey
import com.example.domain.SpanishFormatters
import com.example.ui.MainUiState
import com.example.ui.components.OutageEntrySheet
import com.example.ui.theme.LocalPowerSemanticColors

@Composable
fun HomeScreen(
    uiState: MainUiState,
    bottomContentPadding: androidx.compose.ui.unit.Dp,
    onStartOutage: () -> Unit,
    onStopOutage: () -> Unit,
    onCancelActiveOutage: () -> Unit,
    onAddManualOutage: (startMillis: Long, endMillis: Long, note: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalPowerSemanticColors.current
    var showManualSheet by remember { mutableStateOf(false) }
    var showCancelConfirm by remember { mutableStateOf(false) }

    val activeOutage = uiState.activeOutage
    val isPowerOut = activeOutage != null
    val todayKey = remember(uiState.nowMillis) { DayKey.fromMillis(uiState.nowMillis) }

    val statusContainer by animateColorAsState(
        targetValue = if (isPowerOut) semantic.powerOffContainer else semantic.powerOnContainer,
        label = "statusContainer"
    )
    val statusContent by animateColorAsState(
        targetValue = if (isPowerOut) semantic.powerOffContent else semantic.powerOnContent,
        label = "statusContent"
    )
    val statusAccent by animateColorAsState(
        targetValue = if (isPowerOut) semantic.powerOffAccent else semantic.powerOnAccent,
        label = "statusAccent"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = bottomContentPadding + 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Title + Current Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = SpanishFormatters.formatFullDate(todayKey),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Clear Status Pill
                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusContainer,
                    border = BorderStroke(1.dp, statusAccent.copy(alpha = 0.45f)),
                    modifier = Modifier.testTag("power_status_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(statusAccent)
                        )
                        Icon(
                            imageVector = if (isPowerOut) Icons.Filled.FlashOff else Icons.Filled.Lightbulb,
                            contentDescription = null,
                            tint = statusContent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isPowerOut) {
                                stringResource(R.string.status_power_off)
                            } else {
                                stringResource(R.string.status_power_on)
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusContent
                        )
                    }
                }
            }

            // Hero Action Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 4.dp,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isPowerOut) {
                        semantic.powerOffAccent.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = if (isPowerOut) {
                                    listOf(
                                        semantic.powerOffContainer.copy(alpha = 0.32f),
                                        Color.Transparent
                                    )
                                } else {
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                        Color.Transparent
                                    )
                                }
                            )
                        )
                        .padding(20.dp)
                ) {
                    AnimatedContent(
                        targetState = activeOutage,
                        label = "heroOutageState"
                    ) { currentActive ->
                        if (currentActive == null) {
                            // Hay luz state
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(semantic.powerOnContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Lightbulb,
                                        contentDescription = "Servicio eléctrico activo",
                                        tint = semantic.powerOnAccent,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }

                                Text(
                                    text = "Servicio eléctrico activo",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Toca el botón cuando ocurra un corte para empezar a contar al instante.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                Button(
                                    onClick = onStartOutage,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp)
                                        .testTag("start_outage_button"),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FlashOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = stringResource(R.string.action_power_went_out),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                            }
                        } else {
                            // Sin luz state
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "TIEMPO SIN ELECTRICIDAD",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = semantic.powerOffContent,
                                    letterSpacing = 1.2.sp
                                )

                                Text(
                                    text = SpanishFormatters.formatChronometer(uiState.activeElapsedMillis),
                                    style = MaterialTheme.typography.displayLarge.copy(
                                        fontSize = 42.sp,
                                        lineHeight = 50.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Clip,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.testTag("active_chronometer")
                                )

                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Schedule,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Se fue a las ${SpanishFormatters.formatTime12h(currentActive.startTimeMillis)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Button(
                                    onClick = onStopOutage,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp)
                                        .testTag("stop_outage_button"),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = semantic.powerOnAccent,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Bolt,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.action_power_came_back),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }

                                TextButton(
                                    onClick = { showCancelConfirm = true },
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("cancel_active_button"),
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.action_cancel_active),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Today's Summary
            Text(
                text = "Resumen de hoy",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            val todayStats = uiState.stats.today
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TodayMetricTile(
                        title = "Sin luz hoy",
                        value = SpanishFormatters.formatDurationReadable(todayStats.withoutPowerMillis),
                        icon = Icons.Outlined.PowerSettingsNew,
                        accentColor = semantic.powerOffAccent,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("today_without_power")
                    )
                    TodayMetricTile(
                        title = "Con luz hoy",
                        value = SpanishFormatters.formatDurationReadable(todayStats.withPowerMillis),
                        icon = Icons.Outlined.WbSunny,
                        accentColor = semantic.powerOnAccent,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("today_with_power")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TodayMetricTile(
                        title = "Apagones hoy",
                        value = todayStats.outageCount.toString(),
                        icon = Icons.Filled.FlashOff,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("today_outage_count")
                    )
                    TodayMetricTile(
                        title = "Más largo hoy",
                        value = SpanishFormatters.formatDurationReadable(todayStats.longestOutageMillis),
                        icon = Icons.Outlined.Schedule,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("today_longest")
                    )
                }
            }

            // Secondary Manual Entry Button
            OutlinedButton(
                onClick = { showManualSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("manual_entry_button"),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.action_manual_entry),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    if (showManualSheet) {
        OutageEntrySheet(
            existingOutage = null,
            onDismiss = { showManualSheet = false },
            onSave = { start, end, note ->
                onAddManualOutage(start, end, note)
                showManualSheet = false
            }
        )
    }

    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title = { Text("¿Cancelar apagón activo?") },
            text = {
                Text("Si lo tocaste por error, se descartará sin guardarlo en el historial.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelConfirm = false
                        onCancelActiveOutage()
                    },
                    modifier = Modifier.testTag("confirm_cancel_active")
                ) {
                    Text("Sí, cancelar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirm = false }) {
                    Text("Seguir contando")
                }
            }
        )
    }
}

@Composable
private fun TodayMetricTile(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
