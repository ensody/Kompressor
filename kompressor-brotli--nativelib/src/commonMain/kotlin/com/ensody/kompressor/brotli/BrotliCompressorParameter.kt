package com.ensody.kompressor.brotli

/** Options to be used with ::BrotliEncoderSetParameter. */
internal enum class BrotliCompressorParameter(internal val value: Int) {
    /**
     * Tune encoder for specific input.
     *
     * ::BrotliEncoderMode enumerates all available values.
     */
    MODE(0),

    /**
     * The main compression speed-density lever.
     *
     * The higher the quality, the slower the compression. Range is
     * from ::BROTLI_MIN_QUALITY to ::BROTLI_MAX_QUALITY.
     */
    QUALITY(1),

    /**
     * Recommended sliding LZ77 window size.
     *
     * Encoder may reduce this value, e.g. if input is much smaller than
     * window size.
     *
     * Window size is `(1 << value) - 16`.
     *
     * Range is from ::BROTLI_MIN_WINDOW_BITS to ::BROTLI_MAX_WINDOW_BITS.
     */
    LGWIN(2),

    /**
     * Recommended input block size.
     *
     * Encoder may reduce this value, e.g. if input is much smaller than input
     * block size.
     *
     * Range is from ::BROTLI_MIN_INPUT_BLOCK_BITS to
     * ::BROTLI_MAX_INPUT_BLOCK_BITS.
     *
     * @note Bigger input block size allows better compression, but consumes more
     *       memory. \n The rough formula of memory used for temporary input
     *       storage is `3 << lgBlock`.
     */
    LGBLOCK(3),

    /**
     * Flag that affects usage of "literal context modeling" format feature.
     *
     * This flag is a "decoding-speed vs compression ratio" trade-off.
     */
    DISABLE_LITERAL_CONTEXT_MODELING(4),

    /**
     * Estimated total input size for all ::BrotliEncoderCompressStream calls.
     *
     * The default value is 0, which means that the total input size is unknown.
     */
    SIZE_HINT(5),

    /**
     * Flag that determines if "Large Window Brotli" is used.
     */
    LARGE_WINDOW(6),

    /**
     * Recommended number of postfix bits (NPOSTFIX).
     *
     * Encoder may change this value.
     *
     * Range is from 0 to ::BROTLI_MAX_NPOSTFIX.
     */
    NPOSTFIX(7),

    /**
     * Recommended number of direct distance codes (NDIRECT).
     *
     * Encoder may change this value.
     *
     * Range is from 0 to (15 << NPOSTFIX) in steps of (1 << NPOSTFIX).
     */
    NDIRECT(8),

    /**
     * Number of bytes of input stream already processed by a different instance.
     *
     * @note It is important to configure all the encoder instances with same
     *       parameters (except this one) in order to allow all the encoded parts
     *       obey the same restrictions implied by header.
     *
     * If offset is not 0, then stream header is omitted.
     * In any case output start is byte aligned, so for proper streams stitching
     * "predecessor" stream must be flushed.
     *
     * Range is not artificially limited, but all the values greater or equal to
     * maximal window size have the same effect. Values greater than 2**30 are not
     * allowed.
     */
    STREAM_OFFSET(9),
}
