package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.BitcoinChain
import com.example.data.rpc.NetinfoSnapshot
import com.example.data.rpc.NodeTelemetryBundle
import com.example.data.rpc.PeerTelemetry
import com.example.data.rpc.RpcConnectionConfig
import com.example.ui.components.CopyableMonoBlock
import com.example.ui.components.KeyValueRow
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.TelemetryMetricCard
import com.example.ui.theme.CoreLensTheme
import com.example.ui.theme.MonoTextStyle
import com.example.ui.viewmodel.NodeUiState
import com.example.ui.viewmodel.NodeViewModel
import com.example.ui.viewmodel.RpcConsoleState
import com.example.ui.viewmodel.TelemetryLoadState
import java.util.Locale

@Composable
fun NodeScreen(
    uiState: NodeUiState,
    viewModel: NodeViewModel,
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
                NodeHeroCard(
                    config = uiState.config,
                    telemetryState = uiState.telemetryState,
                    onConfigureClick = { viewModel.openConnectionDialog(true) },
                    onRefreshClick = { viewModel.refreshTelemetry() }
                )
            }

            item {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("node_subtab_selector")
                ) {
                    SegmentedButton(
                        selected = uiState.activeSubTab == 0,
                        onClick = { viewModel.setSubTab(0) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("subtab_telemetry")
                    ) {
                        Text("Telemetry & -netinfo")
                    }
                    SegmentedButton(
                        selected = uiState.activeSubTab == 1,
                        onClick = { viewModel.setSubTab(1) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("subtab_rpc_console")
                    ) {
                        Text("RPC Console")
                    }
                }
            }

            if (uiState.activeSubTab == 0) {
                when (val tState = uiState.telemetryState) {
                    is TelemetryLoadState.Idle -> {
                        item {
                            NodeEmptyConnectionCard(
                                endpoint = uiState.config.fullEndpointUrl(),
                                onConfigure = { viewModel.openConnectionDialog(true) },
                                onConnect = { viewModel.refreshTelemetry() }
                            )
                        }
                    }

                    is TelemetryLoadState.Loading -> {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                    Text(
                                        text = "Querying JSON-RPC endpoint ${uiState.config.fullEndpointUrl()}…",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    is TelemetryLoadState.Error -> {
                        item {
                            NodeErrorCard(
                                endpoint = uiState.config.fullEndpointUrl(),
                                errorMessage = tState.message,
                                onEditEndpoint = { viewModel.openConnectionDialog(true) },
                                onRetry = { viewModel.refreshTelemetry() }
                            )
                        }
                    }

                    is TelemetryLoadState.Success -> {
                        val bundle = tState.bundle
                        item {
                            NodeOverviewSection(bundle = bundle)
                        }
                        item {
                            NetinfoControlsCard(
                                level = uiState.netinfoLevel,
                                outboundOnly = uiState.outboundOnly,
                                onLevelChange = viewModel::setNetinfoLevel,
                                onOutboundOnlyChange = viewModel::setOutboundOnly
                            )
                        }
                        item {
                            NetinfoSummaryMatrix(netinfo = bundle.netinfo)
                        }
                        item {
                            AddrinfoCard(bundle = bundle)
                        }
                        if (uiState.netinfoLevel >= 1) {
                            val filteredPeers = if (uiState.outboundOnly) {
                                bundle.netinfo.peers.filter { it.direction == "out" }
                            } else {
                                bundle.netinfo.peers
                            }
                            item {
                                SectionHeader(
                                    title = "Connected Peers (${filteredPeers.size})",
                                    subtitle = "Sorted by direction and minimum ping (level ${uiState.netinfoLevel})",
                                    icon = Icons.Outlined.Hub
                                )
                            }
                            items(filteredPeers, key = { it.id }) { peer ->
                                PeerTelemetryCard(
                                    peer = peer,
                                    level = uiState.netinfoLevel
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    RpcConsoleSection(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    if (uiState.showConnectionDialog) {
        RpcConnectionDialog(
            initialConfig = uiState.config,
            onDismiss = { viewModel.openConnectionDialog(false) },
            onSaveAndConnect = { host, port, user, pass, chain ->
                viewModel.updateConnectionConfig(host, port, user, pass, chain)
            }
        )
    }
}

@Composable
private fun NodeHeroCard(
    config: RpcConnectionConfig,
    telemetryState: TelemetryLoadState,
    onConfigureClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    val ext = CoreLensTheme.extendedColors
    val (statusLabel, statusColor, statusBg) = when (telemetryState) {
        is TelemetryLoadState.Success -> Triple(
            "Online · ${telemetryState.bundle.overview.chain.uppercase()}",
            ext.valid,
            ext.valid.copy(alpha = 0.14f)
        )
        is TelemetryLoadState.Loading -> Triple(
            "Syncing RPC",
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.secondaryContainer
        )
        is TelemetryLoadState.Error -> Triple(
            "RPC Unreachable",
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.errorContainer
        )
        is TelemetryLoadState.Idle -> Triple(
            "Standby · ${config.chain.displayName}",
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer
        )
    }

    Card(
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_node_hero),
                contentDescription = stringResource(id = R.string.hero_banner_desc),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.25f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(
                        label = statusLabel,
                        color = statusColor,
                        containerColor = statusBg
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = onConfigureClick,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("configure_rpc_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Configure RPC endpoint",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = onRefreshClick,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("refresh_telemetry_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Refresh node telemetry",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Column {
                    Text(
                        text = "CoreLens Node Telemetry",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = config.fullEndpointUrl(),
                        style = MonoTextStyle,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun NodeEmptyConnectionCard(
    endpoint: String,
    onConfigure: () -> Unit,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader(
                title = "Connect to Your Bitcoin Core Node",
                subtitle = "Read-only JSON-RPC telemetry (-getinfo, -netinfo, -addrinfo)",
                icon = Icons.Outlined.SettingsEthernet
            )
            Text(
                text = "Point CoreLens to a reachable bitcoind JSON-RPC endpoint ($endpoint) to inspect chain sync, mempool load, BIP-324 v2 transports, and peer connectivity.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onConfigure,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("empty_configure_rpc_button")
                ) {
                    Text("Configure RPC")
                }
                Button(
                    onClick = onConnect,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("empty_connect_rpc_button")
                ) {
                    Text("Connect Now")
                }
            }
        }
    }
}

@Composable
private fun NodeErrorCard(
    endpoint: String,
    errorMessage: String,
    onEditEndpoint: () -> Unit,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "JSON-RPC Connection Failed",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = "Endpoint: $endpoint",
                style = MonoTextStyle,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onEditEndpoint,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("error_edit_rpc_button")
                ) {
                    Text("Edit Endpoint")
                }
                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("error_retry_rpc_button")
                ) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
private fun NodeOverviewSection(bundle: NodeTelemetryBundle) {
    val ov = bundle.overview
    val syncPct = (ov.verificationProgress * 100.0).coerceIn(0.0, 100.0)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            title = "Node Overview (-getinfo)",
            subtitle = "${ov.subversion.ifEmpty { "Bitcoin Core" }} · proto ${ov.protocolVersion}",
            icon = Icons.Outlined.Storage
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TelemetryMetricCard(
                title = "Block Height",
                value = "%,d".format(ov.blocks),
                caption = "Headers: %,d".format(ov.headers),
                accentColor = MaterialTheme.colorScheme.primary,
                icon = Icons.Outlined.Storage,
                modifier = Modifier.weight(1f)
            )
            TelemetryMetricCard(
                title = "Connections",
                value = "${ov.connectionsTotal}",
                caption = "In ${ov.connectionsIn} · Out ${ov.connectionsOut}",
                accentColor = MaterialTheme.colorScheme.secondary,
                icon = Icons.Outlined.Hub,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TelemetryMetricCard(
                title = "Mempool",
                value = "%,d tx".format(ov.mempoolTxCount),
                caption = "%.2f MB".format(Locale.US, ov.mempoolBytes / 1_000_000.0),
                accentColor = CoreLensTheme.extendedColors.valid,
                icon = Icons.Outlined.Speed,
                modifier = Modifier.weight(1f)
            )
            TelemetryMetricCard(
                title = "Relay Fee",
                value = "%.5f".format(Locale.US, ov.relayFee),
                caption = "BTC/kvB floor",
                accentColor = MaterialTheme.colorScheme.tertiary,
                icon = Icons.Outlined.Lan,
                modifier = Modifier.weight(1f)
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Verification Progress",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "%.4f%%".format(Locale.US, syncPct),
                        style = MonoTextStyle,
                        color = CoreLensTheme.extendedColors.valid
                    )
                }
                LinearProgressIndicator(
                    progress = { (syncPct / 100.0).toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = CoreLensTheme.extendedColors.valid
                )
                if (ov.warnings.isNotBlank()) {
                    Text(
                        text = "Warnings: ${ov.warnings}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NetinfoControlsCard(
    level: Int,
    outboundOnly: Boolean,
    onLevelChange: (Int) -> Unit,
    onOutboundOnlyChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "-netinfo Detail Level",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                (0..4).forEach { lvl ->
                    FilterChip(
                        selected = level == lvl,
                        onClick = { onLevelChange(lvl) },
                        label = { Text("Level $lvl") },
                        modifier = Modifier.testTag("netinfo_level_$lvl")
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Outbound peers only (outonly)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(
                    checked = outboundOnly,
                    onCheckedChange = onOutboundOnlyChange,
                    enabled = level > 0,
                    modifier = Modifier.testTag("netinfo_outonly_switch")
                )
            }
        }
    }
}

@Composable
private fun NetinfoSummaryMatrix(netinfo: NetinfoSnapshot) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Peer Counts by Network",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (netinfo.networkRows.isEmpty()) {
                Text(
                    text = "No reachable network peer rows reported.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Net", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.4f))
                    Text("In", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text("Out", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text("Total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                netinfo.networkRows.forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(row.network, style = MonoTextStyle, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1.4f))
                        Text("${row.inbound}", style = MonoTextStyle, modifier = Modifier.weight(1f))
                        Text("${row.outbound}", style = MonoTextStyle, modifier = Modifier.weight(1f))
                        Text("${row.total}", style = MonoTextStyle.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text = "Block-relay-only: ${netinfo.blockRelayPeersCount} · Manual: ${netinfo.manualPeersCount}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (netinfo.localAddresses.isNotEmpty()) {
                netinfo.localAddresses.forEach { local ->
                    Text(
                        text = "Local: ${local.address}:${local.port} (score ${local.score})",
                        style = MonoTextStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AddrinfoCard(bundle: NodeTelemetryBundle) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Known Node Addresses (-addrinfo · ${bundle.addrinfo.total} total)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                bundle.addrinfo.byNetwork.forEach { (net, count) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(net, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$count", style = MonoTextStyle.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
private fun PeerTelemetryCard(
    peer: PeerTelemetry,
    level: Int
) {
    val dirColor = if (peer.direction == "out") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("peer_card_${peer.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(
                        label = "${peer.direction.uppercase()} · ${peer.connType}",
                        color = dirColor,
                        containerColor = dirColor.copy(alpha = 0.14f)
                    )
                    Text(
                        text = "${peer.network} · ${peer.transport}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "#${peer.id}",
                    style = MonoTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (level == 2 || level == 4) {
                Text(
                    text = peer.address,
                    style = MonoTextStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (level == 3 || level == 4) {
                Text(
                    text = "${peer.version}${peer.subversion}",
                    style = MonoTextStyle,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ping: ${peer.pingMs ?: "-"}ms (min ${peer.minPingMs ?: "-"}ms)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "serv: ${peer.services.ifEmpty { "-" }} · age: ${peer.ageMin}m",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RpcConsoleSection(
    uiState: NodeUiState,
    viewModel: NodeViewModel
) {
    val quickMethods = listOf(
        "getblockchaininfo",
        "getnetworkinfo",
        "getmempoolinfo",
        "getpeerinfo",
        "getblockcount",
        "help"
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            title = "Read-Only JSON-RPC Console",
            subtitle = "Execute inspection RPCs against ${uiState.config.fullEndpointUrl()}",
            icon = Icons.Outlined.Terminal
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickMethods.forEach { method ->
                FilterChip(
                    selected = uiState.rpcMethodInput.equals(method, ignoreCase = true),
                    onClick = { viewModel.updateRpcMethod(method) },
                    label = { Text(method) },
                    modifier = Modifier.testTag("rpc_preset_$method")
                )
            }
        }
        OutlinedTextField(
            value = uiState.rpcMethodInput,
            onValueChange = viewModel::updateRpcMethod,
            label = { Text("RPC Method") },
            singleLine = true,
            textStyle = MonoTextStyle,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("rpc_method_input")
        )
        OutlinedTextField(
            value = uiState.rpcParamsInput,
            onValueChange = viewModel::updateRpcParams,
            label = { Text("Params (JSON array or comma-separated)") },
            placeholder = { Text("e.g. 0 or [\"00000000...\"]") },
            singleLine = true,
            textStyle = MonoTextStyle,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("rpc_params_input")
        )
        Button(
            onClick = viewModel::executeRpcCommand,
            enabled = uiState.rpcConsoleState !is RpcConsoleState.Executing,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("rpc_execute_button")
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = "Execute RPC")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Execute Read-Only RPC")
        }

        when (val cState = uiState.rpcConsoleState) {
            is RpcConsoleState.Idle -> {
                Text(
                    text = "Wallet, key, signing, broadcasting, and mining RPCs are disabled in read-only mode.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            is RpcConsoleState.Executing -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Text("Sending JSON-RPC request…", style = MaterialTheme.typography.bodyMedium)
                }
            }
            is RpcConsoleState.Error -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = cState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            is RpcConsoleState.Result -> {
                CopyableMonoBlock(
                    label = "JSON-RPC Response",
                    value = cState.outputJson,
                    testTag = "rpc_response_block"
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RpcConnectionDialog(
    initialConfig: RpcConnectionConfig,
    onDismiss: () -> Unit,
    onSaveAndConnect: (String, String, String, String, BitcoinChain) -> Unit
) {
    var hostUrl by remember { mutableStateOf(initialConfig.hostUrl) }
    var portText by remember { mutableStateOf(initialConfig.port.toString()) }
    var username by remember { mutableStateOf(initialConfig.username) }
    var password by remember { mutableStateOf(initialConfig.password) }
    var selectedChain by remember { mutableStateOf(initialConfig.chain) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("JSON-RPC Endpoint")
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close dialog")
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BitcoinChain.entries.forEach { chain ->
                        FilterChip(
                            selected = selectedChain == chain,
                            onClick = {
                                selectedChain = chain
                                portText = chain.rpcPort.toString()
                            },
                            label = { Text(chain.displayName) },
                            modifier = Modifier.testTag("rpc_chain_${chain.id}")
                        )
                    }
                }
                OutlinedTextField(
                    value = hostUrl,
                    onValueChange = { hostUrl = it },
                    label = { Text("Host / URL") },
                    singleLine = true,
                    textStyle = MonoTextStyle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rpc_host_input")
                )
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it },
                    label = { Text("RPC Port") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MonoTextStyle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rpc_port_input")
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("RPC Username (optional)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rpc_user_input")
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("RPC Password (optional)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rpc_password_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveAndConnect(hostUrl, portText, username, password, selectedChain) },
                modifier = Modifier.testTag("rpc_save_connect_button")
            ) {
                Text("Save & Connect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
