package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.core.BitcoinChain
import com.example.core.ChainSpecsRepository
import com.example.core.CompactTargetResult
import com.example.core.RawTxParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal
import java.math.RoundingMode

data class ChainSpecsUiState(
    val selectedChain: BitcoinChain = BitcoinChain.MAINNET,
    val btcInput: String = "1.0",
    val satsOutput: String = "100000000",
    val weightInput: String = "400",
    val vbytesOutput: String = "100",
    val nBitsInput: String = "1d00ffff",
    val targetResult: CompactTargetResult = ChainSpecsRepository.decodeCompactNBits("1d00ffff")
)

class ChainSpecsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChainSpecsUiState())
    val uiState: StateFlow<ChainSpecsUiState> = _uiState.asStateFlow()

    fun selectChain(chain: BitcoinChain) {
        _uiState.update { it.copy(selectedChain = chain) }
    }

    fun updateBtcInput(btc: String) {
        val sats = runCatching {
            val bd = BigDecimal(btc.trim())
            bd.multiply(BigDecimal(RawTxParser.COIN))
                .setScale(0, RoundingMode.DOWN)
                .toPlainString()
        }.getOrElse { "Invalid BTC value" }
        _uiState.update { it.copy(btcInput = btc, satsOutput = sats) }
    }

    fun updateSatsInput(sats: String) {
        val btc = runCatching {
            val bd = BigDecimal(sats.trim())
            bd.divide(BigDecimal(RawTxParser.COIN), 8, RoundingMode.UNNECESSARY)
                .stripTrailingZeros()
                .let { if (it.scale() < 0) it.setScale(0) else it }
                .toPlainString()
        }.getOrElse { "Invalid sats value" }
        _uiState.update { it.copy(satsOutput = sats, btcInput = btc) }
    }

    fun updateWeightInput(weight: String) {
        val vbytes = weight.trim().toLongOrNull()?.let { wu ->
            ((wu + 3L) / 4L).toString()
        } ?: "Invalid WU"
        _uiState.update { it.copy(weightInput = weight, vbytesOutput = vbytes) }
    }

    fun updateNBitsInput(nBits: String) {
        _uiState.update {
            it.copy(
                nBitsInput = nBits,
                targetResult = ChainSpecsRepository.decodeCompactNBits(nBits)
            )
        }
    }
}
