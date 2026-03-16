package com.example.multibandradioemulator.audio.bpc

import com.example.multibandradioemulator.audio.SignalShape
import com.example.multibandradioemulator.audio.TimeSignalRecord
import com.example.multibandradioemulator.audio.TimeSignalRenderer
import java.time.ZonedDateTime

/**
 * BPC signal renderer. Generates AM-modulated PCM audio.
 * Uses 4-level modulation with 2-bit symbols (00=100ms, 01=200ms, 10=300ms, 11=400ms).
 * Carrier frequencies are sub-harmonics of 68.5 kHz.
 */
class BpcRenderer : TimeSignalRenderer {

    override val amplitudeDeviation: Double = 0.68 // -10 dB → low ≈ 31.6%
    override val carrierFrequencies: List<Int> = listOf(11416, 13700, 17125)

    override fun makeTimeSignalRecord(time: ZonedDateTime): TimeSignalRecord {
        return BpcRecord(time)
    }

    override fun renderSecondPcm(
        record: TimeSignalRecord,
        secondIndex: Int,
        freq: Double,
        sampleRate: Int,
        signalShape: SignalShape,
        amplitudeDeviation: Double,
        sampleOffset: Long
    ): ByteArray {
        val samples00 = sampleRate / 10          // 100ms
        val samples01 = sampleRate / 5           // 200ms
        val samples10 = sampleRate * 3 / 10      // 300ms
        val samples11 = sampleRate * 2 / 5       // 400ms

        val bpcRecord = record as BpcRecord
        val bitPair = bpcRecord.bcpBitString.getBitPair(secondIndex)

        val syncPrefixSamples = if (secondIndex % 20 == 0) {
            -1  // Reference marker: full power for the whole second
        } else {
            when (bitPair) {
                0 -> samples00
                1 -> samples01
                2 -> samples10
                3 -> samples11
                else -> error("Unexpected bit pair: $bitPair")
            }
        }

        val wavBuffer = ByteArray(sampleRate * 2)

        for (sample in 0 until sampleRate) {
            val amplitude = smoothedAmplitude(sample, syncPrefixSamples, amplitudeDeviation, sampleRate)
            val sampleIndex = sampleOffset + sample
            val pcmValue = signalShape.calculate(sampleIndex, freq, amplitude, sampleRate)
            wavBuffer[sample * 2] = (pcmValue and 0xFF).toByte()
            wavBuffer[sample * 2 + 1] = ((pcmValue ushr 8) and 0xFF).toByte()
        }
        return wavBuffer
    }
}
