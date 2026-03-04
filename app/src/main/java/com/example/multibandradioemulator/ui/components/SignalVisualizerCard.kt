package com.example.multibandradioemulator.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.multibandradioemulator.R
import com.example.multibandradioemulator.audio.TimeSignalRecord
import com.example.multibandradioemulator.audio.bpc.BpcRecord
import com.example.multibandradioemulator.audio.bpc.BpcRenderer
import com.example.multibandradioemulator.audio.dcf77.Dcf77Record
import com.example.multibandradioemulator.audio.dcf77.Dcf77Renderer
import com.example.multibandradioemulator.audio.jjy.JjyRecord
import com.example.multibandradioemulator.audio.jjy.JjyRenderer
import com.example.multibandradioemulator.audio.msf.MsfRecord
import com.example.multibandradioemulator.audio.msf.MsfRenderer
import com.example.multibandradioemulator.audio.wwvb.WwvbRecord
import com.example.multibandradioemulator.audio.wwvb.WwvbRenderer
import com.example.multibandradioemulator.model.AntennaType
import kotlinx.coroutines.delay
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt

// ── Data model ──────────────────────────────────────────────────────────

/**
 * Visualization info for one second of the signal.
 */
data class SecondInfo(
    val secondIndex: Int,
    /** Carrier-off (or reduced) duration in milliseconds. -1 = no modulation. */
    val offDurationMs: Int,
    /** Whether this second is a marker (minute or position marker). */
    val isMarker: Boolean,
    /**
     * If true, reduced→full (most protocols).
     * If false, full→reduced (JJY).
     */
    val reducedFirst: Boolean = true,
    /** Amplitude deviation. 1.0 = OOK (MSF), 0.85–0.95 = AM. */
    val amplitudeDeviation: Double = 0.85,
    /** Label for this second's encoded field (e.g., "Year", "Min"). */
    val fieldLabel: String = ""
)

/**
 * Describes a labelled field range in the 60-second frame.
 */
data class FieldRange(
    val label: String,
    val startSecond: Int,
    val endSecondExclusive: Int,
    val colorIndex: Int = 0
)

// ── Main card composable ────────────────────────────────────────────────

/**
 * Material 3 card displaying signal encoding visuals:
 *  1. Current-second amplitude envelope waveform (top)
 *  2. Full-minute timeline bar chart with field labels (bottom)
 */
