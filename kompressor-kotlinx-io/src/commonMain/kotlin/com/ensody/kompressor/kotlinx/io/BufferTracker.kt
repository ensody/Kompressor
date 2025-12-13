package com.ensody.kompressor.kotlinx.io

import kotlinx.io.Buffer

public class BufferTracker(
    public var buffer: Buffer,
    public var processed: Int = 0,
    public var insufficient: Boolean = false,
)
