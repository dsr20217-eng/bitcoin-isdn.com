package com.example.ui.viewmodel

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AttachedSourceEntity
import com.example.data.local.AutoSavedSettings
import com.example.data.local.CoreLensRepository
import com.example.data.local.DispatchMessageEntity
import com.example.data.rpc.RelayNetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class DispatchSubTab(val label: String) {
    OUTBOX_INBOX("Inbox / Outbox"),
    DRIVES_MEDIA("Drives & Media")
}

data class DispatchUiState(
    val activeTab: DispatchSubTab = DispatchSubTab.OUTBOX_INBOX,
    val showInboxOnly: Boolean = false,
    val customDomain: String = "corelens.local",
    val senderEmail: String = "dispatch@corelens.local",
    val recipientEmail: String = "",
    val subject: String = "CoreLens Protocol Report",
    val webhookUrl: String = "",
    val authHeaderName: String = "",
    val authHeaderValue: String = "",
    val payloadBody: String = "",
    val isDispatching: Boolean = false,
    val feedbackMessage: String? = null,
    val messages: List<DispatchMessageEntity> = emptyList(),
    val attachedSources: List<AttachedSourceEntity> = emptyList()
)

class DispatchViewModel(
    private val repository: CoreLensRepository,
    private val networkClient: RelayNetworkClient = RelayNetworkClient()
) : ViewModel() {

    private val localState = MutableStateFlow(DispatchUiState())
    private var prefsInitialized = false

    val uiState: StateFlow<DispatchUiState> = combine(
        localState,
        repository.dispatchMessages,
        repository.attachedSources,
        repository.preferences.settingsFlow
    ) { state, msgs, sources, prefs: AutoSavedSettings ->
        val merged = if (!prefsInitialized) {
            prefsInitialized = true
            state.copy(
                customDomain = prefs.customDomain,
                senderEmail = prefs.senderEmail,
                recipientEmail = prefs.recipientEmail
            )
        } else {
            state
        }
        merged.copy(
            messages = msgs,
            attachedSources = sources
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DispatchUiState()
    )

    fun selectTab(tab: DispatchSubTab) {
        localState.update { it.copy(activeTab = tab) }
    }

    fun setInboxFilter(inboxOnly: Boolean) {
        localState.update { it.copy(showInboxOnly = inboxOnly) }
    }

    fun updateDomainRouting(domain: String, sender: String, recipient: String) {
        localState.update {
            it.copy(customDomain = domain, senderEmail = sender, recipientEmail = recipient)
        }
        viewModelScope.launch {
            repository.preferences.saveDispatchRouting(domain.trim(), sender.trim(), recipient.trim())
        }
    }

    fun updateComposer(subject: String, webhookUrl: String, authHeaderName: String, authHeaderValue: String, payload: String) {
        localState.update {
            it.copy(
                subject = subject,
                webhookUrl = webhookUrl,
                authHeaderName = authHeaderName,
                authHeaderValue = authHeaderValue,
                payloadBody = payload,
                feedbackMessage = null
            )
        }
    }

    fun saveDraftToOutbox() {
        val state = uiState.value
        if (state.payloadBody.isBlank() && state.subject.isBlank()) return
        viewModelScope.launch {
            repository.saveDispatchMessage(
                DispatchMessageEntity(
                    boxType = "OUTBOX",
                    subject = state.subject.ifBlank { "Untitled Protocol Dispatch" },
                    senderDomain = state.customDomain.trim(),
                    senderEmail = state.senderEmail.trim(),
                    recipientEmail = state.recipientEmail.trim(),
                    targetWebhookUrl = state.webhookUrl.trim(),
                    payloadBody = state.payloadBody,
                    status = "DRAFT"
                )
            )
            localState.update { it.copy(feedbackMessage = "Saved draft to Room Outbox") }
        }
    }

    fun sendViaWebhook() {
        val state = uiState.value
        val url = state.webhookUrl.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            localState.update { it.copy(feedbackMessage = "Enter a valid http:// or https:// webhook URL") }
            return
        }
        viewModelScope.launch {
            localState.update { it.copy(isDispatching = true, feedbackMessage = null) }
            val result = networkClient.dispatchWebhook(
                url = url,
                customDomain = state.customDomain.trim(),
                senderEmail = state.senderEmail.trim(),
                recipientEmail = state.recipientEmail.trim(),
                subject = state.subject.ifBlank { "CoreLens Protocol Report" },
                payloadBody = state.payloadBody,
                authHeaderName = state.authHeaderName,
                authHeaderValue = state.authHeaderValue
            )

            repository.saveDispatchMessage(
                DispatchMessageEntity(
                    boxType = "OUTBOX",
                    subject = state.subject.ifBlank { "CoreLens Webhook" },
                    senderDomain = state.customDomain.trim(),
                    senderEmail = state.senderEmail.trim(),
                    recipientEmail = state.recipientEmail.trim(),
                    targetWebhookUrl = url,
                    payloadBody = state.payloadBody,
                    status = if (result.success) "DELIVERED_WEBHOOK" else "FAILED",
                    httpStatusCode = result.httpStatusCode
                )
            )

            repository.saveDispatchMessage(
                DispatchMessageEntity(
                    boxType = "INBOX",
                    subject = "Webhook Reply: ${state.subject.ifBlank { url }}",
                    senderDomain = Uri.parse(url).host ?: state.customDomain,
                    senderEmail = url,
                    recipientEmail = state.senderEmail.trim(),
                    targetWebhookUrl = url,
                    payloadBody = "HTTP ${result.httpStatusCode ?: "ERR"} (${result.latencyMs} ms)\n${result.responseBody}",
                    status = if (result.success) "WEBHOOK_RESPONSE" else "FAILED",
                    httpStatusCode = result.httpStatusCode
                )
            )

            localState.update {
                it.copy(
                    isDispatching = false,
                    feedbackMessage = if (result.success) {
                        "Webhook delivered (HTTP ${result.httpStatusCode}, ${result.latencyMs} ms) — response logged to Inbox"
                    } else {
                        "Webhook failed: ${result.responseBody}"
                    }
                )
            }
        }
    }

    fun buildEmailIntentAndRecord(): Intent? {
        val state = uiState.value
        val recipient = state.recipientEmail.trim()
        val subject = state.subject.ifBlank { "CoreLens Report" }
        val fullBody = buildString {
            appendLine("Domain Route: ${state.customDomain}")
            appendLine("Sender Identity: ${state.senderEmail}")
            appendLine("---")
            append(state.payloadBody)
        }
        viewModelScope.launch {
            repository.saveDispatchMessage(
                DispatchMessageEntity(
                    boxType = "OUTBOX",
                    subject = subject,
                    senderDomain = state.customDomain.trim(),
                    senderEmail = state.senderEmail.trim(),
                    recipientEmail = recipient,
                    targetWebhookUrl = "",
                    payloadBody = fullBody,
                    status = "SENT_EMAIL"
                )
            )
            localState.update { it.copy(feedbackMessage = "Opened email composer & saved to Outbox") }
        }
        return Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:${Uri.encode(recipient)}")
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, fullBody)
        }
    }

    fun deleteMessage(id: Long) {
        viewModelScope.launch {
            repository.deleteDispatchMessage(id)
        }
    }

    fun attachDocumentUri(context: Context, uri: Uri, sourceType: String) {
        viewModelScope.launch {
            val entity = withContext(Dispatchers.IO) {
                inspectSafUri(context.contentResolver, uri, sourceType)
            }
            repository.saveAttachedSource(entity)
        }
    }

    fun attachDirectoryTreeUri(context: Context, treeUri: Uri) {
        viewModelScope.launch {
            val entity = withContext(Dispatchers.IO) {
                inspectDirectoryTree(context.contentResolver, treeUri)
            }
            repository.saveAttachedSource(entity)
        }
    }

    fun deleteAttachedSource(id: Long) {
        viewModelScope.launch {
            repository.deleteAttachedSource(id)
        }
    }

    private fun inspectSafUri(resolver: ContentResolver, uri: Uri, sourceType: String): AttachedSourceEntity {
        var displayName = uri.lastPathSegment ?: "Attached item"
        var sizeBytes = 0L
        val mime = resolver.getType(uri) ?: "application/octet-stream"

        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) displayName = cursor.getString(nameIdx) ?: displayName
                if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) sizeBytes = cursor.getLong(sizeIdx)
            }
        }

        val snippet = runCatching {
            if (mime.startsWith("text/") || mime.contains("json") || mime.contains("xml") || displayName.endsWith(".conf") || displayName.endsWith(".hex")) {
                resolver.openInputStream(uri)?.use { stream ->
                    val buf = ByteArray(1024)
                    val read = stream.read(buf)
                    if (read > 0) String(buf, 0, read, Charsets.UTF_8) else "(empty file)"
                } ?: ""
            } else {
                "$mime · $sizeBytes bytes"
            }
        }.getOrElse { it.message ?: "Unreadable stream" }

        return AttachedSourceEntity(
            displayName = displayName,
            uriString = uri.toString(),
            sourceType = sourceType,
            mimeType = mime,
            sizeBytes = sizeBytes,
            previewText = snippet
        )
    }

    private fun inspectDirectoryTree(resolver: ContentResolver, treeUri: Uri): AttachedSourceEntity {
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
        val childNames = mutableListOf<String>()
        var totalSize = 0L

        runCatching {
            resolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE
                ),
                null,
                null,
                null
            )?.use { cursor ->
                val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                while (cursor.moveToNext()) {
                    val n = if (nameCol >= 0) cursor.getString(nameCol) else "item"
                    val m = if (mimeCol >= 0) cursor.getString(mimeCol) else ""
                    val s = if (sizeCol >= 0 && !cursor.isNull(sizeCol)) cursor.getLong(sizeCol) else 0L
                    totalSize += s
                    val prefix = if (m == DocumentsContract.Document.MIME_TYPE_DIR) "[DIR] " else ""
                    if (childNames.size < 12) childNames.add("$prefix$n")
                }
            }
        }

        val folderName = docId.substringAfterLast(':').ifBlank { "Selected Directory" }
        val preview = if (childNames.isEmpty()) {
            "Empty directory or restricted provider"
        } else {
            childNames.joinToString("\n")
        }

        return AttachedSourceEntity(
            displayName = folderName,
            uriString = treeUri.toString(),
            sourceType = "DIRECTORY",
            mimeType = DocumentsContract.Document.MIME_TYPE_DIR,
            sizeBytes = totalSize,
            previewText = preview
        )
    }

    class Factory(private val repository: CoreLensRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DispatchViewModel(repository) as T
        }
    }
}
