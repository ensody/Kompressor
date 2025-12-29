package com.ensody.kompressor.brotli

/** Options to be used with ::BrotliDecoderSetParameter. */
internal enum class BrotliDecompressorParameter(internal val value: Int) {
    /**
     * Disable "canny" ring buffer allocation strategy.
     *
     * Ring buffer is allocated according to window size, despite the real size of
     * the content.
     */
    DISABLE_RING_BUFFER_REALLOCATION(0),

    /**
     * Flag that determines if "Large Window Brotli" is used.
     */
    LARGE_WINDOW(1),
}
