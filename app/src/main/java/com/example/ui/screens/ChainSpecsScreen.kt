package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.automirrored.outlined.Rule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.BitcoinChain
import com.example.core.ChainSpecsRepository
import com.example.ui.components.CopyableMonoBlock
import com.example.ui.components.KeyValueRow
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.CoreLensTheme
import com.example.ui.theme.MonoTextStyle
import com.example.ui.viewmodel.ChainSpecsUiState
import com.example.ui.viewmodel.ChainSpecsViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChainSpecsScreen(
    uiState: ChainSpecsUiState,
    viewModel: ChainSpecsViewModel,
    modifier: Modifier = Modifier
) {
    val chain = uiState.selectedChain
    val ext = CoreLensTheme.extendedColors

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
                    title = "Chain Parameters & Consensus",
                    subtitle = "Ported from chainparamsbase.cpp, kernel/chainparams.cpp & consensus.h",
                    icon = Icons.Outlined.Dns
                )
            }

            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BitcoinChain.entries.forEach { item ->
                        FilterChip(
                            selected = item == chain,
                            onClick = { viewModel.selectChain(item) },
                            label = { Text(item.displayName) },
                            modifier = Modifier.testTag("chain_chip_${item.id}")
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(chain.displayName, style = MaterialTheme.typography.titleLarge)
                            StatusPill(
                                label = chain.cliFlag,
                                color = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        }
                        Text(
                            text = chain.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        KeyValueRow("P2P Port", "${chain.p2pPort}", monoValue = true)
                        KeyValueRow("JSON-RPC Port", "${chain.rpcPort}", monoValue = true)
                        KeyValueRow("Tor Onion Port", "${chain.torPort}", monoValue = true)
                        KeyValueRow("Message Magic", chain.messageStartHex, monoValue = true)
                        KeyValueRow("Bech32 HRP", chain.bech32Hrp, monoValue = true)
                        KeyValueRow("Base58 P2PKH", chain.base58P2pkhPrefix, monoValue = true)
                        KeyValueRow("Base58 P2SH", chain.base58P2shPrefix, monoValue = true)
                        KeyValueRow("BIP-32 Pub", chain.bip32PubPrefix, monoValue = true)
                        KeyValueRow("BIP-94 Timewarp", if (chain.bip94Timewarp) "Enforced" else "Disabled")
                        KeyValueRow("Genesis UTC", chain.genesisTimeUtc, monoValue = true)
                        CopyableMonoBlock("Genesis Block Hash", chain.genesisHash, testTag = "chain_genesis_hash")
                    }
                }
            }

            item {
                SectionHeader(
                    title = "Protocol Calculators",
                    subtitle = "Unit conversion, BIP-141 weight, and arith_uint256 nBits target",
                    icon = Icons.Outlined.Calculate
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.btcInput,
                                onValueChange = viewModel::updateBtcInput,
                                label = { Text("BTC") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                textStyle = MonoTextStyle,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("calc_btc_input")
                            )
                            OutlinedTextField(
                                value = uiState.satsOutput,
                                onValueChange = viewModel::updateSatsInput,
                                label = { Text("Satoshis") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = MonoTextStyle,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("calc_sats_input")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.weightInput,
                                onValueChange = viewModel::updateWeightInput,
                                label = { Text("Weight Units (WU)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = MonoTextStyle,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("calc_weight_input")
                            )
                            OutlinedTextField(
                                value = uiState.vbytesOutput,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Virtual Bytes (vB)") },
                                singleLine = true,
                                textStyle = MonoTextStyle,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("calc_vbytes_output")
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        OutlinedTextField(
                            value = uiState.nBitsInput,
                            onValueChange = viewModel::updateNBitsInput,
                            label = { Text("Compact Difficulty nBits (Hex)") },
                            singleLine = true,
                            textStyle = MonoTextStyle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("calc_nbits_input")
                        )

                        if (uiState.targetResult.error != null) {
                            Text(
                                text = uiState.targetResult.error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatusPill(
                                    label = if (uiState.targetResult.valid) "Valid Target" else "Invalid Target",
                                    color = if (uiState.targetResult.valid) ext.valid else MaterialTheme.colorScheme.error,
                                    containerColor = if (uiState.targetResult.valid) ext.valid.copy(alpha = 0.14f) else MaterialTheme.colorScheme.errorContainer
                                )
                            }
                            CopyableMonoBlock(
                                label = "256-bit Target (Big-Endian Hex)",
                                value = uiState.targetResult.targetHex,
                                testTag = "calc_nbits_target"
                            )
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = "Consensus Constants",
                    subtitle = "src/consensus/consensus.h & amount.h",
                    icon = Icons.AutoMirrored.Outlined.Rule
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ChainSpecsRepository.consensusConstants.forEachIndexed { idx, constant ->
                            KeyValueRow(
                                keyLabel = constant.name,
                                valueText = "${constant.value}\n${constant.description}",
                                monoValue = false
                            )
                            if (idx < ChainSpecsRepository.consensusConstants.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
