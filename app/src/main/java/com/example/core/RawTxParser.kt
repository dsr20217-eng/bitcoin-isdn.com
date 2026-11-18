package com.example.core

import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Read-only Bitcoin raw transaction binary/hex parser matching
 * src/primitives/transaction.h, src/core_read.cpp, and BIP-141/144/125.
 * Does NOT generate keys, store wallets, sign, or broadcast transactions.
 */
object RawTxParser {

    const val COIN: Long = 100_000_000L
    const val WITNESS_SCALE_FACTOR: Int = 4

    data class TxInput(
        val index: Int,
        val prevTxid: String,
        val prevVout: Long,
        val isCoinbase: Boolean,
        val scriptSigHex: String,
        val scriptSigSize: Int,
        val sequenceHex: String,
        val sequence: Long,
        val rbfOptIn: Boolean,
        val witnessItems: List<String>
    )

    data class TxOutput(
        val index: Int,
        val valueSatoshis: Long,
        val valueBtcFormatted: String,
        val scriptPubKeyHex: String,
        val scriptType: String,
        val decodedAddress: String?,
        val opReturnText: String?
    )

    data class ParsedTransaction(
        val version: Int,
        val isSegwit: Boolean,
        val txid: String,
        val wtxid: String,
        val totalBytes: Int,
        val strippedBytes: Int,
        val weightUnits: Int,
        val virtualBytes: Int,
        val locktime: Long,
        val locktimeDescription: String,
        val totalOutputSatoshis: Long,
        val totalOutputBtcFormatted: String,
        val inputs: List<TxInput>,
        val outputs: List<TxOutput>
    )

    private class ByteReader(private val data: ByteArray) {
        var pos: Int = 0
            private set

        fun remaining(): Int = data.size - pos

        fun readByte(): Int {
            if (pos >= data.size) throw IllegalArgumentException("Unexpected end of transaction bytes at offset $pos")
            return data[pos++].toInt() and 0xFF
        }

        fun peekByte(): Int {
            if (pos >= data.size) throw IllegalArgumentException("Unexpected end of transaction bytes at offset $pos")
            return data[pos].toInt() and 0xFF
        }

        fun readBytes(count: Int): ByteArray {
            if (count < 0 || pos + count > data.size) {
                throw IllegalArgumentException("Requested $count bytes at offset $pos, only ${remaining()} remaining")
            }
            val slice = data.copyOfRange(pos, pos + count)
            pos += count
            return slice
        }

        fun readUInt16LE(): Int {
            val b0 = readByte()
            val b1 = readByte()
            return b0 or (b1 shl 8)
        }

        fun readUInt32LE(): Long {
            val b0 = readByte().toLong()
            val b1 = readByte().toLong()
            val b2 = readByte().toLong()
            val b3 = readByte().toLong()
            return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
        }

        fun readInt32LE(): Int {
            return readUInt32LE().toInt()
        }

        fun readInt64LE(): Long {
            var res = 0L
            for (i in 0 until 8) {
                res = res or (readByte().toLong() shl (i * 8))
            }
            return res
        }

        fun readCompactSize(): Int {
            val ch = readByte()
            val value = when {
                ch < 0xFD -> ch.toLong()
                ch == 0xFD -> readUInt16LE().toLong()
                ch == 0xFE -> readUInt32LE()
                else -> readInt64LE()
            }
            if (value < 0 || value > 4_000_000L) {
                throw IllegalArgumentException("CompactSize value $value exceeds safe transaction limit")
            }
            return value.toInt()
        }
    }

    private fun writeCompactSize(out: ByteArrayOutputStream, value: Int) {
        when {
            value < 0xFD -> out.write(value)
            value <= 0xFFFF -> {
                out.write(0xFD)
                out.write(value and 0xFF)
                out.write((value ushr 8) and 0xFF)
            }
            else -> {
                out.write(0xFE)
                out.write(value and 0xFF)
                out.write((value ushr 8) and 0xFF)
                out.write((value ushr 16) and 0xFF)
                out.write((value ushr 24) and 0xFF)
            }
        }
    }

    private fun writeUInt32LE(out: ByteArrayOutputStream, value: Long) {
        out.write((value and 0xFF).toInt())
        out.write(((value ushr 8) and 0xFF).toInt())
        out.write(((value ushr 16) and 0xFF).toInt())
        out.write(((value ushr 24) and 0xFF).toInt())
    }

    private fun writeInt64LE(out: ByteArrayOutputStream, value: Long) {
        for (i in 0 until 8) {
            out.write(((value ushr (i * 8)) and 0xFF).toInt())
        }
    }

