package com.example.multibandradioemulator.audio.bpc

/**
 * BPC uses 2-bit symbols (bit pairs) instead of single bits.
 * Stores two separate long bit fields.
 * Ported from dcf77-soundwave's BcpBitString.
 */
class BpcBitString(bitStringText: String) {

    private val bitString0: Long
    private val bitString1: Long

    init {
        var mask = 1L
        var s0 = 0L
        var s1 = 0L
        var bitIndex = 0

        for (c in bitStringText) {
            when (c) {
                '0' -> {
                    if (bitIndex != 0) {
                        mask = mask shl 1
                    }
                    bitIndex = bitIndex xor 1
                }
                '1' -> {
                    if (bitIndex == 0) {
                        s0 = s0 or mask
                    } else {
                        s1 = s1 or mask
                        mask = mask shl 1
                    }
                    bitIndex = bitIndex xor 1
                }
            }
        }

        this.bitString0 = s1
        this.bitString1 = s0
    }

    fun getBitPair(index: Int): Int {
        return ((bitString0 ushr index) and 1L).toInt() or
                (((bitString1 ushr index) and 1L).toInt() shl 1)
    }

    fun isEvenDiapason(fromPair: Int, toPairInclude: Int): Boolean {
        var notEven = 0
        for (i in fromPair..toPairInclude) {
            val bitPair = getBitPair(i)
            if (bitPair == 1 || bitPair == 2) {
                notEven = notEven xor 1
            }
        }
        return notEven != 0
    }

    fun readDiapason(fromPair: Int, toPairInclusive: Int): Int {
        var accum = 0
        for (i in fromPair..toPairInclusive) {
            accum = accum shl 2
            accum = accum or getBitPair(i)
        }
        return accum
    }
}
