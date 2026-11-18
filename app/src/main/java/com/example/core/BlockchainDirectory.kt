package com.example.core

/**
 * Read-only A–Z Blockchain Protocol & Token Standard Technical Specifications.
 * Pure protocol metadata (chain IDs, address prefixes, RPC probe dialects, ABI/script standards).
 * Does NOT implement wallets, private key custody, token swaps, or trading.
 */
data class ProtocolNetworkSpec(
    val name: String,
    val ticker: String,
    val family: String,
    val chainIdentifier: String,
    val addressEncoding: String,
    val defaultRpcDialect: String,
    val probeMethod: String,
    val probeParamsJson: String,
    val blockTimeTarget: String,
    val routingNotes: String
)

data class TokenStandardSpec(
    val standard: String,
    val ecosystem: String,
    val encodingModel: String,
    val interfaceSpec: String,
    val notes: String
)

object BlockchainDirectory {

    val networksAZ: List<ProtocolNetworkSpec> = listOf(
        ProtocolNetworkSpec(
            name = "Arbitrum One",
            ticker = "ARB / ETH",
            family = "EVM L2 Rollup",
            chainIdentifier = "EIP-155: 42161",
            addressEncoding = "Hex 0x (EIP-55 Checksum)",
            defaultRpcDialect = "Ethereum JSON-RPC",
            probeMethod = "eth_blockNumber",
            probeParamsJson = "[]",
            blockTimeTarget = "~0.25 s",
            routingNotes = "Nitro optimistic rollup; supports eth_chainId and eth_getBlockByNumber."
        ),
        ProtocolNetworkSpec(
            name = "Avalanche C-Chain",
            ticker = "AVAX",
            family = "EVM / Snowman",
            chainIdentifier = "EIP-155: 43114",
            addressEncoding = "Hex 0x (C) / Bech32 avax1 (X/P)",
            defaultRpcDialect = "EVM JSON-RPC",
            probeMethod = "eth_blockNumber",
            probeParamsJson = "[]",
            blockTimeTarget = "~2.0 s",
            routingNotes = "P/X/C chain routing via /ext/bc/C/rpc, /ext/bc/X, /ext/P."
        ),
        ProtocolNetworkSpec(
            name = "Base",
            ticker = "ETH",
            family = "OP Stack EVM L2",
            chainIdentifier = "EIP-155: 8453",
            addressEncoding = "Hex 0x (EIP-55 Checksum)",
            defaultRpcDialect = "Ethereum JSON-RPC",
            probeMethod = "eth_blockNumber",
            probeParamsJson = "[]",
            blockTimeTarget = "2.0 s",
            routingNotes = "OP Stack L2 settled to Ethereum L1; standard EIP-1559 fee telemetry."
        ),
        ProtocolNetworkSpec(
            name = "Bitcoin",
            ticker = "BTC",
            family = "UTXO / PoW",
            chainIdentifier = "Mainnet / Testnet4 / Signet",
            addressEncoding = "Base58Check, Bech32 (bc1q), Bech32m (bc1p)",
            defaultRpcDialect = "Bitcoin Core JSON-RPC",
            probeMethod = "getblockchaininfo",
            probeParamsJson = "[]",
            blockTimeTarget = "600 s",
            routingNotes = "Supports -getinfo, -netinfo (BIP-324 v2), and raw tx hex inspection."
        ),
        ProtocolNetworkSpec(
            name = "Cardano",
            ticker = "ADA",
            family = "eUTXO / Ouroboros",
            chainIdentifier = "Network Magic 764824073",
            addressEncoding = "Bech32 (addr1 / stake1)",
            defaultRpcDialect = "Ogmios / Blockfrost REST",
            probeMethod = "GET /",
            probeParamsJson = "[]",
            blockTimeTarget = "20 s",
            routingNotes = "CIP-19 Bech32 addresses; JSON-WSP or REST health endpoints."
        ),
        ProtocolNetworkSpec(
            name = "Cosmos Hub",
            ticker = "ATOM",
            family = "CometBFT / IBC",
            chainIdentifier = "cosmoshub-4",
            addressEncoding = "Bech32 (cosmos1)",
            defaultRpcDialect = "CometBFT RPC / LCD REST",
            probeMethod = "status",
            probeParamsJson = "[]",
            blockTimeTarget = "~6.0 s",
            routingNotes = "Supports Tendermint /status JSON-RPC and /cosmos/base/tendermint/v1beta1 REST."
        ),
        ProtocolNetworkSpec(
            name = "Dogecoin",
            ticker = "DOGE",
            family = "UTXO / AuxPoW",
            chainIdentifier = "P2P Magic 0xc0c0c0c0",
            addressEncoding = "Base58Check (0x1e 'D')",
            defaultRpcDialect = "Bitcoin-derived JSON-RPC",
            probeMethod = "getblockchaininfo",
            probeParamsJson = "[]",
            blockTimeTarget = "60 s",
            routingNotes = "Shares UTXO RPC semantics (getblockcount, getmempoolinfo)."
        ),
        ProtocolNetworkSpec(
            name = "Ethereum",
            ticker = "ETH",
            family = "EVM / PoS",
            chainIdentifier = "EIP-155: 1",
            addressEncoding = "Hex 0x (EIP-55 Checksum)",
            defaultRpcDialect = "Ethereum JSON-RPC",
            probeMethod = "eth_blockNumber",
            probeParamsJson = "[]",
            blockTimeTarget = "12 s",
            routingNotes = "Execution JSON-RPC on 8545, Consensus Beacon REST API on 5052."
        ),
        ProtocolNetworkSpec(
            name = "Fantom / Sonic",
            ticker = "FTM / S",
            family = "EVM / Lachesis aBFT",
            chainIdentifier = "EIP-155: 250 / 146",
            addressEncoding = "Hex 0x (EIP-55 Checksum)",
            defaultRpcDialect = "Ethereum JSON-RPC",
            probeMethod = "eth_blockNumber",
            probeParamsJson = "[]",
            blockTimeTarget = "~1.0 s",
            routingNotes = "aBFT consensus with standard EVM execution JSON-RPC."
        ),
        ProtocolNetworkSpec(
            name = "Litecoin",
            ticker = "LTC",
            family = "UTXO / MWEB",
            chainIdentifier = "P2P Magic 0xfbc0b6db",
            addressEncoding = "Base58Check, Bech32 (ltc1q), MWEB (ltcmweb1)",
            defaultRpcDialect = "Bitcoin-derived JSON-RPC",
            probeMethod = "getblockchaininfo",
            probeParamsJson = "[]",
            blockTimeTarget = "150 s",
            routingNotes = "Bech32 HRP 'ltc'; supports standard UTXO RPC telemetry."
        ),
        ProtocolNetworkSpec(
            name = "NEAR Protocol",
            ticker = "NEAR",
            family = "Sharded Nightshade",
            chainIdentifier = "mainnet",
            addressEncoding = "Named (*.near) / 64-hex implicit",
            defaultRpcDialect = "NEAR JSON-RPC",
            probeMethod = "status",
            probeParamsJson = "[]",
            blockTimeTarget = "~1.0 s",
            routingNotes = "JSON-RPC POST / with method 'status' or 'block'."
        ),
        ProtocolNetworkSpec(
            name = "Optimism",
            ticker = "OP / ETH",
            family = "OP Stack EVM L2",
            chainIdentifier = "EIP-155: 10",
            addressEncoding = "Hex 0x (EIP-55 Checksum)",
            defaultRpcDialect = "Ethereum JSON-RPC",
            probeMethod = "eth_blockNumber",
            probeParamsJson = "[]",
            blockTimeTarget = "2.0 s",
            routingNotes = "OP Stack Superchain rollup with L1 data fee oracle telemetry."
        ),
        ProtocolNetworkSpec(
            name = "Polkadot",
            ticker = "DOT",
            family = "Substrate / GRANDPA",
            chainIdentifier = "SS58 Prefix: 0",
            addressEncoding = "SS58 Base58",
            defaultRpcDialect = "Substrate JSON-RPC",
            probeMethod = "system_health",
            probeParamsJson = "[]",
            blockTimeTarget = "6.0 s",
            routingNotes = "Substrate RPC methods: system_chain, system_health, chain_getHeader."
        ),
        ProtocolNetworkSpec(
            name = "Polygon PoS",
            ticker = "POL",
            family = "EVM / Heimdall+Bor",
            chainIdentifier = "EIP-155: 137",
            addressEncoding = "Hex 0x (EIP-55 Checksum)",
            defaultRpcDialect = "Ethereum JSON-RPC",
            probeMethod = "eth_blockNumber",
            probeParamsJson = "[]",
            blockTimeTarget = "2.0 s",
            routingNotes = "Bor EVM execution RPC paired with Heimdall Tendermint checkpoints."
        ),
        ProtocolNetworkSpec(
            name = "Solana",
            ticker = "SOL",
            family = "SVM / Proof of History",
            chainIdentifier = "Genesis: 5eykt4UsFv8P...",
            addressEncoding = "Base58 (32-byte Ed25519)",
            defaultRpcDialect = "Solana JSON-RPC",
            probeMethod = "getHealth",
            probeParamsJson = "[]",
            blockTimeTarget = "~0.4 s",
            routingNotes = "Supports getHealth, getSlot, getEpochInfo, getVersion."
        ),
        ProtocolNetworkSpec(
            name = "Stellar",
            ticker = "XLM",
            family = "SCP Federated Byzantine",
            chainIdentifier = "Public Global Stellar Network",
            addressEncoding = "StrKey Base32 ('G...')",
            defaultRpcDialect = "Horizon REST / Soroban RPC",
            probeMethod = "getHealth",
            probeParamsJson = "[]",
            blockTimeTarget = "~5.0 s",
            routingNotes = "Soroban JSON-RPC uses getHealth / getLatestLedger."
        ),
        ProtocolNetworkSpec(
            name = "Sui",
            ticker = "SUI",
            family = "Object-Centric Move",
            chainIdentifier = "Chain ID: 35834a8a",
            addressEncoding = "Hex 0x (32-byte Blake2b)",
            defaultRpcDialect = "Sui JSON-RPC",
            probeMethod = "sui_getLatestCheckpointSequenceNumber",
            probeParamsJson = "[]",
            blockTimeTarget = "~0.4 s",
            routingNotes = "Checkpoint sequence & object telemetry via Sui JSON-RPC."
        ),
        ProtocolNetworkSpec(
            name = "Tezos",
            ticker = "XTZ",
            family = "Tenderbake / Michelson",
            chainIdentifier = "NetXdQprcVkpaWU",
            addressEncoding = "Base58Check (tz1/tz2/tz3/KT1)",
            defaultRpcDialect = "Octez REST RPC",
            probeMethod = "GET /chains/main/blocks/head/header",
            probeParamsJson = "[]",
            blockTimeTarget = "10 s",
            routingNotes = "RESTful Octez node RPC inspecting head header and cycle constants."
        ),
        ProtocolNetworkSpec(
            name = "TRON",
            ticker = "TRX",
            family = "TVM / DPoS",
            chainIdentifier = "Mainnet TVM",
            addressEncoding = "Base58Check ('T...', 0x41)",
            defaultRpcDialect = "TronGrid / FullNode HTTP",
            probeMethod = "GET /wallet/getnowblock",
            probeParamsJson = "[]",
            blockTimeTarget = "3.0 s",
            routingNotes = "4-byte double-SHA256 Base58Check with 0x41 version byte."
        ),
        ProtocolNetworkSpec(
            name = "XRP Ledger",
            ticker = "XRP",
            family = "RPCA Consensus",
            chainIdentifier = "XRPL Mainnet (0)",
            addressEncoding = "Base58Check ('r...', custom alphabet)",
            defaultRpcDialect = "Rippled JSON-RPC",
            probeMethod = "server_info",
            probeParamsJson = "[{}]",
            blockTimeTarget = "3–5 s",
            routingNotes = "JSON-RPC POST / with method 'server_info' or 'ledger_closed'."
        ),
        ProtocolNetworkSpec(
            name = "Zcash",
            ticker = "ZEC",
            family = "UTXO / Halo 2",
            chainIdentifier = "Mainnet NU6",
            addressEncoding = "Base58Check (t1/t3), Bech32m Unified (u1)",
            defaultRpcDialect = "Zcashd / Zebrad JSON-RPC",
            probeMethod = "getblockchaininfo",
            probeParamsJson = "[]",
            blockTimeTarget = "75 s",
            routingNotes = "Transparent t-addresses use Base58Check; ZIP-316 unified uses F4Jumble + Bech32m."
        )
    )

