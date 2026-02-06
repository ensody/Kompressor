package com.ensody.kompressor.kotlinx.io

import com.ensody.kompressor.core.AsyncSliceTransform
import com.ensody.kompressor.core.ByteArraySlice
import com.ensody.kompressor.core.transform
import com.ensody.kompressor.test.RandomSource
import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import kotlin.experimental.xor
import kotlin.math.min
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse

internal class AsyncXorSliceTransformTest {
    private val testData = Random.nextBytes(16 * 1024 + 3)
    private val input = Buffer().apply { write(testData) }
    private val xorByte: Byte = 0x63
    private val transformedData = testData.map { it.xor(xorByte) }.toByteArray()
    private val xorAsyncSliceTransform = AsyncXorSliceTransform(xorByte)

    @Test
    fun transformOnceTest() = runTest {
        val numBytes = 1024
        val inputSlice = ByteArraySlice(testData, writeStart = numBytes)
        val outputSlice = ByteArraySlice(numBytes)
        xorAsyncSliceTransform.transform(inputSlice, outputSlice, finish = true)
        assertEquals(0, inputSlice.remainingRead)
        assertEquals(numBytes, outputSlice.remainingRead)
        assertFalse(outputSlice.insufficient)
        assertContentEquals(
            transformedData.sliceArray(0..<numBytes),
            outputSlice.data,
        )
    }

    @Test
    fun transformTest() = runTest {
        assertContentEquals(transformedData, xorAsyncSliceTransform.transform(testData))
    }

    @Test
    fun helperTransform() = runTest {
        val output = Buffer()
        val helper = AsyncBufferSliceTransformHelper(xorAsyncSliceTransform)
        var bytesRead = 0
        while (bytesRead < testData.size) {
            bytesRead += helper.transform(input, output, testData.size - bytesRead, finish = false)
        }
        helper.finishInto(output)
        assertContentEquals(transformedData, output.readByteArray())
    }

    @Test
    fun source() = runTest {
        val helper = AsyncBufferSliceTransformHelper(xorAsyncSliceTransform)
        val output = Buffer()
        var bytesRead = 0
        while (bytesRead < testData.size) {
            bytesRead += helper.transform(input, output, testData.size - bytesRead, finish = true)
        }
        helper.finishInto(output)
        assertContentEquals(transformedData, output.readByteArray())
    }

    @Test
    fun sink() = runTest {
        val helper = AsyncBufferSliceTransformHelper(xorAsyncSliceTransform)
        val output = Buffer()
        var bytesRead = 0
        while (bytesRead < testData.size) {
            bytesRead += helper.transform(input, output, testData.size - bytesRead, finish = true)
        }
        helper.finishInto(output)
        assertContentEquals(transformedData, output.readByteArray())
    }

    @Test
    fun roundtrip() = runTest {
        val compressedBuffer = Buffer()
        val decompressedBuffer = Buffer()
        val compressorHelper = AsyncBufferSliceTransformHelper(xorAsyncSliceTransform)
        val decompressorHelper = AsyncBufferSliceTransformHelper(xorAsyncSliceTransform)

        var bytesRead = 0
        while (bytesRead < testData.size) {
            bytesRead += compressorHelper.transform(input, compressedBuffer, testData.size - bytesRead, finish = true)
        }
        compressorHelper.finishInto(compressedBuffer)

        var compressedBytesRead = 0
        val compressedDataSize = compressedBuffer.size.toInt()
        while (compressedBytesRead < compressedDataSize) {
            compressedBytesRead += decompressorHelper.transform(
                compressedBuffer,
                decompressedBuffer,
                compressedDataSize - compressedBytesRead,
                finish = true,
            )
        }
        decompressorHelper.finishInto(decompressedBuffer)

        assertContentEquals(testData, decompressedBuffer.readByteArray())
    }

    @Test
    fun transformBufferToSink() = runTest {
        val output = Buffer()
        xorAsyncSliceTransform.transform(input, output)
        assertContentEquals(transformedData, output.readByteArray())
    }

    @Test
    fun transformRandomSourceToSink() = runTest {
        val seed = 1234L
        val size = 16 * 1024L + 7
        val source = RandomSource(Random(seed), size).buffered()
        val referenceBuffer = Buffer().apply { source.readTo(this, size) }
        val referenceData = referenceBuffer.readByteArray()
        val expectedData = referenceData.map { it.xor(xorByte) }.toByteArray()

        val input = Buffer().apply { RandomSource(Random(seed), size).buffered().readTo(this, size) }
        val output = Buffer()
        xorAsyncSliceTransform.transform(input, output)
        assertContentEquals(expectedData, output.readByteArray())
    }
}

private class AsyncXorSliceTransform(val byte: Byte) : AsyncSliceTransform {
    override suspend fun transform(input: ByteArraySlice, output: ByteArraySlice, finish: Boolean) {
        output.insufficient = output.remainingWrite < input.remainingRead
        val size = min(input.remainingRead, output.remainingWrite)
        repeat(size) { index ->
            output.data[output.writeStart + index] = input.data[input.readStart + index].xor(byte)
        }
        input.readStart += size
        output.writeStart += size
    }
}
