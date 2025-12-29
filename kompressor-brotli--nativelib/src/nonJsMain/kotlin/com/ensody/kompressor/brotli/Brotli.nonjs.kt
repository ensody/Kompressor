package com.ensody.kompressor.brotli

import com.ensody.kompressor.core.AsyncSliceTransform
import com.ensody.kompressor.core.SliceTransform
import com.ensody.kompressor.core.toAsync

/** Creates a zlib compression transformation. */
public expect fun BrotliCompressor(
    compressionLevel: Int = 11,
    mode: BrotliCompressorMode = BrotliCompressorMode.GENERIC,
    lgwin: Int = 22,
): SliceTransform

/** Creates a zlib decompression transformation. */
public expect fun BrotliDecompressor(): SliceTransform

public actual fun AsyncBrotliCompressor(
    compressionLevel: Int,
    mode: BrotliCompressorMode,
    lgwin: Int,
): AsyncSliceTransform =
    BrotliCompressor(
        mode = mode,
        compressionLevel = compressionLevel,
        lgwin = lgwin,
    ).toAsync()

public actual fun AsyncBrotliDecompressor(): AsyncSliceTransform =
    BrotliDecompressor().toAsync()
