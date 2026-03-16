package com.example.multibandradioemulator.audio.dcf77

import android.util.Log
import com.example.multibandradioemulator.audio.TimeSignalRecord
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * DCF77 time signal record encoder (Germany, 77.5 kHz).
 *
 * Encoding follows the exact same per-bit assignment as TimeStation's
 * tsig_xmit_dcf77(), using a 60-element array for direct bit placement.
 * This makes the implementation trivially verifiable against the reference.
 */
class Dcf77Record private constructor(
    bitString: Long,
    second: Int
) : TimeSignalRecord(bitString, second) {

    override fun getBitString(msb0: Boolean): Long {
        return if (msb0) reverseLowestBits(bitString, 60) else bitString
    }

    override fun extractSourceTime(): ZonedDateTime {
        val raw = bitString
        val minute = fromBCD((reverseLowestBits(bits(raw, 21, 0b1111111L), 7)).toInt())
        val hour = fromBCD((reverseLowestBits(bits(raw, 29, 0b111111L), 6)).toInt())
        val dayOfMonth = fromBCD((reverseLowestBits(bits(raw, 36, 0b111111L), 6)).toInt())
        val month = fromBCD((reverseLowestBits(bits(raw, 45, 0b11111L), 5)).toInt())
        val year = fromBCD((reverseLowestBits(bits(raw, 50, 0b11111111L), 8)).toInt())
        return ZonedDateTime.of(
            year + currentCentury(), month, dayOfMonth,
            hour, minute, second, 0, ZONE_CET
        )
    }

    companion object {
        private const val TAG = "Dcf77Record"

        val ZONE_CET: ZoneId = ZoneId.of("CET")

        /**
         * Create a DCF77 record for the given time.
         *
         * The time should already be the TRANSMITTED time (i.e. the next minute's
         * civil time in CET/CEST). The caller (RadioSignalPlayer) handles adding
         * +1 minute for DCF77's "encodes next minute" behaviour.
         */
        fun create(time: ZonedDateTime): Dcf77Record {
            val cetTime = ensureTimezone(time, ZONE_CET)
            val isDst = ZONE_CET.rules.isDaylightSavings(time.toInstant())

            val bits = encodeTimeStation(
                minute = cetTime.minute,
                hour = cetTime.hour,
                day = cetTime.dayOfMonth,
                dow = cetTime.dayOfWeek.value,  // ISO: 1=Mon..7=Sun
                month = cetTime.monthValue,
                year = cetTime.year,
                isCest = isDst
            )

            val data = bitsArrayToLong(bits)

            Log.d(TAG, buildString {
                append("DCF77 encode ${cetTime.hour}:${String.format("%02d", cetTime.minute)}")
                append(" ${cetTime.dayOfMonth}/${cetTime.monthValue}/${cetTime.year}")
                append(" DOW=${cetTime.dayOfWeek.value}")
                append(" DST=$isDst")
                append(" bits=")
                for (i in 0 until 60) append(if (bits[i] != 0) '1' else '0')
            })

            return Dcf77Record(data, time.second)
        }

        /**
         * Encode DCF77 bits using the exact same per-bit assignment
         * as TimeStation's tsig_xmit_dcf77().
         *
         * Each element of the returned array is 0 or 1 (except index 59
         * which is a marker — but we handle second 59 specially in the renderer).
         */
        private fun encodeTimeStation(
            minute: Int,
            hour: Int,
            day: Int,
            dow: Int,
            month: Int,
            year: Int,
            isCest: Boolean
        ): IntArray {
            val bits = IntArray(60)

            // Bit 0: always 0 (start of minute) — already 0

            // Bits 1-14: civil warning / weather (unused, leave 0)
            // Bit 15: call bit (unused, leave 0)
            // Bit 16: summer time announcement (leave 0 for now)

            // Bit 17: CEST indicator
            bits[17] = if (isCest) 1 else 0
            // Bit 18: CET indicator (inverse of CEST)
            bits[18] = if (isCest) 0 else 1

            // Bit 19: leap second announcement (leave 0)

            // Bit 20: start of time info (always 1)
            bits[20] = 1

            // Bits 21-27: Minutes in BCD (LSB first)
            val minUnits = minute % 10
            bits[21] = (minUnits shr 0) and 1
            bits[22] = (minUnits shr 1) and 1
            bits[23] = (minUnits shr 2) and 1
            bits[24] = (minUnits shr 3) and 1
            val minTens = minute / 10
            bits[25] = (minTens shr 0) and 1
            bits[26] = (minTens shr 1) and 1
            bits[27] = (minTens shr 2) and 1

            // Bit 28: P1 — even parity over bits 21-27
            bits[28] = evenParity(bits, 21, 28)

            // Bits 29-34: Hours in BCD (LSB first)
            val hourUnits = hour % 10
            bits[29] = (hourUnits shr 0) and 1
            bits[30] = (hourUnits shr 1) and 1
            bits[31] = (hourUnits shr 2) and 1
            bits[32] = (hourUnits shr 3) and 1
            val hourTens = hour / 10
            bits[33] = (hourTens shr 0) and 1
            bits[34] = (hourTens shr 1) and 1

            // Bit 35: P2 — even parity over bits 29-34
            bits[35] = evenParity(bits, 29, 35)

            // Bits 36-41: Day of month in BCD (LSB first)
            val dayUnits = day % 10
            bits[36] = (dayUnits shr 0) and 1
            bits[37] = (dayUnits shr 1) and 1
            bits[38] = (dayUnits shr 2) and 1
            bits[39] = (dayUnits shr 3) and 1
            val dayTens = day / 10
            bits[40] = (dayTens shr 0) and 1
            bits[41] = (dayTens shr 1) and 1

            // Bits 42-44: Day of week in BCD (1=Monday..7=Sunday, LSB first)
            bits[42] = (dow shr 0) and 1
            bits[43] = (dow shr 1) and 1
            bits[44] = (dow shr 2) and 1

            // Bits 45-49: Month in BCD (LSB first)
            val monUnits = month % 10
            bits[45] = (monUnits shr 0) and 1
            bits[46] = (monUnits shr 1) and 1
            bits[47] = (monUnits shr 2) and 1
            bits[48] = (monUnits shr 3) and 1
            val monTens = month / 10
            bits[49] = (monTens shr 0) and 1

            // Bits 50-57: Year within century in BCD (LSB first)
            val yearInCentury = year % 100
            val yearUnits = yearInCentury % 10
            bits[50] = (yearUnits shr 0) and 1
            bits[51] = (yearUnits shr 1) and 1
            bits[52] = (yearUnits shr 2) and 1
            bits[53] = (yearUnits shr 3) and 1
            val yearTens = yearInCentury / 10
            bits[54] = (yearTens shr 0) and 1
            bits[55] = (yearTens shr 1) and 1
            bits[56] = (yearTens shr 2) and 1
            bits[57] = (yearTens shr 3) and 1

            // Bit 58: P3 — even parity over bits 36-57
            bits[58] = evenParity(bits, 36, 58)

            // Bit 59: minute marker (no modulation) — handled by renderer, leave 0

            return bits
        }

        /**
         * Even parity over bits[lo] .. bits[hi-1].
         * Returns 1 if the count of set bits is odd (making total even with parity bit).
         * Matches TimeStation's tsig_even_parity(bits, lo, hi).
         */
        private fun evenParity(bits: IntArray, lo: Int, hi: Int): Int {
            var parity = 0
            for (i in lo until hi) {
                if (bits[i] != 0) parity = parity xor 1
            }
            return parity
        }

        /** Convert a 60-element bit array to a Long (bit 0 = LSB). */
        private fun bitsArrayToLong(bits: IntArray): Long {
            var data = 0L
            for (i in 0 until 60) {
                if (bits[i] != 0) {
                    data = data or (1L shl i)
                }
            }
            return data
        }
    }
}
