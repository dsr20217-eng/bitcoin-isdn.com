package com.example.core

/**
 * Direct Kotlin port of Bitcoin Core's src/base58.cpp.
 */
object Base58Codec {

    private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    private val MAP_BASE58 = IntArray(256) { -1 }.apply {
        for (i in ALPHABET.indices) {
            this[ALPHABET[i].code] = i
        }
    }

    data class Base58CheckInspection(
        val valid: Boolean,
        val payload: ByteArray,
        val checksumHex: String,
        val expectedChecksumHex: String,
        val versionByte: Int?,
        val prefixDescription: String,
        val error: String? = null
    )

    fun encode(input: ByteArray): String {
        var zeroes = 0
        var idx = 0
        while (idx < input.size && input[idx].toInt() == 0) {
            zeroes++
            idx++
        }
        val remaining = input.size - idx
        val size = remaining * 138 / 100 + 1
        val b58 = IntArray(size)
        var length = 0

        for (p in idx until input.size) {
            var carry = input[p].toInt() and 0xFF
            var i = 0
            var j = size - 1
            while ((carry != 0 || i < length) && j >= 0) {
                carry += 256 * b58[j]
                b58[j] = carry % 58
                carry /= 58
                j--
                i++
            }
            length = i
        }

        var it = size - length
        while (it < size && b58[it] == 0) {
            it++
        }

        val sb = StringBuilder(zeroes + (size - it))
        repeat(zeroes) { sb.append('1') }
        while (it < size) {
            sb.append(ALPHABET[b58[it]])
            it++
        }
        return sb.toString()
    }

    fun decode(input: String, maxRetLen: Int = 1024): ByteArray? {
        val trimmed = input.trim()
        var pos = 0
        var zeroes = 0
        while (pos < trimmed.length && trimmed[pos] == '1') {
            zeroes++
            if (zeroes > maxRetLen) return null
            pos++
        }

        val size = (trimmed.length - pos) * 733 / 1000 + 1
        val b256 = IntArray(size)
        var length = 0

        while (pos < trimmed.length) {
            val ch = trimmed[pos].code
            val carryInit = if (ch < 256) MAP_BASE58[ch] else -1
            if (carryInit == -1) return null
            var carry = carryInit
            var i = 0
            var j = size - 1
            while ((carry != 0 || i < length) && j >= 0) {
                carry += 58 * b256[j]
                b256[j] = carry % 256
                carry /= 256
                j--
                i++
            }
            length = i
            if (length + zeroes > maxRetLen) return null
            pos++
        }

        val start = size - length
        val out = ByteArray(zeroes + (size - start))
        var outIdx = zeroes
        for (i in start until size) {
            out[outIdx++] = b256[i].toByte()
        }
        return out
    }

    fun encodeCheck(payload: ByteArray): String {
        val hash = BitcoinCrypto.doubleSha256(payload)
        val combined = ByteArray(payload.size + 4)
        System.arraycopy(payload, 0, combined, 0, payload.size)
        System.arraycopy(hash, 0, combined, payload.size, 4)
        return encode(combined)
    }

    fun inspectCheck(input: String): Base58CheckInspection {
        val raw = decode(input, maxRetLen = 256)
            ?: return Base58CheckInspection(
                valid = false,
                payload = ByteArray(0),
                checksumHex = "",
                expectedChecksumHex = "",
                versionByte = null,
                prefixDescription = "Unknown",
                error = "Invalid Base58 characters or length"
            )
        if (raw.size < 4) {
            return Base58CheckInspection(
                valid = false,
                payload = ByteArray(0),
                checksumHex = "",
                expectedChecksumHex = "",
                versionByte = null,
                prefixDescription = "Unknown",
                error = "Decoded data shorter than 4-byte checksum"
            )
        }
        val payload = raw.copyOfRange(0, raw.size - 4)
        val actualChecksum = raw.copyOfRange(raw.size - 4, raw.size)
        val expectedChecksum = BitcoinCrypto.doubleSha256(payload).copyOfRange(0, 4)
        val valid = actualChecksum.contentEquals(expectedChecksum)
        val version = if (payload.isNotEmpty()) payload[0].toInt() and 0xFF else null
        return Base58CheckInspection(
            valid = valid,
            payload = payload,
            checksumHex = BitcoinCrypto.bytesToHex(actualChecksum),
            expectedChecksumHex = BitcoinCrypto.bytesToHex(expectedChecksum),
            versionByte = version,
            prefixDescription = describeBase58Payload(payload),
            error = if (valid) null else "4-byte Double-SHA256 checksum mismatch"
        )
    }

    private fun describeBase58Payload(payload: ByteArray): String {
        if (payload.isEmpty()) return "Empty payload"
        if (payload.size >= 4) {
            val prefix4 = BitcoinCrypto.bytesToHex(payload.copyOfRange(0, 4))
            when (prefix4) {
                "0488b21e" -> return "Mainnet Extended Key Prefix (xpub)"
                "043587cf" -> return "Testnet Extended Key Prefix (tpub)"
            }
        }
        return when (payload[0].toInt() and 0xFF) {
            0x00 -> if (payload.size == 21) "Mainnet P2PKH Script Hash (0x00)" else "Prefix 0x00 (${payload.size} bytes)"
            0x05 -> if (payload.size == 21) "Mainnet P2SH Script Hash (0x05)" else "Prefix 0x05 (${payload.size} bytes)"
            0x6F -> if (payload.size == 21) "Testnet/Signet P2PKH (0x6F)" else "Prefix 0x6F (${payload.size} bytes)"
            0xC4 -> if (payload.size == 21) "Testnet/Signet P2SH (0xC4)" else "Prefix 0xC4 (${payload.size} bytes)"
            else -> "Version 0x${(payload[0].toInt() and 0xFF).toString(16).padStart(2, '0')} (${payload.size} bytes)"
        }
    }
}
