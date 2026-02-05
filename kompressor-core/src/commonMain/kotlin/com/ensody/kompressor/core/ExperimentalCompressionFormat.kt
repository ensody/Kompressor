package com.ensody.kompressor.core

/**
 * Marks a compression format as experimental on a specific platform, see documentation.
 *
 * This is used for formats that might not be supported by all environments (e.g., Brotli in some browsers).
 */
@RequiresOptIn(message = "This compression format is experimental on this platform.")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.TYPEALIAS)
public annotation class ExperimentalCompressionFormat
