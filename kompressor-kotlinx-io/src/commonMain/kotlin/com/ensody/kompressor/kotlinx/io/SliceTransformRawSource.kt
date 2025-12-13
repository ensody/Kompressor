package com.ensody.kompressor.kotlinx.io

import com.ensody.kompressor.core.SliceTransform
import kotlinx.io.Buffer
import kotlinx.io.DelicateIoApi
import kotlinx.io.InternalIoApi
import kotlinx.io.RawSource
import kotlinx.io.Source
import kotlinx.io.UnsafeIoApi
import kotlinx.io.buffered

public fun RawSource.pipe(transform: SliceTransform): RawSource =
    SliceTransformRawSource(this, transform)

private class SliceTransformRawSource(
    private val rawSource: RawSource,
    private val transform: SliceTransform,
) : RawSource {
    private var closed = false
    private val source = rawSource as? Source ?: rawSource.buffered()
    private val helper = BufferSliceTransformHelper(transform)

    // exhaustedTransform == true implies exhaustedSource == true. Yes, two bools for only three possible states because
    // the code is easier to read than with an enum or int and more verbose comparisons.
    private var exhaustedSource = false
    private var exhaustedTransform = false

    private val localInput = Buffer()
    private val blockSize = 8192

    @OptIn(UnsafeIoApi::class, InternalIoApi::class, DelicateIoApi::class)
    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        check(!closed) { "Source is closed" }
        check(byteCount > 0L) { "At least 1 byte must be read" }

        val tracker = BufferTracker(sink)
        while (!exhaustedTransform && tracker.processed < byteCount) {
            val remaining = blockSize - localInput.size
            if (!exhaustedSource && remaining > 1024 && source.readAtMostTo(localInput, remaining) < 0) {
                exhaustedSource = true
            }
            if (!exhaustedTransform) {
                helper.transform(
                    source = localInput,
                    sink = tracker,
                    readLimit = (byteCount - tracker.processed).toInt(),
                    finish = exhaustedSource
                )
                if (exhaustedSource && localInput.exhausted() && !tracker.insufficient) {
                    exhaustedTransform = true
                }
            }
        }
        return if (tracker.processed == 0 && exhaustedTransform) -1 else tracker.processed.toLong()
    }

    override fun close() {
        source.close()
        closed = true
    }
}
