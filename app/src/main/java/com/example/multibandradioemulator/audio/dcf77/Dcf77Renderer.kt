package com.example.multibandradioemulator.audio.dcf77

import com.example.multibandradioemulator.audio.SignalShape
import com.example.multibandradioemulator.audio.TimeSignalRecord
import com.example.multibandradioemulator.audio.TimeSignalRenderer
import java.time.ZonedDateTime

/**
 * DCF77 signal renderer. Generates AM-modulated PCM audio.
 * Carrier frequencies are sub-harmonics of 77.5 kHz (e.g. 77500/5 = 15500 Hz).
 */
class Dcf77Renderer : TimeSignalRenderer {

    override val amplitudeDeviation: Double = 0.85
    override val carrierFrequencies: List<Int> = listOf(12916, 15500, 19375)

    override fun makeTimeSignalRecord(time: ZonedDateTime): TimeSignalRecord {
        return Dcf77Record(time)
    }

    override fun renderSecondPcm(
        record: TimeSignalRecord,
        secondIndex: Int,
        freq: Double,
        sampleRate: Int,
        signalShape: SignalShape,
        amplitudeDeviation: Double
    ): ByteArray {
        val samplesPerSet = sampleRate / 5      // 200ms for bit=1
        val samplesPerReset = sampleRate / 10   // 100ms for bit=0

        val data = record.getBitString(false)
        val bitState = ((data ushr secondIndex) and 1L) != 0L

        val syncPrefixSamples = if (secondIndex == 59) {
            -1  // No reduction for second 59 (minute marker)
        } else {
            if (bitState) samplesPerSet else samplesPerReset
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
