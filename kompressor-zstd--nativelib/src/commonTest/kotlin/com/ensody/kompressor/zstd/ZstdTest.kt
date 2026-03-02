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
    fun dictionaryRoundtrip() {
        val dictionary = "this is a test dictionary".encodeToByteArray()
        val data = "this is some data to compress using a dictionary".encodeToByteArray()
        val compressed = ZstdCompressor(dictionary = dictionary).transform(data)
        val decompressed = ZstdDecompressor(dictionary = dictionary).transform(compressed)
        assertContentEquals(data, decompressed)
    }

    @Test
    fun largeSample() {
        val seed = 42
        val size: Long = 256L * 1024 * 1024 + 3
        val reference = RandomSource(Random(seed = seed), size).buffered()
        val source = RandomSource(Random(seed = seed), size)
            .pipe(ZstdCompressor(compressionLevel = 3)).pipe(ZstdDecompressor()).buffered()
        val referenceBlock = ByteArray(8192)
        val sourceBlock = ByteArray(referenceBlock.size)
        do {
            val read = source.readAtMostTo(sourceBlock)
            val endIndex = if (read >= 0) read else referenceBlock.size
            assertEquals(reference.readAtMostTo(referenceBlock, endIndex = endIndex), read)
            assertContentEquals(referenceBlock, sourceBlock)
        } while (read != -1)
    }

    @Test
    fun sampleHello() {
        // The compressed sample was created with:
        // echo -n "hello compression world" | zstd -c | base64
        val compressed = Base64.decode("KLUv/QRYuQAAaGVsbG8gY29tcHJlc3Npb24gd29ybGR8Qm9f")
        val decompressed = ZstdDecompressor().transform(compressed)
        assertEquals("hello compression world", decompressed.decodeToString())
    }
}
