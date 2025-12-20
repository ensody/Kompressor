package com.ensody.kompressor.zstd

import com.ensody.kompressor.core.ByteArraySlice
import com.ensody.kompressor.core.SliceTransform
import com.ensody.kompressor.core.createCleaner

public actual fun ZstdCompressor(compressionLevel: Int): SliceTransform =
    ZstdCompressorImpl(compressionLevel = compressionLevel)

internal class ZstdCompressorImpl(
    private val compressionLevel: Int = 3,
) : SliceTransform {
    private val cctx: Long = ZstdWrapper.createCompressor().also {
        check(it != 0L) { "Failed allocating zstd cctx" }
    }

    val cleaner = createCleaner(cctx, ZstdWrapper::freeCompressor)

    init {
        ZstdWrapper.setParameter(cctx, ZstdParameter.compressionLevel.toInt(), compressionLevel)
    }

    override fun transform(input: ByteArraySlice, output: ByteArraySlice, finish: Boolean) {
        val result = ZstdWrapper.compressStream(
            cctx = cctx,
            input = input,
            inputByteArray = input.data,
            inputStart = input.readStart,
            inputEndExclusive = input.writeStart,
            output = output,
            outputByteArray = output.data,
            outputStart = output.writeStart,
            outputEndExclusive = output.writeLimit,
            finish = finish,
        )
        checkErrorResult(result)
        output.insufficient = input.hasData || (finish && result != 0L)
    }
}

internal fun checkErrorResult(result: Long) {
    if (result != 0L) {
        ZstdWrapper.getErrorName(result)?.let {
            error("Bad zstd result code $result: $it")
        }
    }
}
