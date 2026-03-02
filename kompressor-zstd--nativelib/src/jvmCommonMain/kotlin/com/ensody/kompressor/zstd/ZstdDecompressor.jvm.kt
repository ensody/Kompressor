package com.ensody.kompressor.zstd

import com.ensody.kompressor.core.ByteArraySlice
import com.ensody.kompressor.core.SliceTransform
import com.ensody.kompressor.core.createCleaner

public actual fun ZstdDecompressor(dictionary: ByteArray?): SliceTransform =
    ZstdDecompressorImpl(dictionary = dictionary)

internal class ZstdDecompressorImpl(
    private val dictionary: ByteArray? = null,
) : SliceTransform {
    private val dctx: Long = ZstdWrapper.createDecompressor().also {
        check(it != 0L) { "Failed allocating zstd dctx" }
    }

    val cleaner = createCleaner(dctx, ZstdWrapper::freeDecompressor)

    init {
        dictionary?.let {
            checkErrorResult(ZstdWrapper.loadDecompressorDictionary(dctx, it))
        }
    }

    override fun transform(input: ByteArraySlice, output: ByteArraySlice, finish: Boolean) {
        val result = ZstdWrapper.decompressStream(
            dctx = dctx,
            input = input,
            inputByteArray = input.data,
            inputStart = input.readStart,
            inputEndExclusive = input.writeStart,
            output = output,
            outputByteArray = output.data,
            outputStart = output.writeStart,
            outputEndExclusive = output.writeLimit,
        )
        checkErrorResult(result)
        output.insufficient = output.isFull && result != 0L
    }
}
