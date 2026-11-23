package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.DayKey
import com.example.domain.PeriodStats
import com.example.domain.SpanishFormatters
import com.example.ui.MainUiState
import com.example.ui.components.Last7DaysBarChart
import com.example.ui.theme.LocalPowerSemanticColors

@Composable
fun StatsScreen(
    uiState: MainUiState,
    bottomContentPadding: Dp,
    modifier: Modifier = Modifier
) {
    val stats = uiState.stats
    val semantic = LocalPowerSemanticColors.current

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
                .padding(top = 12.dp, bottom = bottomContentPadding + 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Estadísticas",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Period Totals: Hoy, Esta semana, Este mes
            PeriodStatsCard(
                title = "Hoy",
                icon = Icons.Outlined.CalendarToday,
                stats = stats.today,
                withoutAccent = semantic.powerOffAccent,
                withAccent = semantic.powerOnAccent,
                modifier = Modifier.testTag("stats_today_card")
            )
            PeriodStatsCard(
                title = "Esta semana",
                icon = Icons.Outlined.DateRange,
                stats = stats.thisWeek,
                withoutAccent = semantic.powerOffAccent,
                withAccent = semantic.powerOnAccent,
                modifier = Modifier.testTag("stats_week_card")
            )
            PeriodStatsCard(
                title = "Este mes",
                icon = Icons.Outlined.CalendarMonth,
                stats = stats.thisMonth,
                withoutAccent = semantic.powerOffAccent,
                withAccent = semantic.powerOnAccent,
                modifier = Modifier.testTag("stats_month_card")
            )

            Text(
                text = "El tiempo con luz se cuenta desde que empezaste a usar la app: ${
                    SpanishFormatters.formatDateTime12h(stats.effectiveTrackingStartMillis)
                }.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .testTag("stats_tracking_start_note")
            )

            // Daily Averages
            Text(
                text = "Promedio diario sin luz",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AverageTile(
                    label = "Últimos 7 días",
                    value = SpanishFormatters.formatDurationReadable(stats.avgDailyLast7DaysMillis),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stats_avg_7d")
                )
                AverageTile(
                    label = "Últimos 30 días",
                    value = SpanishFormatters.formatDurationReadable(stats.avgDailyLast30DaysMillis),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stats_avg_30d")
                )
            }

            // 7-Day Bar Chart
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BarChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Horas sin luz · últimos 7 días",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Last7DaysBarChart(days = stats.last7Days)

                    Text(
                        text = "Los apagones que cruzan la medianoche se reparten proporcionalmente entre ambos días.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Records
            Text(
                text = "Récords registrados",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            RecordCard(
                title = "Apagón más largo",
                icon = Icons.Outlined.EmojiEvents,
                accentColor = MaterialTheme.colorScheme.primary,
                primaryValue = stats.longestRecordedOutage?.let {
                    SpanishFormatters.formatDurationReadable(it.durationMillis)
                } ?: "Sin registros aún",
                subtitle = stats.longestRecordedOutage?.let { record ->
                    val dayText = SpanishFormatters.formatFullDate(
                        DayKey.fromMillis(record.outage.startTimeMillis)
                    )
                    val endMillis = record.outage.endTimeMillis ?: uiState.nowMillis
                    "$dayText · ${SpanishFormatters.formatTimeRange12h(record.outage.startTimeMillis, endMillis)}"
                } ?: "Registra apagones para ver tu récord",
                modifier = Modifier.testTag("stats_longest_record")
            )

            RecordCard(
                title = "Día con más horas sin luz",
                icon = Icons.Outlined.WarningAmber,
                accentColor = semantic.powerOffAccent,
                primaryValue = stats.worstDay?.let {
                    SpanishFormatters.formatDurationReadable(it.withoutPowerMillis)
                } ?: "Sin registros aún",
                subtitle = stats.worstDay?.let { worst ->
                    "${SpanishFormatters.formatFullDate(worst.dayKey)} · ${
                        SpanishFormatters.formatOutageCountPhrase(worst.outageCount)
                    }"
                } ?: "Aún no hay días con apagones",
                modifier = Modifier.testTag("stats_worst_day")
            )

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun PeriodStatsCard(
    title: String,
    icon: ImageVector,
    stats: PeriodStats,
    withoutAccent: Color,
    withAccent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = SpanishFormatters.formatOutageCountPhrase(stats.outageCount),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sin luz",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = SpanishFormatters.formatDurationReadable(stats.withoutPowerMillis),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = withoutAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Con luz",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = SpanishFormatters.formatDurationReadable(stats.withPowerMillis),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = withAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun AverageTile(
    label: String,
    value: String,
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
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Timeline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "por día",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RecordCard(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    primaryValue: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = accentColor.copy(alpha = 0.14f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = primaryValue,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
