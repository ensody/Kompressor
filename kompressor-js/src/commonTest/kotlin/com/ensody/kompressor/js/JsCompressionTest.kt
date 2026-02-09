package com.ensody.kompressor.js

import com.ensody.kompressor.core.transform
import com.ensody.kompressor.kotlinx.io.transform
import com.ensody.kompressor.test.RandomSource
import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import kotlin.random.Random
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

    @Test
    fun testRoundtripRandomSource() = runTest {
        val seed = 42L
        val size: Long = 1024 * 1024 + 3
        val referenceData =
            Buffer().apply { RandomSource(Random(seed), size).buffered().readTo(this, size) }.readByteArray()
        val input = Buffer().apply { RandomSource(Random(seed), size).buffered().readTo(this, size) }

        val compressed = Buffer()
        JsCompressor(JsCompressionFormat.Gzip).transform(input, compressed)

        val decompressed = Buffer()
        JsDecompressor(JsCompressionFormat.Gzip).transform(compressed, decompressed)

        assertContentEquals(referenceData, decompressed.readByteArray())
    }

    private suspend fun checkRoundtrip(testData: ByteArray) {
        val compressor = JsCompressor(JsCompressionFormat.Gzip)
        val decompressor = JsDecompressor(JsCompressionFormat.Gzip)

        val compressed = compressor.transform(testData)
        val decompressed = decompressor.transform(compressed)

        assertContentEquals(testData, decompressed)
    }
}
