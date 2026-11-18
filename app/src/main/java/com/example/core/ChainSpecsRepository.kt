package com.example.core

import java.math.BigInteger

/**
 * Chain parameter specifications and consensus constants ported from
 * Bitcoin Core's src/chainparamsbase.cpp, src/kernel/chainparams.cpp,
 * src/consensus/consensus.h, and src/arith_uint256.cpp.
 */
enum class BitcoinChain(
    val id: String,
    val displayName: String,
    val cliFlag: String,
    val p2pPort: Int,
    val rpcPort: Int,
    val torPort: Int,
    val messageStartHex: String,
    val bech32Hrp: String,
    val base58P2pkhPrefix: String,
    val base58P2shPrefix: String,
    val bip32PubPrefix: String,
    val bip94Timewarp: Boolean,
    val genesisHash: String,
    val genesisTimeUtc: String,
    val notes: String
) {
    MAINNET(
        id = "main",
        displayName = "Mainnet",
        cliFlag = "-chain=main",
        p2pPort = 8333,
        rpcPort = 8332,
        torPort = 8334,
        messageStartHex = "0xf9 0xbe 0xb4 0xd9",
        bech32Hrp = "bc",
        base58P2pkhPrefix = "0x00 ('1')",
        base58P2shPrefix = "0x05 ('3')",
        bip32PubPrefix = "0x0488B21E (xpub)",
        bip94Timewarp = false,
        genesisHash = "000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f",
        genesisTimeUtc = "2009-01-03 18:15:05 UTC",
        notes = "Production Bitcoin peer-to-peer network."
    ),
    TESTNET3(
        id = "test",
        displayName = "Testnet3",
        cliFlag = "-testnet",
        p2pPort = 18333,
        rpcPort = 18332,
        torPort = 18334,
        messageStartHex = "0x0b 0x11 0x09 0x07",
        bech32Hrp = "tb",
        base58P2pkhPrefix = "0x6f ('m'/'n')",
        base58P2shPrefix = "0xc4 ('2')",
        bip32PubPrefix = "0x043587CF (tpub)",
        bip94Timewarp = false,
        genesisHash = "000000000933ea01ad0ee984209779baaec3ced90fa3f408719526f8d77f4943",
        genesisTimeUtc = "2011-02-02 23:16:42 UTC",
        notes = "Legacy test network (deprecated in Bitcoin Core in favor of Testnet4)."
    ),
    TESTNET4(
        id = "testnet4",
        displayName = "Testnet4 (BIP-94)",
        cliFlag = "-testnet4",
        p2pPort = 48333,
        rpcPort = 48332,
        torPort = 48334,
        messageStartHex = "0x1c 0x16 0x3f 0x28",
        bech32Hrp = "tb",
        base58P2pkhPrefix = "0x6f ('m'/'n')",
        base58P2shPrefix = "0xc4 ('2')",
        bip32PubPrefix = "0x043587CF (tpub)",
        bip94Timewarp = true,
        genesisHash = "00000000da84f2bafbbc53dee25a72ae507ff4914b867c565be350b0da8bf043",
        genesisTimeUtc = "2024-05-02 20:00:00 UTC",
        notes = "Modern public test chain enforcing BIP-94 timewarp mitigation."
    ),
    SIGNET(
        id = "signet",
        displayName = "Signet",
        cliFlag = "-signet",
        p2pPort = 38333,
        rpcPort = 38332,
        torPort = 38334,
        messageStartHex = "0x0a 0x03 0xcf 0x40",
        bech32Hrp = "tb",
        base58P2pkhPrefix = "0x6f ('m'/'n')",
        base58P2shPrefix = "0xc4 ('2')",
        bip32PubPrefix = "0x043587CF (tpub)",
        bip94Timewarp = false,
        genesisHash = "00000008819873e925422c1ff0f99f7cc9bbb232af63a077a480a3633bee1ef6",
        genesisTimeUtc = "2020-09-01 16:41:59 UTC",
        notes = "Deterministic signature-gated test chain defined by -signetchallenge."
    ),
    REGTEST(
        id = "regtest",
        displayName = "Regtest",
        cliFlag = "-regtest",
        p2pPort = 18444,
        rpcPort = 18443,
        torPort = 18445,
        messageStartHex = "0xfa 0xbf 0xb5 0xda",
        bech32Hrp = "bcrt",
        base58P2pkhPrefix = "0x6f ('m'/'n')",
        base58P2shPrefix = "0xc4 ('2')",
        bip32PubPrefix = "0x043587CF (tpub)",
        bip94Timewarp = false,
        genesisHash = "0f9188f13cb7b2c71f2a335e3a4fc328bf5beb436012afca590b1a11466e2206",
        genesisTimeUtc = "2011-02-02 23:16:42 UTC",
        notes = "Local regression test chain with instant difficulty adjustment."
    )
}

