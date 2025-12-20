package com.ensody.kompressor.zstd

import com.ensody.kompressor.core.AsyncSliceTransform
import com.ensody.kompressor.core.SliceTransform
import com.ensody.kompressor.core.toAsync

/** Creates a zstd compression transformation. */
public expect fun ZstdCompressor(compressionLevel: Int = 3): SliceTransform

/** Creates a zstd decompression transformation. */
public expect fun ZstdDecompressor(): SliceTransform

public actual fun AsyncZstdCompressor(compressionLevel: Int): AsyncSliceTransform =
    ZstdCompressor(compressionLevel = compressionLevel).toAsync()

public actual fun AsyncZstdDecompressor(): AsyncSliceTransform =
    ZstdDecompressor().toAsync()
