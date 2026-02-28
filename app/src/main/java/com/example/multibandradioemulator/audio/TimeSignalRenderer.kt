package com.example.multibandradioemulator.audio

import java.time.ZonedDateTime

/**
 * Interface for protocol-specific time signal renderers.
 * Each implementation knows how to encode time data and generate
 * amplitude-modulated PCM audio for one specific protocol.
 */
interface TimeSignalRenderer {

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
}
