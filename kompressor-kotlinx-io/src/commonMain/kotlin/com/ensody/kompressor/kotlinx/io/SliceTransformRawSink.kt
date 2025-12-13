package com.ensody.kompressor.kotlinx.io

import com.ensody.kompressor.core.SliceTransform
import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.Sink
import kotlinx.io.UnsafeIoApi
import kotlinx.io.buffered

public fun SliceTransform.pipe(sink: RawSink): RawSink =
    SliceTransformRawSink(this, sink)

private class SliceTransformRawSink(
    private val transform: SliceTransform,
    private val rawSink: RawSink,
) : RawSink {
    private var closed = false
    private val sink = rawSink as? Sink ?: rawSink.buffered()
    private val helper = BufferSliceTransformHelper(transform)

    @OptIn(UnsafeIoApi::class)
    override fun write(source: Buffer, byteCount: Long) {
        check(!closed) { "Sink is closed" }
        if (byteCount <= 0L) {
            return
        }

        var bytesRead = 0L
        while (bytesRead < byteCount) {
            bytesRead += helper.transform(source, sink, (byteCount - bytesRead).toInt(), finish = false)
        }
    }

    override fun flush() {
        check(!closed) { "Sink is closed" }
        sink.flush()
    }

    override fun close() {
        if (closed) return

        helper.finishInto(sink)
        closed = true
        sink.close()
    }
}
