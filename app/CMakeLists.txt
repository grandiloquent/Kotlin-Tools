# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)


add_library( libiconv
             SHARED
             IMPORTED )

set_target_properties( libiconv
                       # Specifies the parameter you want to define.
                       PROPERTIES IMPORTED_LOCATION
                       # Provides the path to the library you want to import.
                       C:/Users/Administrator/Desktop/.exercises/android/Tools/app/libs/${ANDROID_ABI}/libiconv.so )

include_directories(src/main/cpp/thirdparty/include
                    src/main/cpp/thirdparty/libcharset
                    src/main/cpp/thirdparty/libcharset/include
 )

add_library( # Sets the name of the library.
             native-lib

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             src/main/cpp/native-lib.c
             src/main/cpp/file.c)


find_library( # Sets the name of the path variable.
              log-lib
              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

target_link_libraries( libiconv
                       native-lib
                       ${log-lib} )