package com.ensody.kompressor.js

internal interface JsCompressionInterop {
    val desiredSize: Int
    suspend fun write(chunk: ByteArray, offset: Int, length: Int)
    suspend fun close()
    suspend fun read(): ByteArray?
    suspend fun cancel()
    suspend fun awaitWriteReady()
}

internal expect fun createJsCompressionInterop(format: String, isCompression: Boolean): JsCompressionInterop
