package com.example.core

import java.security.MessageDigest

/**
 * Cryptographic digest helpers matching Bitcoin Core's src/hash.h and src/crypto/.
 * Provides pure hashing utilities for developer protocol inspection (no mining/key generation).
 */
object BitcoinCrypto {

    fun sha256(data: ByteArray): ByteArray {
        return MessageDigest.getInstance("SHA-256").digest(data)
    }

    fun doubleSha256(data: ByteArray): ByteArray {
        return sha256(sha256(data))
    }

    fun sha512(data: ByteArray): ByteArray {
        return MessageDigest.getInstance("SHA-512").digest(data)
    }

    fun sha3_256(data: ByteArray): ByteArray {
        return MessageDigest.getInstance("SHA3-256").digest(data)
    }

    fun ripemd160(data: ByteArray): ByteArray {
        return Ripemd160.digest(data)
    }

    fun hash160(data: ByteArray): ByteArray {
        return ripemd160(sha256(data))
    }

    fun hexToBytesOrNull(hex: String): ByteArray? {
        val clean = hex.trim().replace("\\s+".toRegex(), "")
        if (clean.length % 2 != 0) return null
        if (clean.isEmpty()) return ByteArray(0)
        val out = ByteArray(clean.length / 2)
        for (i in out.indices) {
            val hi = Character.digit(clean[i * 2], 16)
            val lo = Character.digit(clean[i * 2 + 1], 16)
            if (hi == -1 || lo == -1) return null
            out[i] = ((hi shl 4) or lo).toByte()
        }
        return out
    }

    fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            sb.append(HEX_CHARS[v ushr 4])
            sb.append(HEX_CHARS[v and 0x0F])
        }
        return sb.toString()
    }

    fun bytesToReversedHex(bytes: ByteArray): String {
        val reversed = ByteArray(bytes.size)
        for (i in bytes.indices) {
            reversed[i] = bytes[bytes.size - 1 - i]
        }
        return bytesToHex(reversed)
    }

    private const val HEX_CHARS = "0123456789abcdef"
}

/**
 * Pure Kotlin RIPEMD-160 implementation matching Bitcoin Core's src/crypto/ripemd160.cpp.
 */
object Ripemd160 {

    private val R1 = intArrayOf(
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
        7, 4, 13, 1, 10, 6, 15, 3, 12, 0, 9, 5, 2, 14, 11, 8,
        3, 10, 14, 4, 9, 15, 8, 1, 2, 7, 0, 6, 13, 11, 5, 12,
        1, 9, 11, 10, 0, 8, 12, 4, 13, 3, 7, 15, 14, 5, 6, 2,
        4, 0, 5, 9, 7, 12, 2, 10, 14, 1, 3, 8, 11, 6, 15, 13
    )

    private val R2 = intArrayOf(
        5, 14, 7, 0, 9, 2, 11, 4, 13, 6, 15, 8, 1, 10, 3, 12,
        6, 11, 3, 7, 0, 13, 5, 10, 14, 15, 8, 12, 4, 9, 1, 2,
        15, 5, 1, 3, 7, 14, 6, 9, 11, 8, 12, 2, 10, 0, 4, 13,
        8, 6, 4, 1, 3, 11, 15, 0, 5, 12, 2, 13, 9, 7, 10, 14,
        12, 15, 10, 4, 1, 5, 8, 7, 6, 2, 13, 14, 0, 3, 9, 11
    )

    private val S1 = intArrayOf(
        11, 14, 15, 12, 5, 8, 7, 9, 11, 13, 14, 15, 6, 7, 9, 8,
        7, 6, 8, 13, 11, 9, 7, 15, 7, 12, 15, 9, 11, 7, 13, 12,
        11, 13, 6, 7, 14, 9, 13, 15, 14, 8, 13, 6, 5, 12, 7, 5,
        11, 12, 14, 15, 14, 15, 9, 8, 9, 14, 5, 6, 8, 6, 5, 12,
        9, 15, 5, 11, 6, 8, 13, 12, 5, 12, 13, 14, 11, 8, 5, 6
    )