data class ConsensusConstant(
    val name: String,
    val value: String,
    val description: String
)

data class CompactTargetResult(
    val valid: Boolean,
    val targetHex: String,
    val negative: Boolean,
    val overflow: Boolean,
    val error: String? = null
)

object ChainSpecsRepository {

    val consensusConstants = listOf(
        ConsensusConstant("MAX_BLOCK_SERIALIZED_SIZE", "4,000,000 bytes", "Maximum serialized block size"),
        ConsensusConstant("MAX_BLOCK_WEIGHT", "4,000,000 WU", "Maximum block weight (BIP-141)"),
        ConsensusConstant("WITNESS_SCALE_FACTOR", "4", "Base data multiplier vs witness byte"),
        ConsensusConstant("MAX_BLOCK_SIGOPS_COST", "80,000", "Consensus signature operations budget"),
        ConsensusConstant("COINBASE_MATURITY", "100 blocks", "Confirmations required before coinbase spendability"),
        ConsensusConstant("COIN", "100,000,000 sats", "Satoshis per 1.00000000 BTC"),
        ConsensusConstant("MAX_MONEY", "21,000,000 BTC", "2,100,000,000,000,000 satoshis cap"),
        ConsensusConstant("LOCKTIME_THRESHOLD", "500,000,000", "< threshold = block height; >= threshold = Unix time")
    )

    /**
     * Port of arith_uint256::SetCompact from src/arith_uint256.cpp.
     * Converts 32-bit compact nBits (e.g. 0x1d00ffff) to 256-bit target hex.
     */
    fun decodeCompactNBits(nBitsInput: String): CompactTargetResult {
        val cleaned = nBitsInput.trim().removePrefix("0x").removePrefix("0X")
        val nCompact = cleaned.toLongOrNull(16)
            ?: return CompactTargetResult(false, "", false, false, "Enter a 32-bit hex value (e.g. 1d00ffff)")
        if (nCompact < 0 || nCompact > 0xFFFFFFFFL) {
            return CompactTargetResult(false, "", false, false, "nBits must fit in 32 bits")
        }

        val nSize = (nCompact ushr 24).toInt()
        var nWord = nCompact and 0x007fffffL
        val target = if (nSize <= 3) {
            nWord = nWord ushr (8 * (3 - nSize))
            BigInteger.valueOf(nWord)
        } else {
            BigInteger.valueOf(nWord).shiftLeft(8 * (nSize - 3))
        }

        val negative = nWord != 0L && (nCompact and 0x00800000L) != 0L
        val overflow = nWord != 0L && (
            (nSize > 34) ||
                (nWord > 0xffL && nSize > 33) ||
                (nWord > 0xffffL && nSize > 32)
            )

        val hex = if (overflow) {
            "OVERFLOW (> 256-bit)"
        } else {
            target.toString(16).padStart(64, '0')
        }

        return CompactTargetResult(
            valid = !negative && !overflow && target > BigInteger.ZERO,
            targetHex = hex,
            negative = negative,
            overflow = overflow
        )
    }
}
