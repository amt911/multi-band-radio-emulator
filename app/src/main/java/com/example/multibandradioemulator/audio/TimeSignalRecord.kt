package com.example.multibandradioemulator.audio

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Base class for minute-based time signal records.
 * Stores a 60-bit encoding as a Long, with BCD encoding utilities.
 * Ported from dcf77-soundwave's AbstractMinuteBasedTimeSignalRecord.
 */
abstract class TimeSignalRecord(
    val bitString: Long,
    val second: Int
) {
    init {
        require(second in 0..59) { "Second must be 0..59: $second" }
    }

    open fun getBitString(msb0: Boolean): Long = bitString

    abstract fun extractSourceTime(): ZonedDateTime

    companion object {
        fun currentCentury(): Int = (LocalDate.now().year / 100) * 100

        fun bits(data: Long, shift: Int, mask: Long): Long {
            var result = (data ushr shift) and mask
            if (mask > 1L) {
                var reversed = 0L
                var restMask = mask
                var r = result
                while (restMask != 0L) {
                    reversed = (reversed shl 1) or (r and 1L)
                    r = r ushr 1
                    restMask = restMask ushr 1
                }
                return reversed
            }
            return result
        }

        fun toBCD(value: Int): Int {
            require(value in 0..255)
            var bcd = 0
            var shift = 0
            var v = value
            while (v > 0) {
                val digit = v % 10
                bcd = bcd or (digit shl (shift * 4))
                v /= 10
                shift++
            }
            return bcd
        }

        fun toBcdPadded5(value: Int): Int {
            return (((value / 100) % 10) shl 10) or (((value / 10) % 10) shl 5) or (value % 10)
        }

        fun fromBcdPadded5(bcd5value: Int): Int {
            val a = bcd5value and 0xF
            val b = (bcd5value shr 5) and 0xF
            val c = (bcd5value shr 10) and 0xF
            return c * 100 + b * 10 + a
        }

        fun fromBCD(bcdValue: Int): Int {
            var value = 0
            var factor = 1
            var bv = bcdValue
            while (bv > 0) {
                val digit = bv and 0xF
                require(digit <= 9) { "Wrong BCD format: $digit" }
                value += digit * factor
                factor *= 10
                bv = bv shr 4
            }
            return value
        }

        fun calcEvenParityOverBits(data: Long, from: Int, to: Int): Boolean {
            var result = false
            for (i in from until to) {
                if (bits(data, i, 1L) != 0L) {
                    result = !result
                }
            }
            return result
        }

        fun reverseLowestBits(value: Long, numberOfLowestBits: Int): Long {
            var acc = 0L
            var src = value
            for (i in 0 until numberOfLowestBits) {
                acc = (acc shl 1) or (src and 1L)
                src = src ushr 1
            }
            return acc
        }

        fun ensureTimezone(time: ZonedDateTime, zoneId: ZoneId): ZonedDateTime {
            return if (time.zone == zoneId) time else time.withZoneSameInstant(zoneId)
        }

        fun setBits(data: Long, value: Long, shift: Int, mask: Long, msb0: Boolean): Long {
            val x: Long
            if (mask != 1L && msb0) {
                var maskBitsCount = 0
                var tempMask = mask
                while (tempMask != 0L) {
                    maskBitsCount++
                    tempMask = tempMask ushr 1
                }
                x = reverseLowestBits(value, maskBitsCount)
            } else {
                x = value
            }
            val shiftedMaskedValue = (x and mask) shl shift
            return (data and ((mask shl shift).inv())) or shiftedMaskedValue
        }
    }
}
