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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.data.local.AppThemeMode
import com.example.domain.SpanishFormatters
import com.example.ui.MainUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: MainUiState,
    bottomContentPadding: Dp,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onExportJsonClick: () -> Unit,
    onImportJsonClick: () -> Unit,
    onDeleteAllConfirmed: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    var deleteConfirmStep by remember { mutableIntStateOf(0) }
    val appVersion = BuildConfig.VERSION_NAME
    val trackingStartFormatted = remember(uiState.stats.effectiveTrackingStartMillis) {
        SpanishFormatters.formatDateTime12h(uiState.stats.effectiveTrackingStartMillis)
    }

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Ajustes e Info",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Theme Card
            SettingsSectionCard(
                title = "Tema de la aplicación",
                icon = Icons.Outlined.Palette
            ) {
                val options = listOf(
                    AppThemeMode.SYSTEM to "Sistema",
                    AppThemeMode.LIGHT to "Claro",
                    AppThemeMode.DARK to "Oscuro"
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, (mode, label) ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                            onClick = { onThemeModeChange(mode) },
                            selected = uiState.themeMode == mode,
                            modifier = Modifier.testTag("theme_option_${mode.name.lowercase()}")
                        ) {
                            Text(label, maxLines = 1)
                        }
                    }
                }
            }

            // Clipboard Summaries
            SettingsSectionCard(
                title = "Resúmenes para compartir",
                icon = Icons.Outlined.ContentCopy
            ) {
                OutlinedButton(
                    onClick = {
                        val text = SpanishFormatters.buildTodayClipboardSummary(
                            nowMillis = uiState.nowMillis,
                            todayStats = uiState.stats.today
                        )
                        clipboardManager.setText(AnnotatedString(text))
                        onShowMessage("Resumen de hoy copiado al portapapeles")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("copy_today_summary"),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copiar resumen de hoy")
                }

                OutlinedButton(
                    onClick = {
                        val text = SpanishFormatters.buildMonthClipboardSummary(
                            nowMillis = uiState.nowMillis,
                            monthStats = uiState.stats.thisMonth
                        )
                        clipboardManager.setText(AnnotatedString(text))
                        onShowMessage("Resumen del mes copiado al portapapeles")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("copy_month_summary"),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copiar resumen del mes")
                }
            }

            // JSON Export / Import
            SettingsSectionCard(
                title = "Respaldo local (JSON)",
                icon = Icons.Outlined.Download
            ) {
                Text(
                    text = "Guarda una copia de tu historial en tu teléfono o restaura un respaldo sin duplicar registros existentes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onExportJsonClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("export_json_button"),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Upload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exportar", maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = onImportJsonClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("import_json_button"),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Importar", maxLines = 1)
                    }
                }
            }

            // Danger Zone: Delete all data
            SettingsSectionCard(
                title = "Gestión de datos",
                icon = Icons.Outlined.DeleteForever
            ) {
                OutlinedButton(
                    onClick = { deleteConfirmStep = 1 },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("delete_all_data_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteForever,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Borrar todos los datos")
                }
            }

            // Info Section
            SettingsSectionCard(
                title = "Información",
                icon = Icons.Outlined.Info
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Versión $appVersion",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("info_version_text")
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("info_tracking_since_row"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Seguimiento desde:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = trackingStartFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Todos los datos se guardan exclusivamente en tu teléfono. Esta aplicación funciona 100% sin conexión a internet, sin cuentas, sin anuncios y sin rastreadores.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Unified Footer + Legal Closing Block (Plus Messenger / Telegram style)
            ClosingLegalBlock(
                appVersion = appVersion,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp)
                    .testTag("legal_disclaimer_block")
            )
        }
    }

    if (deleteConfirmStep == 1) {
        AlertDialog(
            onDismissRequest = { deleteConfirmStep = 0 },
            title = { Text("¿Borrar todos los datos?") },
            text = {
                Text("Se borrarán todos los apagones registrados y el conteo de tiempo empezará de nuevo desde ahora.")
            },
            confirmButton = {
                TextButton(
                    onClick = { deleteConfirmStep = 2 },
                    modifier = Modifier.testTag("delete_all_step1_confirm")
                ) {
                    Text("Continuar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmStep = 0 }) {
                    Text("Cancelar")
                }
            }
        )
    } else if (deleteConfirmStep == 2) {
        AlertDialog(
            onDismissRequest = { deleteConfirmStep = 0 },
            title = { Text("Confirmación definitiva") },
            text = {
                Text("Esta acción no se puede deshacer. Se borrarán todos los apagones registrados y el conteo de tiempo empezará de nuevo desde ahora.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteConfirmStep = 0
                        onDeleteAllConfirmed()
                    },
                    modifier = Modifier.testTag("delete_all_step2_confirm")
                ) {
                    Text("Sí, borrar todo", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmStep = 0 }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
            content()
        }
    }
}

@Composable
private fun ClosingLegalBlock(
    appVersion: String,
    modifier: Modifier = Modifier
) {
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val linkColor = MaterialTheme.colorScheme.primary

    val versionLineAnnotated = remember(appVersion) {
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append("Se fue la luz!")
            }
            append(" · Versión $appVersion")
        }
    }

    val createdByAnnotated = remember(linkColor) {
        buildAnnotatedString {
            append("Created by ")
            withLink(
                LinkAnnotation.Url(
                    url = "https://x.com/saske2512",
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            color = linkColor,
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.None
                        )
                    )
                )
            ) {
                append("SaSuKe2512")
            }
        }
    }

    val legalTextStyle = MaterialTheme.typography.bodySmall.copy(
        fontSize = 12.sp,
        lineHeight = 17.sp,
        color = mutedColor
    )

    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = versionLineAnnotated,
            style = legalTextStyle,
            textAlign = TextAlign.Center
        )
        Text(
            text = "© 2026 Se fue la luz! Todos los derechos reservados.",
            style = legalTextStyle,
            textAlign = TextAlign.Center
        )
        Text(
            text = createdByAnnotated,
            style = legalTextStyle,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "UNE y Unión Eléctrica son marcas y nombres comerciales propiedad de la Unión Eléctrica de Cuba (UNE). Todos los derechos sobre esas marcas pertenecen a su propietario.",
            style = legalTextStyle,
            textAlign = TextAlign.Center
        )
        Text(
            text = "«Se fue la luz!» es una aplicación personal, creada con fines informativos, para que cada usuario lleve su propio registro de los apagones que anota a mano. No está afiliada, respaldada, patrocinada ni aprobada por la Unión Eléctrica de Cuba (UNE) ni por ninguna de sus empresas, y ningún dato de la app proviene de la UNE: todos los registros los introduce el propio usuario en su teléfono.",
            style = legalTextStyle,
            textAlign = TextAlign.Center
        )
    }
}
