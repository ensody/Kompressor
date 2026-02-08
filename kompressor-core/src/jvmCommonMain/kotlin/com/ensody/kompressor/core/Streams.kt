package com.ensody.kompressor.core

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.min

public fun StreamDecompressor(streamFactory: (InputStream) -> InputStream): SliceTransform =
    InputStreamTransform(streamFactory)

public fun StreamCompressor(streamFactory: (OutputStream) -> OutputStream): SliceTransform =
    OutputStreamTransform(streamFactory)

private class NeedsMoreInputException : IOException()

private class InputStreamTransform(val streamFactory: (InputStream) -> InputStream) : SliceTransform {

    private val state = State()
    private lateinit var cleanerHandle: Any

    private class State {
        lateinit var streamFrom: ByteArraySlice
        var finishing = false
        var closed = false
    }

    // some streams like to read on initialization, so we need to make sure streamFrom is set first
    private val inputStream by lazy {
        val state = this.state
        val stream = streamFactory(
            object : InputStream() {
                override fun read(): Int {
                    if (state.streamFrom.remainingRead == 0) {
                        if (state.finishing) return -1
                        throw NeedsMoreInputException()
                    }
                    return state.streamFrom.data[state.streamFrom.readStart++].toInt() and 0xFF
                }

                override fun read(b: ByteArray, off: Int, len: Int): Int {
                    if (state.streamFrom.remainingRead == 0) {
                        if (state.finishing) return -1
                        throw NeedsMoreInputException()
                    }
                    val toRead = min(min(len, state.streamFrom.remainingRead), b.size - off)
                    state.streamFrom.readInto(b, off, toRead)
                    return toRead
                }

                override fun skip(n: Long): Long {
                    val toSkip = min(n, state.streamFrom.remainingRead.toLong()).toInt()
                    state.streamFrom.readStart += toSkip
                    return toSkip.toLong()
                }

                override fun available(): Int {
                    return state.streamFrom.remainingRead
                }
            },
        )

        cleanerHandle = createCleaner(stream) {
            if (!state.closed) {
                state.closed = true
                stream.close()
            }
        }

        stream
    }

    override fun transform(
        input: ByteArraySlice,
        output: ByteArraySlice,
        finish: Boolean,
    ) {
        try {
            this.state.finishing = finish
            if (!input.hasData && !finish) {
                return
            }
            state.streamFrom = input

            val result = try {
                inputStream.read(output.data, output.writeStart, output.remainingWrite)
            } catch (_: NeedsMoreInputException) {
                output.insufficient = true
                return
            }
            if (result == -1) {
                // eof
                output.insufficient = false
                inputStream.close()
                state.closed = true
            } else {
                output.writeStart += result
                // Stream may have more data even if result < remainingWrite
                output.insufficient = inputStream.available() > 0
            }
        } catch (e: Throwable) {
            if (!state.closed) {
                state.closed = true
                inputStream.close()
            }
            throw e
        }
    }
}

private class OutputStreamTransform(val streamFactory: (OutputStream) -> OutputStream) : SliceTransform {
    private val state = State()
    private lateinit var cleanerHandle: Any

    private class State {
        var sliceOutputStream: SliceOutputStream? = null
        var internalOutputStream: OutputStream? = null
        var closed = false
    }

