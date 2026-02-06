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
    private lateinit var streamFrom: ByteArraySlice
    private var finishing = false

    // some streams like to read on initialization, so we need to make sure streamFrom is set first
    private val inputStream by lazy {
        streamFactory(
            object : InputStream() {
                override fun read(): Int {
                    if (streamFrom.remainingRead == 0) {
                        if (finishing) return -1
                        throw NeedsMoreInputException()
                    }
                    return streamFrom.data[streamFrom.readStart++].toInt() and 0xFF
                }

                override fun read(b: ByteArray, off: Int, len: Int): Int {
                    if (streamFrom.remainingRead == 0) {
                        if (finishing) return -1
                        throw NeedsMoreInputException()
                    }
                    val toRead = min(min(len, streamFrom.remainingRead), b.size - off)
                    streamFrom.readInto(b, off, toRead)
                    return toRead
                }

                override fun skip(n: Long): Long {
                    val toSkip = min(n, streamFrom.remainingRead.toLong()).toInt()
                    streamFrom.readStart += toSkip
                    return toSkip.toLong()
                }

                override fun available(): Int {
                    return streamFrom.remainingRead
                }
            },
        )
    }

    override fun transform(
        input: ByteArraySlice,
        output: ByteArraySlice,
        finish: Boolean,
    ) {
        this.finishing = finish
        if (!input.hasData && !finish) {
            return
        }
        streamFrom = input


        val result = try {
            inputStream.read(output.data, output.writeStart, output.remainingWrite)
        } catch (_: NeedsMoreInputException) {
            output.insufficient = true
            return
        }
        if (result == -1) {
            // eof
            output.insufficient = false
        } else {
            output.writeStart += result
            // Stream may have more data even if result < remainingWrite
            output.insufficient = inputStream.available() > 0
        }
    }
}

private class OutputStreamTransform(val streamFactory: (OutputStream) -> OutputStream) : SliceTransform {
    private var sliceOutputStream: SliceOutputStream? = null
    private var internalOutputStream: OutputStream? = null
    private var closed = false

    override fun transform(
        input: ByteArraySlice,
        output: ByteArraySlice,
        finish: Boolean,
    ) {
        // Initialize stream on first call - must set output BEFORE creating wrapped stream
        // because some streams (like GZIPOutputStream) write header data during construction
        if (sliceOutputStream == null) {
            sliceOutputStream = SliceOutputStream()
            sliceOutputStream!!.setOutput(output)
            internalOutputStream = streamFactory(sliceOutputStream!!)
        }

        val sliceOut = sliceOutputStream!!

        // Set the current output slice for direct writing
        sliceOut.setOutput(output)

        // First, drain any buffered overflow data from previous calls
        sliceOut.drainBuffer()

        // Write any remaining input to the compressor (only if output has space and no buffered data)
        if (input.remainingRead > 0 && !closed && !sliceOut.hasBufferedData()) {
            val readRemaining = input.remainingRead
            val readStart = input.readStart
            input.readStart += readRemaining
            internalOutputStream!!.write(input.data, readStart, readRemaining)
        }

        // Close the stream when finishing (to flush all compressed data)
        if (finish && !closed && !sliceOut.hasBufferedData()) {
            internalOutputStream!!.close()
            closed = true
            // Drain any data that was written during close/flush
            sliceOut.drainBuffer()
        }

        // Signal if more data remains to be copied
        output.insufficient = sliceOut.hasBufferedData()
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
        if (overflowBuffer == null) {
            overflowBuffer = ByteArray(kotlin.math.max(8192, len))
        }

        val buf = overflowBuffer!!
        val currentSize = overflowEnd - overflowStart

        // Check if we need to compact or grow the buffer
        if (overflowEnd + len > buf.size) {
            if (overflowStart > 0) {
                // Compact: move data to beginning
                buf.copyInto(buf, 0, overflowStart, overflowEnd)
                overflowEnd = currentSize
                overflowStart = 0
            }
            if (overflowEnd + len > buf.size) {
                // Grow buffer
                val newSize = kotlin.math.max(buf.size * 2, currentSize + len)
                val newBuf = ByteArray(newSize)
                buf.copyInto(newBuf, 0, overflowStart, overflowEnd)
                overflowBuffer = newBuf
                overflowEnd = currentSize
                overflowStart = 0
            }
        }

        b.copyInto(overflowBuffer!!, overflowEnd, off, off + len)
        overflowEnd += len
    }
}
