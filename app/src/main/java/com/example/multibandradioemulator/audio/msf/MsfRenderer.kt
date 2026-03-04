package com.example.multibandradioemulator.audio.msf

import com.example.multibandradioemulator.audio.SignalShape
import com.example.multibandradioemulator.audio.TimeSignalRecord
import com.example.multibandradioemulator.audio.TimeSignalRenderer
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.cos

/**
 * MSF signal renderer. Generates AM-modulated PCM audio.
 * Carrier frequencies are sub-harmonics of 60 kHz.
 *
 * Modulation patterns per second (carrier reduced = low amplitude):
 *  - Second 0 (minute marker): 500ms reduced
 *  - A=0, B=0: 100ms reduced
 *  - A=1, B=0: 200ms reduced
 *  - A=0, B=1: 100ms reduced, 100ms full, 100ms reduced (split)
 *  - A=1, B=1: 300ms reduced
 *  - Second 59: no modulation (full carrier)
 */
class MsfRenderer : TimeSignalRenderer {

    override val encodesNextMinute: Boolean = true
    override val amplitudeDeviation: Double = 0.90
    override val carrierFrequencies: List<Int> = listOf(8571, 12000, 15000)

    override fun makeTimeSignalRecord(time: ZonedDateTime): TimeSignalRecord {
        return MsfRecord(time)
    }

    override fun renderSecondPcm(
        record: TimeSignalRecord,
        secondIndex: Int,
        freq: Double,
        sampleRate: Int,
        signalShape: SignalShape,
        amplitudeDeviation: Double
    ): ByteArray {
        val msfRecord = record as MsfRecord
        val wavBuffer = ByteArray(sampleRate * 2)
        val baseOffset = secondIndex * sampleRate

        when {
            secondIndex == 0 -> {
                // Minute marker: 500ms reduced
                val syncSamples = sampleRate / 2
                renderSamples(wavBuffer, baseOffset, sampleRate, freq, signalShape) { sample ->
                    smoothedAmplitude(sample, syncSamples, amplitudeDeviation, sampleRate)
                }
            }
            secondIndex == 59 -> {
                // No modulation (full carrier)
                renderSamples(wavBuffer, baseOffset, sampleRate, freq, signalShape) { 1.0 }
            }
            else -> {
                val bitA = ((msfRecord.bitA ushr secondIndex) and 1L).toInt()
                val bitB = ((msfRecord.bitB ushr secondIndex) and 1L).toInt()

                if (bitA == 0 && bitB == 1) {
                    // Split pattern: 100ms off, 100ms on, 100ms off, then on
                    val seg = sampleRate / 10 // 100ms in samples
                    renderSamples(wavBuffer, baseOffset, sampleRate, freq, signalShape) { sample ->
                        splitAmplitude(sample, seg, amplitudeDeviation, sampleRate)
                    }
                } else {
                    val syncMs = when {
                        bitA == 0 -> 100    // A=0, B=0
                        bitB == 0 -> 200    // A=1, B=0
                        else -> 300         // A=1, B=1
                    }
                    val syncSamples = sampleRate * syncMs / 1000
                    renderSamples(wavBuffer, baseOffset, sampleRate, freq, signalShape) { sample ->
                        smoothedAmplitude(sample, syncSamples, amplitudeDeviation, sampleRate)
                    }
                }
            }
        }
        return wavBuffer
    }

    private inline fun renderSamples(
        wavBuffer: ByteArray,
        baseOffset: Int,
        sampleRate: Int,
        freq: Double,
        signalShape: SignalShape,
        amplitudeFn: (Int) -> Double
    ) {
        for (sample in 0 until sampleRate) {
            val amplitude = amplitudeFn(sample)
            val sampleIndex = baseOffset + sample
            val pcmValue = signalShape.calculate(sampleIndex, freq, amplitude, sampleRate)
            wavBuffer[sample * 2] = (pcmValue and 0xFF).toByte()
            wavBuffer[sample * 2 + 1] = ((pcmValue ushr 8) and 0xFF).toByte()
        }
    }

    /**
     * Amplitude envelope for the MSF "split" pattern (A=0, B=1):
     * 100ms reduced, 100ms full, 100ms reduced, then full for the rest.
     * Uses cosine ramps at transitions to avoid clicks.
     */
    private fun splitAmplitude(
        sample: Int,
        segmentSamples: Int,
        amplitudeDeviation: Double,
        sampleRate: Int
    ): Double {
        val rampSamples = (sampleRate * 2.0 / 1000.0).toInt() // 2ms ramp
        val reducedLevel = 1.0 - amplitudeDeviation
        val seg2 = segmentSamples * 2
        val seg3 = segmentSamples * 3

        return when {
            // First reduced segment: 0 to seg
            sample < rampSamples -> {
                // Ramp from full (end of previous second) to reduced
                val t = sample.toDouble() / rampSamples
                val s = (1.0 - cos(PI * t)) / 2.0
                1.0 - s * amplitudeDeviation
            }
            sample < segmentSamples -> reducedLevel
            sample < segmentSamples + rampSamples -> {
                // Ramp from reduced to full
                val t = (sample - segmentSamples).toDouble() / rampSamples
                val s = (1.0 - cos(PI * t)) / 2.0
                reducedLevel + s * amplitudeDeviation
            }
            // Full segment: seg to 2*seg
            sample < seg2 -> 1.0
            // Second reduced segment: 2*seg to 3*seg
            sample < seg2 + rampSamples -> {
                val t = (sample - seg2).toDouble() / rampSamples
                val s = (1.0 - cos(PI * t)) / 2.0
                1.0 - s * amplitudeDeviation
            }
            sample < seg3 -> reducedLevel
            sample < seg3 + rampSamples -> {
                val t = (sample - seg3).toDouble() / rampSamples
                val s = (1.0 - cos(PI * t)) / 2.0
                reducedLevel + s * amplitudeDeviation
            }
            // Full for the rest of the second
            else -> 1.0
        }
    }
}
