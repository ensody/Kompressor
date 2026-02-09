@file:OptIn(ExperimentalWasmJsInterop::class)

package com.ensody.kompressor.core

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny

/**
 * Returns a handler which calls [cleanup] when the given [resource] is GC'd.
 *
 * **THIS IS NOT THE SAME API AS THE JVM VERSION**
 */
public fun createCleaner(resource: Any, cleanup: () -> Unit) {
    Cleaner.register(resource) { cleanup() }
}

private object Cleaner {
    private val finalizerRegistry = createFinalizationRegistry { finalizer ->
        getFinalizer(finalizer)()
    }

    fun register(target: Any, finalizer: () -> Unit) {
        finalizerRegistry.register(toJsReference(target), toJsReference(finalizer))
    }
}

internal external class FinalizationRegistry : JsAny {
    fun register(target: JsAny, heldValue: JsAny)
}

internal expect fun createFinalizationRegistry(cleanupCallback: (JsAny) -> Unit): FinalizationRegistry

internal expect fun toJsReference(value: Any): JsAny

internal expect fun getFinalizer(value: JsAny): () -> Unit
