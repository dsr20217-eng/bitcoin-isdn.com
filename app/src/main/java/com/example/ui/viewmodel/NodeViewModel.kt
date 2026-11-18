package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.BitcoinChain
import com.example.data.local.CoreLensPreferences
import com.example.data.rpc.BitcoinRpcClient
import com.example.data.rpc.NodeTelemetryBundle
import com.example.data.rpc.RpcConnectionConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface TelemetryLoadState {
    data object Idle : TelemetryLoadState
    data object Loading : TelemetryLoadState
    data class Success(val bundle: NodeTelemetryBundle) : TelemetryLoadState
    data class Error(val message: String) : TelemetryLoadState
}

sealed interface RpcConsoleState {
    data object Idle : RpcConsoleState
    data object Executing : RpcConsoleState
    data class Result(val outputJson: String) : RpcConsoleState
    data class Error(val message: String) : RpcConsoleState
}

data class NodeUiState(
    val config: RpcConnectionConfig = RpcConnectionConfig(),
    val telemetryState: TelemetryLoadState = TelemetryLoadState.Idle,
    val netinfoLevel: Int = 4,
    val outboundOnly: Boolean = false,
    val activeSubTab: Int = 0,
    val rpcMethodInput: String = "getblockchaininfo",
    val rpcParamsInput: String = "",
    val rpcConsoleState: RpcConsoleState = RpcConsoleState.Idle,
    val showConnectionDialog: Boolean = false
)

class NodeViewModel(
    private val preferences: CoreLensPreferences? = null,
    private val rpcClient: BitcoinRpcClient = BitcoinRpcClient()
) : ViewModel() {

    private val _uiState = MutableStateFlow(NodeUiState())
    val uiState: StateFlow<NodeUiState> = _uiState.asStateFlow()

    init {
        if (preferences != null) {
            viewModelScope.launch {
                val saved = preferences.settingsFlow.first()
                val chain = BitcoinChain.entries.find { it.id == saved.lastChainId } ?: BitcoinChain.MAINNET
                _uiState.update {
                    it.copy(
                        config = it.config.copy(
                            hostUrl = saved.lastRpcHost,
                            port = saved.lastRpcPort,
                            chain = chain
                        )
                    )
                }
            }
        }
    }

    fun setSubTab(index: Int) {
        _uiState.update { it.copy(activeSubTab = index) }
    }

    fun setNetinfoLevel(level: Int) {
        _uiState.update { it.copy(netinfoLevel = level.coerceIn(0, 4)) }
    }

    fun setOutboundOnly(outboundOnly: Boolean) {
        _uiState.update { it.copy(outboundOnly = outboundOnly) }
    }

    fun openConnectionDialog(open: Boolean) {
        _uiState.update { it.copy(showConnectionDialog = open) }
    }

    fun updateConnectionConfig(
        hostUrl: String,
        portText: String,
        username: String,
        password: String,
        chain: BitcoinChain
    ) {
        val port = portText.toIntOrNull()?.coerceIn(1, 65535) ?: chain.rpcPort
        _uiState.update {
            it.copy(
                config = RpcConnectionConfig(
                    hostUrl = hostUrl,
                    port = port,
                    username = username,
                    password = password,
                    chain = chain
                ),
                showConnectionDialog = false
            )
        }
        viewModelScope.launch {
            preferences?.saveRpcEndpoint(hostUrl.trim(), port, chain.id)
        }
        refreshTelemetry()
    }

    fun refreshTelemetry() {
        val currentConfig = _uiState.value.config
        viewModelScope.launch {
            _uiState.update { it.copy(telemetryState = TelemetryLoadState.Loading) }
            rpcClient.fetchTelemetry(currentConfig)
                .onSuccess { bundle ->
                    _uiState.update { it.copy(telemetryState = TelemetryLoadState.Success(bundle)) }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            telemetryState = TelemetryLoadState.Error(
                                err.message ?: "Unable to connect to ${currentConfig.fullEndpointUrl()}"
                            )
                        )
                    }
                }
        }
    }

    fun updateRpcMethod(method: String) {
        _uiState.update { it.copy(rpcMethodInput = method) }
    }

    fun updateRpcParams(params: String) {
        _uiState.update { it.copy(rpcParamsInput = params) }
    }

    fun executeRpcCommand() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(rpcConsoleState = RpcConsoleState.Executing) }
            rpcClient.executeReadOnlyRpc(state.config, state.rpcMethodInput, state.rpcParamsInput)
                .onSuccess { json ->
                    _uiState.update { it.copy(rpcConsoleState = RpcConsoleState.Result(json)) }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(rpcConsoleState = RpcConsoleState.Error(err.message ?: "RPC execution failed"))
                    }
                }
        }
    }

    class Factory(private val preferences: CoreLensPreferences) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NodeViewModel(preferences = preferences) as T
        }
    }
}
