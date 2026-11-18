package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "relay_endpoints")
data class RelayEndpointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val chainName: String,
    val ticker: String,
    val protocolType: String, // "JSON_RPC", "REST_GET", "WEBHOOK"
    val endpointUrl: String,
    val probeMethod: String,
    val probeParamsJson: String,
    val routingTimeoutMs: Int = 8000,
    val retryCount: Int = 1,
    val authHeaderName: String = "",
    val authHeaderValue: String = "",
    val customHeadersText: String = "", // key: value lines
    val lastStatus: String = "Untested",
    val lastLatencyMs: Long? = null,
    val lastResponseSnippet: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "dispatch_messages")
data class DispatchMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val boxType: String, // "OUTBOX" or "INBOX"
    val subject: String,
    val senderDomain: String,
    val senderEmail: String,
    val recipientEmail: String,
    val targetWebhookUrl: String,
    val payloadBody: String,
    val status: String, // "DRAFT", "SENT_EMAIL", "DELIVERED_WEBHOOK", "WEBHOOK_RESPONSE", "FAILED"
    val httpStatusCode: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "attached_sources")
data class AttachedSourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val uriString: String,
    val sourceType: String, // "DIRECTORY", "FILE", "MEDIA"
    val mimeType: String,
    val sizeBytes: Long,
    val previewText: String,
    val attachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "inspection_history")
data class InspectionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val toolType: String, // "BECH32", "BASE58", "TX_HEX"
    val inputValue: String,
    val summaryLabel: String,
    val createdAt: Long = System.currentTimeMillis()
)
