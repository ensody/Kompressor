@file:OptIn(ExperimentalWasmJsInterop::class)

package com.ensody.kompressor.core

import kotlin.js.JsAny
import kotlin.js.JsReference
import kotlin.js.toJsReference

internal actual fun createFinalizationRegistry(cleanupCallback: (JsAny) -> Unit): FinalizationRegistry =
    createFinalizationRegistryImpl(cleanupCallback)

@JsFun("(cleanupCallback) => new FinalizationRegistry(cleanupCallback)")
private external fun createFinalizationRegistryImpl(cleanupCallback: (JsAny) -> Unit): FinalizationRegistry

internal actual fun toJsReference(value: Any): JsAny =
    value.toJsReference()

internal actual fun getFinalizer(value: JsAny): () -> Unit =
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE", "UNCHECKED_CAST")
    (value as JsReference<() -> Unit>).get()
