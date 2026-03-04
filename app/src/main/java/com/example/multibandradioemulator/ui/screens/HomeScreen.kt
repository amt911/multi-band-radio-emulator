package com.example.multibandradioemulator.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.multibandradioemulator.R
import com.example.multibandradioemulator.audio.RadioSignalPlayer
import com.example.multibandradioemulator.model.AntennaType
import com.example.multibandradioemulator.ui.components.SignalVisualizerCard
import com.example.multibandradioemulator.ui.theme.MultiBandRadioEmulatorTheme
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }
    var selectedAntenna by remember { mutableStateOf(AntennaType.DCF77) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }

    // Custom time state
    var useCustomTime by remember { mutableStateOf(false) }
    var customBaseTime by remember { mutableStateOf<ZonedDateTime?>(null) }
    var customSetAtMillis by remember { mutableLongStateOf(0L) }

    // Date/time picker dialog states
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()

    val player = remember { RadioSignalPlayer() }

    // Helper to compute the current custom ZonedDateTime (adjusted for elapsed time)
    fun getCurrentCustomZonedTime(): ZonedDateTime? {
        if (!useCustomTime || customBaseTime == null) return null
        val elapsed = System.currentTimeMillis() - customSetAtMillis
        return customBaseTime!!.plus(elapsed, ChronoUnit.MILLIS)
    }

    // Clean up player when composable leaves composition
    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }

    // Update time every second
    LaunchedEffect(useCustomTime, customBaseTime, customSetAtMillis) {
        while (true) {
            currentTime = if (useCustomTime && customBaseTime != null) {
                val elapsed = System.currentTimeMillis() - customSetAtMillis
                customBaseTime!!.plus(elapsed, ChronoUnit.MILLIS).toLocalDateTime()
            } else {
                LocalDateTime.now()
            }
            val now = System.currentTimeMillis()
            val delayMs = 1000L - (now % 1000L)
            delay(delayMs)
        }
    }

    val deviceLocale = remember { Locale.getDefault() }
    val timeFormatter = remember {
        DateTimeFormatter.ofPattern("HH:mm:ss", deviceLocale)
    }
    val dateFormatter = remember {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(deviceLocale)
    }

    // Resolve antenna strings for selected antenna
    val selectedRegion = stringResource(selectedAntenna.regionRes)
    val selectedLabel = "${selectedAntenna.displayName} — $selectedRegion (${selectedAntenna.frequencyKHz} kHz)"

    // Date picker dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    showTimePicker = true
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time picker dialog
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showTimePicker = false

                    val selectedDate = datePickerState.selectedDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate()
                    } ?: currentTime.toLocalDate()

                    val selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    val selectedDateTime = ZonedDateTime.of(
                        selectedDate, selectedTime, ZoneId.systemDefault()
                    )

                    customBaseTime = selectedDateTime
                    customSetAtMillis = System.currentTimeMillis()
                    useCustomTime = true

                    // If playing, restart with the new custom time
                    if (isPlaying) {
                        player.stop()
                        player.start(selectedAntenna, selectedDateTime)
                        // isPlaying remains true
                    }
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = { Text(stringResource(R.string.select_time)) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Time display
            Text(
                text = currentTime.format(timeFormatter),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Light
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.animateContentSize()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Date display
            Text(
                text = currentTime.format(dateFormatter)
                    .replaceFirstChar { it.titlecase(deviceLocale) },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Timezone info or custom time indicator
            if (useCustomTime) {
                Text(
                    text = stringResource(R.string.using_custom_time),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = stringResource(R.string.timezone_label, ZoneId.systemDefault().id),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Custom time controls
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { showDatePicker = true },
                    label = {
                        Text(
                            if (useCustomTime) stringResource(R.string.change_time)
                            else stringResource(R.string.set_custom_time)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.EditCalendar,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize)
                        )
                    }
                )

                if (useCustomTime) {
                    Spacer(modifier = Modifier.width(8.dp))
                    AssistChip(
                        onClick = {
                            useCustomTime = false
                            customBaseTime = null

                            // If playing, restart with phone time
                            if (isPlaying) {
                                player.stop()
                                player.start(selectedAntenna)
                            }
                        },
                        label = { Text(stringResource(R.string.reset_to_phone_time)) },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.PhoneAndroid,
                                contentDescription = null,
                                modifier = Modifier.size(AssistChipDefaults.IconSize)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Antenna selector
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = !dropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.antenna_label)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                    },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    AntennaType.entries.forEach { antenna ->
                        val region = stringResource(antenna.regionRes)
                        val label = "${antenna.displayName} — $region (${antenna.frequencyKHz} kHz)"
                        val description = stringResource(antenna.descriptionRes)

                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                val wasPlaying = isPlaying
                                if (wasPlaying) {
                                    player.stop()
                                    isPlaying = false
                                }
                                selectedAntenna = antenna
                                dropdownExpanded = false
                                if (wasPlaying) {
                                    player.start(antenna, getCurrentCustomZonedTime())
                                    isPlaying = true
                                }
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Play/Stop button
            FilledIconButton(
                onClick = {
                    if (isPlaying) {
                        player.stop()
                        isPlaying = false
                    } else {
                        player.start(selectedAntenna, getCurrentCustomZonedTime())
                        isPlaying = true
                    }
                },
                modifier = Modifier.size(72.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isPlaying)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) stringResource(R.string.btn_stop) else stringResource(R.string.btn_play),
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isPlaying) stringResource(R.string.emitting_signal, selectedAntenna.displayName) else stringResource(R.string.tap_to_emit),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isPlaying)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Signal encoding visualizer
            SignalVisualizerCard(
                antennaType = selectedAntenna,
                currentSecond = currentTime.second,
                isPlaying = isPlaying,
                time = ZonedDateTime.of(currentTime, ZoneId.systemDefault())
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    MultiBandRadioEmulatorTheme {
        HomeScreen()
    }
}
