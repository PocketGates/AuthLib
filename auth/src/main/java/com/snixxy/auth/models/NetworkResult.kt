package com.snixxy.auth.models

sealed class NetworkResult<out S>(val data: S?, val error: AuthError?) {
    class Success<out S>(value: S?) : NetworkResult<S>(value, null)
    class Error<out S>(error: AuthError) : NetworkResult<S>(null, error)
}
