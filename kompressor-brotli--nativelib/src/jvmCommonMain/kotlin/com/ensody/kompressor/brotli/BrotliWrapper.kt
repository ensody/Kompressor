package com.ensody.kompressor.brotli

import com.ensody.kompressor.core.ByteArraySlice
import com.ensody.nativebuilds.brotli.NativeBuildsJvmLibBrotlicommon
import com.ensody.nativebuilds.brotli.NativeBuildsJvmLibBrotlidec
import com.ensody.nativebuilds.brotli.NativeBuildsJvmLibBrotlienc
import com.ensody.nativebuilds.loader.NativeBuildsJvmLib
import com.ensody.nativebuilds.loader.NativeBuildsJvmLoader

internal object BrotliWrapper : NativeBuildsJvmLib {
    override val packageName: String = "brotli"
    override val libName: String = "brotli-jni"
    override val platformFileName: Map<String, String> = mapOf(
        "linuxArm64" to "libbrotli-jni.so",
        "linuxX64" to "libbrotli-jni.so",
        "macosArm64" to "libbrotli-jni.dylib",
        "macosX64" to "libbrotli-jni.dylib",
        "mingwX64" to "libbrotli-jni.dll",
    )

    init {
        NativeBuildsJvmLoader.load(NativeBuildsJvmLibBrotlicommon)
        NativeBuildsJvmLoader.load(NativeBuildsJvmLibBrotlidec)
        NativeBuildsJvmLoader.load(NativeBuildsJvmLibBrotlienc)
        NativeBuildsJvmLoader.load(this)
    }

    external fun createCompressor(): Long
    external fun freeCompressor(stream: Long)
    external fun setCompressorParameter(stream: Long, param: Int, value: Int): Long

    external fun createDecompressor(): Long
    external fun freeDecompressor(stream: Long)
    external fun setDecompressorParameter(stream: Long, param: Int, value: Int): Long

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
    ): Int
}
