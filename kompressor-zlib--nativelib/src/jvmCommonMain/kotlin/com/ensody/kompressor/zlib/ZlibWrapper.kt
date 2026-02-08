package com.ensody.kompressor.zlib

import com.ensody.kompressor.core.ByteArraySlice
import com.ensody.nativebuilds.loader.NativeBuildsJvmLib
import com.ensody.nativebuilds.loader.NativeBuildsJvmLoader
import com.ensody.nativebuilds.zlib.NativeBuildsJvmLibZ

internal object ZlibWrapper : NativeBuildsJvmLib {
    override val packageName: String = "zlib"
    override val libName: String = "z-jni"
    override val platformFileName: Map<String, String> = mapOf(
        "linuxArm64" to "libz-jni.so",
        "linuxX64" to "libz-jni.so",
        "macosArm64" to "libz-jni.dylib",
        "macosX64" to "libz-jni.dylib",
        "mingwX64" to "libz-jni.dll",
    )

    init {
        NativeBuildsJvmLoader.load(NativeBuildsJvmLibZ)
        NativeBuildsJvmLoader.load(this)
    }

    external fun createCompressor(level: Int, windowBits: Int, memLevel: Int, strategy: Int): Long
    external fun freeCompressor(stream: Long): Long

    external fun createDecompressor(windowBits: Int): Long
    external fun freeDecompressor(stream: Long): Long

    external fun compressStream(
        stream: Long,
        input: ByteArraySlice,
        inputByteArray: ByteArray,
        inputStart: Int,
        inputEndExclusive: Int,
        output: ByteArraySlice,
        outputByteArray: ByteArray,
        outputStart: Int,
        outputEndExclusive: Int,
        finish: Boolean,
    ): Int

    external fun decompressStream(
        stream: Long,
        input: ByteArraySlice,
        inputByteArray: ByteArray,
        inputStart: Int,
        inputEndExclusive: Int,
        output: ByteArraySlice,
        outputByteArray: ByteArray,
        outputStart: Int,
        outputEndExclusive: Int,
        finish: Boolean,
    ): Int
}
