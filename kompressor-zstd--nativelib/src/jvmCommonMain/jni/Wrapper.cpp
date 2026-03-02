#include <zstd.h>
#include <jni.h>
#include "DefaultLoad.h"
#include "SliceClass.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL
Java_com_ensody_kompressor_zstd_ZstdWrapper_createCompressor(
        JNIEnv *env,
        jobject type
) {
    auto cctx = ZSTD_createCCtx();
    return reinterpret_cast<jlong>(cctx);
}

JNIEXPORT void JNICALL
Java_com_ensody_kompressor_zstd_ZstdWrapper_freeCompressor(
        JNIEnv *env,
        jobject type,
        jlong cctxPointer
) {
    auto cctx = reinterpret_cast<ZSTD_CCtx *>(cctxPointer);
    ZSTD_freeCCtx(cctx);
}

JNIEXPORT jlong JNICALL
Java_com_ensody_kompressor_zstd_ZstdWrapper_setParameter(
        JNIEnv *env,
        jobject type,
        jlong cctxPointer,
        jint parameter,
        jint value
) {
    auto cctx = reinterpret_cast<ZSTD_CCtx *>(cctxPointer);
    return ZSTD_CCtx_setParameter(cctx, static_cast<ZSTD_cParameter>(parameter), value);
}

JNIEXPORT jlong JNICALL
Java_com_ensody_kompressor_zstd_ZstdWrapper_loadCompressorDictionary(
        JNIEnv *env,
        jobject type,
        jlong cctxPointer,
        jbyteArray dictionary
) {
    auto cctx = reinterpret_cast<ZSTD_CCtx *>(cctxPointer);
    auto dictionaryElements = env->GetByteArrayElements(dictionary, NULL);
    if (dictionaryElements == NULL) {
        return -ZSTD_error_GENERIC;
    }
    size_t result = ZSTD_CCtx_loadDictionary(cctx, dictionaryElements, env->GetArrayLength(dictionary));
    env->ReleaseByteArrayElements(dictionary, dictionaryElements, JNI_ABORT);
    return result;
}

JNIEXPORT jlong JNICALL
Java_com_ensody_kompressor_zstd_ZstdWrapper_loadDecompressorDictionary(
        JNIEnv *env,
        jobject type,
        jlong dctxPointer,
        jbyteArray dictionary
) {
    auto dctx = reinterpret_cast<ZSTD_DCtx *>(dctxPointer);
    auto dictionaryElements = env->GetByteArrayElements(dictionary, NULL);
    if (dictionaryElements == NULL) {
        return -ZSTD_error_GENERIC;
    }
    size_t result = ZSTD_DCtx_loadDictionary(dctx, dictionaryElements, env->GetArrayLength(dictionary));
    env->ReleaseByteArrayElements(dictionary, dictionaryElements, JNI_ABORT);
    return result;
}

JNIEXPORT jlong JNICALL
Java_com_ensody_kompressor_zstd_ZstdWrapper_compressStream(
        JNIEnv *env,
        jobject type,
        jlong cctxPointer,
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
    auto cctx = reinterpret_cast<ZSTD_CCtx *>(cctxPointer);

    auto outputElements = env->GetByteArrayElements(outputByteArray, NULL);
    if (outputElements == NULL) {
        return -ZSTD_error_GENERIC;
    }
    jbyte *inputElements = env->GetByteArrayElements(inputByteArray, NULL);
    if (inputElements == NULL) {
        env->ReleaseByteArrayElements(outputByteArray, outputElements, 0);
        return -ZSTD_error_GENERIC;
    }
    ZSTD_inBuffer inBuffer = {
            .src = inputElements,
            .size = static_cast<size_t>(inputEndExclusive),
            .pos = static_cast<size_t>(inputStart),
    };
    ZSTD_outBuffer outBuffer = {
            .dst = outputElements,
            .size = static_cast<size_t>(outputEndExclusive),
            .pos = static_cast<size_t>(outputStart),
    };

    size_t result = ZSTD_compressStream2(cctx, &outBuffer, &inBuffer, finish ? ZSTD_e_end : ZSTD_e_continue);

    env->SetIntField(inputSlice, sliceClass->readStart, static_cast<jint>(inBuffer.pos));
    env->SetIntField(outputSlice, sliceClass->writeStart, static_cast<jint>(outBuffer.pos));

    env->ReleaseByteArrayElements(inputByteArray, inputElements, JNI_ABORT);
    env->ReleaseByteArrayElements(outputByteArray, outputElements, 0);

    return result;
}

JNIEXPORT jlong JNICALL
Java_com_ensody_kompressor_zstd_ZstdWrapper_createDecompressor(
        JNIEnv *env,
        jobject type
) {
    ZSTD_DCtx *dctx = ZSTD_createDCtx();
    return reinterpret_cast<jlong>(dctx);
}

JNIEXPORT void JNICALL
Java_com_ensody_kompressor_zstd_ZstdWrapper_freeDecompressor(
        JNIEnv *env,
        jobject type,
        jlong dctxPointer
) {
    auto dctx = reinterpret_cast<ZSTD_DCtx *>(dctxPointer);
    ZSTD_freeDCtx(dctx);
}

JNIEXPORT jlong JNICALL
Java_com_ensody_kompressor_zstd_ZstdWrapper_decompressStream(
        JNIEnv *env,
        jobject type,
        jlong dctxPointer,
        jobject inputSlice,
        jbyteArray inputByteArray,
        jint inputStart,
        jint inputEndExclusive,
        jobject outputSlice,
        jbyteArray outputByteArray,
        jint outputStart,
        jint outputEndExclusive
) {
    auto dctx = reinterpret_cast<ZSTD_DCtx *>(dctxPointer);

    auto outputElements = env->GetByteArrayElements(outputByteArray, NULL);
    if (outputElements == NULL) {
        return -ZSTD_error_GENERIC;
    }
    jbyte *inputElements = env->GetByteArrayElements(inputByteArray, NULL);
    if (inputElements == NULL) {
        env->ReleaseByteArrayElements(outputByteArray, outputElements, 0);
        return -ZSTD_error_GENERIC;
    }
    ZSTD_inBuffer inBuffer = {
            .src = inputElements,
            .size = static_cast<size_t>(inputEndExclusive),
            .pos = static_cast<size_t>(inputStart),
    };
    ZSTD_outBuffer outBuffer = {
            .dst = outputElements,
            .size = static_cast<size_t>(outputEndExclusive),
            .pos = static_cast<size_t>(outputStart),
    };

    size_t result = ZSTD_decompressStream(dctx, &outBuffer, &inBuffer);

    env->SetIntField(inputSlice, sliceClass->readStart, static_cast<jint>(inBuffer.pos));
    env->SetIntField(outputSlice, sliceClass->writeStart, static_cast<jint>(outBuffer.pos));

    env->ReleaseByteArrayElements(inputByteArray, inputElements, JNI_ABORT);
    env->ReleaseByteArrayElements(outputByteArray, outputElements, 0);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ensody_kompressor_zstd_ZstdWrapper_getErrorName(JNIEnv *env, jobject type, jlong code) {
    auto codeValue = static_cast<size_t>(code);
    if (!ZSTD_isError(codeValue)) {
        return NULL;
    }
    return env->NewStringUTF(ZSTD_getErrorName(codeValue));
}

#ifdef __cplusplus
}
#endif
