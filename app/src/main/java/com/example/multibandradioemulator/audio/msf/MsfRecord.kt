package com.example.multibandradioemulator.audio.msf

import com.example.multibandradioemulator.audio.TimeSignalRecord
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * MSF time signal record encoder (Anthorn, United Kingdom, 60 kHz).
 *
 * MSF conceptually uses two bit-streams per second (A and B). In practice,
 * for seconds 1–52, B is always 0 so only A matters. For seconds 53–58,
 * A is always 1 (forming the "secondary minute marker" 01111110) while B
 * carries parity and BST information.
 *
 * This implementation stores a single 60-bit field where:
 *  - Bits 1–52 represent the A-stream time/date data.
 *  - Bits 53–58 represent the B-stream status/parity data.
 * The renderer adds 100 ms extra carrier-off for seconds 53–58 (the
 * secondary minute marker, i.e. A=1 always) on top of the B-bit modulation.
 *
 * Bit A (time/date data, BCD MSB-first):
 *  - Seconds 1–8:   DUT1 positive (unary, not implemented → 0)
 *  - Seconds 9–16:  DUT1 negative (unary, not implemented → 0)
 *  - Seconds 17–20: Year tens (4-bit BCD)
 *  - Seconds 21–24: Year units (4-bit BCD)
 *  - Second 25:     Month tens (1 bit)
 *  - Seconds 26–29: Month units (4-bit BCD)
 *  - Seconds 30–31: Day-of-month tens (2-bit BCD)
 *  - Seconds 32–35: Day-of-month units (4-bit BCD)
 *  - Seconds 36–38: Day of week (0 = Sunday, 3 bits)
 *  - Seconds 39–40: Hour tens (2-bit BCD)
 *  - Seconds 41–44: Hour units (4-bit BCD)
 *  - Seconds 45–47: Minute tens (3-bit BCD)
 *  - Seconds 48–51: Minute units (4-bit BCD)
 *  - Second 52:     Unused (0)
 *
 * Bit B (status & parity):
 *  - Second 53: BST changeover warning (1 = change within 61 minutes)
 *  - Second 54: Odd parity over bits 17A–24A (year)
 *  - Second 55: Odd parity over bits 25A–35A (month + day-of-month)
 *  - Second 56: Odd parity over bits 36A–38A (day of week)
 *  - Second 57: Odd parity over bits 39A–51A (hour + minute)
 *  - Second 58: BST flag (1 = BST active for transmitted time)
 */
class MsfRecord(time: ZonedDateTime) : TimeSignalRecord(-1L, time.second) {

    /** Combined bit field: positions 1–52 = A-stream, 53–58 = B-stream. */
    val msfBits: Long

