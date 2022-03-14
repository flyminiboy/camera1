#include <jni.h>
#include "LogUtil.h"
#include "librtmp/rtmp.h"
#include "native-lib.h"

#include <libyuv.h>

using namespace libyuv;


static const char *RTMPClientClassName = "com/gpf/camera1/RTMPClient";
static const char *YUVUtilClassName = "com/gpf/camera1/YUVUtil";

static JNINativeMethod RTMPClientMethods[] = {
        "connect", "(Ljava/lang/String;)I", (jint *) connect
};
static JNINativeMethod YUVUtilMethods[] = {
        "nv21ToNV12", "([BII[B)V", (void *) nv21ToNV12,
        "nv21ToI420", "([BII[B)V", (void *) nv21ToI420,
        "i420ToNV21", "([BII[B)V", (void *) i420ToNV21,
        "rotateI420", "([BII[BI)V", (void *) rotateI420,
        "nv21ToARGB", "([BII[B)V", (void *) nv21ToARGB
};


extern "C"
JNIEXPORT
jint
JNICALL
connect(JNIEnv *env, jobject thiz, jstring path) {
    LOGE("connect");

    int result = -1;

    do {


    } while (0);

    return result;

}

extern "C"
JNIEXPORT
void
JNICALL
nv21ToNV12(JNIEnv *env, jobject thiz, jbyteArray src, jint width, jint height, jbyteArray dst) {

    jbyte *_src = env->GetByteArrayElements(src, nullptr);
    jbyte *_dst = env->GetByteArrayElements(dst, nullptr);

    jint src_y_size = width * height;

    jbyte *src_nv21_y_data = _src;
    jbyte *src_nv21_vu_data = _src + src_y_size;

    jbyte *dst_nv12_y_data = _dst;
    jbyte *dst_nv12_uv_data = _dst + src_y_size;

    NV21ToNV12(reinterpret_cast<const uint8_t *>(src_nv21_y_data), width,
               reinterpret_cast<const uint8_t *>(src_nv21_vu_data), width / 2,
               reinterpret_cast<uint8_t *>(dst_nv12_y_data), width,
               reinterpret_cast<uint8_t *>(dst_nv12_uv_data), width / 2,
               width,height);

    env->ReleaseByteArrayElements(src, _src, JNI_ABORT);
    env->ReleaseByteArrayElements(dst, _dst, 0);

}

extern "C"
JNIEXPORT
void
JNICALL
nv21ToI420(JNIEnv *env, jobject thiz, jbyteArray src, jint width,
           jint height, jbyteArray dst) {

    // 获取数组指针
    jbyte *_src = env->GetByteArrayElements(src, nullptr);
    jbyte *_dst = env->GetByteArrayElements(dst, nullptr);

    // Y 大小
    jint src_y_size = width * height;
    // u 大小
    jint src_u_size = (width >> 1) * (height >> 1);

    // NV21 yuv格式
    // Y 起始地址
    jbyte *src_nv21_y_data = _src;
    // UV 起始地址
    jbyte *src_nv21_vu_data = _src + src_y_size;

    // i420 yuv格式
    jbyte *dst_i420_y_data = _dst;
    jbyte *dst_i420_u_data = _dst + src_y_size;
    jbyte *dst_i420_v_data = _dst + src_y_size + src_u_size;

    // 理解 YUV stride
//    YUV420 系列
//    NV21
//        Y: 跨距为 width
//        VU: 跨距为 width
//    I420P(YU12):
//        Y: 跨距为 width
//        U: 跨距为 width/2
//        V: 跨距为 width/2
//    ABGR: 跨距为 4 *width


    NV21ToI420(reinterpret_cast<const uint8_t *>(src_nv21_y_data), width,
               reinterpret_cast<const uint8_t *>(src_nv21_vu_data), width,
               reinterpret_cast<uint8_t *>(dst_i420_y_data), width,
               reinterpret_cast<uint8_t *>(dst_i420_u_data), width >> 1,
               reinterpret_cast<uint8_t *>(dst_i420_v_data), width >> 1,
               width, height);

    env->ReleaseByteArrayElements(src, _src, JNI_ABORT);
    env->ReleaseByteArrayElements(dst, _dst, 0);

}

extern "C"
JNIEXPORT
void
JNICALL
rotateI420(JNIEnv *env, jobject thiz, jbyteArray src, jint width,
           jint height, jbyteArray dst, jint degree) {

    // 获取数组指针
    jbyte *_src = env->GetByteArrayElements(src, nullptr);
    jbyte *_dst = env->GetByteArrayElements(dst, nullptr);

    // u 大小
    jint src_y_size = width * height;
    jint src_u_size = (width >> 1) * (height >> 1);

    // Y 起始地址
    jbyte *src_i420_y_data = _src;
    jbyte *src_i420_u_data = _src + src_y_size;
    jbyte *src_i420_v_data = _src + src_y_size + src_u_size;

    // i420 yuv格式
    jbyte *dst_i420_y_data = _dst;
    jbyte *dst_i420_u_data = _dst + src_y_size;
    jbyte *dst_i420_v_data = _dst + src_y_size + src_u_size;

    // TODO 注意 这个地方旋转以后宽高的调整 ？？
    I420Rotate(reinterpret_cast<uint8_t *>(src_i420_y_data), width,
               reinterpret_cast<uint8_t *>(src_i420_u_data), width >> 1,
               reinterpret_cast<uint8_t *>(src_i420_v_data), width >> 1,
               reinterpret_cast<uint8_t *>(dst_i420_y_data), width,
               reinterpret_cast<uint8_t *>(dst_i420_u_data), height >> 1,
               reinterpret_cast<uint8_t *>(dst_i420_v_data), height >> 1,
               width, height, kRotate90);

    env->ReleaseByteArrayElements(src, _src, JNI_ABORT);
    env->ReleaseByteArrayElements(dst, _dst, 0);

}

