package com.ensody.kompressor.js

import com.ensody.kompressor.core.AsyncSliceTransform
import com.ensody.kompressor.core.ByteArraySlice
import com.ensody.kompressor.core.createCleaner
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.Promise

@OptIn(ExperimentalWasmJsInterop::class)
internal class JsCompressionSliceTransform(
    format: String,
    isCompression: Boolean,
) : AsyncSliceTransform {
    private class CleanableInterop(
        val interop: JsCompressionInterop,
    ) : JsCompressionInterop by interop {
        val cleanedUp = AtomicBoolean(false)
        suspend fun cleanup() {
            withContext(NonCancellable) {
                if (cleanedUp.compareAndSet(expectedValue = false, newValue = true)) {
                    interop.abort()
                }
            }
        }
    }

    private val interop = CleanableInterop(createJsCompressionInterop(format, isCompression))

    val cleanerHandle = createCleaner(interop) {
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch {
            it.cleanup()
        }
    }

    private var writerClosed = false
    private var readerDone = false

    // a read promise from the last call that didn't resolve, since there wasn't enough input
    private var pendingRead: Promise<ReadResult>? = null

    // Buffer for output chunks that couldn't fit in the output slice
    private var pendingOutput: ByteArraySlice? = null

    @OptIn(ExperimentalWasmJsInterop::class)
    override suspend fun transform(input: ByteArraySlice, output: ByteArraySlice, finish: Boolean) {
        try {
            // First drain any pending output from previous call
            val pending = pendingOutput
            if (pending != null) {
                if (!writeSliceToOutput(pending, output)) {
                    return
                }
            }

            coroutineScope {
                val writerJob = if (input.remainingRead > 0 && interop.hasRoom) {
                    val toWrite = input.remainingRead
                    launch(start = CoroutineStart.UNDISPATCHED) {
                        interop.write(input.data, input.readStart, toWrite)
                        input.readStart += toWrite
                    }
                } else {
                    null
                }

                val closeJob = if (pendingOutput == null && !writerClosed && finish) {
                    launch(start = CoroutineStart.UNDISPATCHED) {
                        writerJob?.join()
                        if (input.remainingRead == 0) {
                            interop.close()
                            writerClosed = true
                            interop.cleanedUp.store(true)
                        }
                    }
                } else {
                    null
                }

                // Read all output that is available
                while (!readerDone && currentCoroutineContext().isActive) {
                    val readPromise = pendingRead ?: interop.read()
                    val result: ReadResult =
                        if (finish) {
                            readPromise.await()
                        } else {
                            interop.getIfResolved(readPromise) ?: run {
                                pendingRead = readPromise
                                input.insufficient = true
                                break
                            }
                        }

                    pendingRead = null
                    input.insufficient = false

                    val chunk = result.bytesOrNull()
                    if (chunk == null) {
                        readerDone = true
                        if (pendingOutput == null) {
                            output.insufficient = false
                        }
                        break
                    } else {
                        if (!writeChunkToOutput(chunk, output)) {
                            writerJob?.join()
                            closeJob?.join()
                            return@coroutineScope
                        }
                    }

                    yield()
                }

                writerJob?.join()

                closeJob?.join()
            }
        } catch (e: Throwable) {
            withContext(NonCancellable) {
                interop.cleanup()
            }
            throw e
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
