LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_SRC_FILES := C:/Users/Administrator/Desktop/.exercises/android/Tools/app/libs/$(TARGET_ARCH_ABI)/libiconv.so
LOCAL_MODULE := libiconv
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE     :=  native-lib
LOCAL_SRC_FILES  := file.c \
                native-lib.c
LOCAL_C_INCLUDES := \
			$(LOCAL_PATH)/thirdparty/include \
			$(LOCAL_PATH)/thirdparty/libcharset \
			$(LOCAL_PATH)/thirdparty/libcharset/include

LOCAL_SHARED_LIBRARIES := libiconv
include $(BUILD_SHARED_LIBRARY)