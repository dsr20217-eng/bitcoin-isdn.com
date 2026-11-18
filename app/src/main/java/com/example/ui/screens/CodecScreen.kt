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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.core.BitcoinCrypto
import com.example.ui.components.CopyableMonoBlock
import com.example.ui.components.KeyValueRow
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.CoreLensTheme
import com.example.ui.theme.MonoTextStyle
import com.example.ui.viewmodel.CodecTab
import com.example.ui.viewmodel.CodecUiState
import com.example.ui.viewmodel.CodecViewModel

@Composable
fun CodecScreen(
    uiState: CodecUiState,
    viewModel: CodecViewModel,
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
                    title = "Protocol Codec & Crypto Lab",
                    subtitle = "Ported from Bitcoin Core base58.cpp, bech32.cpp & crypto/",
                    icon = Icons.Outlined.Code
                )
            }

            item {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("codec_tab_row")
                ) {
                    CodecTab.entries.forEachIndexed { idx, tab ->
                        SegmentedButton(
                            selected = uiState.activeTab == tab,
                            onClick = { viewModel.selectTab(tab) },
                            shape = SegmentedButtonDefaults.itemShape(idx, CodecTab.entries.size),
                            modifier = Modifier.testTag("codec_tab_${tab.name.lowercase()}")
                        ) {
                            Text(tab.label, maxLines = 1)
                        }
                    }
                }
            }

            when (uiState.activeTab) {
                CodecTab.BECH32 -> item { Bech32Section(uiState, viewModel) }
                CodecTab.BASE58 -> item { Base58Section(uiState, viewModel) }
                CodecTab.HASH -> item { HashDigestSection(uiState, viewModel) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Bech32Section(
    uiState: CodecUiState,
    viewModel: CodecViewModel
) {
    val ext = CoreLensTheme.extendedColors
    val inspection = uiState.bech32Inspection
    val sampleVectors = listOf(
        "SegWit v0" to "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",
        "Taproot v1" to "bc1p5cyxnuxmeuwuvkwfem96lqzszd02n6xdcjrs20cac6yqjjwudpxqkedrcr",
        "Typo Test" to "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t5"
    )

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bech32 / Bech32m Inspector",
                        style = MaterialTheme.typography.titleMedium
                    )
                    StatusPill(
                        label = if (inspection.valid) inspection.encoding.label else "Invalid",
                        color = if (inspection.valid) ext.valid else MaterialTheme.colorScheme.error,
                        containerColor = if (inspection.valid) ext.valid.copy(alpha = 0.14f) else MaterialTheme.colorScheme.errorContainer
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sampleVectors.forEach { (label, vector) ->
                        FilterChip(
                            selected = uiState.bech32Input == vector,
                            onClick = { viewModel.updateBech32Input(vector) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("bech32_preset_${label.lowercase().replace(' ', '_')}")
                        )
                    }
                }

                OutlinedTextField(
                    value = uiState.bech32Input,
                    onValueChange = viewModel::updateBech32Input,
                    label = { Text("Bech32 / Bech32m Address") },
                    textStyle = MonoTextStyle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bech32_decode_input")
                )

                if (inspection.valid) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    KeyValueRow("Network", inspection.networkLabel)
                    KeyValueRow("HRP", inspection.hrp, monoValue = true)
                    KeyValueRow("Witness Version", "v${inspection.witnessVersion}")
                    KeyValueRow("Script Type", inspection.scriptTypeLabel)
                    KeyValueRow("Program Length", "${inspection.witnessProgramLength} bytes")
                    CopyableMonoBlock(
                        label = "Witness Program Hex",
                        value = inspection.witnessProgramHex,
                        testTag = "bech32_program_hex"
                    )
                } else {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = inspection.errorMessage ?: "Invalid Bech32 string",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            if (inspection.errorIndices.isNotEmpty()) {
                                Text(
                                    text = "GF(1024) Syndrome Error Locator (index ${inspection.errorIndices.joinToString(", ")}):",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val highlighted = buildAnnotatedString {
                                    val errSet = inspection.errorIndices.toSet()
                                    uiState.bech32Input.forEachIndexed { idx, ch ->
                                        if (idx in errSet) {
                                            withStyle(
                                                SpanStyle(
                                                    color = MaterialTheme.colorScheme.error,
                                                    background = MaterialTheme.colorScheme.errorContainer,
                                                    fontWeight = FontWeight.Bold,
                                                    textDecoration = TextDecoration.Underline
                                                )
                                            ) {
                                                append(ch)
                                            }
                                        } else {
                                            append(ch)
                                        }
                                    }
                                }
                                Text(text = highlighted, style = MonoTextStyle)
                            }
                        }
                    }
                }
            }
        }

        // Bech32 / Bech32m Encoder
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "SegWit Address Encoder (BIP-173 / BIP-350)",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.encodeHrp,
                        onValueChange = { viewModel.updateEncodeParams(it, uiState.encodeWitnessVer, uiState.encodeProgramHex) },
                        label = { Text("HRP") },
                        singleLine = true,
                        textStyle = MonoTextStyle,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bech32_encode_hrp")
                    )
                    OutlinedTextField(
                        value = uiState.encodeWitnessVer.toString(),
                        onValueChange = {
                            val v = it.toIntOrNull() ?: 0
                            viewModel.updateEncodeParams(uiState.encodeHrp, v, uiState.encodeProgramHex)
                        },
                        label = { Text("Version (0..16)") },
                        singleLine = true,
                        textStyle = MonoTextStyle,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bech32_encode_version")
                    )
                }
                OutlinedTextField(
                    value = uiState.encodeProgramHex,
                    onValueChange = { viewModel.updateEncodeParams(uiState.encodeHrp, uiState.encodeWitnessVer, it) },
                    label = { Text("Witness Program Hex (20 or 32 bytes)") },
                    textStyle = MonoTextStyle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bech32_encode_program")
                )
                if (uiState.encodeError != null) {
                    Text(
                        text = uiState.encodeError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (uiState.encodedBech32Output != null) {
                    CopyableMonoBlock(
                        label = if (uiState.encodeWitnessVer == 0) "Encoded Bech32" else "Encoded Bech32m",
                        value = uiState.encodedBech32Output,
                        testTag = "bech32_encoded_result"
                    )
                }
            }
        }
    }
}

