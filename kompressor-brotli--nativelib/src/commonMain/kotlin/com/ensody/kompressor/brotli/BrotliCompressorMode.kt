package com.ensody.kompressor.brotli

public enum class BrotliCompressorMode(internal val value: Int) {
    /**
     * Default compression mode.
     *
     * In this mode compressor does not know anything in advance about the
     * properties of the input.
     */
    GENERIC(0),

    /** Compression mode for UTF-8 formatted text input. */
    TEXT(1),

    /** Compression mode used in WOFF 2.0. */
    FONT(2),
}
