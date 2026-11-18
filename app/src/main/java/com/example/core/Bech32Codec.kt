package com.example.core

/**
 * Direct Kotlin port of Bitcoin Core's src/bech32.cpp (BIP-173 & BIP-350),
 * including GF(1024) syndrome error localization and SegWit witness program decoding.
 */
object Bech32Codec {

    enum class Encoding(val constant: Int, val label: String) {
        INVALID(0, "Invalid"),
        BECH32(1, "Bech32 (BIP-173)"),
        BECH32M(0x2bc830a3, "Bech32m (BIP-350)")
    }

    data class DecodeResult(
        val encoding: Encoding,
        val hrp: String,
        val data: IntArray
    )

    data class SegwitInspection(
        val valid: Boolean,
        val encoding: Encoding,
        val hrp: String,
        val networkLabel: String,
        val witnessVersion: Int?,
        val witnessProgramHex: String,
        val witnessProgramLength: Int,
        val scriptTypeLabel: String,
        val errorMessage: String? = null,
        val errorIndices: List<Int> = emptyList()
    )

    private const val CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"
    private const val SEPARATOR = '1'
    private const val CHECKSUM_SIZE = 6
    private const val CHAR_LIMIT_BIP173 = 90

    private val CHARSET_REV = IntArray(128) { -1 }.apply {
        for (i in CHARSET.indices) {
            val ch = CHARSET[i]
            this[ch.code] = i
            if (ch in 'a'..'z') {
                this[ch.uppercaseChar().code] = i
            }
        }
    }

    private val gf1024Exp = IntArray(1023)
    private val gf1024Log = IntArray(1024) { -1 }
    private val syndromeConsts = IntArray(25)

    init {
        val gf32Exp = IntArray(31)
        val gf32Log = IntArray(32) { -1 }
        val fmod = 41
        gf32Exp[0] = 1
        gf32Log[1] = 0
        var v = 1
        for (i in 1 until 31) {
            v = v shl 1
            if ((v and 32) != 0) v = v xor fmod
            gf32Exp[i] = v
            gf32Log[v] = i
        }

        gf1024Exp[0] = 1
        gf1024Log[1] = 0
        v = 1
        for (i in 1 until 1023) {
            val v0 = v and 31
            val v1 = v ushr 5
            val v0n = if (v1 != 0) gf32Exp[(gf32Log[v1] + gf32Log[23]) % 31] else 0
            val v1n = (if (v1 != 0) gf32Exp[(gf32Log[v1] + gf32Log[9]) % 31] else 0) xor v0
            v = (v1n shl 5) or v0n
            gf1024Exp[i] = v
            gf1024Log[v] = i
        }

        for (k in 1 until 6) {
            for (shift in 0 until 5) {
                val b = gf1024Log[1 shl shift]
                val c0 = gf1024Exp[(997 * k + b) % 1023]
                val c1 = gf1024Exp[(998 * k + b) % 1023]
                val c2 = gf1024Exp[(999 * k + b) % 1023]
                syndromeConsts[5 * (k - 1) + shift] = (c2 shl 20) or (c1 shl 10) or c0
            }
        }
    }

    private fun polyMod(values: IntArray): Int {
        var c = 1
        for (vi in values) {
            val c0 = c ushr 25
            c = ((c and 0x1ffffff) shl 5) xor vi
            if ((c0 and 1) != 0) c = c xor 0x3b6a57b2
            if ((c0 and 2) != 0) c = c xor 0x26508e6d
            if ((c0 and 4) != 0) c = c xor 0x1ea119fa
            if ((c0 and 8) != 0) c = c xor 0x3d4233dd
            if ((c0 and 16) != 0) c = c xor 0x2a1462b3
        }
        return c
    }

    private fun syndrome(residue: Int): Int {
        val low = residue and 0x1f
        var result = low xor (low shl 10) xor (low shl 20)
        for (i in 0 until 25) {
            if (((residue ushr (5 + i)) and 1) != 0) {
                result = result xor syndromeConsts[i]
            }
        }
        return result
    }

    private fun preparePolynomialCoefficients(hrp: String, values: IntArray): IntArray {
        val ret = IntArray(hrp.length * 2 + 1 + values.size)
        for (i in hrp.indices) {
            ret[i] = hrp[i].code ushr 5
        }
        ret[hrp.length] = 0
        for (i in hrp.indices) {
            ret[hrp.length + 1 + i] = hrp[i].code and 0x1f
        }
        for (i in values.indices) {
            ret[hrp.length * 2 + 1 + i] = values[i]
        }
        return ret
    }

    private fun verifyChecksum(hrp: String, values: IntArray): Encoding {
        val enc = preparePolynomialCoefficients(hrp, values)
        val check = polyMod(enc)
        return when (check) {
            Encoding.BECH32.constant -> Encoding.BECH32
            Encoding.BECH32M.constant -> Encoding.BECH32M
            else -> Encoding.INVALID
        }
    }

