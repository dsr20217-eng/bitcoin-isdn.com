package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.core.BitcoinChain
import com.example.core.RawTxParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

sealed interface TxParseState {
    data object Empty : TxParseState
    data class Parsed(val tx: RawTxParser.ParsedTransaction) : TxParseState
    data class Error(val message: String) : TxParseState
}

data class TxInspectorUiState(
    val rawHexInput: String = "",
    val selectedChain: BitcoinChain = BitcoinChain.MAINNET,
    val parseState: TxParseState = TxParseState.Empty
)

class TxInspectorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TxInspectorUiState())
    val uiState: StateFlow<TxInspectorUiState> = _uiState.asStateFlow()

    fun updateHexInput(hex: String) {
        _uiState.update { state ->
            state.copy(
                rawHexInput = hex,
                parseState = parse(hex, state.selectedChain)
            )
        }
    }

    fun selectChain(chain: BitcoinChain) {
        _uiState.update { state ->
            state.copy(
                selectedChain = chain,
                parseState = parse(state.rawHexInput, chain)
            )
        }
    }

    fun loadBip141TestVector() {
        updateHexInput(RawTxParser.BIP141_SAMPLE_HEX)
    }

    fun clearInput() {
        updateHexInput("")
    }

    private fun parse(hex: String, chain: BitcoinChain): TxParseState {
        if (hex.isBlank()) return TxParseState.Empty
        return RawTxParser.parseHex(hex, chain.bech32Hrp).fold(
            onSuccess = { TxParseState.Parsed(it) },
            onFailure = { TxParseState.Error(it.message ?: "Unable to parse raw transaction hex") }
        )
    }
}
