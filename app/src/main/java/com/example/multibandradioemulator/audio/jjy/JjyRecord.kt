package com.example.multibandradioemulator.audio.jjy

import com.example.multibandradioemulator.audio.TimeSignalRecord
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * JJY time signal record encoder (Japan, 40/60 kHz).
 * Ported from dcf77-soundwave's JjyRecord.
 */
class JjyRecord private constructor(
    bitString: Long,
    second: Int,
    val isCallSignAnnouncement: Boolean
) : TimeSignalRecord(bitString, second) {

    override fun getBitString(msb0: Boolean): Long {
        return if (msb0) reverseLowestBits(bitString, 60) else bitString
    }

    override fun extractSourceTime(): ZonedDateTime {
        return ZonedDateTime.now(ZONE_JST) // simplified
    }

    companion object {
        val ZONE_JST: ZoneId = ZoneId.of("Asia/Tokyo")

        fun isCallSignAnnouncementMinute(minute: Int): Boolean {
            return minute == 15 || minute == 45
        }

        fun create(time: ZonedDateTime): JjyRecord {
            val jstTime = ensureTimezone(time, ZONE_JST)
            return if (isCallSignAnnouncementMinute(jstTime.minute)) {
                createCallSignPacket(jstTime, time.second)
            } else {
                createTimePacket(jstTime, time.second)
            }
        }

        private fun createTimePacket(jstTime: ZonedDateTime, second: Int): JjyRecord {
            val data = makeTimePacketVersion1(
                minute = jstTime.minute,
                hour = jstTime.hour,
                dayOfYear = jstTime.dayOfYear,
                yearWithinCentury = jstTime.year % 100,
                dayOfWeek = jstTime.dayOfWeek.value % 7,
                leapSecondAtCurrentUtcMonthEnd = false,
                leapSecondAdded = false
            )
            return JjyRecord(data, second, false)
        }

        private fun createCallSignPacket(jstTime: ZonedDateTime, second: Int): JjyRecord {
            val data = makeTimePacketVersion2(
                minute = jstTime.minute,
                hour = jstTime.hour,
                dayOfYear = jstTime.dayOfYear,
                callSignAnnouncement = 0,
                serviceInterruptionScheduled = 0,
                serviceInterruptionDaytimeOnly = false,
                serviceInterruptionDuration = 0
            )
            return JjyRecord(data, second, true)
        }

        private fun makeTimePacketVersion1(
            minute: Int, hour: Int, dayOfYear: Int,
            yearWithinCentury: Int, dayOfWeek: Int,
            leapSecondAtCurrentUtcMonthEnd: Boolean,
            leapSecondAdded: Boolean
        ): Long {
            var data = 0L
            data = setBits(data, toBcdPadded5(minute).toLong(), 1, 0b11111111L, true)
            data = setBits(data, toBcdPadded5(hour).toLong(), 12, 0b1111111L, true)
            data = setBits(data, toBcdPadded5(dayOfYear).toLong(), 22, 0b111111111111L, true)
            data = setBits(data, toBCD(yearWithinCentury).toLong(), 41, 0b11111111L, true)
            data = setBits(data, toBcdPadded5(dayOfWeek).toLong(), 50, 0b111L, true)

            data = setBits(data, if (calcEvenParityOverBits(data, 12, 18)) 0L else 1L, 36, 1L, true)
            data = setBits(data, if (calcEvenParityOverBits(data, 1, 8)) 0L else 1L, 37, 1L, true)

            data = setBits(data, if (leapSecondAtCurrentUtcMonthEnd) 1L else 0L, 53, 1L, true)
            data = setBits(data, if (leapSecondAdded) 1L else 0L, 54, 1L, true)
            return data
        }

        private fun makeTimePacketVersion2(
            minute: Int, hour: Int, dayOfYear: Int,
            callSignAnnouncement: Int,
            serviceInterruptionScheduled: Int,
            serviceInterruptionDaytimeOnly: Boolean,
            serviceInterruptionDuration: Int
        ): Long {
            var data = 0L
            data = setBits(data, toBcdPadded5(minute).toLong(), 1, 0b11111111L, true)
            data = setBits(data, toBcdPadded5(hour).toLong(), 12, 0b1111111L, true)
            data = setBits(data, toBcdPadded5(dayOfYear).toLong(), 22, 0b111111111111L, true)

            data = setBits(data, if (calcEvenParityOverBits(data, 12, 18)) 0L else 1L, 36, 1L, true)
            data = setBits(data, if (calcEvenParityOverBits(data, 1, 8)) 0L else 1L, 37, 1L, true)

            data = setBits(data, toBCD(callSignAnnouncement).toLong(), 40, 0b111111111L, true)
            data = setBits(data, serviceInterruptionScheduled.toLong(), 50, 0b111L, true)
            data = setBits(data, if (serviceInterruptionDaytimeOnly) 1L else 0L, 53, 1L, true)
            data = setBits(data, serviceInterruptionDuration.toLong(), 54, 0b11L, true)
            return data
        }
    }
}
