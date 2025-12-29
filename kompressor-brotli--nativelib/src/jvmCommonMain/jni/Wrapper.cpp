#include <brotli/decode.h>
#include <brotli/encode.h>
#include <jni.h>
#include "DefaultLoad.h"
#include "SliceClass.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL
Java_com_ensody_kompressor_brotli_BrotliWrapper_createCompressor(
        JNIEnv *env,
        jobject type
) {
    BrotliEncoderState *state = BrotliEncoderCreateInstance(NULL, NULL, NULL);
    return reinterpret_cast<jlong>(state);
}

JNIEXPORT void JNICALL
Java_com_ensody_kompressor_brotli_BrotliWrapper_freeCompressor(
        JNIEnv *env,
        jobject type,
        jlong statePointer
) {
    auto state = reinterpret_cast<BrotliEncoderState *>(statePointer);
    BrotliEncoderDestroyInstance(state);
}

JNIEXPORT jlong JNICALL
Java_com_ensody_kompressor_brotli_BrotliWrapper_setCompressorParameter(
        JNIEnv *env,
        jobject type,
        jlong statePointer,
        jint param,
        jint value
) {
    auto state = reinterpret_cast<BrotliEncoderState *>(statePointer);
    return BrotliEncoderSetParameter(state, static_cast<BrotliEncoderParameter>(param), static_cast<uint32_t>(value));
}

JNIEXPORT jint JNICALL
Java_com_ensody_kompressor_brotli_BrotliWrapper_compressStream(
        JNIEnv *env,
        jobject type,
        jlong statePointer,
        jobject inputSlice,
        jbyteArray inputByteArray,
        jint inputStart,
        jint inputEndExclusive,
        jobject outputSlice,
        jbyteArray outputByteArray,
        jint outputStart,
        jint outputEndExclusive,
        jboolean finish
) {
    auto state = reinterpret_cast<BrotliEncoderState *>(statePointer);

    auto outputElements = env->GetByteArrayElements(outputByteArray, NULL);
    if (outputElements == NULL) {
        return -1;
    }
    jbyte *inputElements = env->GetByteArrayElements(inputByteArray, NULL);
    if (inputElements == NULL) {
        env->ReleaseByteArrayElements(outputByteArray, outputElements, 0);
        return -1;
    }

    const uint8_t *nextIn = reinterpret_cast<uint8_t *>(inputElements + inputStart);
    size_t availIn = static_cast<size_t>(inputEndExclusive - inputStart);
    uint8_t *nextOut = reinterpret_cast<uint8_t *>(outputElements + outputStart);
    size_t availOut = static_cast<size_t>(outputEndExclusive - outputStart);

    BrotliEncoderOperation operation = finish ? BROTLI_OPERATION_FINISH : BROTLI_OPERATION_PROCESS;
    auto result = BrotliEncoderCompressStream(state, operation, &availIn, &nextIn, &availOut, &nextOut, NULL);

    env->SetIntField(inputSlice, sliceClass->readStart, static_cast<jint>(nextIn - reinterpret_cast<uint8_t *>(inputElements)));
    env->SetIntField(outputSlice, sliceClass->writeStart, static_cast<jint>(nextOut - reinterpret_cast<uint8_t *>(outputElements)));

    env->ReleaseByteArrayElements(inputByteArray, inputElements, JNI_ABORT);
    env->ReleaseByteArrayElements(outputByteArray, outputElements, 0);

    if (result == BROTLI_FALSE) {
        return -1;
    }

    return BrotliEncoderIsFinished(state);
}

JNIEXPORT jlong JNICALL
Java_com_ensody_kompressor_brotli_BrotliWrapper_createDecompressor(
        JNIEnv *env,
        jobject type
) {
    BrotliDecoderState *state = BrotliDecoderCreateInstance(NULL, NULL, NULL);
    return reinterpret_cast<jlong>(state);
}

JNIEXPORT void JNICALL
Java_com_ensody_kompressor_brotli_BrotliWrapper_freeDecompressor(
        JNIEnv *env,
        jobject type,
        jlong statePointer
) {
    auto state = reinterpret_cast<BrotliDecoderState *>(statePointer);
    BrotliDecoderDestroyInstance(state);
}

JNIEXPORT jlong JNICALL
Java_com_ensody_kompressor_brotli_BrotliWrapper_setDecompressorParameter(
        JNIEnv *env,
        jobject type,
        jlong statePointer,
        jint param,
        jint value
) {
    auto state = reinterpret_cast<BrotliDecoderState *>(statePointer);
    return BrotliDecoderSetParameter(state, static_cast<BrotliDecoderParameter>(param), static_cast<uint32_t>(value));
}

JNIEXPORT jint JNICALL
Java_com_ensody_kompressor_brotli_BrotliWrapper_decompressStream(
        JNIEnv *env,
        jobject type,
        jlong statePointer,
        jobject inputSlice,
        jbyteArray inputByteArray,
        jint inputStart,
        jint inputEndExclusive,
        jobject outputSlice,
        jbyteArray outputByteArray,
        jint outputStart,
        jint outputEndExclusive
) {
    auto state = reinterpret_cast<BrotliDecoderState *>(statePointer);

    auto outputElements = env->GetByteArrayElements(outputByteArray, NULL);
    if (outputElements == NULL) {
        return -1;
    }
    jbyte *inputElements = env->GetByteArrayElements(inputByteArray, NULL);
    if (inputElements == NULL) {
        env->ReleaseByteArrayElements(outputByteArray, outputElements, 0);
        return -1;
    }

    const uint8_t *nextIn = reinterpret_cast<uint8_t *>(inputElements + inputStart);
    size_t availIn = static_cast<size_t>(inputEndExclusive - inputStart);
    uint8_t *nextOut = reinterpret_cast<uint8_t *>(outputElements + outputStart);
    size_t availOut = static_cast<size_t>(outputEndExclusive - outputStart);

    auto result = BrotliDecoderDecompressStream(state, &availIn, &nextIn, &availOut, &nextOut, NULL);

    env->SetIntField(inputSlice, sliceClass->readStart, static_cast<jint>(nextIn - reinterpret_cast<uint8_t *>(inputElements)));
    env->SetIntField(outputSlice, sliceClass->writeStart, static_cast<jint>(nextOut - reinterpret_cast<uint8_t *>(outputElements)));

    env->ReleaseByteArrayElements(inputByteArray, inputElements, JNI_ABORT);
    env->ReleaseByteArrayElements(outputByteArray, outputElements, 0);

    return result;
}

#ifdef __cplusplus
}
#endif
