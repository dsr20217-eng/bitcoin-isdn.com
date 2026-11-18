package com.example.data.rpc

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Real HTTP/HTTPS JSON-RPC client for Bitcoin Core (ported from src/bitcoin-cli.cpp).
 * Scoped to read-only node, network, mempool, and block telemetry.
 */
class BitcoinRpcClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val blockedMutatingMethods = setOf(
        "createwallet", "loadwallet", "unloadwallet", "restorewallet",
        "dumpprivkey", "dumpwallet", "importprivkey", "importdescriptors",
        "sendtoaddress", "sendmany", "send", "sendall", "sendrawtransaction",
        "signrawtransactionwithkey", "signrawtransactionwithwallet",
        "generatetoaddress", "generatetodescriptor", "generateblock", "generate",
        "stop"
    )

    suspend fun fetchTelemetry(config: RpcConnectionConfig): Result<NodeTelemetryBundle> = withContext(Dispatchers.IO) {
        runCatching {
            val blockchainInfo = callRpcRaw(config, "getblockchaininfo", JSONArray())
            val networkInfo = callRpcRaw(config, "getnetworkinfo", JSONArray())
            val peerInfo = callRpcArray(config, "getpeerinfo", JSONArray())
            val mempoolInfo = callRpcRaw(config, "getmempoolinfo", JSONArray())
            val nodeAddresses = runCatching {
                callRpcArray(config, "getnodeaddresses", JSONArray().put(0))
            }.getOrDefault(JSONArray())

            val connIn = networkInfo.optInt("connections_in", 0)
            val connOut = networkInfo.optInt("connections_out", 0)
            val connTotal = networkInfo.optInt("connections", connIn + connOut)

            val warningsVal = networkInfo.opt("warnings")
            val warningsText = when (warningsVal) {
                is JSONArray -> (0 until warningsVal.length()).joinToString("; ") { warningsVal.optString(it) }
                is String -> warningsVal
                else -> ""
            }

            val overview = NodeOverview(
                chain = blockchainInfo.optString("chain", config.chain.id),
                clientVersion = networkInfo.optInt("version", 0),
                subversion = networkInfo.optString("subversion", ""),
                protocolVersion = networkInfo.optInt("protocolversion", 0),
                blocks = blockchainInfo.optLong("blocks", 0L),
                headers = blockchainInfo.optLong("headers", 0L),
                verificationProgress = blockchainInfo.optDouble("verificationprogress", 0.0),
                difficulty = blockchainInfo.optDouble("difficulty", 0.0),
                connectionsIn = connIn,
                connectionsOut = connOut,
                connectionsTotal = connTotal,
                mempoolTxCount = mempoolInfo.optInt("size", 0),
                mempoolBytes = mempoolInfo.optLong("bytes", 0L),
                relayFee = networkInfo.optDouble("relayfee", 0.0),
                warnings = warningsText
            )

            val netinfo = buildNetinfoSnapshot(networkInfo, peerInfo)
            val addrinfo = buildAddrinfoSnapshot(nodeAddresses)

            NodeTelemetryBundle(
                overview = overview,
                netinfo = netinfo,
                addrinfo = addrinfo,
                fetchedAtMillis = System.currentTimeMillis()
            )
        }
    }

    suspend fun executeReadOnlyRpc(
        config: RpcConnectionConfig,
        method: String,
        paramsText: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedMethod = method.trim().lowercase()
            if (normalizedMethod.isEmpty()) {
                throw IllegalArgumentException("RPC method name cannot be empty")
            }
            if (normalizedMethod in blockedMutatingMethods) {
                throw SecurityException(
                    "Method '$normalizedMethod' is disabled in read-only Node Telemetry mode (no wallet, signing, broadcasting, or mining RPCs)."
                )
            }
            val paramsArray = parseParamsInput(paramsText)
            val responseJson = executeJsonRpcEnvelope(config, normalizedMethod, paramsArray)
            responseJson.toString(2)
        }
    }

    private fun parseParamsInput(paramsText: String): JSONArray {
        val trimmed = paramsText.trim()
        if (trimmed.isEmpty()) return JSONArray()
        if (trimmed.startsWith("[")) return JSONArray(trimmed)
        val arr = JSONArray()
        trimmed.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { token ->
            when {
                token.equals("true", ignoreCase = true) -> arr.put(true)
                token.equals("false", ignoreCase = true) -> arr.put(false)
                token.toLongOrNull() != null -> arr.put(token.toLong())
                token.toDoubleOrNull() != null -> arr.put(token.toDouble())
                else -> arr.put(token.trim('"'))
            }
        }
        return arr
    }

    private fun callRpcRaw(config: RpcConnectionConfig, method: String, params: JSONArray): JSONObject {
        val envelope = executeJsonRpcEnvelope(config, method, params)
        val result = envelope.optJSONObject("result")
        return result ?: throw IllegalStateException("Unexpected non-object RPC result for $method")
    }

    private fun callRpcArray(config: RpcConnectionConfig, method: String, params: JSONArray): JSONArray {
        val envelope = executeJsonRpcEnvelope(config, method, params)
        val result = envelope.optJSONArray("result")
        return result ?: throw IllegalStateException("Unexpected non-array RPC result for $method")
    }

    private fun executeJsonRpcEnvelope(
        config: RpcConnectionConfig,
        method: String,
        params: JSONArray
    ): JSONObject {
        val payload = JSONObject().apply {
            put("jsonrpc", "1.0")
            put("id", "corelens")
            put("method", method)
            put("params", params)
        }

        val reqBuilder = Request.Builder()
            .url(config.fullEndpointUrl())
            .post(payload.toString().toRequestBody(jsonMediaType))

        if (config.username.isNotBlank() || config.password.isNotBlank()) {
            reqBuilder.header("Authorization", Credentials.basic(config.username, config.password))
        }

        client.newCall(reqBuilder.build()).execute().use { response ->
            val bodyStr = response.body?.string().orEmpty()
            if (!response.isSuccessful && bodyStr.isBlank()) {
                throw IllegalStateException("HTTP ${response.code} ${response.message}")
            }
            val json = JSONObject(bodyStr)
            val errObj = json.optJSONObject("error")
            if (errObj != null) {
                val code = errObj.optInt("code", -1)
                val msg = errObj.optString("message", "Unknown RPC error")
                throw IllegalStateException("RPC Error ($code): $msg")
            }
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code}: $bodyStr")
            }
            return json
        }
    }

    private fun buildNetinfoSnapshot(networkInfo: JSONObject, peerInfo: JSONArray): NetinfoSnapshot {
        val timeNowSec = System.currentTimeMillis() / 1000L
        val knownNetworks = listOf("ipv4", "ipv6", "onion", "i2p", "cjdns", "npr")
        val inCounts = mutableMapOf<String, Int>().withDefault { 0 }
        val outCounts = mutableMapOf<String, Int>().withDefault { 0 }
        var blockRelayCount = 0
        var manualCount = 0

        val peers = mutableListOf<PeerTelemetry>()

        for (i in 0 until peerInfo.length()) {
            val p = peerInfo.optJSONObject(i) ?: continue
            val networkRaw = p.optString("network", "not_publicly_routable")
            val shortNet = when (networkRaw) {
                "not_publicly_routable" -> "npr"
                "internal" -> "int"
                else -> networkRaw
            }
            val isOutbound = !p.optBoolean("inbound", false)
            val connTypeRaw = p.optString("connection_type", "")
            if (connTypeRaw == "block-relay-only") blockRelayCount++
            if (connTypeRaw == "manual") manualCount++

            if (isOutbound) {
                outCounts[shortNet] = outCounts.getValue(shortNet) + 1
            } else {
                inCounts[shortNet] = inCounts.getValue(shortNet) + 1
            }

            val connTypeShort = when (connTypeRaw) {
                "outbound-full-relay" -> "full"
                "block-relay-only" -> "block"
                "manual", "feeler" -> connTypeRaw
                "addr-fetch" -> "addr"
                else -> connTypeRaw
            }

            val servicesArr = p.optJSONArray("servicesnames")
            val servicesStr = buildString {
                if (servicesArr != null) {
                    for (sIdx in 0 until servicesArr.length()) {
                        val s = servicesArr.optString(sIdx)
                        append(
                            when (s) {
                                "NETWORK_LIMITED" -> 'l'
                                "P2P_V2" -> '2'
                                else -> s.firstOrNull()?.lowercaseChar() ?: 'u'
                            }
                        )
                    }
                }
            }

            val connTime = p.optLong("conntime", 0L)
            val lastSend = p.optLong("lastsend", 0L)
            val lastRecv = p.optLong("lastrecv", 0L)
            val lastTxn = p.optLong("last_transaction", 0L)
            val lastBlk = p.optLong("last_block", 0L)
            val minPingSec = if (p.has("minping") && !p.isNull("minping")) p.optDouble("minping") else -1.0
            val pingSec = if (p.has("pingtime") && !p.isNull("pingtime")) p.optDouble("pingtime") else -1.0

            peers.add(
                PeerTelemetry(
                    id = p.optInt("id", i),
                    direction = if (isOutbound) "out" else "in",
                    connType = connTypeShort,
                    network = shortNet,
                    services = servicesStr,
                    transport = p.optString("transport_protocol_type", "v1"),
                    minPingMs = if (minPingSec >= 0) (minPingSec * 1000.0).roundToInt() else null,
                    pingMs = if (pingSec >= 0) (pingSec * 1000.0).roundToInt() else null,
                    lastSendSec = if (lastSend > 0) (timeNowSec - lastSend).coerceAtLeast(0L) else 0L,
                    lastRecvSec = if (lastRecv > 0) (timeNowSec - lastRecv).coerceAtLeast(0L) else 0L,
                    lastTxnMin = if (lastTxn > 0) ((timeNowSec - lastTxn) / 60L).coerceAtLeast(0L) else null,
                    lastBlockMin = if (lastBlk > 0) ((timeNowSec - lastBlk) / 60L).coerceAtLeast(0L) else null,
                    hbTo = p.optBoolean("bip152_hb_to", false),
                    hbFrom = p.optBoolean("bip152_hb_from", false),
                    addrProcessed = p.optLong("addr_processed", 0L),
                    addrRateLimited = p.optLong("addr_rate_limited", 0L),
                    ageMin = if (connTime > 0) ((timeNowSec - connTime) / 60L).coerceAtLeast(0L) else 0L,
                    mappedAs = p.optInt("mapped_as", 0),
                    address = p.optString("addr", ""),
                    subversion = p.optString("subver", ""),
                    version = p.optInt("version", 0)
                )
            )
        }

        val sortedPeers = peers.sortedWith(
            compareBy<PeerTelemetry> { it.direction == "out" }.thenBy { it.minPingMs ?: Int.MAX_VALUE }
        )

        val reachableMap = mutableMapOf<String, Boolean>()
        val netsJson = networkInfo.optJSONArray("networks")
        if (netsJson != null) {
            for (i in 0 until netsJson.length()) {
                val n = netsJson.optJSONObject(i) ?: continue
                reachableMap[n.optString("name")] = n.optBoolean("reachable", false)
            }
        }

        val networkRows = knownNetworks.mapNotNull { net ->
            val inC = inCounts.getValue(net)
            val outC = outCounts.getValue(net)
            val reachable = reachableMap[net] == true
            if (reachable || inC + outC > 0) {
                NetworkPeerCountRow(
                    network = net,
                    inbound = inC,
                    outbound = outC,
                    total = inC + outC,
                    reachable = reachable
                )
            } else {
                null
            }
        }

        val localAddrs = mutableListOf<LocalNodeAddress>()
        val localArr = networkInfo.optJSONArray("localaddresses")
        if (localArr != null) {
            for (i in 0 until localArr.length()) {
                val a = localArr.optJSONObject(i) ?: continue
                localAddrs.add(
                    LocalNodeAddress(
                        address = a.optString("address"),
                        port = a.optInt("port"),
                        score = a.optInt("score")
                    )
                )
            }
        }

        return NetinfoSnapshot(
            serverSubversion = networkInfo.optString("subversion", ""),
            protocolVersion = networkInfo.optInt("protocolversion", 0),
            networkRows = networkRows,
            blockRelayPeersCount = blockRelayCount,
            manualPeersCount = manualCount,
            peers = sortedPeers,
            localAddresses = localAddrs
        )
    }

    private fun buildAddrinfoSnapshot(nodeAddresses: JSONArray): AddrinfoSnapshot {
        val counts = linkedMapOf(
            "ipv4" to 0,
            "ipv6" to 0,
            "onion" to 0,
            "i2p" to 0,
            "cjdns" to 0
        )
        for (i in 0 until nodeAddresses.length()) {
            val item = nodeAddresses.optJSONObject(i) ?: continue
            val net = item.optString("network", "")
            if (counts.containsKey(net)) {
                counts[net] = counts.getValue(net) + 1
            }
        }
        return AddrinfoSnapshot(
            byNetwork = counts,
            total = counts.values.sum()
        )
    }
}
