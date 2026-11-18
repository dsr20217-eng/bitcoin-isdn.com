package com.example

import com.example.core.Base58Codec
import com.example.core.Bech32Codec
import com.example.core.BitcoinCrypto
import com.example.core.ChainSpecsRepository
import com.example.core.RawTxParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolCodecTest {

    @Test
    fun testBech32AndBech32mOfficialVectors() {
        val v0 = Bech32Codec.inspectSegwitAddress("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4")
        assertTrue(v0.valid)
        assertEquals(Bech32Codec.Encoding.BECH32, v0.encoding)
        assertEquals("bc", v0.hrp)
        assertEquals(0, v0.witnessVersion)
        assertEquals("751e76e8199196d454941c45d1b3a323f1433bd6", v0.witnessProgramHex)

        val v1 = Bech32Codec.inspectSegwitAddress("bc1p5cyxnuxmeuwuvkwfem96lqzszd02n6xdcjrs20cac6yqjjwudpxqkedrcr")
        assertTrue(v1.valid)
        assertEquals(Bech32Codec.Encoding.BECH32M, v1.encoding)
        assertEquals(1, v1.witnessVersion)
        assertEquals(32, v1.witnessProgramLength)

        val typo = Bech32Codec.inspectSegwitAddress("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t5")
        assertFalse(typo.valid)
        assertTrue(typo.errorIndices.isNotEmpty())
    }

    @Test
    fun testBase58CheckAndHash160() {
        val satoshiAddr = "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"
        val inspected = Base58Codec.inspectCheck(satoshiAddr)
        assertTrue(inspected.valid)
        assertEquals(0x00, inspected.versionByte)
        assertEquals(satoshiAddr, Base58Codec.encodeCheck(inspected.payload))

        val hash160 = BitcoinCrypto.bytesToHex(BitcoinCrypto.hash160("Bitcoin".toByteArray()))
        assertEquals(40, hash160.length)
    }

    @Test
    fun testRawTxParserBip141Vector() {
        val parsed = RawTxParser.parseHex(RawTxParser.BIP141_SAMPLE_HEX, "bc").getOrNull()
        assertNotNull(parsed)
        parsed!!
        assertEquals(2, parsed.version)
        assertTrue(parsed.isSegwit)
        assertEquals(1, parsed.inputs.size)
        assertEquals(2, parsed.outputs.size)
        assertTrue(parsed.inputs[0].rbfOptIn)
        assertEquals(2, parsed.inputs[0].witnessItems.size)
        assertEquals("P2WPKH (SegWit v0)", parsed.outputs[0].scriptType)
    }

    @Test
    fun testCompactNBitsGenesisTarget() {
        val genesis = ChainSpecsRepository.decodeCompactNBits("1d00ffff")
        assertTrue(genesis.valid)
        assertEquals(
            "00000000ffff0000000000000000000000000000000000000000000000000000",
            genesis.targetHex
        )
    }
}
