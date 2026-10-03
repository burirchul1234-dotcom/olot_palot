package com.olotpalot.core.common

/**
 * A small, explicit result type for repository/use-case boundaries.
 * We deliberately avoid throwing across layers: UI decides how to present failure.
 */
sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

sealed interface AppError {
    data object Offline : AppError
    data class NotFound(val what: String) : AppError
    data class Unexpected(val cause: Throwable? = null) : AppError
    data class Message(val text: String) : AppError
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(value))
    is AppResult.Failure -> this
}

fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Success)?.value

suspend inline fun <T> runCatchingApp(crossinline block: suspend () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: kotlinx.coroutines.CancellationException) {
    throw e
} catch (e: Throwable) {
    AppResult.Failure(AppError.Unexpected(e))
}
