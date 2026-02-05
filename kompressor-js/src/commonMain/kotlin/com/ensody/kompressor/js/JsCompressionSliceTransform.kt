package com.ensody.kompressor.js

import com.ensody.kompressor.core.AsyncSliceTransform
import com.ensody.kompressor.core.ByteArraySlice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.min


internal class JsCompressionSliceTransform(
    format: String,
    isCompression: Boolean,
) : AsyncSliceTransform {
    private val interop = createJsCompressionInterop(format, isCompression)
    val scope = CoroutineScope(Dispatchers.Default)

    private var isDone = false
    private var pendingChunk: ByteArraySlice? = null

    // allocated as needed, powers of 2 like ArrayList
    private val inputs = Channel<ByteArraySlice>(Channel.UNLIMITED)

    val writerJob = scope.launch {
        inputs.consumeAsFlow()
            .distinctUntilChanged()
            .collect { input ->
                var offset = input.readStart
                val end = input.writeStart
                input.readStart = input.writeStart
                while (offset < end && currentCoroutineContext().isActive) {
                    val nextSize = min((end - offset), interop.desiredSize)
                    interop.write(input.data, offset, nextSize)
                    offset += nextSize
                    interop.awaitReady()
                }
            }
        interop.close()
    }

    override suspend fun transform(input: ByteArraySlice, output: ByteArraySlice, finish: Boolean) {
        if (input.remainingRead > 0) {
            inputs.send(input)
        }

        if (finish) {
            // this means that once all current inputs are read, the writer will be closed, which is necessary to flush the last blocks
            inputs.close()
        }

        if (output.isFull && !isDone) {
            output.insufficient = true
            return
        }

        while (currentCoroutineContext().isActive) {
            val chunk = pendingChunk
            if (chunk != null) {
                if (!writeToOutput(chunk, output, !isDone)) {
                    return
                }
            }

            val result = interop.read()
            if (result == null) {
                isDone = true
                output.insufficient = false
                scope.cancel()
                interop.cancel()
                return
            }

            if (output.isFull && !isDone) {
                output.insufficient = true
                return
            }

            if (!writeToOutput(ByteArraySlice(result), output, !isDone)) {
                return
            }
        }
    }

    /**
     * Writes as much of [chunk] to [output] as it can, and returns `true` if there's room for more.
     */
    private fun writeToOutput(chunk: ByteArraySlice, output: ByteArraySlice, isThereMore: Boolean): Boolean {
        val outputWrite = output.remainingWrite
        val chunkRead = chunk.remainingRead
        if (outputWrite >= chunkRead) {
            chunk.readInto(output, chunkRead)
            output.insufficient = isThereMore
            pendingChunk = null
            return !output.isFull
        } else {
            chunk.readInto(output, outputWrite)
            pendingChunk = chunk
            output.insufficient = true
            return false
        }
    }
}
