package com.ensody.kompressor.zstd

import com.ensody.kompressor.core.AsyncSliceTransform

/** Creates a zstd compression transformation. */
public expect fun AsyncZstdCompressor(compressionLevel: Int = 3): AsyncSliceTransform

/** Creates a zstd decompression transformation. */
public expect fun AsyncZstdDecompressor(): AsyncSliceTransform
