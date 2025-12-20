package com.ensody.kompressor.zstd

import com.ensody.kompressor.core.ByteArraySlice
import com.ensody.nativebuilds.loader.NativeBuildsJvmLib
import com.ensody.nativebuilds.loader.NativeBuildsJvmLoader
import com.ensody.nativebuilds.zstd.NativeBuildsJvmLibZstd

internal object ZstdWrapper : NativeBuildsJvmLib {
    override val packageName: String = "zstd"
    override val libName: String = "zstd-jni"
    override val platformFileName: Map<String, String> = mapOf(
        "linuxArm64" to "libzstd-jni.so",
        "linuxX64" to "libzstd-jni.so",
        "macosArm64" to "libzstd-jni.dylib",
        "macosX64" to "libzstd-jni.dylib",
        "mingwX64" to "libzstd-jni.dll",
    )

    init {
        NativeBuildsJvmLoader.load(NativeBuildsJvmLibZstd)
        NativeBuildsJvmLoader.load(this)
    }

    external fun createCompressor(): Long
    external fun freeCompressor(cctx: Long)

    external fun createDecompressor(): Long
    external fun freeDecompressor(dctx: Long)

    external fun setParameter(cctx: Long, parameter: Int, value: Int): Long

    external fun compressStream(
        cctx: Long,
        input: ByteArraySlice,
        inputByteArray: ByteArray,
        inputStart: Int,
        inputEndExclusive: Int,
        output: ByteArraySlice,
        outputByteArray: ByteArray,
        outputStart: Int,
        outputEndExclusive: Int,
        finish: Boolean,
    ): Long

    external fun decompressStream(
        dctx: Long,
        input: ByteArraySlice,
        inputByteArray: ByteArray,
        inputStart: Int,
        inputEndExclusive: Int,
        output: ByteArraySlice,
        outputByteArray: ByteArray,
        outputStart: Int,
        outputEndExclusive: Int,
    ): Long

    external fun getErrorName(code: Long): String?
}
