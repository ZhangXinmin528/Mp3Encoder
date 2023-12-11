    LOCAL_PATH := $(call my-dir)
    include $(CLEAR_VARS)
    LOCAL_SRC_FILES = ./Mp3Encoder.cpp
    LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog
    LOCAL_MODULE := libaudioencoder
    include $(BUILD_SHARED_LIBRARY)