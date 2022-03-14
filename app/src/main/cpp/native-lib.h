#include <jni.h>
#include <string>

#ifndef _NATIVE_LIB_H_
#define _NATIVE_LIB_H_

extern "C" {

JNIEXPORT jint JNICALL
connect(JNIEnv *env, jobject thiz, jstring path);

JNIEXPORT void JNICALL
nv21ToI420(JNIEnv * env, jobject thiz, jbyteArray src, jint width,
       jint height, jbyteArray dst);

JNIEXPORT void JNICALL
nv21ToNV12(JNIEnv * env, jobject thiz, jbyteArray src, jint width,
           jint height, jbyteArray dst);

JNIEXPORT void JNICALL
i420ToNV21(JNIEnv * env, jobject thiz, jbyteArray src, jint width,
           jint height, jbyteArray dst);

JNIEXPORT void JNICALL
rotateI420(JNIEnv * env, jobject thiz, jbyteArray src, jint width,
           jint height, jbyteArray dst, jint degree);

JNIEXPORT void JNICALL
nv21ToARGB(JNIEnv *env, jobject thiz, jbyteArray src, jint width,
        jint height, jbyteArray dst);

}

#endif