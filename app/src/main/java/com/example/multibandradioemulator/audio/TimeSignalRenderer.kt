package com.example.multibandradioemulator.audio

import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.cos

/**
 * Interface for protocol-specific time signal renderers.
 * Each implementation knows how to encode time data and generate
 * amplitude-modulated PCM audio for one specific protocol.
 */
interface TimeSignalRenderer {

    /** Whether this protocol encodes the NEXT minute's time (true) or the current minute (false). */
    val encodesNextMinute: Boolean get() = false

    /** AM modulation depth (0.0 to 1.0). Higher = stronger modulation. */
    val amplitudeDeviation: Double

    /** Available carrier frequencies in Hz (sub-harmonics of the real radio frequency). */
    val carrierFrequencies: List<Int>

    /** Create a time signal record encoding the given time for this protocol. */
    fun makeTimeSignalRecord(time: ZonedDateTime): TimeSignalRecord

    /**
     * Render one second of PCM audio data (16-bit signed LE mono).
     *
     * @param record The time signal record for the current minute
     * @param secondIndex The second within the minute (0-59)
     * @param freq Carrier frequency in Hz
     * @param sampleRate Audio sample rate in Hz
     * @param signalShape Waveform shape
     * @param amplitudeDeviation AM modulation depth
     * @return Raw PCM byte array (sampleRate * 2 bytes)
     */
    fun renderSecondPcm(
        record: TimeSignalRecord,
        secondIndex: Int,
        freq: Double,
        sampleRate: Int,
        signalShape: SignalShape,
        amplitudeDeviation: Double
    ): ByteArray

    /**
     * Calculate smoothed AM envelope amplitude using cosine ramps to avoid
     * audible clicks at modulation transitions.
     *
     * @param sample Current sample index within the second
     * @param syncPrefixSamples Number of samples for the sync prefix (-1 = no modulation)
     * @param amplitudeDeviation AM modulation depth
     * @param sampleRate Audio sample rate in Hz
     * @param reducedFirst If true, second starts at reduced amplitude then rises (DCF77/WWVB/BPC).
     *                     If false, second starts at full amplitude then drops (JJY).
     * @param rampMs Duration of the cosine ramp in milliseconds
     */
    fun smoothedAmplitude(
        sample: Int,
        syncPrefixSamples: Int,
        amplitudeDeviation: Double,
        sampleRate: Int,
        reducedFirst: Boolean = true,
        rampMs: Double = 2.0
    ): Double {
        if (syncPrefixSamples < 0) return 1.0

        val rampSamples = (sampleRate * rampMs / 1000.0).toInt()
        val reducedLevel = 1.0 - amplitudeDeviation

        return if (reducedFirst) {
            // Pattern: reduced → full (DCF77, WWVB, BPC)
            when {
                sample < rampSamples -> {
                    // Ramp down from full (end of previous second) to reduced
                    val t = sample.toDouble() / rampSamples
                    val s = (1.0 - cos(PI * t)) / 2.0
                    1.0 - s * amplitudeDeviation
                }
                sample < syncPrefixSamples -> reducedLevel
                sample < syncPrefixSamples + rampSamples -> {
                    // Ramp up from reduced to full
                    val t = (sample - syncPrefixSamples).toDouble() / rampSamples
                    val s = (1.0 - cos(PI * t)) / 2.0
                    reducedLevel + s * amplitudeDeviation
                }
                else -> 1.0
            }
        } else {
            // Pattern: full → reduced (JJY)
            when {
                sample < rampSamples -> {
                    // Ramp up from reduced (end of previous second) to full
                    val t = sample.toDouble() / rampSamples
                    val s = (1.0 - cos(PI * t)) / 2.0
                    reducedLevel + s * amplitudeDeviation
                }
                sample < syncPrefixSamples -> 1.0
                sample < syncPrefixSamples + rampSamples -> {
                    // Ramp down from full to reduced
                    val t = (sample - syncPrefixSamples).toDouble() / rampSamples
                    val s = (1.0 - cos(PI * t)) / 2.0
                    1.0 - s * amplitudeDeviation
                }
                else -> reducedLevel
            }
        }
    }
}
