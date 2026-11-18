package com.example.data.rpc

import com.example.core.BitcoinChain

data class RpcConnectionConfig(
    val hostUrl: String = "http://127.0.0.1",
    val port: Int = 8332,
    val username: String = "",
    val password: String = "",
    val chain: BitcoinChain = BitcoinChain.MAINNET
) {
    fun fullEndpointUrl(): String {
        val trimmed = hostUrl.trim().trimEnd('/')
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "http://$trimmed"
        }
        val withoutScheme = withScheme.substringAfter("://")
        return if (withoutScheme.contains(":") || withoutScheme.contains("/")) {
            withScheme
        } else {
            "$withScheme:$port"
        }
    }
}

data class NodeOverview(
    val chain: String,
    val clientVersion: Int,
    val subversion: String,
    val protocolVersion: Int,
    val blocks: Long,
    val headers: Long,
    val verificationProgress: Double,
    val difficulty: Double,
    val connectionsIn: Int,
    val connectionsOut: Int,
    val connectionsTotal: Int,
    val mempoolTxCount: Int,
    val mempoolBytes: Long,
    val relayFee: Double,
    val warnings: String
)

data class NetworkPeerCountRow(
    val network: String,
    val inbound: Int,
    val outbound: Int,
    val total: Int,
    val reachable: Boolean
)

data class PeerTelemetry(
    val id: Int,
    val direction: String, // "in" or "out"
    val connType: String,  // "full", "block", "manual", "feeler", "addr"
    val network: String,   // "ipv4", "ipv6", "onion", "i2p", "cjdns", "npr"
    val services: String,  // e.g. "nwl2"
    val transport: String, // "v1" or "v2" (BIP-324)
    val minPingMs: Int?,
    val pingMs: Int?,
    val lastSendSec: Long,
    val lastRecvSec: Long,
    val lastTxnMin: Long?,
    val lastBlockMin: Long?,
    val hbTo: Boolean,
    val hbFrom: Boolean,
    val addrProcessed: Long,
    val addrRateLimited: Long,
    val ageMin: Long,
    val mappedAs: Int,
    val address: String,
    val subversion: String,
    val version: Int
)

data class LocalNodeAddress(
    val address: String,
    val port: Int,
    val score: Int
)

data class NetinfoSnapshot(
    val serverSubversion: String,
    val protocolVersion: Int,
    val networkRows: List<NetworkPeerCountRow>,
    val blockRelayPeersCount: Int,
    val manualPeersCount: Int,
    val peers: List<PeerTelemetry>,
    val localAddresses: List<LocalNodeAddress>
)

data class AddrinfoSnapshot(
    val byNetwork: Map<String, Int>,
    val total: Int
)

data class NodeTelemetryBundle(
    val overview: NodeOverview,
    val netinfo: NetinfoSnapshot,
    val addrinfo: AddrinfoSnapshot,
    val fetchedAtMillis: Long
)
