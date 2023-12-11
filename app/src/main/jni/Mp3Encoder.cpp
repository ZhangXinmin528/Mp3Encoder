#include <jni.h>
#include "com_zxm_mp3encoder_encoder_Mp3Encoder.h"
#include <android/log.h> 

#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define LOG_TAG "Mp3Encoder"

JNIEXPORT void JNICALL
Java_com_zxm_mp3encoder_encoder_Mp3Encoder_encode(JNIEnv * env, jobject obj){
    LOGI("====encoder encode====");
  }
