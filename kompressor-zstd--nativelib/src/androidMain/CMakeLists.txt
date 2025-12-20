cmake_minimum_required(VERSION 3.4.1)

project(crypto-jni)

set(NATIVEBUILDS_CMAKE_DIR "${PROJECT_SOURCE_DIR}/../../build/nativebuilds-cmake")
include("${NATIVEBUILDS_CMAKE_DIR}/zstd-libzstd-android.cmake")

file(GLOB_RECURSE SOURCES "../jvmCommonMain/jni/*.cpp")
add_library(crypto-jni SHARED ${SOURCES})

target_link_libraries(crypto-jni zstd-libzstd)
