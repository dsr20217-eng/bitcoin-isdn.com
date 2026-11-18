# CoreLens (Android)

CoreLens is a Kotlin + Jetpack Compose Android rewrite of the read-only node telemetry, protocol encoding, raw transaction inspection, and chain specification utilities from the imported Bitcoin Core repository.

## Features
- **Node Telemetry & `-netinfo` Monitor**: Connects over HTTP/HTTPS JSON-RPC to a user-configured `bitcoind` endpoint (`getblockchaininfo`, `getnetworkinfo`, `getpeerinfo`, `getmempoolinfo`, `getnodeaddresses`), renders `-netinfo` levels 0–4 peer metrics (direction, connection type, BIP-324 v1/v2 transport, ping/minping, services), and includes a read-only JSON-RPC console.
- **Codec & Crypto Lab**: Exact Kotlin ports of `bech32.cpp` (BIP-173 / BIP-350 with GF(1024) syndrome error localization), `base58.cpp` (Base58Check encode/decode/verify), and cryptographic digests (`SHA-256`, `Double-SHA256`, `HASH160` via RIPEMD-160, `SHA3-256`, `SHA-512`).
- **Read-Only Raw Tx Hex Inspector**: Parses legacy and SegWit (BIP-141/144) raw transaction hex into `txid`, `wtxid`, byte/stripped/weight/vsize metrics, inputs (`vin`, BIP-125 RBF flag, witness stacks), and outputs (`vout`, satoshi/BTC values, `scriptPubKey` classification & derived addresses).
- **Chain Specs & Calculators**: Reference parameters for Mainnet, Testnet3, Testnet4 (BIP-94), Signet, and Regtest, consensus constants, BTC/satoshi & weight/vByte converters, and `arith_uint256::SetCompact` `nBits` target decoding.

## Compliance Scope
To remain eligible for the Google Play Creator Program, wallet/key management (`bitcoin-wallet`), transaction signing/broadcasting, and proof-of-work header grinding (`bitcoin-util grind`) are omitted by default.

## License
Released under the MIT license. See [COPYING](COPYING).
