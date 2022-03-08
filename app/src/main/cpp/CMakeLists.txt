
# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

# 指定版本
cmake_minimum_required(VERSION 3.18.1)

# Declares and names the project.

project("native-lib")

# 添加子目录
add_subdirectory(librtmp)
# 添加子项目，libyuv 作为一个子项目自己编译，有自己的 CMakeList.txt。
add_subdirectory(libyuv)

#指定librtmp头文件查找路径
include_directories(librtmp)
# 外部头文件路径，因为我们要引用 libyuv.h
include_directories(libyuv/include)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.


add_library( # 库名称
        native-lib

        # 动态库
        SHARED

        # 源文件相对路径
        native-lib.cpp)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        native-lib

        # 编译的 rtmp 静态库
        rtmp

        yuv

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})
