package com.example.multibandradioemulator.audio.dcf77

import com.example.multibandradioemulator.audio.TimeSignalRecord
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * DCF77 time signal record encoder (Germany, 77.5 kHz).
 * Ported from dcf77-soundwave's Dcf77Record.
 */
class Dcf77Record : TimeSignalRecord {

    constructor(time: ZonedDateTime) : this(
        civilWarningBits = 0,
        callBit = false,
        summerTimeAnnouncement = false,
        cest = ZONE_CET.rules.isDaylightSavings(time.toInstant()),
        cet = !ZONE_CET.rules.isDaylightSavings(time.toInstant()),
        leapSecondAnnouncement = false,
        second = time.second,
        bcdMinutes = toBCD(ensureTimezone(time, ZONE_CET).minute),
        bcdHours = toBCD(ensureTimezone(time, ZONE_CET).hour),
        bcdDayOfMonth = toBCD(ensureTimezone(time, ZONE_CET).dayOfMonth),
        bcdDayOfWeek = toBCD(ensureTimezone(time, ZONE_CET).dayOfWeek.value),
        bcdMonthNumber = toBCD(ensureTimezone(time, ZONE_CET).monthValue),
        bcdYearWithinCentury = toBCD(ensureTimezone(time, ZONE_CET).year % 100)
    )

    constructor(
        civilWarningBits: Int = 0,
        callBit: Boolean = false,
        summerTimeAnnouncement: Boolean = false,
        cest: Boolean = false,
        cet: Boolean = true,
        leapSecondAnnouncement: Boolean = false,
        second: Int = 0,
        bcdMinutes: Int,
        bcdHours: Int,
        bcdDayOfMonth: Int,
        bcdDayOfWeek: Int,
        bcdMonthNumber: Int,
        bcdYearWithinCentury: Int
    ) : super(
        makeData(
            civilWarningBits, callBit, summerTimeAnnouncement,
            cest, cet, leapSecondAnnouncement,
            bcdMinutes, bcdHours, bcdDayOfMonth, bcdDayOfWeek,
            bcdMonthNumber, bcdYearWithinCentury
        ),
        second
    )

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
        val ZONE_CET: ZoneId = ZoneId.of("CET")

        private fun makeData(
            civilWarningBits: Int,
            callBit: Boolean,
            summerTimeAnnouncement: Boolean,
            cest: Boolean,
            cet: Boolean,
            leapSecondAnnouncement: Boolean,
            bcdMinutes: Int,
            bcdHours: Int,
            bcdDayOfMonth: Int,
            bcdDayOfWeek: Int,
            bcdMonthNumber: Int,
            bcdYearWithinCentury: Int
        ): Long {
            val msb0 = false
            var data = setBits(0L, civilWarningBits.toLong(), 1, 0b11111111111111L, msb0)
            data = setBits(data, if (callBit) 1L else 0L, 15, 1L, msb0)
            data = setBits(data, if (summerTimeAnnouncement) 1L else 0L, 16, 1L, msb0)
            data = setBits(data, if (cest) 1L else 0L, 17, 1L, msb0)
            data = setBits(data, if (cet) 1L else 0L, 18, 1L, msb0)
            data = setBits(data, if (leapSecondAnnouncement) 1L else 0L, 19, 1L, msb0)
            data = setBits(data, 1L, 20, 1L, msb0)
            data = setBits(data, bcdMinutes.toLong(), 21, 0b1111111L, msb0)
            data = setBits(data, bcdHours.toLong(), 29, 0b111111L, msb0)
            data = setBits(data, bcdDayOfMonth.toLong(), 36, 0b111111L, msb0)
            data = setBits(data, bcdDayOfWeek.toLong(), 42, 0b111L, msb0)
            data = setBits(data, bcdMonthNumber.toLong(), 45, 0b11111L, msb0)
            data = setBits(data, bcdYearWithinCentury.toLong(), 50, 0b11111111L, msb0)

            data = setBits(data, if (calcEvenParityOverBits(data, 21, 28)) 1L else 0L, 28, 1L, msb0)
            data = setBits(data, if (calcEvenParityOverBits(data, 29, 35)) 1L else 0L, 35, 1L, msb0)
            data = setBits(data, if (calcEvenParityOverBits(data, 36, 58)) 1L else 0L, 58, 1L, msb0)

            return data
        }
    }
}
