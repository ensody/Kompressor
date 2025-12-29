package com.ensody.kompressor.brotli

import com.ensody.kompressor.core.ByteArraySlice
import com.ensody.kompressor.core.SliceTransform
import com.ensody.kompressor.core.createCleaner

public actual fun BrotliDecompressor(): SliceTransform =
    BrotliDecompressorImpl()

internal class BrotliDecompressorImpl : SliceTransform {
    private val stream: Long = BrotliWrapper.createDecompressor().also {
        check(it != 0L) { "Failed allocating Brotli decoder" }
    }

    val cleaner = createCleaner(stream, BrotliWrapper::freeDecompressor)

    override fun transform(input: ByteArraySlice, output: ByteArraySlice, finish: Boolean) {
        val result = BrotliWrapper.decompressStream(
            stream = stream,
            input = input,
            inputByteArray = input.data,
            inputStart = input.readStart,
            inputEndExclusive = input.writeStart,
            output = output,
            outputByteArray = output.data,
            outputStart = output.writeStart,
            outputEndExclusive = output.writeLimit,
        )
        if (result == BrotliDecoderResult.BROTLI_DECODER_RESULT_ERROR.value) {
            error("Bad Brotli result code: $result")
        }
        input.insufficient = result == BrotliDecoderResult.BROTLI_DECODER_RESULT_NEEDS_MORE_INPUT.value
        output.insufficient =
            input.hasData || (finish && result != BrotliDecoderResult.BROTLI_DECODER_RESULT_SUCCESS.value)
    }
}
