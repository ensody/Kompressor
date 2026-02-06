package com.ensody.kompressor.ktor

import com.ensody.kompressor.core.AsyncSliceTransform
import com.ensody.kompressor.kotlinx.io.transform
import io.ktor.util.ContentEncoder
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.asSink
import io.ktor.utils.io.asSource
import io.ktor.utils.io.readRemaining
import io.ktor.utils.io.reader
import io.ktor.utils.io.writer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.io.Buffer
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import kotlin.coroutines.CoroutineContext

public abstract class BaseAsyncSliceTransformContentEncoder(
    override val name: String,
    private val dispatcher: CoroutineContext = Dispatchers.Default,
) : ContentEncoder {
    public abstract suspend fun compressor(): AsyncSliceTransform
    public abstract suspend fun decompressor(): AsyncSliceTransform

    override fun decode(
        source: ByteReadChannel,
        coroutineContext: CoroutineContext,
    ): ByteReadChannel = CoroutineScope(coroutineContext).writer(dispatcher) {
        val compressor = decompressor()
        val input = Buffer()
        val sink = channel.asSink() as Sink
        try {
            while (!source.isClosedForRead) {
                val packet = source.readRemaining(8192)
                if (packet.exhausted()) break
                input.write(packet.readByteArray())
                compressor.transform(input, sink)
            }
        } finally {
            sink.close()
        }
    }.channel

    override fun encode(
        source: ByteReadChannel,
        coroutineContext: CoroutineContext,
    ): ByteReadChannel = CoroutineScope(coroutineContext).writer(dispatcher) {
        val compressor = compressor()
        val input = Buffer()
        val sink = channel.asSink() as Sink
        try {
            while (!source.isClosedForRead) {
                val packet = source.readRemaining(8192)
                if (packet.exhausted()) break
                input.write(packet.readByteArray())
                compressor.transform(input, sink)
            }
        } finally {
            sink.close()
        }
    }.channel

    override fun encode(
        source: ByteWriteChannel,
        coroutineContext: CoroutineContext,
    ): ByteWriteChannel = CoroutineScope(coroutineContext).reader(dispatcher) {
        val compressor = compressor()
        val input = (channel.asSource() as Source).buffered()
        val sink = source.asSink() as Sink
        val buffer = Buffer()
        try {
            while (true) {
                if (input.readAtMostTo(buffer, 8192) == -1L) break
                compressor.transform(buffer, sink)
            }
        } finally {
            sink.close()
        }
    }.channel
}
