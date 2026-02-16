package com.ensody.kompressor.core

import java.lang.ref.PhantomReference
import java.lang.ref.ReferenceQueue
import java.util.concurrent.ConcurrentHashMap

/**
 * Returns a handler which calls [cleanup] with the given [resource] when the returned handler is garbage collected.
 *
 * IMPORTANT: [resource] MUST NOT, directly or indirectly, hold a reference to the returned cleanup handler.
 */
public fun <T : Any> createCleaner(resource: T, cleanup: (T) -> Unit): Any {
    val ref = Object()
    PhantomCleaner.register(ref, resource, cleanup)
    return ref
}

private object PhantomCleaner {
    private val queue = ReferenceQueue<Any>()
    private val cleanupTasks = ConcurrentHashMap<PhantomReference<Any>, Pair<Any, (Any) -> Unit>>()

    init {
        val cleanerThread = Thread {
            while (true) {
                try {
                    val ref = queue.remove()
                    val (handle, cleanup) = cleanupTasks.remove(ref) ?: continue
                    cleanup(handle)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }
        cleanerThread.isDaemon = true
        cleanerThread.start()
    }

    fun <T : Any> register(obj: Any, handle: T, cleanup: (T) -> Unit) {
        val ref = PhantomReference(obj, queue)
        @Suppress("UNCHECKED_CAST")
        cleanupTasks[ref] = (handle to cleanup) as Pair<Any, (Any) -> Unit>
    }
}
