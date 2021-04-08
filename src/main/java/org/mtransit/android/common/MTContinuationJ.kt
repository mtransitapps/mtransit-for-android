package org.mtransit.android.common

/**
 * Coroutines from Java 7.
 */
abstract class MTContinuationJ<in T> : kotlin.coroutines.Continuation<T> {
    abstract fun resume(value: T)
    abstract fun resumeWithException(exception: Throwable)
    override fun resumeWith(result: Result<T>) = result.fold(::resume, ::resumeWithException)
}