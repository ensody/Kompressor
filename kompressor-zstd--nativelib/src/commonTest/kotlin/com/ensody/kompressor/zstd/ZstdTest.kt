package com.ensody.kompressor.zstd

import com.ensody.kompressor.core.transform
import com.ensody.kompressor.kotlinx.io.pipe
import com.ensody.kompressor.test.RandomSource
import kotlinx.io.Buffer
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import kotlin.io.encoding.Base64
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

internal class ZstdTest {
    val testData = Random.nextBytes(1024 * 1024 + 3)
    val input = Buffer().apply { write(testData) }

    @Test
    fun tinySample() {
        val tinyTestData = Random.nextBytes(9000)
        val compressed = ZstdCompressor(compressionLevel = 3).transform(tinyTestData)
        val decompressed = ZstdDecompressor().transform(compressed)
        assertContentEquals(tinyTestData, decompressed)
    }

    @Test
    fun sampleRoundtrip() {
        val compressed = ZstdCompressor(compressionLevel = 3).transform(testData)
        val decompressed = ZstdDecompressor().transform(compressed)
        assertContentEquals(testData, decompressed)
    }

    @Test
    fun sampleRoundtripKotlinxIoSource() {
        val source = input.pipe(ZstdCompressor(compressionLevel = 3)).pipe(ZstdDecompressor())
        assertContentEquals(testData, source.buffered().readByteArray())
    }

    @Test
    fun sampleRoundtripKotlinxIoSink() {
        val output = Buffer()
        val sink = ZstdCompressor(compressionLevel = 3).pipe(ZstdDecompressor().pipe(output))
        input.transferTo(sink)
        sink.close()
        assertContentEquals(testData, output.readByteArray())
    }

    @Test
    fun largeSample() {
        val seed = 42
        val size: Long = 256 * 1024 * 1024 + 3
        val reference = RandomSource(Random(seed = seed), size).buffered()
        val source = RandomSource(Random(seed = seed), size).buffered()
        source.pipe(ZstdCompressor(compressionLevel = 3)).pipe(ZstdDecompressor())
        val referenceBlock = ByteArray(8192)
        val sourceBlock = ByteArray(referenceBlock.size)
        do {
            val read = source.readAtMostTo(sourceBlock)
            assertEquals(reference.readAtMostTo(referenceBlock), read)
            assertContentEquals(referenceBlock, sourceBlock)
        } while (read != -1)
    }

    @Test
    fun sampleHello() {
        // The compressed sample was created with the official zstd command line app
        val compressed = Base64.decode("KLUv/QRYuQAAaGVsbG8gY29tcHJlc3Npb24gd29ybGR8Qm9f")
        val decompressed = ZstdDecompressor().transform(compressed)
        assertEquals("hello compression world", decompressed.decodeToString())
    }
}
