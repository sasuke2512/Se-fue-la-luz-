package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.DayOutageTotal
import com.example.domain.SpanishFormatters
import kotlin.math.ceil

@Composable
fun Last7DaysBarChart(
    days: List<DayOutageTotal>,
    modifier: Modifier = Modifier
) {
    if (days.isEmpty()) return

    var selectedIndex by remember(days) {
        mutableIntStateOf(days.lastIndex)
    }

    val maxHoursRaw = remember(days) {
        val maxMillis = days.maxOfOrNull { it.withoutPowerMillis } ?: 0L
        (maxMillis / 3_600_000.0).coerceAtLeast(1.0)
    }
    val maxScaleHours = remember(maxHoursRaw) {
        val rounded = ceil(maxHoursRaw).toInt().coerceAtLeast(2)
        if (rounded > 24) 24 else rounded
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryGlow = MaterialTheme.colorScheme.secondary
    val mutedBar = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    val selectedDay = days.getOrNull(selectedIndex) ?: days.last()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("seven_days_chart"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = SpanishFormatters.formatFullDate(selectedDay.dayKey),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${SpanishFormatters.formatDurationReadable(selectedDay.withoutPowerMillis)} sin luz · ${
                        SpanishFormatters.formatOutageCountPhrase(selectedDay.outageCount)
                    }",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(164.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 18.dp, bottom = 6.dp)
            ) {
                val width = size.width
                val height = size.height
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

                // Reference lines: top (max), middle (50%), bottom (0)
                listOf(0f, 0.5f, 1f).forEach { fraction ->
                    val y = height * fraction
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = if (fraction < 1f) dashEffect else null
                    )
                }

                val slotWidth = width / days.size
                val barWidth = (slotWidth * 0.46f).coerceIn(14.dp.toPx(), 34.dp.toPx())
                val corner = CornerRadius(8.dp.toPx(), 8.dp.toPx())

                days.forEachIndexed { index, item ->
                    val centerX = slotWidth * index + slotWidth / 2f
                    val left = centerX - barWidth / 2f

                    // Subtle full-height track
                    drawRoundRect(
                        color = trackColor.copy(alpha = 0.35f),
                        topLeft = Offset(left, 0f),
                        size = Size(barWidth, height),
                        cornerRadius = corner
                    )

                    val hours = (item.withoutPowerMillis / 3_600_000.0).toFloat()
                    val ratio = (hours / maxScaleHours.toFloat()).coerceIn(0f, 1f)
                    if (ratio > 0f) {
                        val barHeight = (height * ratio).coerceAtLeast(6.dp.toPx())
                        val top = height - barHeight
                        val brush = if (index == selectedIndex) {
                            Brush.verticalGradient(
                                colors = listOf(primaryGlow, primaryColor),
                                startY = top,
                                endY = height
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(mutedBar, mutedBar.copy(alpha = 0.7f)),
                                startY = top,
                                endY = height
                            )
                        }
                        drawRoundRect(
                            brush = brush,
                            topLeft = Offset(left, top),
                            size = Size(barWidth, barHeight),
                            cornerRadius = corner
                        )
                    }
                }
            }

            // Hour labels row at top of bars + invisible tap targets
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                days.forEachIndexed { index, item ->
                    val hours = item.withoutPowerMillis / 3_600_000.0
                    val valueLabel = if (item.withoutPowerMillis == 0L) {
                        ""
                    } else if (hours < 0.1) {
                        "<0.1h"
                    } else {
                        String.format(java.util.Locale.US, "%.1fh", hours)
                    }
                    val interactionSource = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { selectedIndex = index }
                            ),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        if (valueLabel.isNotEmpty()) {
                            Text(
                                text = valueLabel,
                                fontSize = 10.sp,
                                fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Medium,
                                color = if (index == selectedIndex) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            days.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                Text(
                    text = SpanishFormatters.formatChartDayLabel(item.dayKey),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedIndex = index }
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
    }
}
