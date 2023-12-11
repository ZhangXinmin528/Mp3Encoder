APP_PLATFORM=android-16

#APP_ABI := armeabi-v7a
APP_ABI := armeabi-v7a arm64-v8a x86 x86_64

APP_OPTIM := release
APP_ARM_MODE := arm

APP_CFLAGS := -O2
APP_CPPFLAGS := -O2

APP_STL := c++_static

APP_CPPFLAGS := -std=gnu++11 -fexceptions -D__STDC_LIMIT_MACROS
NDK_TOOLCHAIN_VERSION = 4.8