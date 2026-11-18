package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.PermMedia
import androidx.compose.material.icons.outlined.Webhook
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AttachedSourceEntity
import com.example.data.local.DispatchMessageEntity
import com.example.ui.components.CopyableMonoBlock
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.CoreLensTheme
import com.example.ui.theme.MonoTextStyle
import com.example.ui.viewmodel.DispatchSubTab
import com.example.ui.viewmodel.DispatchUiState
import com.example.ui.viewmodel.DispatchViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DispatchScreen(
    uiState: DispatchUiState,
    viewModel: DispatchViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val directoryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.attachDirectoryTreeUri(context, uri)
        }
    }

    val documentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.attachDocumentUri(context, uri, "FILE")
        }
    }

    val mediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.attachDocumentUri(context, uri, "MEDIA")
        }
    }

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
                    title = "Dispatch, Inbox/Outbox & Drives",
                    subtitle = "Custom domain routing, webhook POST replies, mailto: composer & zero-permission SAF",
                    icon = Icons.Outlined.Inbox
                )
            }

            item {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dispatch_subtab_row")
                ) {
                    DispatchSubTab.entries.forEachIndexed { idx, tab ->
                        SegmentedButton(
                            selected = uiState.activeTab == tab,
                            onClick = { viewModel.selectTab(tab) },
                            shape = SegmentedButtonDefaults.itemShape(idx, DispatchSubTab.entries.size),
                            modifier = Modifier.testTag("dispatch_tab_${tab.name.lowercase()}")
                        ) {
                            Text(tab.label, maxLines = 1)
                        }
                    }
                }
            }

            if (uiState.activeTab == DispatchSubTab.OUTBOX_INBOX) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Custom Domain & Email Routing", style = MaterialTheme.typography.titleMedium)
                            OutlinedTextField(
                                value = uiState.customDomain,
                                onValueChange = { viewModel.updateDomainRouting(it, uiState.senderEmail, uiState.recipientEmail) },
                                label = { Text("Custom Sender Domain") },
                                singleLine = true,
                                textStyle = MonoTextStyle,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dispatch_domain_input")
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = uiState.senderEmail,
                                    onValueChange = { viewModel.updateDomainRouting(uiState.customDomain, it, uiState.recipientEmail) },
                                    label = { Text("Sender Email") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("dispatch_sender_input")
                                )
                                OutlinedTextField(
                                    value = uiState.recipientEmail,
                                    onValueChange = { viewModel.updateDomainRouting(uiState.customDomain, uiState.senderEmail, it) },
                                    label = { Text("Recipient Email") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("dispatch_recipient_input")
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            OutlinedTextField(
                                value = uiState.subject,
                                onValueChange = {
                                    viewModel.updateComposer(it, uiState.webhookUrl, uiState.authHeaderName, uiState.authHeaderValue, uiState.payloadBody)
                                },
                                label = { Text("Subject") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dispatch_subject_input")
                            )
                            OutlinedTextField(
                                value = uiState.webhookUrl,
                                onValueChange = {
                                    viewModel.updateComposer(uiState.subject, it, uiState.authHeaderName, uiState.authHeaderValue, uiState.payloadBody)
                                },
                                label = { Text("Webhook Endpoint URL (optional for HTTP POST)") },
                                singleLine = true,
                                textStyle = MonoTextStyle,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dispatch_webhook_input")
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = uiState.authHeaderName,
                                    onValueChange = {
                                        viewModel.updateComposer(uiState.subject, uiState.webhookUrl, it, uiState.authHeaderValue, uiState.payloadBody)
                                    },
                                    label = { Text("Header Name") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = uiState.authHeaderValue,
                                    onValueChange = {
                                        viewModel.updateComposer(uiState.subject, uiState.webhookUrl, uiState.authHeaderName, it, uiState.payloadBody)
                                    },
                                    label = { Text("Header Token") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            OutlinedTextField(
                                value = uiState.payloadBody,
                                onValueChange = {
                                    viewModel.updateComposer(uiState.subject, uiState.webhookUrl, uiState.authHeaderName, uiState.authHeaderValue, it)
                                },
                                label = { Text("Protocol Payload / Report Body") },
                                minLines = 3,
                                maxLines = 6,
                                textStyle = MonoTextStyle,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dispatch_payload_input")
                            )

                            if (uiState.feedbackMessage != null) {
                                Text(
                                    text = uiState.feedbackMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = viewModel::sendViaWebhook,
                                    enabled = !uiState.isDispatching,
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("dispatch_send_webhook_button")
                                ) {
                                    Icon(Icons.Outlined.Webhook, contentDescription = "Send webhook")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (uiState.isDispatching) "Sending…" else "POST Webhook")
                                }
                                OutlinedButton(
                                    onClick = {
                                        val intent = viewModel.buildEmailIntentAndRecord()
                                        if (intent != null) {
                                            runCatching { context.startActivity(intent) }
                                        }
                                    },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("dispatch_send_email_button")
                                ) {
                                    Icon(Icons.Outlined.Email, contentDescription = "Compose email")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Email App")
                                }
                                OutlinedButton(
                                    onClick = viewModel::saveDraftToOutbox,
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("dispatch_save_draft_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Save draft")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save Draft")
                                }
                            }
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !uiState.showInboxOnly,
                            onClick = { viewModel.setInboxFilter(false) },
                            label = { Text("All (${uiState.messages.size})") },
                            modifier = Modifier.testTag("filter_all_messages")
                        )
                        FilterChip(
                            selected = uiState.showInboxOnly,
                            onClick = { viewModel.setInboxFilter(true) },
                            label = { Text("Inbox Replies (${uiState.messages.count { it.boxType == "INBOX" }})") },
                            modifier = Modifier.testTag("filter_inbox_messages")
                        )
                    }
                }

                val filtered = if (uiState.showInboxOnly) {
                    uiState.messages.filter { it.boxType == "INBOX" }
                } else {
                    uiState.messages
                }
                if (filtered.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = "Outbox & Inbox are empty. Dispatch a webhook POST, open an email report, or save a draft above—messages auto-save in Room.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(filtered, key = { it.id }) { msg ->
                        DispatchMessageCard(
                            msg = msg,
                            onDelete = { viewModel.deleteMessage(msg.id) }
                        )
                    }
                }
            } else {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Hook Up Directories, Files & Media", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Uses zero-permission Android Storage Access Framework (SAF) and Photo Picker to inspect local configuration files, hex dumps, drives, and media.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { directoryLauncher.launch(null) },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("attach_directory_button")
                                ) {
                                    Icon(Icons.Outlined.CreateNewFolder, contentDescription = "Pick directory")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Attach Directory / Drive")
                                }
                                OutlinedButton(
                                    onClick = { documentLauncher.launch(arrayOf("*/*")) },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("attach_file_button")
                                ) {
                                    Icon(Icons.Outlined.AttachFile, contentDescription = "Pick file")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pick Document")
                                }
                                OutlinedButton(
                                    onClick = {
                                        mediaLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                        )
                                    },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("attach_media_button")
                                ) {
                                    Icon(Icons.Outlined.PermMedia, contentDescription = "Pick media")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pick Media")
                                }
                            }
                        }
                    }
                }

                if (uiState.attachedSources.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = "No directories, documents, or media attached yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(uiState.attachedSources, key = { it.id }) { src ->
                        AttachedSourceCard(
                            source = src,
                            onDelete = { viewModel.deleteAttachedSource(src.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DispatchMessageCard(
    msg: DispatchMessageEntity,
    onDelete: () -> Unit
) {
    val ext = CoreLensTheme.extendedColors
    val isInbox = msg.boxType == "INBOX"
    val pillColor = when (msg.status) {
        "DELIVERED_WEBHOOK", "WEBHOOK_RESPONSE", "SENT_EMAIL" -> ext.valid
        "FAILED" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isInbox) Icons.Outlined.MarkEmailRead else Icons.AutoMirrored.Outlined.Send,
                        contentDescription = msg.boxType,
                        tint = if (isInbox) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = msg.subject,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete message")
                }
            }

            StatusPill(
                label = "${msg.boxType} · ${msg.status}",
                color = pillColor,
                containerColor = pillColor.copy(alpha = 0.14f)
            )
            Text(
                text = "Domain: ${msg.senderDomain} · From: ${msg.senderEmail.ifBlank { "-" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (msg.payloadBody.isNotBlank()) {
                CopyableMonoBlock(
                    label = "Message Payload",
                    value = msg.payloadBody,
                    testTag = "dispatch_msg_${msg.id}",
                    maxLines = 5
                )
            }
        }
    }
}

@Composable
private fun AttachedSourceCard(
    source: AttachedSourceEntity,
    onDelete: () -> Unit
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = source.sourceType,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(source.displayName, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "${source.sourceType} · ${source.sizeBytes} bytes · ${source.mimeType}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Detach item")
                }
            }
            if (source.previewText.isNotBlank()) {
                CopyableMonoBlock(
                    label = "Content / Directory Preview",
                    value = source.previewText,
                    testTag = "attached_${source.id}",
                    maxLines = 6
                )
            }
        }
    }
}