    private fun createChecksum(encoding: Encoding, hrp: String, values: IntArray): IntArray {
        val padded = IntArray(values.size + CHECKSUM_SIZE)
        System.arraycopy(values, 0, padded, 0, values.size)
        val enc = preparePolynomialCoefficients(hrp, padded)
        val mod = polyMod(enc) xor encoding.constant
        return IntArray(CHECKSUM_SIZE) { i ->
            (mod ushr (5 * (5 - i))) and 31
        }
    }

    fun encode(encoding: Encoding, hrp: String, values: IntArray): String {
        val lowerHrp = hrp.lowercase()
        val sb = StringBuilder(lowerHrp.length + 1 + values.size + CHECKSUM_SIZE)
        sb.append(lowerHrp)
        sb.append(SEPARATOR)
        for (v in values) sb.append(CHARSET[v])
        for (v in createChecksum(encoding, lowerHrp, values)) sb.append(CHARSET[v])
        return sb.toString()
    }

    fun decode(str: String, limit: Int = CHAR_LIMIT_BIP173): DecodeResult {
        if (str.length > limit || str.isEmpty()) return DecodeResult(Encoding.INVALID, "", IntArray(0))
        var hasLower = false
        var hasUpper = false
        for (ch in str) {
            if (ch.code < 33 || ch.code > 126) return DecodeResult(Encoding.INVALID, "", IntArray(0))
            if (ch in 'a'..'z') hasLower = true
            if (ch in 'A'..'Z') hasUpper = true
        }
        if (hasLower && hasUpper) return DecodeResult(Encoding.INVALID, "", IntArray(0))

        val pos = str.lastIndexOf(SEPARATOR)
        if (pos <= 0 || pos + CHECKSUM_SIZE >= str.length) {
            return DecodeResult(Encoding.INVALID, "", IntArray(0))
        }
        val values = IntArray(str.length - 1 - pos)
        for (i in values.indices) {
            val c = str[i + pos + 1].code
            val rev = if (c < 128) CHARSET_REV[c] else -1
            if (rev == -1) return DecodeResult(Encoding.INVALID, "", IntArray(0))
            values[i] = rev
        }
        val hrp = str.substring(0, pos).lowercase()
        val encoding = verifyChecksum(hrp, values)
        if (encoding == Encoding.INVALID) return DecodeResult(Encoding.INVALID, "", IntArray(0))
        return DecodeResult(encoding, hrp, values.copyOfRange(0, values.size - CHECKSUM_SIZE))
    }

