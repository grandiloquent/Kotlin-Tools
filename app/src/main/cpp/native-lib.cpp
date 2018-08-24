#include <jni.h>
#include <string>
#include <dirent.h>
#include <sys/stat.h>


inline bool file_exist(const std::string &path) {
    struct stat buffer;
    return (stat(path.c_str(), &buffer) == 0);
}

int list_files() {
}
