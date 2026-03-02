package com.ensody.kompressor.zstd

import com.ensody.kompressor.core.AsyncSliceTransform
import com.ensody.kompressor.core.SliceTransform
import com.ensody.kompressor.core.toAsync

/** Creates a zstd compression transformation. */
public expect fun ZstdCompressor(compressionLevel: Int = 3, dictionary: ByteArray? = null): SliceTransform

/** Creates a zstd decompression transformation. */
public expect fun ZstdDecompressor(dictionary: ByteArray? = null): SliceTransform

public actual fun AsyncZstdCompressor(compressionLevel: Int, dictionary: ByteArray?): AsyncSliceTransform =
    ZstdCompressor(compressionLevel = compressionLevel, dictionary = dictionary).toAsync()

public actual fun AsyncZstdDecompressor(dictionary: ByteArray?): AsyncSliceTransform =
    ZstdDecompressor(dictionary = dictionary).toAsync()
