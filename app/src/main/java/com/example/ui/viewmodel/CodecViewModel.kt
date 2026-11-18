package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.core.Base58Codec
import com.example.core.Bech32Codec
import com.example.core.BitcoinCrypto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class CodecTab(val label: String) {
    BECH32("Bech32 / Bech32m"),
    BASE58("Base58Check"),
    HASH("Hash Digest")
}

data class HashDigestOutputs(
    val inputByteLength: Int = 0,
    val sha256: String = "",
    val doubleSha256: String = "",
    val hash160: String = "",
    val sha3_256: String = "",
    val sha512: String = "",
    val error: String? = null
)

data class CodecUiState(
    val activeTab: CodecTab = CodecTab.BECH32,
    // Bech32 / Bech32m inspector + encoder
    val bech32Input: String = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",
    val bech32Inspection: Bech32Codec.SegwitInspection = Bech32Codec.inspectSegwitAddress("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"),
    val encodeHrp: String = "bc",
    val encodeWitnessVer: Int = 0,
    val encodeProgramHex: String = "751e76e8199196d454941c45d1b3a323f1433bd6",
    val encodedBech32Output: String? = Bech32Codec.encodeSegwitAddress(
        "bc",
        0,
        BitcoinCrypto.hexToBytesOrNull("751e76e8199196d454941c45d1b3a323f1433bd6")!!
    ),
    val encodeError: String? = null,
    // Base58 / Base58Check
    val base58Input: String = "1BgGZ9tcN4rm9KBzDn7KprQz87SZ26SAMH",
    val base58Inspection: Base58Codec.Base58CheckInspection = Base58Codec.inspectCheck("1BgGZ9tcN4rm9KBzDn7KprQz87SZ26SAMH"),
    val base58RawHexInput: String = "00751e76e8199196d454941c45d1b3a323f1433bd6",
    val base58EncodedOutput: String = "",
    val base58EncodeError: String? = null,
    // Hash Digest tool
    val hashInputText: String = "Bitcoin Core",
    val hashInputIsHex: Boolean = false,
    val reverseByteOrder: Boolean = false,
    val hashOutputs: HashDigestOutputs = HashDigestOutputs()
)

class CodecViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        CodecUiState().let { initial ->
            initial.copy(
                base58EncodedOutput = encodeBase58FromHex(initial.base58RawHexInput).first,
                hashOutputs = computeHashes(initial.hashInputText, initial.hashInputIsHex, initial.reverseByteOrder)
            )
        }
    )
    val uiState: StateFlow<CodecUiState> = _uiState.asStateFlow()

    fun selectTab(tab: CodecTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun updateBech32Input(input: String) {
        val inspection = Bech32Codec.inspectSegwitAddress(input)
        _uiState.update { it.copy(bech32Input = input, bech32Inspection = inspection) }
    }

    fun updateEncodeParams(hrp: String, witnessVersion: Int, programHex: String) {
        val cleanHrp = hrp.trim().lowercase()
        val bytes = BitcoinCrypto.hexToBytesOrNull(programHex)
        val (encoded, err) = when {
            cleanHrp.isEmpty() -> null to "HRP cannot be empty (e.g. bc, tb, bcrt)"
            bytes == null -> null to "Witness program must be valid even-length hex"
            witnessVersion !in 0..16 -> null to "Witness version must be 0..16"
            bytes.size !in 2..40 -> null to "Witness program must be 2..40 bytes (got ${bytes.size})"
            witnessVersion == 0 && bytes.size != 20 && bytes.size != 32 ->
                null to "SegWit v0 requires 20 bytes (P2WPKH) or 32 bytes (P2WSH)"
            else -> Bech32Codec.encodeSegwitAddress(cleanHrp, witnessVersion, bytes) to null
        }
        _uiState.update {
            it.copy(
                encodeHrp = hrp,
                encodeWitnessVer = witnessVersion,
                encodeProgramHex = programHex,
                encodedBech32Output = encoded,
                encodeError = err
            )
        }
    }

    fun updateBase58Input(input: String) {
        val inspection = Base58Codec.inspectCheck(input)
        _uiState.update { it.copy(base58Input = input, base58Inspection = inspection) }
    }

    fun updateBase58RawHex(hex: String) {
        val (encoded, err) = encodeBase58FromHex(hex)
        _uiState.update {
            it.copy(
                base58RawHexInput = hex,
                base58EncodedOutput = encoded,
                base58EncodeError = err
            )
        }
    }

    fun updateHashInput(text: String, isHex: Boolean, reverseBytes: Boolean) {
        val outputs = computeHashes(text, isHex, reverseBytes)
        _uiState.update {
            it.copy(
                hashInputText = text,
                hashInputIsHex = isHex,
                reverseByteOrder = reverseBytes,
                hashOutputs = outputs
            )
        }
    }

    private fun encodeBase58FromHex(hex: String): Pair<String, String?> {
        val bytes = BitcoinCrypto.hexToBytesOrNull(hex)
            ?: return "" to "Payload must be valid hexadecimal"
        if (bytes.isEmpty()) return "" to "Enter at least 1 payload byte"
        return Base58Codec.encodeCheck(bytes) to null
    }

    private fun computeHashes(text: String, isHex: Boolean, reverseBytes: Boolean): HashDigestOutputs {
        val bytes = if (isHex) {
            BitcoinCrypto.hexToBytesOrNull(text)
                ?: return HashDigestOutputs(error = "Invalid hexadecimal input")
        } else {
            text.toByteArray(Charsets.UTF_8)
        }

        fun fmt(digest: ByteArray): String =
            if (reverseBytes) BitcoinCrypto.bytesToReversedHex(digest) else BitcoinCrypto.bytesToHex(digest)

        return HashDigestOutputs(
            inputByteLength = bytes.size,
            sha256 = fmt(BitcoinCrypto.sha256(bytes)),
            doubleSha256 = fmt(BitcoinCrypto.doubleSha256(bytes)),
            hash160 = fmt(BitcoinCrypto.hash160(bytes)),
            sha3_256 = fmt(BitcoinCrypto.sha3_256(bytes)),
            sha512 = fmt(BitcoinCrypto.sha512(bytes)),
            error = null
        )
    }
}
