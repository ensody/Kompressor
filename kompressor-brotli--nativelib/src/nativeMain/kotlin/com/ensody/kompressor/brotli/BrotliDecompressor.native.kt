package com.ensody.kompressor.brotli

import com.ensody.kompressor.core.ByteArraySlice
import com.ensody.kompressor.core.SliceTransform
import com.ensody.kompressor.internal.brotli.BROTLI_OPERATION_FINISH
import com.ensody.kompressor.internal.brotli.BROTLI_OPERATION_PROCESS
import com.ensody.kompressor.internal.brotli.BrotliDecoderCreateInstance
import com.ensody.kompressor.internal.brotli.BrotliDecoderDecompressStream
import com.ensody.kompressor.internal.brotli.BrotliDecoderDestroyInstance
import com.ensody.kompressor.internal.brotli.BrotliDecoderState
import com.ensody.kompressor.internal.brotli.BrotliEncoderCompressStream
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.ULongVar
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlin.native.ref.createCleaner

public actual fun BrotliDecompressor(): SliceTransform =
    BrotliDecompressorImpl()

@OptIn(UnsafeNumber::class)
internal class BrotliDecompressorImpl : SliceTransform {
    private val state: CPointer<BrotliDecoderState> = checkNotNull(BrotliDecoderCreateInstance(null, null, null)) {
        "Failed allocating Brotli decoder"
    }

    val cleaner = createCleaner(state) {
        BrotliDecoderDestroyInstance(it)
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
                val result = BrotliDecoderDecompressStream(
                    state = state,
                    available_in = availIn.ptr.reinterpret(),
                    next_in = nextIn.ptr,
                    available_out = availOut.ptr.reinterpret(),
                    next_out = nextOut.ptr,
                    total_out = null,
                )
                input.readStart += input.remainingRead - availIn.value.toInt()
                output.writeStart += output.remainingWrite - availOut.value.toInt()
                if (result == BrotliDecoderResult.BROTLI_DECODER_RESULT_ERROR.value.convert<UInt>()) {
                    error("Bad Brotli result code: $result")
                }
                input.insufficient =
                    result == BrotliDecoderResult.BROTLI_DECODER_RESULT_NEEDS_MORE_INPUT.value.convert<UInt>()
                output.insufficient = input.hasData ||
                    (finish && result != BrotliDecoderResult.BROTLI_DECODER_RESULT_SUCCESS.value.convert<UInt>())
            }
        }
    }
}
