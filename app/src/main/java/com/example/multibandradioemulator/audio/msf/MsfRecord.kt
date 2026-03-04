package com.example.multibandradioemulator.audio.msf

import com.example.multibandradioemulator.audio.TimeSignalRecord
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * MSF time signal record encoder (Anthorn, United Kingdom, 60 kHz).
 *
 * MSF uses two bits per second (A and B), transmitted via carrier modulation patterns.
 * The time code encodes the NEXT minute in UK civil time (GMT/BST).
 *
 * Bit A carries time/date data in BCD format (MSB first):
 *  - Seconds 1-16:  DUT1 correction (set to 0 in this implementation)
 *  - Seconds 17-20: Year tens (BCD)
 *  - Seconds 21-24: Year units (BCD)
 *  - Second 25:     Month tens
 *  - Seconds 26-29: Month units (BCD)
 *  - Seconds 30-31: Day of month tens
 *  - Seconds 32-35: Day of month units (BCD)
 *  - Seconds 36-38: Day of week (0=Sunday)
 *  - Seconds 39-40: Hour tens
 *  - Seconds 41-44: Hour units (BCD)
 *  - Seconds 45-47: Minute tens
 *  - Seconds 48-51: Minute units (BCD)
 *  - Second 52:     Unused (0)
 *
 * Bit B carries parity and DST information:
 *  - Second 53:     BST change warning
 *  - Seconds 54-55: BST status (01=BST, 10=GMT)
 *  - Second 57:     Even parity over year (17A-24A)
 *  - Second 58:     Even parity over month+day (25A-35A)
 */
class MsfRecord(time: ZonedDateTime) : TimeSignalRecord(-1L, time.second) {

    val bitA: Long
    val bitB: Long

    init {
        val londonTime = ensureTimezone(time, ZONE_LONDON)
        val isBst = ZONE_LONDON.rules.isDaylightSavings(time.toInstant())

        val year = londonTime.year % 100
        val month = londonTime.monthValue
        val dayOfMonth = londonTime.dayOfMonth
        val dayOfWeek = londonTime.dayOfWeek.value % 7 // Convert to 0=Sunday
        val hour = londonTime.hour
        val minute = londonTime.minute

        bitA = makeBitA(year, month, dayOfMonth, dayOfWeek, hour, minute)
        bitB = makeBitB(bitA, isBst)
    }

    override fun getBitString(msb0: Boolean): Long {
        throw UnsupportedOperationException("MSF uses bitA and bitB, not a single bit string")
    }

    override fun extractSourceTime(): ZonedDateTime {
        return ZonedDateTime.now(ZONE_LONDON)
    }

    companion object {
        val ZONE_LONDON: ZoneId = ZoneId.of("Europe/London")

        private fun makeBitA(
            year: Int, month: Int, dayOfMonth: Int,
            dayOfWeek: Int, hour: Int, minute: Int
        ): Long {
            var data = 0L

            // Seconds 1-16: DUT1 = 0 (all zeros, simplified)

            // Year: tens at 17-20, units at 21-24
            data = setBits(data, (year / 10).toLong(), 17, 0b1111L, true)
            data = setBits(data, (year % 10).toLong(), 21, 0b1111L, true)

            // Month: tens at 25 (1 bit), units at 26-29
            data = setBits(data, (month / 10).toLong(), 25, 0b1L, true)
            data = setBits(data, (month % 10).toLong(), 26, 0b1111L, true)

            // Day of month: tens at 30-31, units at 32-35
            data = setBits(data, (dayOfMonth / 10).toLong(), 30, 0b11L, true)
            data = setBits(data, (dayOfMonth % 10).toLong(), 32, 0b1111L, true)

            // Day of week: 36-38 (0=Sunday)
            data = setBits(data, dayOfWeek.toLong(), 36, 0b111L, true)

            // Hours: tens at 39-40, units at 41-44
            data = setBits(data, (hour / 10).toLong(), 39, 0b11L, true)
            data = setBits(data, (hour % 10).toLong(), 41, 0b1111L, true)

            // Minutes: tens at 45-47, units at 48-51
            data = setBits(data, (minute / 10).toLong(), 45, 0b111L, true)
            data = setBits(data, (minute % 10).toLong(), 48, 0b1111L, true)

            return data
        }

        private fun makeBitB(bitA: Long, isBst: Boolean): Long {
            var data = 0L

            // BST status at seconds 54-55: 01 = BST, 10 = GMT
            if (isBst) {
                data = setBits(data, 1L, 55, 1L, true)
            } else {
                data = setBits(data, 1L, 54, 1L, true)
            }

            // Parity bits
            // 57B: Even parity over year A-bits (17A-24A)
            data = setBits(
                data,
                if (calcEvenParityOverBits(bitA, 17, 25)) 1L else 0L,
                57, 1L, true
            )
            // 58B: Even parity over month+day A-bits (25A-35A)
            data = setBits(
                data,
                if (calcEvenParityOverBits(bitA, 25, 36)) 1L else 0L,
                58, 1L, true
            )

            return data
        }
    }
}
