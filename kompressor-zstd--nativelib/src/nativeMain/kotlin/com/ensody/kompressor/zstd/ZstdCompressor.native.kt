package com.ensody.kompressor.zstd

import com.ensody.kompressor.core.ByteArraySlice
import com.ensody.kompressor.core.SliceTransform
import com.ensody.kompressor.internal.zstd.ZSTD_CCtx
import com.ensody.kompressor.internal.zstd.ZSTD_CCtx_setParameter
import com.ensody.kompressor.internal.zstd.ZSTD_compressStream2
import com.ensody.kompressor.internal.zstd.ZSTD_createCCtx
import com.ensody.kompressor.internal.zstd.ZSTD_e_continue
import com.ensody.kompressor.internal.zstd.ZSTD_e_end
import com.ensody.kompressor.internal.zstd.ZSTD_freeCCtx
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

public actual fun ZstdCompressor(compressionLevel: Int): SliceTransform =
    ZstdCompressorImpl(compressionLevel = compressionLevel)

@OptIn(UnsafeNumber::class)
internal class ZstdCompressorImpl(
    private val compressionLevel: Int = 3,
) : SliceTransform {
    private val cctx: CPointer<ZSTD_CCtx> = checkNotNull(ZSTD_createCCtx()) {
        "Failed allocating zstd cctx"
    }

    val cleaner = createCleaner(cctx) {
        ZSTD_freeCCtx(it)
    }

    init {
        ZSTD_CCtx_setParameter(cctx, ZstdParameter.compressionLevel, compressionLevel)
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
                val result = ZSTD_compressStream2(
                    cctx = cctx,
                    input = inBuffer.ptr,
                    output = outBuffer.ptr,
                    endOp = if (finish) ZSTD_e_end else ZSTD_e_continue,
                )
                input.readStart = inBuffer.pos.convert()
                output.writeStart = outBuffer.pos.convert()
                if (result.convert<ULong>() != 0UL && ZSTD_isError(result) != 0U) {
                    error("Bad zstd result code $result: ${ZSTD_getErrorName(result)?.toKString()}")
                }
                output.insufficient = input.hasData || (finish && result.convert<ULong>() != 0UL)
            }
        }
    }
}
