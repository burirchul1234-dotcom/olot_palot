package com.olotpalot.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

/**
 * Injected dispatchers so tests can swap in a TestDispatcher
 * and no repository hard-codes Dispatchers.IO.
 */
interface AppDispatchers {
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val main: CoroutineDispatcher
}

/** A process-wide scope for work that must outlive a screen (seeding, progress flushes, analytics). */
class ApplicationScope(val scope: CoroutineScope)
