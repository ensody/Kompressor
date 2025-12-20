#include <jni.h>
#include "SliceClass.h"

SliceClass::SliceClass(JNIEnv *env, jclass progressClass)
        : readStart(env->GetFieldID(progressClass, "readStart", "I")),
          writeStart(env->GetFieldID(progressClass, "writeStart", "I")) {
}
