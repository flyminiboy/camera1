#include <jni.h>
#include "LogUtil.h"
#include "librtmp/rtmp.h"
#include "native-lib.h"

#include <libyuv.h>

using namespace std;
using namespace libyuv;


static const char *RTMPClientClassName = "com/gpf/camera1/RTMPClient";
static const char *YUVUtilClassName = "com/gpf/camera1/YUVUtil";

static JNINativeMethod RTMPClientMethods[] = {
        "connect", "(Ljava/lang/String;)I", (jint *)connect
};
static JNINativeMethod YUVUtilMethods[] = {
        "conver", "([BII[B)V", (void *)conver
};


extern "C"
JNIEXPORT
jint
JNICALL
connect(JNIEnv* env, jobject thiz, jstring path) {
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
conver(JNIEnv * env, jobject thiz, jbyteArray src, jint width,
       jint height, jbyteArray dst) {

    // 获取数组指针
    jbyte *_src = env->GetByteArrayElements(src, nullptr);
    jbyte *_dst = env->GetByteArrayElements(dst, nullptr);

    // 类型转换
    unsigned char *pY = reinterpret_cast<unsigned char *>(_src);
    unsigned char *pUV = reinterpret_cast<unsigned char *>(_dst) + width * height;
    unsigned char *dst_temp = reinterpret_cast<unsigned char *>(_dst);

    NV12ToABGR(pY, width, pUV, width, dst_temp, width * 4, width, height);

    env->ReleaseByteArrayElements(src, _src, JNI_ABORT);
    env->ReleaseByteArrayElements(dst, _dst, 0);

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

        if (registNativeMethod(env, RTMPClientClassName, RTMPClientMethods, sizeof(RTMPClientMethods) / sizeof(RTMPClientMethods[0])) != JNI_OK) {
            break;
        }

        if (registNativeMethod(env, YUVUtilClassName, YUVUtilMethods, sizeof(YUVUtilMethods) / sizeof(YUVUtilMethods[0])) != JNI_OK) {
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
