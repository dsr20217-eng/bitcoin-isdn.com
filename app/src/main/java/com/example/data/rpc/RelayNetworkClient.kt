package com.example.data.rpc

import com.example.data.local.RelayEndpointEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class EndpointProbeResult(
    val success: Boolean,
    val statusLabel: String,
    val latencyMs: Long,
    val responseSnippet: String
)

data class WebhookDispatchResult(
    val success: Boolean,
    val httpStatusCode: Int?,
    val latencyMs: Long,
    val responseBody: String
)

class RelayNetworkClient {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun probeEndpoint(endpoint: RelayEndpointEntity): EndpointProbeResult = withContext(Dispatchers.IO) {
        val timeout = endpoint.routingTimeoutMs.coerceIn(1000, 60000).toLong()
        val client = OkHttpClient.Builder()
            .connectTimeout(timeout, TimeUnit.MILLISECONDS)
            .readTimeout(timeout, TimeUnit.MILLISECONDS)
            .writeTimeout(timeout, TimeUnit.MILLISECONDS)
            .build()

        val maxAttempts = endpoint.retryCount.coerceIn(1, 3)
        var lastErr = "Connection failed"
        var lastLatency = 0L

        repeat(maxAttempts) {
            val start = System.currentTimeMillis()
            try {
                val url = endpoint.endpointUrl.trim()
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    throw IllegalArgumentException("Endpoint URL must start with http:// or https://")
                }

                val builder = Request.Builder().url(url)
                applyCustomHeaders(builder, endpoint.authHeaderName, endpoint.authHeaderValue, endpoint.customHeadersText)

                if (endpoint.protocolType == "REST_GET" || endpoint.probeMethod.startsWith("GET ")) {
                    val targetUrl = if (endpoint.probeMethod.startsWith("GET ")) {
                        url.trimEnd('/') + endpoint.probeMethod.removePrefix("GET ").trim()
                    } else {
                        url
                    }
                    builder.url(targetUrl).get()
                } else {
                    val paramsArr = runCatching { JSONArray(endpoint.probeParamsJson) }.getOrDefault(JSONArray())
                    val bodyJson = if (endpoint.protocolType == "WEBHOOK") {
                        JSONObject().apply {
                            put("event", "corelens.relay.ping")
                            put("chain", endpoint.chainName)
                            put("timestamp", System.currentTimeMillis())
                        }.toString()
                    } else {
                        JSONObject().apply {
                            put("jsonrpc", "2.0")
                            put("id", 1)
                            put("method", endpoint.probeMethod.ifBlank { "eth_blockNumber" })
                            put("params", paramsArr)
                        }.toString()
                    }
                    builder.post(bodyJson.toRequestBody(jsonMediaType))
                }

                client.newCall(builder.build()).execute().use { response ->
                    lastLatency = System.currentTimeMillis() - start
                    val bodyStr = response.body?.string().orEmpty()
                    val snippet = bodyStr.take(600)
                    return@withContext EndpointProbeResult(
                        success = response.isSuccessful,
                        statusLabel = "HTTP ${response.code}",
                        latencyMs = lastLatency,
                        responseSnippet = snippet.ifBlank { response.message }
                    )
                }
            } catch (e: Exception) {
                lastLatency = System.currentTimeMillis() - start
                lastErr = e.message ?: e::class.java.simpleName
            }
        }

        EndpointProbeResult(
            success = false,
            statusLabel = "Error",
            latencyMs = lastLatency,
            responseSnippet = lastErr
        )
    }

    suspend fun dispatchWebhook(
        url: String,
        customDomain: String,
        senderEmail: String,
        recipientEmail: String,
        subject: String,
        payloadBody: String,
        authHeaderName: String = "",
        authHeaderValue: String = "",
        timeoutMs: Int = 10000
    ): WebhookDispatchResult = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
            .build()

        val start = System.currentTimeMillis()
        try {
            val cleanUrl = url.trim()
            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                throw IllegalArgumentException("Webhook URL must start with http:// or https://")
            }

            val jsonEnvelope = JSONObject().apply {
                put("source", "CoreLens")
                put("domain", customDomain)
                put("sender", senderEmail)
                put("recipient", recipientEmail)
                put("subject", subject)
                put("timestamp", System.currentTimeMillis())
                put("payload", payloadBody)
            }.toString()

            val builder = Request.Builder()
                .url(cleanUrl)
                .header("X-CoreLens-Domain", customDomain)
                .header("X-CoreLens-Sender", senderEmail)
                .post(jsonEnvelope.toRequestBody(jsonMediaType))

            if (authHeaderName.isNotBlank() && authHeaderValue.isNotBlank()) {
                builder.header(authHeaderName.trim(), authHeaderValue.trim())
            }

            client.newCall(builder.build()).execute().use { response ->
                val latency = System.currentTimeMillis() - start
                val respBody = response.body?.string().orEmpty().take(1200)
                WebhookDispatchResult(
                    success = response.isSuccessful,
                    httpStatusCode = response.code,
                    latencyMs = latency,
                    responseBody = respBody.ifBlank { "HTTP ${response.code} ${response.message}" }
                )
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - start
            WebhookDispatchResult(
                success = false,
                httpStatusCode = null,
                latencyMs = latency,
                responseBody = e.message ?: "Webhook request failed"
            )
        }
    }

    private fun applyCustomHeaders(
        builder: Request.Builder,
        authHeaderName: String,
        authHeaderValue: String,
        customHeadersText: String
    ) {
        if (authHeaderName.isNotBlank() && authHeaderValue.isNotBlank()) {
            builder.header(authHeaderName.trim(), authHeaderValue.trim())
        }
        customHeadersText.lines().forEach { line ->
            val idx = line.indexOf(':')
            if (idx > 0) {
                val key = line.substring(0, idx).trim()
                val value = line.substring(idx + 1).trim()
                if (key.isNotEmpty() && value.isNotEmpty()) {
                    builder.header(key, value)
                }
            }
        }
    }
}