    private val S2 = intArrayOf(
        8, 9, 9, 11, 13, 15, 15, 5, 7, 7, 8, 11, 14, 14, 12, 6,
        9, 13, 15, 7, 12, 8, 9, 11, 7, 7, 12, 7, 6, 15, 13, 11,
        9, 7, 15, 11, 8, 6, 6, 14, 12, 13, 5, 14, 13, 13, 7, 5,
        15, 5, 8, 11, 14, 14, 6, 14, 6, 9, 12, 9, 12, 5, 15, 8,
        8, 5, 12, 9, 12, 5, 14, 6, 8, 13, 6, 5, 15, 13, 11, 11
    )

    private fun rol(x: Int, s: Int): Int = (x shl s) or (x ushr (32 - s))

    private fun f(j: Int, x: Int, y: Int, z: Int): Int = when {
        j < 16 -> x xor y xor z
        j < 32 -> (x and y) or (x.inv() and z)
        j < 48 -> (x or y.inv()) xor z
        j < 64 -> (x and z) or (y and z.inv())
        else -> x xor (y or z.inv())
    }

    private fun k1(j: Int): Int = when {
        j < 16 -> 0x00000000
        j < 32 -> 0x5a827999
        j < 48 -> 0x6ed9eba1
        j < 64 -> 0x8f1bbcdc.toInt()
        else -> 0xa953fd4e.toInt()
    }

    private fun k2(j: Int): Int = when {
        j < 16 -> 0x50a28be6
        j < 32 -> 0x5c4dd124
        j < 48 -> 0x6d703ef3
        j < 64 -> 0x7a6d76e9
        else -> 0x00000000
    }

    fun digest(input: ByteArray): ByteArray {
        val bitLen = input.size.toLong() * 8L
        val padLen = ((56 - ((input.size + 1) % 64)) + 64) % 64
        val totalLen = input.size + 1 + padLen + 8
        val padded = ByteArray(totalLen)
        System.arraycopy(input, 0, padded, 0, input.size)
        padded[input.size] = 0x80.toByte()
        for (i in 0 until 8) {
            padded[totalLen - 8 + i] = ((bitLen ushr (i * 8)) and 0xFF).toByte()
        }

        var h0 = 0x67452301
        var h1 = 0xefcdab89.toInt()
        var h2 = 0x98badcfe.toInt()
        var h3 = 0x10325476
        var h4 = 0xc3d2e1f0.toInt()

        val x = IntArray(16)
        var offset = 0
        while (offset < padded.size) {
            for (i in 0 until 16) {
                val base = offset + i * 4
                x[i] = (padded[base].toInt() and 0xFF) or
                    ((padded[base + 1].toInt() and 0xFF) shl 8) or
                    ((padded[base + 2].toInt() and 0xFF) shl 16) or
                    ((padded[base + 3].toInt() and 0xFF) shl 24)
            }

            var a1 = h0
            var b1 = h1
            var c1 = h2
            var d1 = h3
            var e1 = h4

            var a2 = h0
            var b2 = h1
            var c2 = h2
            var d2 = h3
            var e2 = h4

            for (j in 0 until 80) {
                var t1 = a1 + f(j, b1, c1, d1) + x[R1[j]] + k1(j)
                t1 = rol(t1, S1[j]) + e1
                a1 = e1
                e1 = d1
                d1 = rol(c1, 10)
                c1 = b1
                b1 = t1

                var t2 = a2 + f(79 - j, b2, c2, d2) + x[R2[j]] + k2(j)
                t2 = rol(t2, S2[j]) + e2
                a2 = e2
                e2 = d2
                d2 = rol(c2, 10)
                c2 = b2
                b2 = t2
            }

            val t = h1 + c1 + d2
            h1 = h2 + d1 + e2
            h2 = h3 + e1 + a2
            h3 = h4 + a1 + b2
            h4 = h0 + b1 + c2
            h0 = t

            offset += 64
        }

        val out = ByteArray(20)
        val words = intArrayOf(h0, h1, h2, h3, h4)
        for (i in 0 until 5) {
            out[i * 4] = (words[i] and 0xFF).toByte()
            out[i * 4 + 1] = ((words[i] ushr 8) and 0xFF).toByte()
            out[i * 4 + 2] = ((words[i] ushr 16) and 0xFF).toByte()
            out[i * 4 + 3] = ((words[i] ushr 24) and 0xFF).toByte()
        }
        return out
    }
}
