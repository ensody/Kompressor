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

    override suspend fun read(): ByteArray? {
        val result = (reader.asDynamic().read() as Promise<dynamic>).await()
        return if (result.done as Boolean) null else (result.value as Uint8Array).toByteArray()
    }

    override suspend fun cancel() {
        (reader.asDynamic().cancel() as Promise<Unit>).await()
    }

    override suspend fun awaitWriteReady() {
        (writer.asDynamic().ready as Promise<Unit>).await()
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
