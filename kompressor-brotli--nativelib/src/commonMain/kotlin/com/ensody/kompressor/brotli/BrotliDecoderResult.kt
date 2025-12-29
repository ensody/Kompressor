package com.ensody.kompressor.brotli

/** Options to be used with ::BrotliDecoderSetParameter. */
internal enum class BrotliDecoderResult(internal val value: Int) {
    /** Decoding error, e.g. corrupted input or memory allocation problem. */
    BROTLI_DECODER_RESULT_ERROR(0),

    /** Decoding successfully completed. */
    BROTLI_DECODER_RESULT_SUCCESS(1),

    /** Partially done; should be called again with more input. */
    BROTLI_DECODER_RESULT_NEEDS_MORE_INPUT(2),

    /** Partially done; should be called again with more output. */
    BROTLI_DECODER_RESULT_NEEDS_MORE_OUTPUT(3),
}
