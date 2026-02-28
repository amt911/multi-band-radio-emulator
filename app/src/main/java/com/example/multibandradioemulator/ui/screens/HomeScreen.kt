package com.example.multibandradioemulator.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.multibandradioemulator.audio.RadioSignalPlayer
import com.example.multibandradioemulator.model.AntennaType
import com.example.multibandradioemulator.ui.theme.MultiBandRadioEmulatorTheme
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
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

    val player = remember { RadioSignalPlayer() }

    // Clean up player when composable leaves composition
    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }

    // Update time every second
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalDateTime.now()
            // Calculate delay to next second boundary for accuracy
            val now = System.currentTimeMillis()
            val delayMs = 1000L - (now % 1000L)
            delay(delayMs)
        }
    }

    val timeFormatter = remember {
        DateTimeFormatter.ofPattern("HH:mm:ss", Locale.getDefault())
    }
    val spanishLocale = remember { Locale.forLanguageTag("es-ES") }
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", spanishLocale)
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
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
                    .replaceFirstChar { it.titlecase(spanishLocale) },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Timezone info
            Text(
                text = "Zona horaria: ${ZoneId.systemDefault().id}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Antenna selector
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = !dropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedAntenna.formattedLabel(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Antena de señal") },
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
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = antenna.formattedLabel(),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = antenna.description,
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
                                    player.start(antenna)
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
                        player.start(selectedAntenna)
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
                    contentDescription = if (isPlaying) "Detener" else "Reproducir",
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isPlaying) "Emitiendo señal ${selectedAntenna.displayName}..." else "Pulsa para emitir",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isPlaying)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
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
