package com.ensody.kompressor.zlib

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

internal class ZlibTest {
    val testData = Random.nextBytes(1024 * 1024 + 3)
    val input = Buffer().apply { write(testData) }

    @Test
    fun tinySample() {
        val tinyTestData = Random.nextBytes(9000)
        val compressed = ZlibCompressor(ZlibFormat.Zlib).transform(tinyTestData)
        val decompressed = ZlibDecompressor().transform(compressed)
        assertContentEquals(tinyTestData, decompressed)
    }

    @Test
    fun sampleRoundtrip() {
        val compressed = ZlibCompressor(ZlibFormat.Zlib).transform(testData)
        val decompressed = ZlibDecompressor().transform(compressed)
        assertContentEquals(testData, decompressed)
    }

    @Test
    fun sampleRoundtripKotlinxIoSource() {
        val source = input.pipe(ZlibCompressor(ZlibFormat.Zlib)).pipe(ZlibDecompressor())
        assertContentEquals(testData, source.buffered().readByteArray())
    }

    @Test
    fun sampleRoundtripKotlinxIoSink() {
        val output = Buffer()
        val sink = ZlibCompressor(ZlibFormat.Zlib).pipe(ZlibDecompressor().pipe(output))
        input.transferTo(sink)
        sink.close()
        assertContentEquals(testData, output.readByteArray())
    }

    @Test
    fun largeSample() {
        val seed = 42
        val size: Long = 256L * 1024 * 1024 + 3
        val reference = RandomSource(Random(seed = seed), size).buffered()
        val source = RandomSource(Random(seed = seed), size)
            .pipe(ZlibCompressor(ZlibFormat.Zlib)).pipe(ZlibDecompressor()).buffered()
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
    fun sampleHelloDeflate() {
        // The compressed sample was created with:
        // echo -n "hello compression world" | \
        //   python3 -c "import zlib,sys; sys.stdout.buffer.write(zlib.compress(sys.stdin.buffer.read()))" | \
        //   base64
        val compressed = Base64.decode("eJzLSM3JyVdIzs8tKEotLs7Mz1Mozy/KSQEAbW0JLw==")
        val sample = "hello compression world"
        assertContentEquals(
            compressed,
            ZlibCompressor(ZlibFormat.Zlib).transform(sample.encodeToByteArray()),
        )

        val decompressed = ZlibDecompressor(ZlibFormat.Zlib).transform(compressed)
        assertEquals(sample, decompressed.decodeToString())

        val autoDecompressed = ZlibDecompressor(ZlibFormat.AutoDetectZlibGzip).transform(compressed)
        assertEquals(sample, autoDecompressed.decodeToString())
    }

    @Test
    fun sampleHelloGzip() {
        // The compressed sample was created with:
        // echo -n "hello compression world" | gzip -c | base64
        val compressed = Base64.decode("H4sIAIUNSGkAA8tIzcnJV0jOzy0oSi0uzszPUyjPL8pJAQDFwzyrFwAAAA==")
        val sample = "hello compression world"

        val decompressed = ZlibDecompressor(ZlibFormat.Gzip).transform(compressed)
        assertEquals(sample, decompressed.decodeToString())

        val autoDecompressed = ZlibDecompressor(ZlibFormat.AutoDetectZlibGzip).transform(compressed)
        assertEquals(sample, autoDecompressed.decodeToString())
    }
}
