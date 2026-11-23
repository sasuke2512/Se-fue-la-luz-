package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.OutageEntity
import com.example.domain.DayKey
import com.example.domain.SpanishFormatters
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutageEntrySheet(
    existingOutage: OutageEntity?,
    onDismiss: () -> Unit,
    onSave: (startMillis: Long, endMillis: Long, note: String) -> Unit,
    onDelete: ((OutageEntity) -> Unit)? = null
) {
    val now = remember { System.currentTimeMillis() }
    val initialStart = existingOutage?.startTimeMillis ?: (now - 3_600_000L)
    val initialEnd = existingOutage?.endTimeMillis ?: now

    var startMillis by remember { mutableLongStateOf(initialStart) }
    var endMillis by remember { mutableLongStateOf(initialEnd) }
    var note by remember { mutableStateOf(existingOutage?.note ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val durationMillis = (endMillis - startMillis).coerceAtLeast(0L)
    val isRangeValid = endMillis > startMillis

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (existingOutage == null) "Registrar apagón a mano" else "Detalle del apagón",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Duración calculada",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isRangeValid) {
                            SpanishFormatters.formatDurationReadable(durationMillis)
                        } else {
                            "Horario inválido"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isRangeValid) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }

            // Start Date & Time
            Text(
                text = "Inicio (cuando se fue la luz)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PickerChipButton(
                    icon = Icons.Outlined.CalendarToday,
                    text = SpanishFormatters.formatFullDate(DayKey.fromMillis(startMillis)),
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pick_start_date")
                )
                PickerChipButton(
                    icon = Icons.Outlined.AccessTime,
                    text = SpanishFormatters.formatTime12h(startMillis),
                    onClick = { showStartTimePicker = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pick_start_time")
                )
            }

            // End Date & Time
            Text(
                text = "Fin (cuando volvió la luz)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PickerChipButton(
                    icon = Icons.Outlined.CalendarToday,
                    text = SpanishFormatters.formatFullDate(DayKey.fromMillis(endMillis)),
                    onClick = { showEndDatePicker = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pick_end_date")
                )
                PickerChipButton(
                    icon = Icons.Outlined.AccessTime,
                    text = SpanishFormatters.formatTime12h(endMillis),
                    onClick = { showEndTimePicker = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pick_end_time")
                )
            }

            if (!isRangeValid) {
                Text(
                    text = "La fecha y hora de fin deben ser posteriores al inicio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Nota opcional") },
                placeholder = { Text("Ej.: Circuito, bloque, madrugada...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.EditNote,
                        contentDescription = "Nota"
                    )
                },
                maxLines = 3,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("outage_note_input")
            )

            errorMessage?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    if (endMillis <= startMillis) {
                        errorMessage = "La hora de fin debe ser posterior a la hora de inicio."
                    } else {
                        errorMessage = null
                        onSave(startMillis, endMillis, note)
                    }
                },
                enabled = isRangeValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("save_outage_button"),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    text = if (existingOutage == null) "Guardar apagón" else "Guardar cambios",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (existingOutage != null && onDelete != null) {
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("delete_outage_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Eliminar apagón")
                }
            }
        }
    }

    if (showStartDatePicker) {
        LocalDatePickerModal(
            initialMillis = startMillis,
            onDismiss = { showStartDatePicker = false },
            onDateSelected = { selectedUtcMidnight ->
                startMillis = mergeDateWithExistingTime(selectedUtcMidnight, startMillis)
                showStartDatePicker = false
                errorMessage = null
            }
        )
    }

    if (showEndDatePicker) {
        LocalDatePickerModal(
            initialMillis = endMillis,
            onDismiss = { showEndDatePicker = false },
            onDateSelected = { selectedUtcMidnight ->
                endMillis = mergeDateWithExistingTime(selectedUtcMidnight, endMillis)
                showEndDatePicker = false
                errorMessage = null
            }
        )
    }

    if (showStartTimePicker) {
        LocalTimePickerModal(
            initialMillis = startMillis,
            title = "Hora de inicio",
            onDismiss = { showStartTimePicker = false },
            onTimeSelected = { hour, minute ->
                startMillis = mergeTimeWithExistingDate(startMillis, hour, minute)
                showStartTimePicker = false
                errorMessage = null
            }
        )
    }

    if (showEndTimePicker) {
        LocalTimePickerModal(
            initialMillis = endMillis,
            title = "Hora de fin",
            onDismiss = { showEndTimePicker = false },
            onTimeSelected = { hour, minute ->
                endMillis = mergeTimeWithExistingDate(endMillis, hour, minute)
                showEndTimePicker = false
                errorMessage = null
            }
        )
    }

    if (showDeleteConfirm && existingOutage != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("¿Eliminar este apagón?") },
            text = { Text("Se borrará permanentemente de tu historial y estadísticas.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(existingOutage)
                    },
                    modifier = Modifier.testTag("confirm_delete_outage")
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun PickerChipButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocalDatePickerModal(
    initialMillis: Long,
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit
) {
    val initialUtcMidnight = remember(initialMillis) {
        localMillisToUtcMidnight(initialMillis)
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtcMidnight)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let(onDateSelected) ?: onDismiss()
                }
            ) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocalTimePickerModal(
    initialMillis: Long,
    title: String,
    onDismiss: () -> Unit,
    onTimeSelected: (Int, Int) -> Unit
) {
    val cal = remember(initialMillis) {
        Calendar.getInstance().apply { timeInMillis = initialMillis }
    }
    val timeState = rememberTimePickerState(
        initialHour = cal.get(Calendar.HOUR_OF_DAY),
        initialMinute = cal.get(Calendar.MINUTE),
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            TimePicker(state = timeState)
        },
        confirmButton = {
            TextButton(onClick = { onTimeSelected(timeState.hour, timeState.minute) }) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

private fun localMillisToUtcMidnight(localMillis: Long): Long {
    val localCal = Calendar.getInstance().apply { timeInMillis = localMillis }
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(
            localCal.get(Calendar.YEAR),
            localCal.get(Calendar.MONTH),
            localCal.get(Calendar.DAY_OF_MONTH),
            0, 0, 0
        )
        set(Calendar.MILLISECOND, 0)
    }
    return utcCal.timeInMillis
}

private fun mergeDateWithExistingTime(selectedUtcMidnight: Long, existingLocalMillis: Long): Long {
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = selectedUtcMidnight
    }
    val localCal = Calendar.getInstance().apply {
        timeInMillis = existingLocalMillis
        set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
        set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
    }
    return localCal.timeInMillis
}

private fun mergeTimeWithExistingDate(existingLocalMillis: Long, hour24: Int, minute: Int): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = existingLocalMillis
        set(Calendar.HOUR_OF_DAY, hour24)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}
