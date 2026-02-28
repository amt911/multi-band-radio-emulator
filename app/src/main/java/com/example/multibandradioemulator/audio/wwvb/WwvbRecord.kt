package com.example.multibandradioemulator.audio.wwvb

import com.example.multibandradioemulator.audio.TimeSignalRecord
import java.time.Month
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * WWVB time signal record encoder (USA, 60 kHz).
 * Ported from dcf77-soundwave's WwvbRecord.
 */
class WwvbRecord(time: ZonedDateTime) : TimeSignalRecord(
    bitString = makeTimePacket(
        minute = ensureTimezone(time, ZoneOffset.UTC).minute,
        hour = ensureTimezone(time, ZoneOffset.UTC).hour,
        dayOfYear = ensureTimezone(time, ZoneOffset.UTC).dayOfYear,
        dut1sign = 0,
        dut1 = 0.0f,
        yearWithinCentury = ensureTimezone(time, ZoneOffset.UTC).year % 100,
        leapYear = ensureTimezone(time, ZoneOffset.UTC).toLocalDate().isLeapYear,
        leapSecondAtEndOfMonth = isLeapSecondMonth(ensureTimezone(time, ZoneOffset.UTC)),
        dstStatus = calculateDstStatus(ensureTimezone(time, ZoneOffset.UTC))
    ),
    second = time.second
) {
    override fun getBitString(msb0: Boolean): Long {
        return if (msb0) reverseLowestBits(bitString, 60) else bitString
    }

    override fun extractSourceTime(): ZonedDateTime {
        return ZonedDateTime.now(ZoneOffset.UTC) // simplified
    }

    companion object {
        private fun isLeapSecondMonth(time: ZonedDateTime): Boolean {
            val month = time.month
            return month == Month.DECEMBER || month == Month.JUNE
        }

        private fun calculateDstStatus(time: ZonedDateTime): Int {
            return when (time.month) {
                Month.JANUARY, Month.FEBRUARY, Month.DECEMBER -> 0b00
                Month.MARCH -> {
                    val dayOfWeek = time.dayOfWeek.value % 7
                    val dayOfMonth = time.dayOfMonth
                    val sundays = (dayOfMonth + 6 - dayOfWeek) / 7
                    when {
                        dayOfWeek == 0 && sundays == 2 -> 0b10
                        sundays >= 2 -> 0b11
                        else -> 0b00
                    }
                }
                Month.APRIL, Month.MAY, Month.JUNE, Month.JULY,
                Month.AUGUST, Month.SEPTEMBER, Month.OCTOBER -> 0b11
                Month.NOVEMBER -> {
                    val dayOfWeek = time.dayOfWeek.value % 7
                    val dayOfMonth = time.dayOfMonth
                    val sundays = (dayOfMonth + 6 - dayOfWeek) / 7
                    when {
                        dayOfWeek == 0 && sundays == 1 -> 0b01
                        sundays < 1 -> 0b11
                        else -> 0b00
                    }
                }
                else -> 0b00
            }
        }

        private fun toDut1(dut1: Float): Int {
            return toBCD(maxOf(0, minOf(9, Math.round(dut1 * 10))))
        }

        private fun makeTimePacket(
            minute: Int, hour: Int, dayOfYear: Int,
            dut1sign: Int, dut1: Float,
            yearWithinCentury: Int, leapYear: Boolean,
            leapSecondAtEndOfMonth: Boolean, dstStatus: Int
        ): Long {
            var data = 0L
            data = setBits(data, toBcdPadded5(minute).toLong(), 1, 0b11111111L, true)
            data = setBits(data, toBcdPadded5(hour).toLong(), 12, 0b1111111L, true)
            data = setBits(data, toBcdPadded5(dayOfYear).toLong(), 22, 0b111111111111L, true)
            data = setBits(data, dut1sign.toLong(), 36, 0b111L, true)
            data = setBits(data, toDut1(dut1).toLong(), 40, 0b1111L, true)
            data = setBits(data, toBcdPadded5(yearWithinCentury).toLong(), 45, 0b111111111L, true)
            data = setBits(data, if (leapYear) 1L else 0L, 55, 1L, true)
            data = setBits(data, if (leapSecondAtEndOfMonth) 1L else 0L, 56, 1L, true)
            data = setBits(data, dstStatus.toLong(), 57, 0b11L, true)
            return data
        }
    }
}
