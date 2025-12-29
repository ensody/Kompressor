cmake_minimum_required(VERSION 3.4.1)

project(brotli-jni)

set(NATIVEBUILDS_CMAKE_DIR "${PROJECT_SOURCE_DIR}/../../build/nativebuilds-cmake")
include("${NATIVEBUILDS_CMAKE_DIR}/brotli-libbrotlicommon-android.cmake")
include("${NATIVEBUILDS_CMAKE_DIR}/brotli-libbrotlidec-android.cmake")
include("${NATIVEBUILDS_CMAKE_DIR}/brotli-libbrotlienc-android.cmake")

file(GLOB_RECURSE SOURCES
        "../jvmCommonMain/jni/*.cpp"
        "../../../jni/common/src/*.cpp"
)
add_library(brotli-jni SHARED ${SOURCES})
include_directories(brotli-jni "../../../jni/common/include")

target_link_libraries(brotli-jni brotli-libbrotlicommon brotli-libbrotlidec brotli-libbrotlienc)
