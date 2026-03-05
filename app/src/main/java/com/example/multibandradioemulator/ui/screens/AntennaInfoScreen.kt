package com.example.multibandradioemulator.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.multibandradioemulator.R
import com.example.multibandradioemulator.model.AntennaType
import com.example.multibandradioemulator.ui.theme.MultiBandRadioEmulatorTheme

@Composable
fun AntennaInfoScreen(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.nav_info),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.antenna_info_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            items(AntennaType.entries.toList()) { antenna ->
                AntennaDetailCard(antenna)
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun AntennaDetailCard(antenna: AntennaType) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val spec = getAntennaSpec(antenna)

    ElevatedCard(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = antenna.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(antenna.descriptionRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(modifier = Modifier.padding(top = 4.dp)) {
                        Text(
                            text = "${antenna.frequencyKHz} kHz",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(antenna.regionRes),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expanded details
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    SpecRow(stringResource(R.string.spec_operator), spec.operator)
                    SpecRow(stringResource(R.string.spec_location), spec.coordinates)
                    SpecRow(stringResource(R.string.spec_power), "${spec.powerKw} kW")
                    SpecRow(stringResource(R.string.spec_timezone), spec.timezone)
                    SpecRow(stringResource(R.string.spec_coverage), spec.coverage)

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    SpecRow(stringResource(R.string.spec_modulation), spec.modulationType)
                    SpecRow(stringResource(R.string.spec_am_deviation), spec.amDeviation)
                    SpecRow(
                        stringResource(R.string.spec_encoding_direction),
                        if (spec.encodesNextMinute) stringResource(R.string.spec_encodes_next)
                        else stringResource(R.string.spec_encodes_current)
                    )

                    if (spec.bitValues.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(R.string.spec_bit_values),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        for (bv in spec.bitValues) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = bv.symbol,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.width(80.dp)
                                )
                                Text(
                                    text = bv.duration,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(80.dp)
                                )
                                Text(
                                    text = bv.meaning,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (spec.timeCodeFields.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(R.string.spec_time_code),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        for (tc in spec.timeCodeFields) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = tc.seconds,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.width(56.dp)
                                )
                                Text(
                                    text = tc.field,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (spec.specialFeatures.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(R.string.spec_special),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = spec.specialFeatures,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── Antenna spec data ──────────────────────────────────────────────────

private data class AntennaSpec(
    val operator: String,
    val coordinates: String,
    val powerKw: Int,
    val timezone: String,
    val coverage: String,
    val modulationType: String,
    val amDeviation: String,
    val encodesNextMinute: Boolean,
    val bitValues: List<BitValueInfo>,
    val timeCodeFields: List<TimeCodeField>,
    val specialFeatures: String
)

private data class BitValueInfo(
    val symbol: String,
    val duration: String,
    val meaning: String
)

private data class TimeCodeField(
    val seconds: String,
    val field: String
)

private fun getAntennaSpec(antenna: AntennaType): AntennaSpec {
    return when (antenna) {
        AntennaType.DCF77 -> AntennaSpec(
            operator = "Physikalisch-Technische Bundesanstalt (PTB)",
            coordinates = "50\u00b001\u2032N, 09\u00b000\u2032E",
            powerKw = 50,
            timezone = "CET / CEST",
            coverage = "Central Europe (~2000 km)",
            modulationType = "AM (reduced-carrier-first)",
            amDeviation = "~85%",
            encodesNextMinute = true,
            bitValues = listOf(
                BitValueInfo("0", "100 ms", "Binary zero"),
                BitValueInfo("1", "200 ms", "Binary one"),
                BitValueInfo("Marker", "No reduction", "Second 59: minute marker")
            ),
            timeCodeFields = listOf(
                TimeCodeField("0", "Minute marker (always 0)"),
                TimeCodeField("1\u201314", "Civil warning / weather"),
                TimeCodeField("15\u201319", "Control (DST, timezone, leap second)"),
                TimeCodeField("20", "Start of time (always 1)"),
                TimeCodeField("21\u201328", "Minutes + parity (BCD, LSB first)"),
                TimeCodeField("29\u201335", "Hours + parity (BCD, LSB first)"),
                TimeCodeField("36\u201341", "Day of month (BCD, LSB first)"),
                TimeCodeField("42\u201344", "Day of week (1=Mon\u20267=Sun)"),
                TimeCodeField("45\u201349", "Month (BCD, LSB first)"),
                TimeCodeField("50\u201358", "Year + date parity (BCD, LSB first)"),
                TimeCodeField("59", "Minute marker (no reduction)")
            ),
            specialFeatures = "BCD encoding, LSB first. 3 even-parity groups (minutes, hours, date). " +
                    "Sub-harmonics: 12916, 15500, 19375 Hz."
        )
        AntennaType.MSF -> AntennaSpec(
            operator = "National Physical Laboratory (NPL)",
            coordinates = "54\u00b055\u2032N, 03\u00b016\u2032W",
            powerKw = 17,
            timezone = "UTC / BST",
            coverage = "UK and NW Europe",
            modulationType = "OOK (on-off keying)",
            amDeviation = "100% (carrier fully off)",
            encodesNextMinute = true,
            bitValues = listOf(
                BitValueInfo("A=0, B=0", "100 ms off", "Binary zero"),
                BitValueInfo("A=1, B=0", "200 ms off", "Binary one"),
                BitValueInfo("A=1, B=1", "300 ms off", "Parity/status"),
                BitValueInfo("Minute", "500 ms off", "Second 0: minute marker")
            ),
            timeCodeFields = listOf(
                TimeCodeField("0", "Minute marker (500 ms off)"),
                TimeCodeField("1\u201316", "DUT1 correction (unary)"),
                TimeCodeField("17\u201324", "Year (BCD, MSB first)"),
                TimeCodeField("25\u201329", "Month (BCD, MSB first)"),
                TimeCodeField("30\u201335", "Day of month (BCD, MSB first)"),
                TimeCodeField("36\u201338", "Day of week (0=Sun\u20266=Sat)"),
                TimeCodeField("39\u201344", "Hours (BCD, MSB first)"),
                TimeCodeField("45\u201351", "Minutes (BCD, MSB first)"),
                TimeCodeField("52", "Unused (always 0)"),
                TimeCodeField("53\u201358", "Parity + BST status (bit B)"),
                TimeCodeField("59", "End (100 ms off)")
            ),
            specialFeatures = "Dual bit-stream (A for time data, B for parity/status). " +
                    "4 odd-parity groups. Secondary minute marker at seconds 53\u201358. " +
                    "Sub-harmonics: 8571, 12000, 15000 Hz."
        )
        AntennaType.WWVB -> AntennaSpec(
            operator = "National Institute of Standards and Technology (NIST)",
            coordinates = "40\u00b040\u2032N, 105\u00b003\u2032W",
            powerKw = 70,
            timezone = "UTC",
            coverage = "Continental US (~3000 km)",
            modulationType = "AM (reduced-carrier-first)",
            amDeviation = "~90% (-17 dB)",
            encodesNextMinute = false,
            bitValues = listOf(
                BitValueInfo("0", "200 ms", "Binary zero"),
                BitValueInfo("1", "500 ms", "Binary one"),
                BitValueInfo("Marker", "800 ms", "Position marker")
            ),
            timeCodeFields = listOf(
                TimeCodeField("0", "Position marker (800 ms)"),
                TimeCodeField("1\u20138", "Minutes (padded-5 BCD, MSB first)"),
                TimeCodeField("9", "Position marker"),
                TimeCodeField("12\u201318", "Hours (padded-5 BCD, MSB first)"),
                TimeCodeField("19", "Position marker"),
                TimeCodeField("22\u201333", "Day of year (padded-5 BCD, MSB first)"),
                TimeCodeField("36\u201338", "DUT1 sign"),
                TimeCodeField("39", "Position marker"),
                TimeCodeField("40\u201343", "DUT1 magnitude (BCD)"),
                TimeCodeField("45\u201353", "Year (padded-5 BCD, MSB first)"),
                TimeCodeField("55", "Leap year flag"),
                TimeCodeField("56", "Leap second flag"),
                TimeCodeField("57\u201358", "DST status"),
                TimeCodeField("59", "Position marker")
            ),
            specialFeatures = "Padded-5 BCD encoding, MSB first. Position markers at seconds 0, 9, 19, 29, 39, 49, 59. " +
                    "Includes DUT1 correction, leap second, leap year, and DST status fields. " +
                    "Sub-harmonics: 8571, 12000, 15000 Hz."
        )
        AntennaType.JJY40 -> AntennaSpec(
            operator = "National Institute of Information and Communications Technology (NICT)",
            coordinates = "33\u00b028\u2032N, 130\u00b011\u2032E",
            powerKw = 50,
            timezone = "JST (UTC+9)",
            coverage = "Western Japan",
            modulationType = "AM (full-carrier-first)",
            amDeviation = "~90%",
            encodesNextMinute = false,
            bitValues = listOf(
                BitValueInfo("0", "800 ms full", "Binary zero"),
                BitValueInfo("1", "500 ms full", "Binary one"),
                BitValueInfo("Marker", "200 ms full", "Position marker")
            ),
            timeCodeFields = listOf(
                TimeCodeField("0", "Position marker"),
                TimeCodeField("1\u20138", "Minutes (padded-5 BCD, MSB first)"),
                TimeCodeField("9", "Position marker"),
                TimeCodeField("12\u201318", "Hours (padded-5 BCD, MSB first)"),
                TimeCodeField("19", "Position marker"),
                TimeCodeField("22\u201333", "Day of year (padded-5 BCD, MSB first)"),
                TimeCodeField("36", "PA1: even parity (hours)"),
                TimeCodeField("37", "PA2: even parity (minutes)"),
                TimeCodeField("39", "Position marker"),
                TimeCodeField("41\u201348", "Year (standard BCD, MSB first)"),
                TimeCodeField("49", "Position marker"),
                TimeCodeField("50\u201352", "Day of week (0=Sun\u20266=Sat)"),
                TimeCodeField("53\u201354", "Leap second flags"),
                TimeCodeField("59", "Position marker")
            ),
            specialFeatures = "Inverted pattern: second starts at full power, then drops. " +
                    "Padded-5 BCD, MSB first. 2 even-parity bits. " +
                    "Morse code call sign \"JJY\" at minutes 15 and 45. " +
                    "Sub-harmonics: 5714, 8000, 13333 Hz."
        )
        AntennaType.JJY60 -> AntennaSpec(
            operator = "National Institute of Information and Communications Technology (NICT)",
            coordinates = "37\u00b022\u2032N, 140\u00b051\u2032E",
            powerKw = 50,
            timezone = "JST (UTC+9)",
            coverage = "Eastern Japan",
            modulationType = "AM (full-carrier-first)",
            amDeviation = "~90%",
            encodesNextMinute = false,
            bitValues = listOf(
                BitValueInfo("0", "800 ms full", "Binary zero"),
                BitValueInfo("1", "500 ms full", "Binary one"),
                BitValueInfo("Marker", "200 ms full", "Position marker")
            ),
            timeCodeFields = listOf(
                TimeCodeField("0\u201359", "Identical encoding to JJY40")
            ),
            specialFeatures = "Identical encoding to JJY40. Covers eastern Japan from Fukushima. " +
                    "Two stations ensure nationwide coverage; different frequencies avoid interference. " +
                    "Sub-harmonics: 8571, 12000, 15000 Hz."
        )
        AntennaType.BPC -> AntennaSpec(
            operator = "Chinese Academy of Sciences, NTSC",
            coordinates = "34\u00b026\u2032N, 115\u00b035\u2032E",
            powerKw = 50,
            timezone = "CST (UTC+8)",
            coverage = "China and surrounding regions",
            modulationType = "AM (2-bit symbols)",
            amDeviation = "~95%",
            encodesNextMinute = false,
            bitValues = listOf(
                BitValueInfo("00", "100 ms", "Symbol 0"),
                BitValueInfo("01", "200 ms", "Symbol 1"),
                BitValueInfo("10", "300 ms", "Symbol 2"),
                BitValueInfo("11", "400 ms", "Symbol 3"),
                BitValueInfo("Ref", "No reduction", "Seconds 0, 20, 40")
            ),
            timeCodeFields = listOf(
                TimeCodeField("0", "Reference marker (full power)"),
                TimeCodeField("1", "Frame ID (00/01/10)"),
                TimeCodeField("2", "Unused"),
                TimeCodeField("3\u20134", "Hours, 12h format (binary)"),
                TimeCodeField("5\u20137", "Minutes (binary)"),
                TimeCodeField("8\u20139", "Day of week (1=Mon\u20267=Sun)"),
                TimeCodeField("10", "PM flag + P1 parity"),
                TimeCodeField("11\u201313", "Day of month (binary)"),
                TimeCodeField("14\u201315", "Month (binary)"),
                TimeCodeField("16\u201318", "Year low bits (binary)"),
                TimeCodeField("19", "Year MSB + P2 parity")
            ),
            specialFeatures = "Unique 2-bit symbol encoding (4 levels per second, 120 bits/minute). " +
                    "Binary encoding instead of BCD. 12-hour format with AM/PM flag. " +
                    "Triple redundancy: 3 identical 20-second frames per minute. " +
                    "Layout above is per 20-second frame (repeated at s0, s20, s40). " +
                    "Sub-harmonics: 11416, 13700, 17125 Hz."
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AntennaInfoScreenPreview() {
    MultiBandRadioEmulatorTheme {
        AntennaInfoScreen()
    }
}