    val tokenStandardsAZ: List<TokenStandardSpec> = listOf(
        TokenStandardSpec("BEP-20", "BNB Smart Chain (EVM)", "Solidity ABI", "name, symbol, decimals, totalSupply", "EVM-compatible fungible contract interface."),
        TokenStandardSpec("BRC-20", "Bitcoin (Ordinals JSON)", "Taproot Witness Inscription", "p=\"brc-20\", op, tick, amt", "JSON envelope encoded inside Taproot script-path witness."),
        TokenStandardSpec("CIP-25 / CIP-68", "Cardano", "CBOR Transaction Metadata", "721 metadata label / datum", "Native multi-asset policy ID + asset name encoding."),
        TokenStandardSpec("ERC-20", "Ethereum & EVM L2s", "Solidity ABI (256-bit words)", "EIP-20 read-only selectors (0x06fdde03, 0x18160ddd)", "Standard fungible contract ABI specification."),
        TokenStandardSpec("ERC-721", "Ethereum & EVM L2s", "Solidity ABI", "EIP-721 tokenURI & ownerOf selectors", "Non-fungible unique token ID schema."),
        TokenStandardSpec("ERC-1155", "Ethereum & EVM L2s", "Solidity ABI", "EIP-1155 uri(uint256) batch schema", "Multi-token ID & metadata URI template specification."),
        TokenStandardSpec("ICS-20", "Cosmos IBC", "Protobuf / JSON Packet", "FungibleTokenPacketData", "Inter-Blockchain Communication packet relay specification."),
        TokenStandardSpec("NEP-141", "NEAR Protocol", "JSON Contract Args", "ft_metadata, ft_total_supply", "NEAR fungible token interface specification."),
        TokenStandardSpec("SPL", "Solana", "Borsh Binary Account Layout", "Mint (82B) & Token-2022 Extensions", "Solana Program Library mint account binary layout."),
        TokenStandardSpec("TRC-20", "TRON (TVM)", "ABI-encoded TVM Call", "TRC-20 read selectors", "TVM smart contract ABI matching EVM 20-byte selectors.")
    )
}
