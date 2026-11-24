// Adapted from trexx/moonlight-android xow_driver_jni.cpp. GPL-3.0-or-later.
#include <jni.h>
#include <memory>
#include <exception>
#include "dongle/dongle.h"
#include "utils/crypto.h"
#include "utils/log.h"

extern "C" JNIEXPORT jlong JNICALL
Java_dev_local_lunatv_receiver_ReceiverManager_createDriver(JNIEnv* env, jobject self, jint fd) {
    jobject ref = nullptr;
    try {
        auto usb = std::make_unique<UsbDevice>(fd);
        JavaVM* vm = nullptr;
        if (env->GetJavaVM(&vm) != JNI_OK) return 0;
        ref = env->NewGlobalRef(self);
        if (!ref) return 0;
        return reinterpret_cast<jlong>(new Dongle(std::move(usb), ref, vm, 20));
    } catch (const std::exception& e) {
        if (ref) env->DeleteGlobalRef(ref);
        Log::error("Receiver open failed: %s", e.what());
        return 0;
    }
}
extern "C" JNIEXPORT jboolean JNICALL
Java_dev_local_lunatv_receiver_ReceiverManager_startDriver(JNIEnv* env, jobject, jlong handle, jstring path) {
    if (!handle || !path) return false;
    const char* utf = env->GetStringUTFChars(path, nullptr);
    if (!utf) return false;
    bool result = false;
    try { result = reinterpret_cast<Dongle*>(handle)->start(utf); }
    catch (const std::exception& e) { Log::error("Receiver start failed: %s", e.what()); }
    env->ReleaseStringUTFChars(path, utf);
    return result;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_dev_local_lunatv_receiver_ReceiverManager_pairDriver(JNIEnv*, jobject, jlong handle, jboolean enable) {
    return handle && reinterpret_cast<Dongle*>(handle)->setPairing(enable);
}
extern "C" JNIEXPORT void JNICALL
Java_dev_local_lunatv_receiver_ReceiverManager_destroyDriver(JNIEnv*, jobject, jlong handle) {
    delete reinterpret_cast<Dongle*>(handle);
}
extern "C" JNIEXPORT void JNICALL
Java_dev_local_lunatv_receiver_ReceiverPad_registerNative(JNIEnv* env, jobject self, jlong handle) {
    JavaVM* vm = nullptr;
    if (handle && env->GetJavaVM(&vm) == JNI_OK)
        reinterpret_cast<Controller*>(handle)->registerJavaContext(vm, env, env->NewGlobalRef(self));
}
extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) return JNI_ERR;
    jclass crypto = env->FindClass("dev/local/lunatv/receiver/GipCrypto");
    if (!crypto) return JNI_ERR;
    GipCrypto::init(env, crypto);
    env->DeleteLocalRef(crypto);
    return JNI_VERSION_1_6;
}
