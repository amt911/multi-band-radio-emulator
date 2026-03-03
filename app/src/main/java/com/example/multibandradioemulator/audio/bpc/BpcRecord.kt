package com.example.multibandradioemulator.audio.bpc

import com.example.multibandradioemulator.audio.TimeSignalRecord
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * BPC time signal record encoder (China, 68.5 kHz).
 * Uses 2-bit symbols instead of single bits.
 * Ported from dcf77-soundwave's BpcRecord.
 */
class BpcRecord(time: ZonedDateTime) : TimeSignalRecord(-1L, time.second) {

    val bcpBitString: BpcBitString

    init {
        val chnTime = ensureTimezone(time, ZONE_CHN)
        bcpBitString = BpcBitString(
            makeTimePacket(
                hours = chnTime.hour,
                minutes = chnTime.minute,
                dayOfWeek = chnTime.dayOfWeek.value,
                dayOfMonth = chnTime.dayOfMonth,
                month = chnTime.monthValue,
                year = chnTime.year % 100
            )
        )
    }

    override fun getBitString(msb0: Boolean): Long {
        throw UnsupportedOperationException("BPC uses BpcBitString, not long bit string")
    }

    override fun extractSourceTime(): ZonedDateTime {
        return ZonedDateTime.now(ZONE_CHN)
    }

    companion object {
        val ZONE_CHN: ZoneId = ZoneId.of("Asia/Shanghai")

        private fun makeTimePacket(
            hours: Int, minutes: Int, dayOfWeek: Int,
            dayOfMonth: Int, month: Int, year: Int
        ): String {
            val buffer = StringBuilder(120)
            for (i in 0..2) {
                val offset = i * 40

                // Position 0: Reference marker (full power, no modulation)
                buffer.append("00")

                // Position 1: Frame identifier
                when (i) {
                    0 -> buffer.append("00")
                    1 -> buffer.append("01")
                    2 -> buffer.append("10")
                }

                // Position 2: Unused
                buffer.append("00")

                // Positions 3-4: Hours (12-hour format, 4 bits)
                writeBinaryInt(buffer, hours % 12, 4)

                // Positions 5-7: Minutes (6 bits)
                writeBinaryInt(buffer, minutes, 6)

                // Half of position 8: Unused
                buffer.append('0')

                // Positions 8-9: Day of week (3 bits)
                writeBinaryInt(buffer, dayOfWeek, 3)

                // Half of position 10: PM flag
                buffer.append(if (hours >= 12) '1' else '0')

                // Half of position 10: P1 parity (symbols 1-9)
                buffer.append(if (isEven(buffer, offset + 2, offset + 20)) '1' else '0')

                // Half of position 11: Unused
                buffer.append('0')

                // Positions 11-13: Day of month (5 bits)
                writeBinaryInt(buffer, dayOfMonth, 5)

                // Positions 14-15: Month (4 bits)
                writeBinaryInt(buffer, month, 4)

                // Positions 16-18: Year (6 bits)
                writeBinaryInt(buffer, year and 0b111111, 6)

                // Half of position 19: Year MSB
                buffer.append(if ((year and 0b1000000) == 0) '0' else '1')

                // Half of position 19: P2 parity (symbols 11-18)
                buffer.append(if (isEven(buffer, offset + 22, offset + 38)) '1' else '0')
            }
            return buffer.toString()
        }

        private fun writeBinaryInt(builder: StringBuilder, value: Int, bits: Int) {
            val bin = Integer.toBinaryString(value)
            var zeros = bits - bin.length
            require(zeros >= 0) { "Value $value doesn't fit in $bits bit(s)" }
            while (zeros > 0) {
                builder.append('0')
                zeros--
            }
            builder.append(bin)
        }

        private fun isEven(charSequence: CharSequence, from: Int, to: Int): Boolean {
            var count = 0
            for (i in from until to) {
                if (charSequence[i] == '1') count++
            }
            return count % 2 != 0
        }
    }
}
