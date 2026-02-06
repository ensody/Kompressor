@file:OptIn(ExperimentalWasmJsInterop::class)

package com.ensody.kompressor.js

import kotlinx.coroutines.await
import kotlin.js.Promise

internal class JsCompressionInteropImpl(
    format: String,
    isCompression: Boolean,
) : JsCompressionInterop {
    private val writer: WritableStreamDefaultWriter
    private val reader: ReadableStreamDefaultReader

    init {
        val stream: JsAny = if (isCompression) CompressionStream(format) else DecompressionStream(format)
        writer = getWriter(getWritable(stream))
        reader = getReader(getReadable(stream))
    }

    override val desiredSize: Int get() = getDesiredSize(writer)

    override suspend fun write(chunk: ByteArray, offset: Int, length: Int) {
        write(writer, chunk.toJsUint8Array(offset, length)).await<JsAny?>()
    }

    override suspend fun close() {
        close(writer).await<JsAny?>()
    }

    override suspend fun read(): ByteArray? {
        val result = read(reader).await<JsReadableStreamReadResult>()
        if (getDone(result)) return null
        val jsValue = getValue(result) ?: return null
        return jsValue.toByteArray()
    }

    override suspend fun cancel() {
        cancel(reader).await<JsAny?>()
    }

    override suspend fun awaitWriteReady() {
        getReady(writer).await<JsAny?>()
    }
}

internal fun ByteArray.toJsUint8Array(offset: Int = 0, length: Int = this.size): Uint8Array {
    val array = createUint8Array(length)
    for (i in 0 until length) {
        setUint8(array, i, this[offset + i])
    }
    return array
}

internal fun Uint8Array.toByteArray(): ByteArray {
    val array = ByteArray(getUint8ArrayLength(this))
    for (i in 0 until array.size) {
        array[i] = getUint8(this, i)
    }
    return array
}

@JsFun("(size) => new Uint8Array(size)")
private external fun createUint8Array(size: Int): Uint8Array

@JsFun("(array, index, value) => array[index] = value")
private external fun setUint8(array: Uint8Array, index: Int, value: Byte)

@JsFun("(array, index) => array[index]")
private external fun getUint8(array: Uint8Array, index: Int): Byte

@JsFun("(array) => array.length")
private external fun getUint8ArrayLength(array: Uint8Array): Int

@JsFun("(stream) => stream.getReader()")
private external fun getReader(stream: ReadableStream): ReadableStreamDefaultReader

@JsFun("(reader) => reader.read()")
private external fun read(reader: ReadableStreamDefaultReader): Promise<JsReadableStreamReadResult>

@JsFun("(stream) => stream.getWriter()")
private external fun getWriter(stream: WritableStream): WritableStreamDefaultWriter

@JsFun("(writer, chunk) => writer.write(chunk)")
private external fun write(writer: WritableStreamDefaultWriter, chunk: Uint8Array): Promise<JsAny?>

@JsFun("(writer) => writer.close()")
private external fun close(writer: WritableStreamDefaultWriter): Promise<JsAny?>

@JsFun("(writer) => writer.ready")
private external fun getReady(writer: WritableStreamDefaultWriter): Promise<JsAny?>

@JsFun("(writer) => writer.desiredSize")
private external fun getDesiredSize(writer: WritableStreamDefaultWriter): Int

@JsFun("(stream) => stream.readable")
private external fun getReadable(stream: JsAny): ReadableStream

@JsFun("(stream) => stream.writable")
private external fun getWritable(stream: JsAny): WritableStream

@JsFun("(result) => result.done")
private external fun getDone(result: JsReadableStreamReadResult): Boolean

@JsFun("(result) => result.value")
private external fun getValue(result: JsReadableStreamReadResult): Uint8Array?

@JsFun("(reader) => reader.cancel()")
private external fun cancel(reader: ReadableStreamDefaultReader): Promise<JsAny?>

internal external class Uint8Array : JsAny
internal external class ReadableStream : JsAny
internal external class ReadableStreamDefaultReader : JsAny
internal external class JsReadableStreamReadResult : JsAny
internal external class WritableStream : JsAny
internal external class WritableStreamDefaultWriter : JsAny
internal external class CompressionStream(format: String) : JsAny
internal external class DecompressionStream(format: String) : JsAny

internal actual fun createJsCompressionInterop(format: String, isCompression: Boolean): JsCompressionInterop =
    JsCompressionInteropImpl(format, isCompression)