    override fun transform(
        input: ByteArraySlice,
        output: ByteArraySlice,
        finish: Boolean,
    ) {
        try {
            // Initialize stream on first call - must set output BEFORE creating wrapped stream
            // because some streams (like GZIPOutputStream) write header data during construction
            val sliceOut = state.sliceOutputStream ?: run {
                val stream = SliceOutputStream()
                state.sliceOutputStream = stream
                stream.setOutput(output)
                val internal = streamFactory(stream)
                state.internalOutputStream = internal

                val state = this.state
                cleanerHandle = createCleaner(internal) {
                    if (!state.closed) {
                        state.closed = true
                        internal.close()
                    }
                }

                stream
            }

            // Set the current output slice for direct writing
            sliceOut.setOutput(output)

            // First, drain any buffered overflow data from previous calls
            sliceOut.drainBuffer()

            // Write any remaining input to the compressor (only if output has space and no buffered data)
            if (input.remainingRead > 0 && !state.closed && !sliceOut.hasBufferedData()) {
                val readRemaining = input.remainingRead
                val readStart = input.readStart
                input.readStart += readRemaining
                state.internalOutputStream?.write(input.data, readStart, readRemaining)
            }

            // Close the stream when finishing (to flush all compressed data)
            if (finish && !state.closed && !sliceOut.hasBufferedData()) {
                state.internalOutputStream?.close()
                state.closed = true
                // Drain any data that was written during close/flush
                sliceOut.drainBuffer()
            }

            // Signal if more data remains to be copied
            output.insufficient = sliceOut.hasBufferedData()
        } catch (e: Throwable) {
            if (!state.closed) {
                state.closed = true
                state.internalOutputStream?.close()
            }
            throw e
        }
    }
}

/**
 * An OutputStream that writes directly to a ByteArraySlice when possible,
 * and buffers overflow data when the slice is full.
 */
private class SliceOutputStream : OutputStream() {
    private var output: ByteArraySlice? = null
    private var overflowBuffer: ByteArray? = null
    private var overflowStart = 0
    private var overflowEnd = 0

    fun setOutput(slice: ByteArraySlice) {
        output = slice
    }

    fun hasBufferedData(): Boolean = overflowStart < overflowEnd

    fun drainBuffer() {
        val out = output ?: return
        if (!hasBufferedData()) return

        val buf = overflowBuffer ?: return
        val available = overflowEnd - overflowStart
        val toCopy = min(available, out.remainingWrite)
        if (toCopy > 0) {
            buf.copyInto(out.data, out.writeStart, overflowStart, overflowStart + toCopy)
            out.writeStart += toCopy
            overflowStart += toCopy
        }

        // Reset buffer if fully drained
        if (overflowStart >= overflowEnd) {
            overflowStart = 0
            overflowEnd = 0
        }
    }

    override fun write(b: Int) {
        write(byteArrayOf(b.toByte()), 0, 1)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        val out = output ?: error("Output slice not set")

        var offset = off
        var remaining = len

        // If we have buffered data, append to buffer
        if (hasBufferedData()) {
            appendToBuffer(b, offset, remaining)
            return
        }

        // Write directly to output slice as much as possible
        val directWrite = min(remaining, out.remainingWrite)
        if (directWrite > 0) {
            b.copyInto(out.data, out.writeStart, offset, offset + directWrite)
            out.writeStart += directWrite
            offset += directWrite
            remaining -= directWrite
        }

        // Buffer any overflow
        if (remaining > 0) {
            appendToBuffer(b, offset, remaining)
        }
    }

    private fun appendToBuffer(b: ByteArray, off: Int, len: Int) {
        val original = overflowBuffer ?: ByteArray(kotlin.math.max(8192, len)).also {
            overflowBuffer = it
        }
        val currentSize = overflowEnd - overflowStart

        // Check if we need to compact or grow the buffer
        if (overflowEnd + len > original.size) {
            if (overflowStart > 0) {
                // Compact: move data to beginning
                original.copyInto(original, 0, overflowStart, overflowEnd)
                overflowEnd = currentSize
                overflowStart = 0
            }
            if (overflowEnd + len > original.size) {
                // Grow buffer
                val newSize = kotlin.math.max(original.size * 2, currentSize + len)
                val newBuf = ByteArray(newSize)
                original.copyInto(newBuf, 0, overflowStart, overflowEnd)
                overflowBuffer = newBuf
                overflowEnd = currentSize
                overflowStart = 0
            }
        }

        b.copyInto(overflowBuffer ?: error("Overflow buffer not initialized"), overflowEnd, off, off + len)
        overflowEnd += len
    }
}
