package com.ensody.kompressor.zlib

import com.ensody.kompressor.core.AsyncSliceTransform

/** Creates a zlib compression transformation. */
public expect fun AsyncZlibCompressor(
    format: ZlibFormat,
    compressionLevel: Int = -1,
    windowBits: Int = 15,
    memLevel: Int = 8,
): AsyncSliceTransform

/** Creates a zlib decompression transformation. */
public expect fun AsyncZlibDecompressor(
    format: ZlibFormat = ZlibFormat.AutoDetectZlibGzip,
    windowBits: Int = 15,
): AsyncSliceTransform
