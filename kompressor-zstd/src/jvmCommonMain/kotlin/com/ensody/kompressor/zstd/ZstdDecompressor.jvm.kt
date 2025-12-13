package com.ensody.kompressor.zstd

import com.ensody.kompressor.core.ByteArraySlice
import com.ensody.kompressor.core.SliceTransform
import com.ensody.kompressor.core.createCleaner

public actual fun ZstdDecompressor(): SliceTransform =
    ZstdDecompressorImpl()

internal class ZstdDecompressorImpl : SliceTransform {
    private val dctx: Long = ZstdWrapper.createDecompressor().also {
        check(it != 0L) { "Failed allocating zstd dctx" }
    }

    val cleaner = createCleaner(dctx, ZstdWrapper::freeDecompressor)

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
