#include <zstd.h>
#include <jni.h>

class __attribute__((visibility("hidden"))) SliceClass {
public:
    SliceClass(JNIEnv *env, jclass progressClass);

    jfieldID readStart;
    jfieldID writeStart;
};

SliceClass::SliceClass(JNIEnv *env, jclass progressClass)
        : readStart(env->GetFieldID(progressClass, "readStart", "I")),
          writeStart(env->GetFieldID(progressClass, "writeStart", "I")) {
}

static JavaVM *sVm;
static SliceClass *sliceClass;

extern "C" JNIEXPORT jint
JNI_OnLoad(
        JavaVM *vm,
        void *reserved
) {
    JNIEnv *env = NULL;
    sVm = vm;

    if (vm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK) {
        return JNI_ERR;
    }

    sliceClass = new SliceClass(env, env->FindClass("com/ensody/kompressor/core/ByteArraySlice"));

    return env->GetVersion();
}

extern "C" JNIEXPORT void
JNI_OnUnLoad(
        JavaVM *vm,
        void *reserved
) {
    delete sliceClass;
    sVm = NULL;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_ensody_kompressor_zstd_ZstdWrapper_createCompressor(
        JNIEnv *env,
        jobject type
) {
    auto cctx = ZSTD_createCCtx();
    return reinterpret_cast<jlong>(cctx);
}

extern "C" JNIEXPORT void JNICALL
Java_com_ensody_kompressor_zstd_ZstdWrapper_freeCompressor(
        JNIEnv *env,
        jobject type,
        jlong cctxPointer
) {
    auto cctx = reinterpret_cast<ZSTD_CCtx *>(cctxPointer);
    ZSTD_freeCCtx(cctx);
}

extern "C" JNIEXPORT jlong JNICALL
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

extern "C" JNIEXPORT jlong JNICALL
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
    ZSTD_outBuffer outBuffer = {
            .dst = outputElements,
            .size = static_cast<size_t>(outputEndExclusive),
            .pos = static_cast<size_t>(outputStart),
    };

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

    size_t result = ZSTD_compressStream2(cctx, &outBuffer, &inBuffer, finish ? ZSTD_e_end : ZSTD_e_continue);

    env->SetIntField(inputSlice, sliceClass->readStart, static_cast<jint>(inBuffer.pos));
    env->SetIntField(outputSlice, sliceClass->writeStart, static_cast<jint>(outBuffer.pos));

    env->ReleaseByteArrayElements(inputByteArray, inputElements, JNI_ABORT);
    env->ReleaseByteArrayElements(outputByteArray, outputElements, 0);

    return result;
}


extern "C" JNIEXPORT jlong JNICALL
Java_com_ensody_kompressor_zstd_ZstdWrapper_createDecompressor(
        JNIEnv *env,
        jobject type
) {
    ZSTD_DCtx *dctx = ZSTD_createDCtx();
    return reinterpret_cast<jlong>(dctx);
}

extern "C" JNIEXPORT void JNICALL
Java_com_ensody_kompressor_zstd_ZstdWrapper_freeDecompressor(
        JNIEnv *env,
        jobject type,
        jlong dctxPointer
) {
    auto dctx = reinterpret_cast<ZSTD_DCtx *>(dctxPointer);
    ZSTD_freeDCtx(dctx);
}

extern "C" JNIEXPORT jlong JNICALL
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
    ZSTD_outBuffer outBuffer = {
            .dst = outputElements,
            .size = static_cast<size_t>(outputEndExclusive),
            .pos = static_cast<size_t>(outputStart),
    };

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

    size_t result = ZSTD_decompressStream(dctx, &outBuffer, &inBuffer);

    env->SetIntField(inputSlice, sliceClass->readStart, static_cast<jint>(inBuffer.pos));
    env->SetIntField(outputSlice, sliceClass->writeStart, static_cast<jint>(outBuffer.pos));

    env->ReleaseByteArrayElements(inputByteArray, inputElements, JNI_ABORT);
    env->ReleaseByteArrayElements(outputByteArray, outputElements, 0);

    return result;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_ensody_kompressor_zstd_ZstdWrapper_getErrorName(JNIEnv *env, jobject type, jlong code) {
    auto codeValue = static_cast<size_t>(code);
    if (!ZSTD_isError(codeValue)) {
        return NULL;
    }
    return env->NewStringUTF(ZSTD_getErrorName(codeValue));
}
