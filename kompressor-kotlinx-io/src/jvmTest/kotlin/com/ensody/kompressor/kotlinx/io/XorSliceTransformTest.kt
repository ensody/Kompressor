package com.ensody.kompressor.kotlinx.io

import com.ensody.kompressor.core.ByteArraySlice
import com.ensody.kompressor.core.SliceTransform
import com.ensody.kompressor.core.transform
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

internal class XorSliceTransformTest {
    private val testData = Random.nextBytes(16 * 1024 + 3)
    private val input = Buffer().apply { write(testData) }
    private val xorByte: Byte = 0x63
    private val transformedData = testData.map { it.xor(xorByte) }.toByteArray()
    private val xorSliceTransform = XorSliceTransform(xorByte)

    @Test
    fun transformOnceTest() {
        val numBytes = 1024
        val inputSlice = ByteArraySlice(testData, writeStart = numBytes)
        val outputSlice = ByteArraySlice(numBytes)
        xorSliceTransform.transform(inputSlice, outputSlice, finish = true)
        assertEquals(0, inputSlice.remainingRead)
        assertEquals(numBytes, outputSlice.remainingRead)
        assertFalse(outputSlice.insufficient)
        assertContentEquals(
            transformedData.sliceArray(0..<numBytes),
            outputSlice.data,
        )
    }

    @Test
    fun transformTest() {
        assertContentEquals(transformedData, xorSliceTransform.transform(testData))
    }

    @Test
    fun source() {
        assertContentEquals(transformedData, input.pipe(xorSliceTransform).buffered().readByteArray())
    }

    @Test
    fun sink() {
        val output = Buffer()
        xorSliceTransform.pipe(output).write(input, testData.size.toLong())
        assertContentEquals(transformedData, output.readByteArray())
    }

    @Test
    fun roundtrip() {
        val output = Buffer()
        input.pipe(xorSliceTransform).buffered().transferTo(xorSliceTransform.pipe(output))
        assertContentEquals(
            testData,
            output.readByteArray(),
        )
    }
}

private class XorSliceTransform(val byte: Byte) : SliceTransform {
    override fun transform(input: ByteArraySlice, output: ByteArraySlice, finish: Boolean) {
        output.insufficient = output.remainingWrite < input.remainingRead
        val size = min(input.remainingRead, output.remainingWrite)
        repeat(size) { index ->
            output.data[output.writeStart + index] = input.data[input.readStart + index].xor(byte)
        }
        input.readStart += size
        output.writeStart += size
    }
}
