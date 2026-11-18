package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddLink
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Token
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NetworkPing
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.ProtocolNetworkSpec
import com.example.core.TokenStandardSpec
import com.example.data.local.RelayEndpointEntity
import com.example.ui.components.CopyableMonoBlock
import com.example.ui.components.KeyValueRow
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.CoreLensTheme
import com.example.ui.theme.MonoTextStyle
import com.example.ui.viewmodel.RelayDraft
import com.example.ui.viewmodel.RelaysSubTab
import com.example.ui.viewmodel.RelaysUiState
import com.example.ui.viewmodel.RelaysViewModel

@Composable
fun RelaysScreen(
    uiState: RelaysUiState,
    viewModel: RelaysViewModel,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SectionHeader(
                    title = "Relays, Connectors & A–Z Protocols",
                    subtitle = "Auto-saved Room endpoints, routing params & multi-chain RPC specs",
                    icon = Icons.Outlined.Router
                )
            }

            item {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("relays_subtab_row")
                ) {
                    RelaysSubTab.entries.forEachIndexed { idx, tab ->
                        SegmentedButton(
                            selected = uiState.activeTab == tab,
                            onClick = { viewModel.selectTab(tab) },
                            shape = SegmentedButtonDefaults.itemShape(idx, RelaysSubTab.entries.size),
                            modifier = Modifier.testTag("relays_tab_${tab.name.lowercase()}")
                        ) {
                            Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            when (uiState.activeTab) {
                RelaysSubTab.MY_RELAYS -> {
                    item {
                        Button(
                            onClick = viewModel::openNewRelayDialog,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("add_relay_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Add relay connector")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Relay / Webhook Connector")
                        }
                    }

                    if (uiState.savedRelays.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("No Saved Connectors Yet", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "Add a custom JSON-RPC server, REST API endpoint, or Webhook URL above—or browse \"Chains A–Z\" to pre-fill routing parameters for any blockchain protocol. All connectors auto-save to Room.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(uiState.savedRelays, key = { it.id }) { relay ->
                            SavedRelayCard(
                                relay = relay,
                                isProbing = relay.id in uiState.probingIds,
                                onProbe = { viewModel.probeRelay(relay) },
                                onEdit = { viewModel.openEditRelay(relay) },
                                onDelete = { viewModel.deleteRelay(relay.id) }
                            )
                        }
                    }
                }

                RelaysSubTab.CHAINS_AZ -> {
                    item {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::updateSearchQuery,
                            label = { Text("Filter A–Z chains, tickers, or RPC dialects") },
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Search chains") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chains_az_search")
                        )
                    }
                    items(uiState.filteredNetworks, key = { it.name }) { spec ->
                        ProtocolNetworkCard(
                            spec = spec,
                            onCreateRelay = { viewModel.openFromNetworkSpec(spec) }
                        )
                    }
                }

                RelaysSubTab.TOKENS_AZ -> {
                    item {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::updateSearchQuery,
                            label = { Text("Filter A–Z token & script specifications") },
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Search token standards") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tokens_az_search")
                        )
                    }
                    items(uiState.filteredTokenStandards, key = { it.standard }) { token ->
                        TokenStandardCard(spec = token)
                    }
                }
            }
        }
    }

    val draft = uiState.editorDraft
    if (draft != null) {
        RelayEditorDialog(
            draft = draft,
            onUpdate = viewModel::updateDraft,
            onDismiss = viewModel::dismissEditor,
            onSave = viewModel::saveDraft
        )
    }
}