    fun parseHex(hex: String, hrp: String = "bc"): Result<ParsedTransaction> {
        return runCatching {
            val rawBytes = BitcoinCrypto.hexToBytesOrNull(hex)
                ?: throw IllegalArgumentException("Input is not valid even-length hexadecimal")
            if (rawBytes.size < 10) {
                throw IllegalArgumentException("Transaction hex too short (${rawBytes.size} bytes)")
            }

            val reader = ByteReader(rawBytes)
            val strippedOut = ByteArrayOutputStream()

            val version = reader.readInt32LE()
            writeUInt32LE(strippedOut, version.toLong() and 0xFFFFFFFFL)

            var isSegwit = false
            var inputCount = reader.readCompactSize()
            if (inputCount == 0 && reader.remaining() > 0 && reader.peekByte() == 0x01) {
                // BIP-141 / BIP-144 SegWit marker (0x00) + flag (0x01)
                reader.readByte()
                isSegwit = true
                inputCount = reader.readCompactSize()
            }

            writeCompactSize(strippedOut, inputCount)

            val rawInputs = mutableListOf<TxInput>()
            for (i in 0 until inputCount) {
                val prevHashBytes = reader.readBytes(32)
                strippedOut.write(prevHashBytes)
                val prevTxid = BitcoinCrypto.bytesToReversedHex(prevHashBytes)

                val prevVout = reader.readUInt32LE()
                writeUInt32LE(strippedOut, prevVout)

                val scriptSigLen = reader.readCompactSize()
                writeCompactSize(strippedOut, scriptSigLen)
                val scriptSigBytes = reader.readBytes(scriptSigLen)
                strippedOut.write(scriptSigBytes)

                val sequence = reader.readUInt32LE()
                writeUInt32LE(strippedOut, sequence)

                val isCoinbase = prevHashBytes.all { it.toInt() == 0 } && prevVout == 0xFFFFFFFFL
                val rbfOptIn = !isCoinbase && sequence < 0xFFFFFFFEL

                rawInputs.add(
                    TxInput(
                        index = i,
                        prevTxid = prevTxid,
                        prevVout = prevVout,
                        isCoinbase = isCoinbase,
                        scriptSigHex = BitcoinCrypto.bytesToHex(scriptSigBytes),
                        scriptSigSize = scriptSigLen,
                        sequenceHex = "0x" + sequence.toString(16).padStart(8, '0'),
                        sequence = sequence,
                        rbfOptIn = rbfOptIn,
                        witnessItems = emptyList()
                    )
                )
            }

            val outputCount = reader.readCompactSize()
            writeCompactSize(strippedOut, outputCount)

            val outputs = mutableListOf<TxOutput>()
            var totalSats = 0L
            for (i in 0 until outputCount) {
                val valueSat = reader.readInt64LE()
                writeInt64LE(strippedOut, valueSat)
                totalSats += valueSat

                val scriptPubKeyLen = reader.readCompactSize()
                writeCompactSize(strippedOut, scriptPubKeyLen)
                val scriptPubKeyBytes = reader.readBytes(scriptPubKeyLen)
                strippedOut.write(scriptPubKeyBytes)

                val classified = classifyScriptPubKey(scriptPubKeyBytes, hrp)
                outputs.add(
                    TxOutput(
                        index = i,
                        valueSatoshis = valueSat,
                        valueBtcFormatted = formatSatoshisToBtc(valueSat),
                        scriptPubKeyHex = BitcoinCrypto.bytesToHex(scriptPubKeyBytes),
                        scriptType = classified.first,
                        decodedAddress = classified.second,
                        opReturnText = classified.third
                    )
                )
            }

            val inputsWithWitness = if (isSegwit) {
                rawInputs.map { input ->
                    val stackCount = reader.readCompactSize()
                    val items = mutableListOf<String>()
                    repeat(stackCount) {
                        val itemLen = reader.readCompactSize()
                        val itemBytes = reader.readBytes(itemLen)
                        items.add(BitcoinCrypto.bytesToHex(itemBytes))
                    }
                    input.copy(witnessItems = items)
                }
            } else {
                rawInputs
            }

            val locktime = reader.readUInt32LE()
            writeUInt32LE(strippedOut, locktime)

            if (reader.remaining() != 0) {
                throw IllegalArgumentException("Trailing unparsed bytes (${reader.remaining()} bytes left)")
            }

            val strippedBytes = strippedOut.toByteArray()
            val txid = BitcoinCrypto.bytesToReversedHex(BitcoinCrypto.doubleSha256(strippedBytes))
            val wtxid = BitcoinCrypto.bytesToReversedHex(BitcoinCrypto.doubleSha256(rawBytes))

            val totalSize = rawBytes.size
            val strippedSize = strippedBytes.size
            val weightUnits = strippedSize * (WITNESS_SCALE_FACTOR - 1) + totalSize
            val vsize = (weightUnits + WITNESS_SCALE_FACTOR - 1) / WITNESS_SCALE_FACTOR

            val locktimeDesc = when {
                locktime == 0L -> "0 (Immediate)"
                locktime < 500_000_000L -> "Block Height #$locktime"
                else -> "Unix Timestamp $locktime"
            }

            ParsedTransaction(
                version = version,
                isSegwit = isSegwit,
                txid = txid,
                wtxid = wtxid,
                totalBytes = totalSize,
                strippedBytes = strippedSize,
                weightUnits = weightUnits,
                virtualBytes = vsize,
                locktime = locktime,
                locktimeDescription = locktimeDesc,
                totalOutputSatoshis = totalSats,
                totalOutputBtcFormatted = formatSatoshisToBtc(totalSats),
                inputs = inputsWithWitness,
                outputs = outputs
            )
        }
    }

