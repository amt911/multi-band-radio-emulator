package com.example.multibandradioemulator.audio.msf

import com.example.multibandradioemulator.audio.SignalShape
import com.example.multibandradioemulator.audio.TimeSignalRecord
import com.example.multibandradioemulator.audio.TimeSignalRenderer
import java.time.ZonedDateTime

/**
 * MSF signal renderer. Generates on-off-keyed PCM audio.
 * Carrier frequencies are sub-harmonics of 60 kHz.
 *
 * MSF uses on-off keying: the carrier is completely cut (0 % power)
 * during the "off" portion of each second, unlike DCF77/WWVB/BPC
 * which only attenuate the carrier.
 *
 * Carrier-off durations per second:
 *  - Second 0  (minute marker): 500 ms off
 *  - Seconds 1–52  (data):      bit=0 → 100 ms off, bit=1 → 200 ms off
 *  - Seconds 53–58 (secondary minute marker): +100 ms extra
 *      → bit=0 → 200 ms off, bit=1 → 300 ms off
 *  - Second 59: NO carrier interruption (full carrier on).
 *      The uninterrupted carrier before second 0's 500 ms gap lets
 *      receivers reliably identify the minute boundary.
 */
class MsfRenderer : TimeSignalRenderer {

    override val encodesNextMinute: Boolean = true
    override val amplitudeDeviation: Double = 1.0  // On-off keying (carrier fully off)
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
        amplitudeDeviation: Double,
        sampleOffset: Long
    ): ByteArray {
        val msfRecord = record as MsfRecord
        val data = msfRecord.msfBits
        val bitState = ((data ushr secondIndex) and 1L) != 0L

        // Calculate carrier-off duration in samples.
        // Per NPL spec, second 59 has NO carrier interruption — the
        // continuous carrier before the 500 ms minute marker at second 0
        // is what lets receivers identify the minute boundary.
        val syncPrefixSamples = when {
            secondIndex == 0 -> sampleRate / 2             // 500 ms (minute marker)
            secondIndex == 59 -> 0                          // no interruption
            secondIndex in 53..58 -> {
                // Secondary minute marker: A=1 always (200 ms base),
                // B-stream adds another 100 ms when set.
                if (bitState) sampleRate * 3 / 10 else sampleRate / 5
            }
            else -> {
                // Normal data seconds (1–52): A=0 → 100 ms, A=1 → 200 ms
                if (bitState) sampleRate / 5 else sampleRate / 10
            }
        }

        val wavBuffer = ByteArray(sampleRate * 2)

        for (sample in 0 until sampleRate) {
            val amplitude = smoothedAmplitude(
                sample, syncPrefixSamples, amplitudeDeviation, sampleRate
            )
            val sampleIndex = sampleOffset + sample
            val pcmValue = signalShape.calculate(sampleIndex, freq, amplitude, sampleRate)
            wavBuffer[sample * 2] = (pcmValue and 0xFF).toByte()
            wavBuffer[sample * 2 + 1] = ((pcmValue ushr 8) and 0xFF).toByte()
        }
        return wavBuffer
    }
}
