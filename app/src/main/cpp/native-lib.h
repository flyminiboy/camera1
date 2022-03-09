#include <jni.h>
#include <string>

#ifndef _NATIVE_LIB_H_
#define _NATIVE_LIB_H_

extern "C" {

JNIEXPORT jint JNICALL
connect(JNIEnv *env, jobject thiz, jstring path);

JNIEXPORT void JNICALL
conver(JNIEnv * env, jobject thiz, jbyteArray src, jint width,
       jint height, jbyteArray dst);

}

#endif