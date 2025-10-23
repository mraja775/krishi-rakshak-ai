package com.krishirakshak.app.util

/**
 * A generic wrapper class for handling success and error states in the app.
 * This follows the Result pattern for clean error handling.
 */
sealed class Result<out T> {
    /**
     * Success state with data
     */
    data class Success<T>(val data: T) : Result<T>()

    /**
     * Error state with exception and optional message
     */
    data class Error(
        val exception: Exception,
        val message: String? = exception.message
    ) : Result<Nothing>()

    /**
     * Loading state (optional, can be used for UI)
     */
    object Loading : Result<Nothing>()
}

/**
 * Extension function to check if Result is success
 */
fun <T> Result<T>.isSuccess(): Boolean = this is Result.Success

/**
 * Extension function to check if Result is error
 */
fun <T> Result<T>.isError(): Boolean = this is Result.Error

/**
 * Extension function to get data or null
 */
fun <T> Result<T>.getOrNull(): T? = when (this) {
    is Result.Success -> data
    else -> null
}

/**
 * Extension function to execute code on success
 */
inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) {
        action(data)
    }
    return this
}

/**
 * Extension function to execute code on error
 */
inline fun <T> Result<T>.onError(action: (Exception) -> Unit): Result<T> {
    if (this is Result.Error) {
        action(exception)
    }
    return this
}