    init {
        val londonTime = ensureTimezone(time, ZONE_LONDON)
        val isBst = ZONE_LONDON.rules.isDaylightSavings(time.toInstant())

        var data = 0L

        // ── Year: tens at 17–20 (4 bits), units at 21–24 (4 bits) ──
        val yearTens = (londonTime.year % 100) / 10
        val yearUnits = londonTime.year % 10
        data = setBit(data, 17, yearTens and 8 != 0)
        data = setBit(data, 18, yearTens and 4 != 0)
        data = setBit(data, 19, yearTens and 2 != 0)
        data = setBit(data, 20, yearTens and 1 != 0)
        data = setBit(data, 21, yearUnits and 8 != 0)
        data = setBit(data, 22, yearUnits and 4 != 0)
        data = setBit(data, 23, yearUnits and 2 != 0)
        data = setBit(data, 24, yearUnits and 1 != 0)

        // ── Month: tens at 25 (1 bit), units at 26–29 (4 bits) ──
        val monthTens = londonTime.monthValue / 10
        val monthUnits = londonTime.monthValue % 10
        data = setBit(data, 25, monthTens and 1 != 0)
        data = setBit(data, 26, monthUnits and 8 != 0)
        data = setBit(data, 27, monthUnits and 4 != 0)
        data = setBit(data, 28, monthUnits and 2 != 0)
        data = setBit(data, 29, monthUnits and 1 != 0)

        // ── Day of month: tens at 30–31 (2 bits), units at 32–35 (4 bits) ──
        val dayTens = londonTime.dayOfMonth / 10
        val dayUnits = londonTime.dayOfMonth % 10
        data = setBit(data, 30, dayTens and 2 != 0)
        data = setBit(data, 31, dayTens and 1 != 0)
        data = setBit(data, 32, dayUnits and 8 != 0)
        data = setBit(data, 33, dayUnits and 4 != 0)
        data = setBit(data, 34, dayUnits and 2 != 0)
        data = setBit(data, 35, dayUnits and 1 != 0)

        // ── Day of week: 36–38 (3 bits, 0 = Sunday) ──
        val dow = londonTime.dayOfWeek.value % 7
        data = setBit(data, 36, dow and 4 != 0)
        data = setBit(data, 37, dow and 2 != 0)
        data = setBit(data, 38, dow and 1 != 0)

        // ── Hour: tens at 39–40 (2 bits), units at 41–44 (4 bits) ──
        val hourTens = londonTime.hour / 10
        val hourUnits = londonTime.hour % 10
        data = setBit(data, 39, hourTens and 2 != 0)
        data = setBit(data, 40, hourTens and 1 != 0)
        data = setBit(data, 41, hourUnits and 8 != 0)
        data = setBit(data, 42, hourUnits and 4 != 0)
        data = setBit(data, 43, hourUnits and 2 != 0)
        data = setBit(data, 44, hourUnits and 1 != 0)

        // ── Minute: tens at 45–47 (3 bits), units at 48–51 (4 bits) ──
        val minuteTens = londonTime.minute / 10
        val minuteUnits = londonTime.minute % 10
        data = setBit(data, 45, minuteTens and 4 != 0)
        data = setBit(data, 46, minuteTens and 2 != 0)
        data = setBit(data, 47, minuteTens and 1 != 0)
        data = setBit(data, 48, minuteUnits and 8 != 0)
        data = setBit(data, 49, minuteUnits and 4 != 0)
        data = setBit(data, 50, minuteUnits and 2 != 0)
        data = setBit(data, 51, minuteUnits and 1 != 0)

        // Second 52: unused (already 0)

        // ── B-stream (seconds 53–58): status & parity ──
        // BST changeover warning (simplified: not implemented)
        // data = setBit(data, 53, bstChangeover)

        // Odd parity
        data = setBit(data, 54, calcOddParityBit(data, 17, 25)) // Year
        data = setBit(data, 55, calcOddParityBit(data, 25, 36)) // Month + day-of-month
        data = setBit(data, 56, calcOddParityBit(data, 36, 39)) // Day of week
        data = setBit(data, 57, calcOddParityBit(data, 39, 52)) // Hour + minute

        // BST flag
        data = setBit(data, 58, isBst)

        msfBits = data
    }

    override fun getBitString(msb0: Boolean): Long {
        throw UnsupportedOperationException("MSF uses msfBits, not standard bitString")
    }

    override fun extractSourceTime(): ZonedDateTime {
        return ZonedDateTime.now(ZONE_LONDON)
    }

    companion object {
        val ZONE_LONDON: ZoneId = ZoneId.of("Europe/London")

        private fun setBit(data: Long, position: Int, set: Boolean): Long {
            val mask = 1L shl position
            return if (set) data or mask else data and mask.inv()
        }

        /**
         * Returns the odd-parity bit for bits [from, to) in [data].
         * Returns true (= 1) when the count of set bits is even, so that
         * adding this bit makes the total count odd.
         */
        private fun calcOddParityBit(data: Long, from: Int, to: Int): Boolean {
            var count = 0
            for (i in from until to) {
                if ((data ushr i) and 1L != 0L) count++
            }
            return count % 2 == 0
        }
    }
}
