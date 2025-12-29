package com.ensody.kompressor.brotli

import com.ensody.kompressor.core.AsyncSliceTransform

/** Creates a Brotli compression transformation. */
public expect fun AsyncBrotliCompressor(
    compressionLevel: Int = 11,
    mode: BrotliCompressorMode = BrotliCompressorMode.GENERIC,
    lgwin: Int = 22,
): AsyncSliceTransform

/** Creates a Brotli decompression transformation. */
public expect fun AsyncBrotliDecompressor(): AsyncSliceTransform
