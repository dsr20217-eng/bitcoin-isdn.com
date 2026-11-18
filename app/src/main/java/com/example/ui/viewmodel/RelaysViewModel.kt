package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.BlockchainDirectory
import com.example.core.ProtocolNetworkSpec
import com.example.core.TokenStandardSpec
import com.example.data.local.CoreLensRepository
import com.example.data.local.RelayEndpointEntity
import com.example.data.rpc.RelayNetworkClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RelaysSubTab(val label: String) {
    MY_RELAYS("My Relays & Webhooks"),
    CHAINS_AZ("Chains A–Z"),
    TOKENS_AZ("Token Specs A–Z")
}

data class RelayDraft(
    val id: Long = 0L,
    val name: String = "",
    val chainName: String = "Bitcoin",
    val ticker: String = "BTC",
    val protocolType: String = "JSON_RPC",
    val endpointUrl: String = "",
    val probeMethod: String = "getblockchaininfo",
    val probeParamsJson: String = "[]",
    val routingTimeoutMs: String = "8000",
    val retryCount: String = "1",
    val authHeaderName: String = "",
    val authHeaderValue: String = "",
    val customHeadersText: String = ""
)

data class RelaysUiState(
    val activeTab: RelaysSubTab = RelaysSubTab.MY_RELAYS,
    val searchQuery: String = "",
    val savedRelays: List<RelayEndpointEntity> = emptyList(),
    val filteredNetworks: List<ProtocolNetworkSpec> = BlockchainDirectory.networksAZ,
    val filteredTokenStandards: List<TokenStandardSpec> = BlockchainDirectory.tokenStandardsAZ,
    val probingIds: Set<Long> = emptySet(),
    val editorDraft: RelayDraft? = null
)

class RelaysViewModel(
    private val repository: CoreLensRepository,
    private val networkClient: RelayNetworkClient = RelayNetworkClient()
) : ViewModel() {

    private val localState = MutableStateFlow(RelaysUiState())

    val uiState: StateFlow<RelaysUiState> = combine(
        localState,
        repository.relays
    ) { state, relays ->
        val q = state.searchQuery.trim().lowercase()
        val nets = if (q.isEmpty()) {
            BlockchainDirectory.networksAZ
        } else {
            BlockchainDirectory.networksAZ.filter {
                it.name.lowercase().contains(q) ||
                    it.ticker.lowercase().contains(q) ||
                    it.family.lowercase().contains(q) ||
                    it.defaultRpcDialect.lowercase().contains(q)
            }
        }
        val tokens = if (q.isEmpty()) {
            BlockchainDirectory.tokenStandardsAZ
        } else {
            BlockchainDirectory.tokenStandardsAZ.filter {
                it.standard.lowercase().contains(q) ||
                    it.ecosystem.lowercase().contains(q) ||
                    it.encodingModel.lowercase().contains(q)
            }
        }
        state.copy(
            savedRelays = relays,
            filteredNetworks = nets,
            filteredTokenStandards = tokens
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RelaysUiState()
    )

    fun selectTab(tab: RelaysSubTab) {
        localState.update { it.copy(activeTab = tab) }
    }

    fun updateSearchQuery(query: String) {
        localState.update { it.copy(searchQuery = query) }
    }

    fun openNewRelayDialog() {
        localState.update {
            it.copy(
                editorDraft = RelayDraft(
                    name = "Custom Relay",
                    chainName = "Bitcoin",
                    ticker = "BTC",
                    protocolType = "JSON_RPC",
                    endpointUrl = "http://127.0.0.1:8332",
                    probeMethod = "getblockchaininfo"
                )
            )
        }
    }

    fun openFromNetworkSpec(spec: ProtocolNetworkSpec) {
        val protoType = if (spec.probeMethod.startsWith("GET ")) "REST_GET" else "JSON_RPC"
        localState.update {
            it.copy(
                editorDraft = RelayDraft(
                    name = "${spec.name} Relay",
                    chainName = spec.name,
                    ticker = spec.ticker,
                    protocolType = protoType,
                    endpointUrl = "",
                    probeMethod = spec.probeMethod,
                    probeParamsJson = spec.probeParamsJson
                )
            )
        }
    }

    fun openEditRelay(entity: RelayEndpointEntity) {
        localState.update {
            it.copy(
                editorDraft = RelayDraft(
                    id = entity.id,
                    name = entity.name,
                    chainName = entity.chainName,
                    ticker = entity.ticker,
                    protocolType = entity.protocolType,
                    endpointUrl = entity.endpointUrl,
                    probeMethod = entity.probeMethod,
                    probeParamsJson = entity.probeParamsJson,
                    routingTimeoutMs = entity.routingTimeoutMs.toString(),
                    retryCount = entity.retryCount.toString(),
                    authHeaderName = entity.authHeaderName,
                    authHeaderValue = entity.authHeaderValue,
                    customHeadersText = entity.customHeadersText
                )
            )
        }
    }

    fun updateDraft(transform: (RelayDraft) -> RelayDraft) {
        localState.update { state ->
            state.copy(editorDraft = state.editorDraft?.let(transform))
        }
    }

    fun dismissEditor() {
        localState.update { it.copy(editorDraft = null) }
    }

    fun saveDraft() {
        val draft = localState.value.editorDraft ?: return
        viewModelScope.launch {
            repository.saveRelay(
                RelayEndpointEntity(
                    id = draft.id,
                    name = draft.name.ifBlank { "${draft.chainName} Endpoint" },
                    chainName = draft.chainName.ifBlank { "Custom" },
                    ticker = draft.ticker.ifBlank { "RPC" },
                    protocolType = draft.protocolType,
                    endpointUrl = draft.endpointUrl.trim(),
                    probeMethod = draft.probeMethod.trim(),
                    probeParamsJson = draft.probeParamsJson.ifBlank { "[]" },
                    routingTimeoutMs = draft.routingTimeoutMs.toIntOrNull()?.coerceIn(1000, 60000) ?: 8000,
                    retryCount = draft.retryCount.toIntOrNull()?.coerceIn(1, 3) ?: 1,
                    authHeaderName = draft.authHeaderName.trim(),
                    authHeaderValue = draft.authHeaderValue.trim(),
                    customHeadersText = draft.customHeadersText.trim(),
                    updatedAt = System.currentTimeMillis()
                )
            )
            localState.update { it.copy(editorDraft = null) }
        }
    }

    fun deleteRelay(id: Long) {
        viewModelScope.launch {
            repository.deleteRelay(id)
        }
    }

    fun probeRelay(entity: RelayEndpointEntity) {
        viewModelScope.launch {
            localState.update { it.copy(probingIds = it.probingIds + entity.id) }
            val result = networkClient.probeEndpoint(entity)
            repository.saveRelay(
                entity.copy(
                    lastStatus = result.statusLabel,
                    lastLatencyMs = result.latencyMs,
                    lastResponseSnippet = result.responseSnippet,
                    updatedAt = System.currentTimeMillis()
                )
            )
            localState.update { it.copy(probingIds = it.probingIds - entity.id) }
        }
    }

    class Factory(private val repository: CoreLensRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RelaysViewModel(repository) as T
        }
    }
}
