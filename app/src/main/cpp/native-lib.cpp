#include <jni.h>
#include "LogUtil.h"
#include "librtmp/rtmp.h"

extern "C"
jint connect(JNIEnv* env, jobject thiz, jstring path) {
    LOGE("connect");

    int result = -1;

    do {




    } while (0);

    return result;

}

jint registNativeMethod(JNIEnv *env) {

    int result = -1;

    do {
        // 获取映射的java类
        jclass clazz = env->FindClass("com/gpf/camera1/RTMPClient");
        if (clazz == nullptr) {
            break;
        }

        JNINativeMethod methods_RTMPClient[] = {
                {"connect", "(Ljava/lang/String;)I", (jint *) connect}
        };
        // 通过RegisterNatives方法动态注册登记
        if (env->RegisterNatives(clazz, methods_RTMPClient, sizeof(methods_RTMPClient) / sizeof(JNINativeMethod)) < 0) {
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

        if (registNativeMethod(env) != JNI_OK) {
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
