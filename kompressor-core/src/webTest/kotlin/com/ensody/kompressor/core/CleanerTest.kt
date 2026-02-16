package com.ensody.kompressor.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class CleanerTest {
    @Test
    fun testCleaner() = runTest {
        var cleaned = false
        val resource = object : AutoCloseable {
            override fun close() {
                cleaned = true
            }
        }

        fun create() {
            // The handle must be local to this function so it can be GC'd after the function returns
            createCleaner(resource) {
                it.close()
            }
        }

        create()

        // trigger GC
        for (i in 0 until 100) {
            if (cleaned) break
            triggerGc()
            delay(10)
        }

        assertTrue(cleaned, "Cleanup should have been called")
    }

    private fun triggerGc() {
        // Create memory pressure to encourage GC
        val list = mutableListOf<ByteArray>()
        for (i in 0 until 10) {
            list.add(ByteArray(1024 * 1024))
        }
    }
}
