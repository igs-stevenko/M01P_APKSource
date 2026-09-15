LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := rkcrypto_jni

LOCAL_SRC_FILES := rkcrypto_jni.c

LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)/include

LOCAL_LDLIBS := -llog

LOCAL_SHARED_LIBRARIES := librkcrypto

LOCAL_CFLAGS := -Wall -Wno-unused-parameter

include $(BUILD_SHARED_LIBRARY)

# Prebuilt librkcrypto (從設備取得或 SDK 提供)
include $(CLEAR_VARS)
LOCAL_MODULE := librkcrypto
LOCAL_SRC_FILES := libs/$(TARGET_ARCH_ABI)/librkcrypto.so
include $(PREBUILT_SHARED_LIBRARY)
