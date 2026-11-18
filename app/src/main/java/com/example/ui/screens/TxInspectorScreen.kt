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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.BitcoinChain
import com.example.core.RawTxParser
import com.example.ui.components.CopyableMonoBlock
import com.example.ui.components.KeyValueRow
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.CoreLensTheme
import com.example.ui.theme.MonoTextStyle
import com.example.ui.viewmodel.TxInspectorUiState
import com.example.ui.viewmodel.TxInspectorViewModel
import com.example.ui.viewmodel.TxParseState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TxInspectorScreen(
    uiState: TxInspectorUiState,
    viewModel: TxInspectorViewModel,
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
                    title = "Read-Only Raw Tx Hex Inspector",
                    subtitle = "Ported from bitcoin-tx.cpp & core_read.cpp (no key storage or signing)",
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Address Prefix Context",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(BitcoinChain.MAINNET, BitcoinChain.TESTNET4, BitcoinChain.REGTEST).forEach { chain ->
                                FilterChip(
                                    selected = uiState.selectedChain == chain,
                                    onClick = { viewModel.selectChain(chain) },
                                    label = { Text("${chain.displayName} (${chain.bech32Hrp})") },
                                    modifier = Modifier.testTag("tx_chain_${chain.id}")
                                )
                            }
                        }

                        OutlinedTextField(
                            value = uiState.rawHexInput,
                            onValueChange = viewModel::updateHexInput,
                            label = { Text("Raw Transaction Hex") },
                            placeholder = { Text("Paste hex-encoded transaction bytes…") },
                            minLines = 3,
                            maxLines = 6,
                            textStyle = MonoTextStyle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tx_hex_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = viewModel::loadBip141TestVector,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("tx_load_sample_button")
                            ) {
                                Text("BIP-141 Test Vector")
                            }
                            OutlinedButton(
                                onClick = viewModel::clearInput,
                                enabled = uiState.rawHexInput.isNotEmpty(),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("tx_clear_button")
                            ) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Clear hex input")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear")
                            }
                        }
                    }
                }
            }

            when (val pState = uiState.parseState) {
                is TxParseState.Empty -> {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Awaiting Raw Transaction Hex",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Paste any legacy or SegWit raw transaction hex above, or tap \"BIP-141 Test Vector\" to inspect txid/wtxid hashes, weight units, vBytes, RBF sequence flags, witness stacks, and scriptPubKey decoding.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                is TxParseState.Error -> {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Decode Error",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = pState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                is TxParseState.Parsed -> {
                    val tx = pState.tx
                    item {
                        TxSummaryCard(tx = tx)
                    }
                    item {
                        SectionHeader(
                            title = "Inputs (${tx.inputs.size})",
                            subtitle = "Previous outpoints, sequence & witness stack",
                            icon = Icons.AutoMirrored.Outlined.CallSplit
                        )
                    }
                    items(tx.inputs, key = { "in_${it.index}" }) { input ->
                        TxInputCard(input = input)
                    }
                    item {
                        SectionHeader(
                            title = "Outputs (${tx.outputs.size})",
                            subtitle = "Total output value: ${tx.totalOutputBtcFormatted}",
                            icon = Icons.Outlined.DataObject
                        )
                    }
                    items(tx.outputs, key = { "out_${it.index}" }) { output ->
                        TxOutputCard(output = output)
                    }
                }
            }
        }
    }
}

@Composable
private fun TxSummaryCard(tx: RawTxParser.ParsedTransaction) {
    val ext = CoreLensTheme.extendedColors
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Transaction Summary", style = MaterialTheme.typography.titleMedium)
                StatusPill(
                    label = if (tx.isSegwit) "SegWit (BIP-141)" else "Legacy",
                    color = if (tx.isSegwit) ext.telemetry else MaterialTheme.colorScheme.primary,
                    containerColor = if (tx.isSegwit) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            KeyValueRow("Version", "${tx.version}")
            KeyValueRow("Size / Stripped", "${tx.totalBytes} B / ${tx.strippedBytes} B")
            KeyValueRow("Weight / vSize", "${tx.weightUnits} WU / ${tx.virtualBytes} vB")
            KeyValueRow("Locktime", tx.locktimeDescription)
            KeyValueRow("Total Output", "${tx.totalOutputBtcFormatted} (${tx.totalOutputSatoshis} sats)")
            CopyableMonoBlock("TXID (Non-Witness Double-SHA256)", tx.txid, testTag = "tx_summary_txid")
            if (tx.isSegwit) {
                CopyableMonoBlock("WTXID (Full Serialization Double-SHA256)", tx.wtxid, testTag = "tx_summary_wtxid")
            }
        }
    }
}

@Composable
private fun TxInputCard(input: RawTxParser.TxInput) {
    val ext = CoreLensTheme.extendedColors
    Card(
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
                Text("vin[${input.index}]", style = MaterialTheme.typography.titleSmall)
                if (input.isCoinbase) {
                    StatusPill(
                        label = "Coinbase",
                        color = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                } else {
                    StatusPill(
                        label = if (input.rbfOptIn) "RBF Opt-In (BIP-125)" else "Final Sequence",
                        color = if (input.rbfOptIn) ext.warning else ext.valid,
                        containerColor = if (input.rbfOptIn) MaterialTheme.colorScheme.primaryContainer else ext.valid.copy(alpha = 0.14f)
                    )
                }
            }
            CopyableMonoBlock(
                label = "Outpoint (txid:vout)",
                value = "${input.prevTxid}:${input.prevVout}",
                testTag = "tx_in_${input.index}_outpoint"
            )
            KeyValueRow("nSequence", "${input.sequenceHex} (${input.sequence})", monoValue = true)
            if (input.scriptSigSize > 0) {
                CopyableMonoBlock(
                    label = "scriptSig (${input.scriptSigSize} bytes)",
                    value = input.scriptSigHex,
                    testTag = "tx_in_${input.index}_scriptsig"
                )
            }
            if (input.witnessItems.isNotEmpty()) {
                Text(
                    text = "Witness Stack (${input.witnessItems.size} items)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                input.witnessItems.forEachIndexed { wIdx, itemHex ->
                    CopyableMonoBlock(
                        label = "witness[$wIdx] (${itemHex.length / 2} bytes)",
                        value = itemHex.ifEmpty { "<empty>" },
                        testTag = "tx_in_${input.index}_wit_$wIdx",
                        maxLines = 3
                    )
                }
            }
        }
    }
}

@Composable
private fun TxOutputCard(output: RawTxParser.TxOutput) {
    Card(
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
                Text("vout[${output.index}]", style = MaterialTheme.typography.titleSmall)
                StatusPill(
                    label = output.scriptType,
                    color = MaterialTheme.colorScheme.secondary,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            }
            KeyValueRow("Value", "${output.valueBtcFormatted} (${output.valueSatoshis} sats)")
            if (output.decodedAddress != null) {
                CopyableMonoBlock(
                    label = "Script Destination Address",
                    value = output.decodedAddress,
                    testTag = "tx_out_${output.index}_address"
                )
            }
            if (!output.opReturnText.isNullOrEmpty()) {
                CopyableMonoBlock(
                    label = "OP_RETURN Decoded Text",
                    value = output.opReturnText,
                    testTag = "tx_out_${output.index}_opreturn"
                )
            }
            CopyableMonoBlock(
                label = "scriptPubKey Hex (${output.scriptPubKeyHex.length / 2} bytes)",
                value = output.scriptPubKeyHex,
                testTag = "tx_out_${output.index}_script"
            )
        }
    }
}
