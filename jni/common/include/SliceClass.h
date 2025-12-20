#ifndef SLICE_CLASS_H
#define SLICE_CLASS_H

#include <jni.h>

class __attribute__((visibility("hidden"))) SliceClass {
public:
    SliceClass(JNIEnv *env, jclass progressClass);

    jfieldID readStart;
    jfieldID writeStart;
};

#endif
