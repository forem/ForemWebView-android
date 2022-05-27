package com.forem.android.utility

/**
 * Helper class used to observer data and its status from local/remote repository.
 *
 * Reference: https://github.com/sberoch/RickAndMorty-AndroidArchitectureSample/blob/master/app/src/main/java/com/example/rickandmorty/utils/Resource.kt
 */
data class Resource<out T>(val status: Status, val data: T?, val message: String?) {

    /** Current status of data call. */
    enum class Status {
        SUCCESS,
        ERROR,
        LOADING
    }

    companion object {
        /** Returns data with successful status. */
        fun <T> success(data: T): Resource<T> {
            return Resource(Status.SUCCESS, data, null)
        }

        /** Returns data with error message and optional data. */
        fun <T> error(message: String, data: T? = null): Resource<T> {
            return Resource(Status.ERROR, data, message)
        }

        /** Returns data during loading phase of data call. */
        fun <T> loading(data: T? = null): Resource<T> {
            return Resource(Status.LOADING, data, null)
        }
    }
}
