package com.ensody.kompressor.brotli.ktor

import com.ensody.kompressor.brotli.BrotliCompressor
import com.ensody.kompressor.brotli.BrotliDecompressor
import com.ensody.kompressor.core.SliceTransform
import com.ensody.kompressor.ktor.BaseSliceTransformContentEncoder
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

public class BrotliContentEncoder(
    public val compressionLevel: Int = 11,
    dispatcher: CoroutineContext = Dispatchers.Default,
) : BaseSliceTransformContentEncoder(dispatcher) {
    override val name: String = "br"

    override suspend fun compressor(): SliceTransform =
        BrotliCompressor(compressionLevel = compressionLevel)

    override suspend fun decompressor(): SliceTransform =
        BrotliDecompressor()
}
