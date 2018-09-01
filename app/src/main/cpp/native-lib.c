#include <jni.h>
#include "file.h"


struct DIR_NODE {
    char *path;
    size_t size;
};

JNIEXPORT void JNICALL
Java_psycho_euphoria_file_Native_deleteFile(JNIEnv *env, jobject thiz, jstring path) {
    const char *dir = (*env)->GetStringUTFChars(env, path, NULL);
    remove_directory(dir);
    (*env)->ReleaseStringChars(env, path, (jchar *) dir);
}

JNIEXPORT jstring JNICALL
Java_psycho_euphoria_file_Native_calculateDirectory(JNIEnv *env, jobject thiz, jstring path) {
    const char *dir = (*env)->GetStringUTFChars(env, path, NULL);

    LIST *dirtories = list_directories(dir);

    size_t len = 10;
    size_t t = 0;
    char *l = malloc(len);
    memset(l, '\0', len);
    while (dirtories) {
        size_t total = 0;
        calculate_files_recursively(dirtories->line, &total);
        char buf[10];
        readable_size(total, buf);
        size_t tl = strlen(dirtories->line) + strlen(buf) + 3;
        t += tl;
        char b[tl];
        sprintf(b, "%s %s\n", dirtories->line, buf);
        if (len < t) {
            len = t << 1;
            l = realloc(l, len);
        }
        strcat(l, b);
        l[len - 1] = '\0';

        dirtories = dirtories->next;
    }


    (*env)->ReleaseStringChars(env, path, (jchar *) dir);
    return (*env)->NewStringUTF(env, (const char *) l);

}