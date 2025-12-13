# Kompressor - Kotlin Multiplatform compression algorithms

[![Maven Central](https://img.shields.io/maven-central/v/com.ensody.kompressor/kompressor-core?color=%2345cf00)](https://central.sonatype.com/artifact/com.ensody.kompressor/kompressor-core)

This project provides compression algorithms for Kotlin Multiplatform.

The underlying native libraries are provided by the [NativeBuilds](https://github.com/ensody/native-builds) project.

## Supported platforms

* Android
* JVM
* All native (iOS, macOS, Linux, Windows, tvOS, watchOS)

## Supported algorithms

* zstd/ZStandard
* ...more coming soon (deflate/gzip, lz4, bzip2)...

## Installation

Add the package to your `build.gradle.kts`'s `dependencies {}`:

```kotlin
dependencies {
    // Add the BOM using the desired library version
    api(platform("com.ensody.kompressor:kompressor-bom:VERSION"))

    // zstd/Zstandard
    api("com.ensody.kompressor:kompressor-zstd")
    // Select the native zstd library version from NativeBuilds
    api("com.ensody.nativebuilds:zstd-libzstd:1.5.7.4")
}
```

IMPORTANT: One reason why you should explicitly define the NativeBuilds library version is that dependabot and other automatic update tools might otherwise not suggest updates of native libraries. Since these are usually written in C/C++ or possibly unsafe Rust, you really want to stay up to date with the latest security fixes.

Also, make sure you've integrated the Maven Central repo, e.g. in your root `build.gradle.kts`:

```kotlin
subprojects {
    repositories {
        // ...
        mavenCentral()
        // ...
    }
}
```

## Example: zstd

Every compression algorithm implements the `Transform` interface.

If the data fits in RAM you can simply call the `.transform(ByteArray)` extension function:

```kotlin
val testData: ByteArray = Random.nextBytes(128 * 1024 + 3)
val compressed: ByteArray = ZstdCompressor(compressionLevel = 3).transform(testData)
val decompressed: ByteArray = ZstdDecompressor().transform(compressed)
assertContentEquals(testData, decompressed)
```

Normally, you'll want to stream the data, so even large inputs fit in RAM. While `Transform` provides a low-level API for this, let's better use kotlinx-io:

```kotlin
val source: Source = ...
val sink: Sink = ...
source.pipe(ZstdCompressor(compressionLevel = 3)).buffered().transferTo(sink)
```

Or for decompression:

```kotlin
source.pipe(ZstdDecompressor()).buffered().transferTo(sink)
```

You can also build a pipe with a sink:

```kotlin
val decompressor = ZstdDecompressor().pipe(sink).buffered()
source.transferTo(decompressor)
decompressor.close() // this writes any remaining data to the sink
```

## License

```
Copyright 2025 Ensody GmbH, Waldemar Kornewald

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
