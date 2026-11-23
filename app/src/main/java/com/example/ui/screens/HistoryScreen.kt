package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.local.OutageEntity
import com.example.domain.DayKey
import com.example.domain.SpanishFormatters
import com.example.ui.MainUiState
import com.example.ui.components.OutageEntrySheet
import com.example.ui.theme.LocalPowerSemanticColors

@Composable
fun HistoryScreen(
    uiState: MainUiState,
    bottomContentPadding: Dp,
    onSearchQueryChange: (String) -> Unit,
    onUpdateOutage: (OutageEntity) -> Unit,
    onDeleteOutage: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingOutage by remember { mutableStateOf<OutageEntity?>(null) }

    val todayKey = remember(uiState.nowMillis) { DayKey.fromMillis(uiState.nowMillis) }
    val yesterdayKey = remember(todayKey) { todayKey.addDays(-1) }

    val filteredOutages = remember(uiState.completedOutages, uiState.searchQuery) {
        val normalizedQuery = SpanishFormatters.normalizeForSearch(uiState.searchQuery.trim())
        if (normalizedQuery.isEmpty()) {
            uiState.completedOutages
        } else {
            uiState.completedOutages.filter { outage ->
                val dayKey = DayKey.fromMillis(outage.startTimeMillis)
                val fullDate = SpanishFormatters.normalizeForSearch(SpanishFormatters.formatFullDate(dayKey))
                val numericDate = SpanishFormatters.formatNumericDate(dayKey)
                val dayHeader = SpanishFormatters.normalizeForSearch(
                    SpanishFormatters.formatDayHeader(dayKey, todayKey, yesterdayKey)
                )
                val note = SpanishFormatters.normalizeForSearch(outage.note)
                fullDate.contains(normalizedQuery) ||
                    numericDate.contains(normalizedQuery) ||
                    dayHeader.contains(normalizedQuery) ||
                    note.contains(normalizedQuery)
            }
        }
    }

    val groupedByDay = remember(filteredOutages) {
        filteredOutages
            .groupBy { DayKey.fromMillis(it.startTimeMillis) }
            .entries
            .sortedByDescending { it.key }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Historial",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = SpanishFormatters.formatOutageCountPhrase(uiState.completedOutages.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Buscar por fecha o nota...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Buscar"
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Limpiar búsqueda"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_search_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (groupedByDay.isEmpty()) {
                HistoryEmptyState(
                    hasAnyOutages = uiState.completedOutages.isNotEmpty(),
                    activeOutagePresent = uiState.activeOutage != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = bottomContentPadding)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("history_list"),
                    contentPadding = PaddingValues(bottom = bottomContentPadding + 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (uiState.activeOutage != null) {
                        item(key = "active_banner") {
                            ActiveOutageBanner(
                                startTimeMillis = uiState.activeOutage.startTimeMillis,
                                elapsedMillis = uiState.activeElapsedMillis
                            )
                        }
                    }

                    groupedByDay.forEach { (dayKey, itemsInDay) ->
                        item(key = "header_${dayKey.year}_${dayKey.month}_${dayKey.dayOfMonth}") {
                            Text(
                                text = SpanishFormatters.formatDayHeader(dayKey, todayKey, yesterdayKey),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                            )
                        }

                        items(
                            items = itemsInDay,
                            key = { it.id }
                        ) { outage ->
                            OutageHistoryCard(
                                outage = outage,
                                onClick = { editingOutage = outage }
                            )
                        }
                    }
                }
            }
        }
    }

    editingOutage?.let { selected ->
        OutageEntrySheet(
            existingOutage = selected,
            onDismiss = { editingOutage = null },
            onSave = { start, end, note ->
                onUpdateOutage(
                    selected.copy(
                        startTimeMillis = start,
                        endTimeMillis = end,
                        note = note
                    )
                )
                editingOutage = null
            },
            onDelete = { toDelete ->
                onDeleteOutage(toDelete.id)
                editingOutage = null
            }
        )
    }
}

@Composable
private fun ActiveOutageBanner(
    startTimeMillis: Long,
    elapsedMillis: Long
) {
    val semantic = LocalPowerSemanticColors.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = semantic.powerOffContainer,
        border = BorderStroke(1.dp, semantic.powerOffAccent.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.FlashOff,
                    contentDescription = null,
                    tint = semantic.powerOffAccent
                )
                Column {
                    Text(
                        text = "Apagón activo ahora",
                        style = MaterialTheme.typography.titleSmall,
                        color = semantic.powerOffContent,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Desde las ${SpanishFormatters.formatTime12h(startTimeMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = semantic.powerOffContent.copy(alpha = 0.85f)
                    )
                }
            }
            Text(
                text = SpanishFormatters.formatChronometer(elapsedMillis),
                style = MaterialTheme.typography.titleMedium,
                color = semantic.powerOffContent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun OutageHistoryCard(
    outage: OutageEntity,
    onClick: () -> Unit
) {
    val end = outage.endTimeMillis ?: return
    val durationMillis = (end - outage.startTimeMillis).coerceAtLeast(0L)
    val dayKey = DayKey.fromMillis(outage.startTimeMillis)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("history_item_${outage.id}"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = SpanishFormatters.formatFullDate(dayKey),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = SpanishFormatters.formatDurationReadable(durationMillis),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = SpanishFormatters.formatTimeRange12h(outage.startTimeMillis, end),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (outage.note.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EditNote,
                        contentDescription = "Nota",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = outage.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryEmptyState(
    hasAnyOutages: Boolean,
    activeOutagePresent: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (hasAnyOutages) {
                "No se encontraron apagones"
            } else if (activeOutagePresent) {
                "Tu primer apagón está en curso"
            } else {
                "Aún no hay apagones guardados"
            },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (hasAnyOutages) {
                "Prueba con otra fecha o palabra de la nota."
            } else {
                "Cuando registres un corte desde Inicio, aparecerá aquí ordenado por día."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
