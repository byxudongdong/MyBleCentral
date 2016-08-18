LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := JniReadFile
LOCAL_SRC_FILES := file_parse.cpp AES.cpp AESENC.cpp aesEnDecode.cpp
#增加 log 函数对应的log 库  liblog.so  libthread_db.a
LOCAL_LDLIBS :=  -lz -llog
LOCAL_CPPFLAGS  += -fexceptions
include $(BUILD_SHARED_LIBRARY)