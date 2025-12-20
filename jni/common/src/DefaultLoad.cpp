#include <jni.h>
#include "DefaultLoad.h"

JavaVM *sVm;
SliceClass *sliceClass;

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint
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

JNIEXPORT void
JNI_OnUnLoad(
        JavaVM *vm,
        void *reserved
) {
    delete sliceClass;
    sVm = NULL;
}

#ifdef __cplusplus
}
#endif