    fun locateErrors(str: String, limit: Int = CHAR_LIMIT_BIP173): Pair<String, List<Int>> {
        val errorLocations = mutableListOf<Int>()
        if (str.length > limit) {
            for (i in limit until str.length) errorLocations.add(i)
            return "Bech32 string too long (max $limit chars)" to errorLocations
        }

        var lower = false
        var upper = false
        for (i in str.indices) {
            val c = str[i].code
            when {
                c in 'a'.code..'z'.code -> if (upper) errorLocations.add(i) else lower = true
                c in 'A'.code..'Z'.code -> if (lower) errorLocations.add(i) else upper = true
                c < 33 || c > 126 -> errorLocations.add(i)
            }
        }
        if (errorLocations.isNotEmpty()) {
            return "Invalid character or mixed case" to errorLocations
        }

        val pos = str.lastIndexOf(SEPARATOR)
        if (pos == -1) return "Missing separator '1'" to emptyList()
        if (pos == 0 || pos + CHECKSUM_SIZE >= str.length) {
            return "Invalid separator position" to listOf(pos)
        }

        val hrp = str.substring(0, pos).lowercase()
        val length = str.length - 1 - pos
        val values = IntArray(length)
        for (i in pos + 1 until str.length) {
            val c = str[i].code
            val rev = if (c < 128) CHARSET_REV[c] else -1
            if (rev == -1) {
                return "Invalid Bech32 character '${str[i]}'" to listOf(i)
            }
            values[i - pos - 1] = rev
        }

        var bestErrors = emptyList<Int>()
        var errorEncoding: Encoding? = null

        for (encoding in listOf(Encoding.BECH32, Encoding.BECH32M)) {
            val possibleErrors = mutableListOf<Int>()
            val enc = preparePolynomialCoefficients(hrp, values)
            val residue = polyMod(enc) xor encoding.constant
            if (residue == 0) {
                return "" to emptyList()
            }

            val syn = syndrome(residue)
            val s0 = syn and 0x3FF
            val s1 = (syn ushr 10) and 0x3FF
            val s2 = syn ushr 20

            val ls0 = gf1024Log[s0]
            val ls1 = gf1024Log[s1]
            val ls2 = gf1024Log[s2]

            if (ls0 != -1 && ls1 != -1 && ls2 != -1 && (2 * ls1 - ls2 - ls0 + 2046) % 1023 == 0) {
                val p1 = (ls1 - ls0 + 1023) % 1023
                val le1 = ls0 + (1023 - 997) * p1
                if (p1 < length && le1 % 33 == 0) {
                    possibleErrors.add(str.length - p1 - 1)
                }
            } else {
                for (p1 in 0 until length) {
                    val s2s1p1 = s2 xor (if (s1 == 0) 0 else gf1024Exp[(ls1 + p1) % 1023])
                    if (s2s1p1 == 0) continue
                    val ls2s1p1 = gf1024Log[s2s1p1]

                    val s1s0p1 = s1 xor (if (s0 == 0) 0 else gf1024Exp[(ls0 + p1) % 1023])
                    if (s1s0p1 == 0) continue
                    val ls1s0p1 = gf1024Log[s1s0p1]

                    val p2 = (ls2s1p1 - ls1s0p1 + 1023) % 1023
                    if (p2 >= length || p1 == p2) continue

                    val s1s0p2 = s1 xor (if (s0 == 0) 0 else gf1024Exp[(ls0 + p2) % 1023])
                    if (s1s0p2 == 0) continue
                    val ls1s0p2 = gf1024Log[s1s0p2]

                    val invP1P2 = 1023 - gf1024Log[gf1024Exp[p1] xor gf1024Exp[p2]]
                    val le2 = ls1s0p1 + invP1P2 + (1023 - 997) * p2
                    if (le2 % 33 != 0) continue

                    val le1 = ls1s0p2 + invP1P2 + (1023 - 997) * p1
                    if (le1 % 33 != 0) continue

                    if (p1 > p2) {
                        possibleErrors.add(str.length - p1 - 1)
                        possibleErrors.add(str.length - p2 - 1)
                    } else {
                        possibleErrors.add(str.length - p2 - 1)
                        possibleErrors.add(str.length - p1 - 1)
                    }
                    break
                }
            }

            if (bestErrors.isEmpty() || (possibleErrors.isNotEmpty() && possibleErrors.size < bestErrors.size)) {
                bestErrors = possibleErrors
                if (bestErrors.isNotEmpty()) errorEncoding = encoding
            }
        }

        val msg = when (errorEncoding) {
            Encoding.BECH32M -> "Invalid Bech32m checksum"
            Encoding.BECH32 -> "Invalid Bech32 checksum"
            else -> "Invalid checksum"
        }
        return msg to bestErrors
    }

    fun convertBits(data: IntArray, fromBits: Int, toBits: Int, pad: Boolean): ByteArray? {
        var acc = 0
        var bits = 0
        val ret = mutableListOf<Byte>()
        val maxv = (1 shl toBits) - 1
        val maxAcc = (1 shl (fromBits + toBits - 1)) - 1
        for (value in data) {
            if (value < 0 || (value ushr fromBits) != 0) return null
            acc = ((acc shl fromBits) or value) and maxAcc
            bits += fromBits
            while (bits >= toBits) {
                bits -= toBits
                ret.add(((acc ushr bits) and maxv).toByte())
            }
        }
        if (pad) {
            if (bits > 0) {
                ret.add(((acc shl (toBits - bits)) and maxv).toByte())
            }
        } else if (bits >= fromBits || ((acc shl (toBits - bits)) and maxv) != 0) {
            return null
        }
        return ret.toByteArray()
    }

    fun encodeSegwitAddress(hrp: String, witnessVersion: Int, program: ByteArray): String? {
        if (witnessVersion < 0 || witnessVersion > 16) return null
        if (program.size < 2 || program.size > 40) return null
        if (witnessVersion == 0 && program.size != 20 && program.size != 32) return null
        val encoding = if (witnessVersion == 0) Encoding.BECH32 else Encoding.BECH32M
        val fiveBitBytes = convertBits(IntArray(program.size) { program[it].toInt() and 0xFF }, 8, 5, true)
            ?: return null
        val values = IntArray(1 + fiveBitBytes.size)
        values[0] = witnessVersion
        for (i in fiveBitBytes.indices) values[i + 1] = fiveBitBytes[i].toInt() and 0xFF
        return encode(encoding, hrp, values)
    }

