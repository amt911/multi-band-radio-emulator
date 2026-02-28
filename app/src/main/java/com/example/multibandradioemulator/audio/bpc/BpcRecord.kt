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

                // Frame identifier
                when (i) {
                    0 -> buffer.append("00")
                    1 -> buffer.append("01")
                    2 -> buffer.append("10")
                }

                // Unused
                buffer.append("00")

                // Hours (12-hour format)
                writeBinaryInt(buffer, hours % 12, 4)

                // Minutes
                writeBinaryInt(buffer, minutes, 6)

                // Unused
                buffer.append('0')

                // Day of week
                writeBinaryInt(buffer, dayOfWeek, 3)

                // PM flag
                buffer.append(if (hours > 12) '1' else '0')

                // P1 parity
                buffer.append(if (isEven(buffer, offset, offset + 18)) '1' else '0')

                // Unused
                buffer.append('0')

                // Day of month
                writeBinaryInt(buffer, dayOfMonth, 5)

                // Month
                writeBinaryInt(buffer, month, 4)

                // Year (6 bits + 1 bit)
                writeBinaryInt(buffer, year and 0b111111, 6)
                buffer.append(if ((year and 0b1000000) == 0) '0' else '1')

                // P2 parity
                buffer.append(if (isEven(buffer, offset + 20, offset + 36)) '1' else '0')

                // Sync marker
                buffer.append("00")
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