extern "C"
JNIEXPORT
void
JNICALL
i420ToNV21(JNIEnv *env, jobject thiz, jbyteArray src, jint width,
           jint height, jbyteArray dst) {

    // 获取数组指针
    jbyte *_src = env->GetByteArrayElements(src, nullptr);
    jbyte *_dst = env->GetByteArrayElements(dst, nullptr);

    // u 大小
    jint src_y_size = width * height;
    jint src_u_size = (width >> 1) * (height >> 1);

    // Y 起始地址
    jbyte *src_i420_y_data = _src;
    jbyte *src_i420_u_data = _src + src_y_size;
    jbyte *src_i420_v_data = _src + src_y_size + src_u_size;

    jbyte *dst_nv21_y_data = _dst;
    // VU 起始地址
    jbyte *dst_nv21_vu_data = _dst + src_y_size;

    I420ToNV21(reinterpret_cast<uint8_t *>(src_i420_y_data), width,
               reinterpret_cast<uint8_t *>(src_i420_u_data), width >> 1,
               reinterpret_cast<uint8_t *>(src_i420_v_data), width >> 1,
               reinterpret_cast<uint8_t *>(dst_nv21_y_data), width,
               reinterpret_cast<uint8_t *>(dst_nv21_vu_data), width,
               width, height);

    env->ReleaseByteArrayElements(src, _src, JNI_ABORT);
    env->ReleaseByteArrayElements(dst, _dst, 0);

}

JNIEXPORT
void
JNICALL
nv21ToARGB(JNIEnv *env, jobject thiz, jbyteArray src, jint width,
           jint height, jbyteArray dst) {

    jbyte *_src = env->GetByteArrayElements(src, nullptr);
    jbyte *_dst = env->GetByteArrayElements(dst, nullptr);

    jint src_y_size = width * height;

    jbyte *src_y_data = _src;
    jbyte *src_vu_data = _src + src_y_size;

    jbyte *argb = _dst;


    // TODO 一个神奇的地方

//    这里值得注意的是，由于libyuv的ARGB和android bitmap的ARGB_8888的存储顺序是不一样的，
//    ARGB_8888的存储顺序实际上是RGBA,对应的是libyuv的ABGR

// NV21ToABGR

// NV21ToARGB 存在色差
//

    NV21ToABGR(reinterpret_cast<uint8_t *>(src_y_data), width,
               reinterpret_cast<uint8_t *>(src_vu_data), width,
               reinterpret_cast<uint8_t *>(argb), width * 4,
               width, height);

}

jint registNativeMethod(JNIEnv *env, const char *className,
                        const JNINativeMethod methods[], const jint methodSize) {

    int result = -1;

    do {
        // 获取映射的java类
        jclass clazz = env->FindClass(className);
        if (clazz == nullptr) {
            break;
        }

        // 通过RegisterNatives方法动态注册登记
        if (env->RegisterNatives(clazz, methods, methodSize) < 0) {
            break;
        }

        result = JNI_OK;

    } while (0);

    return result;
}

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = nullptr;
    int result = -1;

    do {

        // 1.通过JavaVM 创建全新的JNIEnv
        if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
            break;
        }

        if (registNativeMethod(env, RTMPClientClassName, RTMPClientMethods,
                               sizeof(RTMPClientMethods) / sizeof(RTMPClientMethods[0])) !=
            JNI_OK) {
            break;
        }

        if (registNativeMethod(env, YUVUtilClassName, YUVUtilMethods,
                               sizeof(YUVUtilMethods) / sizeof(YUVUtilMethods[0])) != JNI_OK) {
            break;
        }

        result = JNI_VERSION_1_6;

    } while (0);

    return result;


}

/**
 *
 * 静态注册
 * 通过 JNIEXPORT 和 JNICALL 两个宏定义声明，在虚拟机加载 so 时发现上面两个宏定义的函数时就会链接到对应的 native 方法
 * 对应规则
 * Java + 包名 + 类名 + 方法名
 * 其中使用下划线将每部分隔开，包名也使用下划线隔开，如果名称中本来就包含下划线，将使用下划线加数字替换
 *
 * 问题
 * 必须遵循注册规则
 * 名字过长
 * 运行时去找效率不高
 *
 *
 * 动态注册
 * 通过 RegisterNatives 方法手动完成 native 方法和 so 中的方法的绑定，这样虚拟机就可以通过这个函数映射表直接找到相应的方法了
 * 通常我们在 JNI_OnLoad 方法中完成动态注册 （java层通过System.loadLibrary()方法可以加载一个动态库,此时虚拟机就会调用jni库中的JNI_OnLoad()函数）
 */