    fun inspectSegwitAddress(address: String): SegwitInspection {
        val trimmed = address.trim()
        val decoded = decode(trimmed)
        if (decoded.encoding == Encoding.INVALID) {
            val (errMsg, errIdx) = locateErrors(trimmed)
            return SegwitInspection(
                valid = false,
                encoding = Encoding.INVALID,
                hrp = "",
                networkLabel = "Unknown",
                witnessVersion = null,
                witnessProgramHex = "",
                witnessProgramLength = 0,
                scriptTypeLabel = "Invalid",
                errorMessage = errMsg,
                errorIndices = errIdx
            )
        }
        if (decoded.data.isEmpty()) {
            return SegwitInspection(
                valid = false,
                encoding = decoded.encoding,
                hrp = decoded.hrp,
                networkLabel = networkForHrp(decoded.hrp),
                witnessVersion = null,
                witnessProgramHex = "",
                witnessProgramLength = 0,
                scriptTypeLabel = "Invalid",
                errorMessage = "Empty witness data payload"
            )
        }
        val witVer = decoded.data[0]
        if (witVer > 16) {
            return SegwitInspection(
                valid = false,
                encoding = decoded.encoding,
                hrp = decoded.hrp,
                networkLabel = networkForHrp(decoded.hrp),
                witnessVersion = witVer,
                witnessProgramHex = "",
                witnessProgramLength = 0,
                scriptTypeLabel = "Invalid",
                errorMessage = "Witness version $witVer out of range (0..16)"
            )
        }
        val prog = convertBits(decoded.data.copyOfRange(1, decoded.data.size), 5, 8, false)
            ?: return SegwitInspection(
                valid = false,
                encoding = decoded.encoding,
                hrp = decoded.hrp,
                networkLabel = networkForHrp(decoded.hrp),
                witnessVersion = witVer,
                witnessProgramHex = "",
                witnessProgramLength = 0,
                scriptTypeLabel = "Invalid",
                errorMessage = "Invalid 5-to-8 bit padding"
            )
        if (prog.size < 2 || prog.size > 40) {
            return SegwitInspection(
                valid = false,
                encoding = decoded.encoding,
                hrp = decoded.hrp,
                networkLabel = networkForHrp(decoded.hrp),
                witnessVersion = witVer,
                witnessProgramHex = BitcoinCrypto.bytesToHex(prog),
                witnessProgramLength = prog.size,
                scriptTypeLabel = "Invalid",
                errorMessage = "Witness program length ${prog.size} must be 2..40 bytes"
            )
        }
        if (witVer == 0 && prog.size != 20 && prog.size != 32) {
            return SegwitInspection(
                valid = false,
                encoding = decoded.encoding,
                hrp = decoded.hrp,
                networkLabel = networkForHrp(decoded.hrp),
                witnessVersion = witVer,
                witnessProgramHex = BitcoinCrypto.bytesToHex(prog),
                witnessProgramLength = prog.size,
                scriptTypeLabel = "Invalid",
                errorMessage = "SegWit v0 witness program must be 20 or 32 bytes"
            )
        }
        if (witVer == 0 && decoded.encoding != Encoding.BECH32) {
            return SegwitInspection(
                valid = false,
                encoding = decoded.encoding,
                hrp = decoded.hrp,
                networkLabel = networkForHrp(decoded.hrp),
                witnessVersion = witVer,
                witnessProgramHex = BitcoinCrypto.bytesToHex(prog),
                witnessProgramLength = prog.size,
                scriptTypeLabel = "Invalid",
                errorMessage = "SegWit v0 requires Bech32 (BIP-173), not Bech32m"
            )
        }
        if (witVer != 0 && decoded.encoding != Encoding.BECH32M) {
            return SegwitInspection(
                valid = false,
                encoding = decoded.encoding,
                hrp = decoded.hrp,
                networkLabel = networkForHrp(decoded.hrp),
                witnessVersion = witVer,
                witnessProgramHex = BitcoinCrypto.bytesToHex(prog),
                witnessProgramLength = prog.size,
                scriptTypeLabel = "Invalid",
                errorMessage = "SegWit v$witVer requires Bech32m (BIP-350), not Bech32"
            )
        }

        val scriptType = when {
            witVer == 0 && prog.size == 20 -> "P2WPKH (v0, 20-byte)"
            witVer == 0 && prog.size == 32 -> "P2WSH (v0, 32-byte)"
            witVer == 1 && prog.size == 32 -> "P2TR Taproot (v1, 32-byte)"
            else -> "SegWit v$witVer (${prog.size}-byte)"
        }

        return SegwitInspection(
            valid = true,
            encoding = decoded.encoding,
            hrp = decoded.hrp,
            networkLabel = networkForHrp(decoded.hrp),
            witnessVersion = witVer,
            witnessProgramHex = BitcoinCrypto.bytesToHex(prog),
            witnessProgramLength = prog.size,
            scriptTypeLabel = scriptType
        )
    }

    private fun networkForHrp(hrp: String): String = when (hrp.lowercase()) {
        "bc" -> "Mainnet"
        "tb" -> "Testnet / Testnet4 / Signet"
        "bcrt" -> "Regtest"
        else -> "Custom HRP ($hrp)"
    }
}