    fun formatSatoshisToBtc(sats: Long): String {
        return BigDecimal(sats)
            .divide(BigDecimal(COIN), 8, RoundingMode.UNNECESSARY)
            .stripTrailingZeros()
            .let { if (it.scale() < 0) it.setScale(0) else it }
            .toPlainString() + " BTC"
    }

    private fun classifyScriptPubKey(script: ByteArray, hrp: String): Triple<String, String?, String?> {
        // P2PKH: OP_DUP OP_HASH160 <20> <20-byte-hash> OP_EQUALVERIFY OP_CHECKSIG
        if (script.size == 25 &&
            (script[0].toInt() and 0xFF) == 0x76 &&
            (script[1].toInt() and 0xFF) == 0xA9 &&
            (script[2].toInt() and 0xFF) == 0x14 &&
            (script[23].toInt() and 0xFF) == 0x88 &&
            (script[24].toInt() and 0xFF) == 0xAC
        ) {
            val prefix = if (hrp == "bc") 0x00.toByte() else 0x6F.toByte()
            val payload = byteArrayOf(prefix) + script.copyOfRange(3, 23)
            return Triple("P2PKH (Legacy)", Base58Codec.encodeCheck(payload), null)
        }

        // P2SH: OP_HASH160 <20> <20-byte-hash> OP_EQUAL
        if (script.size == 23 &&
            (script[0].toInt() and 0xFF) == 0xA9 &&
            (script[1].toInt() and 0xFF) == 0x14 &&
            (script[22].toInt() and 0xFF) == 0x87
        ) {
            val prefix = if (hrp == "bc") 0x05.toByte() else 0xC4.toByte()
            val payload = byteArrayOf(prefix) + script.copyOfRange(2, 22)
            return Triple("P2SH", Base58Codec.encodeCheck(payload), null)
        }

        // P2WPKH: OP_0 <20> <20-byte-hash>
        if (script.size == 22 &&
            (script[0].toInt() and 0xFF) == 0x00 &&
            (script[1].toInt() and 0xFF) == 0x14
        ) {
            val addr = Bech32Codec.encodeSegwitAddress(hrp, 0, script.copyOfRange(2, 22))
            return Triple("P2WPKH (SegWit v0)", addr, null)
        }

        // P2WSH: OP_0 <32> <32-byte-hash>
        if (script.size == 34 &&
            (script[0].toInt() and 0xFF) == 0x00 &&
            (script[1].toInt() and 0xFF) == 0x20
        ) {
            val addr = Bech32Codec.encodeSegwitAddress(hrp, 0, script.copyOfRange(2, 34))
            return Triple("P2WSH (SegWit v0)", addr, null)
        }

        // P2TR: OP_1 <32> <32-byte-xonly-pubkey>
        if (script.size == 34 &&
            (script[0].toInt() and 0xFF) == 0x51 &&
            (script[1].toInt() and 0xFF) == 0x20
        ) {
            val addr = Bech32Codec.encodeSegwitAddress(hrp, 1, script.copyOfRange(2, 34))
            return Triple("P2TR (Taproot v1)", addr, null)
        }

        // OP_RETURN null data
        if (script.isNotEmpty() && (script[0].toInt() and 0xFF) == 0x6A) {
            val text = decodeOpReturnAscii(script)
            return Triple("OP_RETURN (Null Data)", null, text)
        }

        return Triple("Custom Script (${script.size} bytes)", null, null)
    }

    private fun decodeOpReturnAscii(script: ByteArray): String {
        if (script.size <= 2) return ""
        val sb = StringBuilder()
        var i = 1
        while (i < script.size) {
            val op = script[i].toInt() and 0xFF
            val len = when {
                op in 1..75 -> {
                    i++
                    op
                }
                op == 0x4C && i + 1 < script.size -> {
                    val l = script[i + 1].toInt() and 0xFF
                    i += 2
                    l
                }
                else -> {
                    i++
                    0
                }
            }
            if (len > 0 && i + len <= script.size) {
                for (j in i until i + len) {
                    val ch = script[j].toInt() and 0xFF
                    sb.append(if (ch in 32..126) ch.toChar() else '.')
                }
                i += len
            }
        }
        return sb.toString()
    }

    // Standard public BIP-141 SegWit protocol specification test vector (read-only protocol demonstration)
    const val BIP141_SAMPLE_HEX =
        "02000000000101809986d6089635675a6953953d10d92887d526e9704628d5274814807f3494610000000000fdffffff0240420f00000000001600147f9b1a7fb68d60c536c2fd8aeaa53a8f3cc025a8204e0000000000001600148d7e5c5f8a22f3c8c02b2a4a7d9d4f23f4d98e6a0247304402204e45e16932b8af514961a1d3a1a25fdf3f4f7732e9d624c6c61548ab5fb8cd410220181522ec8eca07de4860a4acdd12909d831cc56cbbac4622082221a8768d1d09012103247f712a9881d18f710d1db93c06c92c1f8bb45b6be7c9a68b4340e1f8459f5600000000"
}
