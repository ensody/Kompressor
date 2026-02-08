@file:OptIn(ExperimentalWasmJsInterop::class)

package com.ensody.kompressor.core

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny

internal actual fun createFinalizationRegistry(cleanupCallback: (JsAny) -> Unit): FinalizationRegistry =
    js("new FinalizationRegistry(cleanupCallback)")

internal actual fun toJsReference(value: Any): JsAny =
    value.asDynamic() as JsAny

internal actual fun getFinalizer(value: JsAny): () -> Unit =
    value.asDynamic() as (() -> Unit)
