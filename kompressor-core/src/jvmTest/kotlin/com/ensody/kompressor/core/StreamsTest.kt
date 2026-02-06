package com.ensody.kompressor.core

import com.ensody.kompressor.kotlinx.io.pipe
import com.ensody.kompressor.test.RandomSource
import kotlinx.io.buffered
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class StreamsTest {
    private val testData = Random.nextBytes(1024 * 128 + 3)

    @Test
    fun testGzipRoundtrip() {
        val compressor = StreamCompressor { GZIPOutputStream(it) }
        val decompressor = StreamDecompressor { GZIPInputStream(it) }

        val compressed = compressor.transform(testData)
        val decompressed = decompressor.transform(compressed)

        assertContentEquals(testData, decompressed)
    }

    @Test
    fun testGzipRoundtripSmall() {
        val testDataSmall = Random.nextBytes(7000)
        val compressor = StreamCompressor { GZIPOutputStream(it) }
        val decompressor = StreamDecompressor { GZIPInputStream(it) }

        val compressed = compressor.transform(testDataSmall)
        val decompressed = decompressor.transform(compressed)

        assertContentEquals(testDataSmall, decompressed)
    }

    @Test
    fun testGzipCompression() {
        val compressor = StreamCompressor { GZIPOutputStream(it) }
        val expected = ByteArrayOutputStream()
        GZIPOutputStream(expected).use { it.write(testData) }

        val compressed = compressor.transform(testData)

        assertContentEquals(expected.toByteArray(), compressed)
    }

    @Test
    fun testGzipDecompression() {
        val compressed = ByteArrayOutputStream()
        GZIPOutputStream(compressed).use { it.write(testData) }

        val decompressor = StreamDecompressor { GZIPInputStream(it) }

        val decompressed = decompressor.transform(compressed.toByteArray())

        assertContentEquals(testData, decompressed)
    }

    @Test
    fun testInputStreamTransformTiny() {
        val tinyData = Random.nextBytes(100)
        val identity = StreamDecompressor { it }
        val result = identity.transform(tinyData)
        assertContentEquals(tinyData, result)
    }

    @Test
    fun testOutputStreamTransformTiny() {
        val tinyData = Random.nextBytes(100)
        val identity = StreamCompressor { it }
        val result = identity.transform(tinyData)
        assertContentEquals(tinyData, result)
    }

    @Test
    fun testGzipRoundtripRandomSource() {
        val seed = 42
        val size: Long = 1024 * 1024 + 3
        val compressor = StreamCompressor { GZIPOutputStream(it) }
        val decompressor = StreamDecompressor { GZIPInputStream(it) }

        val reference = RandomSource(Random(seed), size).buffered()
        val source = RandomSource(Random(seed), size)
            .pipe(compressor).pipe(decompressor).buffered()

        val referenceBlock = ByteArray(8192)
        val sourceBlock = ByteArray(referenceBlock.size)
        do {
            val read = source.readAtMostTo(sourceBlock)
            val endIndex = if (read >= 0) read else referenceBlock.size
            assertEquals(reference.readAtMostTo(referenceBlock, endIndex = endIndex), read)
            assertContentEquals(referenceBlock, sourceBlock)
        } while (read != -1)
    }
}
