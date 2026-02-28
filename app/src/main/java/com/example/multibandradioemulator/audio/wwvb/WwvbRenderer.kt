package com.example.multibandradioemulator.audio.wwvb

import com.example.multibandradioemulator.audio.SignalShape
import com.example.multibandradioemulator.audio.TimeSignalRecord
import com.example.multibandradioemulator.audio.TimeSignalRenderer
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * WWVB signal renderer. Generates AM-modulated PCM audio.
 * Carrier frequencies are sub-harmonics of 60 kHz.
 */
class WwvbRenderer : TimeSignalRenderer {

    override val amplitudeDeviation: Double = 0.90
    override val carrierFrequencies: List<Int> = listOf(8571, 12000, 15000)

    override fun makeTimeSignalRecord(time: ZonedDateTime): TimeSignalRecord {
        return WwvbRecord(time)
    }

    override fun renderSecondPcm(
        record: TimeSignalRecord,
        secondIndex: Int,
        freq: Double,
        sampleRate: Int,
        signalShape: SignalShape,
        amplitudeDeviation: Double
    ): ByteArray {
        val samplesPerMarker = (sampleRate shl 2) / 5  // 800ms
        val samplesPerSetBit = sampleRate / 2           // 500ms
        val samplesPerResetBit = sampleRate / 5         // 200ms

        val data = record.getBitString(false)
        val bitState = ((data ushr secondIndex) and 1L) != 0L

        val syncPrefixSamples = if (secondIndex == 0 || secondIndex % 10 == 9) {
            samplesPerMarker
        } else {
            if (bitState) samplesPerSetBit else samplesPerResetBit
        }

        val wavBuffer = ByteArray(sampleRate * 2)
        val baseOffset = secondIndex * sampleRate

        for (sample in 0 until sampleRate) {
            val amplitude = if (sample <= syncPrefixSamples) {
                1.0 - amplitudeDeviation
            } else {
                1.0
            }
            val sampleIndex = baseOffset + sample
            val volume = signalShape.calculate(sampleIndex, freq, amplitude, sampleRate)
            wavBuffer[sample * 2] = (volume and 0xFF).toByte()
            wavBuffer[sample * 2 + 1] = ((volume ushr 8) and 0xFF).toByte()
        }
        return wavBuffer
    }
}
