#include <jni.h>
#include "file.h"

JNIEXPORT void JNICALL
Java_psycho_euphoria_file_Native_deleteFile(JNIEnv *env, jobject thiz, jstring path) {
    const char *dir = (*env)->GetStringUTFChars(env, path, NULL);
    remove_directory(dir);
    (*env)->ReleaseStringChars(env, path, dir);

}