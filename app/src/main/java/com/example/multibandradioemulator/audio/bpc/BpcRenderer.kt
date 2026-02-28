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

    override val amplitudeDeviation: Double = 0.95
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
        amplitudeDeviation: Double
    ): ByteArray {
        val samples00 = sampleRate / 10          // 100ms
        val samples01 = sampleRate / 5           // 200ms
        val samples10 = sampleRate * 3 / 10      // 300ms
        val samples11 = sampleRate * 2 / 5       // 400ms

        val bpcRecord = record as BpcRecord
        val bitPair = bpcRecord.bcpBitString.getBitPair(secondIndex)

        val syncPrefixSamples = if (secondIndex == 19 || secondIndex == 39 || secondIndex == 59) {
            -1  // Marker: full reduced amplitude for the whole second
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
