@file:OptIn(ExperimentalWasmJsInterop::class)

package com.ensody.kompressor.core

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny

/**
 * Returns a handler which calls [cleanup] with the given [resource] when the returned handler is garbage collected.
 *
 * IMPORTANT: [resource] MUST NOT, directly or indirectly, hold a reference to the returned cleanup handler.
 */
public fun <T> createCleaner(resource: T, cleanup: (T) -> Unit): Any {
    val handle = Any()
    Cleaner.register(handle) { cleanup(resource) }
    return handle
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
