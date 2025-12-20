package com.ensody.kompressor.zstd

import com.ensody.kompressor.core.ByteArraySlice
import com.ensody.kompressor.core.SliceTransform
import com.ensody.kompressor.internal.zstd.ZSTD_DCtx
import com.ensody.kompressor.internal.zstd.ZSTD_createDCtx
import com.ensody.kompressor.internal.zstd.ZSTD_decompressStream
import com.ensody.kompressor.internal.zstd.ZSTD_freeDCtx
import com.ensody.kompressor.internal.zstd.ZSTD_getErrorName
import com.ensody.kompressor.internal.zstd.ZSTD_inBuffer
import com.ensody.kompressor.internal.zstd.ZSTD_isError
import com.ensody.kompressor.internal.zstd.ZSTD_outBuffer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import kotlin.native.ref.createCleaner

public actual fun ZstdDecompressor(): SliceTransform =
    ZstdDecompressorImpl()

@OptIn(UnsafeNumber::class)
internal class ZstdDecompressorImpl : SliceTransform {
    private val dctx: CPointer<ZSTD_DCtx> = checkNotNull(ZSTD_createDCtx()) {
        "Failed allocating zstd dctx"
    }

    val cleaner = createCleaner(dctx) {
        ZSTD_freeDCtx(it)
    }

    override fun transform(input: ByteArraySlice, output: ByteArraySlice, finish: Boolean) = memScoped {
        input.data.usePinned { pinnedInput ->
            output.data.usePinned { pinnedOutput ->
                val inBuffer = alloc<ZSTD_inBuffer>().also {
                    it.src = pinnedInput.addressOf(0)
                    it.pos = input.readStart.convert()
                    it.size = input.writeStart.convert()
                }
                val outBuffer = alloc<ZSTD_outBuffer>().also {
                    it.dst = pinnedOutput.addressOf(0)
                    it.pos = output.writeStart.convert()
                    it.size = output.writeLimit.convert()
                }
                val result = ZSTD_decompressStream(
                    zds = dctx,
                    input = inBuffer.ptr,
                    output = outBuffer.ptr,
                )
                input.readStart = inBuffer.pos.convert()
                output.writeStart = outBuffer.pos.convert()
                if (result.convert<ULong>() != 0UL && ZSTD_isError(result) != 0U) {
                    error("Bad zstd result code $result: ${ZSTD_getErrorName(result)?.toKString()}")
                }
                output.insufficient = output.isFull && result.convert<ULong>() != 0UL
            }
        }
    }
}
