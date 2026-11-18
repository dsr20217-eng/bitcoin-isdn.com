# CoreLens (Android)

CoreLens is a Kotlin + Jetpack Compose Android rewrite of the read-only node telemetry, protocol encoding, raw transaction inspection, and chain specification utilities from the imported Bitcoin Core repository.

## Features
- **Multi-Chain Relays & Connectors A–Z**: Browse read-only RPC/protocol specs for 21+ blockchain networks A–Z and 10+ token standards A–Z, configure custom JSON-RPC/REST/Webhook endpoints with routing timeouts, retry counts, and custom/auth headers, and run live `OkHttp` health probes.
- **Inbox / Outbox, Webhooks & Drives/Media**: Compose outbound protocol reports with custom domain routing and sender/recipient identity, dispatch via HTTP/HTTPS webhooks (logging replies to Inbox) or device email (`ACTION_SENDTO` `mailto:`), and attach folders, documents, or media via zero-permission Android SAF (`OpenDocumentTree`, `OpenDocument`) and Photo Picker (`PickVisualMedia`).
- **Auto-Saving Local Memory**: Persists relay endpoints, inbox/outbox logs, attached source metadata, and node/routing preferences automatically via Room (`CoreLensDatabase`) and Jetpack DataStore (`CoreLensPreferences`).
- **Node Telemetry & `-netinfo` Monitor**: Connects over HTTP/HTTPS JSON-RPC to a user-configured `bitcoind` endpoint (`getblockchaininfo`, `getnetworkinfo`, `getpeerinfo`, `getmempoolinfo`, `getnodeaddresses`), renders `-netinfo` levels 0–4 peer metrics (direction, connection type, BIP-324 v1/v2 transport, ping/minping, services), and includes a read-only JSON-RPC console.
- **Codec & Crypto Lab**: Exact Kotlin ports of `bech32.cpp` (BIP-173 / BIP-350 with GF(1024) syndrome error localization), `base58.cpp` (Base58Check encode/decode/verify), and cryptographic digests (`SHA-256`, `Double-SHA256`, `HASH160` via RIPEMD-160, `SHA3-256`, `SHA-512`).
- **Read-Only Raw Tx Hex Inspector & Chain Specs**: Parses legacy and SegWit (BIP-141/144) raw transaction hex into `txid`, `wtxid`, byte/stripped/weight/vsize metrics, inputs/outputs, plus reference parameters and calculators for Bitcoin networks.

## Compliance Scope
To remain eligible for the Google Play Creator Program, cryptocurrency wallets, private key custody/signing, transaction broadcasting, token exchanges, live bank transfer rails, and on-device proof-of-work mining are omitted by default.

## License
Released under the MIT license. See [COPYING](COPYING).
