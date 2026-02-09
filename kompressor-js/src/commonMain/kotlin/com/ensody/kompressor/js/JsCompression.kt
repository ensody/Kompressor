package com.ensody.kompressor.js

import com.ensody.kompressor.core.AsyncSliceTransform
import com.ensody.kompressor.core.ExperimentalCompressionFormat

/** Creates a [AsyncSliceTransform] using the JS Compression Streams API. */
public fun JsCompressor(format: JsCompressionFormat): AsyncSliceTransform =
    JsCompressionSliceTransform(format.name, isCompression = true)

/** Creates a [AsyncSliceTransform] using the JS Compression Streams API. */
public fun JsDecompressor(format: JsCompressionFormat): AsyncSliceTransform =
    JsCompressionSliceTransform(format.name, isCompression = false)

public sealed class JsCompressionFormat(public open val name: String) {
    /**
     * Not available on many browsers, see https://developer.mozilla.org/en-US/docs/Web/API/CompressionStream#browser_compatibility.
     */
    @ExperimentalCompressionFormat
    public data object Brotli : JsCompressionFormat("brotli")
    public data object Gzip : JsCompressionFormat("gzip")
    public data object Deflate : JsCompressionFormat("deflate")
    public data object DeflateRaw : JsCompressionFormat("deflate-raw")

    /**
     * Not available on many browsers, see https://developer.mozilla.org/en-US/docs/Web/API/CompressionStream#browser_compatibility.
     */
    @ExperimentalCompressionFormat
    public data object Zstd : JsCompressionFormat("zstd")

    @ExperimentalCompressionFormat
    public data class Custom(override val name: String) : JsCompressionFormat(name)
}
