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
 *  - Second 59: 100 ms off (base carrier-off, no data)
 *
 * Every second of the MSF signal has at least 100 ms of carrier-off.
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
        amplitudeDeviation: Double
    ): ByteArray {
        val msfRecord = record as MsfRecord
        val data = msfRecord.msfBits
        val bitState = ((data ushr secondIndex) and 1L) != 0L

        // Calculate carrier-off duration in milliseconds
        val offDurationMs = when {
            secondIndex == 0 -> 500  // Minute marker
            secondIndex in 53..58 -> {
                // Secondary minute marker: base A=1 adds 100 ms,
                // plus bit B determines another 100 or 200 ms.
                if (bitState) 300 else 200
            }
            else -> {
                // Normal data seconds (1–52) and second 59
                if (bitState) 200 else 100
            }
        }

        val syncPrefixSamples = sampleRate * offDurationMs / 1000

        val wavBuffer = ByteArray(sampleRate * 2)
        val baseOffset = secondIndex * sampleRate

        for (sample in 0 until sampleRate) {
            val amplitude = smoothedAmplitude(
                sample, syncPrefixSamples, amplitudeDeviation, sampleRate
            )
            val sampleIndex = baseOffset + sample
            val pcmValue = signalShape.calculate(sampleIndex, freq, amplitude, sampleRate)
            wavBuffer[sample * 2] = (pcmValue and 0xFF).toByte()
            wavBuffer[sample * 2 + 1] = ((pcmValue ushr 8) and 0xFF).toByte()
        }
        return wavBuffer
    }
}
