package com.ensody.kompressor.js

import kotlinx.coroutines.await
import org.khronos.webgl.Uint8Array
import kotlin.js.Promise

@Suppress("unused", "UnusedVariable")
internal fun ByteArray.toJsUint8Array(offset: Int = 0, length: Int = this.size): Uint8Array {
    val self = this
    return js("new Uint8Array(self.buffer, self.byteOffset + offset, length)")
}

@Suppress("unused", "UnusedVariable")
internal fun Uint8Array.toByteArray(): ByteArray {
    val self = this
    return js("new Int8Array(self.buffer, self.byteOffset, self.length)")
}

internal class JsCompressionInteropImpl(
    format: String,
    isCompression: Boolean,
) : JsCompressionInterop {
    private val writer: WritableStreamDefaultWriter
    private val reader: ReadableStreamDefaultReader

    init {
        val stream: dynamic = if (isCompression) JsCompressionStream(format) else JsDecompressionStream(format)
        writer = stream.writable.getWriter() as WritableStreamDefaultWriter
        reader = stream.readable.getReader() as ReadableStreamDefaultReader
    }

    override val desiredSize: Int get() = writer.asDynamic().desiredSize as Int

    override suspend fun write(chunk: ByteArray, offset: Int, length: Int) {
        (writer.asDynamic().write(chunk.toJsUint8Array(offset, length)) as Promise<Unit>).await()
    }

    override suspend fun close() {
        (writer.asDynamic().close() as Promise<Unit>).await()
    }

    override fun read(): Promise<ReadResult> {
        return (reader.asDynamic().read() as Promise<ReadResult>)
    }

    @Suppress("UnusedVariable", "unused")
    override suspend fun getIfResolved(read: Promise<ReadResult>): ReadResult? {
        val timeoutPromise = js(
            """
            new Promise(function(resolve) {
                queueMicrotask(function() { resolve(null); });
            })
        """,
        ) as Promise<ReadResult?>
        return (js("Promise.race([read, timeoutPromise])") as Promise<ReadResult?>).await()
    }

    override suspend fun cancel() {
        (reader.asDynamic().cancel() as Promise<Unit>).await()
    }

    override suspend fun abort() {
        (writer.asDynamic().abort() as Promise<Unit>).await()
    }
}

@JsName("CompressionStream")
internal external class JsCompressionStream(format: String)

@JsName("DecompressionStream")
internal external class JsDecompressionStream(format: String)

internal external class ReadableStreamDefaultReader
internal external class WritableStreamDefaultWriter

internal actual fun createJsCompressionInterop(format: String, isCompression: Boolean): JsCompressionInterop =
    JsCompressionInteropImpl(format, isCompression)

@Suppress("UnusedVariable", "unused")
internal actual fun ReadResult.bytesOrNull(): ByteArray? {
    val self = this
    return if (done) null else (js("self.value") as Uint8Array?)?.toByteArray()
}

@OptIn(markerClass = [ExperimentalWasmJsInterop::class])
internal actual suspend fun <T> Promise<T>.await(): T {
    return this.await()
}