@Composable
private fun SavedRelayCard(
    relay: RelayEndpointEntity,
    isProbing: Boolean,
    onProbe: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val ext = CoreLensTheme.extendedColors
    val isOk = relay.lastStatus.startsWith("HTTP 2")
    val isErr = relay.lastStatus == "Error" || relay.lastStatus.startsWith("HTTP 4") || relay.lastStatus.startsWith("HTTP 5")
    val pillColor = when {
        isOk -> ext.valid
        isErr -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("saved_relay_card_${relay.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(relay.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${relay.chainName} (${relay.ticker}) · ${relay.protocolType}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                StatusPill(
                    label = if (relay.lastLatencyMs != null) "${relay.lastStatus} · ${relay.lastLatencyMs}ms" else relay.lastStatus,
                    color = pillColor,
                    containerColor = pillColor.copy(alpha = 0.14f)
                )
            }

            Text(
                text = relay.endpointUrl.ifBlank { "(No endpoint URL configured)" },
                style = MonoTextStyle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "Method: ${relay.probeMethod} · Timeout: ${relay.routingTimeoutMs}ms · Retries: ${relay.retryCount}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (relay.lastResponseSnippet.isNotBlank()) {
                CopyableMonoBlock(
                    label = "Last Live Probe Response",
                    value = relay.lastResponseSnippet,
                    testTag = "relay_response_${relay.id}",
                    maxLines = 4
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onProbe,
                    enabled = !isProbing,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("probe_relay_${relay.id}")
                ) {
                    if (isProbing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.NetworkPing, contentDescription = "Ping relay")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isProbing) "Probing…" else "Live Ping")
                }
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("edit_relay_${relay.id}")
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit relay")
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("delete_relay_${relay.id}")
                ) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete relay",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun ProtocolNetworkCard(
    spec: ProtocolNetworkSpec,
    onCreateRelay: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Language,
                        contentDescription = spec.name,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(spec.name, style = MaterialTheme.typography.titleMedium)
                        Text(spec.family, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
                StatusPill(
                    label = spec.ticker,
                    color = MaterialTheme.colorScheme.primary,
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            KeyValueRow("Chain ID", spec.chainIdentifier, monoValue = true)
            KeyValueRow("Address Format", spec.addressEncoding)
            KeyValueRow("RPC Dialect", spec.defaultRpcDialect)
            KeyValueRow("Probe Method", spec.probeMethod, monoValue = true)
            KeyValueRow("Block Target", spec.blockTimeTarget)
            Text(
                text = spec.routingNotes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onCreateRelay,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("configure_chain_${spec.name.lowercase().replace(' ', '_')}")
            ) {
                Icon(Icons.Outlined.AddLink, contentDescription = "Configure relay")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Configure ${spec.name} Connector")
            }
        }
    }
}

@Composable
private fun TokenStandardCard(spec: TokenStandardSpec) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Token,
                        contentDescription = spec.standard,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(spec.standard, style = MaterialTheme.typography.titleMedium)
                }
                Text(spec.ecosystem, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            KeyValueRow("Encoding", spec.encodingModel)
            KeyValueRow("Interface", spec.interfaceSpec, monoValue = true)
            Text(
                text = spec.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RelayEditorDialog(
    draft: RelayDraft,
    onUpdate: ((RelayDraft) -> RelayDraft) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (draft.id == 0L) "New Relay Connector" else "Edit Connector")
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close dialog")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("JSON_RPC", "REST_GET", "WEBHOOK").forEach { proto ->
                        FilterChip(
                            selected = draft.protocolType == proto,
                            onClick = { onUpdate { it.copy(protocolType = proto) } },
                            label = { Text(proto) }
                        )
                    }
                }
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { v -> onUpdate { it.copy(name = v) } },
                    label = { Text("Connector Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.endpointUrl,
                    onValueChange = { v -> onUpdate { it.copy(endpointUrl = v) } },
                    label = { Text("Endpoint URL (https://... or http://...)") },
                    singleLine = true,
                    textStyle = MonoTextStyle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("relay_editor_url")
                )
                OutlinedTextField(
                    value = draft.probeMethod,
                    onValueChange = { v -> onUpdate { it.copy(probeMethod = v) } },
                    label = { Text("RPC Method / REST Path") },
                    singleLine = true,
                    textStyle = MonoTextStyle,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = draft.routingTimeoutMs,
                        onValueChange = { v -> onUpdate { it.copy(routingTimeoutMs = v) } },
                        label = { Text("Timeout ms") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = draft.retryCount,
                        onValueChange = { v -> onUpdate { it.copy(retryCount = v) } },
                        label = { Text("Retries (1-3)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = draft.authHeaderName,
                    onValueChange = { v -> onUpdate { it.copy(authHeaderName = v) } },
                    label = { Text("Auth Header Name (e.g. Authorization, X-API-Key)") },
                    singleLine = true,
                    textStyle = MonoTextStyle,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.authHeaderValue,
                    onValueChange = { v -> onUpdate { it.copy(authHeaderValue = v) } },
                    label = { Text("Auth Header Token / Key (stored locally)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.customHeadersText,
                    onValueChange = { v -> onUpdate { it.copy(customHeadersText = v) } },
                    label = { Text("Extra Routing Headers (Key: Value per line)") },
                    minLines = 2,
                    maxLines = 3,
                    textStyle = MonoTextStyle,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                modifier = Modifier.testTag("relay_editor_save_button")
            ) {
                Text("Auto-Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
