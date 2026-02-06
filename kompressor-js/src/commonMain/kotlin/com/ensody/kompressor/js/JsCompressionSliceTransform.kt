package com.ensody.kompressor.js

import com.ensody.kompressor.core.AsyncSliceTransform
import com.ensody.kompressor.core.ByteArraySlice
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.min

/**
 * JS Compression Stream transform implementation.
 *
 * ## Algorithm
 *
 * The JS Streams API's `write()` Promise only resolves once the internal queue has space.
 * To avoid blocking, we use `desiredSize` which indicates how many bytes can be written
 * without the write Promise blocking.
 *
 * ### When `finish=false`:
 * - Buffer input data (don't need to consume all - caller will call again)
 * - No stream operations yet - we accumulate until finish
 *
 * ### When `finish=true`:
 * - Write buffered input to stream, respecting `desiredSize` to avoid blocking
 * - Read available output chunks
 * - If more work remains (input to write or output to read), set `insufficient=true`
 * - Close writer when all input is written
 * - Done when reader returns null (stream closed)
 *
 * This approach requires no background coroutines - all operations complete within
 * a single `transform` call by interleaving small writes and reads.
 */
internal class JsCompressionSliceTransform(
    format: String,
    isCompression: Boolean,
) : AsyncSliceTransform {
    private val interop = createJsCompressionInterop(format, isCompression)

    private var writerClosed = false
    private var readerDone = false

    // Buffer for output chunks that couldn't fit in the output slice
    private var pendingOutput: ByteArraySlice? = null

    override suspend fun transform(input: ByteArraySlice, output: ByteArraySlice, finish: Boolean) {
        // First drain any pending output from previous call
        val pending = pendingOutput
        if (pending != null) {
            if (!writeSliceToOutput(pending, output)) {
                return
            }
        }

        if (input.remainingRead > 0 && interop.desiredSize > 0) {
            val toWrite = min(input.remainingRead, interop.desiredSize)
            interop.write(input.data, input.readStart, toWrite)
            input.readStart += toWrite
        }

        // When finish=false, we just buffer input - no reads yet (would block)
        if (!finish) {
            return
        }

        // finish=true: now we flush and read output
        coroutineScope {
            // close will block until all output is read - we join this job after reading
            val closeJob = if (input.remainingRead == 0 && pendingOutput == null && !writerClosed) {
                launch {
                    interop.close()
                    writerClosed = true
                }
            } else {
                null
            }

            // Read all output
            if (!readerDone) {
                val chunk = interop.read()
                if (chunk == null) {
                    readerDone = true
                } else {
                    if (!writeChunkToOutput(chunk, output)) {
                        return@coroutineScope
                    }
                }
            }

            closeJob?.join()

            // Determine if we need more calls
            val moreWork = !readerDone || pendingOutput != null
            output.insufficient = moreWork
        }
    }

    /**
     * Writes a chunk (ByteArray) to output, returns true if fully written.
     */
    private fun writeChunkToOutput(chunk: ByteArray, output: ByteArraySlice): Boolean {
        return writeSliceToOutput(ByteArraySlice(chunk, 0, chunk.size, chunk.size), output)
    }

    /**
     * Writes a slice to output, returns true if fully written, false if output is full.
     */
    private fun writeSliceToOutput(chunk: ByteArraySlice, output: ByteArraySlice): Boolean {
        val outputWrite = output.remainingWrite
        val chunkRead = chunk.remainingRead
        if (outputWrite >= chunkRead) {
            chunk.readInto(output, chunkRead)
            pendingOutput = null
            return true
        } else if (outputWrite > 0) {
            chunk.readInto(output, outputWrite)
            pendingOutput = chunk
            output.insufficient = true
            return false
        } else {
            pendingOutput = chunk
            output.insufficient = true
            return false
        }
    }
}
