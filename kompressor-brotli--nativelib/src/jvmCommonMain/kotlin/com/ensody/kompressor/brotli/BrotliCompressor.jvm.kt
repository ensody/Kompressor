package com.ensody.kompressor.brotli

import com.ensody.kompressor.core.ByteArraySlice
import com.ensody.kompressor.core.SliceTransform
import com.ensody.kompressor.core.createCleaner

public actual fun BrotliCompressor(
    compressionLevel: Int,
    mode: BrotliCompressorMode,
    lgwin: Int,
): SliceTransform =
    BrotliCompressorImpl(
        mode = mode,
        compressionLevel = compressionLevel,
        lgwin = lgwin,
    )

internal class BrotliCompressorImpl(
    private val compressionLevel: Int,
    private val mode: BrotliCompressorMode,
    private val lgwin: Int,
) : SliceTransform {
    private val stream: Long = BrotliWrapper.createCompressor().also {
        check(it != 0L) { "Failed allocating Brotli encoder" }
    }

    val cleaner = createCleaner(stream, BrotliWrapper::freeCompressor)

    init {
        BrotliWrapper.setCompressorParameter(stream, BrotliCompressorParameter.MODE.value, mode.value)
        BrotliWrapper.setCompressorParameter(stream, BrotliCompressorParameter.QUALITY.value, compressionLevel)
        BrotliWrapper.setCompressorParameter(stream, BrotliCompressorParameter.LGWIN.value, lgwin)
    }

    override fun transform(input: ByteArraySlice, output: ByteArraySlice, finish: Boolean) {
        val result = BrotliWrapper.compressStream(
            stream = stream,
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
        if (result < 0) {
            error("Bad Brotli result code: $result")
        }
        output.insufficient = input.hasData || (finish && result != 1)
    }
}
