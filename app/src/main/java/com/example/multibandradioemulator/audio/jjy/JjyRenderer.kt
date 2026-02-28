package com.example.multibandradioemulator.audio.jjy

import com.example.multibandradioemulator.audio.SignalShape
import com.example.multibandradioemulator.audio.TimeSignalRecord
import com.example.multibandradioemulator.audio.TimeSignalRenderer
import java.time.ZonedDateTime

/**
 * JJY signal renderer. Generates AM-modulated PCM audio.
 * Carrier frequencies are sub-harmonics of 40 kHz.
 * Includes Morse code "JJY" call sign at minutes 15 and 45.
 */
class JjyRenderer : TimeSignalRenderer {

    override val amplitudeDeviation: Double = 0.90
    override val carrierFrequencies: List<Int> = listOf(4100, 13333, 15900)

    override fun makeTimeSignalRecord(time: ZonedDateTime): TimeSignalRecord {
        return JjyRecord.create(time)
    }

    override fun renderSecondPcm(
        record: TimeSignalRecord,
        secondIndex: Int,
        freq: Double,
        sampleRate: Int,
        signalShape: SignalShape,
        amplitudeDeviation: Double
    ): ByteArray {
        val samplesPerMarker = sampleRate / 5       // 200ms
        val samplesPerSet = sampleRate / 2           // 500ms
        val samplesPerReset = (sampleRate shl 2) / 5 // 800ms

        val jjyRecord = record as JjyRecord
        val data = record.getBitString(false)
        val bitState = ((data ushr secondIndex) and 1L) != 0L

        val syncPrefixSamples = if (secondIndex == 0 || secondIndex % 10 == 9) {
            samplesPerMarker
        } else {
            if (bitState) samplesPerSet else samplesPerReset
        }

        val wavBuffer = ByteArray(sampleRate * 2)
        val baseOffset = secondIndex * sampleRate

        // JJY call sign Morse code for seconds 40-48 during call sign announcement minutes
        if (jjyRecord.isCallSignAnnouncement && secondIndex in 40..48) {
            val totalSignalSamples = 9 * sampleRate
            val perBit = MORSE_JJY_MSB0.length.toDouble() / totalSignalSamples
            var sampleOffset = (secondIndex - 40) * sampleRate

            for (sample in 0 until sampleRate) {
                val morseBitIndex =
                    minOf(MORSE_JJY_MSB0.length - 1, Math.round(perBit * sampleOffset).toInt())
                val amplitude = if (MORSE_JJY_MSB0[morseBitIndex] == '0') 0.0 else 1.0
                val sampleIndex = baseOffset + sample
                val volume = signalShape.calculate(sampleIndex, freq, amplitude, sampleRate)
                wavBuffer[sample * 2] = (volume and 0xFF).toByte()
                wavBuffer[sample * 2 + 1] = ((volume ushr 8) and 0xFF).toByte()
                sampleOffset++
            }
        } else {
            // Normal AM modulation (JJY inverts: full power first, then reduced)
            for (sample in 0 until sampleRate) {
                val amplitude = if (sample <= syncPrefixSamples) {
                    1.0
                } else {
                    1.0 - amplitudeDeviation
                }
                val sampleIndex = baseOffset + sample
                val volume = signalShape.calculate(sampleIndex, freq, amplitude, sampleRate)
                wavBuffer[sample * 2] = (volume and 0xFF).toByte()
                wavBuffer[sample * 2 + 1] = ((volume ushr 8) and 0xFF).toByte()
            }
        }
        return wavBuffer
    }

    companion object {
        private const val MORSE_JJY_MSB0 =
            "001011011011001011011011001101011011000101101101100101101101100110101101100"
    }
}
