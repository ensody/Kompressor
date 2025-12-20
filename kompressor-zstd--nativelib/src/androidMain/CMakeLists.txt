cmake_minimum_required(VERSION 3.4.1)

project(zstd-jni)

set(NATIVEBUILDS_CMAKE_DIR "${PROJECT_SOURCE_DIR}/../../build/nativebuilds-cmake")
include("${NATIVEBUILDS_CMAKE_DIR}/zstd-libzstd-android.cmake")

file(GLOB_RECURSE SOURCES
        "../jvmCommonMain/jni/*.cpp"
        "../../../jni/common/src/*.cpp"
)
add_library(zstd-jni SHARED ${SOURCES})
include_directories(zstd-jni "../../../jni/common/include")

target_link_libraries(zstd-jni zstd-libzstd)
