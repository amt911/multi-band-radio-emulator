package com.example.multibandradioemulator.audio

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.round
import kotlin.math.sin

/**
 * Signal waveform shapes for amplitude modulation.
 * Square and triangle waves produce stronger harmonics, potentially improving reception.
 */
enum class SignalShape {
    SIN {
        override fun calculate(sampleIndex: Int, freq: Double, amplitude: Double, sampleRate: Int): Long {
            val time = sampleIndex.toDouble() / sampleRate
            return round(sin(2.0 * PI * freq * time) * Short.MAX_VALUE * amplitude).toLong()
        }
    },
    SQUARE {
        override fun calculate(sampleIndex: Int, freq: Double, amplitude: Double, sampleRate: Int): Long {
            val time = sampleIndex.toDouble() / sampleRate
            val sineValue = sin(2.0 * PI * freq * time)
            return round((if (sineValue >= 0.0) Short.MAX_VALUE else Short.MIN_VALUE).toDouble() * amplitude).toLong()
        }
    },
    TRIANGLE {
        override fun calculate(sampleIndex: Int, freq: Double, amplitude: Double, sampleRate: Int): Long {
            val time = sampleIndex.toDouble() / sampleRate
            val sine = sin(2.0 * PI * freq * time)
            return round((2.0 / PI) * asin(sine) * Short.MAX_VALUE * amplitude).toLong()
        }
    };

    abstract fun calculate(sampleIndex: Int, freq: Double, amplitude: Double, sampleRate: Int): Long
}
