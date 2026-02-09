# Changelog

## Unreleased

* Added `StreamCompressor`/`StreamDecompressor` to `kompressor-core` on JVM, which allow wrapping Java IO streams, such
  as those from Apache Commons
  Compress.
* Added `kompressor-js` module providing support for JS and WasmJS targets using the browser's Compression Streams
  API.
* Added `ExperimentalCompressionFormat` annotation to mark formats that might not be supported on all platforms or execution environments.

## 0.4.6

* Fixed incorrect JNI lib name on Android.
* CI: Run tests on Android emulator to prevent this bug from happening again.
* Bump NativeBuilds 0.9.0 which allows fully overriding the JNI loading logic.

## 0.4.5

* Fixed Android bundling duplicate .so files from NativeBuilds.
* Fixed Android JNI loading.
* Bump NativeBuilds 0.8.4.

## 0.4.4

* Bump NativeBuilds 0.8.1.

## 0.4.3

* Simplified JNI cross-compilation via new NativeBuilds plugin.

## 0.4.2

* Fixed watchosArm32 builds.

## 0.4.1

* Fixed JVM Linux builds.
* Fixed zlib loading in JVM on Windows.

## 0.4.0

* Added Brotli support.

## 0.3.1

* Ensure zlib streams a zero-initialized.

## 0.3.0

* Added zlib support (gzip & deflate).

## 0.2.0

* Added Ktor integration.
* Renamed kompressor-zstd module to kompressor-zstd--nativelib, so Android unit tests on desktop hosts can automatically switch to the JVM dependency.

## 0.1.1

* Added zstd support.
* Added kotlinx-io integration.