@Composable
fun SignalVisualizerCard(
    antennaType: AntennaType,
    currentSecond: Int,
    isPlaying: Boolean,
    time: ZonedDateTime,
    modifier: Modifier = Modifier
) {
    val secondInfos = remember(antennaType, time.minute, time.hour) {
        computeAllSeconds(antennaType, time)
    }
    val fieldRanges = remember(antennaType) { getFieldRanges(antennaType) }
    val currentInfo = secondInfos.getOrNull(currentSecond) ?: secondInfos[0]

    // Animated progress within the current second (0..1)
    var progress by remember { mutableFloatStateOf(0f) }
    var animSecond by remember { mutableIntStateOf(currentSecond) }

    // Sync animation to real clock
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                val now = System.currentTimeMillis()
                progress = (now % 1000L) / 1000f
                animSecond = currentSecond
                delay(16L) // ~60 fps
            }
        } else {
            progress = 0f
        }
    }

    val textMeasurer = rememberTextMeasurer()

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ── Header ──
            Text(
                text = stringResource(R.string.signal_encoding),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Current-second envelope waveform ──
            EnvelopeWaveform(
                secondInfo = currentInfo,
                progress = if (isPlaying) progress else -1f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Second description
            Text(
                text = formatSecondDescription(currentInfo, antennaType),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Minute timeline ──
            Text(
                text = stringResource(R.string.minute_overview),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            MinuteTimeline(
                secondInfos = secondInfos,
                fieldRanges = fieldRanges,
                currentSecond = currentSecond,
                isPlaying = isPlaying,
                textMeasurer = textMeasurer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Legend ──
            FieldLegend(antennaType = antennaType)
        }
    }
}

// ── Envelope waveform ───────────────────────────────────────────────────

/**
 * Canvas showing the amplitude envelope for one second.
 * Draws a filled area chart with smoothed transitions.
 */
@Composable
private fun EnvelopeWaveform(
    secondInfo: SecondInfo,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    // Pre-compute envelope points
    val envelope = remember(secondInfo) {
        computeEnvelope(secondInfo, NUM_ENVELOPE_POINTS)
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val topPad = 4f
        val bottomPad = 4f
        val drawH = h - topPad - bottomPad

        // Background
        drawRoundRect(
            color = surfaceColor,
            cornerRadius = CornerRadius(8f, 8f)
        )

        // Grid lines at 0%, 50%, 100% amplitude
        for (level in listOf(0f, 0.5f, 1f)) {
            val y = topPad + drawH * (1f - level)
            drawLine(
                color = outlineColor,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = if (level == 0f) 1.5f else 0.5f
            )
        }

        // Draw envelope as filled area
        val filledPath = Path().apply {
            moveTo(0f, topPad + drawH)
            for (i in envelope.indices) {
                val x = i * w / (envelope.size - 1)
                val y = topPad + drawH * (1f - envelope[i])
                lineTo(x, y)
            }
            lineTo(w, topPad + drawH)
            close()
        }

        val gradientBrush = Brush.verticalGradient(
            colors = listOf(
                primaryColor.copy(alpha = 0.6f),
                primaryContainerColor.copy(alpha = 0.2f)
            ),
            startY = topPad,
            endY = topPad + drawH
        )
        drawPath(filledPath, brush = gradientBrush)

        // Draw envelope line
        val linePath = Path().apply {
            for (i in envelope.indices) {
                val x = i * w / (envelope.size - 1)
                val y = topPad + drawH * (1f - envelope[i])
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        drawPath(linePath, color = primaryColor, style = Stroke(width = 2f))

        // Playback cursor
        if (progress in 0f..1f) {
            val cursorX = progress * w
            drawLine(
                color = tertiaryColor,
                start = Offset(cursorX, topPad),
                end = Offset(cursorX, topPad + drawH),
                strokeWidth = 2f
            )
            drawCircle(
                color = tertiaryColor,
                radius = 4f,
                center = Offset(
                    cursorX,
                    topPad + drawH * (1f - envelope[
                        (progress * (envelope.size - 1)).roundToInt().coerceIn(envelope.indices)
                    ])
                )
            )
        }
    }
}

// ── Minute timeline bar chart ───────────────────────────────────────────

/**
 * Draws 60 vertical bars representing the modulation pattern for each second.
 * Bars are coloured by encoded field and the current second is highlighted.
 */
@Composable
private fun MinuteTimeline(
    secondInfos: List<SecondInfo>,
    fieldRanges: List<FieldRange>,
    currentSecond: Int,
    isPlaying: Boolean,
    textMeasurer: TextMeasurer,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    // Field palette (8 distinct pastel-ish M3-friendly colours)
    val fieldColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
    )
    val markerColor = MaterialTheme.colorScheme.inversePrimary

    val maxOff = secondInfos.maxOf { it.offDurationMs.coerceAtLeast(0) }.toFloat()
        .coerceAtLeast(1f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val labelAreaH = 28f          // space for labels at bottom
        val numberAreaH = 16f         // space for second numbers at top
        val barAreaH = h - labelAreaH - numberAreaH
        val barGap = 1.5f
        val barWidth = (w - barGap * 59) / 60f
        val cornerR = 2f

        // Background
        drawRoundRect(
            color = surfaceColor,
            cornerRadius = CornerRadius(8f, 8f)
        )

        // Draw bars
        for (i in 0 until 60) {
            val info = secondInfos[i]
            val x = i * (barWidth + barGap)
            val barFraction = if (info.offDurationMs < 0) 0.05f
            else (info.offDurationMs / maxOff).coerceIn(0.05f, 1f)
            val barH = barAreaH * barFraction
            val barY = numberAreaH + barAreaH - barH

            // Determine colour from field range
            val fieldIdx = fieldRanges.indexOfFirst {
                i >= it.startSecond && i < it.endSecondExclusive
            }
            val barColor = when {
                info.isMarker -> markerColor
                fieldIdx >= 0 -> fieldColors[fieldRanges[fieldIdx].colorIndex % fieldColors.size]
                else -> primaryColor.copy(alpha = 0.4f)
            }

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, barY),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(cornerR, cornerR)
            )

            // Highlight current second
            if (i == currentSecond && isPlaying) {
                drawRoundRect(
                    color = tertiaryColor,
                    topLeft = Offset(x - 1f, barY - 1f),
                    size = Size(barWidth + 2f, barH + 2f),
                    cornerRadius = CornerRadius(cornerR + 1f, cornerR + 1f),
                    style = Stroke(width = 2f)
                )
            }
        }

        // Draw second numbers every 10 seconds
        for (s in listOf(0, 10, 20, 30, 40, 50, 59)) {
            val x = s * (barWidth + barGap)
            val label = s.toString()
            val measured = textMeasurer.measure(
                label,
                style = TextStyle(fontSize = 8.sp, textAlign = TextAlign.Center)
            )
            drawText(
                measured,
                color = onSurfaceVariantColor,
                topLeft = Offset(
                    x + barWidth / 2 - measured.size.width / 2,
                    numberAreaH - measured.size.height - 1f
                )
            )
        }

        // Draw field range labels at the bottom
        for (range in fieldRanges) {
            if (range.label.isEmpty()) continue
            val xStart = range.startSecond * (barWidth + barGap)
            val xEnd = (range.endSecondExclusive - 1) * (barWidth + barGap) + barWidth
            val xCenter = (xStart + xEnd) / 2
            val fieldColor = fieldColors[range.colorIndex % fieldColors.size]

            // Small line under the bars
            drawLine(
                color = fieldColor,
                start = Offset(xStart, numberAreaH + barAreaH + 3f),
                end = Offset(xEnd, numberAreaH + barAreaH + 3f),
                strokeWidth = 2f
            )

            val measured = textMeasurer.measure(
                range.label,
                style = TextStyle(fontSize = 7.sp, textAlign = TextAlign.Center)
            )

            // Clip label to available space
            val availableWidth = xEnd - xStart
            if (measured.size.width <= availableWidth + 8) {
                drawText(
                    measured,
                    color = onSurfaceVariantColor,
                    topLeft = Offset(
                        xCenter - measured.size.width / 2,
                        numberAreaH + barAreaH + 6f
                    )
                )
            }
        }
    }
}

// ── Legend row ───────────────────────────────────────────────────────────

@Composable
private fun FieldLegend(antennaType: AntennaType) {
    val markerColor = MaterialTheme.colorScheme.inversePrimary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(color = markerColor, label = stringResource(R.string.legend_marker))
        LegendItem(color = tertiaryColor, label = stringResource(R.string.legend_current), outlined = true)

        when (antennaType) {
            AntennaType.MSF -> {
                Text(
                    text = "OOK",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AntennaType.BPC -> {
                Text(
                    text = "2-bit",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                Text(
                    text = "AM",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    outlined: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Canvas(modifier = Modifier.size(10.dp)) {
            if (outlined) {
                drawRoundRect(
                    color = color,
                    cornerRadius = CornerRadius(2f, 2f),
                    style = Stroke(width = 2f)
                )
            } else {
                drawRoundRect(
                    color = color,
                    cornerRadius = CornerRadius(2f, 2f),
                    style = Fill
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Envelope computation ────────────────────────────────────────────────

private const val NUM_ENVELOPE_POINTS = 500

/**
 * Compute the amplitude envelope (0..1) for one second.
 * Mirrors the smoothedAmplitude() logic from TimeSignalRenderer.
 */
private fun computeEnvelope(info: SecondInfo, numPoints: Int): FloatArray {
    val envelope = FloatArray(numPoints)
    if (info.offDurationMs < 0) {
        // No modulation — full carrier
        envelope.fill(1f)
        return envelope
    }

    val syncPrefixSamples = numPoints * info.offDurationMs / 1000
    val rampMs = 2.0
    val rampSamples = (numPoints * rampMs / 1000.0).toInt().coerceAtLeast(1)
    val deviation = info.amplitudeDeviation
    val reducedLevel = 1.0 - deviation

    for (i in 0 until numPoints) {
        envelope[i] = if (info.reducedFirst) {
            // Pattern: reduced → full (DCF77, WWVB, BPC, MSF)
            when {
                i < rampSamples -> {
                    val t = i.toDouble() / rampSamples
                    val s = (1.0 - cos(PI * t)) / 2.0
                    (1.0 - s * deviation).toFloat()
                }
                i < syncPrefixSamples -> reducedLevel.toFloat()
                i < syncPrefixSamples + rampSamples -> {
                    val t = (i - syncPrefixSamples).toDouble() / rampSamples
                    val s = (1.0 - cos(PI * t)) / 2.0
                    (reducedLevel + s * deviation).toFloat()
                }
                else -> 1f
            }
        } else {
            // Pattern: full → reduced (JJY)
            when {
                i < rampSamples -> {
                    val t = i.toDouble() / rampSamples
                    val s = (1.0 - cos(PI * t)) / 2.0
                    (reducedLevel + s * deviation).toFloat()
                }
                i < syncPrefixSamples -> 1f
                i < syncPrefixSamples + rampSamples -> {
                    val t = (i - syncPrefixSamples).toDouble() / rampSamples
                    val s = (1.0 - cos(PI * t)) / 2.0
                    (1.0 - s * deviation).toFloat()
                }
                else -> reducedLevel.toFloat()
            }
        }
    }
    return envelope
}

// ── Per-protocol second computation ─────────────────────────────────────

/**
 * Compute SecondInfo for all 60 seconds based on the antenna and encoded time.
 */
private fun computeAllSeconds(
    antennaType: AntennaType,
    time: ZonedDateTime
): List<SecondInfo> {
    return when (antennaType) {
        AntennaType.DCF77 -> computeDcf77Seconds(time)
        AntennaType.MSF -> computeMsfSeconds(time)
        AntennaType.WWVB -> computeWwvbSeconds(time)
        AntennaType.JJY40, AntennaType.JJY60 -> computeJjySeconds(time)
        AntennaType.BPC -> computeBpcSeconds(time)
    }
}

private fun computeDcf77Seconds(time: ZonedDateTime): List<SecondInfo> {
    val recordTime = time.plusMinutes(1).withSecond(0).withNano(0)
    val record = Dcf77Record(recordTime)
    val data = record.getBitString(false)
    val fields = getDcf77FieldMap()

    return List(60) { s ->
        val bitState = ((data ushr s) and 1L) != 0L
        val offMs = when {
            s == 59 -> -1  // No modulation (minute marker gap)
            bitState -> 200
            else -> 100
        }
        SecondInfo(
            secondIndex = s,
            offDurationMs = offMs,
            isMarker = s == 59,
            reducedFirst = true,
            amplitudeDeviation = 0.85,
            fieldLabel = fields[s] ?: ""
        )
    }
}

private fun computeMsfSeconds(time: ZonedDateTime): List<SecondInfo> {
    val record = MsfRecord(time.withSecond(0).withNano(0))
    val data = record.msfBits

    return List(60) { s ->
        val bitState = ((data ushr s) and 1L) != 0L
        val offMs = when {
            s == 0 -> 500  // Minute marker
            s in 53..58 -> if (bitState) 300 else 200  // Secondary marker +100ms
            else -> if (bitState) 200 else 100
        }
        SecondInfo(
            secondIndex = s,
            offDurationMs = offMs,
            isMarker = s == 0,
            reducedFirst = true,
            amplitudeDeviation = 1.0,  // OOK
            fieldLabel = ""
        )
    }
}

private fun computeWwvbSeconds(time: ZonedDateTime): List<SecondInfo> {
    val record = WwvbRecord(time.withSecond(0).withNano(0))
    val data = record.getBitString(false)

    return List(60) { s ->
        val bitState = ((data ushr s) and 1L) != 0L
        val isPositionMarker = s == 0 || s % 10 == 9
        val offMs = when {
            isPositionMarker -> 800
            bitState -> 500
            else -> 200
        }
        SecondInfo(
            secondIndex = s,
            offDurationMs = offMs,
            isMarker = isPositionMarker,
            reducedFirst = true,
            amplitudeDeviation = 0.90,
            fieldLabel = ""
        )
    }
}

private fun computeJjySeconds(time: ZonedDateTime): List<SecondInfo> {
    val record = JjyRecord.create(time.withSecond(0).withNano(0))
    val data = record.getBitString(false)

    return List(60) { s ->
        val bitState = ((data ushr s) and 1L) != 0L
        val isPositionMarker = s == 0 || s % 10 == 9
        // JJY: marker = 200ms full then reduced; bit1 = 500ms; bit0 = 800ms
        val offMs = when {
            isPositionMarker -> 200
            bitState -> 500
            else -> 800
        }
        SecondInfo(
            secondIndex = s,
            offDurationMs = offMs,
            isMarker = isPositionMarker,
            reducedFirst = false,  // JJY: full → reduced
            amplitudeDeviation = 0.90,
            fieldLabel = ""
        )
    }
}

private fun computeBpcSeconds(time: ZonedDateTime): List<SecondInfo> {
    val record = BpcRecord(time.withSecond(0).withNano(0))
    val bpc = record.bcpBitString

    return List(60) { s ->
        val isRefMarker = s % 20 == 0
        val offMs = if (isRefMarker) {
            -1  // Full power reference marker
        } else {
            when (bpc.getBitPair(s)) {
                0 -> 100
                1 -> 200
                2 -> 300
                3 -> 400
                else -> 100
            }
        }
        SecondInfo(
            secondIndex = s,
            offDurationMs = offMs,
            isMarker = isRefMarker,
            reducedFirst = true,
            amplitudeDeviation = 0.95,
            fieldLabel = ""
        )
    }
}

// ── Field ranges for each protocol ──────────────────────────────────────

private fun getFieldRanges(antennaType: AntennaType): List<FieldRange> {
    return when (antennaType) {
        AntennaType.DCF77 -> listOf(
            FieldRange("M", 0, 1, 0),           // Minute marker (second 0 = always 0)
            FieldRange("Meteo", 1, 15, 1),       // Civil warning / weather
            FieldRange("Ctrl", 15, 20, 2),       // Call bit, TZ, leap
            FieldRange("S", 20, 21, 3),          // Start bit (always 1)
            FieldRange("Min", 21, 29, 0),        // Minutes + parity
            FieldRange("Hour", 29, 36, 1),       // Hours + parity
            FieldRange("Day", 36, 42, 2),        // Day of month
            FieldRange("DoW", 42, 45, 3),        // Day of week
            FieldRange("Mon", 45, 50, 4),        // Month
            FieldRange("Year", 50, 59, 5),       // Year + date parity
            FieldRange("M", 59, 60, 0)           // Minute marker (no modulation)
        )
        AntennaType.MSF -> listOf(
            FieldRange("M", 0, 1, 0),
            FieldRange("DUT1", 1, 17, 1),
            FieldRange("Year", 17, 25, 2),
            FieldRange("Mon", 25, 30, 3),
            FieldRange("Day", 30, 36, 4),
            FieldRange("DoW", 36, 39, 5),
            FieldRange("Hour", 39, 45, 6),
            FieldRange("Min", 45, 52, 0),
            FieldRange("P", 53, 59, 7),          // Parity + BST
            FieldRange("", 59, 60, 1)
        )
        AntennaType.WWVB -> listOf(
            FieldRange("M", 0, 1, 0),
            FieldRange("Min", 1, 9, 1),
            FieldRange("M", 9, 10, 0),
            FieldRange("Hour", 12, 19, 2),
            FieldRange("M", 19, 20, 0),
            FieldRange("DaY", 22, 29, 3),
            FieldRange("M", 29, 30, 0),
            FieldRange("DaY", 30, 34, 4),
            FieldRange("DUT", 36, 44, 5),
            FieldRange("M", 39, 40, 0),
            FieldRange("Year", 45, 54, 6),
            FieldRange("M", 49, 50, 0),
            FieldRange("DST", 55, 59, 7),
            FieldRange("M", 59, 60, 0)
        )
        AntennaType.JJY40, AntennaType.JJY60 -> listOf(
            FieldRange("M", 0, 1, 0),
            FieldRange("Min", 1, 9, 1),
            FieldRange("M", 9, 10, 0),
            FieldRange("Hour", 12, 19, 2),
            FieldRange("M", 19, 20, 0),
            FieldRange("DaY", 22, 29, 3),
            FieldRange("M", 29, 30, 0),
            FieldRange("DaY", 30, 34, 4),
            FieldRange("P", 36, 38, 5),
            FieldRange("M", 39, 40, 0),
            FieldRange("Year", 41, 49, 6),
            FieldRange("M", 49, 50, 0),
            FieldRange("DoW", 50, 53, 7),
            FieldRange("LS", 53, 55, 1),
            FieldRange("M", 59, 60, 0)
        )
        AntennaType.BPC -> listOf(
            FieldRange("Ref", 0, 1, 0),
            FieldRange("F1", 1, 20, 1),
            FieldRange("Ref", 20, 21, 0),
            FieldRange("F2", 21, 40, 2),
            FieldRange("Ref", 40, 41, 0),
            FieldRange("F3", 41, 60, 3)
        )
    }
}

private fun getDcf77FieldMap(): Map<Int, String> {
    return buildMap {
        put(0, "M")
        for (i in 1..14) put(i, "W")  // weather
        put(15, "C")
        for (i in 16..19) put(i, "S")
        put(20, "1")
        for (i in 21..28) put(i, "Min")
        for (i in 29..35) put(i, "H")
        for (i in 36..41) put(i, "D")
        for (i in 42..44) put(i, "DW")
        for (i in 45..49) put(i, "Mo")
        for (i in 50..58) put(i, "Y")
        put(59, "M")
    }
}

// ── Description text ────────────────────────────────────────────────────

private fun formatSecondDescription(info: SecondInfo, antennaType: AntennaType): String {
    val secondStr = "s${info.secondIndex}"
    return when {
        info.offDurationMs < 0 -> when {
            info.isMarker -> "$secondStr — Marker (full carrier)"
            else -> "$secondStr — Reference (full carrier)"
        }
        info.isMarker -> "$secondStr — Marker (${info.offDurationMs}ms)"
        else -> {
            val bitDesc = when (antennaType) {
                AntennaType.BPC -> {
                    val symbol = info.offDurationMs / 100
                    "symbol $symbol"
                }
                else -> {
                    val bitVal = when {
                        antennaType == AntennaType.MSF && info.secondIndex in 53..58 ->
                            if (info.offDurationMs >= 300) "1" else "0"
                        antennaType == AntennaType.DCF77 ->
                            if (info.offDurationMs >= 200) "1" else "0"
                        antennaType == AntennaType.WWVB ->
                            if (info.offDurationMs >= 500) "1" else "0"
                        antennaType in listOf(AntennaType.JJY40, AntennaType.JJY60) ->
                            if (info.offDurationMs <= 500) "1" else "0"
                        else ->
                            if (info.offDurationMs >= 200) "1" else "0"
                    }
                    "bit=$bitVal"
                }
            }
            "$secondStr — $bitDesc (${info.offDurationMs}ms)"
        }
    }
}
