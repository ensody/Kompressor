package com.ensody.kompressor.js

import com.ensody.kompressor.core.transform
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals

class JsCompressionTest {
    @Test
    fun testRoundtrip() = runTest {
        val testData = ByteArray(100) { it.toByte() }
        checkRoundtrip(testData)
    }

    @Test
    fun testRoundtripLarge() = runTest {
        val testData = ByteArray((8192 * 3.5).toInt()) { it.toByte() }
        checkRoundtrip(testData)
    }

    private suspend fun checkRoundtrip(testData: ByteArray) {
        val compressor = JsCompressor(JsCompressionFormat.Gzip)
        val decompressor = JsDecompressor(JsCompressionFormat.Gzip)

        val compressed = compressor.transform(testData)
        val decompressed = decompressor.transform(compressed)

        assertContentEquals(testData, decompressed)
    }
}
