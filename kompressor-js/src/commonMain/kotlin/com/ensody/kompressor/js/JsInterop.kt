package com.ensody.kompressor.js

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise

internal interface JsCompressionInterop {
    val desiredSize: Int
    val hasRoom get() = desiredSize > 0
    suspend fun write(chunk: ByteArray, offset: Int, length: Int)
    suspend fun close()

    @OptIn(ExperimentalWasmJsInterop::class)
    fun read(): Promise<ReadResult>
    suspend fun cancel()
    suspend fun abort()

    @OptIn(ExperimentalWasmJsInterop::class)
    suspend fun getIfResolved(read: Promise<ReadResult>): ReadResult?
}

internal expect fun createJsCompressionInterop(format: String, isCompression: Boolean): JsCompressionInterop

@OptIn(ExperimentalWasmJsInterop::class)
internal external interface ReadResult : JsAny {
    val done: Boolean
}

internal expect fun ReadResult.bytesOrNull(): ByteArray?

@OptIn(ExperimentalWasmJsInterop::class)
internal expect suspend fun <T : JsAny?> Promise<T>.await(): T

@OptIn(ExperimentalWasmJsInterop::class)
internal sealed class ReadResultOrPromise {
    data class Result(val result: ReadResult) : ReadResultOrPromise()
    data class PromiseResult(val promise: Promise<ReadResult>) : ReadResultOrPromise()
}
