package com.ensody.kompressor.brotli

import com.ensody.kompressor.core.ByteArraySlice
import com.ensody.kompressor.core.SliceTransform
import com.ensody.kompressor.internal.brotli.BROTLI_FALSE
import com.ensody.kompressor.internal.brotli.BROTLI_OPERATION_FINISH
import com.ensody.kompressor.internal.brotli.BROTLI_OPERATION_PROCESS
import com.ensody.kompressor.internal.brotli.BROTLI_TRUE
import com.ensody.kompressor.internal.brotli.BrotliEncoderCompressStream
import com.ensody.kompressor.internal.brotli.BrotliEncoderCreateInstance
import com.ensody.kompressor.internal.brotli.BrotliEncoderDestroyInstance
import com.ensody.kompressor.internal.brotli.BrotliEncoderIsFinished
import com.ensody.kompressor.internal.brotli.BrotliEncoderSetParameter
import com.ensody.kompressor.internal.brotli.BrotliEncoderState
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.ULongVar
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlin.native.ref.createCleaner

public actual fun BrotliCompressor(
    compressionLevel: Int,
    mode: BrotliCompressorMode,
    lgwin: Int,
): SliceTransform =
    BrotliCompressorImpl(
        mode = mode,
        compressionLevel = compressionLevel,
        lgwin = lgwin,
    )

@OptIn(UnsafeNumber::class)
internal class BrotliCompressorImpl(
    private val compressionLevel: Int,
    private val mode: BrotliCompressorMode,
    private val lgwin: Int,
) : SliceTransform {
    private val state: CPointer<BrotliEncoderState> = checkNotNull(BrotliEncoderCreateInstance(null, null, null)) {
        "Failed allocating Brotli encoder"
    }

    val cleaner = createCleaner(state) {
        BrotliEncoderDestroyInstance(it)
    }

    init {
        BrotliEncoderSetParameter(state, BrotliCompressorParameter.MODE.value.convert(), mode.value.convert())
        BrotliEncoderSetParameter(state, BrotliCompressorParameter.QUALITY.value.convert(), compressionLevel.convert())
        BrotliEncoderSetParameter(state, BrotliCompressorParameter.LGWIN.value.convert(), lgwin.convert())
    }

    override fun transform(input: ByteArraySlice, output: ByteArraySlice, finish: Boolean) = memScoped {
        input.data.usePinned { pinnedInput ->
            output.data.usePinned { pinnedOutput ->
                val nextIn = alloc<CPointerVar<UByteVar>> {
                    value = if (input.remainingRead == 0) {
                        null
                    } else {
                        pinnedInput.addressOf(input.readStart).reinterpret()
                    }
                }
                val availIn = alloc<ULongVar> { value = input.remainingRead.convert() }
                val nextOut = alloc<CPointerVar<UByteVar>> {
                    value = if (output.remainingWrite == 0) {
                        null
                    } else {
                        pinnedOutput.addressOf(output.writeStart).reinterpret()
                    }
                }
                val availOut = alloc<ULongVar> { value = output.remainingWrite.convert() }
                val operation = if (finish) BROTLI_OPERATION_FINISH else BROTLI_OPERATION_PROCESS
                val result = BrotliEncoderCompressStream(
                    state = state,
                    op = operation,
                    available_in = availIn.ptr.reinterpret(),
                    next_in = nextIn.ptr,
                    available_out = availOut.ptr.reinterpret(),
                    next_out = nextOut.ptr,
                    total_out = null,
                )
                input.readStart += input.remainingRead - availIn.value.toInt()
                output.writeStart += output.remainingWrite - availOut.value.toInt()
                if (result == BROTLI_FALSE) {
                    error("Bad Brotli result code: $result")
                }
                output.insufficient = input.hasData || (finish && BrotliEncoderIsFinished(state) != BROTLI_TRUE)
            }
        }
    }
}