@Composable
private fun Base58Section(
    uiState: CodecUiState,
    viewModel: CodecViewModel
) {
    val ext = CoreLensTheme.extendedColors
    val inspection = uiState.base58Inspection

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                    Text("Base58Check Decoder", style = MaterialTheme.typography.titleMedium)
                    StatusPill(
                        label = if (inspection.valid) "Checksum Valid" else "Invalid",
                        color = if (inspection.valid) ext.valid else MaterialTheme.colorScheme.error,
                        containerColor = if (inspection.valid) ext.valid.copy(alpha = 0.14f) else MaterialTheme.colorScheme.errorContainer
                    )
                }

                OutlinedTextField(
                    value = uiState.base58Input,
                    onValueChange = viewModel::updateBase58Input,
                    label = { Text("Base58Check String") },
                    textStyle = MonoTextStyle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("base58_decode_input")
                )

                if (inspection.valid) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    KeyValueRow("Classification", inspection.prefixDescription)
                    KeyValueRow("Checksum (4B)", inspection.checksumHex, monoValue = true)
                    CopyableMonoBlock(
                        label = "Decoded Payload Hex (${inspection.payload.size} bytes)",
                        value = BitcoinCrypto.bytesToHex(inspection.payload),
                        testTag = "base58_decoded_payload"
                    )
                } else {
                    Text(
                        text = inspection.error ?: "Invalid Base58Check input",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    if (inspection.expectedChecksumHex.isNotEmpty()) {
                        Text(
                            text = "Actual ${inspection.checksumHex} vs Expected ${inspection.expectedChecksumHex}",
                            style = MonoTextStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VerifiedUser,
                        contentDescription = "Base58Check Encoder",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Base58Check Encoder", style = MaterialTheme.typography.titleMedium)
                }
                OutlinedTextField(
                    value = uiState.base58RawHexInput,
                    onValueChange = viewModel::updateBase58RawHex,
                    label = { Text("Version + Payload Hex") },
                    textStyle = MonoTextStyle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("base58_encode_hex_input")
                )
                if (uiState.base58EncodeError != null) {
                    Text(
                        text = uiState.base58EncodeError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    CopyableMonoBlock(
                        label = "Encoded Base58Check",
                        value = uiState.base58EncodedOutput,
                        testTag = "base58_encoded_result"
                    )
                }
            }
        }
    }
}

@Composable
private fun HashDigestSection(
    uiState: CodecUiState,
    viewModel: CodecViewModel
) {
    val out = uiState.hashOutputs
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Fingerprint,
                    contentDescription = "Cryptographic Hashes",
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text("Cryptographic Hash Inspector", style = MaterialTheme.typography.titleMedium)
            }

            OutlinedTextField(
                value = uiState.hashInputText,
                onValueChange = { viewModel.updateHashInput(it, uiState.hashInputIsHex, uiState.reverseByteOrder) },
                label = { Text(if (uiState.hashInputIsHex) "Raw Hex Bytes" else "UTF-8 Text Input") },
                textStyle = MonoTextStyle,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hash_text_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Parse input as Hex", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = uiState.hashInputIsHex,
                    onCheckedChange = { viewModel.updateHashInput(uiState.hashInputText, it, uiState.reverseByteOrder) },
                    modifier = Modifier.testTag("hash_hex_mode_switch")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Reverse byte order (Bitcoin display)", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = uiState.reverseByteOrder,
                    onCheckedChange = { viewModel.updateHashInput(uiState.hashInputText, uiState.hashInputIsHex, it) },
                    modifier = Modifier.testTag("hash_reverse_switch")
                )
            }

            if (out.error != null) {
                Text(
                    text = out.error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Digests over ${out.inputByteLength} bytes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CopyableMonoBlock("SHA-256", out.sha256, testTag = "hash_sha256")
                CopyableMonoBlock("Double-SHA256 (Hash256)", out.doubleSha256, testTag = "hash_double_sha256")
                CopyableMonoBlock("HASH160 (RIPEMD-160(SHA-256))", out.hash160, testTag = "hash_160")
                CopyableMonoBlock("SHA3-256", out.sha3_256, testTag = "hash_sha3")
                CopyableMonoBlock("SHA-512", out.sha512, testTag = "hash_sha512")
            }
        }
    }
}
